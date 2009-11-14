
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
import android.graphics.Paint;
import android.graphics.Rect;


/**
 * A view which displays sensor data in a 2-axis display.
 * 
 * This could be used, for example, to show the accelerometer or
 * compass values.
 */
class Axis2DAtom
	extends Gauge
{

	// ******************************************************************** //
	// Constructor.
	// ******************************************************************** //

	/**
	 * Set up this view.
	 * 
     * @param   parent          Parent surface.
	 * @param	unit			The size of a unit of measure (for example,
	 * 							1g of acceleration).
	 * @param	range			How many units big to make the graph.
	 * @param	gridCol			Colour for the graph grid.
	 * @param	plotCol			Colour for the graph plot.
	 */
	public Axis2DAtom(SurfaceRunner parent,
					  float unit, float range,
	   				  int gridCol, int plotCol)
	{
		super(parent, gridCol, plotCol);
		
		unitSize = unit;
		plotRange = range;
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

		barLen = (bounds.bottom - bounds.top) / 2;

		// Calculate the scaling factors.
		unitScale = barLen / plotRange;
		dataScale = unitScale / unitSize;
		
		// Figure out the position of the X-Y plot.
		crossX = bounds.left + barLen;
		crossY = bounds.top + barLen;
	}


	// ******************************************************************** //
	// Data Management.
	// ******************************************************************** //

	/**
	 * Set the data range of this gauge.
	 * 
     * @param   unit        The size of a unit of measure (for example,
     *                      1g of acceleration).
	 * @param	range		How many units big to make the gauge.
	 */
	public void setDataRange(float unit, float range) {
        unitSize = unit;
		plotRange = range;

		// Calculate the scaling factors.
		unitScale = barLen / plotRange;
		dataScale = unitScale / unitSize;
	}


	/**
	 * Set the given values as the new values for the displayed data.
	 * Update the display accordingly.
	 * 
	 * @param	values				The new X, Y and Z values.
	 * @param	mag					Their absolute magnitude value.
	 */
	public void setValues(float[] values, float mag) {
		for (int i = 0; i < 3; ++i)
			currentValues[i] = Float.isInfinite(values[i]) ? unitSize : values[i];
		haveValue = true;
	}


	/**
	 * Clear the current value; i.e. go back to a "no data" state.
	 */
	public void clearValues() {
		haveValue = false;
	}


	// ******************************************************************** //
	// View Drawing.
	// ******************************************************************** //

	/**
	 * Do the subclass-specific parts of drawing for this element.
	 * 
	 * Subclasses should override this to do their drawing.
	 * 
	 * @param	canvas		Canvas to draw into.
	 * @param	paint		The Paint which was set up in initializePaint().
     * @param   now         Nominal system time in ms. of this update.
	 */
	@Override
	protected void drawBody(Canvas canvas, Paint paint, long now) {		
		// Get the values and scale them.  Note we're synced at the
		// TridataView level.
		float x = currentValues[0] * dataScale;
		float y = currentValues[1] * dataScale;

		paint.setColor(getGridColor());
		paint.setStyle(Paint.Style.STROKE);
		paint.setStrokeWidth(SCALE_WIDTH);

		// If the graph is too small, stop now.
		if (unitScale < 1)
			return;

		// Draw scale lines on the 2-axis plot.
		canvas.drawRect(crossX - unitScale, crossY - unitScale,
					    crossX + unitScale, crossY + unitScale, paint);

		// Draw our axes, second so they're on top.
		paint.setStrokeWidth(AXIS_WIDTH);
		canvas.drawLine(crossX - barLen, crossY,
						crossX + barLen, crossY, paint);
		canvas.drawLine(crossX, crossY - barLen,
						crossX, crossY + barLen, paint);

		// Draw the data values if we have them.
		// REMEMBER THE SCREEN Y-AXIS is NEGATIVE UP.
		if (haveValue) {
			paint.setColor(getPlotColor());
			paint.setStyle(Paint.Style.FILL_AND_STROKE);
			paint.setStrokeWidth(DATA_WIDTH);
			final float l = Math.min(crossX, crossX + x);
            final float r = Math.max(crossX, crossX + x);
            final float t = Math.min(crossY, crossY - y);
            final float b = Math.max(crossY, crossY - y);
			canvas.drawRect(l, t, r, b, paint);
			canvas.drawLine(crossX + x - 6, crossY - y,
					        crossX + x + 6, crossY - y, paint);
			canvas.drawLine(crossX + x, crossY - y - 6,
					        crossX + x, crossY - y + 6, paint);
		}
	}

	
    // ******************************************************************** //
    // Class Data.
    // ******************************************************************** //

    // Debugging tag.
	@SuppressWarnings("unused")
	private static final String TAG = "tricorder";

	// Widths of the axes.
	private static final int AXIS_WIDTH = 2;

	// Widths of the scale lines.
	private static final int SCALE_WIDTH = 1;

	// Widths of the data plot lines.
	private static final int DATA_WIDTH = 2;

	
	// ******************************************************************** //
	// Private Data.
	// ******************************************************************** //

	// The size of a unit of measure (for example, 1g of acceleration).
	private float unitSize = 1.0f;
	
	// How many units big to make the graph.
	private float plotRange = 1.0f;

	// The scaling factor for the data, based on unitSize, plotRange and
	// barLen.  This scales a data value to screen co-ordinates.
	private float dataScale;
	
	// THe screen size of one unit of the measured value.
	private float unitScale;

	// The current X, Y and Z values, and their absolute magnitude.
	private float currentValues[] = new float[3];
	private boolean haveValue;

	// The length of the data bars extending out from the origin of each
	// display -- ie. half the total bar length.
	private int barLen;

	// X,Y position of the centre of the cross display.
	private int crossX;
	private int crossY;
	
}

