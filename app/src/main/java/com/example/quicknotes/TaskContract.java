package com.example.quicknotes;

import android.provider.BaseColumns;

public final class TaskContract {
    private TaskContract() {}

    public static class TaskEntry implements BaseColumns {
        public static final String TABLE_NAME = "tasks";
        public static final String COLUMN_TEXT = "text";
        public static final String COLUMN_DATETIME = "datetime";
        public static final String COLUMN_TIMESTAMP = "timestamp";
        public static final String COLUMN_PHOTO_PATH = "photo_path";
    }
}