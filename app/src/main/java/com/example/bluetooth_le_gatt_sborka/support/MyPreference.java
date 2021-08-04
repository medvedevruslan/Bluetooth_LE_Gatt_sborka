package com.example.bluetooth_le_gatt_sborka.support;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.preference.PreferenceManager;
import android.text.TextUtils;

import com.example.bluetooth_le_gatt_sborka.R;

import java.util.Map;

public class MyPreference {
    public static final String KEY_MUST_REPORT = "must_report";
    public static final String KEY_REP_TAICHO = "taicho"; // в переводе с японского 'отличная работа'(? не знаю почему решили написать на японском )
    public static final String KEY_REP_TENKEN = "tenken"; // в переводе с японского 'осмотр'
    public static final String KEY_REP_TENKOK = "tenkok"; // в переводе с японского 'перекличка'
    public static final String KEY_SMTP_ACCOUNT = "smtp_account";
    public static final String KEY_SMTP_PASSWORD = "smtp_password";
    private static final String TAG = "MyPreference";
    private final String mCaption1;       // "Test status" - статус теста
    private final String mCaption2;       // "Condition" - состояние, условие
    private final String mCaption3;       // "Daily life check" - ежедневная проверка
    private final SharedPreferences mPref;
    private final String[] mRepItems1;    // массив "rep_tenkok_items" из ресурс-файла "array.xml"
    private final String[] mRepItems2;    // массив "rep_taicho_items" из ресурс-файла "array.xml"
    private final String[] mRepItems3;    // массив "rep_tenken_items" из ресурс-файла "array.xml"
    private boolean mustReport;
    private String repItem1;        // статус теста {'before driving', 'on the way', 'after driving'}
    private String repItem2;        // состояние {'good', 'not good'}
    private String repItem3;        // результат ежедневной проверки {'good', 'not good'}
    private String smtpAccount;
    private String smtpPassword;
    private String userCode;
    private String userName;

    public MyPreference(Context context) {
        mPref = PreferenceManager.getDefaultSharedPreferences(context);
        Resources res = context.getResources();
        mCaption1 = res.getString(R.string.rep_tenkok_caption);
        mCaption2 = res.getString(R.string.rep_taicho_caption);
        mCaption3 = res.getString(R.string.rep_tenken_caption);
        mRepItems1 = res.getStringArray(R.array.rep_tenkok_items); // происходит заполнение массивами из ресурс-файла "array.xml"
        mRepItems2 = res.getStringArray(R.array.rep_taicho_items);
        mRepItems3 = res.getStringArray(R.array.rep_tenken_items);
        initialize();
        load();
    }

    public boolean isMustReport() {
        return mustReport;
    }

    private void initialize() {
        userName = "";
        userCode = "";
        repItem3 = "";
        repItem2 = "";
        repItem1 = "";
        mustReport = true;
        smtpPassword = "";
        smtpAccount = "";
    }

    public void load() {
        repItem1 = mPref.getString(KEY_REP_TENKOK, mRepItems1[0]);   // получаем значение по ключу KEY_REP_TENKOK, т.е. "tenkok", второй параметр - значение по умолчанию
        repItem2 = mPref.getString(KEY_REP_TAICHO, mRepItems2[0]);
        repItem3 = mPref.getString(KEY_REP_TENKEN, mRepItems3[0]);
        mustReport = mPref.getBoolean(KEY_MUST_REPORT, false);
        smtpAccount = mPref.getString(KEY_SMTP_ACCOUNT, "");
        if (!TextUtils.isEmpty(smtpAccount)) {
            smtpAccount = MyCrypt.decrypt(smtpAccount);
        }
        smtpPassword = mPref.getString(KEY_SMTP_PASSWORD, "");
        if (!TextUtils.isEmpty(smtpPassword)) {
            smtpPassword = MyCrypt.decrypt(smtpPassword);
        }
    }

    public String getValue(String key) {
        String result = "";
        switch (key) {
            case KEY_REP_TENKOK:
                result = repItem1;
                break;
            case KEY_REP_TAICHO:
                result = repItem2;
                break;
            case KEY_REP_TENKEN:
                result = repItem3;
                break;
            case KEY_SMTP_ACCOUNT:
                result = smtpAccount;
                break;
            case KEY_SMTP_PASSWORD:
                result = smtpPassword;
                break;
        }
        return result;
    }
}

