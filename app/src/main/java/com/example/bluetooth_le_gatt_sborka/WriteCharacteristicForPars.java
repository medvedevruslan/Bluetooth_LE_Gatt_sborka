package com.example.bluetooth_le_gatt_sborka;

import android.bluetooth.BluetoothGattCharacteristic;
import android.util.Log;

import java.util.StringTokenizer;


public class WriteCharacteristicForPars extends BluetoothLEService {

    /*

    private static final int GATT_MAX_MTU_SIZE = 517;
    private static final String TAG = "Medvedev WCFP";

    public boolean sendBytes(BluetoothGattCharacteristic characteristic, byte[] bytes) {

        if (characteristic == null) {
            Log.d("BleDevice", "couldn't get characteristic from BluetoothGattService");
            return false;
        }

        int writeType;
        writeType = characteristic.getWriteType();

        characteristic.setValue(bytesToWrite);
        characteristic.setWriteType(writeType);
        if (!bluetoothGatt.writeCharacteristic(characteristic)) {
            Log.e(TAG, String.format("ERROR: writeCharacteristic failed for characteristic: %s", characteristic.getUuid()));
            completedCommand();
        } else {
            Log.d(TAG, String.format("writing <%s> to characteristic <%s>", bytes2String(bytesToWrite), characteristic.getUuid()));
            nrTries++;
        }


        boolean writeCharacteristic = bluetoothGatt.writeCharacteristic((BluetoothGattCharacteristic) characteristic);

        return writeCharacteristic;
    }












    public void writeData(BluetoothGattCharacteristic characteristic, ByteArray payload) {

        if (characteristic != null) {
            if (characteristic.isWritable())
                characteristic.isWritable() ->BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
            characteristic.isWritableWithoutResponse() ->{
                BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
            }
        else ->error("Characteristic ${characteristic.uuid} cannot be written to")
        }

        if (bluetoothGatt != null) {
            characteristic.setWriteType(writeType);
            characteristic.setValue();
            bluetoothGatt.writeCharacteristic(characteristic);
        } else {
            Log.d(TAG, "Not connected to a BLE device!");
        }
    }
}
*/
}