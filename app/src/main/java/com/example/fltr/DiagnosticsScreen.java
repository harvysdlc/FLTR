package com.example.fltr;

import android.Manifest;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioRecord;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Switch;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class DiagnosticsScreen extends AppCompatActivity {

    private static final int SAMPLE_RATE = 44100;
    private static final int RECORDING_LENGTH = SAMPLE_RATE * 3; // 3 seconds
    private static final int REQUEST_RECORD_AUDIO = 13;

    private boolean isRecording = false;
    private short[] audioBuffer = new short[RECORDING_LENGTH];

    private Button recordButton;
    private Button saveButton;
    private TextView rtfView;
    private TextView resultView;
    private MfccVisualizerView mfccView;
    private TextView baybayinView;
    private TextView syllableView;



    private boolean simpleMode = true; // SIMPLE MODE is default
    private float[][] paddedMfcc;
    private float[][] originalMfcc;

    private CustomMFCC.InferenceHelper inferenceHelper;  // Using CustomMFCC's InferenceHelper
    private AudioEngine audioEngine = new AudioEngine();

    // UI mode toggle
    private Switch simpleModeToggle;
    private View diagnosticsContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_diagnostics);

        // UI bindings
        baybayinView = findViewById(R.id.baybayinView);
        syllableView = findViewById(R.id.syllableView);
        Button diagnosticsBtn = findViewById(R.id.diagnosticsBtn);
        Button chart = findViewById(R.id.learn_chart);
        recordButton = findViewById(R.id.record);
        resultView = findViewById(R.id.transcribeView);
        mfccView = findViewById(R.id.mfccView);
        rtfView = findViewById(R.id.rtfView);
        Button calibrateBtn = findViewById(R.id.calibrateBtn);
        saveButton = findViewById(R.id.saveBtn);



        // Chart / Learn Baybayin button
        chart.setOnClickListener(view -> {
            Intent intent = new Intent(DiagnosticsScreen.this, Learn.class);
            startActivity(intent);
        });

        diagnosticsBtn.setOnClickListener(v -> {
            startActivity(new Intent(DiagnosticsScreen.this, ScreenMain.class));
        });

        // Save recording button
        saveButton.setOnClickListener(view -> saveLastRecordingAsWav());
        saveButton.setEnabled(false); // disabled until a recording exists

        // Request microphone permission
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, REQUEST_RECORD_AUDIO);

        // Initialize inference helper
        try {
            inferenceHelper = new CustomMFCC.InferenceHelper(this);
        } catch (IOException e) {
            e.printStackTrace();
            Log.e("ScreenMain", "Failed to init InferenceHelper: " + e.getMessage());
        }

        // Record button behavior
        recordButton.setOnClickListener(view -> {
            if (!isRecording) {
                startRecording();
                recordButton.setText("Listening...");
            } else {
                stopRecording();
                recordButton.setText("Tap to Speak");
            }
        });

        // Calibrate button
        calibrateBtn.setOnClickListener(v -> {

            resultView.setText("Calibrating... stay quiet");

            audioEngine.startCalibration(new AudioEngine.CalibrationCallback() {
                @Override
                public void onCalibrationComplete(int threshold) {
                    runOnUiThread(() -> resultView.setText("Calibration complete.\nThreshold = " + threshold));
                }

                @Override
                public void onCalibrationError(Exception e) {
                    runOnUiThread(() -> resultView.setText("Calibration failed: " + e.getMessage()));
                }
            });
        });
    }

    private void startRecording() {
        isRecording = true;
        audioEngine.startRecording(new AudioEngine.RecordingCallback() {
            @Override
            public void onRecordingFinished(short[] audioData) {
                long startTime = System.currentTimeMillis();

                // Extract MFCCs
                CustomMFCC.MfccResult mfccResult = CustomMFCC.extractMFCCs(audioData);

                // Save MFCC for inspection (non-blocking-ish)
                saveMFCCtoFile(mfccResult.paddedMfcc, "mfcc_input_before_inference.txt");

                // Store for visualization
                paddedMfcc = mfccResult.paddedMfcc;
                originalMfcc = mfccResult.originalMfcc; // if your MfccResult contains it

                // Run inference
                CustomMFCC.InferenceResult inferenceResult = null;
                if (inferenceHelper != null) {
                    inferenceResult = inferenceHelper.runInference(mfccResult.paddedMfcc);
                } else {
                    Log.e("ScreenMain", "InferenceHelper is null; skipping inference");
                }

                String predictedLabel = (inferenceResult != null) ? inferenceResult.label : "unknown";
                float confidence = (inferenceResult != null) ? inferenceResult.confidence : 0f;
                float processingTimeSec = (inferenceResult != null) ? inferenceResult.processingTimeSec : 0f;

                // Translate to Baybayin
                String baybayinOutput = BaybayinTranslator.translateToBaybayin(predictedLabel);

                // Audio durations
                float audioDurationSec = (float) audioData.length / AudioEngine.SAMPLE_RATE;
                float frameDurationSec = (float) mfccResult.originalFrameCount * CustomMFCC.HOP_SIZE / AudioEngine.SAMPLE_RATE;

                // Compute RTF safely
                float rtf = audioDurationSec > 0f ? processingTimeSec / audioDurationSec : 0f;

                final float finalRtf = rtf;
                final float finalAudioDurationSec = audioDurationSec;
                final float finalProcessingTimeSec = processingTimeSec;

                // Syllable segmentation
                final String syllables = segmentSyllables(predictedLabel);

                runOnUiThread(() -> {
                    // Update UI
                    rtfView.setText(
                            "Audio Duration: " + String.format("%.6f", finalAudioDurationSec) +
                                    "\nProcessing Time: " + String.format("%.6f", finalProcessingTimeSec) +
                                    "\nRTF: " + String.format("%.6f", finalRtf)
                    );

                    if (paddedMfcc != null && mfccView != null) {
                        mfccView.setMfccData(paddedMfcc, AudioEngine.SAMPLE_RATE, CustomMFCC.HOP_SIZE);
                    }

                    resultView.setText("Prediction: " + predictedLabel + "\nConfidence: " + confidence);
                    baybayinView.setText(baybayinOutput);

                    if (syllableView != null) {
                        syllableView.setText(syllables);
                    }

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
        isRecording = false; // audioEngine should stop naturally
    }

    private void saveLastRecordingAsWav() {
        byte[] pcmBytes = audioEngine.getLastTrimmedPcm();
        if (pcmBytes == null) {
            resultView.setText("No recording available.");
            return;
        }

        String fileName = "recording_" + System.currentTimeMillis() + ".wav";

        ContentValues values = new ContentValues();
        values.put(MediaStore.Downloads.DISPLAY_NAME, fileName);
        values.put(MediaStore.Downloads.MIME_TYPE, "audio/wav");
        values.put(MediaStore.Downloads.IS_PENDING, 1);

        Uri downloads = MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY);
        Uri fileUri = getContentResolver().insert(downloads, values);

        if (fileUri == null) {
            resultView.setText("Failed to create file.");
            return;
        }

        try (OutputStream out = getContentResolver().openOutputStream(fileUri)) {
            if (out == null) throw new IOException("OutputStream is null.");

            writeWavHeader(out, pcmBytes.length, AudioEngine.SAMPLE_RATE, 1, 16);
            out.write(pcmBytes);

            values.clear();
            values.put(MediaStore.Downloads.IS_PENDING, 0);
            getContentResolver().update(fileUri, values, null, null);

            runOnUiThread(() -> resultView.setText("Saved to Downloads as " + fileName));
        } catch (Exception e) {
            e.printStackTrace();
            runOnUiThread(() -> resultView.setText("Failed to save WAV: " + e.getMessage()));
        }
    }

    private void writeWavHeader(OutputStream out, int audioLen, int sampleRate, int channels, int bitsPerSample) throws IOException {
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
        if (mfcc == null) return;

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < mfcc.length; i++) {
            for (int j = 0; j < mfcc[i].length; j++) {
                sb.append(mfcc[i][j]);
                if (j < mfcc[i].length - 1) sb.append(" ");
            }
            sb.append("\n");
        }

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

    /**
     * Basic heuristic syllabifier for UI display. Works with romanized Filipino words.
     * Uses simple greedy matching similar to BaybayinSyllableHelper logic.
     */
    private String segmentSyllables(String word) {
        if (word == null) return "";
        String w = word.toLowerCase().replaceAll("[^a-z]", "");

        StringBuilder out = new StringBuilder();
        int i = 0;

        while (i < w.length()) {
            String chunk = null;

            // Try 3-letter chunk
            if (i + 2 < w.length()) {
                String s3 = w.substring(i, i + 3);
                if (BaybayinTranslator.baybayinMap.containsKey(s3)) {
                    chunk = s3;
                    i += 3;
                }
            }

            // Try 2-letter chunk
            if (chunk == null && i + 1 < w.length()) {
                String s2 = w.substring(i, i + 2);
                if (BaybayinTranslator.baybayinMap.containsKey(s2)) {
                    chunk = s2;
                    i += 2;
                }
            }

            // Fallback to single letter (vowel or consonant)
            if (chunk == null) {
                String s1 = w.substring(i, i + 1);
                chunk = s1;  // even if not in map, follow translator fallback
                i += 1;
            }

            if (out.length() > 0) out.append(" · ");
            out.append(chunk);
        }

        return out.toString();
    }

}
