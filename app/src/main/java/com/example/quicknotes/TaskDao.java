package com.example.quicknotes;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;

public class TaskDao {
    private TaskDbHelper dbHelper;

    public TaskDao(Context context) {
        dbHelper = new TaskDbHelper(context);
    }

    public long insertTask(String text, String datetime, String photoPath) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        try {
            ContentValues values = new ContentValues();
            values.put(TaskContract.TaskEntry.COLUMN_TEXT, text);
            values.put(TaskContract.TaskEntry.COLUMN_DATETIME, datetime);

            if (photoPath != null) {
                values.put(TaskContract.TaskEntry.COLUMN_PHOTO_PATH, photoPath);
            }

            return db.insert(TaskContract.TaskEntry.TABLE_NAME, null, values);
        } catch (Exception e) {
            Log.e("DB_ERROR", "Insert failed", e);
            return -1;
        } finally {
            db.close();
        }
    }

    public int updateTaskPhoto(long id, String photoPath) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(TaskContract.TaskEntry.COLUMN_PHOTO_PATH, photoPath);
        return db.update(
                TaskContract.TaskEntry.TABLE_NAME,
                values,
                TaskContract.TaskEntry._ID + " = ?",
                new String[]{String.valueOf(id)}
        );
    }

    public ArrayList<HashMap<String, String>> getAllTasks() {
        ArrayList<HashMap<String, String>> tasks = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        String[] projection = {
                TaskContract.TaskEntry._ID,
                TaskContract.TaskEntry.COLUMN_TEXT,
                TaskContract.TaskEntry.COLUMN_DATETIME
        };

        Cursor cursor = db.query(
                TaskContract.TaskEntry.TABLE_NAME,
                projection,
                null,
                null,
                null,
                null,
                TaskContract.TaskEntry.COLUMN_DATETIME + " ASC"
        );

        while (cursor.moveToNext()) {
            HashMap<String, String> task = new HashMap<>();
            task.put("id", cursor.getString(cursor.getColumnIndexOrThrow(TaskContract.TaskEntry._ID)));
            task.put("text", cursor.getString(cursor.getColumnIndexOrThrow(TaskContract.TaskEntry.COLUMN_TEXT)));
            task.put("datetime", cursor.getString(cursor.getColumnIndexOrThrow(TaskContract.TaskEntry.COLUMN_DATETIME)));
            tasks.add(task);
        }
        cursor.close();
        return tasks;
    }

    public int deleteTask(long id) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        return db.delete(
                TaskContract.TaskEntry.TABLE_NAME,
                TaskContract.TaskEntry._ID + " = ?",
                new String[]{String.valueOf(id)}
        );
    }
}