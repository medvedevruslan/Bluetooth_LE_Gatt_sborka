package com.example.bluetooth_le_gatt_sborka;

import android.app.Activity;
import android.app.ListActivity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
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


/**
 * Для сканирования и отображения доступных устройств Bluetooth LE.
 */
public class DeviceScanActivity extends ListActivity {

    public static final String TAG = "Medvedev DSA " + DeviceScanActivity.class.getSimpleName();
    private BleDevicesListAdapter bleDevicesListAdapter;
    private BluetoothAdapter bluetoothAdapter;
    private boolean checkScanning;
    private Handler handler;

    private static final int REQUEST_ENABLE_BT = 1;

    private static final long SCAN_PERIOD = 10000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getActionBar().setTitle("Scan Bluetooth Low Energy Devices");
        handler = new Handler();

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
        final BluetoothManager bluetoothManager = (BluetoothManager)getSystemService(Context.BLUETOOTH_SERVICE);
        Log.d(TAG, "Подключение к блютуз менеджеру");
        bluetoothAdapter = bluetoothManager.getAdapter();

        //Проверяет, поддерживается ли Bluetooth на устройстве.
        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth not supported", Toast.LENGTH_SHORT).show();
            finish();
            return;
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
        super.onResume();

        //Убедитесь, что на устройстве включен Bluetooth. Если Bluetooth в настоящее время не включен,
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

        // Пользователь решил не включать Bluetooth
        if (requestCode == REQUEST_ENABLE_BT && resultCode == Activity.RESULT_CANCELED) {
            finish();;
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
        final Intent intent = new Intent(this, DeviceControlActivity.class);
        intent.putExtra(DeviceControlActivity.EXTRAS_DEVICE_NAME, device.getName());
        intent.putExtra(DeviceControlActivity.EXTRAS_DEVICE_ADDRESS, device.getAddress());
        if (checkScanning) {
            //останавливается поиск
            bluetoothAdapter.stopLeScan(lowEnergyScanCallback);
            checkScanning = false;
        }
        startActivity(intent);
    }

    private void scanLowEnergyDevice(final boolean enable) {
        Log.d(TAG, "идет поиск устройств");
        if (enable) {
            // Останавливает сканирование по истечении заданного периода сканирования.
            // PostDelayed - задержка по времени
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    checkScanning = false;
                    bluetoothAdapter.stopLeScan(lowEnergyScanCallback);
                    invalidateOptionsMenu();
                }
            }, SCAN_PERIOD);

            checkScanning = true;
            bluetoothAdapter.startLeScan(lowEnergyScanCallback);
        } else {
            checkScanning = false;
            bluetoothAdapter.stopLeScan(lowEnergyScanCallback);
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
            Log.d(TAG, String.valueOf(DeviceScanActivity.this.getLayoutInflater()));
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

    private BluetoothAdapter.LeScanCallback lowEnergyScanCallback = new BluetoothAdapter.LeScanCallback() {
        @Override
        public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    bleDevicesListAdapter.addDeviceToList(device);

                    //уведомляет прикрепленных наблюдателей, что базовые данные были изменены,
                    //и любое представление, отражающее набор данных, должно обновиться.
                    bleDevicesListAdapter.notifyDataSetChanged();
                }
            });
        }
    };

    static class ViewHolder {
        TextView deviceName;
        TextView deviceAddress;
    }
}