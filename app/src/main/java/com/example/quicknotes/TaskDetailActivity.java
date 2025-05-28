package com.example.quicknotes;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class TaskDetailActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_task_detail);

        // Récupérer les données de l'Intent
        Intent intent = getIntent();
        String text = intent.getStringExtra("task_text");
        String datetime = intent.getStringExtra("task_date");
        String photoPath = intent.getStringExtra("photo_path");

        // Initialiser les vues
        TextView textView = findViewById(R.id.textViewDetail);
        TextView dateView = findViewById(R.id.dateViewDetail);
        ImageView imageView = findViewById(R.id.imageViewDetail);

        // Afficher les données
        if (text != null) {
            textView.setText(text);
        } else {
            textView.setText("Pas de texte disponible");
        }

        if (datetime != null) {
            dateView.setText(datetime);
        } else {
            dateView.setText("Pas de date disponible");
        }

        // Afficher l'image si elle existe
        if (photoPath != null && !photoPath.isEmpty()) {
            try {
                Bitmap bitmap = BitmapFactory.decodeFile(photoPath);
                if (bitmap != null) {
                    imageView.setImageBitmap(bitmap);
                } else {
                    imageView.setImageResource(android.R.drawable.ic_menu_report_image);
                }
            } catch (Exception e) {
                e.printStackTrace();
                imageView.setImageResource(android.R.drawable.ic_menu_report_image);
            }
        } else {
            imageView.setVisibility(View.GONE);
        }
    }
}