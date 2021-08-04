package com.example.bluetooth_le_gatt_sborka.db;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.example.bluetooth_le_gatt_sborka.support.AcPerson;
import com.example.bluetooth_le_gatt_sborka.support.AcResult;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

public class MyDbManager {
    private final MyDbHelper myDbHelper;
    private SQLiteDatabase db;
    private final Context myContext;
    private static String DB_NAME = "driver.db";
    private static String DB_PATH = "/data/data/com.example.breathalyzer/databases/";


    public MyDbManager(Context context) {

        myDbHelper = new MyDbHelper(context);
        myContext = context;
    }

    public void openDb(){   db = myDbHelper.getWritableDatabase();  }

    public void closeDb(){  myDbHelper.close(); }

    public void cleanDataBase(){
        myDbHelper.onUpgrade(db, 1,1);
    }
    /**
     * Копирует базу из папки assets вместо локальной БД
     * Выполняется путем копирования потока байтов.
     * */
    public void loadDataBase() throws IOException {
        this.cleanDataBase();
        //Открываем локальную БД как входящий поток
        InputStream myInput = myContext.getAssets().open(DB_NAME);

        //Путь к БД
        String outFileName = DB_PATH + DbNameClass.DB_NAME;

        //Открываем пустую базу данных как исходящий поток
        OutputStream myOutput = new FileOutputStream(outFileName);

        //перемещаем байты из входящего файла в исходящий
        byte[] buffer = new byte[1024];
        int length;
        while ((length = myInput.read(buffer))>0){
            myOutput.write(buffer, 0, length);
        }

        //закрываем потоки
        myOutput.flush();
        myOutput.close();
        myInput.close();
    }
    /**
     * Метод проверяет, есть ли в таблице имен строка с заданным ID.
     * @param id номер искомой строки
     */
    public boolean isExists(String id) {
        String selection = DbNameClass.USER_ID + " = ?";
        @SuppressLint("Recycle")
        Cursor cur = db.query(DbNameClass.PERSON_TABLE, new String[]{DbNameClass.USER_ID}, selection, new String[]{id}, null,  null, null);
        return cur != null && cur.getCount() > 0;
    }

    /**
     * Метод проверяет, не удалялась ли в таблице имен строка с заданным ID.
     * @param id номер искомой строки
     * @return
     */
    public boolean isEnabled(String id) {
        String selection = DbNameClass.USER_ID + " = ?";
        @SuppressLint("Recycle")
        Cursor cur = db.query(DbNameClass.PERSON_TABLE, new String[]{DbNameClass.IS_ENABLED}, selection, new String[]{id}, null,  null, null);
        cur.moveToFirst();
        return cur.getString(0).equals("true");
    }
    /**
     * Вставка новой строки в таблицу имен.
     */
    public void insertPerson(String id, String name) {

        ContentValues values = new ContentValues();
        values.put(DbNameClass.USER_ID, id);
        values.put(DbNameClass.USER_NAME, name);
        values.put(DbNameClass.IS_ENABLED, "false");
        db.beginTransaction();
        try {
            db.insert(DbNameClass.PERSON_TABLE, null, values);
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }
    /**
     * Вставка новой строки в таблицу результатов тестов.
     */
    public void insertTestResult(AcResult acResult){

        ContentValues cv = new ContentValues();
        cv.put(DbNameClass.USER_ID, acResult.getUserId());
        cv.put(DbNameClass.TEST_TYPE, acResult.getTestType());
        cv.put(DbNameClass.DATE, acResult.getDate());
        cv.put(DbNameClass.TIME, acResult.getTime());
        cv.put(DbNameClass.VALUE, acResult.getValue());
        cv.put(DbNameClass.TEST_STATUS, acResult.getTestStatus());
        cv.put(DbNameClass.CURRENT_RESULT, acResult.getCurrentResult());
        cv.put(DbNameClass.DAY_RESULT, acResult.getDayResult());
        cv.put(DbNameClass.DEVICE, acResult.getDevice());
        db.beginTransaction();
        try {
            db.insert(DbNameClass.RESULT_TABLE, null, cv);
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    /**
     * Обновление данных в заданной строке в таблице имен.
     * @param id ID строки, в которой нужно обновить данные
     * @param name новое значение обновляемой строки
     */
    public void updatePerson(String id, String name) {

        ContentValues values = new ContentValues();
        values.put(DbNameClass.USER_NAME, name);
        String clause = DbNameClass.USER_ID + "= ?";
        db.beginTransaction();
        try {
            db.update(DbNameClass.PERSON_TABLE, values, clause, new String[]{id});
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }
    /**
     * Установка ограничения видимости заданной строки в таблице имен.
     * @param id ID строки, в которой нужно обновить данные
     */
    public void setEnabled(String id) {

        ContentValues values = new ContentValues();
        values.put(DbNameClass.IS_ENABLED, "true");
        String clause = DbNameClass.USER_ID + "= ?";
        db.beginTransaction();
        try {
            db.update(DbNameClass.PERSON_TABLE, values, clause, new String[]{id});
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }
    /**
     * Обновление данных в заданной строке в таблице результатов тестов.
     * @param acResult новые данные обновляемой строки
     * @param id номер обновляемой строки
     */
    public void updateTestResultItem(AcResult acResult, String id){
        String clause = DbNameClass._ID + "= ?";
        ContentValues cv = new ContentValues();
        cv.put(DbNameClass.USER_ID, acResult.getUserId());
        cv.put(DbNameClass.TEST_TYPE, acResult.getTestType());
        cv.put(DbNameClass.DATE, acResult.getDate());
        cv.put(DbNameClass.TIME, acResult.getTime());
        cv.put(DbNameClass.VALUE, acResult.getValue());
        cv.put(DbNameClass.TEST_STATUS, acResult.getTestStatus());
        cv.put(DbNameClass.CURRENT_RESULT, acResult.getCurrentResult());
        cv.put(DbNameClass.DAY_RESULT, acResult.getDayResult());
        cv.put(DbNameClass.DEVICE, acResult.getDevice());
        db.beginTransaction();
        try{
            db.update(DbNameClass.RESULT_TABLE, cv, clause, new String[]{id});
            db.setTransactionSuccessful();
        } finally{
            db.endTransaction();
        }
    }
    /**
     * Удаление строки из таблицы имен.
     * @param id номер удаляемой строки
     */
    public void deletePersonTableRow(String id) {

        db.beginTransaction();
        try {
            db.delete(DbNameClass.PERSON_TABLE, DbNameClass.USER_ID + "=" + id, null);
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }
    /**
     * Удаление строки из таблицы результатов.
     * @param id номер удаляемой строки
     */
    public void deleteResultTableRow(int id){

        db.beginTransaction();
        try{
            db.delete(DbNameClass.RESULT_TABLE, DbNameClass._ID + "=" + id, null);
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    /**
     * Удаление строк из таблицы результатов.
     * @param user_id id пользователя, результаты тестов которого надо удалить.
     */
    public void deleteResultTableRows(String user_id){

        db.beginTransaction();
        try{
            db.delete(DbNameClass.RESULT_TABLE, DbNameClass.USER_ID + "=" + user_id, null);
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }
    /**
     * Чтение данных из таблицы результатов.
     * @param items массив, в который записываются данные таблицы
     */
    public void readFromResultTable( List<AcResult> items) {

        final Cursor cursor = db.query(DbNameClass.RESULT_TABLE, null, null,
                null, null, null, null);

        while (cursor.moveToNext()) {
            AcResult item = new AcResult();
            @SuppressLint("Range") int _id = cursor.getInt(cursor.getColumnIndex(DbNameClass._ID));
            @SuppressLint("Range") String user_id = cursor.getString(cursor.getColumnIndex(DbNameClass.USER_ID));
            @SuppressLint("Range") String test_type = cursor.getString(cursor.getColumnIndex(DbNameClass.TEST_TYPE));
            @SuppressLint("Range") String date = cursor.getString(cursor.getColumnIndex(DbNameClass.DATE));
            @SuppressLint("Range") String time = cursor.getString(cursor.getColumnIndex(DbNameClass.TIME));
            @SuppressLint("Range") String value = cursor.getString(cursor.getColumnIndex(DbNameClass.VALUE));
            @SuppressLint("Range") String test_status = cursor.getString(cursor.getColumnIndex(DbNameClass.TEST_STATUS));
            @SuppressLint("Range") String current_result = cursor.getString(cursor.getColumnIndex(DbNameClass.CURRENT_RESULT));
            @SuppressLint("Range") String day_result = cursor.getString(cursor.getColumnIndex(DbNameClass.DAY_RESULT));
            @SuppressLint("Range") String device = cursor.getString(cursor.getColumnIndex(DbNameClass.DEVICE));

            item.setId(_id);
            item.setUserId(user_id);
            item.setTestType(test_type);
            item.setDate(date);
            item.setTime(time);
            item.setValue(value);
            item.setTestStatus(test_status);
            item.setCurrentResult(current_result);
            item.setDayResult(day_result);
            item.setDevice(device);

            items.add(item);
        }
        cursor.close();
        closeDb();

        openDb();
        int count = items.size();
        for(int i = 0; i < count; i++){
            AcResult item = items.get(i);
            String id = item.getUserId();
            item.setUserName(getPersonName(id));
        }
    }
    /**
     * Чтение данных из таблицы имен.
     * @param items массив, в который записываются данные таблицы
     */
    public void readFromPersonTable( List<AcPerson> items) {

        final Cursor cursor = db.query(DbNameClass.PERSON_TABLE, null, null,
                null, null, null, null);

        while (cursor.moveToNext()) {
            AcPerson item = new AcPerson();
            @SuppressLint("Range") String user_id = cursor.getString(cursor.getColumnIndex(DbNameClass.USER_ID));
            @SuppressLint("Range") String name = cursor.getString(cursor.getColumnIndex(DbNameClass.USER_NAME));
            item.setUserCode(user_id);
            item.setUserName(name);
            items.add(item);
        }
        cursor.close();
    }
    public String getPersonName(String id){

        final Cursor c = db.query(DbNameClass.PERSON_TABLE, new String[]{ DbNameClass.USER_NAME }, DbNameClass.USER_ID + "=" + id, null, null, null, null);
        int itemCount = c.getCount();
        if (c.moveToFirst() &&  itemCount == 1) {
            int columnIndex = c.getColumnIndex(DbNameClass.USER_NAME);
            String userName = c.getString(columnIndex);
            return userName;
        }
        return "";
    }
    /**
     * Метод производит чтение данных из таблицы имен.
     * @return все строки таблицы, которые доступны для показа на экране
     */
    public Cursor selectAll() {
        String selection = DbNameClass.IS_ENABLED + " = ?";
        //сортировка по USER_ID и условие "is_enabled = false"
        Cursor cur = db.query(DbNameClass.PERSON_TABLE, new String[]{DbNameClass.USER_ID, DbNameClass.USER_NAME, DbNameClass.IS_ENABLED},  selection, new String[]{"false"}, null, null, DbNameClass.USER_ID);

        if (cur == null || cur.moveToFirst()) { //проверка условий
            return cur;
        }
        return null; //возврат null ? неизвестно почему возвращает нулл, есть вероятность что не правильно разобралось приложение
    }
    /**
     * Функция возвращает значение '0'-ого столбца таблицы имен.
     */
    public String getCode(Cursor c) {
        return c.getString(0); // возвращает значение '0'-ого столбца в виде строки
    }
    /**
     * Функция возвращает значение ника текущей  таблицы имен.
     */
    public String getName(Cursor c) {
        return c.getString(1);//
    }
    /**
     * Функция возвращает количество строк в таблице имен.
     */
    public long getRowCount() {
        long count = 0;
        String sql = "select count(*) from " + DbNameClass.PERSON_TABLE;
        Cursor c = db.rawQuery(sql, (String[]) null);
        if (c != null && c.moveToLast()) {
            count = c.getLong(0);
        }
        return count;
    }

    /**
     * Функция проверяет сколько строк в таблице имен. Если есть всего одно строка, то записывает в @param result данные этой строки
     */
    public void getPersonWhenOnly(AcResult result) {
        Cursor c = selectAll();
        if (c != null && c.getCount() == 1) {
            result.setUserId(getCode(c));
            result.setUserName(getName(c));
        }
    }
    /**
     * Функция проверяет есть ли элементы в таблице имен. Если таблица не пуста, то возвращает id последней добавленной строки, иначе 1.
     * @return
     */
    public String getLastAddedUserCode(){
        final Cursor cursor = db.query(DbNameClass.PERSON_TABLE, null, null,
                null, null, null, null);
        cursor.moveToLast();
        return (cursor != null) ? getCode(cursor) : "1" ;
    }
}