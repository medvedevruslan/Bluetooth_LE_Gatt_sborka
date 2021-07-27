package com.example.bluetooth_le_gatt_sborka;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
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
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
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

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;

/**
 * Для сканирования и отображения доступных устройств Bluetooth LE.
 */
public class DeviceScanActivity extends ListActivity {

    public static final String TAG = "Medvedev DSA " + DeviceScanActivity.class.getSimpleName();
    private static final int REQUEST_ENABLE_BT = 1;
    private static final long SCAN_PERIOD = 10000;
    int count;
    BluetoothLeScanner bluetoothLeScanner;
    ScanSettings bleScanSettings = null;
    List<ScanFilter> bleScanFilters = null;
    private BleDevicesListAdapter bleDevicesListAdapter;
    private BluetoothAdapter bluetoothAdapter;
    private boolean checkScanning;
    private Handler handler;
    private int permissionCheck;
    ArrayList<BluetoothDevice> notOpenAlertDialogToConnectThisDevices = new ArrayList<>();
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // getActionBar().setTitle(R.string.title_devices);
        getActionBar().setTitle("Scan Bluetooth Low Energy Devices");
        handler = new Handler(Looper.getMainLooper());

        //Используйте эту проверку, чтобы определить, поддерживается ли BLE на устройстве.
        // Затем вы можете выборочно отключить функции, связанные с BLE.
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, "Bluetooth Low Energy не поддерживается на данном устройстве",
                    Toast.LENGTH_SHORT).show();
            finish();
            return;
        }


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

        if (Build.VERSION.SDK_INT >= 23) {
            checkLocationPermission();
        }
    }

    public void checkLocationPermission() {
        permissionCheck = ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION);

        switch (permissionCheck) {
            case PackageManager.PERMISSION_GRANTED:
                break;

            case PackageManager.PERMISSION_DENIED:
                if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                        android.Manifest.permission.ACCESS_COARSE_LOCATION)) {
                    Toast.makeText(this, "Требуется доступ к местоположению, чтобы показывать " +
                            "устройства Bluetooth поблизости", Toast.LENGTH_SHORT).show();
                } else {
                    ActivityCompat.requestPermissions(this, new String[]{
                            Manifest.permission.ACCESS_COARSE_LOCATION}, 1);
                }
                break;
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

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        switch (menuItem.getItemId()) {
            case R.id.menu_scan:
                if (bleDevicesListAdapter != null) {
                    bleDevicesListAdapter.clearList();
                }
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
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }

        bleDevicesListAdapter = new BleDevicesListAdapter();
        setListAdapter(bleDevicesListAdapter);

        /*// Инициализирует адаптер представления списка.
        // поиск устройств при запуске приложения, а конкретно когда запускается onResume

        scanLowEnergyDevice(true);
        */
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // Пользователь решил не включать Bluetooth
        if (requestCode == REQUEST_ENABLE_BT && resultCode == Activity.RESULT_CANCELED) {
            finish();
            return;
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        scanLowEnergyDevice(false);
        bleDevicesListAdapter.clearList();
    }

    @Override
    protected void onListItemClick(ListView listView, View view, int position, long id) {
        final BluetoothDevice device = bleDevicesListAdapter.getDeviceByPosition(position);

        Log.d(TAG, "onListItemClick getDeviceByPosition " + position);
        if (device == null) return;
        connectWithDevice(device);
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
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    checkScanning = false;
                    startOrStopScanBle("stop");
                    Log.d(TAG, " scanLowEnergyDevice.stopLEScan: " + check);
                    // invalidateOptionsMenu - перерисовка ActionBar
                    invalidateOptionsMenu();
                }
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
                bleScanFilters = new ArrayList();
            }

            ScanCallback scanCallback = new ScanCallback() {
                @Override
                public void onScanResult(int callbackType, ScanResult scanResult) {
                    super.onScanResult(callbackType, scanResult);

                    if (scanResult.getDevice().getName() != null && scanResult.getDevice().getAddress() != null) {

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
                            bleDevicesListAdapter.addDeviceToList(bluetoothDevice);

                            //уведомляет прикрепленных наблюдателей, что базовые данные были изменены,
                            //и любое представление, отражающее набор данных, должно обновиться.
                            bleDevicesListAdapter.notifyDataSetChanged();
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
        AlertDialog.Builder quitDialog = new AlertDialog.Builder(DeviceScanActivity.this);
        quitDialog.setTitle("Подсоединиться к устройству: " + bluetoothDevice.getName() + " ?");

        quitDialog.setPositiveButton("Да!", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                connectWithDevice(bluetoothDevice);
            }
        });

        quitDialog.setNegativeButton("Нет", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
            }
        });
        quitDialog.show();
    }

    static class ViewHolder {
        TextView deviceName;
        TextView deviceAddress;
    }

    // Адаптер для устройств, обнаруженный при сканировании.
    private class BleDevicesListAdapter extends BaseAdapter {

        //список обнаруженных BLE устройств
        public final ArrayList<BluetoothDevice> lowEnergyDevices;

        //наполнитель layout`a
        private final LayoutInflater layoutInflater;

        //метод для нахождения layout`a в классе
        public BleDevicesListAdapter() {
            super();
            Log.d(TAG, "создание BleDevicesListAdapter");
            lowEnergyDevices = new ArrayList<BluetoothDevice>();
            layoutInflater = DeviceScanActivity.this.getLayoutInflater();
            Log.d(TAG, DeviceScanActivity.this.getLayoutInflater() + " BleDevicesListAdapter создан");
        }

        public void addDeviceToList(BluetoothDevice device) {

            //если данный девайс не находится в списке девайсов, то добавляем его
            if (!lowEnergyDevices.contains(device)) {
                lowEnergyDevices.add(device);
            }
        }

        public BluetoothDevice getDeviceByPosition(int position) {
            return lowEnergyDevices.get(position);
        }

        public void clearList() {
            Log.d(TAG, "clearList. size ArrayList ble devices = " + lowEnergyDevices.size());
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

            //Общий код оптимизации ListView.
            if (convertView == null) {
                convertView = layoutInflater.inflate(R.layout.listitem_device, null);
                viewHolder = new ViewHolder();
                viewHolder.deviceAddress = convertView.findViewById(R.id.device_address);
                viewHolder.deviceName = convertView.findViewById(R.id.device_name);
                convertView.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) convertView.getTag();
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
}