
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
 * A view which displays a magnitude as a filled horizontal bar.
 * 
 * This could be used, for example, to show the light level or
 * absolute magnetic field strength value.
 */
class BargraphAtom
	extends Gauge
{

	// ******************************************************************** //
	// Public Types.
	// ******************************************************************** //

	/**
	 * Which side the gauge is anchored to.  It grows towards the opposite
	 * direction.
	 */
	enum Orientation {
		LEFT,
		TOP,
		RIGHT,
		BOTTOM;
	}
	
	
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
	 * @param	orient		    The orientation for this gauge.
	 */
	public BargraphAtom(SurfaceRunner parent,
							float unit, float range,
							int gridCol, int plotCol,
							Orientation orient)
	{
		super(parent, gridCol, plotCol);
		
		unitSize = unit;
		plotRange = range;
		orientation = orient;
		vertical = orient == Orientation.TOP || orient == Orientation.BOTTOM;
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
		
		plotWidth = bounds.right - bounds.left;
		plotHeight = bounds.bottom - bounds.top;

		// Calculate the scaling factors.
		unitScale = (vertical ? plotHeight : plotWidth) / plotRange;
		dataScale = unitScale / unitSize;

		// Calculate the size and position of the strength bar within the plot.
		barBaseX = bounds.left;
		barBaseY = bounds.top;
		barEndX = bounds.right;
		barEndY = bounds.bottom;
		int margin = BAR_MARGIN;
		
		switch (orientation) {
		case LEFT:
		case RIGHT:
		    if (barEndY - barBaseY < 10)
		        --margin;
			barBaseY += margin;
			barEndY -= margin;
			break;
		case TOP:
		case BOTTOM:
            if (barEndX - barBaseX < 10)
                --margin;
			barBaseX += margin;
			barEndX -= margin;
			break;
		}
	}

	
	/**
	 * Get the minimum preferred width for this atom.
	 * 
	 * @return			The minimum preferred width for this atom.
	 * 					Returns zero if we don't know yet.
	 */
	@Override
	public int getPreferredWidth() {
		return DEF_PLOT_WIDTH;
	}
	
	
	/**
	 * Get the minimum preferred height for this atom.
	 * 
	 * @return			The minimum preferred height for this atom.
	 * 					Returns zero if we don't know yet.
	 */
	@Override
	public int getPreferredHeight() {
		return DEF_PLOT_WIDTH;
	}
	

	// ******************************************************************** //
	// Data Management.
	// ******************************************************************** //

	/**
	 * Set the given value as the new value for the displayed data.
	 * Update the display accordingly.
	 * 
	 * @param	value				The new value.
	 */
	public void setValue(float value) {
		// Save the values.  Note we're synced at the View level.
		currentValue = value;
		haveValue = true;
	}


	/**
	 * Clear the current value; i.e. go back to a "no data" state.
	 */
	public void clearValue() {
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
		// Get the value and scale it.
		float sval = currentValue * dataScale;
		boolean valid = haveValue;

		// Background color.
		Rect bounds = getBounds();

		// Draw the outlines.
		paint.setColor(getGridColor());
		paint.setStyle(Paint.Style.STROKE);
		paint.setStrokeWidth(SCALE_WIDTH);
        switch (orientation) {
        case LEFT:
            canvas.drawLine(bounds.left, bounds.top,
                            bounds.right, bounds.top, paint);
            canvas.drawLine(bounds.left, bounds.bottom - 1,
                            bounds.right, bounds.bottom - 1, paint);
            canvas.drawLine(bounds.left, bounds.top,
                            bounds.left, bounds.bottom, paint);
            break;
        case TOP:
            canvas.drawLine(bounds.left, bounds.top,
                            bounds.left, bounds.bottom, paint);
            canvas.drawLine(bounds.right - 1, bounds.top,
                            bounds.right - 1, bounds.bottom, paint);
            canvas.drawLine(bounds.left, bounds.top,
                            bounds.right, bounds.top, paint);
            break;
        case RIGHT:
            canvas.drawLine(bounds.left, bounds.top,
                            bounds.right, bounds.top, paint);
            canvas.drawLine(bounds.left, bounds.bottom - 1,
                            bounds.right, bounds.bottom - 1, paint);
            canvas.drawLine(bounds.right, bounds.top,
                            bounds.right, bounds.bottom, paint);
            break;
        case BOTTOM:
            canvas.drawLine(bounds.left, bounds.top,
                            bounds.left, bounds.bottom, paint);
            canvas.drawLine(bounds.right - 1, bounds.top,
                            bounds.right - 1, bounds.bottom, paint);
            canvas.drawLine(bounds.left, bounds.bottom - 1,
                            bounds.right, bounds.bottom - 1, paint);
            break;
        }

        // Draw the scale lines, if the bar is big enough.
        if (unitScale >= 3) {
            for (float off = unitScale; off < plotRange * unitScale; off += unitScale) {
                switch (orientation) {
                case LEFT:
                    canvas.drawLine(bounds.left + off, bounds.top,
                            bounds.left + off, bounds.bottom, paint);
                    break;
                case TOP:
                    canvas.drawLine(bounds.left, bounds.top + off,
                            bounds.right, bounds.top + off, paint);
                    break;
                case RIGHT:
                    canvas.drawLine(bounds.right - off, bounds.top,
                            bounds.right - off, bounds.bottom, paint);
                    break;
                case BOTTOM:
                    canvas.drawLine(bounds.left, bounds.bottom - off - 1,
                            bounds.right, bounds.bottom - off - 1, paint);
                    break;
                }
            }
        }

		// If we have valid data, draw the data value as a bar.
		// REMEMBER THE SCREEN Y-AXIS is NEGATIVE UP.
		if (valid) {
			int baseX = barBaseX;
			int baseY = barBaseY;
			int endX = barEndX;
			int endY = barEndY;
			switch (orientation) {
			case LEFT:
				endX = barBaseX + (int) sval;
				break;
			case TOP:
				endY = barBaseY + (int) sval;
				break;
			case RIGHT:
				baseX = barEndX - (int) sval;
				break;
			case BOTTOM:
				baseY = barEndY - (int) sval;
				break;
			}
			
			// Draw the bar.
			paint.setColor(getPlotColor());
			paint.setStyle(Paint.Style.FILL);
			canvas.drawRect(baseX, baseY, endX, endY, paint);
		}
	}
	

    // ******************************************************************** //
    // Class Data.
    // ******************************************************************** //

    // Debugging tag.
	@SuppressWarnings("unused")
	private static final String TAG = "tricorder";

	// Widths of the scale lines.
	private static final int SCALE_WIDTH = 1;

	// Margin either side of the bar within the plot.
	private static final int BAR_MARGIN = 3;

	// The default width of the overall plot.  The bar is centered in this.
	private static final int DEF_PLOT_WIDTH = 10;

	
	// ******************************************************************** //
	// Private Data.
	// ******************************************************************** //

	// The size of a unit of measure (for example, 1g of acceleration).
	private float unitSize = 1.0f;
	
	// How many units big to make the graph.
	private float plotRange = 1.0f;

	// The orientation of this gauge.
	private Orientation orientation;
	
	// Convenience flag: if true, this is a vertical gauge;
	// otherwise horizontal.
	private boolean vertical;

	// The scaling factor for the data, based on unitSize, plotRange and
	// barLen.  This scales a data value to screen co-ordinates.
	private float dataScale;
	
	// THe screen size of one unit of the measured value.
	private float unitScale;

	// The current value; flag to say whether we actually have data.
	private float currentValue;
	private boolean haveValue = false;
	
	// The width and height of the magnitude data bar.
	private int plotWidth;
	private int plotHeight;

	// The overall bounds of the bar within the plot.
	private int barBaseX;
	private int barBaseY;
	private int barEndX;
	private int barEndY;

}

