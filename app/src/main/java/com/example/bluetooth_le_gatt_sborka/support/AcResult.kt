package com.example.bluetooth_le_gatt_sborka.support;

import java.io.Serializable;
import java.util.Date;

/** Класс-шаблон данных результата тестирования */
public class AcResult implements Serializable {
    /** Дата теста */
    private String acDate,
    /** Время теста */
    acTime,
    /** Значение теста уровня алкоголя */
    acValue,
    /** Имя устройства */
    deviceName;

    public AcResult() {
        initialize();
    }

    public void setDevice(String _device) {
        deviceName = _device;
    }

    private void initialize() {
        acValue = "";
        acTime = "";
        acDate = "";
        deviceName = "";
    }

    public String getDeviceName() {
        return deviceName;
    }

    public String getAcDate() {
        return acDate;
    }

    public String getAcTime() {
        return acTime;
    }

    public void setAcTime(Date acTime2) {
        acDate = MyDate.toDateString(acTime2);
        acTime = MyDate.toTimeString(acTime2);
    }

    public String getAcValue() {
        return acValue;
    }

    public void setAcValue(String acValue2) {
        String[] items = acValue2.split("[:,]");
        if (items.length > 1) {
            acValue = items[1];
            acValue += "mg/L";
        }
    }
}