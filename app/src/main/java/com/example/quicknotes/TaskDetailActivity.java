package com.example.quicknotes;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class TaskDetailActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_task_detail);

        String text = getIntent().getStringExtra("text");
        String datetime = getIntent().getStringExtra("datetime");
        String photoPath = getIntent().getStringExtra("photo_path");

        TextView textView = findViewById(R.id.textViewDetail);
        TextView dateView = findViewById(R.id.dateViewDetail);
        ImageView imageView = findViewById(R.id.imageViewDetail);

        textView.setText(text);
        dateView.setText(datetime);

        if (photoPath != null && !photoPath.isEmpty()) {
            Bitmap bitmap = BitmapFactory.decodeFile(photoPath);
            imageView.setImageBitmap(bitmap);
        }
    }
}
