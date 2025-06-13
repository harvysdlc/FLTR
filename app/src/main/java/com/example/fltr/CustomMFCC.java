package com.example.fltr;

import android.util.Log;

import org.tensorflow.lite.Interpreter;

import org.tensorflow.lite.support.common.FileUtil;

import android.content.Context;

import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.util.Arrays;
import java.util.List;

public class CustomMFCC {
    private static final int SAMPLE_RATE = 44100;
    private static final int NUM_MFCC = 20;
    private static final int NUM_MELS = 40;
    private static final int FFT_SIZE = 2048;
    static final int HOP_SIZE = 512;
    private static final int TARGET_NUM_FRAMES = 221;
    private static final double PRE_EMPHASIS = 0.97;

    // Container class to hold MFCC data and original frame count
    public static class MfccResult {
        public final float[][] paddedMfcc;
        public final float[][] originalMfcc;
        public final int originalFrameCount;
        public final int sampleCount;

        public MfccResult(float[][] paddedMfcc, float[][] originalMfcc, int originalFrameCount, int sampleCount) {
            this.paddedMfcc = paddedMfcc;
            this.originalMfcc = originalMfcc;
            this.originalFrameCount = originalFrameCount;
            this.sampleCount = sampleCount;
        }
    }

    public static class InferenceHelper {
        public final Interpreter tflite;
        public final List<String> labels;

        public InferenceHelper(Context context) throws IOException {
            MappedByteBuffer model = FileUtil.loadMappedFile(context, "model.tflite");
            tflite = new Interpreter(model);
            labels = FileUtil.loadLabels(context, "labels.txt");
        }

        // New method to run inference
        public InferenceResult runInference(float[][] paddedMfcc) {
            long startTime = System.currentTimeMillis();

            int timeSteps = paddedMfcc.length;
            int mfccCount = paddedMfcc[0].length;

            // Prepare input in original orientation [1][timeSteps][mfccCount]
            float[][][] input = new float[1][timeSteps][mfccCount];
            for (int i = 0; i < timeSteps; i++) {
                for (int j = 0; j < mfccCount; j++) {
                    input[0][i][j] = paddedMfcc[i][j];
                }
            }

            Log.d("CustomMFCC", "Input shape: [" + input.length + "][" + input[0].length + "][" + input[0][0].length + "]");

            // Log sample input for debugging
            Log.d("CustomMFCC", "TFLite Input sample:");
            for (int i = 0; i < Math.min(3, mfccCount); i++) {
                Log.d("CustomMFCC", "Input MFCC " + i + ": " + Arrays.toString(Arrays.copyOf(input[0][i], Math.min(20, mfccCount))));
            }

            // Run inference
            float[][] output = new float[1][labels.size()];
            tflite.run(input, output);
            float[] confidences = output[0];

            // Get best prediction
            int bestIdx = argmax(confidences);
            float confidence = confidences[bestIdx];
            String predictedLabel = labels.get(bestIdx);

            Log.d("CustomMFCC", "Predicted class index: " + bestIdx);
            Log.d("CustomMFCC", "Predicted label: " + predictedLabel);
            Log.d("CustomMFCC", "Confidence: " + confidence);

            // Calculate processing time
            long endTime = System.currentTimeMillis();
            float processingTimeSec = (endTime - startTime) / 1000f;

            return new InferenceResult(bestIdx, predictedLabel, confidence, processingTimeSec);
        }

        private int argmax(float[] array) {
            int maxIdx = 0;
            float maxVal = array[0];
            for (int i = 1; i < array.length; i++) {
                if (array[i] > maxVal) {
                    maxVal = array[i];
                    maxIdx = i;
                }
            }
            return maxIdx;
        }
    }

    // Result class to hold inference results
    public static class InferenceResult {
        public final int bestIndex;
        public final String label;
        public final float confidence;
        public final float processingTimeSec;

        public InferenceResult(int bestIndex, String label, float confidence, float processingTimeSec) {
            this.bestIndex = bestIndex;
            this.label = label;
            this.confidence = confidence;
            this.processingTimeSec = processingTimeSec;
        }
    }

    public static MfccResult extractMFCCs(short[] pcm) {
        float[] signal = normalizeAndPreEmphasize(pcm);
        float[][] frames = frameSignal(signal);
        double[][] powerSpectrogram = computePowerSpectrogram(frames);
        double[][] melSpectrogram = applyMelFilterBank(powerSpectrogram);
        float[][] mfcc = dct(melSpectrogram);

        int originalFrameCount = mfcc.length;

        // Deep copy original MFCC (before padding)
        float[][] originalMfcc = new float[originalFrameCount][NUM_MFCC];
        for (int i = 0; i < originalFrameCount; i++) {
            System.arraycopy(mfcc[i], 0, originalMfcc[i], 0, NUM_MFCC);
        }

        // Pad/truncate to TARGET_NUM_FRAMES
        float[][] finalMFCC = new float[TARGET_NUM_FRAMES][NUM_MFCC];
        int copyLen = Math.min(TARGET_NUM_FRAMES, mfcc.length);
        for (int i = 0; i < copyLen; i++) {
            System.arraycopy(mfcc[i], 0, finalMFCC[i], 0, NUM_MFCC);
        }

        Log.d("CustomMFCC", "Extracted MFCC shape: [" + finalMFCC.length + "][" + NUM_MFCC + "]");
        Log.d("CustomMFCC", "Original frame count: " + originalFrameCount);

        return new MfccResult(finalMFCC, originalMfcc, originalFrameCount, pcm.length);
    }

    private static float[] normalizeAndPreEmphasize(short[] pcm) {
        float[] floatSignal = new float[pcm.length];
        for (int i = 0; i < pcm.length; i++) {
            floatSignal[i] = pcm[i] / 32768.0f;
        }

        float[] preEmphasized = new float[floatSignal.length];
        preEmphasized[0] = floatSignal[0];
        for (int i = 1; i < floatSignal.length; i++) {
            preEmphasized[i] = (float) (floatSignal[i] - PRE_EMPHASIS * floatSignal[i - 1]);
        }

        return preEmphasized;
    }

    private static float[][] frameSignal(float[] signal) {
        int numFrames = 1 + (signal.length - FFT_SIZE) / HOP_SIZE;
        float[][] frames = new float[numFrames][FFT_SIZE];

        for (int i = 0; i < numFrames; i++) {
            int start = i * HOP_SIZE;
            for (int j = 0; j < FFT_SIZE; j++) {
                frames[i][j] = (start + j < signal.length) ?
                        (float) (signal[start + j] * (0.54 - 0.46 * Math.cos(2 * Math.PI * j / (FFT_SIZE - 1)))) :
                        0;
            }
        }
        return frames;
    }

    private static double[][] computePowerSpectrogram(float[][] frames) {
        int numFrames = frames.length;
        double[][] powerSpec = new double[numFrames][FFT_SIZE / 2 + 1];

        for (int i = 0; i < numFrames; i++) {
            double[] real = new double[FFT_SIZE];
            double[] imag = new double[FFT_SIZE];
            for (int j = 0; j < FFT_SIZE; j++) {
                real[j] = frames[i][j];
                imag[j] = 0.0;
            }
            fft(real, imag);

            for (int k = 0; k < FFT_SIZE / 2 + 1; k++) {
                powerSpec[i][k] = (real[k] * real[k] + imag[k] * imag[k]) / FFT_SIZE;
            }
        }
        return powerSpec;
    }

    private static double[][] applyMelFilterBank(double[][] powerSpec) {
        int nFrames = powerSpec.length;
        double[][] melSpec = new double[nFrames][NUM_MELS];

        double[] melPoints = new double[NUM_MELS + 2];
        double fMin = 0, fMax = SAMPLE_RATE / 2;
        double melMin = hzToMel(fMin);
        double melMax = hzToMel(fMax);
        for (int i = 0; i < melPoints.length; i++) {
            melPoints[i] = melToHz(melMin + (melMax - melMin) * i / (NUM_MELS + 1));
        }

        int[] bin = new int[melPoints.length];
        for (int i = 0; i < melPoints.length; i++) {
            bin[i] = (int) Math.floor((FFT_SIZE + 1) * melPoints[i] / SAMPLE_RATE);
        }

        for (int i = 0; i < nFrames; i++) {
            for (int m = 1; m <= NUM_MELS; m++) {
                double sum = 0;
                for (int k = bin[m - 1]; k < bin[m]; k++) {
                    sum += ((k - bin[m - 1]) / (double) (bin[m] - bin[m - 1])) * powerSpec[i][k];
                }
                for (int k = bin[m]; k < bin[m + 1]; k++) {
                    sum += (1 - (k - bin[m]) / (double) (bin[m + 1] - bin[m])) * powerSpec[i][k];
                }
                melSpec[i][m - 1] = Math.max(sum, 1e-10); // avoid log(0)
            }
        }

        // Convert to log scale
        for (int i = 0; i < melSpec.length; i++) {
            for (int j = 0; j < melSpec[i].length; j++) {
                melSpec[i][j] = Math.log(melSpec[i][j]);
            }
        }

        return melSpec;
    }

    private static float[][] dct(double[][] melSpec) {
        int nFrames = melSpec.length;
        float[][] mfcc = new float[nFrames][NUM_MFCC];

        for (int i = 0; i < nFrames; i++) {
            for (int k = 0; k < NUM_MFCC; k++) {
                double sum = 0;
                for (int n = 0; n < NUM_MELS; n++) {
                    sum += melSpec[i][n] * Math.cos(Math.PI * k * (2 * n + 1) / (2.0 * NUM_MELS));
                }
                mfcc[i][k] = (float) sum;
            }
        }
        return mfcc;
    }

    // FFT based on Cooley-Tukey (in-place, radix-2 DIT)
    private static void fft(double[] real, double[] imag) {
        int n = real.length;
        if (Integer.bitCount(n) != 1) throw new IllegalArgumentException("FFT size must be power of 2");

        int logN = Integer.numberOfTrailingZeros(n);
        for (int i = 0; i < n; i++) {
            int j = Integer.reverse(i) >>> (32 - logN);
            if (j > i) {
                double tempReal = real[i], tempImag = imag[i];
                real[i] = real[j]; imag[i] = imag[j];
                real[j] = tempReal; imag[j] = tempImag;
            }
        }

        for (int s = 1; s <= logN; s++) {
            int m = 1 << s;
            double theta = -2 * Math.PI / m;
            double wReal = 1, wImag = 0;
            double uReal = Math.cos(theta), uImag = Math.sin(theta);

            for (int j = 0; j < m / 2; j++) {
                for (int k = j; k < n; k += m) {
                    int t = k + m / 2;
                    double tReal = wReal * real[t] - wImag * imag[t];
                    double tImag = wReal * imag[t] + wImag * real[t];
                    real[t] = real[k] - tReal;
                    imag[t] = imag[k] - tImag;
                    real[k] += tReal;
                    imag[k] += tImag;
                }
                double tmpReal = wReal * uReal - wImag * uImag;
                wImag = wReal * uImag + wImag * uReal;
                wReal = tmpReal;
            }
        }
    }

    private static double hzToMel(double hz) {
        return 2595 * Math.log10(1 + hz / 700.0);
    }

    private static double melToHz(double mel) {
        return 700 * (Math.pow(10, mel / 2595.0) - 1);
    }
}

