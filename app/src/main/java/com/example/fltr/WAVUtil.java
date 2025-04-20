package com.example.fltr;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.mfcc.MFCC;
import be.tarsos.dsp.SilenceDetector;



public class WAVUtil {
    public static byte[] convertToWav(short[] audioData, int sampleRate) {
        int byteRate = sampleRate * 2;
        byte[] data = new byte[audioData.length * 2];
        ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().put(audioData);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            // WAV header
            out.write(new byte[] { 'R', 'I', 'F', 'F' });
            out.write(intToByteArray(36 + data.length));
            out.write(new byte[] { 'W', 'A', 'V', 'E' });
            out.write(new byte[] { 'f', 'm', 't', ' ' });
            out.write(intToByteArray(16)); // Subchunk1Size
            out.write(shortToByteArray((short) 1)); // PCM
            out.write(shortToByteArray((short) 1)); // Mono
            out.write(intToByteArray(sampleRate));
            out.write(intToByteArray(byteRate));
            out.write(shortToByteArray((short) 2)); // Block align
            out.write(shortToByteArray((short) 16)); // Bits per sample
            out.write(new byte[] { 'd', 'a', 't', 'a' });
            out.write(intToByteArray(data.length));
            out.write(data);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return out.toByteArray();
    }

    private static byte[] intToByteArray(int value) {
        return ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(value).array();
    }

    private static byte[] shortToByteArray(short value) {
        return ByteBuffer.allocate(2).order(ByteOrder.LITTLE_ENDIAN).putShort(value).array();
    }
}
