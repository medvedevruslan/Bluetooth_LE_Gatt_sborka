package com.example.bluetooth_le_gatt_sborka.support

import java.io.Serializable
import java.util.*

/** Класс-шаблон данных результата тестирования  */
class AcResult : Serializable {

    /** Дата теста  */
    private var _acDate = ""
    val acDate: String
        get() = _acDate

    /** Время теста  */
    private var _acTime = ""
    val acTime: String
        get() = _acTime

    /** Значение теста уровня алкоголя  */
    var acValue = ""
        set(value) {
            val answer = value.substring(value.indexOf(":") + 1, value.indexOf(","))
            if (answer.isNotEmpty()) {
                field = answer
                field += " mg/L"
            }
        }

    /** Имя устройства  */
    var deviceName: String = ""

    fun acTime(acTime: Date) {
        _acDate = MyDate.toDateString(acTime)
        _acTime = MyDate.toTimeString(acTime)
    }

}