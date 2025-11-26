package com.example.fltr;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

public class MfccVisualizerView extends View {

    private float[][] mfccData;  // original: [20][221]
    private int sampleRate = 44100;
    private int hopLength = 512;
    private float minVal = -100f, maxVal = 100f;

    private Paint paint = new Paint();
    private Paint textPaint = new Paint();
    private Paint coeffPaint = new Paint();
    private Paint timePaint = new Paint();

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

        coeffPaint.setColor(Color.LTGRAY);
        coeffPaint.setTextSize(22f);
        coeffPaint.setAntiAlias(true);
        coeffPaint.setTextAlign(Paint.Align.CENTER);

        timePaint.setColor(Color.LTGRAY);
        timePaint.setTextSize(20f);
        timePaint.setAntiAlias(true);
        timePaint.setTextAlign(Paint.Align.RIGHT);
    }

    public void setMfccData(float[][] data, int sampleRate, int hopLength) {
        this.mfccData = data;
        this.sampleRate = sampleRate;
        this.hopLength = hopLength;
        normalizeMfcc();
        invalidate();
    }

    private void normalizeMfcc() {
        minVal = Float.MAX_VALUE;
        maxVal = Float.MIN_VALUE;

        if (mfccData == null) return;

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

        int nFrames = mfccData.length;       // 221
        int nMfcc   = mfccData[0].length;    // 20

        // Layout margins
        int leftMargin = 70;    // space for MFCC labels
        int bottomMargin = 50;  // space for time labels
        int topMargin = 40;     // title

        float cellWidth  = (float) (getWidth() - leftMargin) / nFrames;
        float cellHeight = (float) (getHeight() - topMargin - bottomMargin) / nMfcc;

        // ----- DRAW HEATMAP (time horizontal, coeff vertical)
        for (int frame = 0; frame < nFrames; frame++) {
            for (int coeff = 0; coeff < nMfcc; coeff++) {

                float val = mfccData[frame][coeff];
                float norm = (val - minVal) / (maxVal - minVal);

                int color = Color.HSVToColor(new float[]{
                        240f - norm * 240f, 1f, 1f
                });
                paint.setColor(color);

                float left = leftMargin + frame * cellWidth;
                float top  = topMargin + (nMfcc - coeff - 1) * cellHeight;

                canvas.drawRect(left, top, left + cellWidth, top + cellHeight, paint);
            }
        }

        // ----- TIME LABELS (X-axis, bottom)
        for (int frame = 0; frame < nFrames; frame += 10) {
            float timeSec = (frame * hopLength) / (float) sampleRate;
            canvas.drawText(String.format("%.1fs", timeSec),
                    leftMargin + frame * cellWidth,
                    getHeight() - 10,
                    textPaint);
        }

        // ----- MFCC LABELS (Y-axis)
        for (int coeff = 0; coeff < nMfcc; coeff++) {
            float y = topMargin + (nMfcc - coeff - 1) * cellHeight + 20;
            canvas.drawText("C" + coeff, 10, y, textPaint);
        }

        // ----- TITLE
        canvas.drawText("MFCC", leftMargin, 30, textPaint);
    }

}
