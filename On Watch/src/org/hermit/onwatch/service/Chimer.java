
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


import org.hermit.android.utils.Ticker;
import org.hermit.onwatch.R;


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
	 * @param	ticker          Ticker instance to use for time management.
	 */
	private Chimer(OnWatchService context, Ticker ticker) {
		appContext = context;
        timeTicker = ticker;

    	// Ask the ticker to ping us on the 5 minutes for the bells.
    	timeTicker.listen(5 * 60, tickHandler);
	}

	
	/**
	 * Get the chimer instance, creating it if it doesn't exist.
	 * 
	 * @param	context        Parent application.
	 * @param	ticker         Ticker instance to use for time management.
	 * @return                 The chimer instance.
	 */
	public static Chimer getInstance(OnWatchService context, Ticker ticker) {
		if (chimerInstance == null)
			chimerInstance = new Chimer(context, ticker);
		
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
	public boolean getChimeEnable() {
    	return chimeWatch;
    }


	/**
	 * Enable or disable the half-hour watch chimes.
	 * 
	 * @param	enable			true to enable chimes, false to disable.
	 */
	public void setChimeEnable(boolean enable) {
    	chimeWatch = enable;
    }

    
    /**
     * Get the current repeating alert mode.
     * 
     * @return					The current mode.
     */
    public AlertMode getRepeatAlert() {
    	return alertMode;
    }
    

    /**
     * Set up a repeating alert.
     * 
     * @param	interval		Desired alert mode.
     */
    public void setRepeatAlert(AlertMode mode) {
    	alertMode = mode;
    }
    

	// ******************************************************************** //
	// Event Handling.
	// ******************************************************************** //

    private Ticker.Listener tickHandler = new Ticker.Listener() {
		@Override
		public void tick(long time, int daySecs) {
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
				appContext.soundBells(bell);
			} else {
				int interval = alertMode.minutes;
				if (interval > 0 && dayMins % interval == 0)
					appContext.makeSound(OnWatchService.Sound.RINGRING);
			}
		}
	};
	

	// ******************************************************************** //
	// Class Data.
	// ******************************************************************** //

    // Debugging tag.
	@SuppressWarnings("unused")
	private static final String TAG = "onwatch";
 
	// The instance of the chimer; null if not created yet.
	private static Chimer chimerInstance = null;


	// ******************************************************************** //
	// Private Data.
	// ******************************************************************** //

	// Parent app we're running in.
	private OnWatchService appContext;
	
    // The ticker we use for timing.
	private Ticker timeTicker;

	// True if the half-hour watch chimes are enabled.
	private boolean chimeWatch = false;

    // Repeating alert mode.
    private AlertMode alertMode = AlertMode.OFF;

}

