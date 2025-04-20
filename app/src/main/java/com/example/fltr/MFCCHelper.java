package com.example.fltr;


import java.util.ArrayList;
import java.util.List;

import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.AudioProcessor;
import be.tarsos.dsp.io.TarsosDSPAudioFormat;
import be.tarsos.dsp.mfcc.MFCC;
import be.tarsos.dsp.io.UniversalAudioInputStream;

import java.io.ByteArrayInputStream;
public class MFCCHelper {

    public static float[][] extractMFCC(float[] audioBuffer,
                                        int sampleRate,
                                        int numMfcc,
                                        int maxFrames,
                                        int bufferSize,
                                        int hopSize) {

        List<float[]> mfccList = new ArrayList<>();

        // Convert float[] to byte[] as input stream
        byte[] byteBuffer = floatTo16BitPCM(audioBuffer);
        ByteArrayInputStream inputStream = new ByteArrayInputStream(byteBuffer);

        TarsosDSPAudioFormat format = new TarsosDSPAudioFormat(sampleRate, 16, 1, true, false);
        UniversalAudioInputStream audioStream = new UniversalAudioInputStream(inputStream, format);

        AudioDispatcher dispatcher = new AudioDispatcher(audioStream, bufferSize, hopSize);

        MFCC mfcc = new MFCC(bufferSize, sampleRate, numMfcc, 40, 300, sampleRate / 2);
        dispatcher.addAudioProcessor(mfcc);
        dispatcher.addAudioProcessor(new AudioProcessor() {
            @Override
            public boolean process(AudioEvent audioEvent) {
                float[] mfccs = mfcc.getMFCC();
                mfccList.add(mfccs.clone()); // clone to avoid overwriting
                return true;
            }

            @Override
            public void processingFinished() {}
        });

        dispatcher.run();

        // Pad or truncate to match model expected input
        float[][] output = new float[maxFrames][numMfcc];
        for (int i = 0; i < maxFrames; i++) {
            if (i < mfccList.size()) {
                output[i] = mfccList.get(i);
            } else {
                output[i] = new float[numMfcc]; // zero padding
            }
        }

        return output;
    }

    private static byte[] floatTo16BitPCM(float[] buffer) {
        byte[] pcm = new byte[buffer.length * 2];
        for (int i = 0; i < buffer.length; i++) {
            short s = (short) Math.max(Math.min(buffer[i] * 32767f, 32767f), -32768f);
            pcm[i * 2] = (byte) (s & 0xFF);
            pcm[i * 2 + 1] = (byte) ((s >> 8) & 0xFF);
        }
        return pcm;
    }
}
