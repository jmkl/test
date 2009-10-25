
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

import android.graphics.Canvas;
import android.graphics.Rect;
import android.location.GpsSatellite;
import android.view.SurfaceHolder;


/**
 * A view which displays GPS satellite status graphically.
 */
class SatelliteElement
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
	 * @param	headBgCol		Colour for the header bar.
	 * @param	headTextCol		Colours for the header text.
	 */
	public SatelliteElement(Tricorder context, SurfaceHolder sh,
							int headBgCol, int headTextCol)
	{
		super(context, sh, headBgCol, headTextCol);
		
		// Create the header bar.
    	headerBar = new HeaderBarElement(context, sh, new String[] { "", "" });
    	headerBar.setBarColor(headBgCol);
        headerBar.setTextColor(headTextCol);
        
        // Create the list of GPS bargraphs, displaying ASU.  We'll assume
        // a WiFi ASU range from 0 to 41.
        gpsBars = new MiniBarElement[MAX_GPS];
        for (int g = 0; g < MAX_GPS; ++g) {
            gpsBars[g] = new MiniBarElement(context, sh, 5f, 8.2f,
                                            headBgCol, headTextCol,
                                            "00");
        }
        
        // Create the right-side bar.
        sideBar = new Element(context, sh);
        sideBar.setBackgroundColor(COLOUR_GRID);
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
	      
        int bar = appContext.getSidebarWidth();
        int pad = appContext.getInterPadding();
        
        // Lay out the displays.
        int sx = bounds.left;
        int ex = bounds.right;
        int y = bounds.top;

		// First position the header bar.
		int headHeight = headerBar.getPreferredHeight();
		headerBar.setGeometry(new Rect(sx, y, ex, y + headHeight));
        
        y += headHeight + appContext.getInnerGap();

        // Set up the position of the right-side bar.
        sideBar.setGeometry(new Rect(ex - bar, y, ex, bounds.bottom));
        ex -= bar + pad;
        
        // Divide the remaining space in two.
        int colWidth = (ex - sx - pad) / 2;
        int sc = sx;
        int ec = sc + colWidth;
        int cy = y;
        
        // Place all the GPS signal bars.
        for (numBars = 0; numBars < MAX_GPS; ++numBars) {
            int bh = gpsBars[numBars].getPreferredHeight();
            if (cy + bh > bounds.bottom) {
                sc += colWidth + pad;
                ec = sc + colWidth;
                if (ec > ex)
                    break;
                cy = y;
            }
            gpsBars[numBars].setGeometry(new Rect(sc, cy, ec, cy + bh));
            cy += bh + appContext.getInnerGap();
        }
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
	 * Set the given satellite status data.
	 * 
	 * @param	sats			The new satellite data.
	 */
	public void setValues(Iterable<GpsSatellite> sats) {
        numSats = 0;
        for (GpsSatellite sat : sats) {
            if (numSats >= numBars)
                break;
            
//            float azimuth = sat.getAzimuth();
//            float elev = sat.getElevation();
            int prn = sat.getPrn();
            float snr = sat.getSnr();
            boolean hasAl = sat.hasAlmanac();
            boolean hasEph = sat.hasEphemeris();
            boolean used = sat.usedInFix();
            
            MiniBarElement bar = gpsBars[numSats];
            bar.setLabel("" + prn);
            int colour = used ? 0 : hasEph ? 1 : hasAl ? 2 : 3;
            bar.setDataColors(COLOUR_GRID, COLOUR_PLOT[colour]);

            bar.setValue(snr);
            ++numSats;
        }
        
        for (int i = numSats; i < numBars; ++i) {
            MiniBarElement bar = gpsBars[i];
            bar.setLabel("");
            bar.clearValue();
        }
	}


	/**
	 * Clear the satellite status data; i.e. go back to a "no data" state.
	 */
	public void clearValues() {
        numSats = 0;
        for (int i = numSats; i < numBars; ++i) {
            MiniBarElement bar = gpsBars[i];
            bar.setLabel("");
            bar.clearValue();
        }
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

        // Draw the GPS satellite bars, as many as we have.
		for (int g = 0; g < numBars; ++g)
		    gpsBars[g].draw(canvas, now);
        
        sideBar.draw(canvas, now);
	}
	

    // ******************************************************************** //
    // Class Data.
    // ******************************************************************** //

    // Debugging tag.
	@SuppressWarnings("unused")
	private static final String TAG = "tricorder";

    // The maximum number of GPS signals we will show.
    private static final int MAX_GPS = 26;

    // Bargraph grid and plot colours.
    private static final int COLOUR_GRID = 0xffc0a000;
    private static final int[] COLOUR_PLOT = {
        0xff00ff00, 0xffffff00, 0xffffd000, 0xffff0000,
    };

	
	// ******************************************************************** //
	// Private Data.
	// ******************************************************************** //

	// The header bar for the graph.
	private HeaderBarElement headerBar;
    
    // Bargraph displays for GPS signals.
    private MiniBarElement[] gpsBars;
	
	// The number of bars we actually fit into the display.
    private int numBars = 0;
    
    // Side bar element.
    private Element sideBar;

    // Number of satellites for which we have status info.
    private int numSats = 0;

}

