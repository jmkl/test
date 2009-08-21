
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

import android.graphics.Canvas;
import android.graphics.Rect;
import android.view.SurfaceHolder;


/**
 * A view which displays 3-axis sensor data in a numeric display.
 * 
 * This could be used, for example, to show the accelerometer or
 * compass values.
 */
class Num3DElement
	extends Element
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
	public Num3DElement(Tricorder context, SurfaceHolder sh,
						int gridCol, int plotCol)
	{
		super(context, sh, gridCol, plotCol);
		
		// Create the header bar.
    	headerBar = new HeaderBarElement(context, sh,
    								     new String[] { "" }, 1);
    	headerBar.setBarColor(gridCol);
    	
    	// Create the left-side bar.
    	rightBar = new Element(context, sh);
    	rightBar.setBackgroundColor(gridCol);
		
    	// Create the numeric display.
    	dataDisplay = new Num3DAtom(context, sh, gridCol, plotCol);
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
	protected void setGeometry(Rect bounds) {
		super.setGeometry(bounds);
		
		int y = bounds.top;
		
		// First position the header bar.
		int headHeight = headerBar.getPreferredHeight();
		headerBar.setGeometry(new Rect(bounds.left, y,
									   bounds.right, y + headHeight));
		y += headHeight + appContext.getInnerGap();

		int bar = appContext.getSidebarWidth();
		int ex = bounds.right - bar - INT_PADDING;
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
	int getPreferredWidth() {
		return getContext().getSidebarWidth() + INT_PADDING +
											dataDisplay.getPreferredWidth();
	}
	

	/**
	 * Get the minimum height needed to fit all the text.
	 * 
	 * @return			The minimum height needed to fit all the text.
	 * 					Returns zero if setTextFields() hasn't been called.
	 */
	@Override
	int getPreferredHeight() {
		return headerBar.getPreferredHeight() + getContext().getInnerGap() +
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
	void setDataColors(int grid, int plot) {
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
	 * @param	now				Current system time in ms.
	 */
	@Override
	protected void draw(Canvas canvas, long now) {
		super.draw(canvas, now);
		
		headerBar.draw(canvas, now);
		rightBar.draw(canvas, now);
		dataDisplay.draw(canvas, now);
	}
	

    // ******************************************************************** //
    // Class Data.
    // ******************************************************************** //
	
	// Padding between separate sections.
	private static final int INT_PADDING = 8;

	
	// ******************************************************************** //
	// Private Data.
	// ******************************************************************** //

	// The header bar for the display.
	private HeaderBarElement headerBar;

	// The left-side bar (just a solid colour bar).
	private Element rightBar;
	
	// Numeric data display.
	private Num3DAtom dataDisplay;
	
}

