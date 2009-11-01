
/**
 * Wind Blink: a wind meter for Android.
 * <br>Copyright 2009 Ian Cameron Smith
 *
 * <p>This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2
 * as published by the Free Software Foundation (see COPYING).
 * 
 * <p>This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 */


package org.hermit.windblink;


/**
 * FFT algorithm by Tsan-Kuang Lee:
 * http://www.ling.upenn.edu/~tklee/Projects/dsp/
 * 
 * His copyright statement: "Do whatever you want with the code.
 * Feedbacks and improvement welcome."
 */
public final class FourierTransformer {

    private final int n;
    private final int nu;
    private final float[] xre;
    private final float[] xim;

    public FourierTransformer(int size) {
        if (!isPowerOf2(size))
            throw new IllegalArgumentException("size for FFT must" +
                                               " be a power of 2 (was " + size + ")");
        
        n = size;
        nu = (int) (Math.log(size) / Math.log(2));
        xre = new float[n];
        xim = new float[n];
    }
    
    
    public final float[] fftMag(short[] input, int off, int count, float[] mag) {
        if (count != n)
            throw new IllegalArgumentException("bad input count in FFT:" +
                                               " constructed for " + n +
                                               "; given " + input.length);
        if (mag.length != n / 2)
            throw new IllegalArgumentException("bad output buffer size in FFT:" +
                                               " must be " + (n / 2) +
                                               "; given " + mag.length);
       
        int n2 = n / 2;
        int nu1 = nu - 1;
        float tr, ti, p, arg, c, s;
        for (int i = 0; i < n; i++) {
            xre[i] = (float) input[i] / 32768;
            xim[i] = 0.0f;
        }
        int k = 0;

        for (int l = 1; l <= nu; l++) {
            while (k < n) {
                for (int i = 1; i <= n2; i++) {
                    p = bitrev (k >> nu1);
                    arg = 2 * (float) Math.PI * p / n;
                    c = (float) Math.cos (arg);
                    s = (float) Math.sin (arg);
                    tr = xre[k+n2]*c + xim[k+n2]*s;
                    ti = xim[k+n2]*c - xre[k+n2]*s;
                    xre[k+n2] = xre[k] - tr;
                    xim[k+n2] = xim[k] - ti;
                    xre[k] += tr;
                    xim[k] += ti;
                    k++;
                }
                k += n2;
            }
            k = 0;
            nu1--;
            n2 = n2/2;
        }
        k = 0;
        int r;
        while (k < n) {
            r = bitrev (k);
            if (r > k) {
                tr = xre[k];
                ti = xim[k];
                xre[k] = xre[r];
                xim[k] = xim[r];
                xre[r] = tr;
                xim[r] = ti;
            }
            k++;
        }

        mag[0] = (float) (Math.sqrt(xre[0]*xre[0] + xim[0]*xim[0]))/n;
        for (int i = 1; i < n/2; i++)
            mag[i]= 2 * (float) (Math.sqrt(xre[i]*xre[i] + xim[i]*xim[i]))/n;
        return mag;
    }


    private int bitrev(int j) {
        int j2;
        int j1 = j;
        int k = 0;
        for (int i = 1; i <= nu; i++) {
            j2 = j1 / 2;
            k  = 2*k + j1 - 2*j2;
            j1 = j2;
        }
        return k;
    }

    /**
     * Returns true if the argument is power of 2.
     * 
     * @param n the number to test
     * @return true if the argument is power of 2
     */
    private static boolean isPowerOf2(int n) {
        return (n > 0) && ((n & (n - 1)) == 0);
    }
    
}

