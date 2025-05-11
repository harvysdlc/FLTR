package com.example.fltr;

import android.annotation.SuppressLint;
import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

public class AudioEngine {
    public static final int SAMPLE_RATE = 44100;
    private static final int BUFFER_SIZE = 1024;
    private static final int SILENCE_THRESHOLD = 5000;
    private static final int TRIM_THRESHOLD = 3000;
    private static final int SILENCE_DURATION = 20;
    private static final int MAX_AMPLITUDE = 16384;

    private boolean isRecording;
    private RecordingCallback callback;
    private short[] lastTrimmedPcm;

    public interface RecordingCallback {
        void onRecordingFinished(float[][] mfcc);
        void onError(Exception e);
    }

    public void startRecording(RecordingCallback callback) {
        this.callback = callback;
        new Thread(this::recordLoop).start();
    }

    private void recordLoop() {
        @SuppressLint("MissingPermission") AudioRecord recorder = new AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                AudioRecord.getMinBufferSize(SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
        );

        recorder.startRecording();
        isRecording = true;

        List<Short> recordedData = new ArrayList<>();
        short[] buffer = new short[BUFFER_SIZE];
        boolean triggered = false;
        int silenceCounter = 0;

        while (isRecording) {
            int read = recorder.read(buffer, 0, BUFFER_SIZE);
            if (read <= 0) continue;

            short[] chunk = new short[read];
            System.arraycopy(buffer, 0, chunk, 0, read);

            int max = getMaxAmplitude(chunk);
            boolean silent = max < SILENCE_THRESHOLD;

            if (!triggered && !silent) {
                triggered = true;
                Log.d("AudioEngine", "Voice detected. Recording started.");
            }

            if (triggered) {
                for (short val : chunk) recordedData.add(val);
                silenceCounter = silent ? silenceCounter + 1 : 0;

                if (silenceCounter > SILENCE_DURATION) {
                    Log.d("AudioEngine", "Silence detected. Stopping recording.");
                    break;
                }
            }
        }

        recorder.stop();
        recorder.release();

        short[] rawPcm = toShortArray(recordedData);
        if (rawPcm.length == 0) {
            callback.onError(new IllegalStateException("No audio recorded."));
            return;
        }

        short[] normalized = normalize(rawPcm);
        short[] trimmed = trimSilenceStart(normalized);
        lastTrimmedPcm = trimmed;

        float[][] mfcc = MFCCExtractor.extractMFCC(trimmed, SAMPLE_RATE);
        if (mfcc == null || mfcc.length == 0) {
            callback.onError(new IllegalStateException("MFCC extraction returned empty."));
        } else {
            callback.onRecordingFinished(mfcc);
        }
    }

    private int getMaxAmplitude(short[] buffer) {
        int max = 0;
        for (short val : buffer) {
            max = Math.max(max, Math.abs(val));
        }
        return max;
    }

    private short[] normalize(short[] data) {
        int max = getMaxAmplitude(data);
        if (max == 0) return data;

        float factor = (float) MAX_AMPLITUDE / max;
        short[] normalized = new short[data.length];
        for (int i = 0; i < data.length; i++) {
            normalized[i] = (short) (data[i] * factor);
        }
        return normalized;
    }

    private short[] trimSilenceStart(short[] data) {
        int start = 0;
        while (start < data.length && Math.abs(data[start]) < TRIM_THRESHOLD) {
            start++;
        }
        short[] trimmed = new short[data.length - start];
        System.arraycopy(data, start, trimmed, 0, trimmed.length);
        return trimmed;
    }

    private short[] toShortArray(List<Short> list) {
        short[] array = new short[list.size()];
        for (int i = 0; i < list.size(); i++) array[i] = list.get(i);
        return array;
    }

    public byte[] getLastTrimmedPcm() {
        if (lastTrimmedPcm == null) return null;
        byte[] bytes = new byte[lastTrimmedPcm.length * 2];
        for (int i = 0; i < lastTrimmedPcm.length; i++) {
            bytes[2 * i] = (byte) (lastTrimmedPcm[i] & 0xFF);
            bytes[2 * i + 1] = (byte) ((lastTrimmedPcm[i] >> 8) & 0xFF);
        }
        return bytes;
    }
}
