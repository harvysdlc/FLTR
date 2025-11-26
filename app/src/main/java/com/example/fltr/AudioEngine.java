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

    // Default fallback thresholds
    private static final int DEFAULT_SILENCE_THRESHOLD = 5000;
    private static final int TRIM_THRESHOLD = 3000;
    private static final int SILENCE_DURATION = 20; // ~20 buffers ≈ 0.4 sec

    private int adaptiveThreshold = -1; // -1 = not calibrated

    private boolean isRecording;
    private RecordingCallback callback;
    private CalibrationCallback calibrationCallback;

    private byte[] lastPcmBytes;

    public interface RecordingCallback {
        void onRecordingFinished(short[] audioData);
        void onError(Exception e);
    }

    public interface CalibrationCallback {
        void onCalibrationComplete(int threshold);
        void onCalibrationError(Exception e);
    }

    // ---------------------------------------------------------
    // CALIBRATION MODE (1–2 seconds of background sampling)
    // ---------------------------------------------------------
    public void startCalibration(CalibrationCallback cb) {
        this.calibrationCallback = cb;
        new Thread(this::calibrationLoop).start();
    }

    private void calibrationLoop() {
        @SuppressLint("MissingPermission") AudioRecord recorder = new AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                AudioRecord.getMinBufferSize(SAMPLE_RATE,
                        AudioFormat.CHANNEL_IN_MONO,
                        AudioFormat.ENCODING_PCM_16BIT)
        );

        short[] buffer = new short[BUFFER_SIZE];
        List<Integer> amplitudes = new ArrayList<>();

        recorder.startRecording();
        long startTime = System.currentTimeMillis();

        while (System.currentTimeMillis() - startTime < 1500) { // 1.5 sec calibration
            int read = recorder.read(buffer, 0, BUFFER_SIZE);
            if (read > 0) {
                int max = getMaxAmplitude(buffer);
                amplitudes.add(max);
            }
        }

        recorder.stop();
        recorder.release();

        if (amplitudes.isEmpty()) {
            calibrationCallback.onCalibrationError(new Exception("Calibration failed: no samples."));
            return;
        }

        int avg = 0;
        for (int a : amplitudes) avg += a;
        avg /= amplitudes.size();

        // Adaptive threshold = avg * multiplier
        adaptiveThreshold = (int) (avg * 2.5f);

        Log.d("AudioEngine", "Calibration complete. Avg=" + avg +
                " Threshold=" + adaptiveThreshold);

        calibrationCallback.onCalibrationComplete(adaptiveThreshold);
    }

    // ---------------------------------------------------------
    // MAIN RECORDING LOGIC
    // ---------------------------------------------------------
    public void startRecording(RecordingCallback callback) {
        this.callback = callback;
        new Thread(this::recordLoop).start();
    }

    private void recordLoop() {
        @SuppressLint("MissingPermission") AudioRecord recorder =
                new AudioRecord(MediaRecorder.AudioSource.MIC,
                        SAMPLE_RATE,
                        AudioFormat.CHANNEL_IN_MONO,
                        AudioFormat.ENCODING_PCM_16BIT,
                        AudioRecord.getMinBufferSize(SAMPLE_RATE,
                                AudioFormat.CHANNEL_IN_MONO,
                                AudioFormat.ENCODING_PCM_16BIT));

        recorder.startRecording();
        isRecording = true;

        List<Short> recordedData = new ArrayList<>();
        short[] buffer = new short[BUFFER_SIZE];
        boolean triggered = false;
        int silenceCounter = 0;

        int activeThreshold = (adaptiveThreshold > 0)
                ? adaptiveThreshold
                : DEFAULT_SILENCE_THRESHOLD;

        long startTime = System.currentTimeMillis();
        long maxRecordTimeMs = 5000; // 5 seconds max

        while (isRecording) {

            if (System.currentTimeMillis() - startTime > maxRecordTimeMs) {
                Log.d("AudioEngine", "Max 5 sec reached. Ending.");
                break;
            }

            int read = recorder.read(buffer, 0, BUFFER_SIZE);
            if (read <= 0) continue;

            short[] chunk = new short[read];
            System.arraycopy(buffer, 0, chunk, 0, read);

            int max = getMaxAmplitude(chunk);

            boolean silent = max < activeThreshold;

            if (!triggered && !silent) {
                triggered = true;
                Log.d("AudioEngine", "Voice detected. Recording started.");
            }

            if (triggered) {
                for (short s : chunk) recordedData.add(s);

                silenceCounter = silent ? silenceCounter + 1 : 0;

                if (silenceCounter > SILENCE_DURATION) {
                    Log.d("AudioEngine", "Silence detected. Stopping.");
                    break;
                }
            }
        }

        recorder.stop();
        recorder.release();

        short[] rawPcm = toShortArray(recordedData);

        if (rawPcm.length == 0) {
            callback.onError(new Exception("No audio recorded."));
            return;
        }

        short[] normalized = normalize(rawPcm);
        short[] trimmed = trimSilenceStart(normalized);

        if (trimmed.length < 1024) {
            callback.onError(new Exception("Audio too short after trim."));
            return;
        }

        lastPcmBytes = shortsToBytes(trimmed);
        callback.onRecordingFinished(trimmed);
    }

    // ---------------------------------------------------------
    // UTILITY METHODS
    // ---------------------------------------------------------
    private int getMaxAmplitude(short[] buffer) {
        int max = 0;
        for (short val : buffer) {
            max = Math.max(max, Math.abs(val));
        }
        return max;
    }

    private short[] toShortArray(List<Short> list) {
        short[] arr = new short[list.size()];
        for (int i = 0; i < list.size(); i++) arr[i] = list.get(i);
        return arr;
    }

    private short[] normalize(short[] audio) {
        int max = getMaxAmplitude(audio);
        if (max == 0) return audio;

        float factor = 32767f / max;
        short[] out = new short[audio.length];

        for (int i = 0; i < audio.length; i++) {
            out[i] = (short) Math.max(Math.min(audio[i] * factor, 32767), -32768);
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
        int len = audio.length - start;
        short[] trimmed = new short[len];
        System.arraycopy(audio, start, trimmed, 0, len);
        return trimmed;
    }

    private byte[] shortsToBytes(short[] data) {
        byte[] out = new byte[data.length * 2];
        for (int i = 0; i < data.length; i++) {
            out[i * 2] = (byte) (data[i] & 0xFF);
            out[i * 2 + 1] = (byte) ((data[i] >> 8) & 0xFF);
        }
        return out;
    }

    public byte[] getLastTrimmedPcm() {
        return lastPcmBytes;
    }

    public int getAdaptiveThreshold() {
        return adaptiveThreshold;
    }
}
