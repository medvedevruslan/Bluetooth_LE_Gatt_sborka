package com.example.bluetooth_le_gatt_sborka;

import java.util.HashMap;

/**
 * Этот класс включает небольшое подмножество стандартных атрибутов GATT
 * для демонстрационных целей.
 */
public class SampleGattAttributes {

    private final static HashMap<String, String> attributes = new HashMap<>();
    public final static String HEART_RATE_MEASUREMENT = "00002a37-0000-1000-8000-00805f9b34fb";
    public final static String BLOOD_PRESSURE_MEASUREMENT = "00002a35-0000-1000-8000-00805f9b34fb";
    public final static String CLIENT_CHARACTERISTIC_CONFIG = "00002902-0000-1000-8000-00805f9b34fb";
    public final static String BLOOD_PRESSURE_SERVICE = "00001810-0000-1000-8000-00805f9b34fb";
    public final static String BATTERY_LEVEL = "00002a19-0000-1000-8000-00805f9b34fb";
    public final static String FFF1_CHARACTERISTIC = "0000fff1-0000-1000-8000-00805f9b34fb";
    public final static String FFF2_CHARACTERISTIC = "0000fff2-0000-1000-8000-00805f9b34fb";
    public final static String FFF0_SERVICE = "0000fff0-0000-1000-8000-00805f9b34fb";

    public final static String TESTO_SMART_PYROMETER_ADDRESS = "40:BD:32:A0:6E:D6";
    public final static String MICROLIFE_THERMOMETER_ADDRESS = "18:7A:93:BC:6D:80";
    public final static String MANOMETER_ADDRESS = "34:14:B5:B1:30:C3";

    public final static String TO_TESTO_HEX_1 = "5600030000000c69023e81";
    public final static String FROM_TESTO_ACCESS = "07000000000001ac";
    public final static String TO_TESTO_HEX_FIRMWARE_1 = "200000000000077b";
    public final static String FROM_TESTO_HEX_FIRMWARE_1 = "070004000000009c01000020";
    public final static String TO_TESTO_HEX_FIRMWARE_2 = "04001500000005930f0000004669726d7761726556657273696f6e304f";
    public final static String FROM_TESTO_HEX_FIRMWARE_2 = "07001200000004d40c0000003030312e3130312e303030314409";
    public final static String TESTO_BATTERY_LEVEL = "110000000000035a";
    public final static String TESTO_MATERIAL = "05001a0000000756100000004d6174657269616c";
    public final static String TESTO_EMISSION = "456d697373696f6e3333733f0471";




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
        attributes.put(BATTERY_LEVEL, "Уровень Батареи"); // Battery Level

        attributes.put("0000fff0-0000-1000-8000-00805f9b34fb", "Unknown Custom Service"); // Unknown service (service)
        attributes.put(FFF1_CHARACTERISTIC, "FFF1_characteristic"); // Unknown characteristic
        attributes.put(FFF2_CHARACTERISTIC, "FFF2_characteristic"); // Unknown characteristic

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