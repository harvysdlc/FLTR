package com.example.fltr;

import java.util.Arrays;

public class AudioUtils {

    private static final int TRIM_THRESHOLD = 5000;
    private static final float MAXIMUM = 16384f;

    // Normalize to match Python training behavior
    public static float[] normalize(float[] audio) {
        float max = 0f;
        for (float v : audio) {
            if (Math.abs(v) > max) max = Math.abs(v);
        }
        float scale = MAXIMUM / (max * 32768f);
        float[] result = new float[audio.length];
        for (int i = 0; i < audio.length; i++) {
            result[i] = audio[i] * scale;
        }
        return result;
    }

    // Trim silence from front and back
    public static float[] trimSilence(float[] audio) {
        int start = 0;
        int end = audio.length - 1;
        while (start < audio.length && Math.abs(audio[start] * 32768f) < TRIM_THRESHOLD) start++;
        while (end > start && Math.abs(audio[end] * 32768f) < TRIM_THRESHOLD) end--;
        return Arrays.copyOfRange(audio, start, end + 1);
    }

    // Trim only leading silence (used to match your Python pipeline)
    public static float[] trimLeadingSilence(float[] audio) {
        int start = 0;
        while (start < audio.length && Math.abs(audio[start] * 32768f) < TRIM_THRESHOLD) start++;
        return Arrays.copyOfRange(audio, start, audio.length);
    }

    // Optional: Pre-emphasis filter
    public static float[] applyPreEmphasis(float[] audio) {
        float[] out = new float[audio.length];
        out[0] = audio[0];
        for (int i = 1; i < audio.length; i++) {
            out[i] = audio[i] - 0.97f * audio[i - 1];
        }
        return out;
    }

    // Convert float samples [-1.0, 1.0] to signed 16-bit PCM little endian
    public static byte[] floatToPCM16(float[] floatData) {
        byte[] pcm = new byte[floatData.length * 2];
        for (int i = 0; i < floatData.length; i++) {
            int sample = (int) (floatData[i] * 32767.0f);
            sample = Math.max(-32768, Math.min(32767, sample));
            pcm[i * 2] = (byte) (sample & 0xff);
            pcm[i * 2 + 1] = (byte) ((sample >> 8) & 0xff);
        }
        return pcm;
    }
}
