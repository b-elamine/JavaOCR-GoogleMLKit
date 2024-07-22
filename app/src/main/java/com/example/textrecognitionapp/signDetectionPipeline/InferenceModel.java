package com.example.textrecognitionapp.signDetectionPipeline;

import android.content.Context;

import com.example.textrecognitionapp.R;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;

import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtException;
import ai.onnxruntime.OrtSession;

public class InferenceModel {
    private OrtEnvironment env;
    private OrtSession session;

    public InferenceModel(Context context) {
        try {
            env = OrtEnvironment.getEnvironment();
            File modelFile = getModelFile(context, "best.onnx");
            session = env.createSession(modelFile.getPath());
        } catch (IOException | OrtException e) {
            e.printStackTrace();
        }
    }

    private File getModelFile(Context context, String modelName) throws IOException {
        InputStream inputStream = context.getResources().openRawResource(R.raw.best);
        File modelFile = new File(context.getFilesDir(), modelName);
        try(FileOutputStream outputStream = new FileOutputStream(modelFile)){
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
        }
        return  modelFile;
    }

    public float[][][] runInference(float[][][][] inputTensor) {
        try {
            OnnxTensor input = OnnxTensor.createTensor(env, inputTensor);
            OrtSession.Result result = session.run(Collections.singletonMap("images", input));
            float [][][] output = (float[][][]) result.get(0).getValue();
            return  output;
        } catch (OrtException e) {
            e.printStackTrace();
            return null;
        }
    }
}
