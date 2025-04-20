package com.example.fltr;

import android.content.Context;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class AudioProcessor {
    private static final String TAG = "AudioProcessor";

    public static final int SAMPLE_RATE = 44100;
    public static final int BUFFER_SIZE = 2048;
    public static final int HOP_SIZE = 512;
    private static final int NUM_MFCC = InferenceEngine.DEFAULT_NUM_MFCC;
    private static final int MAX_FRAMES = InferenceEngine.DEFAULT_MAX_LENGTH;
    public static final float TRIM_THRESHOLD = 5000f;

    private final Context context;
    private AudioRecord audioRecord;
    private ByteArrayOutputStream pcmStream;
    private boolean isRecording = false;
    private RecordingCallback callback;
    private byte[] lastTrimmedPcm;

    public interface RecordingCallback {
        void onRecordingFinished(RecordingResult result);
        void onError(Exception e);
    }

    public static class RecordingResult {
        public final float[][] fullMfcc;
        public final float[][] trimmedMfcc;
        public final boolean[] silenceMarkers;

        public RecordingResult(float[][] full, float[][] trimmed, boolean[] markers) {
            this.fullMfcc = full;
            this.trimmedMfcc = trimmed;
            this.silenceMarkers = markers;
        }
    }

    public AudioProcessor(Context context) {
        this.context = context;
    }

    public void startRecording(RecordingCallback cb) {
        this.callback = cb;
        pcmStream = new ByteArrayOutputStream();
        isRecording = true;

        if (context.checkSelfPermission(android.Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            callback.onError(new SecurityException("RECORD_AUDIO permission not granted"));
            return;
        }

        int minBufferSize = AudioRecord.getMinBufferSize(
                SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT
        );

        audioRecord = new AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                minBufferSize * 2
        );

        audioRecord.startRecording();

        new Thread(() -> {
            try {
                byte[] buffer = new byte[BUFFER_SIZE * 2];
                while (isRecording) {
                    int read = audioRecord.read(buffer, 0, buffer.length);
                    if (read > 0) {
                        pcmStream.write(buffer, 0, read);
                    }
                }

                audioRecord.stop();
                audioRecord.release();

                processRecording();

            } catch (Exception e) {
                Log.e(TAG, "Recording error", e);
                callback.onError(e);
            }
        }).start();
    }

    public void stopRecording() {
        isRecording = false;
    }

    private void processRecording() {
        try {
            byte[] rawPcm = pcmStream.toByteArray();
            short[] pcmShorts = byteArrayToShorts(rawPcm);
            float[] floatPcm = shortsToFloats(pcmShorts);

            // Full MFCC
            TarsosMFCC extractor = new TarsosMFCC(SAMPLE_RATE, BUFFER_SIZE, HOP_SIZE, NUM_MFCC, MAX_FRAMES);
            float[][] fullMfcc = extractor.extractFromFloatArray(floatPcm);
            fullMfcc = normalizeMfcc(fullMfcc);

            Log.d(TAG, "Full MFCC shape: " + fullMfcc.length + " x " + fullMfcc[0].length);

            // Trim leading silence
            int start = findVoiceStart(pcmShorts, TRIM_THRESHOLD);
            short[] trimmedShorts = new short[pcmShorts.length - start];
            System.arraycopy(pcmShorts, start, trimmedShorts, 0, trimmedShorts.length);

            // Save trimmed PCM
            ByteBuffer buf = ByteBuffer.allocate(trimmedShorts.length * 2).order(ByteOrder.LITTLE_ENDIAN);
            for (short s : trimmedShorts) buf.putShort(s);
            lastTrimmedPcm = buf.array();

            float[] trimmedFloat = shortsToFloats(trimmedShorts);

            // Trimmed MFCC
            TarsosMFCC mfccExtractor = new TarsosMFCC(SAMPLE_RATE, BUFFER_SIZE, HOP_SIZE, NUM_MFCC, MAX_FRAMES);
            float[][] trimmedMfcc = mfccExtractor.extractFromFloatArray(trimmedFloat);

            trimmedMfcc = normalizeMfcc(trimmedMfcc);

            // Silence markers
            boolean[] markers = SilenceDetector.computeSilenceMarkers(trimmedMfcc, -40f);

            callback.onRecordingFinished(new RecordingResult(fullMfcc, trimmedMfcc, markers));

        } catch (Exception e) {
            Log.e(TAG, "Processing error", e);
            callback.onError(e);
        }
    }

    public byte[] getLastTrimmedPcm() {
        return lastTrimmedPcm;
    }

    // ========== Helpers ==========

    private short[] byteArrayToShorts(byte[] data) {
        short[] out = new short[data.length / 2];
        ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(out);
        return out;
    }

    private float[] shortsToFloats(short[] input) {
        float[] out = new float[input.length];
        for (int i = 0; i < input.length; i++) {
            out[i] = input[i] / 32768f;
        }
        return out;
    }

    private int findVoiceStart(short[] pcm, float threshold) {
        for (int i = 0; i < pcm.length; i++) {
            if (Math.abs(pcm[i]) > threshold) return i;
        }
        return 0;
    }

    private float[][] normalizeMfcc(float[][] mfcc) {
        float mean = 0f, std = 0f;
        int count = 0;
        for (float[] row : mfcc) {
            for (float v : row) {
                mean += v;
                count++;
            }
        }
        mean /= count;
        for (float[] row : mfcc) {
            for (float v : row) {
                std += (v - mean) * (v - mean);
            }
        }
        std = (float) Math.sqrt(std / count);

        float[][] normed = new float[mfcc.length][mfcc[0].length];
        for (int i = 0; i < mfcc.length; i++) {
            for (int j = 0; j < mfcc[0].length; j++) {
                normed[i][j] = (mfcc[i][j] - mean) / std;
            }
        }
        return normed;
    }
}
