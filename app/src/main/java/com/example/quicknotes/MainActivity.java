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
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.Manifest;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
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
    private static final int REQUEST_CAMERA_PERMISSION = 1002;
    private static final int REQUEST_IMAGE_CAPTURE = 1003;
    private String currentPhotoPath;
    private long currentTaskIdForPhoto = -1;

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
                ImageButton buttonDelete = view.findViewById(R.id.buttonDelete);
                ImageView imageViewPhoto = view.findViewById(R.id.imageViewPhoto);
                TextView photoIndicator = view.findViewById(R.id.textViewPhotoIndicator);

                HashMap<String, String> note = notesList.get(position);
                textViewNote.setText(note.get("text"));
                textViewDate.setText(note.get("datetime"));
                ImageView imageView = view.findViewById(R.id.imageViewPhoto);

                String photoPath = note.get("photo_path");
                if (photoPath != null && !photoPath.isEmpty()) {
                    imageViewPhoto.setVisibility(View.VISIBLE);
                    Bitmap bitmap = BitmapFactory.decodeFile(photoPath);
                    imageView.setImageBitmap(Bitmap.createScaledBitmap(bitmap, 100, 100, false));
                    photoIndicator.setVisibility(View.VISIBLE);
                } else {
                    imageViewPhoto.setVisibility(View.GONE);
                    photoIndicator.setVisibility(View.GONE);
                }
                ImageButton buttonPhoto = view.findViewById(R.id.buttonPhoto);
                buttonPhoto.setOnClickListener(v -> {
                    currentTaskIdForPhoto = Long.parseLong(note.get("id"));
                    checkCameraPermission();
                });

                ImageButton buttonDetails = view.findViewById(R.id.buttonDetails);
                buttonDetails.setOnClickListener(v -> {
                    HashMap<String, String> selectedTask = notesList.get(position);

                    Intent detailIntent = new Intent(MainActivity.this, TaskDetailActivity.class);
                    detailIntent.putExtra("task_id", selectedTask.get("id"));
                    detailIntent.putExtra("task_text", selectedTask.get("text"));
                    detailIntent.putExtra("task_date", selectedTask.get("datetime"));
                    detailIntent.putExtra("photo_path", selectedTask.get("photo_path"));

                    startActivity(detailIntent);
                });


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
                try {
                    // Ajoutez des logs pour le débogage
                    Log.d("DEBUG", "Ajout d'une nouvelle note: " + noteText);

                    String datetime = dateTimeFormat.format(selectedDateTime.getTime());
                    long id = taskDao.insertTask(noteText, datetime, null);
                    checkNotificationPermission();
                    scheduleNotification(noteText,selectedDateTime.getTimeInMillis());

                    HashMap<String, String> task = new HashMap<>();
                    task.put("id", String.valueOf(id));
                    task.put("text", noteText);
                    task.put("datetime", datetime);
                    task.put("photo_path", "");

                    notesList.add(task);
                    adapter.notifyDataSetChanged();
                    editTextNote.setText("");

                    Log.d("DEBUG", "Note ajoutée avec succès, ID: " + id);
                } catch (Exception e) {
                    Log.e("ERROR", "Erreur lors de l'ajout", e);
                    Toast.makeText(this, "Erreur: " + e.getMessage(), Toast.LENGTH_LONG).show();
                }
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
    private void checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.CAMERA},
                    REQUEST_CAMERA_PERMISSION
            );
        } else {
            dispatchTakePictureIntent();
        }
    }

    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                ex.printStackTrace();
            }

            if (photoFile != null) {
                Uri photoURI = FileProvider.getUriForFile(this,
                        "com.example.quicknotes.fileprovider",
                        photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
            }
        }
    }
    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        // Sauvegarder le chemin du fichier pour une utilisation ultérieure
        currentPhotoPath = image.getAbsolutePath();
        return image;
    }
    // Gérer le résultat de la prise de photo
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_IMAGE_CAPTURE) {
            if (resultCode == RESULT_OK) {
                // Mettre à jour la tâche avec le chemin de la photo
                if (currentTaskIdForPhoto != -1 && currentPhotoPath != null) {
                    taskDao.updateTaskPhoto(currentTaskIdForPhoto, currentPhotoPath);
                    // Mettre à jour la liste
                    for (HashMap<String, String> task : notesList) {
                        if (task.get("id").equals(String.valueOf(currentTaskIdForPhoto))) {
                            task.put("photo_path", currentPhotoPath);
                            break;
                        }
                    }
                    adapter.notifyDataSetChanged();
                }
            }else {
                Toast.makeText(this, "La prise de photo a échoué", Toast.LENGTH_SHORT).show();
            }
        }
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                dispatchTakePictureIntent();
            } else {
                Toast.makeText(this, "Camera permission is required to take photos", Toast.LENGTH_SHORT).show();
            }
        }
    }





}