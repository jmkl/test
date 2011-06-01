
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


import java.util.ArrayList;
import java.util.Calendar;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.util.Log;


/**
 * WakeupManager manages periodic wakeup alarms, and facilitates asynchronous
 * processing by co-ordinated management of wake locks.  Alarms are handled
 * even when the device is asleep; so it is appropriate to use this class
 * to co-ordinate long-term, low-overhead background processing at reasonably
 * long intervals (e.g. 5 minutes).
 * 
 * <p>We use the AlarmManager to schedule regular alarms.  This gives
 * us the ability to work while the device is asleep; however, it sadly makes
 * the client interface a bit more complex, as alarms are delivered as intents.
 * So, to use this class, clients must:</p>
 * 
 * <ul>
 * <li>Create a {@link android.app.PendingIntent} which we can register
 *     with the AlarmManager, and give us that intent in
 *     {@link #setupAlarms(PendingIntent, long)}.</li>
 * <li>Catch the intent coming in to the application, and call
 *     {@link #handleAlarm()} when it does so.</li>
 * </ul>
 */
public class WakeupManager
{

	// ******************************************************************** //
    // Public Classes.
    // ******************************************************************** //
	
    /**
     * Enum defining the alert modes.
     */
	public abstract static class WakeupClient {

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
		public abstract void alarm(long time, int daySecs);
	    
		/**
		 * We are finished processing the alarm.  The client must call
		 * this when done.  It can be called from any thread.
		 */
		public final void done() {
			if (handler == null)
				throw new IllegalStateException("WakeupClient must be registered" +
												" before done() can be called");
			handler.sendEmptyMessage(message);
		}
		
		
		/**
		 * Set this client up with the handler it needs to notify when
		 * done, and the message to send.  WakeupManager calls this when the
		 * client is registered.
		 */
		private void setup(Handler handler, int message) {
			this.handler = handler;
			this.message = message;
		}
		
		
		private Handler handler = null;
		private int message = 0;
    }
    

	// ******************************************************************** //
    // Constructors.
    // ******************************************************************** //
	
	/**
	 * Create a chimer.  As a singleton we have a private constructor.
	 * 
	 * @param	context			Parent application.
	 */
	private WakeupManager(Context context) {
		appContext = context;
		
		// We use partial wake locks while handling alarms, so we can complete
		// asynchronous processing without the device sleeping.  Set that 
		// up here.
		powerManager = (PowerManager)
					appContext.getSystemService(Context.POWER_SERVICE);
		alarmLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
										     TAG);
		
		// Get the alarm manager.
		alarmManager = (AlarmManager)
					appContext.getSystemService(Context.ALARM_SERVICE);

		wakeupClients = new ArrayList<WakeupClient>();
		numClientIds = 0;
		clientsNotified = 0;
	}


	/**
	 * Get the chimer instance, creating it if it doesn't exist.
	 * 
	 * @param	context        Parent application.
	 * @return                 The chimer instance.
	 */
	static WakeupManager getInstance(Context context) {
		if (chimerInstance == null)
			chimerInstance = new WakeupManager(context);
		
		return chimerInstance;
	}
	

	// ******************************************************************** //
	// Setup.
	// ******************************************************************** //

    /**
     * Schedule our regular wakeup alarms.  We use AlarmManager to schedule
     * a regular alarm, which we handle to set off all the background
     * processing (chimes, weather logging, etc).  AlarmManager alarms
     * are delivered even when the device is asleep, unlike scheduled
     * Handler messages etc., so this technique ensures that we can keep
     * working permanently.  It is also a lot more efficient than holding
     * a wake lock.
     * 
     * @param	action		The action to use to schedule our alarms.
     * @param	interval	The interval to set the alarms at, in ms.
     */
    void setupAlarms(PendingIntent action, long interval) {
    	alarmInterval = interval;
    	
		long time = System.currentTimeMillis();
		
		// Get the time in milliseconds of the start of the local day.
		// This is used for aligning to the configured interval.
		Calendar cal = Calendar.getInstance();
		cal.setTimeInMillis(time);
		cal.set(Calendar.MILLISECOND, 0);
		cal.set(Calendar.SECOND, 0);
		cal.set(Calendar.MINUTE, 0);
		cal.set(Calendar.HOUR_OF_DAY, 0);
		dayStart = cal.getTimeInMillis();
		long dayTime = time - dayStart;

		// Try to sleep up to the next interval boundary, so we
		// tick just about on the interval boundary.
		long offset = dayTime % alarmInterval;
		long next = time + (alarmInterval - offset);
		alarmManager.setRepeating(AlarmManager.RTC_WAKEUP,
								  next, alarmInterval, action);
		alarmSignal = action;
    }

	
    /**
     * Cancel the regular wakeup alarms.
     */
    void cancelAlarms() {
		if (alarmSignal != null) {
	        alarmManager.cancel(alarmSignal);
	        alarmSignal = null;
		}
    }


	// ******************************************************************** //
	// Client Handling.
	// ******************************************************************** //

	/**
	 * Register the given client for wakeup events.
	 * 
	 * @param	client		Client to register.
	 */
	public void register(WakeupClient client) {
		if (numClientIds >= 32)
			throw new IllegalStateException("WakeupManager: too many clients!");
		
		int id = 1 << numClientIds++;
		wakeupClients.add(client);
		client.setup(alarmCompleteHandler, id);
		
		Log.i(TAG, "WM registered " + id);
	}
	
	
	// ******************************************************************** //
	// Event Handling.
	// ******************************************************************** //
    
    /**
     * Handle the regular wakeup alarms.  Users of this class must call
     * this method when the intent registered in
     * {@link #setupAlarms(PendingIntent, long)} is fired.
     */
	void handleAlarm() {
	    // This alarm will be delivered even if the device was asleep.
	    // The snag is that the device will go back to sleep when we finish
	    // here.  So, if we need to do any asynchronous processing, we must
	    // take a wake lock for the duration of that work.  So, the below
		// processing is designed to keep track of which clients are running
		// and which are done, so that the wake lock can be released properly
		// when they are all done.

		// In 30 seconds, give up on all clients and release the wake lock.
		alarmCompleteHandler.sendEmptyMessageDelayed(0xffffffff, 30 * 1000);
		
		// Take our alarm processing wake lock.  It's essential that we
		// eventually release this.
		Log.i(TAG, "WM start: take lock");
		alarmLock.acquire();
		
		// Calculate aligned seconds in the day, as a service to our clients.
		long time = System.currentTimeMillis();
		long dayTime = time - dayStart;
		int daySec = (int) ((dayTime + 200) / 1000) % DAY_SECS;

		// Pass the alarm to our clients.
		for (WakeupClient client : wakeupClients) {
			int id = client.message;
			Log.i(TAG, "WM notify " + id);

			clientsNotified |= id;
			client.alarm(time, daySec);
		}
	}
	
	
	// Handler which we use to let alarm handlers tell us when all
	// asynchronous alarm processing is complete.
	private Handler alarmCompleteHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			int id = msg.what;
			clientsNotified &= ~id;
			if (id == 0xffffffff)
				Log.i(TAG, "WM TIMEOUT");
			else
				Log.i(TAG, "WM done " + id);

			if (clientsNotified == 0) {
				// Cancel the bailout message.
				removeMessages(0xffffffff);
				
				// Release the lock.
				alarmLock.release();
				Log.i(TAG, "WM complete: release lock");
			}
		}
	};
	

	// ******************************************************************** //
	// Class Data.
	// ******************************************************************** //

    // Debugging tag.
	private static final String TAG = "onwatch";

	// Seconds in a day.
	private static final int DAY_SECS = 24 * 3600;
	 
	// The instance of the wakeup manager; null if not created yet.
	private static WakeupManager chimerInstance = null;

	
	// ******************************************************************** //
	// Private Data.
	// ******************************************************************** //

	// Parent app we're running in.
	private Context appContext;

    // Our power manager, for wake locks.
    private PowerManager powerManager = null;
    
    // A partial wake lock used for processing alarms.
    private PowerManager.WakeLock alarmLock = null;

    // Our alarm manager.
    private AlarmManager alarmManager = null;
    
    // The PendingIntent fired by the AlarmManager to do our regular updates.
    // null if not scheduled.
    private PendingIntent alarmSignal = null;

	// The requested interval between wakeup alarms, in ms.
	private long alarmInterval = 5 * 60 * 1000;

    // List of registered clients.
    private ArrayList<WakeupClient> wakeupClients = null;
    
    // The number of client IDs allocated to date.
    private int numClientIds;
    
    // Bitmask of clients that have received the latest notification.
    private int clientsNotified;
    
    // Time in ms at the start of a local day -- note not necessarily the
    // current day, but some day.  This is used to align time intervals
    // to times of the day.
    private long dayStart = 0;

}

