package com.example.bluetooth_le_gatt_sborka;

import android.app.Activity;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ExpandableListView;
import android.widget.SimpleExpandableListAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Для данного устройства BLE это действие предоставляет пользовательский интерфейс для подключения,
 * отображения данных и отображения служб и характеристик GATT,
 * поддерживаемых устройством. Действие взаимодействует с {@code BluetoothLeService},
 * который, в свою очередь, взаимодействует с Bluetooth LE API.
 */

public class DeviceControlActivity extends Activity {

    private final static String TAG = "Medvedev DCA " + DeviceControlActivity.class.getSimpleName();

    public static final String EXTRAS_DEVICE_NAME = "DEVICE_NAME";
    public static final String EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS";

    private TextView connectionStatus;
    private TextView dataField;
    private String deviceName;
    private String deviceAddress;
    private ExpandableListView gattServicesList;
    private BluetoothLEService bluetoothLEService;
    private ArrayList<ArrayList<BluetoothGattCharacteristic>> mGattCharacteristics =
            new ArrayList<ArrayList<BluetoothGattCharacteristic>>();
    private boolean connected = false;
    private BluetoothGattCharacteristic notifyCharacteristic;

    private final String LIST_NAME = "NAME";
    private final String LIST_UUID = "UUID";

    //Код для управления жизненным циклом службы.
    private final ServiceConnection serviceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            bluetoothLEService = ((BluetoothLEService.LocalBinder)service).getService();
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

    /*
      Обрабатывает различные события, инициированные Сервисом.
      ACTION_GATT_CONNECTED: подключен к серверу GATT.
      ACTION_GATT_DISCONNECTED: отключен от сервера GATT.
      ACTION_GATT_SERVICES_DISCOVERED: обнаружены службы GATT.
      ACTION_DATA_AVAILABLE: получены данные от устройства.
      Это может быть результатом операций чтения или уведомления.
     */
    private final BroadcastReceiver gattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (BluetoothLEService.ACTION_GATT_DISCONNECTED.equals(action)) {
                connected = true;
                updateConnectionState(R.string.connected);
                invalidateOptionsMenu();
            } else if (BluetoothLEService.ACTION_GATT_DISCONNECTED.equals(action)) {
                connected = false;
                updateConnectionState(R.string.disconnected);
                invalidateOptionsMenu();
                clearUI();
            } else if (BluetoothLEService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                //Показать все поддерживаемые услуги и характеристики в пользовательском интерфейсе.
                displayGattServices(bluetoothLEService.getSupportedGattServices());
            } else if (BluetoothLEService.ACTION_DATA_AVAILABLE.equals(action)) {
                displayData(intent.getStringExtra(BluetoothLEService.EXTRA_DATA));
            }
        }
    };

    /**
     * Если выбрана данная характеристика GATT, проверьте наличие поддерживаемых функций.
     * В этом примере демонстрируются функции «Чтение» и «Уведомление».
     * См. Http: d.android.comreferenceandroidbluetoothBluetoothGatt.html
     * для получения полного списка поддерживаемых характерных функций.
     * @param data
     */
    private final ExpandableListView.OnChildClickListener servicesListListener =
            new ExpandableListView.OnChildClickListener() {
                @Override
                public boolean onChildClick(ExpandableListView parent, View v,
                                            int groupPosition, int childPosition, long id) {
                    if (mGattCharacteristics != null) {
                        final BluetoothGattCharacteristic characteristic =
                                mGattCharacteristics.get(groupPosition).get(childPosition);
                        final int charaProp = characteristic.getProperties();
                        if ((charaProp | BluetoothGattCharacteristic.PROPERTY_READ) > 0) {
                            //Если есть активное уведомление о характеристике, сначала очистите его,
                            // чтобы оно не обновляло поле данных в пользовательском интерфейсе.
                            if (notifyCharacteristic != null) {
                                bluetoothLEService.setCharacteristicNotification(notifyCharacteristic, false);
                                notifyCharacteristic = null;
                            }
                            bluetoothLEService.readCharacteristic(characteristic);
                        }
                        if ((charaProp | BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0 ) {
                            notifyCharacteristic = characteristic;
                            bluetoothLEService.setCharacteristicNotification(characteristic, true);
                        }
                        return true;
                    }
                    return false;
                }
            };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.gatt_services_characteristics);

        final Intent intent = getIntent();
        deviceName = intent.getStringExtra(EXTRAS_DEVICE_NAME);
        deviceAddress = intent.getStringExtra(EXTRAS_DEVICE_ADDRESS);

        // Устанавливает ссылки на пользовательский интерфейс.
        ((TextView) findViewById(R.id.device_address)).setText(deviceAddress);
        gattServicesList = (ExpandableListView) findViewById(R.id.gatt_services_list);
        gattServicesList.setOnChildClickListener(servicesListListener);
        connectionStatus = findViewById(R.id.connection_state);
        dataField = findViewById(R.id.data_value);

        getActionBar().setTitle(deviceName);
        getActionBar().setDisplayHomeAsUpEnabled(true);

        Intent gattServiceIntent = new Intent(this, BluetoothLEService.class);

        // BIND_AUTO_CREATE, означающий, что, если сервис,
        // к которому мы пытаемся подключиться, не работает, то он будет запущен
        bindService(gattServiceIntent, serviceConnection, BIND_AUTO_CREATE);
    }

    @Override
    protected  void onResume() {
        super.onResume();

        // registerReceiver - регистрация приемника BroadcastReceiver
        registerReceiver(gattUpdateReceiver, makeGattUpdateIntentFilter());
        if (bluetoothLEService != null) {
            final boolean result = bluetoothLEService.connect(deviceAddress);
            Log.d(TAG, "Connect request result = " + result);
        }
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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.gatt_services, menu);
        if (connected) {
            menu.findItem(R.id.menu_connect).setVisible(false);
            menu.findItem(R.id.menu_disconnect).setVisible(true);
        } else {
            menu.findItem(R.id.menu_connect).setVisible(true);
            menu.findItem(R.id.menu_disconnect).setVisible(false);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_connect:
                bluetoothLEService.connect(deviceAddress);
                return true;
            case R.id.menu_disconnect:
                bluetoothLEService.disconnect();
                return true;
            case android.R.id.home:
                onBackPressed();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }








    private void displayData(String data) {
        if ( data != null) dataField.setText(data);
    }





    private void displayGattServices(List<BluetoothGattService> bluetoothGattServices) {
        if (bluetoothGattServices == null) return;
        String uuid = null;
        String unknownServiceString = getResources().getString(R.string.unknown_service);
        String unknownCharacteristicString = getResources().getString(R.string.unknown_characteristic);

        ArrayList<HashMap<String, String>> gattServiceData = new ArrayList<HashMap<String, String>>();

        ArrayList<ArrayList<HashMap<String, String>>> gattCharacteristicData =
                new ArrayList<ArrayList<HashMap<String, String>>>();

        mGattCharacteristics = new ArrayList<ArrayList<BluetoothGattCharacteristic>>();

        //Переборка доступных служб GATT.
        for (BluetoothGattService gattService : bluetoothGattServices) {
            HashMap<String, String> currentServiceData = new HashMap<String, String>();
            uuid = gattService.getUuid().toString();
            currentServiceData.put(LIST_NAME, SampleGattAttributes.lookup(uuid, unknownServiceString));
            currentServiceData.put(LIST_UUID, uuid);
            gattServiceData.add(currentServiceData);

            ArrayList<HashMap<String, String>> gattCharacteristicGroupData = new ArrayList<>();
            List<BluetoothGattCharacteristic> gattCharacteristics = gattService.getCharacteristics();
            ArrayList<BluetoothGattCharacteristic> characteristics = new ArrayList<>();

            //Цикл по доступным характеристикам.
            for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
                characteristics.add(gattCharacteristic);
                HashMap<String, String> currentCharaData = new HashMap<>();
                uuid = gattCharacteristic.getUuid().toString();
                currentCharaData.put(LIST_NAME, SampleGattAttributes.lookup(uuid, unknownCharacteristicString));
                currentCharaData.put(LIST_UUID, uuid);
                gattCharacteristicGroupData.add(currentCharaData);
            }
            mGattCharacteristics.add(characteristics);
            gattCharacteristicData.add(gattCharacteristicGroupData);
        }

        SimpleExpandableListAdapter gattServiceAdapter = new SimpleExpandableListAdapter(
                this,
                gattServiceData,
                android.R.layout.simple_expandable_list_item_2,
                new String[] {LIST_NAME, LIST_UUID},
                new int [] {android.R.id.text1, android.R.id.text2},
                gattCharacteristicData,
                android.R.layout.simple_expandable_list_item_2,
                new String[] {LIST_NAME, LIST_UUID},
                new int[]{android.R.id.text1, android.R.id.text2}
                );

        gattServicesList.setAdapter(gattServiceAdapter);

    }

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLEService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLEService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLEService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothLEService.ACTION_DATA_AVAILABLE);
        return intentFilter;
    }

    private void clearUI() {
        gattServicesList.setAdapter((SimpleExpandableListAdapter)null);
        dataField.setText(R.string.no_data);
    }

    private void updateConnectionState(final int resourseId) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                connectionStatus.setText(resourseId);
            }
        });
    }
}
