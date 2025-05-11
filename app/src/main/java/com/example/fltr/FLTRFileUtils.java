package com.example.fltr;

import android.content.Context;
import android.media.MediaScannerConnection;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class FLTRFileUtils {
    private final Context context;

    public FLTRFileUtils(Context context) {
        this.context = context;
    }

    public void saveTrimmedRecording(byte[] pcmData, int sampleRate) {
        File outFile = new File(context.getExternalFilesDir(null),
                "recording_" + System.currentTimeMillis() + ".pcm");
        try (FileOutputStream fos = new FileOutputStream(outFile)) {
            fos.write(pcmData);
            fos.flush();
            Log.d("FLTRFileUtils", "Saved to: " + outFile.getAbsolutePath());

            // Let media scanner index it (optional)
            MediaScannerConnection.scanFile(context,
                    new String[]{outFile.getAbsolutePath()},
                    null, null);
        } catch (IOException e) {
            Log.e("FLTRFileUtils", "Error saving PCM file", e);
        }
    }
}
