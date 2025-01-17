package com.example.textrecognitionapp.signDetectionPipeline;

import android.graphics.RectF;

public class Detection {
    float x, y, width, height, confidence;
    int classId;

    public Detection(float x, float y, float width, float height, float confidence, int classId) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.confidence = confidence;
        this.classId = classId;
    }

    public float getArea() {
        return width * height;
    }

    public RectF toRectF() {
        return new RectF(x, y, x + width, y + height);
    }

    @Override
    public String toString() {
        return "Detection{" +
                "x=" + x +
                ", y=" + y +
                ", width=" + width +
                ", height=" + height +
                ", confidence=" + confidence +
                ", classId=" + classId +
                '}';
    }
}
