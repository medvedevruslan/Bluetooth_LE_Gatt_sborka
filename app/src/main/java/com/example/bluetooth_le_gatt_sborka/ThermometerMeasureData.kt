package com.example.bluetooth_le_gatt_sborka

import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.util.Log
import java.util.*

class ThermometerMeasureData(private val context: Context) {
    lateinit var hexString: StringBuilder
    @JvmField
    var thermoHandler: Handler = object : Handler(Looper.getMainLooper()) {
        override fun handleMessage(message: Message) {
            super.handleMessage(message)
            val value = message.obj as ByteArray
            var valueFromCharacteristic = ""
            val zero = "0"
            for (b in value) {
                var valueString = Integer.toHexString(b.toInt())
                // Log.d(TAG, "ffffff to null1: " + valueString);
                if (valueString.length == 1) {
                    valueString = zero + valueString
                } else if (valueString.length > 5) {
                    valueString = valueString.replace("ffffff", "")
                    // Log.d(TAG, "ffffff to null2: " + valueString);
                }
                valueFromCharacteristic += valueString
            }
            Log.d(TAG, "valueFromCharacteristic : $valueFromCharacteristic")
            val dataWithMeasurementsAndDateTime =
                valueFromCharacteristic.substring(10, valueFromCharacteristic.length - 2)
            val cmdFromValue = valueFromCharacteristic.substring(8, 10).toInt(16)
            Log.d(TAG, "cmdFromValue : $cmdFromValue | data : $dataWithMeasurementsAndDateTime")
            when {
                cmdFromValue == UPLOAD_MEASURE_DATA -> {
                    Log.d(TAG, "UPLOAD_MEASURE_DATA data：$dataWithMeasurementsAndDateTime")
                    hexString = StringBuilder().append(dataWithMeasurementsAndDateTime)
                    parseDataFromValue()
                    replyUploadMeasureData()
                }
                cmdFromValue != SEND_REQUEST -> {
                    Log.d(TAG, "neponyatnii signal from Microlife")
                }
                else -> {
                    if (BluetoothLEService.codeRepeatCheck.contains(valueFromCharacteristic)) {
                        Log.d(TAG, "povtor signala, ignore")
                        return
                    } else {
                        BluetoothLEService.codeRepeatCheck += valueFromCharacteristic
                    }

                    // Log.d(TAG, "SEND_REQUEST data： " + dataWithMeasurementsAndDateTime);
                    replyMacAddressOrTime(Date(System.currentTimeMillis()))
                }
            }
        }
    }

    fun parseMeasurement(i: Int): Int {
        val parseInt = hexString.substring(0, i).toInt(16)
        hexString.delete(0, i)
        return parseInt
    }

    fun parseDataFromValue() {
        val parseIntAmbientTemperature = parseMeasurement(4)
        val parseInt2Mode = parseMeasurement(4)
        val parseInt3Day = parseMeasurement(2)
        val parseInt4Hour = parseMeasurement(2)
        val parseInt5Minute = parseMeasurement(2)
        val parseInt6Year = parseMeasurement(2)
        val year = parseInt6Year and 63
        val ambientTemperature = parseIntAmbientTemperature.toFloat() / 100.0f
        val mode = 32768 and parseInt2Mode shr 15
        val measureTemperature = (parseInt2Mode and 32767).toFloat() / 100.0f
        val month = parseInt3Day and 192 shr 4 or (parseInt4Hour and 192 shr 6)
        val day = parseInt3Day and 63
        val hour = parseInt4Hour and 63
        Log.d(
            TAG, "measurements | year: " + year + " | ambientTemperature: " + ambientTemperature
                    + " | mode:" + mode + " | measureTemperature:" + measureTemperature
                    + " | measurementTempearure: " + " | month:" + month + " | day:" + day + " | hour:" + hour + " | minute:" + parseInt5Minute
        )
        val intent = Intent(BluetoothLEService.ACTION_DATA_AVAILABLE)
        intent.putExtra(BluetoothLEService.MEASUREMENTS_DATA, "$measureTemperature C")
        context.sendBroadcast(intent)
    }

    fun replyMacAddressOrTime(date: Date) {
        val str1 = String.format("%02X", date.year % 100) + String.format(
            "%02X",
            date.month + 1
        ) + String.format("%02X", date.date) + String.format(
            "%02X",
            date.hours
        ) + String.format("%02X", date.minutes) + String.format("%02X", date.seconds)
        val buildCmdStringForThermo = buildCmdStringForThermo("01", str1)
        //  Log.d(TAG, "replyMacAddressOrTime：" + buildCmdStringForThermo);
        writeToBLE(buildCmdStringForThermo)
    }

    private fun buildCmdStringForThermo(str: String, str2: String): String {
        val format = String.format("%04x", str2.length / 2 + 1 + 1)
        // Log.d(TAG, "buildCmdStringForThermo = " + "4DFE" + format + str + str2 + calcChecksum(HEADER, DEVICE_CODE_THERMO_APP_REPLY, format, str, str2));
        return "4DFE$format$str$str2" + calcChecksum(
            HEADER,
            DEVICE_CODE_THERMO_APP_REPLY,
            format,
            str,
            str2
        )
    }

    fun calcChecksum(str: String, str2: String, str3: String, str4: String, str5: String): String {
/*        Log.d(TAG, "calcChecksum cmd = " + str4);
        Log.d(TAG, "calcChecksum lengthstr = " + str3);
        Log.d(TAG, "calcChecksum  data = " + str5);*/
        return try {
            var parseInt = str.toInt(16)
            val str6 = str2 + str3 + str4 + str5
            // Log.d(TAG, "calcChecksum AllData = " + str6);
            val length = str6.length
            var i = 0
            var i2 = 2
            while (i2 <= length) {
                parseInt += str6.substring(i, i2).toInt(16)
                i += 2
                i2 += 2
            }
            val format = String.format("%02x", parseInt and 255)
            // Log.d(TAG, "calcChecksum = " + format);
            format.uppercase()
        } catch (e: Exception) {
            e.printStackTrace()
            "00"
        }
    }

    fun replyUploadMeasureData() {
        val buildCmdStringForThermo = buildCmdStringForThermo(CMD_REPLY_RESULT_SUCCESS, "")

        // Log.d(TAG, "replyUploadMeasureData：" + buildCmdStringForThermo);
        writeToBLE(buildCmdStringForThermo)
    }

    // заметка себе: может написать отправку сообщений writecharacteristic в отдельном потоке? пример есть в Microlife APP class: MyWriteThread.
    @Synchronized
    fun writeToBLE(str: String?) {
        Log.d(TAG, "writeToBLE = $str")
        if (str == null) {
            return
        }
        val thermoCharacteristic =
            BluetoothLEService.bluetoothGatt?.getService(UUID.fromString(SampleGattAttributes.FFF0_SERVICE))
                ?.getCharacteristic(UUID.fromString(SampleGattAttributes.FFF2_CHARACTERISTIC))
        thermoCharacteristic?.value = BluetoothLEService.convertHexToByteArray(str)
        BluetoothLEService.bluetoothGatt?.writeCharacteristic(thermoCharacteristic)
    }

    companion object {
        private const val TAG = "Medvedev1_TMS"
        const val HEADER = "4D"
        const val DEVICE_CODE_THERMO_APP_REPLY = "FE"
        private const val UPLOAD_MEASURE_DATA = 160
        private const val SEND_REQUEST = 161
        private const val CMD_REPLY_RESULT_SUCCESS = "81"
    }
}