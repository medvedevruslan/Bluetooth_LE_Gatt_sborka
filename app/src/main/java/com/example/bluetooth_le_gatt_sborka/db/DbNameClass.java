package com.example.bluetooth_le_gatt_sborka.db;

import android.provider.BaseColumns;

/** Класс, содержащий в себе следующие аттрибуты:
 * <p>
 * - имена таблиц,
 * </p><p>
 * - имена столбцов таблиц,
 * </p><p>
 * - SQL запросы для создания, удаления таблиц,
 * </p><p>
 * - название базы данных,
 * </p><p>
 * - версию базы данных.
 * </p>
 */
public class DbNameClass implements BaseColumns {

    // Таблица пользователей
    public static final String PERSON_TABLE = "person_table";
    public static final String USER_NAME = "name";
    public static final String USER_ID = "user_id";
    public static final String IS_ENABLED = "is_enabled";
    public static final String CREATE_PERSON = "CREATE TABLE IF NOT EXISTS " + PERSON_TABLE + " ( "+
            USER_ID + " INTEGER PRIMARY KEY , " + USER_NAME + " TEXT, " +
            IS_ENABLED + " TEXT );" ;
    public static final String DROP_PERSON = "DROP TABLE IF EXISTS " + PERSON_TABLE + " ;";

    // Таблица результатов
    public static final String RESULT_TABLE = "result_table";
    public static final String TEST_TYPE = "test_type";     // {"breathalyzer", "pyrometer","tonometer"}
    public static final String DATE = "date";
    public static final String TIME = "time";
    public static final String VALUE = "value";
    public static final String TEST_STATUS = "test_status";         // {"before driving", "on the way", "after driving"}
    public static final String CURRENT_RESULT = "current_result";   // {"good", "not good"} - результат текущего теста
    public static final String DAY_RESULT = "day_result";           // {"good", "not good"} - результат дня
    public static final String DEVICE = "device";                   //  устройство, на котором был произведен тест
    public static final String CREATE_RESULT_TABLE = "CREATE TABLE IF NOT EXISTS " + RESULT_TABLE + " ( "+
                                    BaseColumns._ID + " INTEGER PRIMARY KEY, "  + USER_ID + " TEXT, " +
                                    TEST_TYPE + " TEXT, " + DATE + " TEXT, " +
                                    TIME + " TEXT, " + VALUE + " TEXT, " +
                                    TEST_STATUS + " TEXT, " + CURRENT_RESULT + " TEXT, " +
                                    DAY_RESULT + " TEXT, " + DEVICE + " TEXT );" ;
    public static final String DROP_RESULT_TABLE = "DROP TABLE IF EXISTS " + RESULT_TABLE + " ;";

    public static final String DB_NAME = "driver.db";
    public static final int DB_VERSION = 1;
}
