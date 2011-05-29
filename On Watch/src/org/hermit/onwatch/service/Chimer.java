
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

import android.media.AudioManager;
import android.media.SoundPool;
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

		// Get the wakeup manager for handling async processing.
		wakeupManager = WakeupManager.getInstance(appContext);
		wakeupManager.register(alarmHandler);

        // Load the sounds.
        soundPool = createSoundPool();
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

    private WakeupManager.WakeupClient alarmHandler =
    										new WakeupManager.WakeupClient() {
    	/**
	     * Handle a wakeup alarm.  A wake lock will be held while we are
	     * processing the alarm, allowing us to do asynchronous processing
	     * without letting the device sleep.  However, it's essential that we
	     * notify the caller by calling {@link #done()} when we're done.
    	 * 
    	 * @param	time		The actual time in ms of the alarm, which may
    	 * 						be slightly before or after the boundary it
    	 * 						was scheduled for.
    	 * @param	daySecs		The number of seconds elapsed in the local day,
    	 * 						adjusted to align to the nearest second boundary.
    	 */
    	public void alarm(long time, int daySecs) {
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
    			Log.i(TAG, "Chime " + dayMins + " = " + bell + " bells");
    	    	bellRinger = new BellRinger(bell, 0);
    	    	bellRinger.start();
    		} else {
    			int interval = alertMode.minutes;
    			if (interval > 0 && dayMins % interval == 0) {
    				Log.i(TAG, "Chime " + dayMins + " = alert " + interval);
        	    	bellRinger = new BellRinger(0, 1);
        	    	bellRinger.start();
    			} else {
    				Log.i(TAG, "Chime " + dayMins + " = nuffin");
    				done();
    			}
    		}
    	}
    };


	// ******************************************************************** //
	// Sound.
	// ******************************************************************** //
    
    /**
     * Create a SoundPool containing the app's sound effects.
     */
    private SoundPool createSoundPool() {
        SoundPool pool = new SoundPool(3, AudioManager.STREAM_MUSIC, 0);
        for (Sound sound : Sound.values())
            sound.soundId = pool.load(appContext, sound.soundRes, 1);
        
        return pool;
    }

    
    /**
     * Make a sound.  Play it immediately.  Don't touch the queue.
     * 
     * @param	which			ID of the sound to play.
     */
    private void makeSound(Sound which) {
        float vol = 1.0f;
        soundPool.play(which.soundId, vol, vol, 1, 0, 1f);
	}
	

	// ******************************************************************** //
	// Private Classes.
	// ******************************************************************** //

    private class BellRinger extends Thread {
    	BellRinger(int bells, int alerts) {
    		numBells = bells;
    		numAlerts = alerts;
    	}

    	public void run() {
    		while (numBells > 0) {
    			Sound bell = numBells > 1 ? Sound.BELL2 : Sound.BELL1;
    			numBells -= numBells > 1 ? 2 : 1;
    		    makeSound(bell);
    			try {
    				sleep(3000);
    			} catch (InterruptedException e) { }
    		}
    		
    		while (numAlerts > 0) {
    			--numAlerts;
    		    makeSound(Sound.RINGRING);
    			try {
    				sleep(3000);
    			} catch (InterruptedException e) { }
    		}
    		
    		try {
    			sleep(3000);
    		} catch (InterruptedException e) { }
    		
    		alarmHandler.done();
    		bellRinger = null;
    	}
    	
    	// Number of bells left to chime.
    	private int numBells;
    	
    	// Number of alerts left to sound.
    	private int numAlerts;
    }
    

	// ******************************************************************** //
	// Class Data.
	// ******************************************************************** //

    // Debugging tag.
	private static final String TAG = "onwatch";
 
	// The instance of the chimer; null if not created yet.
	private static Chimer chimerInstance = null;

    /**
     * The sounds that we make.
     */
	private static enum Sound {
    	/** A single bell. */
    	BELL1(R.raw.bells_1),
    	
    	/** Two bells. */
    	BELL2(R.raw.bells_2),
    	
    	/** An alert sound. */
    	RINGRING(R.raw.ring_ring);
    	
    	private Sound(int res) {
    		soundRes = res;
    	}
    	
    	private int soundRes;           // Resource ID for the sound file.
        private int soundId = 0;        // Sound ID for playing.
    }


	// ******************************************************************** //
	// Private Data.
	// ******************************************************************** //

	// Parent app we're running in.
	private OnWatchService appContext;
    
    // Our wakeup manager, used for alarm processing.
    private WakeupManager wakeupManager = null;

	// True if the half-hour watch chimes are enabled.
	private boolean chimeWatch = false;

    // Repeating alert mode.
    private AlertMode alertMode = AlertMode.OFF;
    
    // Sound pool used for sound effects.
    private SoundPool soundPool = null;
	
	// Ringer thread currently playing bells; null if not playing.
	private BellRinger bellRinger = null;

}

