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

import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.UUID;

/**
 * Служба для управления подключением и передачей данных с сервером GATT, размещенным на данном устройстве Bluetooth LE.
 */
public class BluetoothLEService extends Service {

    public final static String
            ACTION_GATT_CONNECTED = "com.example.bluetooth_le_gatt_sborka.ACTION_GATT_CONNECTED",
            ACTION_GATT_DISCONNECTED = "com.example.bluetooth_le_gatt_sborka.ACTION_GATT_DISCONNECTED",
            ACTION_GATT_SERVICES_DISCOVERED = "com.example.bluetooth_le_gatt_sborka.ACTION_GATT_SERVICES_DISCOVERED",
            ACTION_DATA_AVAILABLE = "com.example.bluetooth_le_gatt_sborka.ACTION_DATA_AVAILABLE",
            EXTRA_DATA = "com.example.bluetooth_le_gatt_sborka.ACTION_DATA";
    /**
     * UUID для прибора сердечного ритма
     */
    public final static UUID UUID_HEART_RATE_MEASUREMENT = UUID.fromString(SampleGattAttributes.HEART_RATE_MEASUREMENT);
    public final static UUID BLOOD_PRESSURE_MEASUREMENT = UUID.fromString(SampleGattAttributes.BLOOD_PRESSURE_MEASUREMENT);
    private final static String TAG = "Medvedev1 BLES " + BluetoothLEService.class.getSimpleName();
    private static final int
            STATE_DISCONNECTED = 0,
            STATE_CONNECTING = 1,
            STATE_CONNECTED = 2;
    private final IBinder binder = new LocalBinder();
    protected BluetoothGatt bluetoothGatt;
    private BluetoothManager bluetoothManager;
    private BluetoothAdapter bluetoothAdapter;
    private String bluetoothDeviceAddress;
    private int connectionStatus = STATE_DISCONNECTED;
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
                // Log.d(TAG, "Connected to GATT server");
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

                if (bluetoothGatt.getDevice().getAddress().equals("40:BD:32:A0:6E:D6")) { // testo smart
                    connectionWithTestoSmartPyrometer(bluetoothGatt);





                } else if (bluetoothGatt.getDevice().getAddress().equals("18:7A:93:BC:6D:80")) { // microlife nc 150bt

                    //заметка себе: вытащить логику в отдельный метод

                    BluetoothGattCharacteristic BtGattCharacteristic =
                            bluetoothGatt.getService(UUID.fromString("0000fff0-0000-1000-8000-00805f9b34fb"))
                                    .getCharacteristic(UUID.fromString("0000fff1-0000-1000-8000-00805f9b34fb"));

                    if (setCharacteristicNotification(BtGattCharacteristic, true)) {
                        Log.d(TAG, "Notifications/indications FFF1 successfully enabled!");
                        readCharacteristic(BtGattCharacteristic);
                        Log.d(TAG, "readCharacteristicAfterNotification " + BtGattCharacteristic + " | " + Arrays.toString(BtGattCharacteristic.getValue()));

                    } else
                        Log.d(TAG, "Notifications/indications FFF1 enabling error!");





                } else if (bluetoothGatt.getDevice().getAddress().equals("34:14:B5:B1:30:C3")) { // manometer AND
                    if (setIndication(bluetoothGatt, true))
                        Log.d(TAG, "indication enable");
                    else
                        Log.d(TAG, "indication NOT enable");
                    connectionWithManometer(bluetoothGatt);
                }


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
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            Log.d(TAG, "onCharacteristicWrite == BluetoothGatt：" + bluetoothGatt + " BluetoothGattCharacteristic：" + characteristic + " status：" + status);
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
            int format;
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
            Log.d(TAG, "oncharacteristicChanged | " + characteristic.getUuid().toString() + " | " + Arrays.toString(characteristic.getValue()));

            final byte[] data = characteristic.getValue();
            if (data != null && data.length > 0) {
                if (BLOOD_PRESSURE_MEASUREMENT.equals(characteristic.getUuid())) {
                    Log.d(TAG, "замеры манометром: SYS: " + data[1] + ". DYA: " + data[3] + ". PULSE: " + data[14]);
                }

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
        //Log.d(TAG, "Попытка создать новое соединение");
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

    /**
     * Включает или отключает уведомление о заданной характеристике.
     *
     * @param characteristic характеристики
     * @param enabled        Если true, включить уведомление
     * @return true, если включение/отключение завершилось успешно
     */
    public boolean setCharacteristicNotification(BluetoothGattCharacteristic characteristic, boolean enabled) {
        if (bluetoothAdapter == null || bluetoothGatt == null) {
            Log.d(TAG, "BluetoothAdapter не инициализирован");
            return false;
        }
        // Это относится к измерению частоты пульса.
//        if (UUID_HEART_RATE_MEASUREMENT.equals(characteristic.getUuid())) {
//            BluetoothGattDescriptor descriptor = characteristic.getDescriptor(
//                    UUID.fromString(SampleGattAttributes.CLIENT_CHARACTERISTIC_CONFIG));
//            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
//            bluetoothGatt.writeDescriptor(descriptor);
//        }
        return bluetoothGatt.setCharacteristicNotification(characteristic, enabled);
    }

    public List<BluetoothGattService> getSupportedGattServices() {
        if (bluetoothGatt == null) return null;
        return bluetoothGatt.getServices();
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

    public class LocalBinder extends Binder {
        BluetoothLEService getService() {
            return BluetoothLEService.this;
        }
    }

    // /**
    //  * Функция отправки значения характеристики по UUID
    //  *
    //  * @param gatt GATT соединённого устройства
    //  */
    // private void sendValue(BluetoothGatt gatt) {
    //     if (gatt == null) {
    //         Log.e("Error", "BluetoothAdapter not initialized!");
    //         return;
    //     }
    //     BluetoothGattCharacteristic characteristic = gatt.getService(UUID.fromString("0000fff0-0000-1000-8000-00805f9b34fb")).getCharacteristic(UUID.fromString("0000fff2-0000-1000-8000-00805f9b34fb"));
    //     if (characteristic == null) {
    //         Log.e("Error", "Can't get characteristic!");
    //         return;
    //     }
    //     byte[] command = new byte[]{};
    //     characteristic.setValue(command);
    //
    //     if (!gatt.writeCharacteristic(characteristic)) {
    //         Log.e(TAG, String.format("ERROR: writeCharacteristic failed for characteristic: %s", characteristic.getUuid()));
    //         Toast.makeText(getApplicationContext(), String.format("Ошибка записи значения характеристики %s!", characteristic.getUuid()), Toast.LENGTH_SHORT).show();
    //     } else {
    //         Log.d(TAG, String.format("Writing <%s> to characteristic <%s>", Arrays.toString(command), characteristic.getUuid()));
    //     }
    // }

    /**
     * Функция попытки подключения к уст-ву [testo 805i или ???].
     * <p>Записывает значение "01-00" в дескриптор 00002902-0000-1000-8000-00805f9b34fb</p>
     */
    public void setNotification(BluetoothGattCharacteristic characteristic, boolean enabled) {
        // заметки себе: принимать не характеристику, а BluetoothGatt, чтобы вся логика по извлечению характеристики проходила здесь, а не загромождала там
        if (bluetoothGatt == null) {
            Log.e("ConnectionAttempt", "BluetoothGatt is null!");
            return;
        }
        if (characteristic == null) {
            Log.e("ConnectionAttempt", "Can't get characteristic!");
            return;
        }
        if (setCharacteristicNotification(characteristic, enabled)) {
            Log.d(TAG, "Notifications/indications fff2 successfully enabled!");
        } else {
            Log.e(TAG, "Notifications/indications fff2 enabling error!");
            return;
        }

        BluetoothGattDescriptor descriptor = characteristic.getDescriptor(UUID.fromString(SampleGattAttributes.CLIENT_CHARACTERISTIC_CONFIG));
        if (descriptor != null) {

            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            if (bluetoothGatt.writeDescriptor(descriptor))
                Log.d(TAG, "Successfully writing a value to a descriptor 2902!");
            else Log.e(TAG, "Error writing value to descriptor 2902!");

        } else Log.e(TAG, "Descriptor 2902 is null!");
    }

    private void connectionWithManometer(BluetoothGatt bluetoothGatt) {
        BluetoothGattCharacteristic dateAndTimeCharacteristic =
                bluetoothGatt.getService(UUID.fromString(SampleGattAttributes.BLOOD_PRESSURE_SERVICE))
                        .getCharacteristic(UUID.fromString(SampleGattAttributes.BLOOD_PRESSURE_MEASUREMENT));
        bluetoothGatt.writeCharacteristic(setDateAndTimeValueToCharacteristic(dateAndTimeCharacteristic, Calendar.getInstance()));
    }

    public boolean setIndication(BluetoothGatt bluetoothGatt, boolean enable) {
        boolean isSuccess = false;
        if (bluetoothGatt != null) {
            BluetoothGattService service = bluetoothGatt.getService(UUID.fromString(SampleGattAttributes.BLOOD_PRESSURE_SERVICE));
            if (service != null) {
                BluetoothGattCharacteristic characteristic = service.getCharacteristic(UUID.fromString(SampleGattAttributes.BLOOD_PRESSURE_MEASUREMENT));
                if (characteristic != null) {
                    isSuccess = bluetoothGatt.setCharacteristicNotification(characteristic, enable);
                    BluetoothGattDescriptor descriptor = characteristic.getDescriptor(UUID.fromString(SampleGattAttributes.CLIENT_CHARACTERISTIC_CONFIG));
                    if (enable) {
                        descriptor.setValue(BluetoothGattDescriptor.ENABLE_INDICATION_VALUE);
                    } else {
                        descriptor.setValue(BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE);
                    }
                    bluetoothGatt.writeDescriptor(descriptor);
                } else {
                    Log.d(TAG, "Characteristic NULL");
                }
            } else {
                Log.d(TAG, "Service NULL");
            }
        }
        return isSuccess;
    }

    public static BluetoothGattCharacteristic setDateAndTimeValueToCharacteristic(BluetoothGattCharacteristic characteristic, Calendar calendar) {
        int year = calendar.get(1); //год

        characteristic.setValue(new byte[]{
                (byte) (year & 255),
                (byte) (year >> 8),
                (byte) (calendar.get(2) + 1), // месяц
                (byte) calendar.get(5), // день
                (byte) calendar.get(11), // часы
                (byte) calendar.get(12), // минуты
                (byte) calendar.get(13) // секунды
        });
        return characteristic;
    }

    public void connectionWithTestoSmartPyrometer(BluetoothGatt bluetoothGatt) {
        /*



0)fff2 notification +++

1) to fff1
5600030000000c69023e81 +++

1.1) from fff2
07000000000001ac (Без реакции на смартфоне)







проверить можно ли без этих запросов {


3) to fff1 (запрос версии прошивки)
200000000000077b

4) from fff2 (возврат версиии прошивки)
070004000000009c01000020

5) to fff1
04001500000005930f0000004669726d77617265
56657273696f6e304f (запрос версии прошивки)

a1) from fff2
07001200000004d40c0000003030312e3130312e
303030314409 (возврат версии прошивки)

6) to fff1
110000000000035a (battery Level)

6.1) from fff2
07000000000001ac

потом приходит значение температуры в 2х пакетах с определенной периодичностью, пример:
108016000000071d0c000000426174746572794c
6576656ca2c9b8423206


}






7) to fff1  (material)
05001a0000000756100000004d6174657269616c

8) to fff1 (emission)
456d697373696f6e3333733f0471

8.1) from fff2
07000000000001ac

9) from fff2 (после нажатия на кнопку на устройстве)
108012000000062d0b000000427574746f6e436c
69636b01fae8

10) значения температур по fff2 в 2х пакетах





        if (setCharacteristicNotification(bluetoothGatt.getService(UUID.fromString("00001801-0000-1000-8000-00805f9b34fb")).getCharacteristic(UUID.fromString("00002a05-0000-1000-8000-00805f9b34fb")), true)) {
            Log.d(TAG, "Notifications/indications 2A05 successfully enabled!");
            // постановка на уведомление и изменение дескриптора
            setNotification(bluetoothGatt.getService(UUID.fromString("0000fff0-0000-1000-8000-00805f9b34fb")).getCharacteristic(UUID.fromString("0000fff2-0000-1000-8000-00805f9b34fb")), true);
        } else
            Log.e("ConnectionAttempt0", "Notifications/indications 2A05 enabling error!");

*/
    }

}
