package com.example.textrecognitionapp;

import android.Manifest;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

import com.example.textrecognitionapp.databinding.ActivityMainBinding;
import com.example.textrecognitionapp.signDetectionPipeline.ImageProcessor;
import com.example.textrecognitionapp.signDetectionPipeline.InferenceModel;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private MainViewModel viewModel;
    private Uri detectImageUri;
    private Bitmap detectImage;
    private List<String> classNames;
    private ImageProcessor imageProcessor;
    private InferenceModel yolo;
    private AppCompatImageView viewResultImage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        viewResultImage = findViewById(R.id.ViewResultImage);
        viewModel = new ViewModelProvider(this).get(MainViewModel.class);



        // Class names (only sign)
        classNames = new ArrayList<>();
        classNames.add("sign");

        imageProcessor = new ImageProcessor();
        yolo = new InferenceModel(this);

        binding.textViewResult.setMovementMethod(new ScrollingMovementMethod());
        binding.buttonCopy.bringToFront();
        copy();
        buttonClick();
    }

    private void buttonClick() {
        binding.buttonCamera.setOnClickListener(view -> controlCameraPermission());
        binding.buttonGallery.setOnClickListener(view -> controlGalleryPermission());
        binding.buttonDetect.setOnClickListener(view -> {
            if (detectImage != null) {
                Toast.makeText(this, "Re Recognition Image!",
                        Toast.LENGTH_SHORT).show();
                Bitmap signBitmap = signDetection(detectImage, 0.5f, 0.5f);
                setRecognitionTextFromBitmap(signBitmap);
                viewResultImage.setImageBitmap(signBitmap != null ? signBitmap : detectImage);
            }
        });
        binding.buttonShowImage.setOnClickListener(view -> {
            if (detectImage != null) {
                Bitmap signBitmap = signDetection(detectImage, 0.5f, 0.5f);
                // Show the detected image directly in the ImageView
                viewResultImage.setImageBitmap(signBitmap != null ? signBitmap : detectImage);
            } else {
                Toast.makeText(this, "No images have been selected!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void controlCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_CODE);
        } else {
            openCamera();
        }
    }

    private void openCamera() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        cameraLauncher.launch(intent);
    }

    private final ActivityResultLauncher<Intent> cameraLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK) {
                    detectImage = (Bitmap) result.getData().getExtras().get("data");

                    // Sign detection code:
                    Bitmap signBitmap = signDetection(detectImage, 0.5f, 0.5f);

                    setRecognitionTextFromBitmap(signBitmap);
                    viewResultImage.setImageBitmap(signBitmap != null ? signBitmap : detectImage);
                }
            });

    private void controlGalleryPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, GALLERY_PERMISSION_CODE);
        } else {
            openGallery();
        }
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        galleryLauncher.launch(intent);
    }

    private final ActivityResultLauncher<Intent> galleryLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getData()!=null){
                    Uri uri = result.getData().getData();
                    if (result.getResultCode() == RESULT_OK && uri != null) {
                        detectImageUri = uri;
                        try (InputStream inputStream = getContentResolver().openInputStream(detectImageUri)) {
                            detectImage = BitmapFactory.decodeStream(inputStream);
                            Bitmap signBitmap = signDetection(detectImage, 0.5f, 0.5f);
                            setRecognitionTextFromBitmap(signBitmap);
                            viewResultImage.setImageBitmap(signBitmap != null ? signBitmap : detectImage);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            });

    private void copy() {
        if (binding.textViewResult.getText() != null && !binding.textViewResult.getText().toString().isEmpty()) {
            binding.buttonCopy.setOnClickListener(view -> {
                ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("copied", binding.textViewResult.getText());
                clipboard.setPrimaryClip(clip);
                Toast.makeText(this, "Text Copied!", Toast.LENGTH_SHORT).show();
            });
        }
    }

    private void setRecognitionTextFromBitmap(Bitmap bitmap) {
        new Thread(() -> viewModel.textRecognizer(this, binding.textViewResult, bitmap)).start();
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == CAMERA_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openCamera();
            } else {
                Toast.makeText(this, "Camera Permission Denied!", Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == GALLERY_PERMISSION_CODE) {
            openGallery();

            for (int i = 0; i < grantResults.length; i++) {
                String permission = permissions[i];
                String status = (grantResults[i] == PackageManager.PERMISSION_GRANTED) ? "granted" : "denied";
                Log.d("Permissions", permission + ": " + status);
            }
        }
    }

    private Bitmap loadImageFromAssets(String fileName) {
        try {
            InputStream inputStream = getAssets().open(fileName);
            return BitmapFactory.decodeStream(inputStream);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public Bitmap signDetection (Bitmap inputImage, float confidenceThreshold, float iouThreshold) {
        // Sign detection code
        float[][][][] inputTensor = imageProcessor.preprocessImage(inputImage);
        float[][][] signResult = yolo.runInference(inputTensor);
        return imageProcessor.processOutput(signResult, inputImage, classNames, confidenceThreshold, iouThreshold);
    }

    private static final int CAMERA_PERMISSION_CODE = 0;
    private static final int GALLERY_PERMISSION_CODE = 1;
}
