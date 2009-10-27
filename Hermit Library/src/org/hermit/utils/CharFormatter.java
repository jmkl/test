
/**
 * utils: general utility functions.
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

package org.hermit.utils;


/**
 * Utilities for quickly formatting numbers into character buffers, without
 * memory allocations.  These routines are much faster than using
 * String.format, and can be used to avoid GC.
 *
 * @author	Ian Cameron Smith
 */
public class CharFormatter
{

	// ******************************************************************** //
	// Private Constructors.
	// ******************************************************************** //

	/**
	 * No instances of this class.
	 */
	private CharFormatter() {
	}

	
	// ************************************************************************ //
	// Static Formatting Utilities.
	// ************************************************************************ //

    /**
     * Format an integer into a fixed-width field.  MUCH faster
     * than String.format.
     * 
     * @param   buf         Buffer to place the result in.
     * @param   off         Offset within buf to start writing at.
     * @param   val         The value to format.
     * @param   field       Width of the field to format in.
     * @param   signed      Iff true, add a sign character, space for
     *                      positive, '-' for negative.  This takes
     *                      up one place in the given field width.
     */
    public static final void formatInt(char[] buf, int off, int val,
                                       int field, boolean signed)
    {
        int sign = val >= 0 ? 1 : -1;
        val *= sign;
        char schar = signed ? (sign < 0 ? '-' : ' ') : 0;
        formatInt(buf, off, val, field, schar, false);
    }


    /**
     * Format a floating-point value into a fixed-width field.  MUCH faster
     * than String.format.
     * 
     * @param   buf         Buffer to place the result in.
     * @param   off         Offset within buf to start writing at.
     * @param   val         The value to format.
     * @param   field       Width of the field to format in.
     * @param   frac        Number of digits after the decimal.
     * @param   signed      Iff true, add a sign character, space for
     *                      positive, '-' for negative.  This takes
     *                      up one place in the given field width.
     */
    public static final void formatFloat(char[] buf, int off, double val,
                                         int field, int frac, boolean signed)
    {
        int intDigits = field - frac - 1;
        int sign = val >= 0 ? 1 : -1;
        val *= sign;
        char schar = signed ? (sign < 0 ? '-' : ' ') : 0;
        int intPart = (int) val;
        double fracPart = val - intPart;
        for (int i = 0; i < frac; ++i)
            fracPart *= 10;

        formatInt(buf, off, intPart, intDigits, schar, false);
        buf[off + intDigits] = '.';
        formatInt(buf, off + intDigits + 1, (int) fracPart, frac, (char) 0, true);
    }


    /**
     * Internal integer formatter.
     * 
     * @param   buf         Buffer to place the result in.
     * @param   off         Offset within buf to start writing at.
     * @param   val         The value to format.  Must not be negative.
     * @param   field       Width of the field to format in.
     * @param   schar       Iff not zero, add this sign character.  This takes
     *                      up one place in the given field width.
     * @param   leadZero    Iff true, pad on the left with leading zeros
     *                      instead of spaces.
     */
    private static final void formatInt(char[] buf, int off, int val,
                                        int field, char schar, boolean leadZero)
    {
        int intDigits = field - (schar != 0 ? 1 : 0);
        
        int last = 0;
        for (int i = off + field - 1; i >= off + field - intDigits; --i) {
            if (val == 0 && !leadZero && i < off + field - 1) {
                buf[i] = ' ';
            } else {
                buf[i] = val == 0 ? '0' : (char) ('0' + val % 10);
                val /= 10;
                last = i;
            }
        }
        if (schar != 0) {
            buf[off] = ' ';
            buf[last - 1] = schar;
        }
    }


	/**
	 * Format an angle for user display in degrees and minutes.
	 * Negative angles are formatted with a "-" sign, as in
	 * "-171°15.165'".  Place the result in the supplied buffer.
     *
     * @param   buf         Buffer to place the result in.
     * @param   off         Offset within buf to start writing at.
	 * @param	angle		The angle to format.
     * @return              Number of characters written.
	 */
	public static int formatDegMin(char[] buf, int off, double angle) {
		return formatDegMin(buf, off, angle, ' ', '-', false);
	}


	/**
	 * Format an angle as a string in the format
	 * "W171°15.165'".  Place the result in the supplied buffer.
	 * 
     * @param   buf         Buffer to place the result in.
     * @param   off         Offset within buf to start writing at.
     * @param   angle       The angle to format.
	 * @param	pos			Sign character to use if positive.
	 * @param	neg			Sign character to use if negative.
     * @param   space       If true, leave a space after the sign and degrees.
     *                      Otherwise pack them.
     * @return              Number of characters written.
	 */
	public static int formatDegMin(char[] buf, int off, double angle,
									char pos, char neg, boolean space)
	{
	    int p = off;
	    
		if (angle < 0) {
			buf[p++] = neg;
			angle = -angle;
		} else
            buf[p++] = pos;
		if (space)
            buf[p++] = ' ';
		
		int deg = (int) angle;
		int min = (int) (angle * 60.0 % 60.0);
		int frac = (int) (angle * 60000.0 % 1000.0);
		
        formatInt(buf, p, deg, 3, (char) 0, false);
        p += 3;
        buf[p++] = '°';
        if (space)
            buf[p++] = ' ';
        
        formatInt(buf, p, min, 2, (char) 0, true);
        p += 2;
        buf[p++] = '.';
        
        formatInt(buf, p, frac, 3, (char) 0, true);
        p += 2;
        buf[p++] = '\'';
        
        return p - off;
	}
	

	/**
	 * Format a latitude and longitude for user display in degrees and
	 * minutes.  Place the result in the supplied buffer.
	 *
     * @param   buf         Buffer to place the result in.
     * @param   off         Offset within buf to start writing at.
	 * @param	lat			The latitude.
	 * @param	lon			The longitude.
     * @param   space       If true, leave a space after the sign and degrees.
     *                      Otherwise pack them.
	 * @return				Number of characters written.
	 */
	public static int formatLatLon(char[] buf, int off, double lat, double lon, boolean space) {
        int p = off;
        
		p += formatDegMin(buf, off, lat, 'N', 'S', space);
		buf[p++] = ' ';
		p += formatDegMin(buf, off, lon, 'E', 'W', space);
        
        return p - off;
	}

}

