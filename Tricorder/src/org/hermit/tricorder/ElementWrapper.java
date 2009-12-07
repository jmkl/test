
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
 * A view which displays a basic Gauge in a Tricorder-styled window.
 */
class ElementWrapper
	extends Gauge
{

	// ******************************************************************** //
	// Constructor.
	// ******************************************************************** //

	/**
	 * Set up this view.
	 * 
     * @param   parent          Parent surface.
	 * @param	child			The child view to wrap.
     * @param   gridCol         Colour for the graph grid.
     * @param   plotCol         Colour for the graph plot.
     * @param   fields          Strings representing the columns to display
     *                          in the header bar.
	 */
	public ElementWrapper(SurfaceRunner parent, Gauge child,
	                      int gridCol, int plotCol, String[] fields)
	{
		super(parent);
		
		childView = child;
		
        // Create the header bar.
        headerBar = new HeaderBarElement(parent, fields);
        headerBar.setBarColor(gridCol);
        
        // The right-hand side bar.
        sideBar = new Gauge(parent, gridCol, plotCol);
        sideBar.setBackgroundColor(gridCol);
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
		
		// Position the side bar.
		int barWidth = getSidebarWidth();
		int barLeft = bounds.right - barWidth;
		int gap = getInnerGap();
		sideBar.setGeometry(new Rect(barLeft, chartTop,
									bounds.right, bounds.bottom));
		
		// Position the chart plot.
		childView.setGeometry(new Rect(bounds.left, chartTop,
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
		headerBar.setBarColor(grid);
		sideBar.setDataColors(grid, plot);
		childView.setDataColors(grid, plot);
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
	 * Set a specific text value displayed in the header bar.
	 * 
	 * @param	row				Row of the field to change.
	 * @param	col				Column of the field to change.
	 * @param	text			The new text field value.
	 */
	protected void setText(int row, int col, String text) {
		headerBar.setText(row, col, text);
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
		sideBar.draw(canvas, now, bg);
		childView.draw(canvas, now, bg);
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

	// THe child view we're wrapping.
	private final Gauge childView;
	
	// The header bar for the graph.
	private HeaderBarElement headerBar;

	// The right-hand side bar.
	private Gauge sideBar;

}

