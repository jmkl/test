
/**
 * test: test code.
 * <br>Copyright 2004-2009 Ian Cameron Smith
 *
 * <p>This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as
 * published by the Free Software Foundation (see COPYING).
 *
 * <p>This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * General Public License for more details.
 *
 * <p>You should have received a copy of the GNU General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA
 * 02111-1307, USA.
 */

package org.hermit.test.dsp;

import org.hermit.dsp.SignalPower;


/**
 * Test the signal power calculations.
 */
public class PowerTest {

    // ******************************************************************** //
    // Signal generation.
    // ******************************************************************** //

    private static short[] makeSine(double max, int rate, float freq, float len) {
        // Make a buffer of the right length.
        int n = (int) (rate * len);
        short[] buf = new short[n];
        
        // The length of 1 cycle in samples.
        int c = (int) (rate / freq);
        
        // Fill it with a sine wave.
        for (int i = 0; i < n; ++i) {
            long val = Math.round(Math.sin((double) i / c * Math.PI * 2) * max);
            if (val < Short.MIN_VALUE)
                buf[i] = Short.MIN_VALUE;
            else if (val > Short.MAX_VALUE)
                buf[i] = Short.MAX_VALUE;
            else
                buf[i] = (short) val;
        }
        
        return buf;
    }
    
    
    // ******************************************************************** //
    // Main.
    // ******************************************************************** //
    
	/**
	 * @param args
	 */
	public static void main(String[] args) {
        short[] zero = makeSine(0f, 8000, 400f, 0.1f);
        double zerop = SignalPower.calculatePowerDb(zero, 0, zero.length);
        System.out.format("Zero: %10.5f\n", zerop);
        
        short[] one = makeSine(0.5f, 8000, 400f, 0.1f);
        double onep = SignalPower.calculatePowerDb(one, 0, one.length);
        System.out.format("One: %10.5f\n", onep);
        
        short[] tiny = makeSine(0.55f, 8000, 400f, 0.1f);
        double tinyp = SignalPower.calculatePowerDb(tiny, 0, tiny.length);
        System.out.format("Tiny: %10.5f\n", tinyp);
        
	    short[] min = makeSine(1, 8000, 400f, 0.1f);
	    double minp = SignalPower.calculatePowerDb(min, 0, min.length);
	    System.out.format("Min: %10.5f\n", minp);
        
        short[] full = makeSine(32768, 8000, 400f, 0.1f);
        double fullp = SignalPower.calculatePowerDb(full, 0, full.length);
        System.out.format("Full: %10.5f\n", fullp);
	    
        short[] max = makeSine(200000, 8000, 80f, 0.1f);
        double maxp = SignalPower.calculatePowerDb(max, 0, max.length);
        System.out.format("Max: %10.5f\n", maxp);
	}
	
	
    // ******************************************************************** //
    // Private Constants.
    // ******************************************************************** //
	
}

