package com.example.bluetooth_le_gatt_sborka.device_scan_appcompatactivity;

import static android.bluetooth.le.ScanSettings.SCAN_MODE_LOW_LATENCY;
import static com.example.bluetooth_le_gatt_sborka.Activity3.EXTRA_ACRESULT;
import static com.example.bluetooth_le_gatt_sborka.BluetoothLEService.ACTION_DATA_AVAILABLE;
import static com.example.bluetooth_le_gatt_sborka.BluetoothLEService.ACTION_GATT_CONNECTED;
import static com.example.bluetooth_le_gatt_sborka.BluetoothLEService.ACTION_GATT_DISCONNECTED;
import static com.example.bluetooth_le_gatt_sborka.BluetoothLEService.ACTION_GATT_SERVICES_DISCOVERED;
import static com.example.bluetooth_le_gatt_sborka.BluetoothService.DEVICE_NAME;
import static com.example.bluetooth_le_gatt_sborka.BluetoothService.MESSAGE_CONNECTION_FAILED;
import static com.example.bluetooth_le_gatt_sborka.BluetoothService.MESSAGE_CONNECTION_LOST;
import static com.example.bluetooth_le_gatt_sborka.BluetoothService.MESSAGE_DEVICE_NAME;
import static com.example.bluetooth_le_gatt_sborka.BluetoothService.MESSAGE_READ;
import static com.example.bluetooth_le_gatt_sborka.BluetoothService.MESSAGE_STATE_CHANGE;
import static com.example.bluetooth_le_gatt_sborka.BluetoothService.STATE_CONNECTED;
import static com.example.bluetooth_le_gatt_sborka.BluetoothService.STATE_CONNECTING;
import static com.example.bluetooth_le_gatt_sborka.BluetoothService.STATE_LISTEN;
import static com.example.bluetooth_le_gatt_sborka.BluetoothService.STATE_NONE;
import static com.example.bluetooth_le_gatt_sborka.SampleGattAttributes.MANOMETER_ADDRESS;
import static com.example.bluetooth_le_gatt_sborka.SampleGattAttributes.MICROLIFE_THERMOMETER_ADDRESS;

import android.Manifest;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.bluetooth_le_gatt_sborka.Activity3;
import com.example.bluetooth_le_gatt_sborka.BluetoothLEService;
import com.example.bluetooth_le_gatt_sborka.BluetoothService;
import com.example.bluetooth_le_gatt_sborka.DeviceControlActivity;
import com.example.bluetooth_le_gatt_sborka.R;
import com.example.bluetooth_le_gatt_sborka.support.Ac015;
import com.example.bluetooth_le_gatt_sborka.support.AcResult;
import com.example.bluetooth_le_gatt_sborka.support.MyDate;
import com.example.bluetooth_le_gatt_sborka.support.MyPreference;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * Для сканирования и отображения доступных устройств Bluetooth LE.
 */
public class DeviceScanAppCompatActivity extends AppCompatActivity implements PermissionsProcessing {

    private BluetoothLEService bluetoothLEService;

    public static final String TAG = "Medvedev DSA " + DeviceScanAppCompatActivity.class.getSimpleName();
    /**
     * Bluetooth LE
     */
    private static final long SCAN_PERIOD = 10000;
    /**
     * Обработка запроса на включение Bluetooth-модуля устройства
     */
    private final ActivityResultLauncher<Intent> mStartForResult = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
        if (result.getResultCode() == RESULT_CANCELED)
            Toast.makeText(this, "Приложение не сможет подключаться к устройствам с отключенным Bluetooth-модулем!", Toast.LENGTH_SHORT).show();
    });
    /**
     * Объект для использования API сканирования Bluetooth LE устройств
     */
    BluetoothLeScanner bluetoothLeScanner;
    /**
     * Настройки сканирования Bluetooth LE устройств
     */
    ScanSettings bleScanSettings = null;
    private TextView
            viewProgress,
            viewStatus;
    private Ac015 mAc015;
    private AlertDialog mDialog = null;
    private AcResult mResult;
    private final Handler handler = new Handler(Looper.getMainLooper()) {
        public void handleMessage(Message msg) { //обработчик сообщений принятых от BTService
            int messageType = msg.what;
            switch (messageType) {
                case MESSAGE_STATE_CHANGE:
                    int state = msg.arg1;
                    switch (state) {
                        case STATE_NONE:
                            Log.d(TAG, "MESSAGE_STATE_CHANGE/none");
                            setViewStatus(R.string.bth_sts_none, null);
                            break;
                        case STATE_LISTEN:
                            Log.d(TAG, "MESSAGE_STATE_CHANGE/listen");
                            setViewStatus(R.string.bth_sts_listen, null);
                            break;
                        default:
                            switch (state) {
                                case STATE_CONNECTING:
                                    Log.d(TAG, "MESSAGE_STATE_CHANGE/connecting");
                                    setViewStatus(R.string.bth_sts_connecting, mAc015.device().getName());
                                    break;
                                case STATE_CONNECTED:
                                    Log.d(TAG, "MESSAGE_STATE_CHANGE/connected");
                                    setViewStatus(R.string.bth_sts_connected, mAc015.device().getName());
                                    mAc015.initializeBuffer();
                                    break;
                            }
                            break;
                    }
                    break;
                case MESSAGE_READ:
                    String data = new String((byte[]) msg.obj, 0, msg.arg1);
                    Log.d(TAG, "MESSAGE_READ[" + data.replaceAll("\r", "x0D").replaceAll("\n", "x0A") + "]");
                    dataReceived(data);
                    break;
                case MESSAGE_DEVICE_NAME:
                    Log.d(TAG, "MESSAGE_DEVICE_NAME[" + msg.getData().getString(DEVICE_NAME) + "]");
                    break;
                case MESSAGE_CONNECTION_FAILED:
                    Log.d(TAG, "MESSAGE_CONNECTION_FAILED sts=" + bluetoothService.getState());
                    connectionFailed();
                    break;
                case MESSAGE_CONNECTION_LOST:
                    Log.d(TAG, "MESSAGE_CONNECTION_LOST sts=" + bluetoothService.getState());
                    Toast.makeText(DeviceScanAppCompatActivity.this, R.string.msg_connection_lost, Toast.LENGTH_SHORT).show();
                    clear();
                    break;
            }
        }
    };
    private final BluetoothService bluetoothService = new BluetoothService(handler);
    private String device = "error";
    /**
     * Флаг разрешённости всех опасных разрешений приложения пользователем.
     */
    private boolean
            allPermissionsGranted = false,
            checkScanning;
    private BluetoothAdapter bluetoothAdapter;
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

    /**
     * Функция обрабатывает @param data и отправляет результат теста в другие активити для сохранения в базе данных.
     */
    private void dataReceived(String data) {
        Date xtime = new Date();
        String data2 = mAc015.receive(data);
        if (data2.length() >= 1) {
            int msgId = mAc015.analyze(data2);
            // if (msgId == R.string.ac_result) {
            //     setViewProgress(xtime, msgId);
            // }
            setViewProgress(xtime, msgId);
            if (mAc015.isResultReceived()) {
                bluetoothService.stop();
                mResult.setAcTime(xtime);
                mResult.setAcValue(data2);
                int resId = mResult.saveResult(this, mResult);
                if (resId == 0) {
                    Intent intent = new Intent(getApplication(), Activity3.class);
                    intent.putExtra(EXTRA_ACRESULT, mResult);
                    startActivity(intent);
                }
            } else if (mAc015.isErrorReceived()) {
                bluetoothService.stop();
                Intent intent4 = new Intent();
                intent4.putExtra("errm", getString(msgId));
                setResult(RESULT_CANCELED, intent4);
                finish();
            }
        }
    }

    private void setViewProgress(Date date, int resId) {
        viewProgress.setText(MyDate.toTimeString(date));
        viewProgress.append(">");
        viewProgress.append(getString(resId));
    }

    private void connectionFailed() {
        AlertDialog.Builder adb = new AlertDialog.Builder(this);
        adb.setTitle(mAc015.device().getName());
        adb.setMessage(R.string.msg_confirm_power);
        adb.setCancelable(false);
        adb.setPositiveButton(R.string.connect, (dialog, which) -> {
            bluetoothService.connect(mAc015.device());
            dialog.dismiss();
            mDialog = null;
        });
        adb.setNegativeButton(R.string.cancel, (dialog, which) -> {
            dialog.dismiss();
            mDialog = null;
            bluetoothService.stop();
            clear();
        });
        mDialog = adb.show();
    }

    private void connectPairedDevice() {
        BluetoothDevice device = bluetoothService.getPairedDeviceByName("E-200B");
        if (device == null) {
            showToast(R.string.msg_notfound_paired_device);
            return;
        }
        mResult.setDevice(device.getName());
        mAc015.setConnectStart(device);
        bluetoothService.connect(device);
    }

    private void showToast(int resId) {
        if (resId > 0) {
            Toast.makeText(getApplicationContext(), resId, Toast.LENGTH_SHORT).show();
        }
    }

    private void setViewStatus(int resId, String device) {
        viewStatus.setText(resId);
        if (device != null) {
            viewStatus.append(":");
            viewStatus.append(device);
            mResult.setDevice(device);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle("Scan Bluetooth Low Energy Devices");
        setContentView(R.layout.device_scan_appcompatactivity);
        findViewById(R.id.next_alco_test).setOnClickListener(view -> {
            if (allPermissionsGranted) {
                if (bluetoothService.getState() == 0) {
                    mResult.setTestType("alco_test");
                    bluetoothService.start();
                    connectPairedDevice();
                }
            } else new DialogFragment(this).show(getSupportFragmentManager(), "AlertDialog");
        });
        findViewById(R.id.next_term_test).setOnClickListener(view -> {
            device = MICROLIFE_THERMOMETER_ADDRESS;
            if (allPermissionsGranted) {
                mResult.setTestType("temp_test");
                scanLowEnergyDevice(true);
            } else new DialogFragment(this).show(getSupportFragmentManager(), "AlertDialog");
        });
        findViewById(R.id.next_pres_test).setOnClickListener(view -> {
            device = MANOMETER_ADDRESS;
            if (allPermissionsGranted) {
                mResult.setTestType("pres_test");
                scanLowEnergyDevice(true);
            } else new DialogFragment(this).show(getSupportFragmentManager(), "AlertDialog");
        });

        checkBluetoothModule();

        mAc015 = new Ac015();
        mResult = new AcResult();
        mResult.setFromPreference(new MyPreference(this));

        viewStatus = findViewById(R.id.bth_status);
        viewProgress = findViewById(R.id.bth_progress);

        //Инициализирует адаптер Bluetooth.
        // Для уровня API 18 и выше получите ссылку на BluetoothAdapter через BluetoothManager
        final BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();

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

    private void clear() {
        viewStatus.setText("");
        viewProgress.setText("");
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
    protected void onStart() {
        super.onStart();

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
        if (checkScanning) scanLowEnergyDevice(false);
        bluetoothService.stop();
        if (mDialog != null) {
            mDialog.dismiss();
            mDialog = null;
        }
        unregisterReceiver(gattUpdateReceiver);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(serviceConnection);
    }

    @Override
    protected void onResume() {
        super.onResume();

        clear();
        // registerReceiver - регистрация приемника BroadcastReceiver
        registerReceiver(gattUpdateReceiver, makeGattUpdateIntentFilter());
        if (bluetoothLEService != null) {
            // Log.d(TAG, "Connect request result = " + bluetoothLEService.connect(deviceAddress));
        }
    }

    /**
     * Bluetooth LE
     */
    public void connectWithDevice(BluetoothDevice device) {
        setViewStatus(R.string.bth_sts_connected, device.getName());

        // final Intent intent = new Intent(this, DeviceControlActivity.class);
        // intent.putExtra(DeviceControlActivity.EXTRAS_DEVICE_NAME, device.getName());
        // intent.putExtra(DeviceControlActivity.EXTRAS_DEVICE_ADDRESS, device.getAddress());
        if (checkScanning) {
            //останавливается поиск
            startOrStopScanBle(true);
            //bluetoothAdapter.stopLeScan(lowEnergyScanCallback);
            checkScanning = false;
        }
        // startActivity(intent);

        Intent gattServiceIntent = new Intent(this, BluetoothLEService.class);

        if (serviceConnection == null) bindService(gattServiceIntent, serviceConnection(device.getAddress()), BIND_AUTO_CREATE);
        else bindService(gattServiceIntent, serviceConnection, BIND_AUTO_CREATE);
    }

    /**
     * ???Функция сканирования Bluetooth LE устройств
     *
     * @param check true - запускает сканирование, false - останавливает
     */
    private void scanLowEnergyDevice(final boolean check) {
        String name = "";
        if (device.equals(MANOMETER_ADDRESS)) name += "UA-911BT-C";
        else if (device.equals(MICROLIFE_THERMOMETER_ADDRESS)) name += "NC150 BT";
        setViewStatus(R.string.bth_sts_connecting, name);
        // если true то сканирование идёт, а после запускается handler.postDelayed с остановкой сканирования
        if (check) {
            // Останавливает сканирование по истечении заданного периода сканирования.
            // PostDelayed - задержка по времени
            handler.postDelayed(() -> {
                checkScanning = false;
                startOrStopScanBle(true);
                Log.d(TAG, " scanLowEnergyDevice.stopLEScan: " + true);
            }, SCAN_PERIOD);

            checkScanning = true;
            startOrStopScanBle(false);
            Log.d(TAG, " scanLowEnergyDevice.startLeScan " + true);

        } else {
            checkScanning = false;
            startOrStopScanBle(true);
            Log.d(TAG, " scanLowEnergyDevice.stopLeScan" + false + "№2");
        }
    }

    /**
     * Bluetooth LE
     */
    public void startOrStopScanBle(boolean stopTask) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && bluetoothAdapter != null) {
            Log.d(TAG, "VERSION.SDK_INT: " + Build.VERSION.SDK_INT + " check#1");
            if (bluetoothLeScanner == null) {
                bluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();
                // настройки для поиска термометра, взяты из приложения Microlife
                bleScanSettings = new ScanSettings.Builder().setScanMode(SCAN_MODE_LOW_LATENCY).build();
            }

            if (stopTask) {
                Log.d(TAG, "stop to scan bluetooth devices.");
                checkScanning = false;
                try {
                    bluetoothLeScanner.stopScan(scanCallback);
                    clear();
                } catch (Exception e2) {
                    Log.e(TAG, "stopScan error." + e2.getMessage());
                }
            } else {
                Log.d(TAG, "begin to scan bluetooth devices...");
                checkScanning = true;

                try {
                    bluetoothLeScanner.startScan(scanCallback);
                    //bluetoothLeScanner.startScan((List<ScanFilter>) null, bleScanSettings, scanCallback);
                } catch (Exception e) {
                    Log.e(TAG, "startScan error." + e.getMessage());
                }

            }

        } else if (bluetoothAdapter != null) {
            Log.d(TAG, "VERSION.SDK_INT: " + Build.VERSION.SDK_INT + " check#2");

            if (stopTask) {
                bluetoothAdapter.stopLeScan(lowEnergyScanCallback);
            } else {
                bluetoothAdapter.startLeScan(lowEnergyScanCallback);
            }
        } else {
            Log.e(TAG, "bluetoothAdapter = null");
        }
    }
    private ServiceConnection serviceConnection;

    //Код для управления жизненным циклом службы.
    private ServiceConnection serviceConnection(String deviceAddress) {
        return serviceConnection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                bluetoothLEService = ((BluetoothLEService.LocalBinder) service).getService();
                if (!bluetoothLEService.initialize()) {
                    Log.d(TAG, "Невозможно инициализировать BluetoothManager");
                    finish();
                }
                //Автоматически подключается к устройству при успешной инициализации запуска к блютуз параметрам устройства.

                bluetoothLEService.connect(deviceAddress);
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                bluetoothLEService = null;
            }
        };
    }

    private final BroadcastReceiver gattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            switch (action) {
                case ACTION_GATT_CONNECTED:
                    break;
                case ACTION_GATT_DISCONNECTED:
                    break;
                case ACTION_GATT_SERVICES_DISCOVERED:
                    break;
                case ACTION_DATA_AVAILABLE:
                    displayMeasurements(intent.getStringExtra(BluetoothLEService.MEASUREMENTS_DATA));
                    break;
            }

            if (intent.getStringExtra("writeType") != null) {
                // writeTypeField.setText(intent.getStringExtra("writeType"));
                Log.d(TAG, "charprop " + intent.getStringExtra("writeType"));
            }
        }
    };

    /**
     * Функция создания фильтра канала широковещательных сообщений
     *
     * @return фильтр канала широковещательных сообщений
     */
    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLEService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(ACTION_DATA_AVAILABLE);
        return intentFilter;
    }

    public void displayMeasurements(String measurements) {
        if (measurements != null) {

            Handler displayHandler = new Handler(Looper.getMainLooper()) {
                @Override
                public void handleMessage(Message msg) {
                    super.handleMessage(msg);
                    if (msg.obj != null){
                        String measurements = msg.obj.toString();

                        //!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
                    }
                }
            };

            Message message = new Message();
            message.obj = measurements;
            displayHandler.sendMessage(message);
        }
    }
}