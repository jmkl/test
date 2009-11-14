
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

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;


/**
 * A view which displays a magnitude as a tracking graph.
 * 
 * This could be used, for example, to show the light level or
 * absolute magnetic field strength value.
 */
class ChartAtom
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
	public ChartAtom(SurfaceRunner parent,
							float unit, float range,
							int gridCol, int plotCol)
	{
		this(parent, 1, unit, range, gridCol, new int[] { plotCol }, false);
	}


	/**
	 * Set up this view.
	 * 
     * @param   parent          Parent surface.
	 * @param	num				The number of values plotted on this graph.
	 * @param	unit			The size of a unit of measure (for example,
	 * 							1g of acceleration).
	 * @param	range			How many units big to make the graph.
	 * @param	gridCol			Colour for the graph grid.
	 * @param	plotCols		Colours for the graph plots.
	 * @param	centered		If true, the zero value is in the centre;
	 * 							else at the left or bottom.
	 */
	public ChartAtom(SurfaceRunner parent,
							int num, float unit, float range,
							int gridCol, int[] plotCols, boolean centered)
	{
		super(parent, gridCol, plotCols[0]);
		
		numPlots = num;
		unitSize = unit;
		plotRange = range;
		plotColours = plotCols;
		datumCenter = centered;
		
		scrollingChart = true;
		
		// Reset the stored data buffer.  It gets set up in setGeometry().
		currentValue = null;
		dataLength = 0;
		dataBuffer = null;
		dataCursor = 0;
		dataCount = 0;
		chartCursor = 0;
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
		
		// Position of the chart on screen.
		screenX = bounds.left;
		screenY = bounds.top;
		
		chartWidth = bounds.right - bounds.left;
		chartHeight = bounds.bottom - bounds.top;

		// Set the zero point.
		datumPoint = datumCenter ? chartHeight / 2 : chartHeight - 1;

		// Calculate the scaling factors.
		unitScale = datumCenter ?
				chartHeight / 2 / plotRange : chartHeight / plotRange;
		dataScale = unitScale / unitSize;
		
		// Create a Bitmap to draw the chart into.
		chartBitmap = Bitmap.createBitmap(chartWidth, chartHeight,
										  Bitmap.Config.RGB_565);
		chartCanvas = new Canvas(chartBitmap);
		
		// Set up the rects for drawing the plot to the screen.  We
		// draw the bitmap in two bits to make it scroll.
		drawSource1 = new Rect(0, 0, chartWidth, chartHeight);
		drawDest1 = new Rect(screenX, screenY, 0, screenY + chartHeight);
		drawSource2 = new Rect(0, 0, 0, chartHeight);
		drawDest2 = new Rect(0, screenY,
							 screenX + chartWidth, screenY + chartHeight);
		
		// If this is a fresh start, create a data buffer to store the
		// chart's plotted values.  However, we may just be coming back after
		// a resume, in which case keep the existing data and re-plot it.
		setupBuffer((int) Math.ceil(chartWidth / timeScale));
	}


	private void setupBuffer(int dlen) {
		boolean haveData = dataCount > 0;
		
		if (dataBuffer == null || dataLength == 0) {
			dataLength = dlen;
			dataBuffer = new float[numPlots][dataLength];
			currentValue = new float[numPlots];
			dataCursor = 0;
			dataCount = 0;
			chartCursor = 0;
		} else if (dataLength != dlen) {
			float[][] oldBuf = dataBuffer;
			int oldLen = dataLength;
			int oldCount = dataCount;
			int oldCursor = dataCursor;
			
			dataLength = dlen;
			dataBuffer = new float[numPlots][dataLength];
			currentValue = new float[numPlots];
			dataCursor = 0;
			dataCount = 0;
			chartCursor = 0;
			
			// Copy the most recent data into the new buffer.
			int copyCount = Math.min(oldCount, dataLength);
			int from = (oldCursor - copyCount + oldLen) % oldLen;
			for (int i = 0; i < copyCount; ++i) {
				for (int p = 0; p < numPlots; ++p)
					dataBuffer[p][dataCursor] = oldBuf[p][from];
				from = (from + 1) % oldLen;
				dataCursor = (dataCursor + 1) % dataLength;
				if (dataCount < dataLength)
					++dataCount;
			}
		}
		
		if (haveData)
			redrawGraph();
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
	 * Get the number of data items displayed.
	 */
	int getDataLength() {
		return dataLength;
	}
	
	
	/**
	 * Set whether this chart scrolls itself.
	 * 
	 * @param	scroll		If true, this chart scrolls by itself; i.e. we
	 * 						advance it one data unit each time we draw.  If
	 * 						false, it is only advanced by the arrival of data.
	 */
	void setScrolling(boolean scroll) {
		scrollingChart = scroll;
	}


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

		// Re-calculate the scaling factors.
		unitScale = datumCenter ?
				chartHeight / 2 / plotRange : chartHeight / plotRange;
		dataScale = unitScale / unitSize;
		
		// Re-plot at the new scale.
		redrawGraph();
	}

	
	/**
	 * Set a scale factor on the time scale.
	 * 
	 * @param	scale			Scale factor for the time scale.  Each sample
	 * 							occupies 1 * scale pixels horizontally.
	 */
	public void setTimeScale(float scale) {
		timeScale = scale;
		
		// Size the data buffer for the new scale.
		if (chartWidth > 0)
		    setupBuffer((int) Math.ceil(chartWidth / timeScale));
	}
	

	/**
	 * Set the given value as the new value for the displayed data.
	 * Update the display accordingly.  This method should only be used
	 * if there is a single plotted value.
	 * 
	 * @param	value			The new value.
	 */
	public void setValue(float value) {
	    if (currentValue == null)
	        return;
	    
		currentValue[0] = value;
		haveValue = true;
		
		// Add the values right now, if not scrolling.
		if (!scrollingChart)
			addToChart(currentValue);
	}


	/**
	 * Set the given values as the new values for the displayed data.
	 * Update the display accordingly.
	 * 
	 * @param	values				The new values.
	 */
	public void setValue(float[] values) {
        if (currentValue == null)
            return;
        
		for (int p = 0; p < numPlots; ++p)
			currentValue[p] = values[p];
		haveValue = true;
		
		// Add the values right now, if not scrolling.
		if (!scrollingChart)
			addToChart(values);
	}


	/**
	 * Clear the current value; i.e. go back to a "no data" state.
	 */
	public void clearValue() {
		haveValue = false;
		dataCursor = 0;
		dataCount = 0;
		chartCursor = 0;
		if (chartCanvas != null)
			chartCanvas.drawColor(getBackgroundColor());
	}

	
	/**
	 * Add the given value set to the chart.  We both plot it on the chart
	 * (the offscreen bitmap), and save it in the data history to allow
	 * for later redrawing.
	 * 
	 * @param	values				The new values.
	 */
	private void addToChart(float[] values) {
	    if (dataBuffer == null)
	        return;
	    
		// Buffer the value we are about to plot.
		for (int p = 0; p < numPlots && p < values.length; ++p)
			dataBuffer[p][dataCursor] = values[p];
		if (dataCount < dataLength)
			++dataCount;

		// Advance the data cursor.
		int cursor = dataCursor;
		dataCursor = (dataCursor + 1) % dataLength;
		
		// Plot the new value on the chart.
		plotValue(cursor, chartCanvas, getPaint(), true);
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
		// If we aren't set up yet or the graph is too small, stop now.
		if (currentValue == null || unitScale < 1)
			return;

		// Add the values right now, if we're auto-scrolling.
		if (scrollingChart)
			addToChart(currentValue);
		
		// Now draw the chart on screen.  We draw it in two parts to
		// make it scroll.
		
		// First, draw the oldest values: from the next insert point (which is
		// the oldest value) to the end of the bitmap.
		drawSource1.left = Math.round(chartCursor);
		drawDest1.right = Math.round(screenX + (chartWidth - chartCursor));
		canvas.drawBitmap(chartBitmap, drawSource1, drawDest1, paint);

		// Now draw the newer values, from the left edge of the bitmap
		// to the insert point.
		drawSource2.right = Math.round(chartCursor);
		drawDest2.left = drawDest1.right;
		canvas.drawBitmap(chartBitmap, drawSource2, drawDest2, paint);
	}


	/**
	 * Redraw the graph from buffered data into the offscreen bitmap.
	 * This can be used after a rescale, for example, to re-render the graph
	 * at the new scale.
	 */
	void redrawGraph() {
	    if (chartCanvas == null)
	        return;
	        
		Paint paint = getPaint();
		
		// First clear the bitmap.
		chartCanvas.drawColor(getBackgroundColor());
		chartCursor = 0;
		
		// Figure out where to start from.
		int cursor = (dataCursor - dataCount + dataLength) % dataLength;
		
		// Draw all the data values.
		for (int i = 0; i < dataCount; ++i) {
			plotValue(cursor, chartCanvas, paint, false);
			cursor = (cursor + 1) % dataLength;
		}
	}
	
	
	/**
	 * Plot a single value on the chart.
	 * 
	 * @param	cursor			Index of the data value to plot.
	 * @param	canvas			Canvas to draw on.
	 * @param	paint			A Paint we can draw with.
	 * @param	clear			Iff true, clear ahead before drawing.
	 */
	private void plotValue(int cursor, Canvas canvas, Paint paint, boolean clear) {
		int bgColour = getBackgroundColor();

		// Figure out where we're drawing.
		int offset = dataCursor - cursor;
		if (offset <= 0)
			offset += dataLength;
		
		// Advance the plot cursor.
		chartCursor = (chartCursor + timeScale ) % chartWidth;

		// Calculate the indices of the latest and previous values.  At the
		// beginning of the data, current is also prev (so we don't draw
		// a line up from zero).
		final int indexC = cursor;
		final int indexP = offset == dataCount ? indexC :
									(cursor - 1 + dataLength) % dataLength;
		
		// Calculate the X positions on the plot of the two values.  Note
		// that the prev position xP may be off the left edge.
		final float xC = chartCursor;
		final float xP = offset == dataCount ? xC : chartCursor - timeScale;
			
		// First, clear ahead of where the value is plotted if required.
		if (clear) {
			paint.setColor(bgColour);
			paint.setStyle(Paint.Style.FILL);
			chartCanvas.drawRect(xP, 0, xC + 1, chartHeight, paint);
			if (xP < 0)
				chartCanvas.drawRect(xP + chartWidth, 0,
									 xC + chartWidth, chartHeight, paint);
		}

		// Draw scale lines.
		paint.setColor(getGridColor());
		paint.setStyle(Paint.Style.STROKE);
		paint.setStrokeWidth(SCALE_WIDTH);
		for (int y = 0; y < plotRange * unitScale; y += unitScale) {
			chartCanvas.drawLine(xP, datumPoint - y, xC, datumPoint - y, paint);
			if (datumCenter && y > 0)
				chartCanvas.drawLine(xP, datumPoint + y, xC, datumPoint + y, paint);
			if (xP < 0) {
				chartCanvas.drawLine(xP + chartWidth, datumPoint - y,
									 xC + chartWidth, datumPoint - y, paint);
				if (datumCenter && y > 0)
					chartCanvas.drawLine(xP + chartWidth, datumPoint + y,
										 xC + chartWidth, datumPoint + y, paint);
			}
		}

		// If there's no value to plot, leave it as just the scale lines.
		if (!haveValue || offset > dataCount)
			return;
		
		// Draw each of the plotted values.
		for (int p = 0; p < numPlots; ++p) {
			final float sC = dataBuffer[p][indexC] * dataScale;
			final float sP = dataBuffer[p][indexP] * dataScale;

			// Now draw the line.  Note we draw to the right, even if
			// this goes off the bitmap.
			paint.setColor(plotColours[p]);
			paint.setStrokeWidth(DATA_WIDTH);
			chartCanvas.drawLine(xP, datumPoint - sP,
								 xC, datumPoint - sC, paint);

			// If it did go off the right edge, draw the same line coming
			// in at the left edge.
			if (xP < 0)
				chartCanvas.drawLine(xP + chartWidth, datumPoint - sP,
									 xC + chartWidth, datumPoint - sC, paint);
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

	// Widths of the data plots.
	private static final int DATA_WIDTH = 2;

	
	// ******************************************************************** //
	// Private Data.
	// ******************************************************************** //

	// The number of lines plotted on this graph.
	private int numPlots;
	
	// The size of a unit of measure (for example, 1g of acceleration).
	private float unitSize = 1.0f;
	
	// How many units big to make the graph.
	private float plotRange = 1.0f;
	
	// If true, this chart scrolls by itself; i.e. we advance it one data
	// unit each time we draw.  If false, it is only advanced by the
	// arrival of data.
	private boolean scrollingChart;
	
	// Colours to plot the data lines in, one colour per line.
	private int[] plotColours;

	// The scaling factor for the data, based on unitSize, plotRange and
	// barLen.  This scales a data value to screen co-ordinates.
	private float dataScale;

	// The screen size of one unit of the measured value.
	private float unitScale;
	
	// Scale factor for the time scale.  Each sample occupies
	// 1 * timeScale pixels horizontally.
	private float timeScale = 1;

	// If true, the zero value is in the center; else at the bottom.
	private boolean datumCenter;

	// Width and height of the chart display.  chartCursor is the X position
	// at which we will draw the next value.
	private int chartWidth;
	private int chartHeight;
	private float chartCursor;

	// The datum point for the chart -- the vertical offset which
	// represents zero.
	private int datumPoint;

	// Bitmap to draw the chart into, and Canvas for drawing in it.
	private Bitmap chartBitmap = null;
	private Canvas chartCanvas = null;
	
	// THe latest value from the sensor.
	private float[] currentValue;
	private boolean haveValue = false;
	
	// Data buffer in which we store the plotted values, so we can re-plot
	// them.  The first index is the plot number, the second selects a
	// value.  dataCursor is the index at which we will insert the next
	// value.  dataCount is the number of values in the buffer, which will
	// reach and stop at dataBuffer.length.
	private float[][] dataBuffer;
	private int dataLength;
	private int dataCursor;
	private int dataCount;
	
	// X,Y position of the chart display.
	private int screenX;
	private int screenY;
	
	// Rects used for drawing the chart to the screen in two parts.
	private Rect drawSource1;
	private Rect drawDest1;
	private Rect drawSource2;
	private Rect drawDest2;
	
}

