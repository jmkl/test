
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


import org.hermit.onwatch.provider.WeatherSchema;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.util.Log;


/**
 * This class manages the passage data.
 */
public class WeatherService
{

	// ******************************************************************** //
    // Constructor.
    // ******************************************************************** //

	/**
	 * Create a WeatherService.  As a singleton we have a private
	 * constructor.
	 * 
	 * @param	context        Parent application.
	 */
	private WeatherService(Context context) {
        appContext = context;
		contentResolver = appContext.getContentResolver();
		
		// Get a handler for messages.
		msgHandler = new Handler();

		// Get the wakeup manager for handling async processing.
		wakeupManager = WakeupManager.getInstance(appContext);
		wakeupManager.register(alarmHandler);
	}
	
	
	/**
	 * Get the weather service instance, creating it if it doesn't exist.
	 * 
	 * @param	context        Parent application.
	 * @return                 The weather service instance.
	 */
	public static WeatherService getInstance(Context context) {
		if (serviceInstance == null)
			serviceInstance = new WeatherService(context);
		
		return serviceInstance;
	}
	

	// ******************************************************************** //
	// Run Control.
	// ******************************************************************** //

	void open() {
        // Open the barometer, if we have one.
        sensorManager = (SensorManager)
        				appContext.getSystemService(Context.SENSOR_SERVICE);
        baroSensor = sensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE);

    	obsValues = new ContentValues();
	}


	void close() {
        Log.i(TAG, "Weather service close");
        if (baroSensor != null)
        	sensorManager.unregisterListener(baroListener);
	}


    // ******************************************************************** //
    // Sensor Handling.
    // ******************************************************************** //

	/**
	 * Listener for barometer events.
	 */
	private SensorEventListener baroListener = new SensorEventListener() {
		@Override
		public void onAccuracyChanged(Sensor s, int accuracy) {
			// Nothing much to do here.
		}

		@Override
		public void onSensorChanged(SensorEvent event) {
	        Log.i(TAG, "Weather " + event.values[0]);
			synchronized (WeatherService.this) {
				if (wantObservation)
					recordObservation(event.timestamp, event.values[0]);
			}
		}
	};
	

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
			// If there's no barometer, there's nothing we can do.
			if (baroSensor == null){
				done();
				return;
			}
			
			// Register for barometer updates.
        	sensorManager.registerListener(baroListener,
					   					   baroSensor,
					   					   SensorManager.SENSOR_DELAY_NORMAL);
        	
			synchronized (WeatherService.this) {
				wantObservation = true;
				
				// In 15 seconds, give up.
				msgHandler.postDelayed(cancelObservation, 15 * 1000);
			}
		}
	};
	
	private void recordObservation(long time, double value) {
		// The timestamp in the event is garage; replace it.
        time = System.currentTimeMillis();
        
		// Create an Observation record, and add it to the database.
		obsValues.put(WeatherSchema.Observations.TIME, time);
		obsValues.put(WeatherSchema.Observations.PRESS, value);
		contentResolver.insert(WeatherSchema.Observations.CONTENT_URI, obsValues);

		alarmHandler.done();
		wantObservation = false;
		msgHandler.removeCallbacks(cancelObservation);
		
    	sensorManager.unregisterListener(baroListener);
	}

	private Runnable cancelObservation = new Runnable() {
		public void run() {
			synchronized (WeatherService.this) {
		        Log.i(TAG, "Weather CANCEL");
				alarmHandler.done();
				wantObservation = false;
				
		    	sensorManager.unregisterListener(baroListener);
			}
		}
	};


	// ******************************************************************** //
	// Class Data.
	// ******************************************************************** //

    // Debugging tag.
	private static final String TAG = "onwatch";

	// The instance of the weather service; null if not created yet.
	private static WeatherService serviceInstance = null;


	// ******************************************************************** //
	// Private Data.
	// ******************************************************************** //

	// Parent app we're running in.
	private Context appContext;

	// Our content resolver.
	private ContentResolver contentResolver;
    
    // Our wakeup manager, used for alarm processing.
    private WakeupManager wakeupManager = null;

    // Our sensor manager, and barometer sensor.  The latter is null if
    // we don't have one.
    private SensorManager sensorManager;
    private Sensor baroSensor;

    // Values record used for logging observations.
    private ContentValues obsValues;

    // Flag true if an observation is wanted; if so, we need to call
    // done() on our alarm handler.
	private boolean wantObservation = false;
	
	// Handler for messages.
	private Handler msgHandler = null;

}

