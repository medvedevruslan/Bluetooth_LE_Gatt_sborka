package com.example.bluetooth_le_gatt_sborka.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import androidx.annotation.Nullable;

public class MyDbHelper extends SQLiteOpenHelper {

    public MyDbHelper(@Nullable Context context) {
        super(context, DbNameClass.DB_NAME, null, DbNameClass.DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {

        db.execSQL(DbNameClass.CREATE_PERSON);
        db.execSQL(DbNameClass.CREATE_RESULT_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

        db.execSQL(DbNameClass.DROP_PERSON);
        db.execSQL(DbNameClass.DROP_RESULT_TABLE);
        onCreate(db);

    }
}