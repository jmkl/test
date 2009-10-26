
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


import static java.lang.Math.PI;
import static java.lang.Math.abs;
import static java.lang.Math.round;
import static java.lang.Math.toDegrees;
import static java.lang.Math.toRadians;

import java.text.NumberFormat;


/**
 * Utilities for handling and formatting angles, including latitudes
 * and longitudes.
 *
 * @author	Ian Cameron Smith
 */
public class CharFormatter
{

	// ******************************************************************** //
	// Public Constants.
	// ******************************************************************** //

	/**
	 * Half pi; a quarter circle in radians; same as 90 degrees.
	 */
	public static final double HALFPI = PI / 2;


	/**
	 * Two times pi; a circle in radians; same as 360 degrees.
	 */
	public static final double TWOPI = PI * 2;


	// ******************************************************************** //
	// Public Constructors.
	// ******************************************************************** //

	/**
	 * Create an Angle from an angle given in radians.
	 * 
	 * @param	radians		Source angle in radians.
	 */
	private CharFormatter() {
	}

	
	// ************************************************************************ //
	// Static Formatting Utilities.
	// ************************************************************************ //

    /**
     * Format a float to a field width of 6, with 1
     * decimals.  MUCH faster than String.format.
     */
    public static final void formatInt(char[] buf, int off, int val,
                                       int field, boolean signed)
    {
        int intDigits = field - (signed ? 1 : 0);
        int sign = val >= 0 ? 1 : -1;
        val *= sign;
        
        int last = 0;
        for (int i = off + field - 1; i >= off + field - intDigits; --i) {
            if (val == 0 && i < off + field - 1) {
                buf[i] = ' ';
            } else {
                buf[i] = (char) ('0' + val % 10);
                val /= 10;
                last = i;
            }
        }
        if (signed)
            buf[last - 1] = sign > 0 ? ' ' : '-';
    }


    /**
     * Format a float to a field width of 6, with 1
     * decimals.  MUCH faster than String.format.
     */
    public static final void formatFloat(char[] buf, int off, double val, int field, int frac, boolean signed) {
        int intDigits = field - frac - (signed ? 2 : 1);
        
        int intPart = (int) val;
        int fracPart = (int) ((val - intPart) * 10);

        formatInt(buf, off, intPart, field - frac - 1, signed);
                
        String b = "" + intPart;
        String a = "" + fracPart;
        StringBuilder res = new StringBuilder("    .0");
        int bs = intDigits - b.length();
        res.replace((bs < 0 ? 0 : bs), intDigits, b);
        res.replace(field - a.length(), field, a);
        return res.toString();
    }

   
	/**
	 * Format an angle as a bearing.
	 *
	 * @param	val			The value to format.
	 * @return				The formatted value.
	 */
	public static String formatBearing(double val) {
		return Math.round(val) + "°";
	}


	/**
	 * Format an angle for user display in degrees and minutes.
	 * Negative angles are formatted with a "-" sign.
	 *
	 * @param	angle		The angle to format.
	 * @return				The formatted angle.
	 */
	public static String formatDegMin(double angle) {
		return formatDegMin(angle, ' ', '-');
	}


	/**
	 * Format a latitude or longitude angle as a string in the format
	 * "W171° 15.165'".
	 *
	 * @param	angle		Angle to format.
	 * @param	pos			Sign character to use if positive.
	 * @param	neg			Sign character to use if negative.
	 * @return				The formatted angle.
	 */
	public static String formatDegMin(double angle, char pos, char neg) {
		StringBuilder sb = new StringBuilder(12);
		formatDegMin(angle, pos, neg, sb);
		return sb.toString();
	}


	/**
	 * Format a latitude or longitude angle as a string in the format
	 * "W171°15.165'".  Place the result in a supplied StringBuilder.
	 * 
	 * The StringBuilder will be set to the required length, 12.  For 
	 * best efficiency, leave it at that length.
	 * 
	 * @param	angle		Angle to format.
	 * @param	pos			Sign character to use if positive.
	 * @param	neg			Sign character to use if negative.
	 * @param	sb			StringBuilder to write the result into.
	 */
	public static void formatDegMin(double angle,
									char pos, char neg, StringBuilder sb)
	{
		if (sb.length() != 12)
			sb.setLength(12);
		
		if (angle < 0) {
			sb.setCharAt(0, neg);
			angle = -angle;
		} else
			sb.setCharAt(0, pos);
	
		int deg = (int) angle;
		int min = (int) (angle * 60.0 % 60.0);
		int frac = (int) (angle * 60000.0 % 1000.0);

		sb.setCharAt( 1, deg < 100 ? ' ' : (char) ('0' + deg / 100));
		sb.setCharAt( 2, deg < 10 ? ' ' : (char) ('0' + deg / 10 % 10));
		sb.setCharAt( 3, (char) ('0' + deg % 10));
		sb.setCharAt( 4, '°');
		sb.setCharAt( 5, (char) ('0' + min / 10));
		sb.setCharAt( 6, (char) ('0' + min % 10));
		sb.setCharAt( 7, '.');
		sb.setCharAt( 8, (char) ('0' + frac / 100));
		sb.setCharAt( 9, (char) ('0' + frac / 10 % 10));
		sb.setCharAt(10, (char) ('0' + frac % 10));
		sb.setCharAt(11, '\'');
	}
	

	/**
	 * Format an angle for user display in degrees and minutes.
	 *
	 * @param	angle		The angle to format.
	 * @return				The formatted angle.
	 */
	public static String formatDegMinSec(double angle) {
		return formatDegMinSec(angle, ' ', '-');
	}
	
	
	/**
	 * Format an angle for user display in degrees and minutes.
	 *
	 * @param	angle		The angle to format.
	 * @param	posSign		Sign to use for positive values; none if null.
	 * @param	negSign		Sign to use for negative values; none if null.
	 * @return				The formatted angle.
	 */
	public static String formatDegMinSec(double angle, char posSign, char negSign) {
		char sign = angle >= 0 ? posSign : negSign;
		angle = Math.abs(angle);

		int deg = (int) angle;
		angle = (angle - deg) * 60.0;
		int min = (int) angle;
		angle = (angle - min) * 60.0;
		double sec = angle;

		// Rounding errors?
		if (sec >= 60.0) {
			sec = 0;
			++min;
		}
		if (min >= 60) {
			min -= 60;
			++deg;
		}
		
		return String.format("%s%3d° %2d' %8.5f\"", sign, deg, min, sec);
		//return sign + deg + "° " + min + "' " + angleFormat.format(sec) + "\"";
	}


	/**
	 * Format a latitude and longitude for user display in degrees and
	 * minutes.
	 *
	 * @param	lat			The latitude.
	 * @param	lon			The longitude.
	 * @return				The formatted angle.
	 */
	public static String formatLatLon(double lat, double lon) {
		return formatDegMin(lat, 'N', 'S') + " " + formatDegMin(lon, 'E', 'W');
	}

	
	/**
	 * Format an angle for user display as a right ascension.
	 *
	 * @param	angle		The angle to format.
	 * @return				The formatted angle.
	 * TODO: units!
	 */
	public static String formatRightAsc(double angle) {
		if (angle < 0)
			angle += 360.0;
		double hours = angle / 15.0;
		
		int h = (int) hours;
		hours = (hours - h) * 60.0;
		int m = (int) hours;
		hours = (hours - m) * 60.0;
		double s = hours;

		// Rounding errors?
		if (s >= 60.0) {
			s = 0;
			++m;
		}
		if (m >= 60) {
			m -= 60;
			++h;
		}
		if (h >= 24) {
			h -= 24;
		}
		
		return String.format("%02dh %02d' %08.5f\"", h, m, s);
	}

}

