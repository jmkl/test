
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

import org.hermit.android.core.SurfaceRunner;
import org.hermit.android.instruments.Gauge;
import org.hermit.android.net.WebBasedData;

import android.database.Cursor;
import android.graphics.Canvas;
import android.graphics.Rect;


/**
 * A view which displays a magnitude as a vertical bar with a tracking graph.
 * 
 * This could be used, for example, to show the light level or
 * absolute magnetic field strength value.
 */
class MagnitudeElement
	extends Gauge
	implements Observer
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
	 * @param	fields			Strings representing the columns to display
	 *							in the header bar.
	 */
	public MagnitudeElement(SurfaceRunner parent,
							float unit, float range,
							int gridCol, int plotCol,
							String[] fields)
	{
		this(parent, 1, unit, range,
						gridCol, new int[] { plotCol }, fields, false);
    }


    /**
     * Set up this view.
     * 
     * @param   parent          Parent surface.
     * @param   num             The number of values plotted on this graph.
     * @param   unit            The size of a unit of measure (for example,
     *                          1g of acceleration).
     * @param   range           How many units big to make the graph.
     * @param   gridCol         Colour for the graph grid.
     * @param   plotCols        Colours for the graph plots.
     * @param   fields          Strings representing the columns to display
     *                          in the header bar.
     */
    public MagnitudeElement(SurfaceRunner parent,
                            int num, float unit, float range,
                            int gridCol, int[] plotCols,
                            String[] fields)
    {
        this(parent, num, unit, range,
                gridCol, plotCols, fields, false);
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
	 * @param	fields			Strings representing the columns to display
	 *							in the header bar.
	 * @param	centered		If true, the zero value is in the centre;
	 * 							else at the left or bottom.
	 */
	public MagnitudeElement(SurfaceRunner parent,
						    int num, float unit, float range,
							int gridCol, int[] plotCols,
							String[] fields, boolean centered)
	{
		super(parent, gridCol, plotCols[0]);
		
		numPlots = num;
		//		baseDataUnit = unit;
		//		baseDataRange = range;
		dataUnit = unit;
		dataRange = range;

		currentValue = new float[numPlots];
		tempValue = new float[numPlots];
		
		// Create the header bar.
    	headerBar = new HeaderBarElement(parent, fields);
    	headerBar.setBarColor(gridCol);
    	
    	// The magnitude gauge bar.
    	magBar = new GaugeAtom(parent, num, unit, range,
    						   gridCol, plotCols,
    						   GaugeAtom.Orientation.RIGHT, centered);

    	// The graph plot.
    	chartPlot = new ChartAtom(parent, num, unit, range,
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
	public void setGeometry(Rect bounds) {
		super.setGeometry(bounds);
		
		// First position the header bar.
		int headHeight = headerBar.getPreferredHeight();
		headerBar.setGeometry(new Rect(bounds.left, bounds.top,
									   bounds.right, bounds.top + headHeight));
		
		int chartTop = bounds.top + headHeight + getInnerGap();
		
		// Position the magnitude bar.
		int barWidth = magBar.getPreferredWidth();
		int barLeft = bounds.right - barWidth;
		int gap = getInnerGap();
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
	    while (dataRange > 10) {
	        dataRange /= 10f;
	        dataUnit *= 10f;
	    }
		magBar.setDataRange(dataUnit, dataRange);
		chartPlot.setDataRange(dataUnit, dataRange);
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
	    if (Float.isInfinite(value))
	        value = dataUnit;
	    else if (value / dataUnit > dataRange)
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
	    synchronized (currentValue) {
	        for (int i = 0; i < values.length; ++i) {
	            float value = values[i];
	            if (Float.isInfinite(value))
	                value = dataUnit;
	            else if (value / dataUnit > dataRange)
	                setDataRange(value / dataUnit);
	            currentValue[i] = value;
	        }

	        magBar.setValue(currentValue);
	        chartPlot.setValue(currentValue);
	    }
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
		    for (int i = 0; i < numPlots && i < columns.length; ++i)
		        tempValue[i] = c.getFloat(columns[i]);
		    setValue(tempValue);
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
	 * @param	now			Current system time in ms.
     * @param   bg          Iff true, tell the gauge to draw its background
     *                      first.
	 */
	@Override
	public void draw(Canvas canvas, long now, boolean bg) {
		super.draw(canvas, now, bg);
		
		headerBar.draw(canvas, now, bg);
		magBar.draw(canvas, now, bg);
		chartPlot.draw(canvas, now, bg);
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

	// If we're getting data from a data source, this is the source.
	private WebBasedData dataSource = null;

	// The number of values plotted on this gauge.
	private int numPlots;
	
	// Basic and current data unit size.
//	private final float baseDataUnit;
	private float dataUnit;

	// Basic and current range of displayed data.
//	private final float baseDataRange;
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

