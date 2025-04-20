package com.example.fltr;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class ScreenMain extends AppCompatActivity {

    private Button recordButton, toggleViewButton, saveButton;
    private TextView transcribeView, frameCountView;
    private MfccVisualizerView mfccView;

    private boolean isRecording = false;
    private boolean showTrimmed = true;

    private AudioProcessor audioProcessor;
    private InferenceEngine inferenceEngine;

    private float[][] lastFullMfcc, lastTrimmedMfcc;
    private boolean[] silenceMarkers;

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

        audioProcessor = new AudioProcessor(this);
        inferenceEngine = new InferenceEngine(this);

        recordButton.setOnClickListener(v -> {
            if (!isRecording) {
                startRecording();
            } else {
                stopRecording();
            }
        });

        toggleViewButton.setOnClickListener(v -> toggleMfccView());

        saveButton.setOnClickListener(v -> {
            byte[] trimmedPcm = audioProcessor.getLastTrimmedPcm();
            if (trimmedPcm != null) {
                FLTRFileUtils fileUtils = new FLTRFileUtils(this);
                fileUtils.saveTrimmedRecording(trimmedPcm, AudioProcessor.SAMPLE_RATE);
            } else {
                transcribeView.setText("Nothing to save.");
            }
        });

    }

    private void startRecording() {
        isRecording = true;
        recordButton.setText("Stop");
        transcribeView.setText("Recording...");
        mfccView.clear();
        frameCountView.setText("");

        audioProcessor.startRecording(new AudioProcessor.RecordingCallback() {
            @Override
            public void onRecordingFinished(AudioProcessor.RecordingResult result) {
                runOnUiThread(() -> {
                    lastFullMfcc = result.fullMfcc;
                    lastTrimmedMfcc = result.trimmedMfcc;
                    silenceMarkers = result.silenceMarkers;

                    updateMfccView();
                    runInference();
                });
            }

            @Override
            public void onError(Exception e) {
                runOnUiThread(() -> transcribeView.setText("Error: " + e.getMessage()));
            }
        });
    }

    private void stopRecording() {
        isRecording = false;
        recordButton.setText("Record");
        transcribeView.setText("Processing...");
        audioProcessor.stopRecording();
    }

    private void runInference() {
        String label = inferenceEngine.predict(lastTrimmedMfcc);
        String baybayin = BaybayinTranslator.translateToBaybayin(label);
        transcribeView.setText("Predicted: " + label + "\nBaybayin: " + baybayin);

    }

    private void updateMfccView() {
        float[][] mfccToShow = showTrimmed ? lastTrimmedMfcc : lastFullMfcc;
        mfccView.setMFCC(mfccToShow, silenceMarkers);
        frameCountView.setText("Frames: " + mfccToShow.length);
    }

    private void toggleMfccView() {
        showTrimmed = !showTrimmed;
        updateMfccView();
    }
}
