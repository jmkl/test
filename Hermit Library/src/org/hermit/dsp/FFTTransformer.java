
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
        
        blockSize = size;
        
        // Calculate the base 2 log of size; the number of bits in an index
        // into the arrays.
        blockIndexBits = (int) (Math.log(size) / Math.log(2));
        
        // Allocate working data arrays.
        xre = new float[blockSize];
        xim = new float[blockSize];
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
        if (count != blockSize)
            throw new IllegalArgumentException("bad input count in FFT:" +
                                               " constructed for " + blockSize +
                                               "; given " + input.length);
       
        for (int i = 0; i < blockSize; i++) {
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
        if (count != blockSize)
            throw new IllegalArgumentException("bad input count in FFT:" +
                                               " constructed for " + blockSize +
                                               "; given " + input.length);
       
        for (int i = 0; i < blockSize; i++) {
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
        int n2 = blockSize / 2;
        int nu1 = blockIndexBits - 1;

        for (int l = 1; l <= blockIndexBits; l++) {
            int k = 0;
            while (k < blockSize) {
                for (int i = 1; i <= n2; i++) {
                    final int k2 = k + n2;
                    final float r2 = xre[k2];
                    final float i2 = xim[k2];
                    final float p = bitrev(k >> nu1, blockIndexBits);
                    final float arg = 2 * (float) Math.PI * p / blockSize;
                    final float c = (float) Math.cos (arg);
                    final float s = (float) Math.sin (arg);
                    final float tr = r2 * c + i2 * s;
                    final float ti = i2 * c - r2 * s;
                    xre[k2] = xre[k] - tr;
                    xim[k2] = xim[k] - ti;
                    xre[k] += tr;
                    xim[k] += ti;
                    k++;
                }
                k += n2;
            }
            --nu1;
            n2 = n2 / 2;
        }
        
        for (int k = 0; k < blockSize; ++k) {
            final int r = bitrev(k, blockIndexBits);
            if (r > k) {
                final float tr = xre[k];
                final float ti = xim[k];
                xre[k] = xre[r];
                xim[k] = xim[r];
                xre[r] = tr;
                xim[r] = ti;
            }
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
        if (buffer.length != blockSize / 2)
            throw new IllegalArgumentException("bad output buffer size in FFT:" +
                                               " must be " + (blockSize / 2) +
                                               "; given " + buffer.length);
       
        buffer[0] = (float) (Math.sqrt(xre[0]*xre[0] + xim[0]*xim[0]))/blockSize;
        for (int i = 1; i < blockSize / 2; i++)
            buffer[i] = 2 * (float) (Math.sqrt(xre[i]*xre[i] + xim[i]*xim[i]))/blockSize;
        return buffer;
    }


    /**
     * Get the rolling average real results of the last n transformations.
     * 
     * @param   average     Buffer in which the averaged real part of the
     *                      results will be maintained.  This buffer must be
     *                      half the length of the input block.  It is
     *                      important that this buffer is kept intact and
     *                      undisturbed between calls, as the average
     *                      calculation for each value depends on the
     *                      previous average.
     * @param   histories   Buffer in which the historical values of the
     *                      results will be kept.  This must be a rectangular
     *                      array, the first dimension being the same as
     *                      average.  The second dimension determines the
     *                      length of the history, and hence the time over
     *                      which values are averaged.  It is
     *                      important that this buffer is kept intact and
     *                      undisturbed between calls.
     * @param   index       Current history index.  The caller needs to pass
     *                      in zero initially, and save the return value
     *                      of this method to pass in as index next time.
     * @return              The updated index value.  Pass this in as
     *                      the index parameter next time around.
     * @throws  IllegalArgumentException    Invalid buffer size.
     */
    public final int getResults(float[] average, float[][] histories, int index) {
        if (average.length != blockSize / 2)
            throw new IllegalArgumentException("bad history buffer size in FFT:" +
                                               " must be " + (blockSize / 2) +
                                               "; given " + average.length);
        if (histories.length != blockSize / 2)
            throw new IllegalArgumentException("bad average buffer size in FFT:" +
                                               " must be " + (blockSize / 2) +
                                               "; given " + histories.length);
    
        // Update the index.
        int historyLen = histories[0].length;
        if (++index >= historyLen)
            index = 0;
       
        // Now do the rolling average of each value.
        for (int i = 0; i < blockSize/2; i++) {
            final float val = (i == 0 ? 1 : 2) *
                    (float) (Math.sqrt(xre[i]*xre[i] + xim[i]*xim[i]))/blockSize;
            
            final float[] hist = histories[i];
            final float prev = hist[index];
            hist[index] = val;
            average[i] -= prev / historyLen;
            average[i] += val / historyLen;
        }
        
        return index;
    }


    // ******************************************************************** //
    // Results Analysis.
    // ******************************************************************** //

    /**
     * Given the results of an FFT, identify prominent frequencies
     * in the spectrum.
     * 
     * @param   spectrum    Audio spectrum data, as returned by
     *                      {@link #getResults(float[])}.
     * @param   results     Buffer into which the results will be placed.
     * @return              The parameter buffer.
     * @throws  IllegalArgumentException    Invalid buffer size.
     */
    public final int findKeyFrequencies(float[] spectrum, float[] results) {
        final int len = spectrum.length;
        
        // Find the average strength.
        float average = 0f;
        for (int i = 0; i < len; ++i) {
            average += spectrum[i];
        }
        average /= len;
        
        // Find all excursions above 2*average.  Group adjacent highs
        // together.
        int count = 0;
        for (int i = 0; i < len && count < results.length; ++i) {
            if (spectrum[i] > 2 * average) {
                // Compute the weighted average frequency of this peak.
                float tot = 0f;
                float wavg = 0f;
                int j;
                for (j = i; j < len && spectrum[j] > 3 * average; ++j) {
                    tot += spectrum[j];
                    wavg += spectrum[j] * (float) j;
                }
                wavg /= tot;
                results[count++] = wavg;
                
                // Skip past this peak.
                i = j;
            }
        }
 
        return count;
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
            k  = 2 * k + j1 - 2 * j2;
            j1 = j2;
        }
        return k;
    }


    /**
     * Reverse the lowest n bits of j.
     * 
     * @param   j       Number to be reversed.
     * @param   n       Number of low-order bits of j which are significant
     *                  and to be reversed.
     */
    private static final int bitrev2(int j, int n) {
        int r = 0;
        for (int i = 0; i < n; ++i, j >>= 1)
            r = (r << 1) | (j & 0x0001);
        return r;
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

    // The size of an input data block.
    private final int blockSize;
    
    // The base 2 log of blockSize; the number of bits in an index
    // into the arrays.
    private final int blockIndexBits;
    
    // Working arrays -- real and imaginary parts of the data being processed.
    private final float[] xre;
    private final float[] xim;

}

