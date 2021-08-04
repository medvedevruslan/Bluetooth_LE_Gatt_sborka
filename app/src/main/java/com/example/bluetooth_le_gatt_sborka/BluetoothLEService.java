package com.example.bluetooth_le_gatt_sborka;

import static android.bluetooth.BluetoothProfile.STATE_CONNECTED;
import static android.bluetooth.BluetoothProfile.STATE_DISCONNECTED;

import static com.example.bluetooth_le_gatt_sborka.BluetoothService.MESSAGE_STATE_CHANGE;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;

import androidx.core.view.InputDeviceCompat;

import java.util.Arrays;
import java.util.Calendar;
import java.util.UUID;


/**
 * Служба для управления подключением и передачей данных с сервером GATT, размещенным на данном устройстве Bluetooth LE.
 */
public class BluetoothLEService extends Service {

    public final static String
            TAG = "Medvedev1 BLES",
            ACTION_GATT_CONNECTED = "com.example.bluetooth_le_gatt_sborka.ACTION_GATT_CONNECTED",
            ACTION_GATT_DISCONNECTED = "com.example.bluetooth_le_gatt_sborka.ACTION_GATT_DISCONNECTED",
            ACTION_GATT_SERVICES_DISCOVERED = "com.example.bluetooth_le_gatt_sborka.ACTION_GATT_SERVICES_DISCOVERED",
            ACTION_DATA_AVAILABLE = "com.example.bluetooth_le_gatt_sborka.ACTION_DATA_AVAILABLE",
            MEASUREMENTS_DATA = "com.example.bluetooth_le_gatt_sborka.MEASUREMENTS_DATA";
    /**
     * UUID для прибора сердечного ритма
     */
    public final static UUID UUID_HEART_RATE_MEASUREMENT = UUID.fromString(SampleGattAttributes.HEART_RATE_MEASUREMENT),
            BLOOD_PRESSURE_MEASUREMENT = UUID.fromString(SampleGattAttributes.BLOOD_PRESSURE_MEASUREMENT),
    // FFF2_CHARACTERISTIC = UUID.fromString(SampleGattAttributes.FFF2_CHARACTERISTIC),
    FFF1_CHARACTERISTIC = UUID.fromString(SampleGattAttributes.FFF1_CHARACTERISTIC),
            FFF0_SERVICE = UUID.fromString(SampleGattAttributes.FFF0_SERVICE);
    public static String codeRepeatCheck = "";
    public static BluetoothGatt bluetoothGatt;
    private final IBinder binder = new LocalBinder();
    // private final int
    //         statusConnectToTesto = 0;
    private BluetoothManager bluetoothManager;
    private BluetoothAdapter bluetoothAdapter;
    private String bluetoothDeviceAddress;
    // private int connectionStatus = STATE_DISCONNECTED;
    //Реализация методов обратного вызова для событий GATT,
    //о которых заботится приложение. Например, изменение подключения и обнаружение служб.
    public final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        @Override
        //при изменении состояния подключения
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            String intentAction;
            if (newState == STATE_CONNECTED) {
                intentAction = ACTION_GATT_CONNECTED;
                //connectionStatus = STATE_CONNECTED;
                broadcastUpdate(intentAction);
                // Log.d(TAG, "Connected to GATT server");
                //Попытки обнаружить службы после успешного подключения.
                //discoverServices - Обнаруживает сервисы, предлагаемые удаленным устройством,
                // а также их характеристики и дескрипторы
                Log.d(TAG, "Попытка запустить обнаружение сервисов: " + bluetoothGatt.discoverServices());
            } else if (newState == STATE_DISCONNECTED) {
                intentAction = ACTION_GATT_DISCONNECTED;
                // connectionStatus = STATE_DISCONNECTED;
                Log.d(TAG, "Disconnected from GATT server");
                codeRepeatCheck = "";
                broadcastUpdate(intentAction);
                connect(bluetoothDeviceAddress);
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt bluetoothGatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {

                switch (bluetoothGatt.getDevice().getAddress()) {
                    // case SampleGattAttributes.TESTO_SMART_PYROMETER_ADDRESS: // testo smart
                    //     connectionWithTestoSmartPyrometer(bluetoothGatt);
                    //     break;

                    case SampleGattAttributes.MICROLIFE_THERMOMETER_ADDRESS:  // microlife nc 150bt
                        //заметка себе: вытащить логику в отдельный метод
                        BluetoothGattCharacteristic microlifeCharacteristic =
                                bluetoothGatt.getService(FFF0_SERVICE).getCharacteristic(FFF1_CHARACTERISTIC);

                        if (setNotification(microlifeCharacteristic, true)) {
                            Log.d(TAG, "Notifications/indications FFF1 successfully enabled!");
                        } else
                            Log.d(TAG, "Microlife Notifications/indications FFF1 enabling error!");

                        break;
                    case SampleGattAttributes.MANOMETER_ADDRESS:  // manometer AND
                        if (setIndicationManometer(bluetoothGatt, true))
                            Log.d(TAG, "indication enable");
                        else
                            Log.d(TAG, "indication NOT enable");
                        connectionWithManometer(bluetoothGatt);
                        break;
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
                broadcastUpdate(characteristic);
            }
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            Log.d(TAG, "onCharacteristicWrite == BluetoothGatt：" + bluetoothGatt + " BluetoothGattCharacteristic：" + characteristic + " status：" + status);
            if (status == BluetoothGatt.GATT_SUCCESS)
                broadcastUpdate(characteristic);
        }

        // обратный вызов об изменении характеристик ble
        @Override
        public void onCharacteristicChanged(BluetoothGatt bluetoothGatt, BluetoothGattCharacteristic characteristic) {
            broadcastUpdate(characteristic);
        }
    };

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

    public static byte[] convertHexToByteArray(String str) {
        char[] charArray = str.toCharArray();
        int length = charArray.length / 2;
        byte[] bArr = new byte[length];
        for (int i = 0; i < length; i++) {
            int i2 = i * 2;
            int digit = Character.digit(charArray[i2 + 1], 16) | (Character.digit(charArray[i2], 16) << 4);
            if (digit > 127) {
                digit += InputDeviceCompat.SOURCE_ANY;
            }
            bArr[i] = (byte) digit;
        }
        return bArr;
    }

    // public static byte[] hexStringToByteArray(String s) {
    //     int len = s.length();
    //     byte[] data = new byte[len / 2];
    //     for (int i = 0; i < len; i += 2) {
    //         data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
    //                 + Character.digit(s.charAt(i + 1), 16));
    //     }
    //     return data;
    // }

    private void broadcastUpdate(final String action) {
        final Intent intent = new Intent(action);
        sendBroadcast(intent);
    }

    private void broadcastUpdate(final BluetoothGattCharacteristic characteristic) {
        final Intent intent = new Intent(BluetoothLEService.ACTION_DATA_AVAILABLE);

        Log.d(TAG, "oncharacteristicChanged | " + characteristic.getUuid().toString() + " | " + Arrays.toString(characteristic.getValue()));
        final byte[] data = characteristic.getValue();
        if (data != null && data.length > 0) {

            if (BLOOD_PRESSURE_MEASUREMENT.equals(characteristic.getUuid())) { // manometer
                String measurementsFromByte = "замеры манометром: SYS: " + data[1] + ". DYA: " + data[3] + ". PULSE: " + data[14];
                intent.putExtra(MEASUREMENTS_DATA, measurementsFromByte);

            } else if (SampleGattAttributes.MICROLIFE_THERMOMETER_ADDRESS.equals(bluetoothGatt.getDevice().getAddress())) {// заменить на проверку имени!
                Log.d(TAG, "characteristic changed on Microlife device");
                ThermometerMeasureData thermometerMeasureData = new ThermometerMeasureData(this);

                Runnable handleValueThread = () -> {
                    Message message = Message.obtain();
                    message.obj = characteristic.getValue();
                    thermometerMeasureData.thermoHandler.handleMessage(message);
                };

                Thread handleValueFromCharacteristic = new Thread(handleValueThread);
                handleValueFromCharacteristic.start();

            } else {
                Log.d(TAG, "подключаемое устройство не поддерживается данным приложением");
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

    // /**
    //  * Отключает существующее соединение или отменяет ожидающее соединение.
    //  * Результат отключения передается асинхронно через обратный вызов
    //  */
    // public void disconnect() {
    //     if (bluetoothAdapter == null || bluetoothGatt == null) {
    //         Log.d(TAG, "BluetoothAdapter не инициализирован");
    //         return;
    //     }
    //     bluetoothGatt.disconnect();
    // }

    // public void readCharacteristic(BluetoothGattCharacteristic characteristic) {
    //     if (bluetoothAdapter == null || bluetoothGatt == null) {
    //         Log.d(TAG, "BluetoothAdapter не инициализирован");
    //         return;
    //     }
    //     bluetoothGatt.readCharacteristic(characteristic);
    // }

    // public List<BluetoothGattService> getSupportedGattServices() {
    //     if (bluetoothGatt == null) return null;
    //     return bluetoothGatt.getServices();
    // }

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
        if (deviceAddress.equals(bluetoothDeviceAddress)
                && bluetoothGatt != null) {
            Log.d(TAG, "Попытка использовать существующий mBluetoothGatt для подключения.");
            if (bluetoothGatt.connect()) {
                // connectionStatus = STATE_CONNECTING;
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
        // connectionStatus = STATE_CONNECTING;
        return true;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        //После использования данного устройства вы должны убедиться, что вызывается BluetoothGatt.close (),
        // чтобы ресурсы были очищены должным образом.
        // В этом конкретном примере close() вызывается, когда пользовательский интерфейс отключается от службы
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

    /**
     * Функция попытки подключения к уст-ву [testo 805i или ???].
     * <p>Записывает значение "01-00" в дескриптор 00002902-0000-1000-8000-00805f9b34fb</p>
     */
    public boolean setNotification(BluetoothGattCharacteristic characteristic, boolean enabled) {

        if (bluetoothAdapter == null || bluetoothGatt == null) {
            Log.d(TAG, "BluetoothAdapter не инициализирован");
            return false;
        }
        if (characteristic == null) {
            Log.e(TAG, "Can't get characteristic!");
            return false;
        }

        boolean isSuccess = bluetoothGatt.setCharacteristicNotification(characteristic, enabled);
        BluetoothGattDescriptor descriptor = characteristic.getDescriptor(UUID.fromString(SampleGattAttributes.CLIENT_CHARACTERISTIC_CONFIG));
        if (descriptor != null) {
            if (enabled) {
                descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            } else {
                descriptor.setValue(BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE);
            }
        }
        bluetoothGatt.writeDescriptor(descriptor);
        return isSuccess;
    }

    private void connectionWithManometer(BluetoothGatt bluetoothGatt) {
        BluetoothGattCharacteristic dateAndTimeCharacteristic =
                bluetoothGatt.getService(UUID.fromString(SampleGattAttributes.BLOOD_PRESSURE_SERVICE))
                        .getCharacteristic(UUID.fromString(SampleGattAttributes.BLOOD_PRESSURE_MEASUREMENT));
        bluetoothGatt.writeCharacteristic(setDateAndTimeValueToCharacteristic(dateAndTimeCharacteristic, Calendar.getInstance()));
    }

    public boolean setIndicationManometer(BluetoothGatt bluetoothGatt, boolean enable) {
        boolean checkingForIndication = false;
        if (bluetoothGatt != null) {
            BluetoothGattService service = bluetoothGatt.getService(UUID.fromString(SampleGattAttributes.BLOOD_PRESSURE_SERVICE));
            if (service != null) {
                BluetoothGattCharacteristic characteristic = service.getCharacteristic(UUID.fromString(SampleGattAttributes.BLOOD_PRESSURE_MEASUREMENT));
                if (characteristic != null) {
                    checkingForIndication = setIndication(characteristic, enable);
                } else {
                    Log.d(TAG, "Characteristic NULL");
                }
            } else {
                Log.d(TAG, "Service NULL");
            }
        }
        return checkingForIndication;
    }

    public boolean setIndication(BluetoothGattCharacteristic characteristic, boolean enable) {
        boolean isSuccess = bluetoothGatt.setCharacteristicNotification(characteristic, enable);
        BluetoothGattDescriptor descriptor = characteristic.getDescriptor(UUID.fromString(SampleGattAttributes.CLIENT_CHARACTERISTIC_CONFIG));
        if (enable) {
            descriptor.setValue(BluetoothGattDescriptor.ENABLE_INDICATION_VALUE);
        } else {
            descriptor.setValue(BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE);
        }
        bluetoothGatt.writeDescriptor(descriptor);
        return isSuccess;
    }

    // public void connectionWithTestoSmartPyrometer(BluetoothGatt bluetoothGatt) {
//
    //     if (setNotification(bluetoothGatt.getService(FFF0_SERVICE).getCharacteristic(FFF2_CHARACTERISTIC), true)) {
//
    //         BluetoothGattCharacteristic testoCharacteristic = (bluetoothGatt.getService(FFF0_SERVICE).getCharacteristic(FFF1_CHARACTERISTIC));
    //         timeToChangeCharacteristicOnDevice();
    //         if (testoCharacteristic.setValue(convertHexToByteArray(SampleGattAttributes.TO_TESTO_HEX_1))) {
    //             if (!bluetoothGatt.writeCharacteristic(testoCharacteristic)) {
    //                 Log.d(TAG, "TESTO ERROR 1");
    //             }
    //         } else {
    //             Log.d(TAG, "TESTO ERROR 0");
    //         }
    //     } else {
    //         Log.d(TAG, " TESTO ERROR 3");
    //     }
    // }

    // public void timeToChangeCharacteristicOnDevice() {
    //     Thread oneSecondThread = new Thread() {
    //         public void run() {
    //             try {
    //                 sleep(2000);
    //             } catch (InterruptedException e) {
    //                 e.printStackTrace();
    //             }
    //         }
    //     };
    //     oneSecondThread.start();
    // }

    public class LocalBinder extends Binder {
        public BluetoothLEService getService() {
            return BluetoothLEService.this;
        }
    }
}

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

потом приходит уровень батареи в 2х пакетах с определенной периодичностью, пример:
108016000000071d0c000000426174746572794c
6576656ca2c9b8423206

}


7) to fff1  (material)
05001a0000000756100000004d6174657269616c

8) to fff1 (emission)
456d697373696f6e3333733f0471

8.1) from fff2
07000000000001ac

9.1) from fff2 (после нажатия на кнопку на устройстве)
108012000000062d0b000000427574746f6e436c
69636b01fae8

9.2) значения температур по fff2 в 2х пакетах

*/
