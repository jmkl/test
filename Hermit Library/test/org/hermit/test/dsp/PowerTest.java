
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
    // Testing.
    // ******************************************************************** //
    
    private static void runTest(String name, double max, int rate, float freq, float len) {
        // Test on a buffer of all zeroes.
        short[] buf = makeSine(max, rate, freq, len);
        double power = SignalPower.calculatePowerDb(buf, 0, buf.length);
        System.out.format("%-8s@ %5d: %10.5f\n", name, rate, power);
    }
    
    
    private static void runAll(int rate, float freq) {
        // Test on a buffer of all zeroes.
        runTest("Zero", 0f, rate, freq, 0.1f);
        
        // A truly miniscule signal; every 40th sample is 1, all others
        // are zero.
        runTest("Tiny", 0.5f, rate, freq, 0.1f);
        
        // A very small signal; 5 1s, 15 0s, 5 -1s, 15 0s.
        runTest("Small", 0.55f, rate, freq, 0.1f);
        
        // Minimum "real" signal, oscillating between 1 and -1.
        runTest("Min", 1, rate, freq, 0.1f);
        
        // A full-range sine wave, from -32768 to 32767.
        runTest("Full", 32768, rate, freq, 0.1f);
        
        // Maximum saturated signal.
        runTest("Sat", 10000000, rate, freq, 0.1f);
        
        // Maximum saturated signal at a low frequency to reduce the small
        // values.  This is an unrealistically over-saturated signal.
        runTest("Oversat", 10000000, rate, 80f, 0.1f);
    }
    

    // ******************************************************************** //
    // Main.
    // ******************************************************************** //
    
	/**
	 * @param args
	 */
	public static void main(String[] args) {
	    // Run the tests at a couple of different sample rates.
	    runAll(8000, 1000);
        runAll(16000, 1000);
	}
	
	
    // ******************************************************************** //
    // Private Constants.
    // ******************************************************************** //
	
}

