package com.example.bluetooth_le_gatt_sborka

import android.app.Service
import android.bluetooth.*
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.os.Message
import android.util.Log
import androidx.core.view.InputDeviceCompat
import java.util.*

/**
 * Служба для управления подключением и передачей данных с сервером GATT, размещенным на данном устройстве Bluetooth LE.
 */
class BluetoothLEService : Service() {
    private val binder: IBinder = LocalBinder()
    private var bluetoothManager: BluetoothManager? = null
    private var bluetoothAdapter: BluetoothAdapter? = null
    private lateinit var bluetoothDeviceAddress: String

    //Реализация методов обратного вызова для событий GATT,
    //о которых заботится приложение. Например, изменение подключения и обнаружение служб.
    private val gattCallback: BluetoothGattCallback = object : BluetoothGattCallback() {
        //при изменении состояния подключения
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            val intentAction: String
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                intentAction = ACTION_GATT_CONNECTED
                broadcastUpdate(intentAction)
                Log.d(TAG, "Connected to GATT server")
                //Попытки обнаружить службы после успешного подключения.
                //discoverServices - Обнаруживает сервисы, предлагаемые удаленным устройством,
                // а также их характеристики и дескрипторы
                Log.d(
                    TAG,
                    "Попытка запустить обнаружение сервисов: " + bluetoothGatt!!.discoverServices()
                )
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                intentAction = ACTION_GATT_DISCONNECTED
                Log.d(TAG, "Disconnected from GATT server")
                codeRepeatCheck = ""
                broadcastUpdate(intentAction)
                connect(bluetoothDeviceAddress)
            }
        }

        override fun onServicesDiscovered(bluetoothGatt: BluetoothGatt, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                when (bluetoothGatt.device.address) {
                    SampleGattAttributes.MICROLIFE_THERMOMETER_ADDRESS -> {
                        //заметка себе: вытащить логику в отдельный метод
                        val microlifeCharacteristic =
                            bluetoothGatt.getService(FFF0_SERVICE).getCharacteristic(
                                FFF1_CHARACTERISTIC
                            )
                        if (setNotification(microlifeCharacteristic, true)) {
                            Log.d(TAG, "Notifications/indications FFF1 successfully enabled!")
                        } else Log.d(
                            TAG,
                            "Microlife Notifications/indications FFF1 enabling error!"
                        )
                    }
                    SampleGattAttributes.MANOMETER_ADDRESS -> {
                        if (setIndicationManometer(bluetoothGatt, true)) Log.d(
                            TAG,
                            "indication enable"
                        ) else Log.d(
                            TAG, "indication NOT enable"
                        )
                        connectionWithManometer(bluetoothGatt)
                    }
                }
                broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED)
            } else {
                Log.d(TAG, "об обнаруженных услугах получено: $status")
            }
        }

        // обратный вызов докладывающий об рузельтате чтения зарактеристик ble
        override fun onCharacteristicRead(
            bluetoothGatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic, status: Int
        ) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                broadcastUpdate(characteristic)
            }
        }

        override fun onCharacteristicWrite(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) {
            Log.d(
                TAG,
                "onCharacteristicWrite == BluetoothGatt：$bluetoothGatt BluetoothGattCharacteristic：$characteristic status：$status"
            )
            if (status == BluetoothGatt.GATT_SUCCESS) broadcastUpdate(characteristic)
        }

        // обратный вызов об изменении характеристик ble
        override fun onCharacteristicChanged(
            bluetoothGatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic
        ) {
            broadcastUpdate(characteristic)
        }
    }

    private fun broadcastUpdate(action: String) {
        val intent = Intent(action)
        sendBroadcast(intent)
    }

    private fun broadcastUpdate(characteristic: BluetoothGattCharacteristic) {
        val intent = Intent(ACTION_DATA_AVAILABLE)

        //Это особая обработка профиля измерения частоты пульса.
        // Анализ данных выполняется в соответствии со спецификациями профиля:
        // http: developer.bluetooth.orggattcharacteristicsPagesCharacteristicViewer.aspx? U = org.bluetooth.characteristic.heart_rate_measurement.xml
        if (UUID_HEART_RATE_MEASUREMENT == characteristic.uuid) {
            val flag = characteristic.properties
            val format: Int
            if (flag and 0x01 != 0) {
                format = BluetoothGattCharacteristic.FORMAT_UINT16
                Log.d(TAG, "Heart rate format UINT16.")
            } else {
                format = BluetoothGattCharacteristic.FORMAT_UINT8
                Log.d(TAG, "Heart rate format UINT8.")
            }
            val heartRate = characteristic.getIntValue(format, 1)
            Log.d(
                TAG,
                "properties from Heart Rate: $characteristic | Received heartrate: $heartRate"
            )
            intent.putExtra(EXTRA_DATA, heartRate.toString())
        } else {
            val data = characteristic.value
            if (data != null && data.isNotEmpty()) {
                //Для всех остальных профилей записывает данные в формате HEX
                Log.d(
                    TAG,
                    "oncharacteristicChanged | $characteristic.uuid.toString() | ${Arrays.toString(data)}"
                )
                when {
                    BLOOD_PRESSURE_MEASUREMENT == characteristic.uuid -> { // manometer
                        val measurementsFromByte: String = if (data.size >= 14) "SYS: " + data[1] + ". DYA: " + data[3] + ". PULSE: " + data[14] else "SYS: " + data[1] + ". DYA: " + data[3] + ". PULSE: " + data[7]
                        intent.putExtra(MEASUREMENTS_DATA, measurementsFromByte)
                    }
                    SampleGattAttributes.MICROLIFE_THERMOMETER_ADDRESS == bluetoothGatt!!.device.address -> {
                        Log.d(TAG, "characteristic changed on Microlife device")
                        val thermometerMeasureData = ThermometerMeasureData(this)
                        val handleValueThread = Runnable {
                            val message = Message.obtain()
                            message.obj = characteristic.value
                            thermometerMeasureData.thermoHandler.handleMessage(message)
                        }
                        val handleValueFromCharacteristic = Thread(handleValueThread)
                        handleValueFromCharacteristic.start()
                    }
                    else -> {
                        Log.d(TAG, "подключаемое устройство не поддерживается данным приложением")
                    }
                }
            }
            sendBroadcast(intent)
        }
    }

    fun initialize(): Boolean {
        // Для уровня API 18 и выше получите ссылку на BluetoothAdapter
        if (bluetoothManager == null) {
            bluetoothManager = getSystemService(BLUETOOTH_SERVICE) as BluetoothManager
            if (bluetoothManager == null) {
                Log.d(TAG, "Невозможно инициализировать BluetoothManager")
                return false
            }
        }
        bluetoothAdapter = bluetoothManager!!.adapter
        if (bluetoothAdapter == null) {
            Log.d(TAG, "Невозможно получить адаптер Bluetooth")
            return false
        }
        return true
    }

    /**
     * Подключается к серверу GATT, размещенному на устройстве Bluetooth LE.
     *
     * @param deviceAddress Адрес устройства назначения.
     * @return Возвращает истину, если соединение инициировано успешно.
     * Результат подключения передается асинхроннос помощью обратного
     * вызова `BluetoothGattCallback#onConnectionStateChange (android.bluetooth.BluetoothGatt, int, int)`.
     */
    fun connect(deviceAddress: String): Boolean {
        bluetoothDeviceAddress = deviceAddress
        if (bluetoothAdapter == null) {
            Log.d(TAG, "BluetoothAdapter не инициализирован или адрес не указан")
            return false
        }

        //Ранее подключенное устройство. Попытка переподключиться.
        if (deviceAddress == bluetoothDeviceAddress && bluetoothGatt != null) {
            Log.d(TAG, "Попытка использовать существующий mBluetoothGatt для подключения.")
            return bluetoothGatt!!.connect()
        }
        val bluetoothDevice = bluetoothAdapter!!.getRemoteDevice(deviceAddress)
        if (bluetoothDevice == null) {
            Log.d(TAG, "устройство не найдено. Невозможно подключиться")
            return false
        }

        // Мы хотим напрямую подключиться к устройству,
        // поэтому устанавливаем для параметра autoConnect значение false.
        bluetoothGatt = bluetoothDevice.connectGatt(this, false, gattCallback)
        //Log.d(TAG, "Попытка создать новое соединение");
        return true
    }

    override fun onBind(intent: Intent): IBinder {
        return binder
    }

    override fun onUnbind(intent: Intent): Boolean {
        //После использования данного устройства вы должны убедиться, что вызывается BluetoothGatt.close (),
        // чтобы ресурсы были очищены должным образом.
        // В этом конкретном примере close () вызывается, когда пользовательский интерфейс отключается от службы.
        close()
        return super.onUnbind(intent)
    }

    /**
     * После использования данного устройства BLE приложение должно вызвать этот метод,
     * чтобы обеспечить правильное высвобождение ресурсов.
     */
    private fun close() {
        if (bluetoothGatt == null) {
            return
        }
        bluetoothGatt!!.close()
        bluetoothGatt = null
    }

    inner class LocalBinder : Binder() {
        val service: BluetoothLEService
            get() = this@BluetoothLEService
    }

    /**
     * Функция попытки подключения к уст-ву [testo 805i или ???].
     *
     * Записывает значение "01-00" в дескриптор 00002902-0000-1000-8000-00805f9b34fb
     */
    fun setNotification(characteristic: BluetoothGattCharacteristic?, enabled: Boolean): Boolean {
        if (bluetoothAdapter == null || bluetoothGatt == null) {
            Log.d(TAG, "BluetoothAdapter не инициализирован")
            return false
        }
        if (characteristic == null) {
            Log.e(TAG, "Can't get characteristic!")
            return false
        }
        val isSuccess = bluetoothGatt!!.setCharacteristicNotification(characteristic, enabled)
        val descriptor =
            characteristic.getDescriptor(UUID.fromString(SampleGattAttributes.CLIENT_CHARACTERISTIC_CONFIG))
        if (descriptor != null) {
            if (enabled) {
                descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
            } else {
                descriptor.value = BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE
            }
        }
        bluetoothGatt!!.writeDescriptor(descriptor)
        return isSuccess
    }

    private fun connectionWithManometer(bluetoothGatt: BluetoothGatt) {
        val dateAndTimeCharacteristic =
            bluetoothGatt.getService(UUID.fromString(SampleGattAttributes.BLOOD_PRESSURE_SERVICE))
                .getCharacteristic(UUID.fromString(SampleGattAttributes.BLOOD_PRESSURE_MEASUREMENT))
        bluetoothGatt.writeCharacteristic(
            setDateAndTimeValueToCharacteristic(
                dateAndTimeCharacteristic,
                Calendar.getInstance()
            )
        )
    }

    fun setIndicationManometer(bluetoothGatt: BluetoothGatt?, enable: Boolean): Boolean {
        var checkingForIndication = false
        if (bluetoothGatt != null) {
            val service =
                bluetoothGatt.getService(UUID.fromString(SampleGattAttributes.BLOOD_PRESSURE_SERVICE))
            if (service != null) {
                val characteristic =
                    service.getCharacteristic(UUID.fromString(SampleGattAttributes.BLOOD_PRESSURE_MEASUREMENT))
                if (characteristic != null) {
                    checkingForIndication = setIndication(characteristic, enable)
                } else {
                    Log.d(TAG, "Characteristic NULL")
                }
            } else {
                Log.d(TAG, "Service NULL")
            }
        }
        return checkingForIndication
    }

    private fun setIndication(characteristic: BluetoothGattCharacteristic, enable: Boolean): Boolean {
        val isSuccess = bluetoothGatt!!.setCharacteristicNotification(characteristic, enable)
        val descriptor =
            characteristic.getDescriptor(UUID.fromString(SampleGattAttributes.CLIENT_CHARACTERISTIC_CONFIG))
        if (enable) {
            descriptor.value = BluetoothGattDescriptor.ENABLE_INDICATION_VALUE
        } else {
            descriptor.value = BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE
        }
        bluetoothGatt!!.writeDescriptor(descriptor)
        return isSuccess
    }

    companion object {
        const val ACTION_GATT_CONNECTED =
            "com.example.bluetooth_le_gatt_sborka.ACTION_GATT_CONNECTED"
        const val ACTION_GATT_DISCONNECTED =
            "com.example.bluetooth_le_gatt_sborka.ACTION_GATT_DISCONNECTED"
        const val ACTION_GATT_SERVICES_DISCOVERED =
            "com.example.bluetooth_le_gatt_sborka.ACTION_GATT_SERVICES_DISCOVERED"
        const val ACTION_DATA_AVAILABLE =
            "com.example.bluetooth_le_gatt_sborka.ACTION_DATA_AVAILABLE"
        const val MEASUREMENTS_DATA = "com.example.bluetooth_le_gatt_sborka.MEASUREMENTS_DATA"
        const val EXTRA_DATA = "com.example.bluetooth_le_gatt_sborka.ACTION_DATA"

        /**
         * UUID для прибора сердечного ритма
         */
        val UUID_HEART_RATE_MEASUREMENT =
            UUID.fromString(SampleGattAttributes.HEART_RATE_MEASUREMENT)!!
        val BLOOD_PRESSURE_MEASUREMENT =
            UUID.fromString(SampleGattAttributes.BLOOD_PRESSURE_MEASUREMENT)!!
        val FFF1_CHARACTERISTIC = UUID.fromString(SampleGattAttributes.FFF1_CHARACTERISTIC)!!
        val FFF0_SERVICE = UUID.fromString(SampleGattAttributes.FFF0_SERVICE)!!
        var codeRepeatCheck = ""
        private const val TAG = "Medvedev1 BLES"
        var bluetoothGatt: BluetoothGatt? = null
        fun setDateAndTimeValueToCharacteristic(
            characteristic: BluetoothGattCharacteristic,
            calendar: Calendar
        ): BluetoothGattCharacteristic {
            val year = calendar[1] //год
            characteristic.value = byteArrayOf(
                (year and 255).toByte(),
                (year shr 8).toByte(),
                (calendar[2] + 1).toByte(),  // месяц
                calendar[5].toByte(),  // день
                calendar[11].toByte(),  // часы
                calendar[12].toByte(),  // минуты
                calendar[13].toByte() // секунды
            )
            return characteristic
        }

        fun convertHexToByteArray(str: String): ByteArray {
            val charArray = str.toCharArray()
            val length = charArray.size / 2
            val bArr = ByteArray(length)
            for (i in 0 until length) {
                val i2 = i * 2
                var digit = Character.digit(charArray[i2 + 1], 16) or (Character.digit(
                    charArray[i2], 16
                ) shl 4)
                if (digit > 127) {
                    digit += InputDeviceCompat.SOURCE_ANY
                }
                bArr[i] = digit.toByte()
            }
            return bArr
        }
    }
}