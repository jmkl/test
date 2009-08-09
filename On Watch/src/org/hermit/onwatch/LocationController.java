
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


import org.hermit.onwatch.LocationModel.GpsState;
import org.hermit.utils.Angle;

import android.app.Activity;
import android.location.Location;
import android.widget.TextView;


/**
 * This class controls the GPS location display.  It displays the
 * current location info and GPS state.
 */
public class LocationController
{

	// ******************************************************************** //
    // Constructors.
    // ******************************************************************** //
	
	/**
	 * Create a location display view controller.
	 * 
	 * @param	context			Parent application.
	 */
	public LocationController(Activity context) {
		// Get the text display widgets.
    	statusField = (TextView) context.findViewById(R.id.location_status);
    	latitudeField = (TextView) context.findViewById(R.id.location_lat);
    	longitudeField = (TextView) context.findViewById(R.id.location_lon);
    	headField = (TextView) context.findViewById(R.id.location_head);
    	speedField = (TextView) context.findViewById(R.id.location_speed);
    	descriptionField = (TextView) context.findViewById(R.id.location_desc);

		// Get our location model.  Ask it to keep us up to date.
		locationModel = LocationModel.getInstance(context);
		locationModel.listen(new LocationModel.Listener() {
			@Override
			public void locChange(GpsState state, String stateMsg,
								  Location loc, String locMsg) {
				update(state, stateMsg, loc, locMsg);
			}
		});
	
		latitudeText = new StringBuilder(12);
		longitudeText = new StringBuilder(12);
	}

  
	// ******************************************************************** //
	// Display.
	// ******************************************************************** //

    /**
     * Display the current date and time.
     */
	private void update(GpsState gpsState, String stateMsg,
			  			Location l, String locMsg)
	{
//		Log.v(TAG, "Location Display: update");
		
		synchronized (this) {
			statusField.setText(stateMsg);
			
			boolean ok = gpsState == LocationModel.GpsState.ENABLED ||
						 gpsState == LocationModel.GpsState.TEMP_OOS;
			if (!ok || l == null) {
				latitudeField.setText(" ---°--.---'");
				longitudeField.setText(" ---°--.---'");
				headField.setText("---");
				speedField.setText("---");
				descriptionField.setText("---");
			} else {
				Angle.formatDegMin(l.getLatitude(), 'N', 'S', latitudeText);
				latitudeField.setText(latitudeText);
				Angle.formatDegMin(l.getLongitude(), 'E', 'W', longitudeText);
				longitudeField.setText(longitudeText);
				
				if (l.hasBearing())
					headField.setText("" + Math.round(l.getBearing()) + "°");
				else
					headField.setText("---");
				if (l.hasSpeed()) {
					// Display in knots.
					float kt0 = l.getSpeed() * 19.438445f;
					speedField.setText("" + (int) (kt0 / 10) + "." +
											(int) (kt0 % 10) + " kt");
				} else
					speedField.setText("---");
				
				descriptionField.setText(locMsg);
			}
		}
	}


	// ******************************************************************** //
	// Class Data.
	// ******************************************************************** //

    // Debugging tag.
	@SuppressWarnings("unused")
	private static final String TAG = "onwatch";

    
	// ******************************************************************** //
	// Private Data.
	// ******************************************************************** //

	// The location model we use for all our positioning.
	private LocationModel locationModel;

	// Buffer we create the latitude and longitude displays in.
	private StringBuilder latitudeText;
	private StringBuilder longitudeText;

    // Field for displaying the GPS status.
	private TextView statusField;
	   
    // Field for displaying the latitude and longitude.
    private TextView latitudeField;
    private TextView longitudeField;
    
    // Fields for heading, speed, accuracy.
    private TextView headField;
    private TextView speedField;

    // Field for displaying a location description.
	private TextView descriptionField;

}

