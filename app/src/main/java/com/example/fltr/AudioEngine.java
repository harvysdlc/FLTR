package com.example.fltr;

import android.annotation.SuppressLint;
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

    private boolean isRecording;
    private RecordingCallback callback;
    private byte[] lastPcmBytes;

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

        if (trimmed.length < 1024) {
            callback.onError(new IllegalStateException("Trimmed audio too short."));
            return;
        }

        // Save for WAV export
        lastPcmBytes = shortsToBytes(trimmed);

        float[][] mfcc = CustomMFCC.extractMFCCs(trimmed);
        if (mfcc == null || mfcc.length == 0) {
            callback.onError(new IllegalStateException("MFCC extraction returned empty."));
        } else {
            callback.onRecordingFinished(mfcc);
        }
    }

    private int getMaxAmplitude(short[] buffer) {
        int max = 0;
        for (short val : buffer) {
            if (Math.abs(val) > max) max = Math.abs(val);
        }
        return max;
    }

    private short[] toShortArray(List<Short> list) {
        short[] result = new short[list.size()];
        for (int i = 0; i < list.size(); i++) result[i] = list.get(i);
        return result;
    }

    private short[] normalize(short[] input) {
        int max = getMaxAmplitude(input);
        if (max == 0) return input;

        float normFactor = 32767.0f / max;
        short[] out = new short[input.length];
        for (int i = 0; i < input.length; i++) {
            out[i] = (short) Math.max(Math.min(input[i] * normFactor, 32767), -32768);
        }
        return out;
    }

    private short[] trimSilenceStart(short[] audio) {
        int start = 0;
        for (int i = 0; i < audio.length; i++) {
            if (Math.abs(audio[i]) > TRIM_THRESHOLD) {
                start = i;
                break;
            }
        }
        int trimmedLength = audio.length - start;
        short[] trimmed = new short[trimmedLength];
        System.arraycopy(audio, start, trimmed, 0, trimmedLength);
        return trimmed;
    }

    private byte[] shortsToBytes(short[] data) {
        byte[] bytes = new byte[data.length * 2];
        for (int i = 0; i < data.length; i++) {
            bytes[i * 2] = (byte) (data[i] & 0xff);
            bytes[i * 2 + 1] = (byte) ((data[i] >> 8) & 0xff);
        }
        return bytes;
    }

    public byte[] getLastTrimmedPcm() {
        return lastPcmBytes;
    }
}
