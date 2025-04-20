package com.example.fltr;

import android.util.Log;

import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.AudioProcessor;
import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.io.TarsosDSPAudioFormat;
import be.tarsos.dsp.io.UniversalAudioInputStream;
import be.tarsos.dsp.mfcc.MFCC;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;

public class MFCCProcessor {

    public static float[] extract(float[] audioData, int sampleRate, int numMfcc, int maxLength) {
        int fftSize = 2048;
        int hopSize = 512;
        int numMelBands = 128;

        // Convert float[] to byte[] PCM 16-bit little endian
        byte[] pcmBytes = AudioUtils.floatToPCM16(audioData);

        ByteArrayInputStream bais = new ByteArrayInputStream(pcmBytes);
        TarsosDSPAudioFormat format = new TarsosDSPAudioFormat(sampleRate, 16, 1, true, false);
        UniversalAudioInputStream stream = new UniversalAudioInputStream(bais, format);

        AudioDispatcher dispatcher = new AudioDispatcher(stream, fftSize, hopSize);

        List<float[]> mfccList = new ArrayList<>();

        MFCC mfccProcessor = new MFCC(fftSize, sampleRate, numMfcc, numMelBands, 300, sampleRate / 2);
        dispatcher.addAudioProcessor(mfccProcessor);
        dispatcher.addAudioProcessor(new AudioProcessor() {
            @Override
            public boolean process(AudioEvent audioEvent) {
                float[] mfcc = mfccProcessor.getMFCC();
                // Clone to avoid overwrite on next frame
                mfccList.add(mfcc.clone());
                return true;
            }

            @Override
            public void processingFinished() {
                // no-op
            }
        });

        dispatcher.run();

        Log.d("TarsosMFCC", "Extracted " + mfccList.size() + " MFCC frames");

        // Adjust to maxLength
        float[][] adjusted = new float[maxLength][numMfcc];
        for (int i = 0; i < maxLength; i++) {
            if (i < mfccList.size()) {
                adjusted[i] = mfccList.get(i);
            } else {
                adjusted[i] = new float[numMfcc];
            }
        }

        return flatten(adjusted);
    }

    private static float[] flatten(float[][] mfcc) {
        float[] flat = new float[mfcc.length * mfcc[0].length];
        int idx = 0;
        for (float[] row : mfcc) {
            for (float val : row) {
                flat[idx++] = val;
            }
        }
        return flat;
    }
}
