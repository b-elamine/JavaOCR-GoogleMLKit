package com.example.textrecognitionapp;

import static com.example.textrecognitionapp.PostProcessor.processPrice;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Rect;
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

        if (blocks.isEmpty()) {
            Toast.makeText(context, "No Text Detected in Image!", Toast.LENGTH_SHORT).show();
            return;
        }

        List<Map<String, Object>> elements = new ArrayList<>();

        for (Text.TextBlock block : blocks) {
            for (Text.Line line : block.getLines()) {
                for (Text.Element element : line.getElements()) {
                    Rect boundingBox = element.getBoundingBox();
                    if (boundingBox != null) {
                        Map<String, Object> elementData = new HashMap<>();
                        elementData.put("text", element.getText());
                        elementData.put("right", boundingBox.right);
                        elementData.put("left", boundingBox.left);
                        elementData.put("top", boundingBox.top);
                        elementData.put("bottom", boundingBox.bottom);
                        elementData.put("centerX", boundingBox.exactCenterX());
                        elementData.put("centerY", boundingBox.exactCenterY());
                        elementData.put("width", boundingBox.width());
                        elementData.put("height", boundingBox.height());
                        elementData.put("conf", element.getConfidence());
                        elements.add(elementData);
                    }
                }
            }
        }

        for (Map<String, Object> element : elements) {
            Log.d("TextRecognition", "Element data: " + element.toString());
        }

        // Applying post-processing for specific fuel price detection task
        List<Map<String, Object>> processedLabels = PostProcessor.processElements(elements);
        List<Map<String, Object>> processedPrices = PostProcessor.processPrice(elements);

        System.out.println("Res.Labels : " + processedLabels);
        System.out.println("Res.Prices : " + processedPrices);


        List<Map<String, String>> matchedLabelsAndPrices = new ArrayList<>();

        StringBuilder resultText = new StringBuilder();

        for (Map<String, Object> processedLabel : processedLabels) { // Change "processedLabels" to "elements" for raw results

           /*

           ProcessedLabels Data format :
            {
                "text" : text // Combined and preprocessed text
                "bounding_box" :
                {
                    "text" : text,    // combined and preprocessed text
                    ...other bounding box info
                }
            }

            */

            Map<String, Object> label = (Map<String, Object>) processedLabel.get("bounding_box");
            Rect labelBox = createBoundingBox(label);
            String labelText = (String) label.get("text");

            for (Map<String, Object> processedPrice : processedPrices) {

                /*

           ProcessedPrices Data format :
            {
                "text" : text // preprocessed price
                "bounding_box" :
                {
                    "text" : text,    // Raw detected price
                    ...other bounding box info
                }
            }

            */

                Map<String, Object> price = (Map<String, Object>) processedPrice.get("bounding_box");
                System.out.println("LOL" + price);
                Rect priceBox = createBoundingBox(price);
                String priceText = (String) processedPrice.get("text");

                // Here we are implementing labels-prices mapping algorithm
                if ((   priceBox.left > labelBox.right &&
                        Math.abs(priceBox.centerY() - labelBox.centerY()) < labelBox.height())
                        ||
                        (priceBox.right < labelBox.left &&
                                Math.abs(priceBox.centerY() - labelBox.centerY()) < labelBox.height())
                        ) {
                    Map<String, String> matchedPair = new HashMap<>();
                    matchedPair.put("label", labelText);
                    matchedPair.put("price", priceText);
                    matchedLabelsAndPrices.add(matchedPair);
                    System.out.println("PRICES | Original Price : " + price.get("text") + ", " + "Processed Price : " + processedPrice.get("text"));
                    break;
                }
            }
        }

        for (Map<String, String> pair : matchedLabelsAndPrices) {
            resultText.append("Label : ").append(pair.get("label")).append(", Price : ").append(pair.get("price")).append("\n");
            System.out.println("Label : " + pair.get("label") + "Processed Price : " + pair.get("price"));
        }

        textView.setText(resultText.toString().trim());
    }

    private Rect createBoundingBox(Map<String, Object> boundingBoxData) {
        try {
            return new Rect(
                    (int) boundingBoxData.get("left"),
                    (int) boundingBoxData.get("top"),
                    (int) boundingBoxData.get("right"),
                    (int) boundingBoxData.get("bottom")
            );
        } catch (Exception e) {
            Log.e("TextRecognition", "Error creating bounding box: " + e.toString());
            return null;
        }
    }
}
