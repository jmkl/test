
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


package org.hermit.onwatch.service;


import org.hermit.onwatch.R;

import android.util.Log;


/**
 * This class implements a chimer.  It sounds various audible alerts.
 */
public class Chimer
{

	// ******************************************************************** //
    // Public Classes.
    // ******************************************************************** //
	
    /**
     * Enum defining the alert modes.
     */
	public enum AlertMode {
    	OFF(0, R.drawable.ic_menu_alert_off),
    	EVERY_05(5, R.drawable.ic_menu_alert_5),
    	EVERY_10(10, R.drawable.ic_menu_alert_10),
    	EVERY_15(15, R.drawable.ic_menu_alert_15);
    	
    	AlertMode(int mins, int icon) {
    		this.minutes = mins;
    		this.icon = icon;
    	}
    	
    	public AlertMode next() {
    		if (this == EVERY_15)
    			return OFF;
    		else
    			return VALUES[ordinal() + 1];
    	}
    	
    	private static final AlertMode[] VALUES = values();
    	final int minutes;
    	public final int icon;
    }
    

	// ******************************************************************** //
    // Constructors.
    // ******************************************************************** //
	
	/**
	 * Create a chimer.  As a singleton we have a private constructor.
	 * 
	 * @param	context			Parent application.
	 */
	private Chimer(OnWatchService context) {
		appContext = context;
	}

	
	/**
	 * Get the chimer instance, creating it if it doesn't exist.
	 * 
	 * @param	context        Parent application.
	 * @return                 The chimer instance.
	 */
	static Chimer getInstance(OnWatchService context) {
		if (chimerInstance == null)
			chimerInstance = new Chimer(context);
		
		return chimerInstance;
	}
	

	// ******************************************************************** //
	// Configuration.
	// ******************************************************************** //

	/**
	 * Query whether the half-hour watch chimes are enabled.
	 * 
	 * @return					true iff the chimes are enabled.
	 */
	boolean getChimeEnable() {
    	return chimeWatch;
    }


	/**
	 * Enable or disable the half-hour watch chimes.
	 * 
	 * @param	enable			true to enable chimes, false to disable.
	 */
	void setChimeEnable(boolean enable) {
    	chimeWatch = enable;
    }

    
    /**
     * Get the current repeating alert mode.
     * 
     * @return					The current mode.
     */
    AlertMode getRepeatAlert() {
    	return alertMode;
    }
    

    /**
     * Set up a repeating alert.
     * 
     * @param	interval		Desired alert mode.
     */
    void setRepeatAlert(AlertMode mode) {
    	alertMode = mode;
    }
    

	// ******************************************************************** //
	// Event Handling.
	// ******************************************************************** //

    /**
     * Handle a wakeup alarm.
     * 
     * @param	time		The actual time in ms of the alarm, which may
     * 						be slightly before or after the boundary it
     * 						was scheduled for.
     * @param	daySecs		The number of seconds elapsed in the local day,
     * 						adjusted to align to the nearest second boundary.
     */
    void alarm(long time, int daySecs) {
    	int dayMins = daySecs / 60;
    	int hour = dayMins / 60;

    	// Chime the bells on the half hours.  Otherwise, look for
    	// an alert -- we only alert if we're not chiming the half-hour.
    	if (dayMins % 30 == 0) {
    		// We calculate the bells at the *start* of this half hour -
    		// 1 to 8.  Special for the dog watches -- first dog watch
    		// has 8 bells at the end, second goes 5, 6, 7, 8.
    		int bell = (dayMins / 30) % 8;
    		if (bell == 0 || (hour == 18 && bell == 4))
    			bell = 8;
    		Log.i(TAG, "SC tick " + dayMins + " = " + bell + " bells");
    		appContext.soundBells(bell);
    	} else {
    		int interval = alertMode.minutes;
    		if (interval > 0 && dayMins % interval == 0) {
    			Log.i(TAG, "SC tick " + dayMins + " = alert " + interval);
    			appContext.makeSound(OnWatchService.Sound.RINGRING);
    		} else
    			Log.i(TAG, "SC tick " + dayMins + " = nuffin");
    	}
    }


	// ******************************************************************** //
	// Class Data.
	// ******************************************************************** //

    // Debugging tag.
	private static final String TAG = "onwatch";
 
	// The instance of the chimer; null if not created yet.
	private static Chimer chimerInstance = null;


	// ******************************************************************** //
	// Private Data.
	// ******************************************************************** //

	// Parent app we're running in.
	private OnWatchService appContext;

	// True if the half-hour watch chimes are enabled.
	private boolean chimeWatch = false;

    // Repeating alert mode.
    private AlertMode alertMode = AlertMode.OFF;

}

