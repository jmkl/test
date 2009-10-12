
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

import java.util.Observable;
import java.util.Observer;

import org.hermit.android.net.WebBasedData;

import android.database.Cursor;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.view.SurfaceHolder;


/**
 * A view which displays a magnitude as a vertical bar with a tracking graph.
 * 
 * This could be used, for example, to show the light level or
 * absolute magnetic field strength value.
 */
class MagnitudeElement
	extends Element
	implements Observer
{

	// ******************************************************************** //
	// Constructor.
	// ******************************************************************** //

	/**
	 * Set up this view.
	 * 
	 * @param	context			Parent application context.
     * @param	sh				SurfaceHolder we're drawing in.
	 * @param	unit			The size of a unit of measure (for example,
	 * 							1g of acceleration).
	 * @param	range			How many units big to make the graph.
	 * @param	gridCol			Colour for the graph grid.
	 * @param	plotCol			Colour for the graph plot.
	 * @param	fields			Strings representing the columns to display
	 *							in the header bar.
	 * @param	rows			The number of rows to display.
	 */
	public MagnitudeElement(Tricorder context, SurfaceHolder sh,
							float unit, float range,
							int gridCol, int plotCol,
							String[] fields, int rows)
	{
		this(context, sh, 1, unit, range,
						gridCol, new int[] { plotCol }, fields, rows, false);
	}


	/**
	 * Set up this view.
	 * 
	 * @param	context			Parent application context.
     * @param	sh				SurfaceHolder we're drawing in.
	 * @param	num				The number of values plotted on this graph.
	 * @param	unit			The size of a unit of measure (for example,
	 * 							1g of acceleration).
	 * @param	range			How many units big to make the graph.
	 * @param	gridCol			Colour for the graph grid.
	 * @param	plotCols		Colours for the graph plots.
	 * @param	fields			Strings representing the columns to display
	 *							in the header bar.
	 * @param	rows			The number of rows to display.
	 * @param	centered		If true, the zero value is in the centre;
	 * 							else at the left or bottom.
	 */
	public MagnitudeElement(Tricorder context, SurfaceHolder sh,
						    int num, float unit, float range,
							int gridCol, int[] plotCols,
							String[] fields, int rows, boolean centered)
	{
		super(context, sh, gridCol, plotCols[0]);
		
		surfaceHolder = sh;

		numPlots = num;
		dataUnit = unit;
		baseDataRange = range;
		dataRange = range;
		
		currentValue = new float[numPlots];
		tempValue = new float[numPlots];
		
		// Create the header bar.
    	headerBar = new HeaderBarElement(context, sh, fields, rows);
    	headerBar.setBarColor(gridCol);
    	
    	// The magnitude gauge bar.
    	magBar = new GaugeAtom(context, sh, num, unit, range,
    						   gridCol, plotCols,
    						   GaugeAtom.Orientation.RIGHT, centered);

    	// The graph plot.
    	chartPlot = new ChartAtom(context, sh, num, unit, range,
    							  gridCol, plotCols, centered);
	}


	/**
	 * Set the data fields to display.  Calling this allows the user
	 * to then call setValue(ContentValues values).
	 * 
     * @param   source          Data source for this element.
	 * @param	fields			Data fields to display when the user
	 * 							calls setValue(ContentValues values).
	 */
	public void setDataSource(WebBasedData source, String[] fields) {
		dataSource = source;
		dataFields = fields;
		
		// We don't want this to scroll on its own.
		setScrolling(false);
		
		// Register for updates from the source.
		source.addObserver(this);
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
		
		// First position the header bar.
		int headHeight = headerBar.getPreferredHeight();
		headerBar.setGeometry(new Rect(bounds.left, bounds.top,
									   bounds.right, bounds.top + headHeight));
		
		int chartTop = bounds.top + headHeight + getContext().getInnerGap();
		
		// Position the magnitude bar.
		int barWidth = magBar.getPreferredWidth();
		int barLeft = bounds.right - barWidth;
		int gap = getContext().getInnerGap();
		magBar.setGeometry(new Rect(barLeft, chartTop,
									bounds.right, bounds.bottom));
		
		// Position the chart plot.
		chartPlot.setGeometry(new Rect(bounds.left, chartTop,
									   barLeft - gap, bounds.bottom));
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
	void setDataColors(int grid, int plot) {
		setDataColors(grid, new int[] { plot });
	}
	

	/**
	 * Set the colours of this element.
	 * 
     * @param	grid			Colour for drawing a data scale / grid.
     * @param	plot			Colours for drawing data plots.
	 */
	void setDataColors(int grid, int[] plot) {
		headerBar.setBarColor(grid);
		magBar.setDataColors(grid, plot);
		chartPlot.setDataColors(grid, plot);
	}
	

	// ******************************************************************** //
	// Data Management.
	// ******************************************************************** //

	/**
	 * Get the number of data items displayed.
	 */
	int getDataLength() {
		return chartPlot.getDataLength();
	}


	/**
	 * Set whether this chart scrolls itself.
	 * 
	 * @param	scroll		If true, this chart scrolls by itself; i.e. we
	 * 						advance it one data unit each time we draw.  If
	 * 						false, it is only advanced by the arrival of data.
	 */
	void setScrolling(boolean scroll) {
		chartPlot.setScrolling(scroll);
	}

	
	/**
	 * Set a scale factor on the time scale.
	 * 
	 * @param	scale			Scale factor for the time scale.  Each sample
	 * 							occupies 1 * scale pixels horizontally.
	 */
	public void setTimeScale(float scale) {
		chartPlot.setTimeScale(scale);
	}
	

	/**
	 * Get the data range of this chart.
	 * 
	 * @return					How many units big to make the graph.
	 */
	public float getDataRange() {
		return dataRange;
	}


	/**
	 * Set the data range of this chart.
	 * 
	 * @param	range			How many units big to make the graph.
	 */
	public void setDataRange(float range) {
		dataRange = range;
		magBar.setDataRange(range);
		chartPlot.setDataRange(range);
	}


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
	 * Set the text values displayed in the header bar.
	 * 
	 * @param	text			The new text field values.
	 */
	protected void setText(String[][] text) {
		headerBar.setText(text);
	}


	/**
	 * Set a specific text value displayed in the header bar.
	 * 
	 * @param	row				Row of the field to change.
	 * @param	col				Column of the field to change.
	 * @param	text			The new text field value.
	 */
	protected void setText(int row, int col, String text) {
		headerBar.setText(row, col, text);
	}


	/**
	 * Set the given value as the new value for the displayed data.
	 * Update the display accordingly.
	 * 
	 * @param	value				The new value.
	 */
	public void setValue(float value) {
	    if (value / dataUnit > dataRange)
	        setDataRange(value / dataUnit);
		magBar.setValue(value);
		chartPlot.setValue(value);
	}


	/**
	 * Set the given values as the new values for the displayed data.
	 * Update the display accordingly.
	 * 
	 * @param	values				The new values.
	 */
	public void setValue(float[] values) {
	    for (int i = 0; i < values.length; ++i) {
	        final float value = values[i];
	        if (value / dataUnit > dataRange)
	            setDataRange(value / dataUnit);
	        currentValue[i] = value;
	    }
		
		magBar.setValue(currentValue);
		chartPlot.setValue(currentValue);
	}


	/**
	 * Clear the current value; i.e. go back to a "no data" state.
	 */
	public void clearValue() {
		magBar.clearValue();
		chartPlot.clearValue();
	}


	/**
	 * Observer method, called when an update happens on a data source
	 * we're observing.
	 * 
	 * @param	o			The Observable.
	 * @param	arg			The update argument.  This is the timestamp
	 * 						of the last record loaded (not the file last
	 * 						mod date).
	 */
	public void update(Observable o, Object arg) {
		if (!(o instanceof WebBasedData) || !(arg instanceof Long))
			return;
		long latest = (Long) arg;

		// Get all the available data since our last update.
		Cursor c = dataSource.allRecordsSince(latestRecord);

		// Get the column indices.
		int[] columns = new int[dataFields.length];
		for (int i = 0; i < dataFields.length; ++i)
			columns[i] = c.getColumnIndex(dataFields[i]);

		// Now, go through all the data sets and add them.
		while (c.moveToNext()) {
			synchronized (surfaceHolder) {
				for (int i = 0; i < numPlots && i < columns.length; ++i)
					tempValue[i] = c.getFloat(columns[i]);
				setValue(tempValue);
			}
		}
		c.close();

		// Update where we are.
		latestRecord = latest;
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
		magBar.draw(canvas, now);
		chartPlot.draw(canvas, now);
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

	// The surface we're drawing on.
	private final SurfaceHolder surfaceHolder;

	// If we're getting data from a data source, this is the source.
	private WebBasedData dataSource = null;

	// The number of values plotted on this gauge.
	private int numPlots;
	
	// Data unit size.
	private float dataUnit;

	// Basic and current range of displayed data.
	public final float baseDataRange;
	private float dataRange;

	// If this is set, the user can call setValue(ContentValues values);
	// the fields pulled out of values for display are the ones named
	// by dataFields.
	private String[] dataFields = null;
	
	// The header bar for the graph.
	private HeaderBarElement headerBar;

	// The magnitude gauge bar.
	private GaugeAtom magBar;

	// The graph plot.
	private ChartAtom chartPlot;

	// When using a data source, the timestamp of the latest value we have.
	private long latestRecord = 0;
	
	// The current values.
	private float[] currentValue = null;
	
	// Temp. values array.
	private float[] tempValue = null;

}

