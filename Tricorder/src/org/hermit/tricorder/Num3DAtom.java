
/**
 * Tricorder: turn your phone into a tricorder.
 * 
 * This is an Android implementation of a Star Trek tricorder, based on
 * the phone's own sensors.  It's also a demo project for sensor access.
 *
 *   This program is free software; you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License version 2
 *   as published by the Free Software Foundation (see COPYING).
 * 
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 */


package org.hermit.tricorder;

import android.view.SurfaceHolder;


/**
 * An atom which displays 3-axis sensor data in a numeric display.
 * 
 * This could be used, for example, to show the accelerometer or
 * compass values.
 */
class Num3DAtom
	extends TextAtom
{

	// ******************************************************************** //
	// Constructor.
	// ******************************************************************** //

	/**
	 * Set up this view.
	 * 
	 * @param	context			Parent application context.
     * @param	sh				SurfaceHolder we're drawing in.
	 * @param	gridCol			Colour for the framing elements.
	 * @param	plotCol			Colour for the data display.
	 */
	public Num3DAtom(Tricorder context, SurfaceHolder sh, int gridCol, int plotCol) {
		super(context, sh);
		
		setTextColor(plotCol);
		
		// Set up the text values array and all the static labels.
		textValues = new String[3][4];
		textValues[0][0] = getRes(R.string.lab_x);
		textValues[1][0] = getRes(R.string.lab_y);
		textValues[2][0] = getRes(R.string.lab_z);
		textValues[0][2] = getRes(R.string.lab_azi);
		textValues[1][2] = getRes(R.string.lab_alt);
		textValues[2][2] = getRes(R.string.lab_mag);
		
		// Define the display format.
		final String[] fields = {
				textValues[0][0], "88888888",
				textValues[2][2], "88888888",
		};
		setTextFields(fields, 3);
	}

	
	// ******************************************************************** //
	// Data Management.
	// ******************************************************************** //

	/**
	 * Set the given values as the new values for the displayed data.
	 * Update the display accordingly.
	 * 
	 * @param	values			The new X, Y and Z values.
	 * @param	mag				Their absolute magnitude value.
	 * @param	az 				The azimuth calculated from X and Y.
	 * @param	alt 			The altitude calculated from Z and mag.
	 */
	public void setValues(float[] values, float mag, float az, float alt) {
		float xv = values[0];
		float yv = values[1];
		float zv = values[2];
		
		textValues[0][1] = format(xv);
		textValues[1][1] = format(yv);
		textValues[2][1] = format(zv);
		
		textValues[0][3] = format(az);
		textValues[1][3] = format(alt);
		textValues[2][3] = format(mag);
		
		setText(textValues);
	}
	

	/**
	 * Clear the current value; i.e. go back to a "no data" state.
	 */
	public void clearValues() {
		textValues[0][1] = "";
		textValues[1][1] = "";
		textValues[2][1] = "";
		
		textValues[0][3] = "";
		textValues[1][3] = "";
		textValues[2][3] = "";
		
		setText(textValues);
	}


	/**
	 * Format a float to a field width of 8, including sign, with 3
	 * decimals.  MUCH faster than String.format.
	 */
	private static final String format(float val) {
		int s = val < 0 ? -1 : 1;
		val *= s;
		int before = (int) val;
		int after = (int) ((val - before) * 1000);
		
		String b = (s < 0 ? "-" : " ") + before;
		String a = "" + after;
		StringBuilder res = new StringBuilder("    .000");
		int bs = 4 - b.length();
		res.replace((bs < 0 ? 0 : bs), 4, b);
		res.replace(8 - a.length(), 8, a);
		return res.toString();
	}

	
	// ******************************************************************** //
	// Private Data.
	// ******************************************************************** //

	// Values of all the text fields.
	private String[][] textValues;
	
}

