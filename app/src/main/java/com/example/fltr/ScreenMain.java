package com.example.fltr;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.graphics.Typeface;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.media.AudioFormat;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import org.tensorflow.lite.Interpreter;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.nio.MappedByteBuffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class ScreenMain extends AppCompatActivity {

    private static final int SAMPLE_RATE = 44100;
    private static final int RECORDING_LENGTH = SAMPLE_RATE * 3; // 3 seconds
    private static final int REQUEST_RECORD_AUDIO = 13;

    private boolean isRecording = false;
    private AudioRecord recorder;
    private short[] audioBuffer = new short[RECORDING_LENGTH];
    private Button recordButton;
    private Button saveButton;
    private Button toggleViewButton;
    private TextView rtfView;

    private TextView resultView;
    private MfccVisualizerView mfccView;
    private TextView baybayinView;

    private boolean showingPadded = true;

    private float[][] paddedMfcc;
    private float[][] originalMfcc;

    private CustomMFCC.InferenceHelper inferenceHelper;  // Using CustomMFCC's InferenceHelper

    private AudioEngine audioEngine = new AudioEngine();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_screen_main);

        baybayinView = findViewById(R.id.baybayinView);

        recordButton = findViewById(R.id.record);
        resultView = findViewById(R.id.transcribeView);
        mfccView = findViewById(R.id.mfccView);
        rtfView = findViewById(R.id.rtfView);

        toggleViewButton = findViewById(R.id.toggleView);

        saveButton = findViewById(R.id.saveBtn);
        saveButton.setOnClickListener(view -> saveLastRecordingAsWav());
        saveButton.setEnabled(false); // Disable initially

        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, REQUEST_RECORD_AUDIO);

        try {
            inferenceHelper = new CustomMFCC.InferenceHelper(this);
        } catch (IOException e) {
            e.printStackTrace();
        }

        recordButton.setOnClickListener(view -> {
            if (!isRecording) {
                startRecording();
                recordButton.setText("Listening...");
            } else {
                stopRecording();
                toggleViewButton.setText("Show Original MFCC");
                recordButton.setText("Tap to Speak");
            }
        });
    }

    private void startRecording() {
        isRecording = true;
        audioEngine.startRecording(new AudioEngine.RecordingCallback() {
            @Override
            public void onRecordingFinished(short[] audioData) {
                long startTime = System.currentTimeMillis();  // Start timing

                // Process the audio data using CustomMFCC
                CustomMFCC.MfccResult mfccResult = CustomMFCC.extractMFCCs(audioData);

                saveMFCCtoFile(mfccResult.paddedMfcc, "mfcc_input_before_inference.txt");
                // Store the MFCC data for visualization
                paddedMfcc = mfccResult.paddedMfcc;


                // Use CustomMFCC's InferenceHelper to run inference
                CustomMFCC.InferenceResult result = inferenceHelper.runInference(mfccResult.paddedMfcc);

                // Process and display the result
                String predictedLabel = result.label;
                float confidence = result.confidence;
                String baybayinOutput = BaybayinTranslator.translateToBaybayin(predictedLabel);

                // Calculate actual audio duration from sample count
                float audioDurationSec = (float) audioData.length / AudioEngine.SAMPLE_RATE;
                Log.d("ScreenMain", "Raw audio duration: " + audioDurationSec + " seconds");

                // Alternative based on original frame count
                float frameDurationSec = (float) mfccResult.originalFrameCount * CustomMFCC.HOP_SIZE / AudioEngine.SAMPLE_RATE;
                Log.d("ScreenMain", "Frame-based duration: " + frameDurationSec + " seconds");

                // Compute RTF
                float rtf = result.processingTimeSec / audioDurationSec;
                Log.d("ScreenMain", "RTF: " + rtf);

                final float finalRtf = rtf;
                final float finalAudioDurationSec = audioDurationSec;
                final float finalProcessingTimeSec = result.processingTimeSec;

                runOnUiThread(() -> {
                    toggleViewButton.setOnClickListener(v -> {
                        showingPadded = !showingPadded;
                        float[][] toDisplay = showingPadded ? paddedMfcc : originalMfcc;
                        mfccView.setMfccData(toDisplay, 44100, 512);
                        // Dynamically update button text
                        if (showingPadded) {
                            toggleViewButton.setText("Show Original MFCC");
                        } else {
                            toggleViewButton.setText("Show Padded MFCC");
                        }
                    });

                    rtfView.setText(
                            "Audio Duration: " + String.format("%.6f", finalAudioDurationSec) +
                                    "\nProcessing Time: " + String.format("%.6f", finalProcessingTimeSec) +
                                    "\nRTF: " + String.format("%.6f", finalRtf)
                    );
                    mfccView.setMfccData(paddedMfcc, 44100, 512);
                    resultView.setText("Prediction: " + predictedLabel + "\nConfidence: " + confidence);
                    baybayinView.setText(baybayinOutput);
                    recordButton.setText("Tap to Speak");
                    isRecording = false;
                    saveButton.setEnabled(true);


                });
            }

            @Override
            public void onError(Exception e) {
                runOnUiThread(() -> {
                    resultView.setText("Error: " + e.getMessage());
                    recordButton.setText("Tap to Speak");
                    isRecording = false;
                });
            }
        });
    }

    private void stopRecording() {
        isRecording = false; // Triggers AudioEngine to exit loop
    }

    private void saveLastRecordingAsWav() {
        byte[] pcmBytes = audioEngine.getLastTrimmedPcm();
        if (pcmBytes == null) {
            resultView.setText("No recording available.");
            return;
        }

        String directory = getExternalFilesDir(null).getAbsolutePath();
        String fileName = directory + "/recording_" + System.currentTimeMillis() + ".wav";

        try (FileOutputStream out = new FileOutputStream(fileName)) {
            writeWavHeader(out, pcmBytes.length, AudioEngine.SAMPLE_RATE, 1, 16);
            out.write(pcmBytes);
            runOnUiThread(() -> resultView.setText("Saved: " + fileName));
        } catch (IOException e) {
            e.printStackTrace();
            runOnUiThread(() -> resultView.setText("Failed to save WAV: " + e.getMessage()));
        }
    }

    private void writeWavHeader(FileOutputStream out, int audioLen, int sampleRate, int channels, int bitsPerSample) throws IOException {
        int byteRate = sampleRate * channels * bitsPerSample / 8;
        int dataLen = 36 + audioLen;

        out.write("RIFF".getBytes());
        out.write(intToLE(dataLen));
        out.write("WAVE".getBytes());
        out.write("fmt ".getBytes());
        out.write(intToLE(16)); // Subchunk1Size
        out.write(shortToLE((short) 1)); // PCM
        out.write(shortToLE((short) channels));
        out.write(intToLE(sampleRate));
        out.write(intToLE(byteRate));
        out.write(shortToLE((short) (channels * bitsPerSample / 8)));
        out.write(shortToLE((short) bitsPerSample));
        out.write("data".getBytes());
        out.write(intToLE(audioLen));
    }

    private byte[] intToLE(int val) {
        return ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(val).array();
    }

    private byte[] shortToLE(short val) {
        return ByteBuffer.allocate(2).order(ByteOrder.LITTLE_ENDIAN).putShort(val).array();
    }

    private void saveMFCCtoFile(float[][] mfcc, String filename) {
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < mfcc.length; i++) {
            for (int j = 0; j < mfcc[i].length; j++) {
                sb.append(mfcc[i][j]);
                if (j < mfcc[i].length - 1) sb.append(" ");
            }
            sb.append("\n");
        }

        // Save to Downloads folder
        String mfccFile = "mfcc_from_app.txt";
        File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        File file = new File(downloadsDir, mfccFile);

        try (FileOutputStream fos = new FileOutputStream(file)) {
            fos.write(sb.toString().getBytes(StandardCharsets.UTF_8));
            Log.d("ScreenMain", "MFCC saved to file: " + file.getAbsolutePath());
        } catch (IOException e) {
            Log.e("ScreenMain", "Error saving MFCC to file", e);
        }
    }
}


