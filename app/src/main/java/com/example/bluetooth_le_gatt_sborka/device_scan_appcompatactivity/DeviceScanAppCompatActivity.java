package com.example.bluetooth_le_gatt_sborka.device_scan_appcompatactivity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
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
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.bluetooth_le_gatt_sborka.DeviceControlActivity;
import com.example.bluetooth_le_gatt_sborka.R;
import com.example.bluetooth_le_gatt_sborka.SampleGattAttributes;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Для сканирования и отображения доступных устройств Bluetooth LE.
 */
public class DeviceScanAppCompatActivity extends AppCompatActivity implements Scanning, PermissionsProcessing {

    public static final String TAG = "Medvedev DSA " + DeviceScanAppCompatActivity.class.getSimpleName();
    private static final long SCAN_PERIOD = 10000;
    // private BleDevicesListAdapter bleDevicesListAdapter;
    private final RecyclerViewAdapter adapter = new RecyclerViewAdapter(this);
    // Device scan callback KITKAT and below.
    private final BluetoothAdapter.LeScanCallback lowEnergyScanCallback = new BluetoothAdapter.LeScanCallback() {

        /**
         * @param device название устройства
         * @param rssi уровень сигнала, чем ниже значение, тем хуже сигнал
         * @param scanRecord Содержание записи advertising, предлагаемой удаленным устройством.
         */
        @Override
        public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
            Log.d(TAG, " onLeScan LeScanCallback lowEnergyScanCallback");
            runOnUiThread(() -> {
                Log.d(TAG, "adding device " + device);
                adapter.addItem(device);
                Log.d(TAG, "lowEnergyScanCallback");

                //уведомляет прикрепленных наблюдателей, что базовые данные были изменены,
                //и любое представление, отражающее набор данных, должно обновиться.
                // bleDevicesListAdapter.notifyDataSetChanged();
            });
        }
    };
    private final ActivityResultLauncher<Intent> mStartForResult = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
        if (result.getResultCode() == RESULT_CANCELED) finish();
    });
    BluetoothLeScanner bluetoothLeScanner;
    ScanSettings bleScanSettings = null;
    ArrayList<BluetoothDevice> notOpenAlertDialogToConnectThisDevices = new ArrayList<>();
    /**
     * Флаг разрешённости всех опасных разрешений приложения пользователем.
     * <p>
     * P.S. На данный момент проверяется только разрешение на определение точного местоположения!
     * </p>
     */
    private boolean allPermissionsGranted = false;
    private BluetoothAdapter bluetoothAdapter;
    private boolean checkScanning;
    private Handler handler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle("Scan Bluetooth Low Energy Devices");
        setContentView(R.layout.device_scan_appcompatactivity);

        checkBluetoothModule();

        RecyclerView rcView = findViewById(R.id.rcView);
        rcView.setLayoutManager(new LinearLayoutManager(this));
        rcView.setAdapter(adapter);

        handler = new Handler(Looper.getMainLooper());

        // // Используйте эту проверку, чтобы определить, поддерживается ли BLE на устройстве.
        // // Затем вы можете выборочно отключить функции, связанные с BLE.
        // if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
        //     Toast.makeText(this, "Bluetooth Low Energy не поддерживается на данном устройстве",
        //             Toast.LENGTH_SHORT).show();
        //     finish();
        //     return;
        // }


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
            // permissionsList.add(Manifest.permission.ACCESS_BACKGROUND_LOCATION);
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
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        if (!checkScanning) {
            menu.findItem(R.id.menu_stop).setVisible(false);
            menu.findItem(R.id.menu_scan).setVisible(true);
            menu.findItem(R.id.menu_refresh).setActionView(null);
        } else {
            menu.findItem(R.id.menu_stop).setVisible(true);
            menu.findItem(R.id.menu_scan).setVisible(false);
            menu.findItem(R.id.menu_refresh).setActionView(R.layout.actionbar_indeterminate_progress);
        }
        return true;
    }

    @SuppressLint("NonConstantResourceId")
    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        switch (menuItem.getItemId()) {
            case R.id.menu_scan:
                /*
                Проверка на предоставление опасных разрешений
                P.S. на рассмотрении вариант блокирования кнопки до предоставления всех разрешений
                 */
                if (!allPermissionsGranted) {
                    new DialogFragment(this).show(getSupportFragmentManager(), "AlertDialog");
                    break;
                }
                if (adapter.isNotEmpty()) {
                    adapter.clearList();
                }
                notOpenAlertDialogToConnectThisDevices.clear();
                scanLowEnergyDevice(true);
                break;
            case R.id.menu_stop:
                scanLowEnergyDevice(false);
                break;
        }
        return true;
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

        // bleDevicesListAdapter = new BleDevicesListAdapter();
        // setListAdapter(bleDevicesListAdapter);

        /*// Инициализирует адаптер представления списка.
        // поиск устройств при запуске приложения, а конкретно когда запускается onResume

        scanLowEnergyDevice(true);
        */
    }

    @Override
    protected void onPause() {
        super.onPause();
        scanLowEnergyDevice(false);
    }

    @Override
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
                Log.d(TAG, " scanLowEnergyDevice.stopLEScan: " + check);
                // invalidateOptionsMenu - перерисовка ActionBar
                invalidateOptionsMenu();
            }, SCAN_PERIOD);

/*            Thread threadStopwatch10Seconds = new Thread(new Runnable() {
                // отсчет времени в логах посекундно
                @Override
                public void run() {
                    count = 1;
                    for (count = 1; count <= 10; count ++ ) {
                        try {
                            TimeUnit.SECONDS.sleep(1);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        Log.d(TAG, "Время : " + count + " сек");
                    }
                }
            });threadStopwatch10Seconds.start();*/

            checkScanning = true;
            startOrStopScanBle("start");
            Log.d(TAG, " scanLowEnergyDevice.startLeScan " + check);

        } else {
            checkScanning = false;
            startOrStopScanBle("stop");
            Log.d(TAG, " scanLowEnergyDevice.stopLeScan" + check + "№2");

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

            ScanCallback scanCallback = new ScanCallback() {
                @Override
                public void onScanResult(int callbackType, ScanResult scanResult) {
                    super.onScanResult(callbackType, scanResult);

                    if (scanResult.getDevice().getName() != null && scanResult.getDevice().getAddress() != null) {

                        Log.d("112121", scanResult.getDevice().getName());

                        runOnUiThread(() -> {

                            /*Log.d(TAG, "onScanResult. callbackType is : " + callbackType + ",name: " +
                                    scanResult.getDevice().getName() + ",address: " +
                                    scanResult.getDevice().getAddress() + ",rssi: " + scanResult.getRssi());*/

                            BluetoothDevice bluetoothDevice = scanResult.getDevice();

                            // заметки себе: записать все адреса bluetooth устройств в класс SampleGattAttributes
                            // заметки себе: подключаться может по названию устройства, а не по мак адресам?
                            // заметки себе: сделать Тоаст с уведомлением о автоматическом подсоединении с конкретным устройством

                            // автоматическое соединение при сопряжении с Манометром u пирометром
                            if (bluetoothDevice.getAddress().equals(SampleGattAttributes.MANOMETER_ADDRESS) || bluetoothDevice.getAddress().equals(SampleGattAttributes.TESTO_SMART_PYROMETER_ADDRESS)) {
                                if (!notOpenAlertDialogToConnectThisDevices.contains(bluetoothDevice))
                                    showConnectionAlertDialog(bluetoothDevice);
                            }
                            adapter.addItem(bluetoothDevice);
                            // bleDevicesListAdapter.addDeviceToList(bluetoothDevice);
//
                            // //уведомляет прикрепленных наблюдателей, что базовые данные были изменены,
                            // //и любое представление, отражающее набор данных, должно обновиться.
                            // bleDevicesListAdapter.notifyDataSetChanged();
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
                    this.bluetoothLeScanner.stopScan(scanCallback);
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

    public void showConnectionAlertDialog(BluetoothDevice bluetoothDevice) {
        notOpenAlertDialogToConnectThisDevices.add(bluetoothDevice);
        AlertDialog.Builder quitDialog = new AlertDialog.Builder(this);
        quitDialog.setTitle("Подсоединиться к устройству: " + bluetoothDevice.getName() + " ?");

        quitDialog.setPositiveButton("Да!", (dialog, which) -> connectWithDevice(bluetoothDevice));

        quitDialog.setNegativeButton("Нет", (dialog, which) -> {

        });
        quitDialog.show();
    }
}