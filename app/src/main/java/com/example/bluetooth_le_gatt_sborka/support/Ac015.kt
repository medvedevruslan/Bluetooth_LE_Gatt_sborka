package com.example.bluetooth_le_gatt_sborka.support;

import android.bluetooth.BluetoothDevice;
import android.util.Log;

import androidx.annotation.NonNull;

import com.example.bluetooth_le_gatt_sborka.R;

/**
 * Класс для связи с устройствоми обработки входящих от него результатов
 */
public class Ac015 {
    private static final String DELIMITER = "\r\n";
    private boolean errorReceived;
    private String mBuffer = "";
    private BluetoothDevice mDevice;
    private boolean resultReceived;

    public Ac015() {
        mDevice = null;
    }

    public void setConnectStart(BluetoothDevice device) {
        mDevice = device;
    }

    public BluetoothDevice device() {
        return mDevice;
    }

    @NonNull
    public String toString() {
        if (mDevice == null) {
            return "";
        }
        return mDevice.getName() + "\n" + mDevice.getAddress();
    }

    public void initializeBuffer() {
        mBuffer = "";
    }

    /**
     * Запись в mBuffer новых данных
     *
     * @param data это значение кладется в mBuffer
     * @return старое значение mBuffer
     */
    public String receive(String data) {
        String str = mBuffer + data;
        mBuffer = str;
        int ix = str.indexOf(DELIMITER);
        if (ix < 0) {
            return "";
        }
        String result = mBuffer.substring(0, ix);
        mBuffer = mBuffer.substring(DELIMITER.length() + ix);
        return result;
    }

    public boolean isResultReceived() {
        return resultReceived;
    }

    public boolean isErrorReceived() {
        return errorReceived;
    }

    public int analyze(String data) {
        errorReceived = false;
        resultReceived = false;
        Log.i("Ac015ANALYZE", data);
        if (data.startsWith("$WAIT")) {
            return R.string.ac_wait;
        }
        if (data.startsWith("$STANBY")) {
            return R.string.ac_stanby;
        }
        if (data.startsWith("$TRIGGER")) {
            return R.string.ac_trigger;
        } else if (data.startsWith("$BREATH")) {
            return R.string.ac_breath;
        } else {
            if (data.matches("\\$R:[0-9]\\.[0-9]{3}.*") || data.matches("\\$R:[0-9]{4}.*")) {
                resultReceived = true;
                return R.string.ac_result;
            } else if (data.startsWith("$FLOW,ERR")) {
                return R.string.ac_flow_err;
            } else {
                if (data.startsWith("$MODULE,ERR")) {
                    errorReceived = true;
                    return R.string.ac_module_err;
                } else if (data.startsWith("$TEMP,ERR")) {
                    errorReceived = true;
                    return R.string.ac_temp_err;
                } else if (data.startsWith("$CALIBRATION")) {
                    errorReceived = true;
                    return R.string.ac_calibration;
                } else if (data.startsWith("$BAT,LOW")) {
                    errorReceived = true;
                    return R.string.ac_bat_low;
                } else if (data.startsWith("$SYSTEM,ERR")) {
                    errorReceived = true;
                    return R.string.ac_system_err;
                } else if (data.startsWith("$TIME,OUT")) {
                    errorReceived = true;
                    return R.string.ac_time_out;
                } else if (data.startsWith("$SENSOR,ERR")) {
                    errorReceived = true;
                    return R.string.ac_sensor_err;
                } else if (data.startsWith("$LIFETIMEOVER")) {
                    errorReceived = true;
                    return R.string.ac_lifetimeover;
                } else {
                    errorReceived = true;
                    return R.string.ac_unknown;
                }
            }
        }
    }
}
