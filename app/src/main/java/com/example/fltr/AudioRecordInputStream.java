package com.example.fltr;

import android.media.AudioRecord;

import java.io.IOException;
import java.io.InputStream;

public class AudioRecordInputStream extends InputStream {
    private final AudioRecord audioRecord;
    private final byte[] buffer;
    private int bufferPointer = 0;
    private int bytesRead = 0;

    public AudioRecordInputStream(AudioRecord audioRecord, int bufferSize) {
        this.audioRecord = audioRecord;
        this.buffer = new byte[bufferSize];
    }

    @Override
    public int read() throws IOException {
        if (bufferPointer >= bytesRead) {
            bytesRead = audioRecord.read(buffer, 0, buffer.length);
            if (bytesRead <= 0) return -1;
            bufferPointer = 0;
        }
        return buffer[bufferPointer++] & 0xFF;
    }

    @Override
    public int read(byte[] dest, int off, int len) throws IOException {
        return audioRecord.read(dest, off, len);
    }
}
