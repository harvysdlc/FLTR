package com.example.fltr;

import android.content.Context;
import android.util.Log;

import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.support.common.FileUtil;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.List;
import java.util.Locale;

public class InferenceEngine {
    private static final String TAG = "InferenceEngine";
    private static final String DEFAULT_MODEL = "model_gan.tflite";
    private static final String DEFAULT_LABELS = "labels.txt";
    private static final int NUM_MFCC = 13;
    private static final int MAX_MFCC_LENGTH = 177;
    private static final int SAMPLE_RATE = 44100;

    public static final int DEFAULT_SAMPLE_RATE = SAMPLE_RATE;
    public static final int DEFAULT_NUM_MFCC = NUM_MFCC;
    public static final int DEFAULT_MAX_LENGTH = MAX_MFCC_LENGTH;

    private Interpreter tflite;
    private List<String> labels;

    /**
     * Convenience ctor: loads the default model + labels from assets.
     */
    public InferenceEngine(Context context) {
        this(context, DEFAULT_MODEL, DEFAULT_LABELS);
    }

    /**
     * Full ctor if you ever want to supply custom paths.
     */
    public InferenceEngine(Context context, String modelPath, String labelFile) {
        try {
            tflite = new Interpreter(FileUtil.loadMappedFile(context, modelPath));
            labels = FileUtil.loadLabels(context, labelFile);
            Log.d(TAG, "Model and labels loaded.");
        } catch (IOException e) {
            throw new RuntimeException("Failed to load TFLite model or labels", e);
        }
    }

    /**
     * Predicts a label from a 2D MFCC matrix [frames][coeffs].
     */
    public String predict(float[][] mfccMatrix) {
        // 1) flatten to [1, frames*coeffs]
        float[] flat = new float[MAX_MFCC_LENGTH * NUM_MFCC];
        int idx = 0;
        for (int i = 0; i < MAX_MFCC_LENGTH; i++) {
            for (int j = 0; j < NUM_MFCC; j++) {
                flat[idx++] = mfccMatrix[i][j];
            }
        }

        int maxFrames = Math.min(mfccMatrix.length, MAX_MFCC_LENGTH);
        for (int i = 0; i < maxFrames; i++) {
            for (int j = 0; j < NUM_MFCC; j++) {
                flat[i * NUM_MFCC + j] = mfccMatrix[i][j];
            }
        }
        // Zero-pad remaining values
        for (int i = maxFrames * NUM_MFCC; i < flat.length; i++) {
            flat[i] = 0f;
        }

        // 2) reshape to match model's input
        int[] shape = tflite.getInputTensor(0).shape(); // e.g. [1,177,13]
        int total = shape[1] * shape[2];
        ByteBuffer inputBuff = ByteBuffer.allocateDirect(total * 4).order(ByteOrder.nativeOrder());
        for (int i = 0; i < total; i++) {
            inputBuff.putFloat(flat[i]);
        }
        inputBuff.rewind();

        TensorBuffer inputTensor = TensorBuffer.createFixedSize(shape, org.tensorflow.lite.DataType.FLOAT32);
        inputTensor.loadBuffer(inputBuff);

        // 3) run
        float[][] output = new float[1][labels.size()];
        long start = System.currentTimeMillis();
        tflite.run(inputTensor.getBuffer(), output);
        long end = System.currentTimeMillis();
        Log.d(TAG, "Inference in " + (end - start) + "ms");

        // 4) find max
        int best = 0;
        float bestScore = output[0][0];
        for (int i = 1; i < output[0].length; i++) {
            if (output[0][i] > bestScore) {
                bestScore = output[0][i];
                best = i;
            }
        }

        String label = labels.get(best);
        return String.format(Locale.US, "%s (%.2f)", label, bestScore);
    }

    public void close() {
        if (tflite != null) tflite.close();
    }
}
