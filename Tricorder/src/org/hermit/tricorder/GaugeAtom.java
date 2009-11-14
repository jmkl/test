
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
import android.graphics.Path;
import android.graphics.Rect;


/**
 * A view which displays a magnitude as a horizontal or vertical bar.
 * 
 * This could be used, for example, to show the light level or
 * absolute magnetic field strength value.
 */
class GaugeAtom
	extends Gauge
{

	// ******************************************************************** //
	// Public Types.
	// ******************************************************************** //

	/**
	 * Which way the gauge is drawn relative to the body it's attached
	 * to.  Hence LEFT means a vertical gauge with the pointer pointing
	 * to the right.
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
	 * @param	orient   		The orientation for this gauge.
	 * @param	centered		If true, the zero value is in the center;
	 * 							else at the left or bottom.
	 */
	public GaugeAtom(SurfaceRunner parent, float unit, float range,
					 int gridCol, int plotCol,
					 Orientation orient, boolean centered)
	{
		this(parent, 1, unit, range, gridCol, new int[] { plotCol },
															orient, centered);
	}


	/**
	 * Set up this view.
	 * 
     * @param   parent          Parent surface.
	 * @param	num				The number of values plotted on this gauge.
	 * @param	unit			The size of a unit of measure (for example,
	 * 							1g of acceleration).
	 * @param	range			How many units big to make the graph.
	 * @param	gridCol			Colour for the graph grid.
	 * @param	plotCols		Colours for the graph plots.
	 * @param	orient   		The orientation for this gauge.
	 * @param	centered		If true, the zero value is in the center;
	 * 							else at the left or bottom.
	 */
	public GaugeAtom(SurfaceRunner parent,
					 int num, float unit, float range,
					 int gridCol, int[] plotCols,
					 Orientation orient, boolean centered)
	{
		super(parent, gridCol, plotCols[0]);
		
		numPlots = num;
		unitSize = unit;
		plotRange = range;
		plotColours = plotCols;
		orientation = orient;
		vertical = orient == Orientation.LEFT || orient == Orientation.RIGHT;
		datumCenter = centered;
		
		barThickness = getSidebarWidth();
		currentValue = new float[numPlots];
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
		
		barRect = new Rect(bounds);
		
		switch (orientation) {
		case LEFT:
			barHeight = bounds.bottom - bounds.top;
			barWidth = barThickness;
			barRect.right = barRect.left + barThickness;
			barLength = barHeight;
			break;
		case TOP:
			barHeight = barThickness;
			barWidth = bounds.right - bounds.left;
			barRect.bottom = barRect.top + barThickness;
			barLength = barWidth;
			break;
		case RIGHT:
			barHeight = bounds.bottom - bounds.top;
			barWidth = barThickness;
			barRect.left = barRect.right - barThickness;
			barLength = barHeight;
			break;
		case BOTTOM:
			barHeight = barThickness;
			barWidth = bounds.right - bounds.left;
			barRect.top = barRect.bottom - barThickness;
			barLength = barWidth;
			break;
		}
		
		// Work out the datum point and unit size.
		datumPoint = datumCenter ? barLength / 2 : vertical ? barLength - 1 : 0;
		unitScale = datumCenter ?
							barLength / 2 / plotRange : barLength / plotRange;

		// Calculate the scaling factors.
		dataScale = unitScale / unitSize;
		
		// Create a path that defines the pointer.
		pointerPath = new Path();
		int p = barThickness * 2;
		if (orientation == Orientation.RIGHT || orientation == Orientation.BOTTOM)
			p = -p;
		int s = barThickness * 2 / 3;
		int b = barThickness / 2;
		if (vertical) {
			pointerPath.moveTo(b, -s);
			pointerPath.lineTo(b + p, 0);
			pointerPath.lineTo(b, s);
		} else {
			pointerPath.moveTo(-s, b);
			pointerPath.lineTo(0, b + p);
			pointerPath.lineTo(s, b);
		}
		pointerPath.close();
	}

	
	/**
	 * Get the minimum preferred width for this atom.
	 * 
	 * @return			The minimum preferred width for this atom.
	 * 					Returns zero if we don't know yet.
	 */
	@Override
	public int getPreferredWidth() {
		return vertical ? (int) (barThickness * 2.5f + 1f) : 0;
	}
	
	
	/**
	 * Get the minimum preferred height for this atom.
	 * 
	 * @return			The minimum preferred height for this atom.
	 * 					Returns zero if we don't know yet.
	 */
	@Override
	public int getPreferredHeight() {
		return vertical ? 0 : (int) (barThickness * 2.5f + 1f);
	}
	

    // ******************************************************************** //
	// Appearance.
	// ******************************************************************** //

	/**
	 * Set the colours of this element.
	 * 
     * @param	grid			Colour for drawing a data scale / grid.
     * @param	plot			Colour for drawing data plots.
	 */
	@Override
	public void setDataColors(int grid, int plot) {
		setDataColors(grid, new int[] { plot });
	}
	

	/**
	 * Set the colours of this element.
	 * 
     * @param	grid			Colour for drawing a data scale / grid.
     * @param	plot			Colours for drawing data plots.
	 */
	void setDataColors(int grid, int[] plot) {
		super.setDataColors(grid, plot[0]);
		plotColours = plot;
	}
	

	// ******************************************************************** //
	// Data Management.
	// ******************************************************************** //

	/**
	 * Set the data range of this gauge.
	 * 
     * @param   unit            The size of a unit of measure (for example,
     *                          1g of acceleration).
	 * @param	range			How many units big to make the gauge.
	 */
	public void setDataRange(float unit, float range) {
        unitSize = unit;
		plotRange = range;
		
		// Calculate the scaling factors.
		unitScale = datumCenter ?
				barLength / 2 / plotRange : barLength / plotRange;
		dataScale = unitScale / unitSize;
	}


	/**
	 * Set the given value as the new value for the displayed data.
	 * Update the display accordingly.
	 * 
	 * @param	value				The new value.
	 */
	public void setValue(float value) {
		currentValue[0] = value;
		haveValue = true;
	}


	/**
	 * Set the given values as the new values for the displayed data.
	 * Update the display accordingly.
	 * 
	 * @param	values				The new values.
	 */
	public void setValue(float[] values) {
		for (int p = 0; p < numPlots; ++p)
			currentValue[p] = values[p];
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
		// Draw the bar.
		paint.setColor(getGridColor());
		paint.setStyle(Paint.Style.FILL);
		canvas.drawRect(barRect, paint);

		// If the graph is too small, stop now.
		if (unitScale < 1)
			return;
		
		// Background color.
		int bgColour = getBackgroundColor();

		// Draw scale lines.
		paint.setColor(bgColour);
		paint.setStyle(Paint.Style.STROKE);
		paint.setStrokeWidth(SCALE_WIDTH);
		if (vertical) {
			for (int y = 0; y < plotRange * unitScale; y += unitScale) {
				if (datumCenter || y > 0)
					canvas.drawLine(barRect.left, barRect.top + datumPoint - y,
									barRect.right, barRect.top + datumPoint - y,
									paint);
				if (datumCenter && y > 0)
					canvas.drawLine(barRect.left, barRect.top + datumPoint + y,
									barRect.right, barRect.top + datumPoint + y,
									paint);
			}
		} else {
			for (int x = 0; x < plotRange * unitScale; x += unitScale) {
				if (datumCenter || x > 0)
					canvas.drawLine(barRect.left + datumPoint + x, barRect.top,
							    	barRect.left + datumPoint + x, barRect.bottom,
							    	paint);
				if (datumCenter && x > 0)
					canvas.drawLine(barRect.left + datumPoint - x, barRect.top,
									barRect.left + datumPoint - x, barRect.bottom,
									paint);
			}
		}

		// If we have valid data, draw each of the plotted values as a pointer.
		// REMEMBER THE SCREEN Y-AXIS is NEGATIVE UP.
		if (haveValue) {
			for (int p = 0; p < numPlots; ++p) {
				// Get the value and scale it.  We're synced at the View level.
				final float sval = currentValue[p] * dataScale;

				float xoff = 0f;
				float yoff = 0f;
				switch (orientation) {
				case LEFT:
				case RIGHT:
					xoff = barRect.left;
					yoff = barRect.top + datumPoint - sval;
					break;
				case TOP:
				case BOTTOM:
					xoff = barRect.left + datumPoint + sval;
					yoff = barRect.top;
					break;
				}

				// Adjust the path that defines the pointer.
				pointerPath.offset(xoff, yoff);
				paint.setAntiAlias(true);

				// Draw the pointer, with a contrasting outline.
				paint.setColor(bgColour);
				paint.setStyle(Paint.Style.STROKE);
				paint.setStrokeWidth(1.5f);
				canvas.drawPath(pointerPath, paint);
				paint.setColor(plotColours[p]);
				paint.setStyle(Paint.Style.FILL);
				canvas.drawPath(pointerPath, paint);

				pointerPath.offset(-xoff, -yoff);
				paint.setAntiAlias(false);
			}
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

	
	// ******************************************************************** //
	// Private Data.
	// ******************************************************************** //

	// The number of values plotted on this gauge.
	private int numPlots;

	// Width of the vertical bar within the strength plot.
	private final int barThickness;

	// The size of a unit of measure (for example, 1g of acceleration).
	private float unitSize = 1.0f;
	
	// How many units big to make the graph.
	private float plotRange = 1.0f;

	// Colours to plot the data lines in, one colour per line.
	private int[] plotColours;

	// The orientation of this gauge.
	private Orientation orientation;
	
	// Convenience flag: if true, this is a vertical gauge;
	// otherwise horizontal.
	private boolean vertical;

	// If true, the zero value is in the center; else at the left or bottom.
	private boolean datumCenter;

	// A path that defines the pointer.
	private Path pointerPath;

	// The scaling factor for the data, based on unitSize, plotRange and
	// barLen.  This scales a data value to screen co-ordinates.
	private float dataScale;
	
	// THe screen size of one unit of the measured value.
	private float unitScale;

	// The current value; flag to say whether we actually have data.
	private float[] currentValue;
	private boolean haveValue = false;
	
	// The width and height of the magnitude data bar.
	private int barWidth;
	private int barHeight;
	
	// Rectangle that defines the outline of the bar itself.
	private Rect barRect;
	
	// The length of the bar.
	private int barLength;

	// The datum point for the bar -- the offset along the length of the bar
	// which represents zero.
	private int datumPoint;

}

