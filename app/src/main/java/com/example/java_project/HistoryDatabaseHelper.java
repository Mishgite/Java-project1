package com.example.java_project;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class HistoryDatabaseHelper extends SQLiteOpenHelper {

    public static final String DB_NAME = "history.db";
    public static final int DB_VERSION = 1;
    public static final String TABLE_NAME = "history";
    public static final String COL_ID = "_id";
    public static final String COL_TEXT = "text";
    public static final String COL_DATE = "date";

    public HistoryDatabaseHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + TABLE_NAME + " (" +
                COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_TEXT + " TEXT, " +
                COL_DATE + " TEXT)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        onCreate(db);
    }
}
