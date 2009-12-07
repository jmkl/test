
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
import org.hermit.android.instruments.TextGauge;

import android.graphics.Canvas;
import android.graphics.Rect;


/**
 * A view which displays 3-axis sensor data in an axis-based display.
 * 
 * This could be used, for example, to show the accelerometer or
 * compass values.
 */
class AxisElement
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
	 * @param	fields			Strings representing the columns to display
	 *							in the header bar.
	 */
	public AxisElement(SurfaceRunner parent,
					   float unit, float range,
	   				   int gridCol, int plotCol, String[] fields)
	{
		super(parent, gridCol, plotCol);
		
		// Create the header bar.
    	headerBar = new HeaderBarElement(parent, fields);
    	headerBar.setBarColor(gridCol);
    	
    	xGauge = new GaugeAtom(parent, unit, range,
				   			   gridCol, Tricorder.COL_POINTER,
				   			   GaugeAtom.Orientation.BOTTOM, true);
    	yGauge = new GaugeAtom(parent, unit, range,
				   			   gridCol, Tricorder.COL_POINTER,
				   			   GaugeAtom.Orientation.LEFT, true);
    	zGauge = new GaugeAtom(parent, unit, range,
				   			   gridCol, Tricorder.COL_POINTER,
				   			   GaugeAtom.Orientation.LEFT, true);
    	
    	xyAxes = new Axis2DAtom(parent, unit, range,
	   			   			    gridCol, plotCol);
    	
    	// Size of the gauge labels.
    	final float labSize = getBaseTextSize() - 7;

    	String[] zTemplate = new String[] { parent.getRes(R.string.lab_z) };
    	zLabel = new TextGauge(parent, zTemplate, 1);
    	zLabel.setTextColor(gridCol);
    	zLabel.setTextSize(labSize);
    	
    	String[] altTemplate = new String[] { parent.getRes(R.string.lab_alt) };
    	altLabel = new TextGauge(parent, altTemplate, 1);
    	altLabel.setTextColor(gridCol);
    	altLabel.setTextSize(labSize);
	
    	ell = new EllAtom(parent, getSidebarWidth());
    	ell.setBarColor(gridCol);
    	
    	altDial = new DialAtom(parent,
    						   gridCol, Tricorder.COL_POINTER,
    						   DialAtom.Orientation.RIGHT);
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
		
		if (bounds.right - bounds.left < bounds.bottom - bounds.top)
			layoutPortrait(bounds);
		else
			layoutLandscape(bounds);
	}


    /**
     * Set up the layout of this view in portrait mode.
     * 
	 * @param	bounds		The bounding rect of this element within
	 * 						its parent View.
     */
	private void layoutPortrait(Rect bounds) {
		// First position the header bar.
		int headHeight = headerBar.getPreferredHeight();
		headerBar.setGeometry(new Rect(bounds.left, bounds.top,
									   bounds.right, bounds.top + headHeight));
		
		int x = bounds.left;
		int y = bounds.top + headHeight + getInnerGap();
		int xySize = (bounds.bottom - y) / 2 - getInterPadding();

		int plotSize = layoutXY(x, y, xySize);
		y += xySize + getInterPadding();
	
		// Position the Z gauge and its label.
		int zWidth = zGauge.getPreferredWidth();
		Rect zRect = new Rect(x, y, x + zWidth, y + plotSize);
		Rect zlRect = new Rect(x, y + plotSize + 1, x + zWidth, bounds.bottom);
		
		x += zWidth + getInterPadding();
		
		int dialWidth = altDial.getWidthForHeight(xySize);
		x = bounds.right - dialWidth;
		Rect altRect = new Rect(x, y, x + dialWidth, y + plotSize);
		Rect altlRect = new Rect(x + dialWidth / 2,
							     y + plotSize + 1, x + dialWidth, bounds.bottom);

		zGauge.setGeometry(zRect);
		zLabel.setGeometry(zlRect);
		altDial.setGeometry(altRect);
		altLabel.setGeometry(altlRect);
	}


    /**
     * Set up the layout of this view in landscape mode.
     * 
	 * @param	bounds		The bounding rect of this element within
	 * 						its parent View.
     */
	private void layoutLandscape(Rect bounds) {
		// First position the header bar.
		int headHeight = headerBar.getPreferredHeight();
		headerBar.setGeometry(new Rect(bounds.left, bounds.top,
									   bounds.right, bounds.top + headHeight));
		
		int x = bounds.left;
		int y = bounds.top + headHeight + getInnerGap();
		int xySize = bounds.bottom - y;

		int plotSize = layoutXY(x, y, xySize);
		x += xySize + getInterPadding();

		int plotTop = bounds.top + headHeight + getInnerGap();
		int plotBot = plotTop + plotSize;
	
		// Position the Z gauge and its label.
		int zWidth = zGauge.getPreferredWidth();
		Rect zRect = new Rect(x, plotTop, x + zWidth, plotBot);
		Rect zlRect = new Rect(x, plotBot + 1, x + zWidth, bounds.bottom);
		
		x += zWidth + getInterPadding();
		
		int dialWidth = altDial.getWidthForHeight(xySize);
		Rect altRect = new Rect(x, plotTop, x + dialWidth, plotBot);
		Rect altlRect = new Rect(x + dialWidth / 2, plotBot + 1,
								 x + dialWidth, bounds.bottom);
		
		x += dialWidth;

		// Distribute any excess width.
		int excess = (bounds.right - x) / 2;
		if (excess > 0) {
			zRect.left += excess;
			zRect.right += excess;
			zlRect.left += excess;
			zlRect.right += excess;
			altRect.left += excess * 2;
			altRect.right += excess * 2;
			altlRect.left += excess * 2;
			altlRect.right += excess * 2;
		}

		zGauge.setGeometry(zRect);
		zLabel.setGeometry(zlRect);
		altDial.setGeometry(altRect);
		altLabel.setGeometry(altlRect);
	}


    /**
     * Set up the layout of the X-Y plot.
     * 
	 * @param	bounds		The bounding rect of this element within
	 * 						its parent View.
     */
	private int layoutXY(int left, int top, int size) {
		int leftWidth = yGauge.getPreferredWidth();
		int bottomHeight = xGauge.getPreferredHeight();
		
		int plotSize = size - bottomHeight - getInnerGap();
		int plotBot = top + plotSize;
		int plotLeft = left + leftWidth + getInnerGap();
		
		// Position the left-side Y gauge.
		yGauge.setGeometry(new Rect(left, top, left + leftWidth, plotBot));
		
		// Position the X-Y plot.
		xyAxes.setGeometry(new Rect(plotLeft, top, left + size, plotBot));
		
		// Position the bottom X gauge.
		xGauge.setGeometry(new Rect(plotLeft, top + size - bottomHeight,
								    left + size, top + size));

		// Position the ell.
		ell.setGeometry(new Rect(left, top + size - bottomHeight,
								 left + leftWidth, top + size));
		
		return plotSize;
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
    	ell.setBarColor(grid);
    	headerBar.setBarColor(grid);
		zLabel.setTextColor(grid);
		altLabel.setTextColor(grid);
		xGauge.setDataColors(grid, Tricorder.COL_POINTER);
		yGauge.setDataColors(grid, Tricorder.COL_POINTER);
		zGauge.setDataColors(grid, Tricorder.COL_POINTER);
		xyAxes.setDataColors(grid, plot);
		altDial.setDataColors(grid, Tricorder.COL_POINTER);
	}
	

	// ******************************************************************** //
	// Data Management.
	// ******************************************************************** //

	/**
	 * Set the data range of this chart.
	 * 
     * @param   unit        The size of a unit of measure (for example,
     *                      1g of acceleration).
	 * @param	range		How many units big to make the graph.
	 */
	public void setDataRange(float unit, float range) {
		xGauge.setDataRange(unit, range);
		yGauge.setDataRange(unit, range);
		zGauge.setDataRange(unit, range);
		xyAxes.setDataRange(unit, range);
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
		xGauge.setValue(values[0]);
		yGauge.setValue(values[1]);
		zGauge.setValue(values[2]);
		xyAxes.setValues(values, mag);
		
		// Set the altitude dial.  Dials count clockwise, and clockwise is
		// up for this dial (with RIGHT orientation), so that's OK.
		altDial.setValue(alt);
	}


	/**
	 * Clear the current value; i.e. go back to a "no data" state.
	 */
	public void clearValues() {
		xGauge.clearValue();
		yGauge.clearValue();
		zGauge.clearValue();
		xyAxes.clearValues();
		altDial.clearValue();
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
		
		ell.draw(canvas, now, bg);
		headerBar.draw(canvas, now, bg);
		zLabel.draw(canvas, now, bg);
		altLabel.draw(canvas, now, bg);
		xGauge.draw(canvas, now, bg);
		yGauge.draw(canvas, now, bg);
		zGauge.draw(canvas, now, bg);
		xyAxes.draw(canvas, now, bg);
		altDial.draw(canvas, now, bg);
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

	// The header bar for the display.
	private HeaderBarElement headerBar;

	// Gauges used to display the X, Y and Z values.
	private GaugeAtom xGauge;
	private GaugeAtom yGauge;
	private GaugeAtom zGauge;
	
	// X-Y plot for the X and Y data.
	private Axis2DAtom xyAxes;
	
	private TextGauge zLabel;
	private TextGauge altLabel;
	
	// The ell in the bottom left corner.
	private EllAtom ell;
	
	// Dial gauge used to show altitude.
	private DialAtom altDial;

}

