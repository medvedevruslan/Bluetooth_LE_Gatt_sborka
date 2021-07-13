package com.example.bluetooth_le_gatt_sborka;

import java.util.HashMap;

/**
 * Этот класс включает небольшое подмножество стандартных атрибутов GATT
 * для демонстрационных целей.
 */
public class SampleGattAttributes {

    private static HashMap<String, String> attributes = new HashMap<>();
    public static String HEART_RATE_MEASUREMENT = "00002a37-0000-1000-8000-00805f9b34fb";
    public static String CLIENT_CHARACTERISTIC_CONFIG = "00002902-0000-1000-8000-00805f9b34fb";

    static {
        //характеристики, скачанные с Bluetooth SIG Specification
        // https://btprodspecificationrefs.blob.core.windows.net/assigned-values/16-bit%20UUID%20Numbers%20Document.pdf

        // Sample Services.
        attributes.put("0000180d-0000-1000-8000-00805f9b34fb", "Служба измерения пульса"); // Heart Rate Service (service)
        attributes.put("00002a38-0000-1000-8000-00805f9b34fb", "Расположение датчика тела"); // Body Sensor Location

        attributes.put("0000180a-0000-1000-8000-00805f9b34fb", "Служба информации об устройстве"); // Device Information Service (service)
        attributes.put("00002a24-0000-1000-8000-00805f9b34fb", "Строка с номером модели"); // Model Number String
        attributes.put("00002a25-0000-1000-8000-00805f9b34fb", "Строка серийного номера"); // Serial Number String
        attributes.put("00002a26-0000-1000-8000-00805f9b34fb", "Строка версии прошивки"); // Firmware Revision String
        attributes.put("00002a27-0000-1000-8000-00805f9b34fb", "Строка версии оборудования"); // Hardware Revision String
        attributes.put("00002a28-0000-1000-8000-00805f9b34fb", "Строка версии программного обеспечения"); // Software Revision String
        attributes.put("00002a29-0000-1000-8000-00805f9b34fb", "Строка названия производителя"); // Manufacturer Name String

        attributes.put("00001803-0000-1000-8000-00805f9b34fb", "Потеря связи"); // Link Loss (service)
        attributes.put("00002a06-0000-1000-8000-00805f9b34fb", "Уровень предупреждения"); // Alert Level

        attributes.put("00001802-0000-1000-8000-00805f9b34fb", "Немедленное оповещение"); // Immediate Alert (service)

        attributes.put("00001804-0000-1000-8000-00805f9b34fb", "Мощность Tx"); // Tx Power (service)
        attributes.put("00002a07-0000-1000-8000-00805f9b34fb", "Уровень мощности Tx"); // Tx Power Level

        attributes.put("0000180f-0000-1000-8000-00805f9b34fb", "Батарея"); // Battery (service)
        attributes.put("00002a19-0000-1000-8000-00805f9b34fb", "Уровень Батареи"); // Battery Level

        attributes.put("0000fff0-0000-1000-8000-00805f9b34fb", "Unknown Custom Service"); // Unknown service (service)
        attributes.put("0000fff1-0000-1000-8000-00805f9b34fb", "Unknown Custom Characteristic"); // Unknown characteristic
        attributes.put("0000fff2-0000-1000-8000-00805f9b34fb", "Unknown Custom Characteristic"); // Unknown characteristic

        attributes.put("0000fff3-0000-1000-8000-00805f9b34fb", "FiRa Consortium"); // FiRa Consortium (service)
        attributes.put("0000fff4-0000-1000-8000-00805f9b34fb", "FiRa Consortium"); // FiRa Consortium
        attributes.put("0000fff5-0000-1000-8000-00805f9b34fb", "Car Connectivity Consortium, LLC"); // Car Connectivity Consortium, LLC


        attributes.put("00001800-0000-1000-8000-00805f9b34fb", "Общий доступ"); // Generic Access (service)
        attributes.put("00002a00-0000-1000-8000-00805f9b34fb", "Имя устройства"); // Device Name
        attributes.put("00002a01-0000-1000-8000-00805f9b34fb", "Появление или внешний вид"); // Appearance
        attributes.put("00002a04-0000-1000-8000-00805f9b34fb", "Параметры предпочтительного периферийного подключения"); // Peripheral Preferred Connection Parameters

        attributes.put("00001801-0000-1000-8000-00805f9b34fb", "Общий атрибут"); // Generic Attribute (service)
        attributes.put("00002a05-0000-1000-8000-00805f9b34fb", "Сервис изменен"); // Service Changed

        // Sample Characteristics.
        attributes.put(HEART_RATE_MEASUREMENT, "Измерение пульса"); // Heart Rate Measurement
    }

    public static String lookup(String uuid, String defaultName) {
        String name = attributes.get(uuid);
        //return name == null ? defaultName : name;
        if (name == null) return defaultName;
        return name;
    }
}