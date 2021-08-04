package com.example.bluetooth_le_gatt_sborka;

import static com.example.bluetooth_le_gatt_sborka.BluetoothLEService.ACTION_DATA_AVAILABLE;
import static com.example.bluetooth_le_gatt_sborka.BluetoothLEService.ACTION_GATT_CONNECTED;
import static com.example.bluetooth_le_gatt_sborka.BluetoothLEService.ACTION_GATT_DISCONNECTED;
import static com.example.bluetooth_le_gatt_sborka.BluetoothLEService.ACTION_GATT_SERVICES_DISCOVERED;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.bluetooth_le_gatt_sborka.support.MyDate;

import java.util.Date;

/**
 * Для данного устройства BLE это действие предоставляет пользовательский интерфейс для подключения,
 * отображения данных и отображения служб и характеристик GATT,
 * поддерживаемых устройством. Действие взаимодействует с {@code BluetoothLeService},
 * который, в свою очередь, взаимодействует с Bluetooth LE API.
 */
public class DeviceControlActivity extends AppCompatActivity {

    public static final String
            EXTRAS_DEVICE_NAME = "DEVICE_NAME",
            EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS",
            TAG = DeviceControlActivity.class.getSimpleName();
    private TextView measurementsField;
    /**
     * Обрабатывает различные события, инициированные Сервисом.
     * <p>ACTION_GATT_CONNECTED: подключен к серверу GATT.</p>
     * <p>ACTION_GATT_DISCONNECTED: отключен от сервера GATT.</p>
     * <p>ACTION_GATT_SERVICES_DISCOVERED: обнаружены службы GATT.</p>
     * <p>ACTION_DATA_AVAILABLE: получены данные от устройства.</p>
     * <p>Это может быть результатом операций чтения или уведомления.</p>
     */
    private final BroadcastReceiver gattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            switch (action) {
                case ACTION_GATT_CONNECTED:
                    updateConnectionState(R.string.connected);
                    invalidateOptionsMenu();
                    break;
                case ACTION_GATT_DISCONNECTED:
                    updateConnectionState(R.string.disconnected);
                    invalidateOptionsMenu();
                    break;
                case ACTION_DATA_AVAILABLE:
                    displayMeasurements(intent.getStringExtra(BluetoothLEService.MEASUREMENTS_DATA));
                    break;
            }
        }
    };
    private String deviceAddress;
    private BluetoothLEService bluetoothLEService;
    /**
     * Код для управления жизненным циклом службы
     */
    private final ServiceConnection serviceConnection = new ServiceConnection() {

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

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.result);

        findViewById(R.id.button_back).setOnClickListener(view -> finish());

        final Intent intent = getIntent();
        deviceAddress = intent.getStringExtra(EXTRAS_DEVICE_ADDRESS);

        measurementsField = findViewById(R.id.ac_value);

        if (getSupportActionBar() != null) getSupportActionBar().setTitle(intent.getStringExtra(EXTRAS_DEVICE_NAME));

        Intent gattServiceIntent = new Intent(this, BluetoothLEService.class);

        // BIND_AUTO_CREATE, означающий, что, если сервис,
        // к которому мы пытаемся подключиться, не работает, то он будет запущен
        bindService(gattServiceIntent, serviceConnection, BIND_AUTO_CREATE);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // registerReceiver - регистрация приемника BroadcastReceiver
        registerReceiver(gattUpdateReceiver, makeGattUpdateIntentFilter());
        if (bluetoothLEService != null)
            Log.d(TAG, "Connect request result = " + bluetoothLEService.connect(deviceAddress));
    }

    @Override
    protected void onPause() {
        super.onPause();

        //отключение приёмника BroadcastReceiver
        unregisterReceiver(gattUpdateReceiver);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // отсоединение от сервиса
        unbindService(serviceConnection);
    }

    public void displayMeasurements(String measurements) {
        if (measurements != null) {

            Handler displayHandler = new Handler(Looper.getMainLooper()) {
                @Override
                public void handleMessage(Message msg) {
                    super.handleMessage(msg);
                    if (msg.obj != null) {

                        Date time = new Date();
                        ((TextView) findViewById(R.id.ac_date)).setText(MyDate.toDateString(time));
                        ((TextView) findViewById(R.id.ac_time)).setText(MyDate.toTimeString(time));

                        String measurements = msg.obj.toString();
                        measurementsField.setText(measurements);
                    }
                }
            };

            Message message = new Message();
            message.obj = measurements;
            displayHandler.sendMessage(message);
        }
    }

    private void updateConnectionState(int resourceId) {
        Log.i(TAG, getString(resourceId));
        // runOnUiThread(() -> connectionStatus.setText(resourceId));
    }
}
