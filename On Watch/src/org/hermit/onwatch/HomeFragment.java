
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


import org.hermit.onwatch.CrewModel.Crew;
import org.hermit.utils.Angle;
import org.hermit.utils.TimeUtils;

import android.graphics.drawable.Drawable;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;


/**
 * This class controls the home display.  It displays the
 * current location info and GPS state, as well as the watch clock.
 */
public class HomeFragment
	extends ViewFragment
{

	// ******************************************************************** //
    // Fragment Lifecycle.
    // ******************************************************************** //
	
	/**
	 * Called to do initial creation of a fragment. This is called after
	 * onAttach(Activity) and before
	 * onCreateView(LayoutInflater, ViewGroup, Bundle).
	 * 
	 * Note that this can be called while the fragment's activity is still
	 * in the process of being created.  As such, you can not rely on things
	 * like the activity's content view hierarchy being initialized at this
	 * point.  If you want to do work once the activity itself is created,
	 * see onActivityCreated(Bundle).
	 * 
	 * @param	icicle		If the fragment is being re-created from a
	 * 						previous saved state, this is the state.
	 */
	public void onCreate(Bundle icicle) {
		Log.i(TAG, "onCreate(" + (icicle != null ? "icicle" : "null") + ")");
		
		super.onCreate(icicle);
	}


	/**
	 * Called to have the fragment instantiate its user interface view.
	 * This is optional, and non-graphical fragments can return null
	 * (which is the default implementation).  This will be called between
	 * onCreate(Bundle) and onActivityCreated(Bundle).
	 *
	 * If you return a View from here, you will later be called in
	 * onDestroyView() when the view is being released.
	 *
	 * @param	inflater	The LayoutInflater object that can be used to
	 * 						inflate any views in the fragment.
	 * @param	container	If non-null, this is the parent view that the
	 * 						fragment's UI should be attached to.  The
	 * 						fragment should not add the view itself, but
	 * 						this can be used to generate the LayoutParams
	 * 						of the view.
	 * @param	icicle		If non-null, this fragment is being re-constructed
	 * 						from a previous saved state as given here.
	 * @return				The View for the fragment's UI, or null.
	 */
	public View onCreateView(LayoutInflater inflater,
							 ViewGroup container,
							 Bundle icicle)
	{
		Log.i(TAG, "onCreateView(" + (icicle != null ? "icicle" : "null") + ")");
		
        // Inflate the layout for this fragment
        appContext = (OnWatch) container.getContext();
        View view = inflater.inflate(R.layout.home_view, container, false);
 		
		
		// Clock
		
		timeModel = TimeModel.getInstance(appContext);
		crewModel = CrewModel.getInstance(appContext);
        crewModel.open();

		// Register for watch crew changes.
		crewModel.listen(new CrewModel.Listener() {
			@Override
			public void watchPlanChanged() {
			}
			@Override
			public void watchChange(int day, int watch, Crew[] crew) {
				crewField.setText(crewModel.getWatchCrewNames());
		    	nextCrewField.setText("");
			}
			@Override
			public void watchAlert(Crew[] nextCrew) {
		    	nextCrewField.setText(crewModel.getNextCrewNames());
			}
		});

		// Get the relevant widgets.  Set a handler on the zone set button.
		dateField = (TextView) view.findViewById(R.id.clock_date);
		zoneChoice = (Button) view.findViewById(R.id.clock_set_zone);
		timeField = (TextView) view.findViewById(R.id.clock_time);
		watchField = (TextView) view.findViewById(R.id.clock_watch);
		crewField = (TextView) view.findViewById(R.id.clock_watchcrew);
		nextCrewField = (TextView) view.findViewById(R.id.clock_nextcrew);

    	zoneChoice.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				((OnWatch) appContext).requestTimezone();
			}
    	});

		timeText = new StringBuilder(8);
		timeText.append("00:00:00");
		
    	crewField.setText(crewModel.getWatchCrewNames());
    	if (crewModel.getTimeToNext() < 15)
    		nextCrewField.setText(crewModel.getNextCrewNames());
    	else
    		nextCrewField.setText("");

    	
    	
    	// Location
    	
		// Get the text display widgets.
    	statusField = (TextView) view.findViewById(R.id.location_status);
    	latitudeField = (TextView) view.findViewById(R.id.location_lat);
    	longitudeField = (TextView) view.findViewById(R.id.location_lon);
    	headField = (TextView) view.findViewById(R.id.location_head);
    	speedField = (TextView) view.findViewById(R.id.location_speed);
    	descriptionField = (TextView) view.findViewById(R.id.location_desc);

		// Get our location model.  Ask it to keep us up to date.
		locationModel = LocationModel.getInstance(appContext);
		locationModel.listen(new LocationModel.Listener() {
			@Override
			public void locChange(LocationModel.GpsState state, String stateMsg,
								  Location loc, String locMsg) {
				updateLocation(state, stateMsg, loc, locMsg);
			}
		});
	
		latitudeText = new StringBuilder(12);
		longitudeText = new StringBuilder(12);

    	
    	
        
        return view;
	}

	
	/**
	 * Called when the fragment is visible to the user and actively running.
	 * This is generally tied to Activity.onResume() of the containing
	 * Activity's lifecycle.
	 */
	public void onResume () {
		Log.i(TAG, "onResume()");
		
		super.onResume();
	}

	
	/**
	 * Called when the Fragment is no longer resumed.  This is generally
	 * tied to Activity.onPause of the containing Activity's lifecycle.
	 */
	public void onPause() {
		Log.i(TAG, "onPause()");
		
		super.onPause();
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
	public void tick(long time) {
		updateClock();
	}


    /**
     * Display the current date and time.
     */
    private void updateClock() {
    	// Display the date, and set the timezone button.
    	String wday = timeModel.getName(TimeModel.Field.WDAY);
    	String mon = timeModel.getName(TimeModel.Field.MONTH);
    	String day = timeModel.getName(TimeModel.Field.DAY);
    	
    	String date = wday + " " + day + " " + mon;
    	String zoff = TimeUtils.intervalMsToHmsShort(timeModel.getTimezoneOffset());
    	dateField.setText(date);
    	
    	boolean naut = timeModel.isNauticalTime();
    	zoneChoice.setText((naut ? "Naut" : "Civil") + " " + zoff);
    	
    	// Display the time.
    	int hour = timeModel.get(TimeModel.Field.HOUR);
    	int min = timeModel.get(TimeModel.Field.MINUTE);
    	int sec = timeModel.get(TimeModel.Field.SECOND);
    	timeText.setCharAt(0, (char) ('0' + hour / 10));
    	timeText.setCharAt(1, (char) ('0' + hour % 10));
    	timeText.setCharAt(3, (char) ('0' + min / 10));
    	timeText.setCharAt(4, (char) ('0' + min % 10));
    	timeText.setCharAt(6, (char) ('0' + sec / 10));
    	timeText.setCharAt(7, (char) ('0' + sec % 10));
    	timeField.setText(timeText);

    	// Display the watch name and names of the on-watch crew.
    	watchField.setText(timeModel.getWatchName());
//    	crewField.setText(crewModel.getWatchCrewNames());
    	Drawable bar = crewField.getBackground();
    	bar.setLevel((int) (crewModel.getWatchFrac() * 10000));
//    	nextCrewField.setText(crewModel.getNextCrewNames());
    }


    /**
     * Display the current date and time.
     */
	private void updateLocation(LocationModel.GpsState gpsState, String stateMsg,
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
	// Debug Control.
	// ******************************************************************** //

//	void setDebug(boolean space, boolean time) {
//		debugSpace = space;
//		debugTime = time;
//		mainView.setFocusable(space || time);
//		
//		if (!space) {
//			LocationModel locModel = LocationModel.getInstance(appContext);
//			locModel.adjustReset();
//		}
//		if (!time) {
//			TimeModel timeModel = TimeModel.getInstance(appContext);
//			timeModel.adjustReset();
//		}
//	}
	

//	/**
//	 * Handle a key event.
//	 * 
//	 * @param	keyCode			Key code that represents the button pressed.
//	 * @param	event			KeyEvent object that defines the button action.
//	 * @return					If you handled the event, return true.
//	 * 							If you want to allow the event to be handled
//	 *							by the next receiver, return false. 
//	 */
//	private boolean debugKey(int keyCode, KeyEvent event) {
//		if (debugSpace) {
//			LocationModel locModel = LocationModel.getInstance(appContext);
//			double amt = 11.33;
//			switch (keyCode) {
//			case KeyEvent.KEYCODE_DPAD_LEFT:
//				locModel.adjust(0, -amt);
//				return true;
//			case KeyEvent.KEYCODE_DPAD_UP:
//				locModel.adjust(amt, 0);
//				return true;
//			case KeyEvent.KEYCODE_DPAD_RIGHT:
//				locModel.adjust(0, amt);
//				return true;
//			case KeyEvent.KEYCODE_DPAD_DOWN:
//				locModel.adjust(-amt, 0);
//				return true;
//			}
//		}
//
//		if (debugTime) {
//			TimeModel timeModel = TimeModel.getInstance(appContext);
//			switch (keyCode) {
//			case KeyEvent.KEYCODE_DPAD_UP:
//				timeModel.adjust(1);
//				return true;
//			case KeyEvent.KEYCODE_DPAD_RIGHT:
//				timeModel.adjust(2);
//				return true;
//			case KeyEvent.KEYCODE_DPAD_DOWN:
//				timeModel.adjust(3);
//				return true;
//			}
//		}
//
//		return false;
//	}


	// ******************************************************************** //
	// Class Data.
	// ******************************************************************** //

    // Debugging tag.
	private static final String TAG = "onwatch";

    
	// ******************************************************************** //
	// Private Data.
	// ******************************************************************** //

	// Parent app we're running in.
	private OnWatch appContext;

    
    
    // Clock
	
	// Watch calendar, which does all our date/time calculations.
	private TimeModel timeModel;
	
	// The crew list and watch plan model.
	private CrewModel crewModel;
	
	// Buffer we create the time display in.
	private StringBuilder timeText;
	   
    // Fields for displaying the date and time.
	private TextView dateField;
    private TextView timeField;
    
    // Timezone selection button.
    private Button zoneChoice;
    
    // Field for displaying the watch name and the watch crew names.
    private TextView watchField;
    private TextView crewField;
    private TextView nextCrewField;

    
    
    // Location

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

