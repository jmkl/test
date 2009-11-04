
/**
 * dsp: various digital signal processing algorithms
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


package org.hermit.dsp;


/**
 * Implementation of the Cooley–Tukey FFT algorithm by Tsan-Kuang Lee,
 * for real-valued data and results:
 * http://www.ling.upenn.edu/~tklee/Projects/dsp/
 * 
 * <p>His copyright statement: "Do whatever you want with the code.
 * Feedbacks and improvement welcome."
 * 
 * <p>Usage: create an FFTTransformer with a specified block size, to
 * pre-allocate the necessary resources.  Then, for each block that
 * you want to transform:
 * <ul>
 * <li>Call {@link #setInput(float[], int, int)} to
 *     supply the input data.  The execution of this method is the only
 *     time your input buffer will be accessed; the data is converted
 *     to complex and copied to a different buffer.
 * <li>Call {@link #transform()} to actually do the FFT.  This is the
 *     time-consuming part.
 * <li>Call {@link #getResults(float[])} to get the results into
 *     your output buffer.
 * </ul>
 * <p>The flow is broken up like this to allow you to make best use of
 * locks.  For example, if the input buffer is also accessed by a thread
 * which reads from the audio, you only need to lock out that thread during
 * {@link #setInput(float[], int, int)}, not the entire FFT process.
 */
public final class FFTTransformer {

    // ******************************************************************** //
    // Constructor.
    // ******************************************************************** //

    /**
     * Create an FFT transformer for a given sample size.  This preallocates
     * resources appropriate to that block size.
     * 
     * @param   size        The number of samples in a block that we will
     *                      be asked to transform.  Must be a power of 2.
     * @throws  IllegalArgumentException    Invalid parameter.
     */
    public FFTTransformer(int size) {
        if (!isPowerOf2(size))
            throw new IllegalArgumentException("size for FFT must" +
                                               " be a power of 2 (was " + size + ")");
        
        n = size;
        nu = (int) (Math.log(size) / Math.log(2));
        xre = new float[n];
        xim = new float[n];
    }
    

    // ******************************************************************** //
    // Data Setup.
    // ******************************************************************** //

    /**
     * Set up a new data block for the FFT algorithm.  The data in
     * the provided buffer will be copied out, and that buffer
     * will not be referenced again.  This is separated
     * out from the main computation to allow for more efficient use
     * of locks.
     * 
     * @param   input       The input data buffer.
     * @param   off         Offset in the buffer at which the data to
     *                      be transformed starts.
     * @param   count       Number of samples in the data to be
     *                      transformed.  Must be the same as the size
     *                      parameter that was given to the constructor.
     * @throws  IllegalArgumentException    Invalid data size.
     */
    public final void setInput(float[] input, int off, int count) {
        if (count != n)
            throw new IllegalArgumentException("bad input count in FFT:" +
                                               " constructed for " + n +
                                               "; given " + input.length);
       
        for (int i = 0; i < n; i++) {
            xre[i] = input[i];
            xim[i] = 0.0f;
        }
    }
    

    /**
     * Set up a new data block for the FFT algorithm.  The data in
     * the provided buffer will be copied out, and that buffer
     * will not be referenced again.  This is separated
     * out from the main computation to allow for more efficient use
     * of locks.
     * 
     * @param   input       The input data buffer.
     * @param   off         Offset in the buffer at which the data to
     *                      be transformed starts.
     * @param   count       Number of samples in the data to be
     *                      transformed.  Must be the same as the size
     *                      parameter that was given to the constructor.
     * @throws  IllegalArgumentException    Invalid data size.
     */
    public final void setInput(short[] input, int off, int count) {
        if (count != n)
            throw new IllegalArgumentException("bad input count in FFT:" +
                                               " constructed for " + n +
                                               "; given " + input.length);
       
        for (int i = 0; i < n; i++) {
            xre[i] = (float) input[i] / 32768f;
            xim[i] = 0.0f;
        }
    }
    

    // ******************************************************************** //
    // Transform.
    // ******************************************************************** //

    /**
     * Transform the data provided in the last call to setInput.
     */
    public final void transform() {
        int n2 = n / 2;
        int nu1 = nu - 1;
        float tr, ti, p, arg, c, s;
        int k = 0;

        for (int l = 1; l <= nu; l++) {
            while (k < n) {
                for (int i = 1; i <= n2; i++) {
                    p = bitrev (k >> nu1, nu);
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
            r = bitrev (k, nu);
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
    }


    // ******************************************************************** //
    // Results.
    // ******************************************************************** //

    /**
     * Get the real results of the last transformation.
     * 
     * @param   buffer  Buffer in which the real part of the results
     *                  will be placed.  This buffer must be half the
     *                  length of the input block.  If transform() has
     *                  not been called, the results will be garbage.
     * @return          The parameter buffer.
     * @throws  IllegalArgumentException    Invalid buffer size.
     */
    public final float[] getResults(float[] buffer) {
        if (buffer.length != n / 2)
            throw new IllegalArgumentException("bad output buffer size in FFT:" +
                                               " must be " + (n / 2) +
                                               "; given " + buffer.length);
       
        buffer[0] = (float) (Math.sqrt(xre[0]*xre[0] + xim[0]*xim[0]))/n;
        for (int i = 1; i < n/2; i++)
            buffer[i]= 2 * (float) (Math.sqrt(xre[i]*xre[i] + xim[i]*xim[i]))/n;
        return buffer;
    }


    // ******************************************************************** //
    // Internal Utilities.
    // ******************************************************************** //

    /**
     * Reverse the lowest n bits of j.
     * 
     * @param   j       Number to be reversed.
     * @param   n       Number of low-order bits of j which are significant
     *                  and to be reversed.
     */
    private static final int bitrev(int j, int n) {
        int j1 = j;
        int j2;
        int k = 0;
        for (int i = 1; i <= n; i++) {
            j2 = j1 / 2;
            k  = 2*k + j1 - 2*j2;
            j1 = j2;
        }
        return k;
    }

    
    /**
     * Returns true if the argument is power of 2.
     * 
     * @param   n       The number to test.
     * @return          true if the argument is power of 2.
     */
    private static final boolean isPowerOf2(int n) {
        return (n > 0) && ((n & (n - 1)) == 0);
    }

    
    // ******************************************************************** //
    // Private Data.
    // ******************************************************************** //

    private final int n;
    private final int nu;
    private final float[] xre;
    private final float[] xim;

}

