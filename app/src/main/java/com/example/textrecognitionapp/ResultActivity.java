package com.example.textrecognitionapp;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Pair;

import androidx.appcompat.app.AppCompatActivity;

import com.example.textrecognitionapp.databinding.ActivityResultBinding;

public class ResultActivity extends AppCompatActivity {

    private ActivityResultBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityResultBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        Pair<Bitmap, String> imagePair = getImageFromIntent();
        setImage(imagePair.second, imagePair.first);

        binding.buttonBack.setOnClickListener(view -> onBackPressed());
    }

    private void setImage(String uri, Bitmap bitmap) {
        binding.imageView.setImageDrawable(null);
        if (uri != null) {
            binding.imageView.setImageURI(Uri.parse(uri));
        } else {
            binding.imageView.setImageBitmap(bitmap);
        }
    }

    private Pair<Bitmap, String> getImageFromIntent() {
        Intent intent = getIntent();
        byte[] byteArray = intent.getByteArrayExtra("image");
        if (byteArray != null) {
            Bitmap bitmap = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.length);
            return new Pair<>(bitmap, null); // No URI provided
        }
        return new Pair<>(null, null); // No data provided
    }
}
