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
    private float minVal = -100f, maxVal = 100f; // dB range

    private Paint paint = new Paint();
    private Paint textPaint = new Paint();
    private Paint labelPaint = new Paint();
    private Paint framePaint = new Paint();

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

        labelPaint.setColor(Color.LTGRAY);
        labelPaint.setTextSize(20f);
        labelPaint.setAntiAlias(true);

        framePaint.setColor(Color.rgb(50, 50, 0));
        framePaint.setTextSize(24f);
        framePaint.setAntiAlias(true);
        framePaint.setTextAlign(Paint.Align.RIGHT);
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

        int nMfcc = mfccData.length;       // 13
        int nFrames = mfccData[0].length;  // ~221

        // Calculate margins for labels
        int leftMargin = 50;  // space for coefficient labels
        int bottomMargin = 40; // space for time labels
        int topMargin = 40;    // space for title

        float cellWidth = (float) (getWidth() - leftMargin) / nFrames;
        float cellHeight = (float) (getHeight() - topMargin - bottomMargin) / nMfcc;

        // Draw MFCC heatmap: time on X, coeff on Y
        // FLIPPED: C0 now at the bottom (highest Y value)
        for (int coeff = 0; coeff < nMfcc; coeff++) {
            for (int frame = 0; frame < nFrames; frame++) {
                float val = mfccData[coeff][frame];
                float norm = (val - minVal) / (maxVal - minVal);
                int color = Color.HSVToColor(new float[]{240f - norm * 240f, 1f, 1f});
                paint.setColor(color);

                // X = time/frame position
                // Y = coefficient position (flipped: nMfcc-coeff-1)
                float left = leftMargin + frame * cellWidth;
                float top = topMargin + (nMfcc - coeff - 1) * cellHeight;
                canvas.drawRect(left, top, left + cellWidth, top + cellHeight, paint);
            }
        }

        // Draw time labels on X-axis every 10 frames
        for (int frame = 0; frame < nFrames; frame += 10) {
            float timeSec = (frame * hopLength) / (float) sampleRate;
            canvas.drawText(String.format("%.1fs", timeSec),
                    leftMargin + frame * cellWidth, getHeight() - 10, textPaint);
        }

        // Label Y-axis with MFCC coefficient index (flipped)
        for (int coeff = 0; coeff < nMfcc; coeff++) {
            // Use (nMfcc - coeff - 1) to flip the position
            canvas.drawText("C" + coeff, 5,
                    topMargin + (nMfcc - coeff - 1) * cellHeight + 20, labelPaint);
        }

        // Title
        canvas.drawText("MFCC", 10, 30, textPaint);


    }
}