package com.example.bluetooth_le_gatt_sborka.support

import java.text.SimpleDateFormat
import java.util.*

object MyDate {
    @JvmStatic
    fun toDateString(date: Date): String {
        return SimpleDateFormat("yyyy/MM/dd").format(date)
    }

    @JvmStatic
    fun toTimeString(date: Date): String {
        return SimpleDateFormat("HH:mm:ss").format(date)
    }
}