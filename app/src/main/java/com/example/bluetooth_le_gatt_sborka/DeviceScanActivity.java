package com.example.bluetooth_le_gatt_sborka;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.ListActivity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.ParcelUuid;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;


/**
 * Для сканирования и отображения доступных устройств Bluetooth LE.
 */
public class DeviceScanActivity extends ListActivity {

    public static final String TAG = "Medvedev DSA " + DeviceScanActivity.class.getSimpleName();
    private BleDevicesListAdapter bleDevicesListAdapter;
    private BluetoothAdapter bluetoothAdapter;
    private boolean checkScanning;
    private Handler handler;
    int count;
    private int permissionCheck;

    BluetoothLeScanner bluetoothLeScanner;
    ScanSettings bleScanSettings = null;


    private static final int REQUEST_ENABLE_BT = 1;

    private static final long SCAN_PERIOD = 10000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // getActionBar().setTitle(R.string.title_devices);
        getActionBar().setTitle("Scan Bluetooth Low Energy Devices");
        handler = new Handler();
        Log.d(TAG, "onCreate");
        //Используйте эту проверку, чтобы определить, поддерживается ли BLE на устройстве.
        // Затем вы можете выборочно отключить функции, связанные с BLE.
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, "Bluetooth Low Energy не поддерживается на данном устройстве",
                    Toast.LENGTH_SHORT).show();
            finish();
            return;
        } else {
            Log.d(TAG, "Bluetooth Low Energy поддерживается на данном устройстве");
        }


        //Инициализирует адаптер Bluetooth.
        // Для уровня API 18 и выше получите ссылку на BluetoothAdapter через BluetoothManager
        final BluetoothManager bluetoothManager = (BluetoothManager)getSystemService(Context.BLUETOOTH_SERVICE);
        Log.d(TAG, "Подключение к блютуз менеджеру");
        bluetoothAdapter = bluetoothManager.getAdapter();

        //Проверяет, поддерживается ли Bluetooth на устройстве.
        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth not supported", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        if (Build.VERSION.SDK_INT>=23) {
            checkLocationPermission();
        }
    }

    public void checkLocationPermission() {
        111

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        Log.d(TAG, "onCreateOptionsMenu");
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

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        Log.d(TAG, "onOptionsItemSelected");
        switch (menuItem.getItemId()) {
            case R.id.menu_scan:
                bleDevicesListAdapter.clearList();
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
        Log.d(TAG, "onResume");

        super.onResume();

        // Убедитесь, что на устройстве включен Bluetooth. Если Bluetooth в настоящее время не включен,
        // активируйте намерение отобразить диалоговое окно с просьбой предоставить пользователю разрешение
        // на его включение
        if (!bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }

        // Инициализирует адаптер представления списка.
        // поиск устройств при запуске приложения, а конкретно когда запускается onResume
        bleDevicesListAdapter = new BleDevicesListAdapter();
        setListAdapter(bleDevicesListAdapter);
        scanLowEnergyDevice(true);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d(TAG, "onActivityResult");
        // Пользователь решил не включать Bluetooth
        if (requestCode == REQUEST_ENABLE_BT && resultCode == Activity.RESULT_CANCELED) {
            finish();;
            return;
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPause");
        scanLowEnergyDevice(false);
        bleDevicesListAdapter.clearList();
    }

    @Override
    protected void onListItemClick(ListView listView, View view, int position, long id) {
        final BluetoothDevice device = bleDevicesListAdapter.getDeviceByPosition(position);

        Log.d(TAG, "onListItemClick getDeviceByPosition " + position);

        if (device == null) return;
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
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    checkScanning = false;
                    startOrStopScanBle("stop");
                    Log.d(TAG, " scanLowEnergyDevice.stopLeScan" + check);
                    // invalidateOptionsMenu - перерисовка ActionBar
                    invalidateOptionsMenu();
                }
            }, SCAN_PERIOD);

            // отсчет времени в логах посекундно
            Thread threadStopwatch10Seconds = new Thread(new Runnable() {
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
            });threadStopwatch10Seconds.start();


            checkScanning = true;
            startOrStopScanBle("start");
            Log.d(TAG, " scanLowEnergyDevice.startLeScan" + check);

        } else {
            checkScanning = false;
            startOrStopScanBle("stop");
            Log.d(TAG, " scanLowEnergyDevice.stopLeScan" + check + "№2");

        }
        invalidateOptionsMenu();
    }

    // Адаптер для устройств, обнаруженный при сканировании.
    private class BleDevicesListAdapter extends BaseAdapter {

        //список обнаруженных BLE устройств
        private ArrayList<BluetoothDevice> lowEnergyDevices;

        //наполнитель layout`a
        private LayoutInflater layoutInflater;

        //метод для нахождения layout`a в классе
        public BleDevicesListAdapter() {
            super();
            Log.d(TAG, "создание BleDevicesListAdapter");
            lowEnergyDevices = new ArrayList<BluetoothDevice>();
            layoutInflater = DeviceScanActivity.this.getLayoutInflater();
            Log.d(TAG, String.valueOf(DeviceScanActivity.this.getLayoutInflater()) + " BleDevicesListAdapter создан");
        }

        public void addDeviceToList(BluetoothDevice device) {

            //если данный девайс не находится в списке девайсов, то добавляем его
            if (!lowEnergyDevices.contains(device)) {
                lowEnergyDevices.add(device);
            }

            Log.d(TAG, String.valueOf(device));

        }

        public BluetoothDevice getDeviceByPosition(int position) {
            return lowEnergyDevices.get(position);
        }

        public void clearList() {
            Log.d(TAG, "size ArrayList ble devices = " + lowEnergyDevices.size());
            Log.d(TAG, "clearList");
            lowEnergyDevices.clear();
        }

        @Override
        public int getCount() {
            return lowEnergyDevices.size();
        }

        @Override
        public Object getItem(int position) {
            return lowEnergyDevices.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder viewHolder;

            Log.d(TAG, "gitView");


            //Общий код оптимизации ListView.
            if (convertView == null) {
                convertView = layoutInflater.inflate(R.layout.listitem_device, null);
                viewHolder = new ViewHolder();
                viewHolder.deviceAddress = convertView.findViewById(R.id.device_address);
                viewHolder.deviceName = convertView.findViewById(R.id.device_name);
                convertView.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder)convertView.getTag();
            }

            BluetoothDevice device = lowEnergyDevices.get(position);
            final String deviceName = device.getName();
            if (deviceName != null && deviceName.length() > 0)
                viewHolder.deviceName.setText(deviceName);
            else
                viewHolder.deviceName.setText(R.string.unknown_device);

            viewHolder.deviceAddress.setText(device.getAddress());

            return convertView;
        }
    }


    public void startOrStopScanBle(String stopOrStartTask) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && bluetoothAdapter != null) {
            Log.d(TAG, "VERSION.SDK_INT: " + String.valueOf(Build.VERSION.SDK_INT) + " check#1");
            if (bluetoothLeScanner == null) {
                bluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();
                bleScanSettings = new ScanSettings.Builder().setScanMode(2).build();
            }

            ScanCallback scanCallback = new ScanCallback() {



                @Override
                public void onScanResult(int callbackType, ScanResult scanResult) {
                    super.onScanResult(callbackType, scanResult);

                    if (scanResult.getDevice().getName() != null && scanResult.getDevice().getAddress() != null) {

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Log.d(TAG, "onScanResult. callbackType is :" + callbackType + ",name: " +
                                        scanResult.getDevice().getName() + ",address: " +
                                        scanResult.getDevice().getAddress() + ",rssi: " + scanResult.getRssi());

                                BluetoothDevice bluetoothDevice = scanResult.getDevice();
                                bleDevicesListAdapter.addDeviceToList(bluetoothDevice);

                                //уведомляет прикрепленных наблюдателей, что базовые данные были изменены,
                                //и любое представление, отражающее набор данных, должно обновиться.
                                bleDevicesListAdapter.notifyDataSetChanged();
                            }
                        });
                    }
                }



                @Override
                public void onBatchScanResults(List<ScanResult> results) {
                    super.onBatchScanResults(results);
                    Log.d(TAG, "onBatchScanResults");
                    for (ScanResult sr : results) {
                        Log.i("ScanResult - Results: ", sr.toString());
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
                    bluetoothLeScanner.startScan((List<ScanFilter>) null, bleScanSettings, scanCallback);
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
            Log.d(TAG, "VERSION.SDK_INT: " + String.valueOf(Build.VERSION.SDK_INT) + " check#2");

            if (stopOrStartTask.equals("start")) {
                bluetoothAdapter.startLeScan(lowEnergyScanCallback);
            } else if (stopOrStartTask.equals("stop")) {
                bluetoothAdapter.stopLeScan(lowEnergyScanCallback);
            }
        } else {
            Log.e(TAG, "bluetoothAdapter = null");
        }
    }


    static class ViewHolder {
        TextView deviceName;
        TextView deviceAddress;
    }

    // Device scan callback KITKAT and below.
    private BluetoothAdapter.LeScanCallback lowEnergyScanCallback = new BluetoothAdapter.LeScanCallback() {

        /**
         * @param device название устройства
         * @param rssi уровень сигнала, чем ниже значение, тем хуже сигнал
         * @param scanRecord Содержание записи advertising, предлагаемой удаленным устройством.
         */
        @Override
        public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
            Log.d(TAG, " onLeScan LeScanCallback lowEnergyScanCallback");
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Log.d(TAG, "adding device " + device);
                    bleDevicesListAdapter.addDeviceToList(device);
                    Log.d(TAG, "lowEnergyScanCallback");

                    //уведомляет прикрепленных наблюдателей, что базовые данные были изменены,
                    //и любое представление, отражающее набор данных, должно обновиться.
                    bleDevicesListAdapter.notifyDataSetChanged();
                }
            });
        }
    };
}