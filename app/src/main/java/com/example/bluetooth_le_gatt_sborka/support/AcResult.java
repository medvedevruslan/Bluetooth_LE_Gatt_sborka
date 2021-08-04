package com.example.bluetooth_le_gatt_sborka.support;

import android.content.Context;
import android.text.TextUtils;

import com.example.bluetooth_le_gatt_sborka.db.MyDbManager;

import java.io.Serializable;
import java.util.Date;

/** Класс-шаблон данных результата тестирования */
public class AcResult implements Serializable {
    private static final String AC_DATE = "acdate:",
            AC_TIME = "actime:",
            AC_VALUE = "acvalue:",
            DELIMITER = ":",
            DEVICE_NAME = "device:";

    private static final String LF = System.getProperty("line.separator"),
            REP_ITEM1 = "repitem1:",
            REP_ITEM2 = "repitem2:",
            REP_ITEM3 = "repitem3:",
            USER_CODE = "usercode:",
            USER_NAME = "username:";
    private static final long serialVersionUID = 1;
    /** Дата теста */
    private String acDate,
    /** Время теста */
    acTime,
    /** Значение теста уровня алкоголя */
    acValue,
    /** Имя устройства */
    deviceName;
    /**
     * Статус теста.
     * <p>
     * Возможные значения: 'before driving', 'on the way', 'after driving'
     * </p>
     */
    private String repItem1,
    /**
     * Состояние
     * <p>
     * Возможные значения: 'good', 'not good'
     * </p>
     */
    repItem2,
    /**
     * Результат ежедневной проверки
     * <p>
     * Возможные значения: 'good', 'not good'
     * </p>
     */
    repItem3,
    /** Id пользователя */
    userId,
    /** Ник пользователя */
    userName,
    /** Тип теста */
    testType;
    /** Уникальный идентификатор строки в таблице результатов */
    private int id;

    private boolean isAdded;

    public AcResult() {
        initialize();
    }

    public void setUserName(String name) {
        userName = name;
    }

    public int getId() {
        return id;
    }

    public void setId(int _id) {
        id = _id;
    }

    public String getUserId() {
        return userId;
    }
    public void setUserId(String _userId) {
        userId = _userId;
    }

    public String getTestType() {
        return testType;
    }

    public void setTestType(String _testType) {
        testType = _testType;
    }

    public String getDate() {
        return acDate;
    }

    public void setDate(String _date) {
        acDate = _date;
    }

    public String getTime() {
        return acTime;
    }

    public void setTime(String _time) {
        acTime = _time;
    }

    public String getValue() {
        return acValue;
    }

    public void setValue(String _value) {
        acValue = _value;
    }

    public String getTestStatus() {
        return repItem1;
    }

    public void setTestStatus(String _testStatus) {
        repItem1 = _testStatus;
    }

    public String getCurrentResult() {
        return repItem2;
    }

    public void setCurrentResult(String _currentResult) {
        repItem2 = _currentResult;
    }

    public String getDayResult() {
        return repItem3;
    }

    public void setDayResult(String _dayResult) {
        repItem3 = _dayResult;
    }

    public String getDevice() {
        return deviceName;
    }

    public void setDevice(String _device) {
        deviceName = _device;
    }

    private void initialize() {
        userName = "";
        userId = "";
        repItem3 = "";
        repItem2 = "";
        repItem1 = "";
        acValue = "";
        acTime = "";
        acDate = "";
        deviceName = "";
        isAdded = false;
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
        if (items != null && items.length > 1) {
            acValue = items[1];
            String unitCode = "";
            if (items.length > 2) {
                unitCode = items[2];
            }
            acValue += "mg/L";
        }
    }

    public boolean isValueZero() {
        if (TextUtils.isEmpty(acValue)) {
            return false;
        }
        return acValue.startsWith("0.000") || acValue.startsWith("0000");
    }

    /**
     * Заполнение результатов теста данными из входного pref
     *
     * @param pref экземпляр класса MyPreference
     */
    public void setFromPreference(MyPreference pref) {
        if (pref.isMustReport()) {
            repItem1 = pref.getValue(MyPreference.KEY_REP_TENKOK);
            repItem2 = pref.getValue(MyPreference.KEY_REP_TAICHO);
            repItem3 = pref.getValue(MyPreference.KEY_REP_TENKEN);
        }
    }

    public String getUserText() {
        if (TextUtils.isEmpty(userId) || TextUtils.isEmpty(userName)) {
            return "";
        }
        return String.format("ID = %s: name = %s", userId, userName);
    }

    /**
     * Возвращает дату и время
     *
     * @return отформатированная строка вида (дата_время)
     */
    private String getFilenameResult() {
        if (TextUtils.isEmpty(acDate)) {
            return "none";
        }
        return String.format("%s_%s.txt", acDate.replace("/", ""), acTime.replace(DELIMITER, ""));
        // пример: {11/11/11. и 11:11} -> { 111111_1111}
    }

    /**
     * Метод возвращает значения всех аттрибутов
     *
     * @return строка, в которой данные разделены System.getProperty("line.separator")
     */
    private String getResultText() {
        StringBuilder sb = new StringBuilder(1024);
        sb.append(AC_DATE);
        sb.append(acDate);
        sb.append(LF);
        sb.append(AC_TIME);
        sb.append(acTime);
        sb.append(LF);
        sb.append(AC_VALUE);
        sb.append(acValue);
        sb.append(LF);
        sb.append(USER_CODE);
        sb.append(userId);
        sb.append(LF);
        sb.append(USER_NAME);
        sb.append(userName);
        sb.append(LF);
        sb.append(REP_ITEM1);
        sb.append(repItem1);
        sb.append(LF);
        sb.append(REP_ITEM2);
        sb.append(repItem2);
        sb.append(LF);
        sb.append(REP_ITEM3);
        sb.append(repItem3);
        sb.append(LF);
        sb.append(DEVICE_NAME);
        sb.append(deviceName);
        sb.append(LF);
        return sb.toString();
    }


    /**
     * Сохранение результатов в таблице результатов
     *
     * @return
     */
    public int saveResult(Context context, AcResult result) {

        if (!TextUtils.isEmpty(result.userId) && !TextUtils.isEmpty(result.userName)) {
            result.setUserId(userId);
            result.setUserName(userName);
            MyDbManager myDbManager = new MyDbManager(context);
            myDbManager.openDb();
            myDbManager.insertTestResult(result);
            myDbManager.closeDb();
        }
        isAdded = true;
        return 0;
    }

    /**
     * Разделяет строку по регулярному выражению на массив строк.
     *
     * @param line  строка, которая будет разделена
     * @param regex регулярное выражение по которому будет произведено разделение строки
     * @param limit количество элментов в новом массиве
     * @param ix    позиция возвращаемого элемента в новом массиве
     * @return возвращает элемент нового массива с 'ix'-й позиции
     */
    private String getItem(String line, String regex, int limit, int ix) {
        String[] items = line.split(regex, limit);
        if (items == null || items.length <= ix) {
            return "";
        }
        return items[ix].trim();
    }

    /**
     * Удаление строки с заданным уникальным идентификаором id
     *
     * @param context контекст, в котором ведётся работа с БД
     * @param id      уникальный идентификатор строки в таблице результатов
     */
    public boolean delete(Context context, int id) {

        MyDbManager myDbManager = new MyDbManager(context);
        myDbManager.openDb();
        myDbManager.deleteResultTableRow(id);
        myDbManager.closeDb();
        return true;
    }
}

