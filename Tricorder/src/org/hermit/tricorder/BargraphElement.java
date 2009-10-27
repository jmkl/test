
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

import org.hermit.utils.CharFormatter;
import org.hermit.utils.CharFormatter.OverflowException;

import android.graphics.Canvas;
import android.graphics.Rect;
import android.util.Log;
import android.view.SurfaceHolder;


/**
 * A view which displays a magnitude as a horizontal filled bar.
 * 
 * This could be used, for example, to show the light level or
 * absolute magnetic field strength value.
 */
class BargraphElement
	extends Element
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
	 * @param	rows			The number of rows of text to display.
	 */
	public BargraphElement(Tricorder context, SurfaceHolder sh,
							float unit, float range,
							int gridCol, int plotCol,
							String[] fields, int rows)
	{
		super(context, sh, gridCol, plotCol);
		
		// Create the label.
    	headerBar = new TextAtom(context, sh, fields, rows);
    	headerBar.setTextSize(context.getBaseTextSize() - 5);
    	headerBar.setTextColor(plotCol);
    	fieldBuffers = headerBar.getBuffer();
    	
    	// The magnitude gauge bar.
    	magBar = new BargraphAtom(context, sh,
    							  unit, range,
    							  gridCol, plotCol,
				 				  BargraphAtom.Orientation.LEFT);
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
		
		int y = bounds.top;
		
		// First position the header bar.
		int headHeight = headerBar.getPreferredHeight();
		headerBar.setGeometry(new Rect(bounds.left, y,
									   bounds.right, y + headHeight));
		y += headHeight + PADDING;
		
		// Position the magnitude bar.
		magBar.setGeometry(new Rect(bounds.left, y,
									bounds.right, bounds.bottom));
	}


	/**
	 * Get the minimum preferred height for this atom.
	 * 
	 * @return			The minimum preferred height for this atom.
	 * 					Returns zero if we don't know yet.
	 */
	@Override
	int getPreferredHeight() {
		return headerBar.getPreferredHeight() +
						magBar.getPreferredHeight() + PADDING;
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
	void setDataColors(int grid, int plot) {
		headerBar.setDataColors(grid, plot);
		magBar.setDataColors(grid, plot);
	}
	

	// ******************************************************************** //
	// Data Management.
	// ******************************************************************** //

    /**
     * Get the text buffers for the field values.  The caller can change
     * a field's content by writing to the appropriate member of the
     * array, as in "buffer[row][col][0] = 'X';".
     * 
     * @return             Text buffers for the field values.
     */
    public char[][][] getBuffer() {
        return headerBar.getBuffer();
    }
    
    
	/**
	 * Set the displayed frequency value.
	 * 
	 * @param	freq			The new frequency value.
	 */
	protected void setFreq(float freq) {
        try {
            CharFormatter.formatFloat(fieldBuffers[0][0], 0, freq, 5, 3, false);
        } catch (OverflowException e) {
            Log.e(TAG, "Freq field too small");
        }
	}

    
    /**
     * Set the displayed cell ID value.
     * 
     * @param   cid             The new cell ID value.
     */
    protected void setCid(int cid) {
        try {
            CharFormatter.formatInt(fieldBuffers[0][0], 0, cid, 9, false);
        } catch (OverflowException e) {
            Log.e(TAG, "CID field too small");
        }
    }


    /**
     * Clear the displayed cell ID value.
     */
    protected void clearCid() {
        CharFormatter.blank(fieldBuffers[0][0], 0, -1);
    }

    
    /**
     * Set the displayed label.
     * 
     * @param   text            The new label.
     */
    protected void setLabel(String text) {
        CharFormatter.formatString(fieldBuffers[0][1], 0, text, -1);
    }


    /**
     * Set the displayed ASU value.
     * 
     * @param   asu             The new ASU value.
     */
    protected void setAsu(int asu) {
        try {
            CharFormatter.formatInt(fieldBuffers[0][2], 0, asu, 2, false);
        } catch (OverflowException e) {
            Log.e(TAG, "ASU field too small");
        }
    }


    /**
     * Clear the displayed ASU value.
     */
    protected void clearAsu() {
        CharFormatter.blank(fieldBuffers[0][2], 0, -1);
    }

    
	/**
	 * Set the given value as the new value for the displayed magnitude.
	 * Update the display accordingly.
	 * 
	 * @param	value				The new magnitude.
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
	 * @param	now				Current system time in ms.
	 */
	@Override
	protected void draw(Canvas canvas, long now) {
		super.draw(canvas, now);
		
		headerBar.draw(canvas, now);
		magBar.draw(canvas, now);
	}
	

    // ******************************************************************** //
    // Class Data.
    // ******************************************************************** //

    // Debugging tag.
	@SuppressWarnings("unused")
	private static final String TAG = "tricorder";
	
	// Padding under the head.
	private static final int PADDING = 3;

	
	// ******************************************************************** //
	// Private Data.
	// ******************************************************************** //

	// The header bar for the graph.
	private TextAtom headerBar;

	// The magnitude gauge bar.
	private BargraphAtom magBar;

    // Text field buffers for the header display.
    private char[][][] fieldBuffers;
    
}

