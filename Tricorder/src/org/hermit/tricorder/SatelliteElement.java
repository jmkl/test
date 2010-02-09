
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
 * A view which displays GPS satellite status graphically.
 */
class SatelliteElement
	extends Gauge
{

    // ******************************************************************** //
	// Constructor.
	// ******************************************************************** //

	/**
	 * Set up this view.
	 * 
     * @param   parent          Parent surface.
	 * @param	headBgCol		Colour for the header bar.
	 * @param	headTextCol		Colours for the header text.
	 */
	public SatelliteElement(SurfaceRunner parent,
							int headBgCol, int headTextCol)
	{
		super(parent, headBgCol, headTextCol);
		
		// Create the header bar.
        final String[] hFields = {  parent.getRes(R.string.title_sats), "99",  };
    	headerBar = new HeaderBarElement(parent, hFields);
    	headerBar.setBarColor(headBgCol);
        headerBar.setTextColor(headTextCol);
        headerBar.setText(0, 0, hFields[0]);
        
        // Create the sky diagram.
        skyMap = new SkyMapAtom(parent, headBgCol, headTextCol);
        
        // Create the list of GPS bargraphs, displaying ASU.  We'll assume
        // a WiFi ASU range from 0 to 41.  Since satellite numbers are
        // in the range 1-NUM_SATS, we'll allocate NUM_SATS + 1 so we
        // can index directly.
        gpsBars = new MiniBarElement[GeoView.NUM_SATS + 1];
        for (int g = 1; g <= GeoView.NUM_SATS; ++g) {
            gpsBars[g] = new MiniBarElement(parent, 5f, 8.2f,
                                            headBgCol, headTextCol,
                                            "00");
        }
        
        // Create the right-side bar.
        sideBar = new Gauge(parent);
        sideBar.setBackgroundColor(headBgCol);
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
	      
        int bar = getSidebarWidth();
        int gap = getInnerGap();
        int pad = getInterPadding();
        
        // Lay out the displays.
        int sx = bounds.left;
        int ex = bounds.right;
        int y = bounds.top;

		// First position the header bar.
		int headHeight = headerBar.getPreferredHeight();
		headerBar.setGeometry(new Rect(sx, y, ex, y + headHeight));
        
        y += headHeight + gap;

        // Set up the position of the right-side bar.
        sideBar.setGeometry(new Rect(ex - bar, y, ex, bounds.bottom));
        ex -= bar + pad;
        
        // Position the sky map.
        skyMap.setGeometry(new Rect(sx, y, ex, bounds.bottom));

        // Set the organization based on display size.
        int tw = ex - sx;
        int rows = tw / 16 >= 11 ? 2 : 4;
        int cols = 32 / rows;
        
        // Calculate the bar widths.  Space them out as nicely as we can.
        int bw = tw / cols;
        sx += (tw - (bw * cols)) / 2;

        // Bar height is easy.  Allow for some padding between rows.
        int bh = (bounds.bottom - y - gap * (rows - 1)) / rows;

        // Place all the GPS signal bars.
        for (int r = 0; r < rows; ++r) {
            int x = sx;
            for (int c = 0; c < cols; ++c) {
                int b = r * cols + c + 1;
                gpsBars[b].setGeometry(new Rect(x, y, x + bw, y + bh));
                x += bw;
            }
            y += bh + gap;
        }
	}


    /**
     * This is called after we have our geometry to do any required layout
     * on the given satellite status data.
     * 
     * @param   sats            The satellite data.
     */
    void formatValues(GeoView.GpsInfo[] sats) {
        skyMap.formatValues(sats);
    }


    /**
     * Toggle the mode of the satellite view.
     */
    void toggleMode() {
        barsMode = !barsMode;
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
     * Set the azimuth of the device.
     * 
     * @param   trueAz          The new true (not magnetic) azimuth in degrees.
     * @param   dec             The new magnetic declination in degrees.
     */
    public void setAzimuth(float trueAz, float dec) {
        skyMap.setAzimuth(trueAz, dec);
    }


	/**
	 * Set the given satellite status data.
	 * 
	 * @param	sats			The new satellite data.
     * @param   num             Number of sats for which we have info.
	 */
	public void setValues(GeoView.GpsInfo[] sats, int num) {
	    // Set the sat count in the header.
        headerBar.setText(0, 1, num);

	    // Set the sky map.
	    skyMap.setValues(sats);
	    
	    // Set all the signal bars.
        for (int prn = 1; prn < sats.length && prn <= GeoView.NUM_SATS; ++prn) {
            GeoView.GpsInfo ginfo = sats[prn];
            MiniBarElement bar = gpsBars[prn];
            if (ginfo.time == 0) {
                bar.setLabel("");
                bar.clearValue();
            } else {
                bar.setLabel("" + prn);
                bar.setDataColors(getGridColor(), ginfo.colour);
                bar.setValue(ginfo.snr);
            }
        }
	}


	/**
	 * Clear the satellite status data; i.e. go back to a "no data" state.
	 */
	public void clearValues() {
        for (int prn = 1; prn <= GeoView.NUM_SATS; ++prn) {
            MiniBarElement bar = gpsBars[prn];
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
	 * @param	now			Current system time in ms.
     * @param   bg          Iff true, tell the gauge to draw its background
     *                      first.
	 */
	@Override
	public void draw(Canvas canvas, long now, boolean bg) {
		super.draw(canvas, now, bg);
		
		headerBar.draw(canvas, now, bg);

		// In normal mode, draw the sky map.  In bars mode, draw the
		// GPS satellite signal bars, as many as we have.
		if (!barsMode) {
		    skyMap.draw(canvas, now, bg);
		} else {
		    for (int g = 1; g <= GeoView.NUM_SATS; ++g)
		        gpsBars[g].draw(canvas, now, bg);
		}
        
        sideBar.draw(canvas, now, bg);
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
	private HeaderBarElement headerBar;

	// Current display mode -- true to display signal bars, false for
	// the sky diagram mode.
	private boolean barsMode = false;
	
	// Sky diagram displaying satellite locations.
    private SkyMapAtom skyMap;

    // Bargraph displays for GPS signals.  Since satellite numbers are
    // in the range 1-NUM_SATS, we'll allocate NUM_SATS + 1 so we
    // can index directly.
    private MiniBarElement[] gpsBars;
	
    // Side bar element.
    private Gauge sideBar;

}

