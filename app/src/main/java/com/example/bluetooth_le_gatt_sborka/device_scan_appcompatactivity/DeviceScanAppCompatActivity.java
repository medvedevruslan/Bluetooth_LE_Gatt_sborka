package com.example.bluetooth_le_gatt_sborka.device_scan_appcompatactivity;

import static com.example.bluetooth_le_gatt_sborka.SampleGattAttributes.MANOMETER_ADDRESS;
import static com.example.bluetooth_le_gatt_sborka.SampleGattAttributes.MICROLIFE_THERMOMETER_ADDRESS;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.bluetooth_le_gatt_sborka.DeviceControlActivity;
import com.example.bluetooth_le_gatt_sborka.R;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Для сканирования и отображения доступных устройств Bluetooth LE.
 */
public class DeviceScanAppCompatActivity extends AppCompatActivity implements PermissionsProcessing {

    public static final String TAG = "Medvedev DSA " + DeviceScanAppCompatActivity.class.getSimpleName();
    private static final long SCAN_PERIOD = 10000;

    private String device = "error";

    /**
     * Обратный вызов сканирования при API выше 21
     */
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private final ScanCallback scanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult scanResult) {
            super.onScanResult(callbackType, scanResult);

            if (scanResult.getDevice().getName() != null && scanResult.getDevice().getAddress() != null) {

                runOnUiThread(() -> {

                    BluetoothDevice bluetoothDevice = scanResult.getDevice();

                    // заметки себе: записать все адреса bluetooth устройств в класс SampleGattAttributes
                    // заметки себе: подключаться может по названию устройства, а не по мак адресам?
                    // заметки себе: сделать Тоаст с уведомлением о автоматическом подсоединении с конкретным устройством

                    // автоматическое соединение при сопряжении с Манометром u пирометром
                    if (bluetoothDevice.getAddress().equals(device)) {
                        connectWithDevice(bluetoothDevice);
                    }
                });
            }
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            super.onBatchScanResults(results);
            Log.d(TAG, "onBatchScanResults, results size:" + results.size());
            for (ScanResult sr : results) {
                Log.d(TAG, "onBatchScanResults results:  " + sr.toString());
            }
        }

        @Override
        public void onScanFailed(int errorCode) {
            super.onScanFailed(errorCode);
            Log.e(TAG, "onScanFailed, code is : " + errorCode);
        }
    };

    /**
     * Обратный вызов сканирования при API 20 и ниже
     */
    private final BluetoothAdapter.LeScanCallback lowEnergyScanCallback = new BluetoothAdapter.LeScanCallback() {

        /**
         * @param bluetoothDevice название устройства
         * @param rssi уровень сигнала, чем ниже значение, тем хуже сигнал
         * @param scanRecord Содержание записи advertising, предлагаемой удаленным устройством.
         */
        @Override
        public void onLeScan(BluetoothDevice bluetoothDevice, int rssi, byte[] scanRecord) {
            Log.d(TAG, " onLeScan LeScanCallback lowEnergyScanCallback");
            runOnUiThread(() -> {
                if (bluetoothDevice.getAddress().equals(device)) {
                    connectWithDevice(bluetoothDevice);
                }
                Log.d(TAG, "lowEnergyScanCallback");
            });
        }
    };
    private final ActivityResultLauncher<Intent> mStartForResult = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
        if (result.getResultCode() == RESULT_CANCELED) finish();
    });
    BluetoothLeScanner bluetoothLeScanner;
    ScanSettings bleScanSettings = null;
    /** Флаг разрешённости всех опасных разрешений приложения пользователем. */
    private boolean allPermissionsGranted = false;
    private BluetoothAdapter bluetoothAdapter;
    private boolean checkScanning;
    private Handler handler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle("Scan Bluetooth Low Energy Devices");
        setContentView(R.layout.device_scan_appcompatactivity);
        findViewById(R.id.next_alco_test).setOnClickListener(view -> {

        });
        findViewById(R.id.next_term_test).setOnClickListener(view -> {
            device = MICROLIFE_THERMOMETER_ADDRESS;
            if (allPermissionsGranted) scanLowEnergyDevice(true);
            else new DialogFragment(this).show(getSupportFragmentManager(), "AlertDialog");
        });
        findViewById(R.id.next_pres_test).setOnClickListener(view -> {
            device = MANOMETER_ADDRESS;
            if (allPermissionsGranted) scanLowEnergyDevice(true);
            else new DialogFragment(this).show(getSupportFragmentManager(), "AlertDialog");
        });

        checkBluetoothModule();

        handler = new Handler(Looper.getMainLooper());


        //Инициализирует адаптер Bluetooth.
        // Для уровня API 18 и выше получите ссылку на BluetoothAdapter через BluetoothManager
        final BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();

        //Проверяет, поддерживается ли Bluetooth на устройстве.
        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth not supported", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        checkPermissions();
    }

    /**
     * Функция проверки наличия Bluetooth-модуля и его возможности использования технологии
     * поключения Bluetooth LE
     * <p>
     * Завершает приложение при отсутствии необходимого на устройстве
     * </p>
     */
    private void checkBluetoothModule() {
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH)) {
            Toast.makeText(this, "На данном устройстве отсутствует Bluetooth-модуль! Приложение не будет функционировать!", Toast.LENGTH_SHORT).show();
            finish();
        }
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, "Данное устройство не поддерживает возможность подключения Bluetooth LE! Приложение не будет функционировать!", Toast.LENGTH_SHORT).show();
            finish();
        }

    }

    @Override
    public void checkPermissions() {
        ArrayList<String> permissionsList = new ArrayList<>();
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q)
            permissionsList.add(Manifest.permission.ACCESS_COARSE_LOCATION);
        else {
            permissionsList.add(Manifest.permission.ACCESS_BACKGROUND_LOCATION);
            permissionsList.add(Manifest.permission.ACCESS_FINE_LOCATION);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                permissionsList.addAll(Arrays.asList(
                        Manifest.permission.BLUETOOTH_CONNECT,
                        Manifest.permission.BLUETOOTH_SCAN));
            }
        }
        // Очищаем список опасных разрешений от тех, разрешение для которых уже дано
        for (int i = 0; i < permissionsList.size(); i++) {
            String permission = permissionsList.get(i);
            if (ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED) {
                permissionsList.remove(i);
                i--;
            }
        }
        if (permissionsList.size() > 0) {
            ActivityCompat.requestPermissions(this, permissionsList.toArray(new String[0]), 13);
        } else allPermissionsGranted = true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 13) {
            if (grantResults.length > 0) {
                allPermissionsGranted = true;
                for (int result : grantResults) {
                    if (result != PackageManager.PERMISSION_GRANTED) {
                        allPermissionsGranted = false;
                        break;
                    }
                }
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Убедитесь, что на устройстве включен Bluetooth. Если Bluetooth в настоящее время не включен,
        // активируйте намерение отобразить диалоговое окно с просьбой предоставить пользователю разрешение
        // на его включение
        if (!bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            mStartForResult.launch(enableBtIntent);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        scanLowEnergyDevice(false);
    }

    public void connectWithDevice(BluetoothDevice device) {

        final Intent intent = new Intent(this, DeviceControlActivity.class);
        intent.putExtra(DeviceControlActivity.EXTRAS_DEVICE_NAME, device.getName());
        intent.putExtra(DeviceControlActivity.EXTRAS_DEVICE_ADDRESS, device.getAddress());
        if (checkScanning) {
            //останавливается поиск
            startOrStopScanBle("stop");
            //bluetoothAdapter.stopLeScan(lowEnergyScanCallback);
            checkScanning = false;
        }
        startActivity(intent);
    }

    private void scanLowEnergyDevice(final boolean check) {

        // если true то сканирование идёт, а после запускается handler.postDelayed с остановкой сканирования
        if (check) {
            // Останавливает сканирование по истечении заданного периода сканирования.
            // PostDelayed - задержка по времени
            handler.postDelayed(() -> {
                checkScanning = false;
                startOrStopScanBle("stop");
                Log.d(TAG, " scanLowEnergyDevice.stopLEScan: " + true);
                // invalidateOptionsMenu - перерисовка ActionBar
                invalidateOptionsMenu();
            }, SCAN_PERIOD);

            checkScanning = true;
            startOrStopScanBle("start");
            Log.d(TAG, " scanLowEnergyDevice.startLeScan " + true);

        } else {
            checkScanning = false;
            startOrStopScanBle("stop");
            Log.d(TAG, " scanLowEnergyDevice.stopLeScan" + false + "№2");

        }
        invalidateOptionsMenu();
    }

    public void startOrStopScanBle(String stopOrStartTask) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && bluetoothAdapter != null) {
            Log.d(TAG, "VERSION.SDK_INT: " + Build.VERSION.SDK_INT + " check#1");
            if (bluetoothLeScanner == null) {
                bluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();
                // настройки для поиска термометра, взяты из приложения Microlife
                bleScanSettings = new ScanSettings.Builder().setScanMode(2).build();
            }

            if (stopOrStartTask.equals("start")) {
                Log.d(TAG, "begin to scan bluetooth devices...");
                checkScanning = true;

                try {
                    bluetoothLeScanner.startScan(scanCallback);
                    //bluetoothLeScanner.startScan((List<ScanFilter>) null, bleScanSettings, scanCallback);
                } catch (Exception e) {
                    Log.e(TAG, "startScan error." + e.getMessage());
                }

            } else {

                Log.d(TAG, "stop to scan bluetooth devices.");
                checkScanning = false;
                try {
                    bluetoothLeScanner.stopScan(scanCallback);
                } catch (Exception e2) {
                    Log.e(TAG, "stopScan error." + e2.getMessage());
                }
            }

        } else if (bluetoothAdapter != null) {
            Log.d(TAG, "VERSION.SDK_INT: " + Build.VERSION.SDK_INT + " check#2");

            if (stopOrStartTask.equals("start")) {
                bluetoothAdapter.startLeScan(lowEnergyScanCallback);
            } else if (stopOrStartTask.equals("stop")) {
                bluetoothAdapter.stopLeScan(lowEnergyScanCallback);
            }
        } else {
            Log.e(TAG, "bluetoothAdapter = null");
        }
    }
}