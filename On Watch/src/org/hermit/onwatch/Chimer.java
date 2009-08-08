
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


import org.hermit.onwatch.TimeModel.Field;

import android.widget.LinearLayout;


/**
 * This class implements a chimer.  It sounds various audible alerts.
 */
public class Chimer
	extends LinearLayout
{

	// ******************************************************************** //
    // Constructors.
    // ******************************************************************** //
	
	/**
	 * Create a chimer.  As a singleton we have a private constructor.
	 * 
	 * @param	context			Parent application.
	 */
	private Chimer(OnWatch context) {
		super(context);
		
		appContext = context;

        // Get the time model, and register for notification when we need
		// to ring some bells.
		timeModel = TimeModel.getInstance(context);
		timeModel.listen(TimeModel.Field.BELLS, new TimeModel.Listener() {
			@Override
			public void change(Field field, long time, int bells) {
		    	// Sound bells on a new half hour.  Note: on startup, this field
		    	// will always have changed, so only sound if we're in the first
		    	// minute of the half hour.
				int min = timeModel.get(TimeModel.Field.MINUTE);
		    	if (chimeWatch && (min == 0 || min == 30)) {
		    		int chimes = timeModel.get(TimeModel.Field.CHIMING);
		    		appContext.soundBells(chimes);
		    	}
			}
		});
		
		timeModel.listen(TimeModel.Field.MINUTE, new TimeModel.Listener() {
			@Override
			public void change(Field field, long time, int min) {
		    	// Only alert if we're not chiming the half-hour.
				boolean bells = timeModel.changed(TimeModel.Field.BELLS);
		    	if (alertInterval > 0 && !bells && min % alertInterval == 0)
		    		appContext.makeSound(OnWatch.Sound.RINGRING);
			}
		});
	}

	
	/**
	 * Get the time model, creating it if it doesn't exist.
	 * 
	 * @param	context			Parent application.
	 */
	public static Chimer getInstance(OnWatch context) {
		if (chimerInstance == null)
			chimerInstance = new Chimer(context);
		
		return chimerInstance;
	}
	

	// ******************************************************************** //
	// Configuration.
	// ******************************************************************** //

	/**
	 * Enable or disable the half-hour watch chimes.
	 * 
	 * @param	enable			true to enable chimes, false to disable.
	 */
	public void setChimeEnable(boolean enable) {
    	chimeWatch = enable;
    }

    
    /**
     * Set up a repeating alert.
     * 
     * @param	interval		Alert interval in minutes.  Zero to
     * 							disable alerts.
     */
    public void setRepeatAlert(int interval) {
    	alertInterval = interval;
    }
    

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
	private OnWatch appContext;
	
	// Watch calendar, which does all our date/time calculations.
	private TimeModel timeModel;

	// True if the half-hour watch chimes are enabled.
	private boolean chimeWatch = false;

    // Repeating alert interval in minutes.  Zero to disable alerts
    private int alertInterval = 0;

}

