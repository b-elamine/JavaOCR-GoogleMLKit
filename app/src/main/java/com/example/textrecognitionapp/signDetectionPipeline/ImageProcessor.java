package com.example.textrecognitionapp.signDetectionPipeline;

import android.graphics.Bitmap;
import android.graphics.RectF;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ImageProcessor {

    private static final String TAG = "ImageProcessor"; // Tag for logging

    public float[][][][] preprocessImage(Bitmap bitmap) {
        Bitmap resizedBitmap = Bitmap.createScaledBitmap(bitmap, 640, 640, true);
        float[][][][] input = new float[1][3][640][640];
        for (int y = 0; y < 640; y++) {
            for (int x = 0; x < 640; x++) {
                int pixel = resizedBitmap.getPixel(x, y);
                input[0][0][y][x] = (pixel >> 16 & 0xFF) / 255.0f; // R
                input[0][1][y][x] = (pixel >> 8 & 0xFF) / 255.0f;  // G
                input[0][2][y][x] = (pixel & 0xFF) / 255.0f;       // B
            }
        }
        return input;
    }

    public Bitmap processOutput(float[][][] output, Bitmap originalBitmap, List<String> classNames, float confidenceThreshold, float iouThreshold) {
        int numDetections = output[0][0].length;
        int numClasses = output[0].length - 4;
        int probabilityStartIndex = 4;

        int originalWidth = originalBitmap.getWidth();
        int originalHeight = originalBitmap.getHeight();
        float scaleX = originalWidth / 640.0f;
        float scaleY = originalHeight / 640.0f;

        List<Detection> detections = new ArrayList<>();

        for (int i = 0; i < numDetections; i++) {
            float centerX = output[0][0][i] * scaleX;
            float centerY = output[0][1][i] * scaleY;
            float width = output[0][2][i] * scaleX;
            float height = output[0][3][i] * scaleY;

            int classId = -1;
            float maxClassProb = 0;
            for (int j = probabilityStartIndex; j < probabilityStartIndex + numClasses; j++) {
                float classProb = output[0][j][i];
                if (classProb > maxClassProb) {
                    maxClassProb = classProb;
                    classId = j - probabilityStartIndex;
                }
            }

            if (maxClassProb > confidenceThreshold) {
                detections.add(new Detection(centerX - width / 2, centerY - height / 2, width, height, maxClassProb, classId));
            }
        }

        Log.d("Detection", "Total detections before NMS: " + detections.size());
        List<Detection> nmsDetections = applyNMS(detections, iouThreshold);
        Log.d("Detection", "Total detections after NMS: " + nmsDetections.size());

        if (!nmsDetections.isEmpty()) {
            Detection detection = nmsDetections.get(0);
            Log.d("Detection", "NMS Detection: " + detection.toString());
            float left = detection.x;
            float top = detection.y;
            float right = detection.x + detection.width;
            float bottom = detection.y + detection.height;

            int cropLeft = Math.max(0, Math.round(left));
            int cropTop = Math.max(0, Math.round(top));
            int cropWidth = Math.min(originalWidth, Math.round(right)) - cropLeft;
            int cropHeight = Math.min(originalHeight, Math.round(bottom)) - cropTop;

            return Bitmap.createBitmap(originalBitmap, cropLeft, cropTop, cropWidth, cropHeight);
        }

        return originalBitmap; // Return original bitmap if no detections are found
    }


    private List<Detection> applyNMS(List<Detection> detections, float iouThreshold) {
        List<Detection> nmsDetections = new ArrayList<>();
        Collections.sort(detections, (d1, d2) -> Float.compare(d2.confidence, d1.confidence));

        while (!detections.isEmpty()) {
            Detection bestDetection = detections.remove(0);
            nmsDetections.add(bestDetection);

            detections.removeIf(detection -> calculateIoU(bestDetection, detection) > iouThreshold);
        }

        return nmsDetections;
    }



    private float calculateIoU(Detection d1, Detection d2) {
        RectF rect1 = d1.toRectF();
        RectF rect2 = d2.toRectF();

        float intersectionLeft = Math.max(rect1.left, rect2.left);
        float intersectionTop = Math.max(rect1.top, rect2.top);
        float intersectionRight = Math.min(rect1.right, rect2.right);
        float intersectionBottom = Math.min(rect1.bottom, rect2.bottom);

        if (intersectionLeft < intersectionRight && intersectionTop < intersectionBottom) {
            float intersectionArea = (intersectionRight - intersectionLeft) * (intersectionBottom - intersectionTop);
            float unionArea = d1.getArea() + d2.getArea() - intersectionArea;
            return intersectionArea / unionArea;
        }

        return 0;
    }

    private void saveCroppedImage(Bitmap bitmap) {
        try {
            File file = new File(Environment.getExternalStorageDirectory(), "cropped_image.jpg");
            FileOutputStream out = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out);
            out.flush();
            out.close();
            Log.d("ImageProcessor", "Cropped image saved to " + file.getAbsolutePath());
        } catch (IOException e) {
            Log.e("ImageProcessor", "Failed to save cropped image", e);
        }
    }

}
