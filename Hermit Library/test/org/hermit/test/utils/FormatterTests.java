
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
	// Test Definitions.
	// ******************************************************************** //

	private static final class TestData {
		public TestData(int val, int field, boolean signed, String res) {
			this.val = val;
			this.field = field;
			this.signed = signed;
			this.expect = res;
		}

		private void run(char[] buf, int off) {
		    CharFormatter.formatInt(buf, off, val, field, signed);
		    String res = new String(buf, off, field);
		    assertEquals(expect, res);
		}

		private int val;
		private int field;
		private boolean signed;
		private String expect;
	}
	
	
	/**
	 * Table of test data.  Each row contains:
	 *   - Test name, ellipsoid to use
	 *   - Station 1 latitude, degrees; longitude, degrees
	 *   - Station 2 latitude, degrees; longitude, degrees
	 *   - Forward azimuth, degrees; back azimuth, degrees
	 *   - Distance, metres
	 */
	private static final TestData[] testData = {
		new TestData(173, 7, false, "    173"),
        new TestData(173, 3, false, "173"),
        new TestData(173, 3, true, " 73"),
        new TestData(173, 2, true, " 3"),
        new TestData(-173, 7, false, "    173"),
        new TestData(-173, 7, true, "   -173"),
        new TestData(-173, 3, false, "173"),
        new TestData(-173, 3, true, "-73"),
        new TestData(-173, 2, true, "-3"),
	};


	// ******************************************************************** //
	// Tests.
	// ******************************************************************** //

	public void testFormatInt() {
	    char[] buf = new char[63];
		for (TestData test : testData)
			test.run(buf, 13);
	}
		
}

