package com.example.bluetooth_le_gatt_sborka.support

import android.bluetooth.BluetoothDevice
import android.util.Log
import com.example.bluetooth_le_gatt_sborka.R

/**
 * Класс для связи с устройствоми обработки входящих от него результатов
 */


class Ac015 {
    private var _isErrorReceived = false
    val isErrorReceived
        get() = _isErrorReceived
    private var mBuffer = ""
    private lateinit var _device: BluetoothDevice
    val device
        get() = _device

    private var _isResultReceived = false
    val isResultReceived
        get() = _isResultReceived

    fun setConnectStart(device: BluetoothDevice) {
        _device = device
    }

    override fun toString(): String {
        return """
     ${_device.name}
     ${_device.address}
     """.trimIndent()
    }

    fun initializeBuffer() {
        mBuffer = ""
    }

    /**
     * Запись в mBuffer новых данных
     *
     * @param data это значение кладется в mBuffer
     * @return старое значение mBuffer
     */
    fun receive(data: String): String {
        val str = mBuffer + data
        mBuffer = str
        val ix = str.indexOf(DELIMITER)
        if (ix < 0) {
            return ""
        }
        val result = mBuffer.substring(0, ix)
        mBuffer = mBuffer.substring(DELIMITER.length + ix)
        return result
    }

    fun analyze(data: String): Int {
        _isErrorReceived = false
        _isResultReceived = false
        Log.i("Ac015ANALYZE", data)
        if (data.startsWith("\$WAIT")) {
            return R.string.ac_wait
        }
        if (data.startsWith("\$STANBY")) {
            return R.string.ac_stanby
        }
        return if (data.startsWith("\$TRIGGER")) {
            R.string.ac_trigger
        } else if (data.startsWith("\$BREATH")) {
            R.string.ac_breath
        } else {
            if (data.contains("R:[0-9].[0-9]{3}.*".toRegex()) || data.matches("R:[0-9]{4}.*".toRegex())) {
                _isResultReceived = true
                R.string.ac_result
            } else if (data.startsWith("\$FLOW,ERR")) {
                R.string.ac_flow_err
            } else {
                when {
                    data.startsWith("\$MODULE,ERR") -> {
                        _isErrorReceived = true
                        R.string.ac_module_err
                    }
                    data.startsWith("\$TEMP,ERR") -> {
                        _isErrorReceived = true
                        R.string.ac_temp_err
                    }
                    data.startsWith("\$CALIBRATION") -> {
                        _isErrorReceived = true
                        R.string.ac_calibration
                    }
                    data.startsWith("\$BAT,LOW") -> {
                        _isErrorReceived = true
                        R.string.ac_bat_low
                    }
                    data.startsWith("\$SYSTEM,ERR") -> {
                        _isErrorReceived = true
                        R.string.ac_system_err
                    }
                    data.startsWith("\$TIME,OUT") -> {
                        _isErrorReceived = true
                        R.string.ac_time_out
                    }
                    data.startsWith("\$SENSOR,ERR") -> {
                        _isErrorReceived = true
                        R.string.ac_sensor_err
                    }
                    data.startsWith("\$LIFETIMEOVER") -> {
                        _isErrorReceived = true
                        R.string.ac_lifetimeover
                    }
                    else -> {
                        _isErrorReceived = true
                        R.string.ac_unknown
                    }
                }
            }
        }
    }

    companion object {
        private const val DELIMITER = "\r\n"
    }
}