
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
import org.hermit.utils.CharFormatter;

import android.graphics.Canvas;
import android.graphics.Rect;


/**
 * A view which displays a magnitude as a mini bar display with no
 * header text.
 * 
 * This could be used, for example, to show a GPS satellite signal bar.
 */
class MiniBarElement
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
     * @param   text            Initial text for the label.
	 */
	public MiniBarElement(SurfaceRunner parent,
							float unit, float range,
							int gridCol, int plotCol, String text)
	{
		super(parent, gridCol, plotCol);
		
		// Create the label.
        String[] template = new String[] { text };
    	barLabel = new TextGauge(parent, template, 1);
    	barLabel.setTextSize(getTinyTextSize());
    	barLabel.setTextColor(plotCol);
    	fieldBuffers = barLabel.getBuffer();
    	
    	// The magnitude gauge bar.
    	magBar = new BargraphAtom(parent, unit, range, gridCol, plotCol,
				 				  BargraphAtom.Orientation.BOTTOM);
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
		
        int gap = getInnerGap();
		int x = bounds.left;
        int y = bounds.bottom;
        int w = bounds.right - bounds.left;
		int cx = x + w / 2;
		
		// First position the label.
		int lw = barLabel.getPreferredWidth();
        int lh = barLabel.getPreferredHeight() - 3;
		barLabel.setGeometry(new Rect(cx - lw / 2, y - lh,
		                              cx + lw / 2, y));
		y -= lh + gap;
		
		// Position the magnitude bar.
		magBar.setGeometry(new Rect(cx - 4, bounds.top, cx + 4, y));
	}


	/**
	 * Get the minimum preferred height for this atom.
	 * 
	 * @return			The minimum preferred height for this atom.
	 * 					Returns zero if we don't know yet.
	 */
	@Override
	public int getPreferredWidth() {
	    int bw = 8;
	    int lw = barLabel.getPreferredWidth();
		return bw > lw ? bw : lw;
	}
	

    /**
     * Get the minimum preferred height for this atom.
     * 
     * @return          The minimum preferred height for this atom.
     *                  Returns zero if we don't know yet.
     */
    @Override
    public int getPreferredHeight() {
        return barLabel.getPreferredHeight() - 4 + magBar.getPreferredHeight();
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
		barLabel.setDataColors(grid, plot);
		magBar.setDataColors(grid, plot);
	}
	

	// ******************************************************************** //
	// Data Management.
	// ******************************************************************** //

	/**
	 * Set the text values displayed in this view.
	 * 
	 * @param	text			The new text field values.
	 */
	protected void setLabel(String text) {
	    CharFormatter.formatString(fieldBuffers[0][0], 0, text, -1);
	}


	/**
	 * Set the given value as the new value for the displayed data.
	 * Update the display accordingly.
	 * 
	 * @param	value				The new value.
	 */
	public void setValue(float value) {
		magBar.setValue(value);
	}


	/**
	 * Clear the current value; i.e. go back to a "no data" state.
	 */
	public void clearValue() {
		magBar.clearValue();
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
		
		barLabel.draw(canvas, now, bg);
		magBar.draw(canvas, now, bg);
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

	// The header bar for the graph.
	private TextGauge barLabel;

	// The magnitude gauge bar.
	private BargraphAtom magBar;
	
    // Text field buffers for the label display.
    private char[][][] fieldBuffers;
    
}

