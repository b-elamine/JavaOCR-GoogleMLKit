package com.example.textrecognitionapp.signDetectionPipeline;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ImageProcessor {

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
        int classIdSign = 0; // Assuming the "sign" class has ID 0
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

            int classId = classIdSign; // Only process the "sign" class
            float classProb = output[0][probabilityStartIndex][i];

            if (classProb > confidenceThreshold) {
                detections.add(new Detection(centerX - width / 2, centerY - height / 2, width, height, classProb, classId));
            }
        }

        List<Detection> nmsDetections = applyNMS(detections, iouThreshold);

        if (nmsDetections.isEmpty()) {
            return originalBitmap; // No detections, return original image
        }

        // Assuming we're interested in the first detection after NMS
        Detection detection = nmsDetections.get(0);

        int left = Math.max(0, (int) detection.x);
        int top = Math.max(0, (int) detection.y);
        int right = Math.min(originalWidth, (int) (detection.x + detection.width));
        int bottom = Math.min(originalHeight, (int) (detection.y + detection.height));

        // Crop the original bitmap to the bounding box of the detected object
        Bitmap croppedBitmap = Bitmap.createBitmap(originalBitmap, left, top, right - left, bottom - top);

        // Optionally draw the bounding box and label on the cropped image
        Bitmap mutableBitmap = croppedBitmap.copy(Bitmap.Config.ARGB_8888, true);
        Canvas canvas = new Canvas(mutableBitmap);
        Paint paint = new Paint();
        paint.setColor(Color.RED);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(2);
        Paint textPaint = new Paint();
        textPaint.setColor(Color.RED);
        textPaint.setTextSize(20);

        canvas.drawRect(0, 0, right - left, bottom - top, paint);
        String className = classNames.get(detection.classId);
        canvas.drawText(className + ": " + String.format("%.2f", detection.confidence), 10, 30, textPaint);

        return mutableBitmap;
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

}
