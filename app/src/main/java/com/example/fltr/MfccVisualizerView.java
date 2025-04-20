package com.example.fltr;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

public class MfccVisualizerView extends View {
    private float[][] mfccData;           // [frames][coeffs]
    private boolean[] silenceMarkers;     // same length as mfccData.length
    private boolean showTrimmedOnly;      // toggle

    private final Paint paint = new Paint();
    private final Paint silencePaint = new Paint();
    private final Paint textPaint = new Paint();

    public MfccVisualizerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        paint.setStyle(Paint.Style.FILL);
        silencePaint.setColor(Color.YELLOW);
        silencePaint.setStrokeWidth(2f);
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(32f);
    }

    public void setMfccData(float[][] mfccData) {
        this.mfccData = mfccData;
        invalidate();  // Redraw the view after setting new data
    }

    public void setSilenceMarkers(boolean[] silenceMarkers) {
        this.silenceMarkers = silenceMarkers;
        invalidate();  // Redraw the view after setting new markers
    }

    public void toggleTrimmedView() {
        showTrimmedOnly = !showTrimmedOnly;
        invalidate();  // Redraw the view after toggling
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (mfccData == null) return;

        int width = getWidth();
        int height = getHeight();
        int frameCount = mfccData.length;
        int coeffCount = mfccData[0].length;

        float frameWidth = (float) width / frameCount;
        float coeffHeight = (float) height / coeffCount;

        for (int i = 0; i < frameCount; i++) {
            if (showTrimmedOnly && silenceMarkers[i]) continue; // Skip if trimmed and silence detected

            for (int j = 0; j < coeffCount; j++) {
                int color = infernoColorMap(mfccData[i][j]);

                // Draw MFCC data as a rectangle
                paint.setColor(color);
                float left = i * frameWidth;
                float top = j * coeffHeight;
                float right = (i + 1) * frameWidth;
                float bottom = (j + 1) * coeffHeight;
                canvas.drawRect(left, top, right, bottom, paint);
            }

            // Mark silence regions in yellow
            if (silenceMarkers != null && silenceMarkers[i]) {
                float left = i * frameWidth;
                float right = (i + 1) * frameWidth;
                canvas.drawRect(left, 0, right, height, silencePaint);
            }
        }
    }

    private int infernoColorMap(float value) {
        // Normalize value to range 0-1 (assuming the MFCC values are in a specific range, adjust as needed)
        float normalizedValue = Math.min(Math.max(value, -1), 1);
        int index = (int) ((normalizedValue + 1) / 2 * (INFERNO_COLORMAP.length - 1));
        return INFERNO_COLORMAP[index];
    }

    private static final int[] INFERNO_COLORMAP = new int[] {
            Color.rgb(0, 0, 3),
            Color.rgb(0, 0, 4),
            Color.rgb(0, 0, 6),
            Color.rgb(1, 0, 7),
            Color.rgb(1, 1, 9),
            Color.rgb(1, 1, 11),
            Color.rgb(2, 1, 14),
            Color.rgb(2, 2, 16),
            Color.rgb(3, 2, 18),
            Color.rgb(4, 3, 20),
            Color.rgb(4, 3, 22),
            Color.rgb(5, 4, 24),
            Color.rgb(6, 4, 27),
            Color.rgb(7, 5, 29),
            Color.rgb(8, 6, 31),
            Color.rgb(9, 6, 33),
            Color.rgb(10, 7, 35),
            Color.rgb(11, 7, 38),
            Color.rgb(13, 8, 40),
            Color.rgb(14, 8, 42),
            Color.rgb(15, 9, 45),
            Color.rgb(16, 9, 47),
            Color.rgb(18, 10, 50),
            Color.rgb(19, 10, 52),
            Color.rgb(20, 11, 54),
            Color.rgb(22, 11, 57),
            Color.rgb(23, 11, 59),
            Color.rgb(25, 11, 62),
            Color.rgb(26, 11, 64),
            Color.rgb(28, 12, 67),
            Color.rgb(29, 12, 69),
            Color.rgb(31, 12, 71),
            Color.rgb(32, 12, 74),
            Color.rgb(34, 11, 76),
            Color.rgb(36, 11, 78),
            Color.rgb(38, 11, 80),
            Color.rgb(39, 11, 82),
            Color.rgb(41, 11, 84),
            Color.rgb(43, 10, 86),
            Color.rgb(45, 10, 88),
            Color.rgb(46, 10, 90),
            Color.rgb(48, 10, 92),
            Color.rgb(50, 9, 93),
            Color.rgb(52, 9, 95),
            Color.rgb(53, 9, 96),
            Color.rgb(55, 9, 97),
            Color.rgb(57, 9, 98),
            Color.rgb(59, 9, 100),
            Color.rgb(60, 9, 101),
            Color.rgb(62, 9, 102),
            Color.rgb(64, 9, 102),
            Color.rgb(65, 9, 103),
            Color.rgb(67, 10, 104),
            Color.rgb(69, 10, 105),
            Color.rgb(70, 10, 105),
            Color.rgb(72, 11, 106),
            Color.rgb(74, 11, 106),
            Color.rgb(75, 12, 107),
            Color.rgb(77, 12, 107),
            Color.rgb(79, 13, 108),
            Color.rgb(80, 13, 108),
            Color.rgb(82, 14, 108),
            Color.rgb(83, 14, 109),
            Color.rgb(85, 15, 109),
            Color.rgb(87, 15, 109),
            Color.rgb(88, 16, 109),
            Color.rgb(90, 17, 109),
            Color.rgb(91, 17, 110),
            Color.rgb(93, 18, 110),
            Color.rgb(95, 18, 110),
            Color.rgb(96, 19, 110),
            Color.rgb(98, 20, 110),
            Color.rgb(99, 20, 110),
            Color.rgb(101, 21, 110),
            Color.rgb(102, 21, 110),
            Color.rgb(104, 22, 110),
            Color.rgb(106, 23, 110),
            Color.rgb(107, 23, 110),
            Color.rgb(109, 24, 110),
            Color.rgb(110, 24, 110),
            Color.rgb(112, 25, 110),
            Color.rgb(114, 25, 109),
            Color.rgb(115, 26, 109),
            Color.rgb(117, 27, 109),
            Color.rgb(118, 27, 109),
            Color.rgb(120, 28, 109),
            Color.rgb(122, 28, 109),
            Color.rgb(123, 29, 108),
            Color.rgb(125, 29, 108),
            Color.rgb(126, 30, 108),
            Color.rgb(128, 31, 107),
            Color.rgb(129, 31, 107),
            Color.rgb(131, 32, 107),
            Color.rgb(133, 32, 106),
            Color.rgb(134, 33, 106),
            Color.rgb(136, 33, 106),
            Color.rgb(137, 34, 105),
            Color.rgb(139, 34, 105),
            Color.rgb(141, 35, 105),
            Color.rgb(142, 36, 104),
            Color.rgb(144, 36, 104),
            Color.rgb(145, 37, 103),
            Color.rgb(147, 37, 103),
            Color.rgb(149, 38, 102),
            Color.rgb(150, 38, 102),
            Color.rgb(152, 39, 101),
            Color.rgb(153, 40, 100),
            Color.rgb(155, 40, 100),
            Color.rgb(156, 41, 99),
            Color.rgb(158, 41, 99),
            Color.rgb(160, 42, 98),
            Color.rgb(161, 43, 97),
            Color.rgb(163, 43, 97),
            Color.rgb(164, 44, 96),
            Color.rgb(166, 44, 95),
            Color.rgb(167, 45, 95),
            Color.rgb(169, 46, 94),
            Color.rgb(171, 46, 93),
            Color.rgb(172, 47, 92),
            Color.rgb(174, 48, 91),
            Color.rgb(175, 49, 91),
            Color.rgb(177, 49, 90),
            Color.rgb(178, 50, 89),
            Color.rgb(180, 51, 88),
            Color.rgb(181, 51, 87),
            Color.rgb(183, 52, 86),
            Color.rgb(184, 53, 86),
            Color.rgb(186, 54, 85),
            Color.rgb(187, 55, 84),
            Color.rgb(189, 55, 83),
            Color.rgb(190, 56, 82),
            Color.rgb(191, 57, 81),
            Color.rgb(193, 58, 80),
            Color.rgb(194, 59, 79),
            Color.rgb(196, 60, 78),
            Color.rgb(197, 61, 77),
            Color.rgb(199, 62, 76),
            Color.rgb(200, 62, 75),
            Color.rgb(201, 63, 74),
            Color.rgb(203, 64, 73),
            Color.rgb(204, 65, 72),
            Color.rgb(205, 66, 71),
            Color.rgb(207, 68, 70),
            Color.rgb(208, 69, 68),
            Color.rgb(209, 70, 67),
            Color.rgb(210, 71, 66),
            Color.rgb(212, 72, 65),
            Color.rgb(213, 73, 64),
            Color.rgb(214, 74, 63),
            Color.rgb(215, 75, 62),
            Color.rgb(217, 77, 61),
            Color.rgb(218, 78, 59),
            Color.rgb(219, 79, 58),
            Color.rgb(220, 80, 57),
            Color.rgb(221, 82, 56),
            Color.rgb(222, 83, 55),
            Color.rgb(223, 84, 54),
            Color.rgb(224, 86, 52),
            Color.rgb(226, 87, 51),
            Color.rgb(227, 88, 50),
            Color.rgb(228, 90, 49),
            Color.rgb(229, 91, 48),
            Color.rgb(230, 92, 46),
            Color.rgb(230, 94, 45),
            Color.rgb(231, 95, 44),
            Color.rgb(232, 97, 43),
            Color.rgb(233, 98, 42),
            Color.rgb(234, 100, 40),
            Color.rgb(235, 101, 39),
            Color.rgb(236, 103, 38),
            Color.rgb(237, 104, 37),
            Color.rgb(237, 106, 35),
            Color.rgb(238, 108, 34),
            Color.rgb(239, 109, 33),
            Color.rgb(240, 111, 31),
            Color.rgb(240, 112, 30),
            Color.rgb(241, 114, 29),
            Color.rgb(242, 116, 28),
            Color.rgb(242, 117, 26),
            Color.rgb(243, 119, 25),
            Color.rgb(243, 121, 24),
            Color.rgb(244, 122, 22),
            Color.rgb(245, 124, 21),
            Color.rgb(245, 126, 20),
            Color.rgb(246, 128, 18),
            Color.rgb(246, 129, 17),
            Color.rgb(247, 131, 16),
            Color.rgb(247, 133, 14),
            Color.rgb(248, 135, 13),
            Color.rgb(248, 136, 12),
            Color.rgb(248, 138, 11),
            Color.rgb(249, 140, 9),
            Color.rgb(249, 142, 8),
            Color.rgb(249, 144, 8),
            Color.rgb(250, 145, 7),
            Color.rgb(250, 147, 6),
            Color.rgb(250, 149, 6),
            Color.rgb(250, 151, 6),
            Color.rgb(251, 153, 6),
            Color.rgb(251, 155, 6),
            Color.rgb(251, 157, 6),
            Color.rgb(251, 158, 7),
            Color.rgb(251, 160, 7),
            Color.rgb(251, 162, 8),
            Color.rgb(251, 164, 10),
            Color.rgb(251, 166, 11),
            Color.rgb(251, 168, 13),
            Color.rgb(251, 170, 14),
            Color.rgb(251, 172, 16),
            Color.rgb(251, 174, 18),
            Color.rgb(251, 176, 20),
            Color.rgb(251, 177, 22),
            Color.rgb(251, 179, 24),
            Color.rgb(251, 181, 26),
            Color.rgb(251, 183, 28),
            Color.rgb(251, 185, 30),
            Color.rgb(250, 187, 33),
            Color.rgb(250, 189, 35),
            Color.rgb(250, 191, 37),
            Color.rgb(250, 193, 40),
            Color.rgb(249, 195, 42),
            Color.rgb(249, 197, 44),
            Color.rgb(249, 199, 47),
            Color.rgb(248, 201, 49),
            Color.rgb(248, 203, 52),
            Color.rgb(248, 205, 55),
            Color.rgb(247, 207, 58),
            Color.rgb(247, 209, 60),
            Color.rgb(246, 211, 63),
            Color.rgb(246, 213, 66),
            Color.rgb(245, 215, 69),
            Color.rgb(245, 217, 72),
            Color.rgb(244, 219, 75),
            Color.rgb(244, 220, 79),
            Color.rgb(243, 222, 82),
            Color.rgb(243, 224, 86),
            Color.rgb(243, 226, 89),
            Color.rgb(242, 228, 93),
            Color.rgb(242, 230, 96),
            Color.rgb(241, 232, 100),
            Color.rgb(241, 233, 104),
            Color.rgb(241, 235, 108),
            Color.rgb(241, 237, 112),
            Color.rgb(241, 238, 116),
            Color.rgb(241, 240, 121),
            Color.rgb(241, 242, 125),
            Color.rgb(242, 243, 129),
            Color.rgb(242, 244, 133),
            Color.rgb(243, 246, 137),
            Color.rgb(244, 247, 141),
            Color.rgb(245, 248, 145),
            Color.rgb(246, 250, 149),
            Color.rgb(247, 251, 153),
            Color.rgb(249, 252, 157),
            Color.rgb(250, 253, 160),
            Color.rgb(252, 254, 164)
            // (End of INFERNO_COLORMAP array)
    };

    // Set MFCC data and silence markers
    public void setMFCC(float[][] mfccData, boolean[] silenceMarkers) {
        this.mfccData = mfccData;
        this.silenceMarkers = silenceMarkers;
        invalidate(); // Trigger a redraw
    }

    // Clear the visualizer
    public void clear() {
        this.mfccData = null;
        this.silenceMarkers = null;
        invalidate(); // Trigger a redraw to clear the screen
    }


}
