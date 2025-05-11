package com.example.fltr;

import java.util.ArrayList;
import java.util.List;

import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.mfcc.MFCC;
import be.tarsos.dsp.util.fft.FFT;
import be.tarsos.dsp.io.TarsosDSPAudioFormat;

public class MFCCExtractor {

    public static float[][] extractMFCC(short[] audioData, int sampleRate) {
        final int bufferSize = 1024;
        final int bufferOverlap = 512;

        // Return early if input is too short
        if (audioData == null || audioData.length < bufferSize) {
            System.out.println("Audio too short for MFCC extraction.");
            return null;
        }

        // Convert short[] to float[] in the range [-1.0, 1.0]
        float[] floatData = new float[audioData.length];
        for (int i = 0; i < audioData.length; i++) {
            floatData[i] = audioData[i] / 32768f;
        }

        List<float[]> mfccList = new ArrayList<>();
        MFCC mfccProcessor = new MFCC(bufferSize, sampleRate, 13, 40, 300, 8000);
        FFT fft = new FFT(bufferSize);

        TarsosDSPAudioFormat format = new TarsosDSPAudioFormat(sampleRate, 16, 1, true, false);

        for (int i = 0; i < floatData.length - bufferSize; i += bufferSize - bufferOverlap) {
            float[] buffer = new float[bufferSize];
            System.arraycopy(floatData, i, buffer, 0, bufferSize);

            float[] windowed = applyHammingWindow(buffer);
            fft.forwardTransform(windowed);

            // Allocate non-null arrays to hold FFT results
            float[] magnitude = new float[bufferSize / 2];
            float[] phase = new float[bufferSize / 2];
            fft.modulus(windowed, phase); // Proper usage with non-null arrays

            AudioEvent event = new AudioEvent(format);
            event.setFloatBuffer(windowed); // Use processed buffer

            mfccProcessor.process(event);
            float[] mfccs = mfccProcessor.getMFCC();

            if (mfccs != null) {
                float[] copy = new float[mfccs.length];
                System.arraycopy(mfccs, 0, copy, 0, mfccs.length);
                mfccList.add(copy);
            }
        }

        if (mfccList.isEmpty()) {
            System.out.println("No MFCC frames were extracted.");
            return null;
        }

        System.out.println("Generated MFCC frames: " + mfccList.size());
        return mfccList.toArray(new float[0][0]);
    }

    private static float[] applyHammingWindow(float[] buffer) {
        int N = buffer.length;
        for (int n = 0; n < N; n++) {
            buffer[n] *= 0.54f - 0.46f * Math.cos((2 * Math.PI * n) / (N - 1));
        }
        return buffer;
    }
}
