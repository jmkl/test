
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
import org.hermit.android.instruments.Gauge;

import android.graphics.Canvas;
import android.graphics.Rect;


/**
 * A view which displays 3-axis sensor data in a numeric display.
 * 
 * This could be used, for example, to show the accelerometer or
 * compass values.
 */
class Num3DElement
	extends Gauge
{

	// ******************************************************************** //
	// Constructor.
	// ******************************************************************** //

	/**
	 * Set up this view.
	 * 
     * @param   parent      Parent surface.
	 * @param	gridCol		Colour for the framing elements.
	 * @param	plotCol		Colour for the data display.
     * @param   fields      Strings representing the columns to display.
     *                      Each one should be a sample piece of text
     *                      which will be measured to determine the
     *                      required space for each column.
	 */
	public Num3DElement(SurfaceRunner parent,
						int gridCol, int plotCol, String[] fields)
	{
		super(parent, gridCol, plotCol);
		
		// Create the header bar.
    	headerBar = new HeaderBarElement(parent, fields);
    	headerBar.setBarColor(gridCol);
    	
    	// Create the left-side bar.
    	rightBar = new Gauge(parent);
    	rightBar.setBackgroundColor(gridCol);
		
    	// Create the numeric display.
    	dataDisplay = new Num3DAtom(parent, gridCol, plotCol);
	}

	   
    // ******************************************************************** //
	// Geometry Management.
	// ******************************************************************** //

    /**
     * This is called during layout when the size of this element has
     * changed.  This is where we first discover our size, so set
     * our geometry to match.
     * 
	 * @param	bounds		The bounding rect of this element within
	 * 						its parent View.
     */
	@Override
	public void setGeometry(Rect bounds) {
		super.setGeometry(bounds);
		
		int y = bounds.top;
		
		// First position the header bar.
		int headHeight = headerBar.getPreferredHeight();
		headerBar.setGeometry(new Rect(bounds.left, y,
									   bounds.right, y + headHeight));
		y += headHeight + getInnerGap();

		int bar = getSidebarWidth();
		int ex = bounds.right - bar - getInterPadding();
		int dataHeight = dataDisplay.getPreferredHeight();
		dataDisplay.setGeometry(new Rect(bounds.left, y, ex, y + dataHeight));
		
		rightBar.setGeometry(new Rect(bounds.right - bar, y,
									  bounds.right, y + dataHeight));
	}

	
	/**
	 * Get the minimum width needed to fit all the text.
	 * 
	 * @return			The minimum width needed to fit all the text.
	 * 					Returns zero if setTextFields() hasn't been called.
	 */
	@Override
	public int getPreferredWidth() {
		return getSidebarWidth() + getInterPadding() +
											dataDisplay.getPreferredWidth();
	}
	

	/**
	 * Get the minimum height needed to fit all the text.
	 * 
	 * @return			The minimum height needed to fit all the text.
	 * 					Returns zero if setTextFields() hasn't been called.
	 */
	@Override
	public int getPreferredHeight() {
		return headerBar.getPreferredHeight() + getInnerGap() +
											dataDisplay.getPreferredHeight();
	}
	

    // ******************************************************************** //
	// Appearance.
	// ******************************************************************** //

	/**
	 * Set the background colour of this element.
	 * 
     * @param	grid			Colour for drawing a data scale / grid.
     * @param	plot			Colour for drawing data plots.
	 */
	@Override
	public void setDataColors(int grid, int plot) {
		headerBar.setBarColor(grid);
    	rightBar.setBackgroundColor(grid);
		dataDisplay.setDataColors(grid, plot);
	}
	

	// ******************************************************************** //
	// Data Management.
	// ******************************************************************** //

	/**
	 * Set an indicator in the header bar of this element.
	 * 
	 * @param	show			Whether the indicator should be drawn.
	 * @param	colour			Colour for the indicator.
	 */
	protected void setIndicator(boolean show, int colour) {
    	headerBar.setTopIndicator(show, colour);
	}


	/**
	 * Set a specific text value displayed in this view.
	 * 
	 * @param	row				Row of the field to change.
	 * @param	col				Column of the field to change.
	 * @param	text			The new text field value.
	 */
	protected void setText(int row, int col, String text) {
		headerBar.setText(row, col, text);
	}


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
		dataDisplay.setValues(values, mag, az, alt);
	}


	/**
	 * Clear the current value; i.e. go back to a "no data" state.
	 */
	public void clearValues() {
		dataDisplay.clearValues();
	}


	// ******************************************************************** //
	// View Drawing.
	// ******************************************************************** //
	
	/**
	 * This method is called to ask the element to draw itself.
	 * 
	 * @param	canvas		Canvas to draw into.
	 * @param	now			Current system time in ms.
     * @param   bg          Iff true, tell the gauge to draw its background
     *                      first.
	 */
	@Override
	public void draw(Canvas canvas, long now, boolean bg) {
		super.draw(canvas, now, bg);
		
		headerBar.draw(canvas, now, bg);
		rightBar.draw(canvas, now, bg);
		dataDisplay.draw(canvas, now, bg);
	}
	

	// ******************************************************************** //
	// Private Data.
	// ******************************************************************** //

	// The header bar for the display.
	private HeaderBarElement headerBar;

	// The left-side bar (just a solid colour bar).
	private Gauge rightBar;
	
	// Numeric data display.
	private Num3DAtom dataDisplay;
	
}

