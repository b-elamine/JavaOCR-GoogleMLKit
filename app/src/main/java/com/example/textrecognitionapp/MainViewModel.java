package com.example.textrecognitionapp;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.widget.AppCompatTextView;
import androidx.lifecycle.ViewModel;

import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

        List<Map<String, Object>> elements = new ArrayList<>();
        for (Text.TextBlock block : blocks) {
            for (Text.Line line : block.getLines()) {
                for (Text.Element element : line.getElements()) {
                    Map<String, Object> elementData = new HashMap<>();
                    elementData.put("text", element.getText());
                    elementData.put("right", element.getBoundingBox().right);
                    elementData.put("left", element.getBoundingBox().left);
                    elementData.put("top", element.getBoundingBox().top);
                    elementData.put("bottom", element.getBoundingBox().bottom);
                    elementData.put("centerX", element.getBoundingBox().exactCenterX());
                    elementData.put("centerY", element.getBoundingBox().exactCenterY());
                    elementData.put("width", element.getBoundingBox().width());
                    elementData.put("height", element.getBoundingBox().height());
                    elements.add(elementData);
                }
            }
        }

        for (Map<String, Object> element : elements) {
            Log.d("TextRecognition", "Element data: " + element.toString());
        }

        // Applying post-processing for specific fuel price detection task to g

        List<Map<String, Object>> processedData = PostProcessor.processElements(elements);

        for (Map<String, Object> element : processedData) {
            Log.d("TextRecognition", "Processed Element data: " + element.toString());
        }

        StringBuilder resultText = new StringBuilder();
        for (Map<String, Object> label : processedData) {    // Change "processedData" by "elements" to get RAW results
            resultText.append(label.get("bounding_box").toString()).append("\n");
            /*System.out.println("Bbx Text :"+label.get("text"));
            System.out.println("Bbx LEFT : "+label.get("left"));
            System.out.println("Bbx RIGHT : "+label.get("right").toString());
            System.out.println("Bbx WIDTH : "+label.get("width").toString());
            System.out.println("Bbx HEIGHT : "+label.get("height").toString());*/
        }
        resultText.append("\n\nleft : The X coordinate of the left side of the rectangle\n" +
                          "top : The Y coordinate of the top of the rectangle\n" +
                          "right : The X coordinate of the right side of the rectangle\n" +
                          "bottom : The Y coordinate of the bottom of the rectangle");

        textView.setText(resultText.toString().trim());
    }
}
