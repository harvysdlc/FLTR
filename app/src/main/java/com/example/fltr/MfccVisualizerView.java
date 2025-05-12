package com.example.fltr;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

public class MfccVisualizerView extends View {

    private float[][] mfccData;  // Shape: [n_mfcc][n_frames]
    private int sampleRate = 44100;
    private int hopLength = 512;
    private float minVal = -100f, maxVal = 100f; // dB scale range (adjust as needed)

    private Paint paint = new Paint();
    private Paint textPaint = new Paint();

    public MfccVisualizerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public MfccVisualizerView(Context context) {
        super(context);
        init();
    }

    private void init() {
        paint.setStyle(Paint.Style.FILL);
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(28f);
        textPaint.setAntiAlias(true);
    }

    public void setMfccData(float[][] data, int sampleRate, int hopLength) {
        this.mfccData = data;
        this.sampleRate = sampleRate;
        this.hopLength = hopLength;
        normalizeMfcc();
        invalidate(); // Redraw
    }

    private void normalizeMfcc() {
        minVal = Float.MAX_VALUE;
        maxVal = Float.MIN_VALUE;
        for (float[] row : mfccData) {
            for (float v : row) {
                if (v < minVal) minVal = v;
                if (v > maxVal) maxVal = v;
            }
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (mfccData == null || mfccData.length == 0) return;

        int nMfcc = mfccData.length;
        int nFrames = mfccData[0].length;

        float cellWidth = (float) getWidth() / nFrames;
        float cellHeight = (float) getHeight() / nMfcc;

        // Draw MFCC heatmap
        for (int i = 0; i < nMfcc; i++) {
            for (int j = 0; j < nFrames; j++) {
                float val = mfccData[i][j];
                float norm = (val - minVal) / (maxVal - minVal);
                int color = Color.HSVToColor(new float[]{240f - norm * 240f, 1f, 1f});
                paint.setColor(color);

                float left = j * cellWidth;
                float top = i * cellHeight;
                canvas.drawRect(left, top, left + cellWidth, top + cellHeight, paint);
            }
        }

        // Optional: draw frame count overlay every 10 frames
        for (int j = 0; j < nFrames; j += 10) {
            float timeSec = (j * hopLength) / (float) sampleRate;
            canvas.drawText(String.format("%.1fs", timeSec), j * cellWidth, getHeight() - 10, textPaint);
        }

        // Optional: title
        canvas.drawText("MFCC", 10, 30, textPaint);
    }
}
