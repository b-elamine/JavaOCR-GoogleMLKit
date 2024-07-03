package com.example.textrecognitionapp;


import android.content.Context;
import android.graphics.Bitmap;
import android.widget.Toast;

import androidx.appcompat.widget.AppCompatTextView;
import androidx.lifecycle.ViewModel;

import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import java.util.List;

public class MainViewModel extends ViewModel {

    public void textRecognizer(Context context, AppCompatTextView textView, Bitmap bitmap) {
        InputImage image = InputImage.fromBitmap(bitmap, 0);
        TextRecognizer recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);

        recognizer.process(image)
                .addOnSuccessListener(visionText -> processText(context, textView, visionText))
                .addOnFailureListener(e ->
                        Toast.makeText(context, "Text could not be read!", Toast.LENGTH_SHORT).show()
                );
    }

    private void processText(Context context, AppCompatTextView textView, Text visionText) {
        List<Text.TextBlock> blocks = visionText.getTextBlocks();

        if (blocks.size() == 0) {
            Toast.makeText(context, "No Text Detected in Image!", Toast.LENGTH_SHORT).show();
            return;
        }

        StringBuilder text = new StringBuilder();
        for (Text.TextBlock block : blocks) {
            for (Text.Line line : block.getLines()) {
                for (Text.Element element : line.getElements()) {
                    text.append(element.getText()).append(" ");
                }
            }
        }
        textView.setText(text.toString());
    }
}
