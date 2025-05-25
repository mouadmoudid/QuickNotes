package com.example.quicknotes;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.util.Log;

import androidx.annotation.RequiresPermission;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

public class NotificationReceiver extends BroadcastReceiver {
    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    @Override
    public void onReceive(Context context, Intent intent) {
        try {
            String noteText = intent.getStringExtra("noteText");
            if (noteText == null || noteText.isEmpty()) return;

            createNotificationChannel(context);

            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, "notes_channel")
                    .setSmallIcon(R.drawable.ic_launcher_foreground)
                    .setContentTitle(context.getString(R.string.notification_title))
                    .setContentText(noteText)
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .setAutoCancel(true);

            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);

            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                    == PackageManager.PERMISSION_GRANTED) {
                notificationManager.notify(noteText.hashCode(), builder.build());
            }

        } catch (Exception e) {
            Log.e("NotificationReceiver", "Error showing notification", e);
        }
    }

    private void createNotificationChannel(Context context) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    "notes_channel",
                    "Rappels de notes",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            context.getSystemService(NotificationManager.class).createNotificationChannel(channel);
        }
    }
}