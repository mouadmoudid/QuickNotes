package com.example.quicknotes;

import android.app.AlarmManager;
import android.app.DatePickerDialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.Manifest;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private EditText editTextNote;
    private Button buttonAdd, buttonDate;
    private ListView listViewNotes;
    private ArrayList<HashMap<String, String>> notesList;
    private Calendar selectedDateTime = Calendar.getInstance();
    private SimpleDateFormat dateTimeFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
    private TaskDao taskDao;
    private ArrayAdapter<HashMap<String, String>> adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        createNotificationChannel();

        // Initialisation de la base de données
        taskDao = new TaskDao(this);

        editTextNote = findViewById(R.id.editTextNote);
        buttonAdd = findViewById(R.id.buttonAdd);
        buttonDate = findViewById(R.id.buttonDate);
        listViewNotes = findViewById(R.id.listViewNotes);

        notesList = new ArrayList<>();

        // Charger les tâches existantes depuis la base de données
        loadTasksFromDatabase();

        // Adapter personnalisé
        adapter = new ArrayAdapter<HashMap<String, String>>(
                this, R.layout.note_item, R.id.textViewNote, notesList) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                TextView textViewNote = view.findViewById(R.id.textViewNote);
                TextView textViewDate = view.findViewById(R.id.textViewDate);
                Button buttonDelete = view.findViewById(R.id.buttonDelete);

                HashMap<String, String> note = notesList.get(position);
                textViewNote.setText(note.get("text"));
                textViewDate.setText(note.get("datetime"));

                buttonDelete.setOnClickListener(v -> {
                    // Supprimer de la base de données
                    long id = Long.parseLong(note.get("id"));
                    taskDao.deleteTask(id);

                    // Annuler la notification associée
                    cancelScheduledNotification(id);

                    // Supprimer de la liste
                    notesList.remove(position);
                    notifyDataSetChanged();
                });

                return view;
            }
        };

        listViewNotes.setAdapter(adapter);

        // Sélection de date et heure
        buttonDate.setOnClickListener(v -> showDateTimePicker());

        // Ajout de note
        buttonAdd.setOnClickListener(v -> {
            String noteText = editTextNote.getText().toString().trim();
            if (!noteText.isEmpty()) {
                // Ajouter à la base de données
                String datetime = dateTimeFormat.format(selectedDateTime.getTime());
                long id = taskDao.insertTask(noteText, datetime);

                // Ajouter à la liste
                HashMap<String, String> task = new HashMap<>();
                task.put("id", String.valueOf(id));
                task.put("text", noteText);
                task.put("datetime", datetime);
                notesList.add(task);

                // Programmer la notification
                scheduleNotification(noteText, selectedDateTime.getTimeInMillis());

                adapter.notifyDataSetChanged();
                editTextNote.setText("");
            }
        });
    }

    private void loadTasksFromDatabase() {
        ArrayList<HashMap<String, String>> savedTasks = taskDao.getAllTasks();
        notesList.addAll(savedTasks);
    }

    private void showDateTimePicker() {
        // D'abord sélectionner la date
        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    selectedDateTime.set(year, month, dayOfMonth);
                    // Ensuite sélectionner l'heure
                    showTimePicker();
                },
                selectedDateTime.get(Calendar.YEAR),
                selectedDateTime.get(Calendar.MONTH),
                selectedDateTime.get(Calendar.DAY_OF_MONTH)
        );
        datePickerDialog.show();
    }

    private void showTimePicker() {
        TimePickerDialog timePickerDialog = new TimePickerDialog(
                this,
                (view, hourOfDay, minute) -> {
                    selectedDateTime.set(Calendar.HOUR_OF_DAY, hourOfDay);
                    selectedDateTime.set(Calendar.MINUTE, minute);
                    selectedDateTime.set(Calendar.SECOND, 0);

                    // Mettre à jour le texte du bouton
                    buttonDate.setText(dateTimeFormat.format(selectedDateTime.getTime()));
                },
                selectedDateTime.get(Calendar.HOUR_OF_DAY),
                selectedDateTime.get(Calendar.MINUTE),
                true // Format 24h
        );
        timePickerDialog.show();
    }

    private void cancelScheduledNotification(long id) {
        Intent intent = new Intent(this, NotificationReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                this,
                (int) id,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        if (alarmManager != null) {
            alarmManager.cancel(pendingIntent);
        }
    }
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Notes Channel";
            String description = "Channel for note reminders";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;

            NotificationChannel channel = new NotificationChannel("notes_channel", name, importance);
            channel.setDescription(description);

            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    private void scheduleNotification(String noteText, long triggerTime) {
        // version android
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
            if (!alarmManager.canScheduleExactAlarms()) {
                // swl 3la les perm
                Intent intent = new Intent(android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
                startActivity(intent);
                return; //lamakano ta permission
            }
        }

        Intent intent = new Intent(this, NotificationReceiver.class);
        intent.putExtra("noteText", noteText);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                this,
                noteText.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        if (alarmManager != null) {
            alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
            );
        }
    }

    private void checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                        this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS},
                        1001
                );
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
            if (!alarmManager.canScheduleExactAlarms()) {
                Intent intent = new Intent(android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
                startActivity(intent);
            }
        }
    }



}