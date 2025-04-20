package com.example.fltr;

import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.AudioProcessor;
import be.tarsos.dsp.io.TarsosDSPAudioFormat;
import be.tarsos.dsp.mfcc.MFCC;
import be.tarsos.dsp.util.fft.FFT;

import java.util.ArrayList;
import java.util.List;

public class TarsosMFCC {
    private final int sampleRate;
    private final int bufferSize;
    private final int hopSize;
    private final int numMfcc;
    private final int maxFrames;

    public TarsosMFCC(int sampleRate, int bufferSize, int hopSize, int numMfcc, int maxFrames) {
        this.sampleRate = sampleRate;
        this.bufferSize = bufferSize;
        this.hopSize = hopSize;
        this.numMfcc = numMfcc;
        this.maxFrames = maxFrames;
    }

    public float[][] extractFromFloatArray(float[] audioBuffer) {
        MFCC mfccProcessor = new MFCC(bufferSize, sampleRate, numMfcc, 40, 300, sampleRate / 2);
        TarsosDSPAudioFormat format = new TarsosDSPAudioFormat(sampleRate, 16, 1, true, false);

        List<float[]> mfccList = new ArrayList<>();
        int totalSamples = audioBuffer.length;
        int position = 0;

        while (position + bufferSize <= totalSamples) {
            float[] buffer = new float[bufferSize];
            System.arraycopy(audioBuffer, position, buffer, 0, bufferSize);
            AudioEvent audioEvent = new AudioEvent(format);  // Fixed constructor
            audioEvent.setFloatBuffer(buffer);
            mfccProcessor.process(audioEvent);
            mfccList.add(mfccProcessor.getMFCC());
            position += hopSize;
        }

        // Pad or trim to match exactly maxFrames
        float[][] mfccs = new float[maxFrames][numMfcc];

        for (int i = 0; i < maxFrames; i++) {
            if (i < mfccList.size()) {
                System.arraycopy(mfccList.get(i), 0, mfccs[i], 0, numMfcc);
            } else {
                // Padding with zeros
                for (int j = 0; j < numMfcc; j++) {
                    mfccs[i][j] = 0f;
                }
            }
        }

        return mfccs;
    }


}
