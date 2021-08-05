package com.example.bluetooth_le_gatt_sborka.support;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MyDate {
    private static final SimpleDateFormat fmtDate = new SimpleDateFormat("yyyy/MM/dd");
    private static final SimpleDateFormat fmtTime = new SimpleDateFormat("HH:mm:ss");

    public static String toDateString(Date date) {
        return fmtDate.format(date);
    }

    public static String toTimeString(Date date) {
        return fmtTime.format(date);
    }
}

