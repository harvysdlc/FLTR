package com.example.fltr;

import android.content.Context;
import android.os.Environment;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class FLTRFileUtils {

    private final Context context;

    public FLTRFileUtils(Context context) {
        this.context = context.getApplicationContext();
    }

    /**
     * Save raw PCM data as a WAV file into the emulator's Downloads directory.
     *
     * @param pcmData    16‑bit little‑endian PCM data
     * @param sampleRate sample rate (e.g. 44100)
     */
    public void saveTrimmedRecording(byte[] pcmData, int sampleRate) {
        try {
            // 1) Convert byte[] → short[]
            short[] pcmShorts = new short[pcmData.length / 2];
            ByteBuffer.wrap(pcmData)
                    .order(ByteOrder.LITTLE_ENDIAN)
                    .asShortBuffer()
                    .get(pcmShorts);

            // 2) Convert to WAV bytes
            byte[] wavData = WAVUtil.convertToWav(pcmShorts, sampleRate);

            // 3) Build output file path
            String fileName = "recording_" + System.currentTimeMillis() + ".wav";
            File downloads = Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_DOWNLOADS);
            if (!downloads.exists()) downloads.mkdirs();
            File outFile = new File(downloads, fileName);

            // 4) Write to disk
            try (FileOutputStream fos = new FileOutputStream(outFile)) {
                fos.write(wavData);
                fos.flush();
            }

            Toast.makeText(context,
                    "Saved WAV to: " + outFile.getAbsolutePath(),
                    Toast.LENGTH_LONG).show();

        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(context,
                    "Failed to save recording: " + e.getMessage(),
                    Toast.LENGTH_LONG).show();
        }
    }
}
