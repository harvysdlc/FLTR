package com.example.fltr;

import android.content.Context;
import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.support.common.FileUtil;

import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.util.List;

public class InferenceEngine {

    private final Interpreter tflite;
    private final List<String> labels;

    public InferenceEngine(Context context) {
        try {
            MappedByteBuffer model = FileUtil.loadMappedFile(context, "model_gan.tflite");
            tflite = new Interpreter(model);
            labels = FileUtil.loadLabels(context, "labels.txt"); // ✅ load labels from assets
        } catch (IOException e) {
            throw new RuntimeException("Failed to load model or labels", e);
        }
    }

    public String predict(float[][][] modelInput) {
        if (modelInput == null || modelInput.length == 0 || modelInput[0].length == 0 || modelInput[0][0].length == 0) {
            throw new IllegalArgumentException("Invalid input shape for model. Received: "
                    + (modelInput == null ? "null" : modelInput.length + "x" + modelInput[0].length + "x" + modelInput[0][0].length));
        }

        float[][] output = new float[1][labels.size()];
        tflite.run(modelInput, output);

        int bestIndex = 0;
        float max = output[0][0];
        for (int i = 1; i < output[0].length; i++) {
            if (output[0][i] > max) {
                max = output[0][i];
                bestIndex = i;
            }
        }

        return getLabel(bestIndex);
    }


    private String getLabel(int index) {
        return (index >= 0 && index < labels.size()) ? labels.get(index) : "unknown";
    }
}
