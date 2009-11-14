
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

import org.hermit.android.core.SurfaceRunner;
import org.hermit.android.instruments.TextGauge;
import org.hermit.utils.CharFormatter;


/**
 * An atom which displays 3-axis sensor data in a numeric display.
 * 
 * This could be used, for example, to show the accelerometer or
 * compass values.
 */
class Num3DAtom
	extends TextGauge
{

	// ******************************************************************** //
	// Constructor.
	// ******************************************************************** //

	/**
	 * Set up this view.
	 * 
     * @param   parent          Parent surface.
	 * @param	gridCol			Colour for the framing elements.
	 * @param	plotCol			Colour for the data display.
	 */
	public Num3DAtom(SurfaceRunner parent, int gridCol, int plotCol) {
		super(parent);
		
		setTextColor(plotCol);
		
		// Define the display format.
		final String[] fields = {
		        parent.getRes(R.string.lab_x), "88888888",
		        parent.getRes(R.string.lab_mag), "88888888",
		};
		setTextFields(fields, 3);
		
		fieldBuffers = getBuffer();
		CharFormatter.formatString(fieldBuffers[0][0], 0, parent.getRes(R.string.lab_x), 1);
        CharFormatter.formatString(fieldBuffers[1][0], 0, parent.getRes(R.string.lab_y), 1);
        CharFormatter.formatString(fieldBuffers[2][0], 0, parent.getRes(R.string.lab_z), 1);
        CharFormatter.formatString(fieldBuffers[0][2], 0, parent.getRes(R.string.lab_azi), 3);
        CharFormatter.formatString(fieldBuffers[1][2], 0, parent.getRes(R.string.lab_alt), 3);
        CharFormatter.formatString(fieldBuffers[2][2], 0, parent.getRes(R.string.lab_mag), 3);
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
		
		CharFormatter.formatFloat(fieldBuffers[0][1], 0, xv, -1, 3);
		CharFormatter.formatFloat(fieldBuffers[1][1], 0, yv, -1, 3);
		CharFormatter.formatFloat(fieldBuffers[2][1], 0, zv, -1, 3);

		CharFormatter.formatFloat(fieldBuffers[0][3], 0, az, -1, 3);
		CharFormatter.formatFloat(fieldBuffers[1][3], 0, alt, -1, 3);
		CharFormatter.formatFloat(fieldBuffers[2][3], 0, mag, -1, 3);
	}


	/**
	 * Clear the current value; i.e. go back to a "no data" state.
	 */
	public void clearValues() {
	    CharFormatter.blank(fieldBuffers[0][1], 0, -1);
        CharFormatter.blank(fieldBuffers[1][1], 0, -1);
        CharFormatter.blank(fieldBuffers[2][1], 0, -1);
        CharFormatter.blank(fieldBuffers[0][3], 0, -1);
        CharFormatter.blank(fieldBuffers[1][3], 0, -1);
        CharFormatter.blank(fieldBuffers[2][3], 0, -1);
	}


    // ******************************************************************** //
    // Class Data.
    // ******************************************************************** //

    // Debugging tag.
    @SuppressWarnings("unused")
    private static final String TAG = "tricorder";
    
    
	// ******************************************************************** //
	// Private Data.
	// ******************************************************************** //

    // Text field buffers for the data display.
    private char[][][] fieldBuffers;
	
}

