package com.example.bluetooth_le_gatt_sborka

import java.util.HashMap

/**
 * Этот класс включает небольшое подмножество стандартных атрибутов GATT
 * для демонстрационных целей.
 */
object SampleGattAttributes {
    private val attributes = HashMap<String, String>()
    const val HEART_RATE_MEASUREMENT = "00002a37-0000-1000-8000-00805f9b34fb"
    const val BLOOD_PRESSURE_MEASUREMENT = "00002a35-0000-1000-8000-00805f9b34fb"
    const val CLIENT_CHARACTERISTIC_CONFIG = "00002902-0000-1000-8000-00805f9b34fb"
    const val BLOOD_PRESSURE_SERVICE = "00001810-0000-1000-8000-00805f9b34fb"
    const val BATTERY_LEVEL = "00002a19-0000-1000-8000-00805f9b34fb"
    const val FFF1_CHARACTERISTIC = "0000fff1-0000-1000-8000-00805f9b34fb"
    const val FFF2_CHARACTERISTIC = "0000fff2-0000-1000-8000-00805f9b34fb"
    const val FFF0_SERVICE = "0000fff0-0000-1000-8000-00805f9b34fb"
    const val TESTO_SMART_PYROMETER_ADDRESS = "40:BD:32:A0:6E:D6"
    const val MICROLIFE_THERMOMETER_ADDRESS = "18:7A:93:BC:6D:80"
    const val MANOMETER_ADDRESS = "34:14:B5:B1:30:C3"
    const val TO_TESTO_HEX_1 = "5600030000000c69023e81"

    init {
        //характеристики, скачанные с Bluetooth SIG Specification
        // https://btprodspecificationrefs.blob.core.windows.net/assigned-values/16-bit%20UUID%20Numbers%20Document.pdf
        // Sample Services.
        attributes["0000180d-0000-1000-8000-00805f9b34fb"] =
            "Служба измерения пульса" // Heart Rate Service (service)
        attributes["00002a38-0000-1000-8000-00805f9b34fb"] =
            "Расположение датчика тела" // Body Sensor Location
        attributes["0000180a-0000-1000-8000-00805f9b34fb"] =
            "Служба информации об устройстве" // Device Information Service (service)
        attributes["00002a24-0000-1000-8000-00805f9b34fb"] =
            "Строка с номером модели" // Model Number String
        attributes["00002a25-0000-1000-8000-00805f9b34fb"] =
            "Строка серийного номера" // Serial Number String
        attributes["00002a26-0000-1000-8000-00805f9b34fb"] =
            "Строка версии прошивки" // Firmware Revision String
        attributes["00002a27-0000-1000-8000-00805f9b34fb"] =
            "Строка версии оборудования" // Hardware Revision String
        attributes["00002a28-0000-1000-8000-00805f9b34fb"] =
            "Строка версии программного обеспечения" // Software Revision String
        attributes["00002a29-0000-1000-8000-00805f9b34fb"] =
            "Строка названия производителя" // Manufacturer Name String
        attributes["00001803-0000-1000-8000-00805f9b34fb"] = "Потеря связи" // Link Loss (service)
        attributes["00002a06-0000-1000-8000-00805f9b34fb"] =
            "Уровень предупреждения" // Alert Level
        attributes["00001802-0000-1000-8000-00805f9b34fb"] =
            "Немедленное оповещение" // Immediate Alert (service)
        attributes["00001804-0000-1000-8000-00805f9b34fb"] = "Мощность Tx" // Tx Power (service)
        attributes["00002a07-0000-1000-8000-00805f9b34fb"] = "Уровень мощности Tx" // Tx Power Level
        attributes["0000180f-0000-1000-8000-00805f9b34fb"] = "Батарея" // Battery (service)
        attributes[BATTERY_LEVEL] = "Уровень Батареи" // Battery Level
        attributes["0000fff0-0000-1000-8000-00805f9b34fb"] =
            "Unknown Custom Service" // Unknown service (service)
        attributes[FFF1_CHARACTERISTIC] = "FFF1_characteristic" // Unknown characteristic
        attributes[FFF2_CHARACTERISTIC] = "FFF2_characteristic" // Unknown characteristic
        attributes["0000fff3-0000-1000-8000-00805f9b34fb"] =
            "FiRa Consortium" // FiRa Consortium (service)
        attributes["0000fff4-0000-1000-8000-00805f9b34fb"] = "FiRa Consortium" // FiRa Consortium
        attributes["0000fff5-0000-1000-8000-00805f9b34fb"] =
            "Car Connectivity Consortium, LLC" // Car Connectivity Consortium, LLC
        attributes["00001800-0000-1000-8000-00805f9b34fb"] =
            "Общий доступ" // Generic Access (service)
        attributes["00002a00-0000-1000-8000-00805f9b34fb"] = "Имя устройства" // Device Name
        attributes["00002a01-0000-1000-8000-00805f9b34fb"] =
            "Появление или внешний вид" // Appearance
        attributes["00002a04-0000-1000-8000-00805f9b34fb"] =
            "Параметры предпочтительного периферийного подключения" // Peripheral Preferred Connection Parameters
        attributes["00001801-0000-1000-8000-00805f9b34fb"] =
            "Общий атрибут" // Generic Attribute (service)
        attributes["00002a05-0000-1000-8000-00805f9b34fb"] = "Сервис изменен" // Service Changed

        // Sample Characteristics.
        attributes[HEART_RATE_MEASUREMENT] = "Измерение пульса" // Heart Rate Measurement
    }
}