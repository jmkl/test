
/**
 * geo: geographical utilities.
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

package org.hermit.test.utils;


import junit.framework.TestCase;

import org.hermit.utils.CharFormatter;


/**
 * Test geodetic calculations.
 *
 * @author	Ian Cameron Smith
 */
public class FormatterTests
    extends TestCase
{

    // ******************************************************************** //
    // Test Framework.
    // ******************************************************************** //

    protected void setUp() {
        buf = new char[63];
    }


    private void run(int off, int val, int field, boolean signed, String expect) {
        CharFormatter.formatInt(buf, off, val, field, signed);
        String res = new String(buf, off, field);
        assertEquals(expect, res);
    }

    private void run(int off, double val, int field, int frac, boolean signed, String expect) {
        CharFormatter.formatFloat(buf, off, val, field, frac, signed);
        String res = new String(buf, off, field);
        assertEquals(expect, res);
    }

    
    // ******************************************************************** //
    // Integer Tests.
    // ******************************************************************** //

    public void testPosIntUns() {
        run(13, 173, 7, false, "    173");
        run(13, 173, 3, false, "173");
    }


    public void testZeroIntUns() {
        run(13, 0, 7, false, "      0");
        run(13, 0, 1, false, "0");
    }


    public void testNegIntUns() {
        run(13, -173, 7, false, "    173");
        run(13, -173, 3, false, "173");
    }


    public void testPosIntSgn() {
        run(13, 173, 3, true, " 73");
        run(13, 173, 2, true, " 3");
    }


    public void testZeroIntSgn() {
        run(13, 0, 7, true, "      0");
        run(13, 0, 2, true, " 0");
    }


    public void testNegIntSgn() {
        run(13, -173, 7, true, "   -173");
        run(13, -173, 3, true, "-73");
        run(13, -173, 2, true, "-3");
    }


    // ******************************************************************** //
    // Float Tests.
    // ******************************************************************** //

    public void testPosFloatUns() {
        run(13, 173.45678, 7, 2, false, " 173.45");
        run(13, 173.45678, 7, 3, false, "173.456");
        run(13, 73.00678, 7, 4, false, "73.0067");
    }


    public void testZeroFloatUns() {
        run(13, 0, 7, 2, false, "   0.00");
        run(13, 0, 7, 3, false, "  0.000");
    }


    public void testNegFloatUns() {
        run(13, -173.45678, 7, 2, false, " 173.45");
        run(13, -173.45678, 7, 3, false, "173.456");
        run(13, -73.00678, 7, 4, false, "73.0067");
    }


    public void testPosFloatSgn() {
        run(13, 173.45678, 7, 2, true, " 173.45");
        run(13, 173.45678, 7, 3, true, " 73.456");
        run(13, 73.00678, 7, 4, true, " 3.0067");
    }


    public void testZeroFloatSgn() {
        run(13, 0, 7, 2, true, "   0.00");
        run(13, 0, 7, 3, true, "  0.000");
    }


    public void testNegFloatSgn() {
        run(13, -173.45678, 7, 2, true, "-173.45");
        run(13, -173.45678, 7, 3, true, "-73.456");
        run(13, -73.00678, 7, 4, true, "-3.0067");
    }


    public void testSpeed() {
        final int COUNT = 100000;
        
        long j1 = System.currentTimeMillis();
        for (int i = 0; i < COUNT; ++i)
            String.format("%7.2f", -(i / 1345678f));
        long j2 = System.currentTimeMillis();
        
        long c1 = System.currentTimeMillis();
        for (int i = 0; i < COUNT; ++i)
            CharFormatter.formatFloat(buf, 13, -(i / 1345678f), 7, 2, true);
        long c2 = System.currentTimeMillis();
        
        // CharFormatter should be at least 10 times faster.
        assertTrue((c2 - c1) * 10 < (j2 - j1));
    }


    // ******************************************************************** //
    // Member Data.
    // ******************************************************************** //

    private char[] buf;

}

