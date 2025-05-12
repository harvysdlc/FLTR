package com.example.fltr;

import android.util.Log;

import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.io.UniversalAudioInputStream;
import be.tarsos.dsp.io.TarsosDSPAudioFormat;
import be.tarsos.dsp.mfcc.MFCC;
import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.AudioProcessor;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;

public class MFCCExtractor {
    private static final int SAMPLE_RATE = 44100;
    private static final int FFT_SIZE = 2048;
    private static final int HOP_SIZE = 512;
    private static final int NUM_MFCC = 13;
    private static final int NUM_MEL_BANDS = 40;
    private static final int TARGET_NUM_FRAMES = 177;  // For LSTM: [1,177,13]

    public static float[][] extractMFCCs(short[] pcmData) {
        Log.d("MFCCExtractor", "Input PCM length: " + pcmData.length);

        if (pcmData == null || pcmData.length < FFT_SIZE) {
            Log.e("MFCCExtractor", "PCM too short for MFCC extraction.");
            return new float[0][0];
        }

        // Convert short[] to byte[] (little-endian)
        byte[] byteData = new byte[pcmData.length * 2];
        for (int i = 0; i < pcmData.length; i++) {
            byteData[2 * i] = (byte) (pcmData[i] & 0xFF);           // LSB
            byteData[2 * i + 1] = (byte) ((pcmData[i] >> 8) & 0xFF); // MSB
        }

        // Prepare TarsosDSP format and stream
        TarsosDSPAudioFormat format = new TarsosDSPAudioFormat(
                SAMPLE_RATE, 16, 1, true, false); // PCM signed, little-endian

        ByteArrayInputStream bais = new ByteArrayInputStream(byteData);
        UniversalAudioInputStream audioStream = new UniversalAudioInputStream(bais, format);
        AudioDispatcher dispatcher = new AudioDispatcher(audioStream, FFT_SIZE, HOP_SIZE);

        // Initialize MFCC processor
        MFCC mfccProcessor = new MFCC(FFT_SIZE, SAMPLE_RATE, NUM_MFCC, NUM_MEL_BANDS, 300, SAMPLE_RATE / 2);
        List<float[]> mfccList = new ArrayList<>();

        // Attach processors
        dispatcher.addAudioProcessor(mfccProcessor);
        dispatcher.addAudioProcessor(new AudioProcessor() {
            @Override
            public boolean process(AudioEvent audioEvent) {
                float[] mfccs = mfccProcessor.getMFCC();
                if (mfccs != null) {
                    mfccList.add(mfccs.clone());
                }
                return true;
            }

            @Override
            public void processingFinished() {
                // No-op
            }
        });

        // Run dispatcher (blocking)
        dispatcher.run();

        Log.d("MFCCExtractor", "Extracted MFCC frames: " + mfccList.size());

        if (mfccList.isEmpty()) {
            Log.e("MFCCExtractor", "No MFCCs extracted.");
            return new float[0][0];
        }

        // Final MFCC output with padding/truncation
        float[][] mfccOutput = new float[TARGET_NUM_FRAMES][NUM_MFCC];
        int copyLen = Math.min(TARGET_NUM_FRAMES, mfccList.size());
        for (int i = 0; i < copyLen; i++) {
            System.arraycopy(mfccList.get(i), 0, mfccOutput[i], 0, NUM_MFCC);
        }

        Log.d("MFCCExtractor", "Final MFCC shape: [" + mfccOutput.length + "][" + NUM_MFCC + "]");
        return mfccOutput;
    }
}
