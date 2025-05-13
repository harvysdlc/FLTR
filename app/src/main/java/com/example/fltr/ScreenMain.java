package com.example.fltr;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.media.AudioFormat;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import org.tensorflow.lite.Interpreter;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.nio.MappedByteBuffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ScreenMain extends AppCompatActivity {

    private static final int SAMPLE_RATE = 44100;
    private static final int RECORDING_LENGTH = SAMPLE_RATE * 3; // 3 seconds
    private static final int REQUEST_RECORD_AUDIO = 13;

    private boolean isRecording = false;
    private AudioRecord recorder;
    private short[] audioBuffer = new short[RECORDING_LENGTH];
    private Button recordButton;
    private Button saveButton;


    private TextView resultView;
    private Interpreter tflite;
    private MfccVisualizerView mfccView;

    private List<String> labels;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_screen_main);

        recordButton = findViewById(R.id.record);
        resultView = findViewById(R.id.transcribeView);
        mfccView = findViewById(R.id.mfccView);



        saveButton = findViewById(R.id.saveBtn);
        saveButton.setOnClickListener(view -> saveLastRecordingAsWav());
        saveButton.setEnabled(false); // Disable initially


        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, REQUEST_RECORD_AUDIO);

        try {
            tflite = new Interpreter(loadModelFile("model_gan.tflite"));
            labels = loadLabels(this);
        } catch (IOException e) {
            e.printStackTrace();
        }

        recordButton.setOnClickListener(view -> {
            if (!isRecording) {
                startRecording();
                recordButton.setText("Stop");
            } else {
                stopRecording();
                recordButton.setText("Record");
            }
        });
    }

    private AudioEngine audioEngine = new AudioEngine();

    private void startRecording() {
        isRecording = true;
        audioEngine.startRecording(new AudioEngine.RecordingCallback() {
            @Override

            public void onRecordingFinished(float[][] mfcc) {
                // Transpose MFCC from [177][13] to [13][177]
                int timeSteps = mfcc.length;
                int mfccCount = mfcc[0].length;
                float[][] transposed = new float[mfccCount][timeSteps];
                for (int i = 0; i < timeSteps; i++) {
                    for (int j = 0; j < mfccCount; j++) {
                        transposed[j][i] = mfcc[i][j];
                    }
                }
                Log.d("ScreenMain","Transposed MFCC shape: [" + transposed.length + "][" + transposed[0].length + "]");
                mfccView.setMfccData(transposed, 44100,  512 );




                // Create input batch shape [1][13][177]
                float[][][] input = new float[1][mfccCount][timeSteps];
                input[0] = transposed;

                // Run inference
                float[][] output = new float[1][getNumLabels()];
                tflite.run(input, output);
                float[] confidences = output[0];

                // Get best prediction
                final int bestIdx = argmax(confidences);
                final float confidence = confidences[bestIdx];

                runOnUiThread(() -> {
                    resultView.setText("Prediction: " + getLabel(bestIdx) + "\nConfidence: " + confidence);
                    recordButton.setText("Record");
                    isRecording = false;
                    saveButton.setEnabled(true);
                });
            }



            @Override
            public void onError(Exception e) {
                runOnUiThread(() -> {
                    resultView.setText("Error: " + e.getMessage());
                    recordButton.setText("Record");
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

        String fileName = getExternalFilesDir(null).getAbsolutePath() + "/recording.wav";
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


    private MappedByteBuffer loadModelFile(String modelName) throws IOException {
        FileInputStream inputStream = new FileInputStream(getAssets().openFd(modelName).getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = getAssets().openFd(modelName).getStartOffset();
        long declaredLength = getAssets().openFd(modelName).getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }

    private List<String> loadLabels(Context context) {
        List<String> labels = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(context.getAssets().open("labels.txt")))) {
            String line;
            while ((line = reader.readLine()) != null) {
                labels.add(line.trim());
            }
        } catch (IOException e) {
            Log.e("LabelLoader", "Error loading labels", e);
        }
        return labels;
    }





    private String getLabel(int index) {
        if (labels != null && index >= 0 && index < labels.size()) {
            return labels.get(index);
        }
        return "Unknown";
    }

    private int getNumLabels() {
        return labels != null ? labels.size() : 0;
    }
    private int argmax(float[] array) {
        int maxIdx = 0;
        float maxVal = array[0];
        for (int i = 1; i < array.length; i++) {
            if (array[i] > maxVal) {
                maxVal = array[i];
                maxIdx = i;
            }
        }
        return maxIdx;
    }
}
