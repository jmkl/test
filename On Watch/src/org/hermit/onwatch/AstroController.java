
/**
 * On Watch: sailor's watchkeeping assistant.
 * <br>Copyright 2009 Ian Cameron Smith
 *
 * <p>This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2
 * as published by the Free Software Foundation (see COPYING).
 * 
 * <p>This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 */


package org.hermit.onwatch;


import org.hermit.geo.Position;

import android.util.Log;
import android.view.View;
import android.widget.TextView;


/**
 * Controller for the astronomical data view for OnWatch.  This class
 * displays interesting astronomical info such as rise and set times.
 *
 * @author	Ian Cameron Smith
 */
public class AstroController
	extends OnWatchController
{

	// ******************************************************************** //
    // Constructor.
    // ******************************************************************** //
	
	/**
	 * Create a view instance.
	 * 
	 * @param	context			Parent application.
	 */
	public AstroController(OnWatch context) {
		super(context);
		
		appContext = context;
		timeModel = TimeModel.getInstance(context);
		locationModel = LocationModel.getInstance(context);
		
		// Get the astro calendar widget's handle.  Tell it the ID
		// of it's parent scroller.
        astroCal = (AstroCalendarWidget) context.findViewById(R.id.astro_calendar);
        View astroScroller = context.findViewById(R.id.astro_scroller);
        astroCal.setParentScroller(astroScroller);
	}
	
    
	// ******************************************************************** //
	// State Control.
	// ******************************************************************** //

	/**
	 * Regular tick event, for housekeeping.  Occurs every second, as
	 * close as possible to the 1-second boundary.
	 * 
	 * @param	time			Current system time in millis.
	 */
	@Override
	public void tick(long time) {
		// If more than UPDATE_INTERVAL since the last update, update now.
//		if (time - lastUpdate > UPDATE_INTERVAL)
			update();
	}


	// ******************************************************************** //
	// Data Display.
	// ******************************************************************** //

	/**
	 * Update the displayed data.
	 */
	private void update() {
		// Where are we?
		Position pos = locationModel.getCurrentPos();
		if (pos == null)
			return;
		
		long time = System.currentTimeMillis();
//		Observation o = new Observation(time, pos);
		Log.i(TAG, "Update Astro " + pos);
//		
//		// Get the timezone, as decimal hours.
//		double tz = (double) timeModel.getTimezoneOffset() / 1000.0 / 3600.0;
//
//    	for (Body.Name n : Body.Name.values()) {
//    		if (n == Body.Name.EARTH)
//    			continue;
//    		
//    		int i = n.ordinal();
//    		Body b = o.getBody(n);
//    		
//    		double rise, set, mag;
//			try {
//				rise = b.get(Body.Field.RISE_TIME);
//	    		set = b.get(Body.Field.SET_TIME);
//	    		mag = b.get(Body.Field.MAGNITUDE);
//			} catch (AstroError e) {
//				new AlertDialog.Builder(appContext)
//						.setMessage("Error computing data for " + n.name +
//								    ": " + e.getMessage())
//						.setPositiveButton(R.string.button_ok, null)
//						.show();
//				return;
//			}
//    		dataFields[i][0].setText(Instant.timeAsHm(rise + tz));
//    		dataFields[i][1].setText(Instant.timeAsHm(set + tz));
//    		dataFields[i][2].setText("" + String.format("%5.1f", mag));
//    	}
//    	
//    	long elap = System.currentTimeMillis() - time;
//		Log.i(TAG, "Updated astro in " + elap + " ms");
    	
    	lastUpdate = time;
	}
	

	// ******************************************************************** //
	// Class Data.
	// ******************************************************************** //

    // Debugging tag.
	@SuppressWarnings("unused")
	private static final String TAG = "onwatch";

    // Padding at the sides of widgets.
    private static final int PAD_SIDE = 6;
    
    // Data field text size.
    private static final int DATA_SIZE = 20;
    
    // Interval at which we update the display, in ms.  Note that updates
    // involve a lot of maths.
    private static final long UPDATE_INTERVAL = 3 * 1000;


	// ******************************************************************** //
	// Private Data.
	// ******************************************************************** //

	// Parent app we're running in.
	private OnWatch appContext;

	// The time and location models.
	private TimeModel timeModel;
	private LocationModel locationModel;

    // The data fields to display the astro data.
    private TextView[][] dataFields;

    // The astronomical calendar widget.
    private AstroCalendarWidget astroCal;
	
	// Time at which we last updated the display.
	private long lastUpdate = 0;

}

