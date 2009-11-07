
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
 * A power metering algorithm.
 */
public final class SignalPower {

    // ******************************************************************** //
    // Constructor.
    // ******************************************************************** //

    /**
     * Only static methods are provided in this class.
     */
    private SignalPower() {
    }
    

    // ******************************************************************** //
    // Algorithm.
    // ******************************************************************** //

    /**
     * Calculate the power of the given input signal.
     * 
     * @param   sdata       Buffer containing the input samples to process.
     * @param   off         Offset in sdata of the data of interest.
     * @param   samples     Number of data samples to process.
     * @param   out         A float array in which the results will be placed
     *                      Must have space for two entries, which will be
     *                      set to:
     *                      <ul>
     *                      <li>The bias, i.e. the offset of the average
     *                      signal value from zero.
     *                      <li>The range, i.e. the absolute value of the largest
     *                      departure from the bias level.
     *                      </ul>
     * @throws  NullPointerException    Null output array reference.
     * @throws  ArrayIndexOutOfBoundsException  Output array too small.
     */
    public final static void biasAndRange(short[] sdata, int off, int samples,
                                          float[] out)
    {
        // Find the max and min signal values, and calculate the bias.
        short min =  32767;
        short max = -32768;
        int total = 0;
        for (int i = off; i < off + samples; ++i) {
            final short val = sdata[i];
            total += val;
            if (val < min)
                min = val;
            if (val > max)
                max = val;
        }
        final float bias = (float) total / (float) samples;
        final float bmin = min + bias;
        final float bmax = max - bias;
        final float range = Math.abs(bmax - bmin) / 2f;
        
        out[0] = bias;
        out[1] = range;
    }
    
    
    /**
     * Calculate the power of the given input signal.
     * 
     * @param   sdata       Buffer containing the input samples to process.
     * @param   off         Offset in sdata of the data of interest.
     * @param   samples     Number of data samples to process.
     * @return              The calculated power in dB; zero represents
     *                      100dB and 1 is 0dB (maximum power).
     */
    public final static float calculatePowerDb(short[] sdata, int off, int samples) {
        /**
        Calculate power of the signal depending on format.

        Since the signal may not have an average value of 0 precisely,
        we shouldn't simply calculate:

        sum_for_all_samples (pulse_value²) / number_of_samples

        but this formula assumes that the average is zero, which is not
        always true (for example, in 8 bits on a Sound Blaster 64,
        there is always a shift by one unit.

        We could calculate in two passes, first the average, then the
        power of the measure minus the average. But we can do this in
        one pass.

        Let measure = signal + bias,
        where measure is the pulse value,
        signal is what we want,
        bias is a constant, such that the average of signal is zero.
        
        What we want is the value of: power = sum_for_all_samples (signal²)

        Let's calculate in the same pass:
        a=sum_for_all_samples (measure²)
        and
        b=sum_for_all_samples (measure)
        
        Then a and b are equivalent to:
        a = sum_for_all_samples (measure²)
          = sum_for_all_samples ((signal + bias)²)
          = sum_for_all_samples (signal² + bias²)
          = sum_for_all_samples (signal²) + number_of_samples * bias²
      
        and 
        b = sum_for_all_samples (measure)
          = bias * number_of_samples
        that is, number_of_samples * bias² = b² / number_of_samples

        So a = power + b² / number_of_samples

        And power = a - b² / number_of_samples

        So we've got the correct power of the signal in one pass.
        
      */



        long b = 0;
        long a = 0;
        float floatPower = 0;
        for (int i = 0; i < samples; i++) {
            /* Since we calculate the square of something that can be
              as big as +-32767 we assume a width of at least 32 bits
              for a signed int. Moreover, we add a thousand of these
              to calculate power, so 32 bits aren't enough. I chose 64
              bits unsigned int for precision. We could have switched
              to float or double instead... */
            final int v = sdata[off + i];
            /* Note: we calculate max value anyway, to detect clipping */
            a += (v * v);
            b += v;
        }

        /* Ok for raw power. Now normalize it. */
        long power = a - b * b / samples;

        int maxAmp = 32768;
        floatPower = ((float) power) / ((float) maxAmp) / ((float) maxAmp) / ((float) samples);

        /* we want leftmost to be 100dB
          (though signal-to-noise ratio can't be more than 96.33dB in power)
          and rightmost to be 0dB (maximum power) */
        float dBvalue = 1f + (float) Math.log10(floatPower) / 10f;
        return dBvalue;
    }
    
}

