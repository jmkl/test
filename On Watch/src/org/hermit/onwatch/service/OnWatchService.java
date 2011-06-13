
/**
 * On Watch: sailor's watchkeeping assistant.
 * <br>Copyright 2009 Ian Cameron Smith
 * 
 * <p>This program acts as a bridge buddy for a cruising sailor on watch.
 * It displays time and navigation data, sounds chimes and alarms, etc.
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


import org.hermit.onwatch.OnWatch;
import org.hermit.onwatch.R;
import org.hermit.onwatch.service.WeatherService.ChangeRate;
import org.hermit.onwatch.service.WeatherService.PressState;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Binder;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;


/**
 * This class is the main activity for OnWatch.
 */
public class OnWatchService
	extends Service
{

	// ******************************************************************** //
	// Public Types.
	// ******************************************************************** //

    /**
     * Class used for the client Binder.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with IPC.
     */
    public class OnWatchBinder extends Binder {
    	public OnWatchService getService() {
            // Return this instance of the service so clients can call
    		// our public methods.
            return OnWatchService.this;
        }
    }


	// ******************************************************************** //
    // Service Lifecycle.
    // ******************************************************************** //

	/**
	 * Called by the system when the service is first created.
	 */
    @Override
    public void onCreate() {
    	Log.i(TAG, "S onCreate()");
        super.onCreate();

        // Set myself up as a foreground service.
        Notification n = new Notification(R.drawable.android,
        								  getText(R.string.service_notif),
        								  System.currentTimeMillis());
        Intent ni = new Intent(this, OnWatch.class);
        PendingIntent pi = PendingIntent.getActivity(this, 0, ni, 0);
        n.setLatestEventInfo(this, getText(R.string.service_notif),
        					 getText(R.string.service_notif), pi);
        startForeground(1, n);

		// Get the wakeup manager for handling async processing.
		wakeupManager = WakeupManager.getInstance(this);
		
		// Create the sound service.
		soundService = SoundService.getInstance(this);
		
		// Set ourselves running.
		resume();
    }
    

    /**
     * Called by the system every time a client explicitly starts the service
     * by calling startService(Intent), providing the arguments it supplied
     * and a unique integer token representing the start request.
     * 
     * Note that the system calls this on your service's main thread.  A
     * service's main thread is the same thread where UI operations take
     * place for Activities running in the same process.
     * 
     * @param	intent		The Intent supplied to startService(Intent),
     * 						as given.  This may be null if the service is
     * 						being restarted after its process has gone away,
     * 						and it had previously returned anything except
     * 						START_STICKY_COMPATIBILITY.
     * @param	flags		Additional data about this start request.
     * 						Currently either 0, START_FLAG_REDELIVERY, or
     * 						START_FLAG_RETRY.
     * @param	startId		A unique integer representing this specific
     * 						request to start.  Use with stopSelfResult(int).
     * @return				May be one of the constants associated with
     * 						the START_CONTINUATION_MASK bits.  The value
     * 						indicates what semantics the system should use
     * 						for the service's current started state.
     */
    @Override
	public int onStartCommand(Intent intent, int flags, int startId) {
    	String action = intent == null ? null : intent.getAction();
    	Log.i(TAG, "onStartCommand: " + action);
    	
    	// If we got an alarm, handle it now.
    	if (action != null && action.equals(ACTION_ALARM))
    		handleAlarm();
    	
    	return START_STICKY;
    }


    /**
     * Return the communication channel to the service.  May return null if
     * clients can not bind to the service.  The returned IBinder is usually
     * for a complex interface that has been described using aidl.
     * 
     * Note that unlike other application components, calls on to the
     * IBinder interface returned here may not happen on the main thread of
     * the process.
     * 
     * @param	intent		The Intent that was used to bind to this service,
     * 						as given to Context.bindService. Note that any
     * 						extras that were included with the Intent at that
     * 						point will not be seen here.
     * @return				An IBinder through which clients can call the
     * 						service.
     */
    @Override
	public IBinder onBind(Intent intent) {
    	if (serviceBinder == null)
    		serviceBinder = new OnWatchBinder();
        
    	return serviceBinder;
    }
    
    
    /**
     * Called by the system to notify a Service that it is no longer used
     * and is being removed.  The service should clean up an resources it
     * holds (threads, registered receivers, etc) at this point.
     * 
     * Upon return, there will be no more calls in to this Service object
     * and it is effectively dead.
     */
    @Override
	public void onDestroy() {
        Log.i(TAG, "S onDestroy()");
        
        super.onDestroy();
        
        // Shut down.
        pause();
    }


	// ******************************************************************** //
	// Run Control.
	// ******************************************************************** //

    /**
     * Pause the service (e.g. for maintenance).
     */
    public void pause() {
        Log.i(TAG, "S pause()");
        
        cancelAlarms();
		
		if (passageService != null) {
			passageService.close();
			passageService = null;
		}
		
		if (weatherService != null) {
			weatherService.close();
			weatherService = null;
		}
    }
    

    /**
     * Resume the service from a pause.
     */
    public void resume() {
        Log.i(TAG, "S resume()");
        
		// Get the passage and weather services.
		passageService = PassageService.getInstance(this);
		weatherService = WeatherService.getInstance(this);
		
        // Restore our preferences.
        updatePreferences();

        // Start everything up.
		passageService.open();
		weatherService.open();
		
		// Start our regular alarms.
		setupAlarms();
    }
    

    /**
     * Shut down the service.
     */
    public void shutdown() {
        Log.i(TAG, "S shutdown()");
        
        pause();
    	stopSelf();
    }
    

	// ******************************************************************** //
	// Preferences Handling.
	// ******************************************************************** //

    /**
     * Read our application preferences and configure ourself appropriately.
     */
    private void updatePreferences() {
    	SharedPreferences prefs =
    					PreferenceManager.getDefaultSharedPreferences(this);

    	boolean chimeWatch = true;
    	try {
    		chimeWatch = prefs.getBoolean("chimeWatch", true);
    	} catch (Exception e) {
    		Log.i(TAG, "Pref: bad chimeWatch");
    	}
    	Log.i(TAG, "Prefs: chimeWatch " + chimeWatch);
    	setChimeEnable(chimeWatch);

    	SoundService.RepeatAlarmMode alertMode = SoundService.RepeatAlarmMode.OFF;
    	try {
    		String mval = prefs.getString("alertMode", "OFF");
    		alertMode = SoundService.RepeatAlarmMode.valueOf(mval);
    	} catch (Exception e) {
    		Log.i(TAG, "Pref: bad alertMode");
    	}
    	Log.i(TAG, "Prefs: alertMode " + alertMode);
    	setRepeatAlarm(alertMode);
   }


	// ******************************************************************** //
	// Speech Setup.
	// ******************************************************************** //

    /**
     * Be informed that we have done all the checks for TTS data, and the
     * TTS service is as initialised as it will ever be.
     */
    public void ttsInitialised() {
    	soundService.ttsInitialised();
    }
    
    
	// ******************************************************************** //
	// Alert Controls Handling.
	// ******************************************************************** //

	/**
	 * Query whether the half-hour watch chimes are enabled.
	 * 
	 * @return					true iff the chimes are enabled.
	 */
	public boolean getChimeEnable() {
    	return soundService.getChimeEnable();
    }


	/**
	 * Enable or disable the half-hour watch chimes.
	 * 
	 * @param	enable			true to enable chimes, false to disable.
	 */
	public void setChimeEnable(boolean enable) {
    	SharedPreferences prefs =
			PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean("chimeWatch", enable);
        editor.commit();

        soundService.setChimeEnable(enable);
    }

    
    /**
     * Get the current repeating alarm mode.
     * 
     * @return					The current mode.
     */
    public SoundService.RepeatAlarmMode getRepeatAlarm() {
    	return soundService.getRepeatAlarm();
    }
    

    /**
     * Set the repeating alarm.
     * 
     * @param	interval		Desired alarm mode.
     */
    public void setRepeatAlarm(SoundService.RepeatAlarmMode mode) {
    	SharedPreferences prefs =
			PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("alertMode", mode.toString());
        editor.commit();
        
        soundService.setRepeatAlarm(mode);
    }

    
	// ******************************************************************** //
	// Passage Service.
	// ******************************************************************** //

    /**
     * Determine whether any passage is currently running.
     */
    public boolean isAnyPassageRunning() {
        return passageService.isAnyPassageRunning();
    }


    /**
     * Determine whether a specified passage is currently running.
     * 
     * @param   uri         Database URI of the passage to check.
     */
    public boolean isRunning(Uri uri) {
        return passageService.isRunning(uri);
    }


    /**
     * Start (or restart) the specified passage.  Does nothing if there
     * is no current passage, or if it is already started.
     * 
     * @param   uri         Database URI of the passage to start.
     */
    public void startPassage(Uri uri) {
    	passageService.startPassage(uri);
    }


    /**
     * Finish the current passage.  Does nothing if there is no current
     * passage, or if it is not started or already finished.
     */
    public void finishPassage() {
    	passageService.finishPassage();
    }
    

	// ******************************************************************** //
	// Weather Data Access.
	// ******************************************************************** //

	/**
	 * Get the current weather message text, if any.
	 * 
	 * @return				Current weather message; null if none.
	 */
    public String getWeatherMessage() {
		return weatherService.getWeatherMessage();
	}
	

	// ******************************************************************** //
	// Event Handling.
	// ******************************************************************** //
    
    /**
     * Schedule our regular wakeup alarms.  We use AlarmManager to schedule
     * a regular alarm, which we handle to set off all the background
     * processing (chimes, weather logging, etc).  AlarmManager alarms
     * are delivered even when the device is asleep, unlike scheduled
     * Handler messages etc., so this technique ensures that we can keep
     * working permanently.  It is also a lot more efficient than holding
     * a wake lock.
     */
    private void setupAlarms() {
		// Create the PendingIntent that the alarm manager will fire
		// every ALARM_INTERVAL.
		Intent intent = new Intent(this, OnWatchService.class);
		intent.setAction(ACTION_ALARM);
		alarmSignal = PendingIntent.getService(this, 0, intent, 0);
		
		// Set up the repeating alarms.
		wakeupManager.setupAlarms(alarmSignal, ALARM_INTERVAL);
    }

    
    /**
     * Handle the regular wakeup alarms.
     * 
     * This alarm will be delivered even if the device was asleep.
     * The snag is that the device will go back to sleep when we finish
     * here.  So, if we need to do any asynchronous processing, we must
     * take a wake lock for the duration of that work.
     */
	private void handleAlarm() {
    	wakeupManager.handleAlarm();
	}
	
	
    /**
     * Cancel the regular wakeup alarms.
     */
    private void cancelAlarms() {
    	wakeupManager.cancelAlarms();
    }

    
    // ******************************************************************** //
    // Debug.
    // ******************************************************************** //

    /**
     * Play all the alerts, for testing.
     */
    public void debugPlayAlerts() {
		for (ChangeRate r : ChangeRate.values()) {
			if (r.alertSound != null)
				soundService.playSound(r.alertSound, null);
		}
		for (PressState r : PressState.values()) {
			if (r.alertSound != null)
				soundService.playSound(r.alertSound, null);
		}
	}


	// ******************************************************************** //
	// Class Data.
	// ******************************************************************** //

    // Debugging tag.
	private static final String TAG = "onwatch";

	// The interval between wakeup alarms, in ms.
	private static final long ALARM_INTERVAL = 5 * 60 * 1000;

	// Intent action: wakeup alarm.
	private static final String ACTION_ALARM = "org.hermit.onwatch.ACTION_ALARM";

	
	// ******************************************************************** //
	// Private Data.
	// ******************************************************************** //
	
    // The Binder given to clients.
    private IBinder serviceBinder = null;
    
    // Our wakeup manager, used for alarm processing.
    private WakeupManager wakeupManager = null;
    
    // The PendingIntent fired by the AlarmManager to do our regular updates.
    // null if not scheduled.
    private PendingIntent alarmSignal = null;

	// The sound service.
	private SoundService soundService = null;
	
	// Passage service.
	private PassageService passageService = null;

	// Weather service.
	private WeatherService weatherService = null;

}

