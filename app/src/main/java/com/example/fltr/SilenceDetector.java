package com.example.fltr;

public class SilenceDetector {

    public static boolean[] computeSilenceMarkers(float[][] mfcc, float thresholdDb) {
        boolean[] m = new boolean[mfcc.length];
        for (int i=0; i<m.length; i++) {
            float sum=0;
            for (float c:mfcc[i]) sum+=c*c;
            float db = 10f*(float)Math.log10(sum+1e-8f);
            m[i] = db<thresholdDb;
        }
        return m;
    }
}
