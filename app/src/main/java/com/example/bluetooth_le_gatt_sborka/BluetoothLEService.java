package com.example.bluetooth_le_gatt_sborka;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import java.util.List;
import java.util.UUID;


/**
 * Служба для управления подключением и передачей данных с сервером GATT, размещенным на данном устройстве Bluetooth LE.
 */
public class BluetoothLEService extends Service {

    private final static String TAG = "Medvedev BLES " + BluetoothLEService.class.getSimpleName();

    private BluetoothManager bluetoothManager;
    private BluetoothAdapter bluetoothAdapter;
    private String bluetoothDeviceAddress;
    protected BluetoothGatt bluetoothGatt;
    private int connectionStatus = STATE_DISCONNECTED;

    private static final int STATE_DISCONNECTED = 0;
    private static final int STATE_CONNECTING = 1;
    private static final int STATE_CONNECTED = 2;

    public final static String ACTION_GATT_CONNECTED = "com.example.bluetooth_le_gatt_sborka.ACTION_GATT_CONNECTED";
    public final static String ACTION_GATT_DISCONNECTED = "com.example.bluetooth_le_gatt_sborka.ACTION_GATT_DISCONNECTED";
    public final static String ACTION_GATT_SERVICES_DISCOVERED =
            "com.example.bluetooth_le_gatt_sborka.ACTION_GATT_SERVICES_DISCOVERED";
    public final static String ACTION_DATA_AVAILABLE = "com.example.bluetooth_le_gatt_sborka.ACTION_DATA_AVAILABLE";
    public final static String EXTRA_DATA = "com.example.bluetooth_le_gatt_sborka.ACTION_DATA";

    //UUID для прибора сердечного ритма
    public final static UUID UUID_HEART_RATE_MEASUREMENT = UUID.fromString(SampleGattAttributes.HEART_RATE_MEASUREMENT);

    //Реализация методов обратного вызова для событий GATT,
    //о которых заботится приложение. Например, изменение подключения и обнаружение служб.
    public final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {

        @Override
        //при изменении состояния подключения
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            String intentAction;
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                intentAction = ACTION_GATT_CONNECTED;
                connectionStatus = STATE_CONNECTED;
                broadcastUpdate(intentAction);
                Log.d(TAG, "Connected to GATT server");
                //Попытки обнаружить службы после успешного подключения.
                //discoverServices - Обнаруживает сервисы, предлагаемые удаленным устройством,
                // а также их характеристики и дескрипторы
                Log.d(TAG, "Попытка запустить обнаружение сервисов: " + bluetoothGatt.discoverServices());
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                intentAction = ACTION_GATT_DISCONNECTED;
                connectionStatus = STATE_DISCONNECTED;
                Log.d(TAG, "Disconnected from GATT server");
                broadcastUpdate(intentAction);
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt bluetoothGatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED);
            } else {
                Log.d(TAG, "об обнаруженных услугах получено: " + status);
            }
        }

        // обратный вызов докладывающий об рузельтате чтения зарактеристик ble
        @Override
        public void onCharacteristicRead(BluetoothGatt bluetoothGatt,
                                         BluetoothGattCharacteristic characteristic, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
            }
        }


        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt,
                                          BluetoothGattCharacteristic characteristic, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS)
                broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
        }

        // обратный вызов об изменении характеристик ble
        @Override
        public void onCharacteristicChanged(BluetoothGatt bluetoothGatt, BluetoothGattCharacteristic characteristic) {
            broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
        }
    };

    private void broadcastUpdate(final String action) {
        final Intent intent = new Intent(action);
        sendBroadcast(intent);
    }

    private void broadcastUpdate(final String action, final BluetoothGattCharacteristic characteristic) {
        final Intent intent = new Intent(action);

        //Это особая обработка профиля измерения частоты пульса.
        // Анализ данных выполняется в соответствии со спецификациями профиля:
        // http: developer.bluetooth.orggattcharacteristicsPagesCharacteristicViewer.aspx? U = org.bluetooth.characteristic.heart_rate_measurement.xml
        if (UUID_HEART_RATE_MEASUREMENT.equals(characteristic.getUuid())) {
            int flag = characteristic.getProperties();
            int format = -1;
            if ((flag & 0x01) != 0) {
                format = BluetoothGattCharacteristic.FORMAT_UINT16;
                Log.d(TAG, "Heart rate format UINT16.");
            } else {
                format = BluetoothGattCharacteristic.FORMAT_UINT8;
                Log.d(TAG, "Heart rate format UINT8.");
            }

            final int heartRate = characteristic.getIntValue(format, 1);
            Log.d(TAG, "properties from Heart Rate: " + characteristic + " | " + String.format("Received heartrate: %d", heartRate));
            intent.putExtra(EXTRA_DATA, String.valueOf(heartRate));

        } else {
            //Для всех остальных профилей записывает данные в формате HEX
            final byte[] data = characteristic.getValue();
            if (data != null && data.length > 0) {
                final StringBuilder stringBuilder = new StringBuilder(data.length);
                for (byte byteChar : data)
                    stringBuilder.append(String.format("%02X ", byteChar));
                intent.putExtra(EXTRA_DATA, new String(data) + "\n" + stringBuilder.toString());
            }
        }
        sendBroadcast(intent);
    }


    public boolean initialize() {
        // Для уровня API 18 и выше получите ссылку на BluetoothAdapter
        if (bluetoothManager == null) {
            bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            if (bluetoothManager == null) {
                Log.d(TAG, "Невозможно инициализировать BluetoothManager");
                return false;
            }
        }

        bluetoothAdapter = bluetoothManager.getAdapter();
        if (bluetoothAdapter == null) {
            Log.d(TAG, "Невозможно получить адаптер Bluetooth");
            return false;
        }
        return true;
    }

    /**
     * Подключается к серверу GATT, размещенному на устройстве Bluetooth LE.
     *
     * @param deviceAddress Адрес устройства назначения.
     * @return Возвращает истину, если соединение инициировано успешно.
     * Результат подключения передается асинхроннос помощью обратного
     * вызова {@code BluetoothGattCallback#onConnectionStateChange (android.bluetooth.BluetoothGatt, int, int)}.
     */
    public boolean connect(final String deviceAddress) {
        if (bluetoothAdapter == null || deviceAddress == null) {
            Log.d(TAG, "BluetoothAdapter не инициализирован или адрес не указан");
            return false;
        }

        //Ранее подключенное устройство. Попытка переподключиться.
        if (bluetoothDeviceAddress != null && deviceAddress.equals(bluetoothDeviceAddress)
                && bluetoothGatt != null) {
            Log.d(TAG, "Попытка использовать существующий mBluetoothGatt для подключения.");
            if (bluetoothGatt.connect()) {
                connectionStatus = STATE_CONNECTING;
                return true;
            } else {
                return false;
            }
        }

        final BluetoothDevice bluetoothDevice = bluetoothAdapter.getRemoteDevice(deviceAddress);
        if (bluetoothDevice == null) {
            Log.d(TAG, "устройство не найдено. Невозможно подключиться");
            return false;
        }

        // Мы хотим напрямую подключиться к устройству,
        // поэтому устанавливаем для параметра autoConnect значение false.
        bluetoothGatt = bluetoothDevice.connectGatt(this, false, gattCallback);
        Log.d(TAG, "Попытка создать новое соединение");
        bluetoothDeviceAddress = deviceAddress;
        connectionStatus = STATE_CONNECTING;

        return true;
    }


    /**
     * Отключает существующее соединение или отменяет ожидающее соединение.
     * Результат отключения передается асинхронно через обратный вызов
     */
    public void disconnect() {
        if (bluetoothAdapter == null || bluetoothGatt == null) {
            Log.d(TAG, "BluetoothAdapter не инициализирован");
            return;
        }
        bluetoothGatt.disconnect();
    }

    public void readCharacteristic(BluetoothGattCharacteristic characteristic) {
        if (bluetoothAdapter == null || bluetoothGatt == null) {
            Log.d(TAG, "BluetoothAdapter не инициализирован");
            return;
        }
        bluetoothGatt.readCharacteristic(characteristic);
    }


/*    public boolean writecharacterisctic() {
        bluetoothGatt.writeCharacteristic();
        return true;
    }*/

    /**
     * Включает или отключает уведомление о заданной характеристике.
     *
     * @param characteristic характеристики
     * @param enabled        Если true, включить уведомление
     */
    public void setCharacteristicNotification(BluetoothGattCharacteristic characteristic, boolean enabled) {
        if (bluetoothAdapter == null || bluetoothGatt == null) {
            Log.d(TAG, "BluetoothAdapter не инициализирован");
            return;
        }
        bluetoothGatt.setCharacteristicNotification(characteristic, enabled);

        // Это относится к измерению частоты пульса.

        if (UUID_HEART_RATE_MEASUREMENT.equals(characteristic.getUuid())) {
            BluetoothGattDescriptor descriptor = characteristic.getDescriptor(
                    UUID.fromString(SampleGattAttributes.CLIENT_CHARACTERISTIC_CONFIG));
            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            bluetoothGatt.writeDescriptor(descriptor);
        }
    }

    public List<BluetoothGattService> getSupportedGattServices() {
        if (bluetoothGatt == null) return null;
        return bluetoothGatt.getServices();
    }


    public class LocalBinder extends Binder {
        BluetoothLEService getService() {
            return BluetoothLEService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        //После использования данного устройства вы должны убедиться, что вызывается BluetoothGatt.close (),
        // чтобы ресурсы были очищены должным образом.
        // В этом конкретном примере close () вызывается, когда пользовательский интерфейс отключается от службы.
        close();
        return super.onUnbind(intent);
    }


    /**
     * После использования данного устройства BLE приложение должно вызвать этот метод,
     * чтобы обеспечить правильное высвобождение ресурсов.
     */
    private void close() {
        if (bluetoothGatt == null) {
            return;
        }
        bluetoothGatt.close();
        bluetoothGatt = null;
    }

    private final IBinder binder = new LocalBinder();

}
