
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

import org.hermit.android.widgets.MultistateImageButton;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;


/**
 * This class is the home view for OnWatch.  This view displays basic
 * info including time and position.
 */
public class HomeController
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
	public HomeController(OnWatch context) {
		super(context);
		
		appContext = context;
		
		// Create the controllers for the clock and location parts.
		watchClock = new ClockController(context);
		new LocationController(context);
		
		// Get the control buttons and set up their handlers.
		chimeSwitch = (MultistateImageButton)
								context.findViewById(R.id.home_chime_button);
		alertSwitch = (MultistateImageButton)
								context.findViewById(R.id.home_alert_button);
		chimeSwitch.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				MultistateImageButton but = (MultistateImageButton) arg0;
				setChimes(but.getState());
			}
		});
		alertSwitch.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				MultistateImageButton but = (MultistateImageButton) arg0;
				setAlarms(but.getState());
			}
		});

		// Get the chimer.
		bellChime = Chimer.getInstance(context);
		
		updateSettings();
	}
	
        
	// ******************************************************************** //
	// Settings Control.
	// ******************************************************************** //

    /**
     * Read our application preferences and configure ourself appropriately.
     */
    private void updateSettings() {
    	SharedPreferences prefs =
    				PreferenceManager.getDefaultSharedPreferences(appContext);

    	boolean chimeWatch = true;
    	try {
    		chimeWatch = prefs.getBoolean("chimeWatch", true);
    	} catch (Exception e) {
    		Log.i(TAG, "Pref: bad chimeWatch");
    	}
    	Log.i(TAG, "Prefs: chimeWatch " + chimeWatch);
    	chimeSwitch.setState(chimeWatch ? 1 : 0);
    	bellChime.setChimeEnable(chimeWatch);

    	int alertMode = 0;
    	try {
    		alertMode = prefs.getInt("alertMode", 0);
    	} catch (Exception e) {
    		Log.i(TAG, "Pref: bad alertMode");
    	}
    	Log.i(TAG, "Prefs: alertMode " + alertMode);
    	alertSwitch.setState(alertMode);
    	bellChime.setRepeatAlert(alertMode == 1 ? 5 :
    							     alertMode == 2 ? 10 : alertMode == 3 ? 15 : 0);
    }


    /**
     * Set the half-hourly watch chimes on or off.
     * 
     * @param	state				Requested state: 0=off, 1=on.
     */
    private void setChimes(int state) {
    	SharedPreferences prefs =
			PreferenceManager.getDefaultSharedPreferences(appContext);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean("chimeWatch", state > 0);
        editor.commit();
        bellChime.setChimeEnable(state > 0);
    }
    

    /**
     * Set the repeating alarm on or off.
     * 
     * @param	alertMode			Requested state: 0=off, 1=5 min,
     * 								2=10 min, 3=15 min.
     */
    private void setAlarms(int alertMode) {
    	SharedPreferences prefs =
			PreferenceManager.getDefaultSharedPreferences(appContext);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt("alertMode", alertMode);
        editor.commit();
        bellChime.setRepeatAlert(alertMode == 1 ? 5 :
			  					     alertMode == 2 ? 10 : alertMode == 3 ? 15 : 0);
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
		watchClock.tick(time);
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

	// Parent app we're running in.
	private OnWatch appContext;
	
	// Chimer.
	private Chimer bellChime;

    // The watch clock display.
    private ClockController watchClock;

    // Switches used to control the half-hour chimes and wake-up alarms.
    private MultistateImageButton chimeSwitch;
    private MultistateImageButton alertSwitch;

}

