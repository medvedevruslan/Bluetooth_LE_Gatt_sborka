package com.example.bluetooth_le_gatt_sborka;

import android.bluetooth.BluetoothGattCharacteristic;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import java.util.Date;
import java.util.UUID;

import static com.example.bluetooth_le_gatt_sborka.BluetoothLEService.bluetoothGatt;
import static com.example.bluetooth_le_gatt_sborka.BluetoothLEService.codeRepeatCheck;

public class ThermometerMeasureData {

    private final static String TAG = "Medvedev1_TMS";
    public static final String HEADER = "4D";
    public static final String DEVICE_CODE_THERMO_APP_REPLY = "FE";
    private static final int UPLOAD_MEASURE_DATA = 160;
    private static final int SEND_REQUEST = 161;
    private float ambientTemperature;
    private int day;
    private int hour;
    private float measureTemperature;
    private int minute;
    private int mode;
    private int month;
    private int year;
    private static final String CMD_REPLY_RESULT_SUCCESS = "81";
    public StringBuilder hexString;

    Handler thermoHandler = new Handler(Looper.getMainLooper()) {

        public void handleMessage(Message message) {
            super.handleMessage(message);
            byte[] value = (byte[]) message.obj;
            String valueFromCharacteristic = "";
            String zero = "0";

            for (int i = 0; i < value.length; i++) {
                String valueString = Integer.toHexString(value[i]);
                // Log.d(TAG, "ffffff to null1: " + valueString);
                if (valueString.length() == 1) {
                    valueString = zero + valueString;
                } else if (valueString.length() > 5) {
                    valueString = valueString.replace("ffffff", "");
                    // Log.d(TAG, "ffffff to null2: " + valueString);
                }
                valueFromCharacteristic += valueString;
            }

            Log.d(TAG, "valueFromCharacteristic : " + valueFromCharacteristic);
            String dataWithMeasurementsAndDateTime = valueFromCharacteristic.substring(10, valueFromCharacteristic.length() - 2);
            int cmdFromValue = Integer.parseInt(valueFromCharacteristic.substring(8, 10), 16);
            Log.d(TAG, "cmdFromValue : " + cmdFromValue + " | data : " + dataWithMeasurementsAndDateTime);

            if (cmdFromValue == UPLOAD_MEASURE_DATA) {
                Log.d(TAG, "UPLOAD_MEASURE_DATA data：" + dataWithMeasurementsAndDateTime);
                ThermometerMeasureData.this.hexString = new StringBuilder().append(dataWithMeasurementsAndDateTime);
                parseDataFromValue();
                replyUploadMeasureData();

            } else if (cmdFromValue != SEND_REQUEST) {
                Log.d(TAG, "neponyatnii signal from Microlife");
                return;

            } else {
                if (codeRepeatCheck.contains(valueFromCharacteristic)) {
                    Log.d(TAG, "povtor signala, ignore");
                    return;
                } else {
                    codeRepeatCheck += valueFromCharacteristic;
                }

                // Log.d(TAG, "SEND_REQUEST data： " + dataWithMeasurementsAndDateTime);
                replyMacAddressOrTime(new Date(System.currentTimeMillis()));
            }
        }
    };

    public int parseMeasurement(int i) {
        int parseInt = Integer.parseInt(this.hexString.substring(0, i), 16);
        this.hexString.delete(0, i);
        return parseInt;
    }

    public void parseDataFromValue() {
        int parseIntAmbientTemperature = parseMeasurement(4);
        int parseInt2Mode = parseMeasurement(4);
        int parseInt3Day = parseMeasurement(2);
        int parseInt4Hour = parseMeasurement(2);
        int parseInt5Minute = parseMeasurement(2);
        int parseInt6Year = parseMeasurement(2);

        this.year = parseInt6Year & 63;
        this.ambientTemperature = ((float) parseIntAmbientTemperature) / 100.0f;
        this.mode = (32768 & parseInt2Mode) >> 15;
        this.measureTemperature = ((float) (parseInt2Mode & 32767)) / 100.0f;
        this.month = ((parseInt3Day & 192) >> 4) | ((parseInt4Hour & 192) >> 6);
        this.day = parseInt3Day & 63;
        this.hour = parseInt4Hour & 63;
        this.minute = parseInt5Minute;
        Log.d(TAG, "measurements | year: " + year + " | ambientTemperature: " + ambientTemperature
                + " | mode:" + mode + " | measureTemperature:" + measureTemperature
                + " | measurementTempearure: " + " | month:" + month + " | day:" + day + " | hour:" + hour + " | minute:" + minute);
        DeviceControlActivity.displayMeasurements("Замеры температуры с Microlife: " + String.valueOf(measureTemperature));
    }

    public void replyMacAddressOrTime(Date date) {

        String str1 = String.format("%02X", Integer.valueOf(date.getYear() % 100)) +
                String.format("%02X", Integer.valueOf(date.getMonth() + 1)) +
                String.format("%02X", Integer.valueOf(date.getDate())) +
                String.format("%02X", Integer.valueOf(date.getHours())) +
                String.format("%02X", Integer.valueOf(date.getMinutes())) +
                String.format("%02X", Integer.valueOf(date.getSeconds()));

        String buildCmdStringForThermo = buildCmdStringForThermo("01", str1);
        //  Log.d(TAG, "replyMacAddressOrTime：" + buildCmdStringForThermo);
        writeToBLE(buildCmdStringForThermo);
    }

    public String buildCmdStringForThermo(String str, String str2) {
        String format = String.format("%04x", (str2.length() / 2) + 1 + 1);
        String str3 = "4DFE" + format + str + str2 + calcChecksum(HEADER, DEVICE_CODE_THERMO_APP_REPLY, format, str, str2);
        // Log.d(TAG, "buildCmdStringForThermo = " + str3);
        return str3;
    }

    public String calcChecksum(String str, String str2, String str3, String str4, String str5) {
/*        Log.d(TAG, "calcChecksum cmd = " + str4);
        Log.d(TAG, "calcChecksum lengthstr = " + str3);
        Log.d(TAG, "calcChecksum  data = " + str5);*/
        try {
            int parseInt = Integer.parseInt(str, 16);
            String str6 = str2 + str3 + str4 + str5;
            // Log.d(TAG, "calcChecksum AllData = " + str6);
            int length = str6.length();
            int i = 0;
            for (int i2 = 2; i2 <= length; i2 += 2) {
                parseInt += Integer.parseInt(str6.substring(i, i2), 16);
                i += 2;
            }
            String format = String.format("%02x", Integer.valueOf(parseInt & 255));
            // Log.d(TAG, "calcChecksum = " + format);
            return format.toUpperCase();
        } catch (Exception e) {
            e.printStackTrace();
            return "00";
        }
    }

    public void replyUploadMeasureData() {
        String buildCmdStringForThermo = this.buildCmdStringForThermo(CMD_REPLY_RESULT_SUCCESS, "");

        // Log.d(TAG, "replyUploadMeasureData：" + buildCmdStringForThermo);
        this.writeToBLE(buildCmdStringForThermo);
    }

    // заметка себе: может написать отправку сообщений writecharacteristic в отдельном потоке? пример есть в Microlife APP class: MyWriteThread.

    public synchronized boolean writeToBLE(String str) {
        Log.d(TAG, "writeToBLE = " + str);
        if (str == null) {
            return false;
        }

        BluetoothGattCharacteristic thermoCharacteristic = (bluetoothGatt.getService(UUID.fromString(SampleGattAttributes.FFF0_SERVICE))
                .getCharacteristic(UUID.fromString(SampleGattAttributes.FFF2_CHARACTERISTIC)));

        thermoCharacteristic.setValue(BluetoothLEService.convertHexToByteArray(str));

        return bluetoothGatt.writeCharacteristic(thermoCharacteristic);
    }
}
