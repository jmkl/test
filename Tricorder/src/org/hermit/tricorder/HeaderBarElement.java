
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

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;


/**
 * An element which displays a textual heading in a fancy swooped header bar.
 */
class HeaderBarElement
	extends TextGauge
{

	// ******************************************************************** //
	// Constructor.
	// ******************************************************************** //
	
	/**
	 * Set up this view, and configure the text fields to be displayed in
	 * this element.  This is equivalent to calling setTextFields()
	 * after the basic constructor.
	 * 
	 * We support display of a single field, or a rectangular table
	 * of fields.  The fields are specified by passing in sample text
	 * values to be measured; we then allocate the space automatically.
	 * 
     * @param   parent          Parent surface.
	 * @param	fields		Strings representing the columns to display.
	 * 						Each one should be a sample piece of text
	 * 						which will be measured to determine the
	 * 						required space for each column.
	 */
	HeaderBarElement(SurfaceRunner parent, String[] fields) {
		super(parent, fields, 1);
		fieldBuffers = getBuffer();
		
		init();
	}

	
	/**
	 * Common initialization.
	 */
	private void init() {
		// Set the text black since we probably have a light background.
		setTextColor(0xff000000);
    	setTextSize(getBaseTextSize());

		sideBarWidth = getSidebarWidth();
		
		// Calculate the corner dimensions.
		swoopWidth = sideBarWidth * 3;
		swoopHeight = sideBarWidth * 2;
		
		// The height of the gutter under the header bar proper, which the sides
		// swoop down into.
		gutterHeight = sideBarWidth * 2;
		
		// Set margins around the text in the header bar so that (a) it
		// fits within the curves at the top left and right, and (b)
		// it fits above the gutter at the bottom.
		//
		// We have to do this here (in the constructor, not setGeometry())
		// because our parent will ask us our preferred height before
		// calling setGeometry().
		setMargins(swoopWidth, 0, swoopWidth, gutterHeight);
	}
	   
	
    // ******************************************************************** //
	// Geometry.
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
		headerBounds = bounds;

		final int l = bounds.left;
		final int r = bounds.right;
		final int t = bounds.top;
		final int b = bounds.bottom;

		barPath = new Path();
		barPath.moveTo(l, b);
		barPath.lineTo(l, t);
		barPath.lineTo(r, t);
		barPath.lineTo(r, b);
		barPath.lineTo(r - sideBarWidth, b);
		barPath.lineTo(r - sideBarWidth, b - gutterHeight);
		barPath.lineTo(l + sideBarWidth, b - gutterHeight);
		barPath.lineTo(l + sideBarWidth, b);
		barPath.close();
	}

	
    // ******************************************************************** //
	// Appearance.
	// ******************************************************************** //

	/**
	 * Set the background colour of this element.
	 * 
	 * @param	col			The new background colour, in ARGB format.
	 */
	void setBarColor(int col) {
		barColor = col;
	}
	

	/**
	 * Get the background colour of this element.
	 * 
	 * @return				The background colour, in ARGB format.
	 */
	int getBarColor() {
		return barColor;
	}
	

	/**
	 * Set the top indicator in the header bar of this element.
	 * 
	 * @param	show			Whether the indicator should be drawn.
	 * @param	colour			Colour for the indicator.
	 */
	protected void setTopIndicator(boolean show, int colour) {
		showIndicT = show;
		indicColourT = colour;
	}


	/**
	 * Set the bottom indicator in the header bar of this element.
	 * 
	 * @param	show			Whether the indicator should be drawn.
	 * @param	colour			Colour for the indicator.
	 */
	protected void setBotIndicator(boolean show, int colour) {
		showIndicB = show;
		indicColourB = colour;
	}


    /**
     * Display a text value in this view.
     * 
     * @param   row             Row of the field to change.
     * @param   col             Column of the field to change.
     * @param   text            The new text field value.
     */
    protected void setText(int row, int col, String text) {
        CharFormatter.formatString(fieldBuffers[row][col], 0, text, -1);
    }


    /**
     * Display a numeric value in the header.
     * 
     * @param   row             Row of the field to change.
     * @param   col             Column of the field to change.
     * @param   val             The value to display.
     */
    protected void setText(int row, int col, int val) {
        CharFormatter.formatInt(fieldBuffers[row][col], 0, val, -1, false);
    }


	// ******************************************************************** //
	// View Drawing.
	// ******************************************************************** //
	
	/**
	 * This method is called to ask the element to draw itself.
	 * 
	 * @param	canvas		Canvas to draw into.
	 * @param	paint		The Paint which was set up in initializePaint().
     * @param   now         Nominal system time in ms. of this update.
	 */
	@Override
	protected void drawBody(Canvas canvas, Paint paint, long now) {
		// Drawing the bar is easy -- just draw the path.
		paint.setColor(barColor);
		paint.setStyle(Paint.Style.FILL);
		canvas.drawPath(barPath, paint);
		
		// If the indicator is on, draw it in.
		if (showIndicT) {
			paint.setColor(indicColourT);
			canvas.drawCircle(headerBounds.left + sideBarWidth * 4 / 3,
							  headerBounds.top + sideBarWidth * 2,
							  sideBarWidth, paint);
		}
		if (showIndicB) {
			paint.setColor(indicColourB);
			canvas.drawCircle(headerBounds.left + sideBarWidth * 4 / 3,
							  headerBounds.top + sideBarWidth * 4,
							  sideBarWidth, paint);
		}

		// Call the base class to draw the text in.
		super.drawBody(canvas, paint, now);
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
	
	// Our position and bounds.
	private Rect headerBounds;
	
	// Width of the sidebar we swoop down to.
	private int sideBarWidth;
	
	// The swoop top corner dimensions.
	private int swoopWidth;
	private int swoopHeight;
	
	// The height of the gutter under the header bar proper, which the sides
	// swoop down into.
	private int gutterHeight;

	// Rectangles defining the bounds of the outer and inner curves
	// of the swoop at each end.
	private RectF outerCurveLeft;
	private RectF innerCurveLeft;
	private RectF outerCurveRight;
	private RectF innerCurveRight;

	// Path defining the shape of the bar.
	private Path barPath;
	
	// Background colour of the header bar.
	private int barColor = 0xff00ffff;
	
	// Indicator -- a shape which can be used to show status info.  We
	// have 2 indicators, top and bottom.  For each, we flag whether it
	// is shown, and store its colour.
	private boolean showIndicT = false;
	private int indicColourT = 0xffff0000;
	private boolean showIndicB = false;
	private int indicColourB = 0xffff0000;
    
    // Buffers where the values of the fields will be stored.
    private char[][][] fieldBuffers;

}

