package com.example.fltr;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Date;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class ScreenMain extends AppCompatActivity {

    private Button recordButton, toggleViewButton, saveButton;
    private TextView transcribeView, frameCountView;
    private MfccVisualizerView mfccView;

    private boolean isRecording = false;
    private boolean showTrimmed = true;

    private AudioEngine audioEngine;
    private InferenceEngine inferenceEngine;

    private float[][] lastMfcc;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_screen_main);

        recordButton = findViewById(R.id.record);
        toggleViewButton = findViewById(R.id.toggleView);
        saveButton = findViewById(R.id.saveBtn);
        transcribeView = findViewById(R.id.transcribeView);
        frameCountView = findViewById(R.id.frameCountView);
        mfccView = findViewById(R.id.mfccView);

        audioEngine = new AudioEngine();
        inferenceEngine = new InferenceEngine(this);

        recordButton.setOnClickListener(v -> {
            if (!isRecording) {
                startRecording();
            }
        });

        toggleViewButton.setOnClickListener(v -> toggleMfccView());

        saveButton.setOnClickListener(v -> {
            byte[] trimmedPcm = audioEngine.getLastTrimmedPcm();
            if (trimmedPcm != null) {
                saveWavFile(trimmedPcm, AudioEngine.SAMPLE_RATE);
            } else {
                transcribeView.setText("Nothing to save.");
            }
        });

    }

    private void startRecording() {
        isRecording = true;
        recordButton.setText("Listening...");
        transcribeView.setText("Listening for voice...");
        mfccView.clear();
        frameCountView.setText("");

        audioEngine.startRecording(new AudioEngine.RecordingCallback() {
            @Override
            public void onRecordingFinished(float[][] mfcc) {
                runOnUiThread(() -> {
                    isRecording = false;
                    recordButton.setText("Record");
                    lastMfcc = mfcc;
                    updateMfccView();
                    runInference();
                });
            }

            @Override
            public void onError(Exception e) {
                runOnUiThread(() -> {
                    isRecording = false;
                    recordButton.setText("Record");
                    transcribeView.setText("Error: " + e.getMessage());
                });
            }
        });
    }

    private void updateMfccView() {
        if (lastMfcc != null) {
            mfccView.setMFCC(lastMfcc, new boolean[lastMfcc.length]);
            frameCountView.setText("Frames: " + lastMfcc.length);
        }
    }

    private void toggleMfccView() {
        // No trimmed vs full logic anymore. Placeholder for future logic if needed.
    }

    private void runInference() {
        if (lastMfcc == null || lastMfcc.length == 0 || lastMfcc[0].length == 0) {
            transcribeView.setText("No valid MFCC input available.");
            return;
        }

        if (lastMfcc.length != 177 || lastMfcc[0].length != 13) {
            transcribeView.setText("Expected MFCC shape [177][13], got [" + lastMfcc.length + "][" + lastMfcc[0].length + "]");
            return;
        }

        float[][][] modelInput = new float[1][13][177];
        for (int f = 0; f < 177; f++) {
            for (int c = 0; c < 13; c++) {
                modelInput[0][c][f] = lastMfcc[f][c];
            }
        }

        String label = inferenceEngine.predict(modelInput);
        String baybayin = BaybayinTranslator.translateToBaybayin(label);
        transcribeView.setText("Predicted: " + label + "\nBaybayin: " + baybayin);
    }


    private void saveWavFile(byte[] pcmData, int sampleRate) {
        try {
            File file = new File(getExternalFilesDir(null), "recording_" + new Date().getTime() + ".wav");
            FileOutputStream out = new FileOutputStream(file);

            // WAV Header
            int totalDataLen = pcmData.length + 36;
            int byteRate = sampleRate * 2; // 16-bit mono

            ByteBuffer buffer = ByteBuffer.allocate(44);
            buffer.order(ByteOrder.LITTLE_ENDIAN);
            buffer.put("RIFF".getBytes());
            buffer.putInt(totalDataLen);
            buffer.put("WAVE".getBytes());
            buffer.put("fmt ".getBytes());
            buffer.putInt(16); // Subchunk1Size for PCM
            buffer.putShort((short) 1); // AudioFormat PCM
            buffer.putShort((short) 1); // NumChannels
            buffer.putInt(sampleRate);
            buffer.putInt(byteRate);
            buffer.putShort((short) 2); // BlockAlign
            buffer.putShort((short) 16); // BitsPerSample
            buffer.put("data".getBytes());
            buffer.putInt(pcmData.length);
            out.write(buffer.array());
            out.write(pcmData);
            out.close();

            transcribeView.setText("Saved WAV file: " + file.getName());
        } catch (IOException e) {
            transcribeView.setText("Failed to save WAV: " + e.getMessage());
        }
    }
}
