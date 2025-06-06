package com.example.quicknotes;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class TaskDbHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "tasks.db";
    private static final int DATABASE_VERSION = 2;

    private static final String SQL_CREATE_ENTRIES =
            "CREATE TABLE " + TaskContract.TaskEntry.TABLE_NAME + " (" +
                    TaskContract.TaskEntry._ID + " INTEGER PRIMARY KEY," +
                    TaskContract.TaskEntry.COLUMN_TEXT + " TEXT," +
                    TaskContract.TaskEntry.COLUMN_DATETIME + " TEXT," +
                    TaskContract.TaskEntry.COLUMN_PHOTO_PATH + " TEXT," +
                    TaskContract.TaskEntry.COLUMN_TIMESTAMP + " TIMESTAMP DEFAULT CURRENT_TIMESTAMP)";

    public TaskDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_ENTRIES);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            // Ajoutez la nouvelle colonne pour les versions antérieures
            db.execSQL("ALTER TABLE " + TaskContract.TaskEntry.TABLE_NAME +
                    " ADD COLUMN " + TaskContract.TaskEntry.COLUMN_PHOTO_PATH + " TEXT");
        }
    }
}