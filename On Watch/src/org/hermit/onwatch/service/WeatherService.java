
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
        sensorManager = (SensorManager) appContext.getSystemService(Context.SENSOR_SERVICE);
        baroSensor = sensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE);
        Log.i(TAG, "Weather service open: " +
        		   (baroSensor != null ? "got" : "no") + " barometer");
        
        if (baroSensor != null) {
        	// Register for sensor updates.
        	sensorManager.registerListener(baroListener, baroSensor, BARO_SENSOR_DELAY);

        	obsValues = new ContentValues();
        }
	}


	void close() {
        if (baroSensor != null)
        	sensorManager.unregisterListener(baroListener);
	}


    // ******************************************************************** //
    // Sensor Handling.
    // ******************************************************************** //

	/**
	 * Listener for barometer events.
	 */
	SensorEventListener baroListener = new SensorEventListener() {
		@Override
		public void onAccuracyChanged(Sensor s, int accuracy) {
			// Nothing much to do here.
		}

		@Override
		public void onSensorChanged(SensorEvent event) {
			lastPressTime = event.timestamp;
			lastPressValue = event.values[0];
		}
	};
	

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
    	// If we have a new reading, log it.
    	if (lastPressTime > lastLogTime) {
    		// Create an Observation record, and add it to the database.
    		obsValues.put(WeatherSchema.Observations.TIME, time);
    		obsValues.put(WeatherSchema.Observations.PRESS, lastPressValue);
    		contentResolver.insert(WeatherSchema.Observations.CONTENT_URI, obsValues);
    		
    		lastLogTime = time;
    	}
    }


	// ******************************************************************** //
	// Class Data.
	// ******************************************************************** //

    // Debugging tag.
	private static final String TAG = "onwatchsvc";

	// The instance of the weather service; null if not created yet.
	private static WeatherService serviceInstance = null;
	
	// Time in seconds desired between service ticks.
	private static final int INTERVAL_SECS = 600;
	
	// Time in usec desired between sensor updates.
	private static final int BARO_SENSOR_DELAY = INTERVAL_SECS / 2 * 1000000;


	// ******************************************************************** //
	// Private Data.
	// ******************************************************************** //

	// Parent app we're running in.
	private Context appContext;

	// Our content resolver.
	private ContentResolver contentResolver;

    // Our sensor manager, and barometer sensor.  The latter is null if
    // we don't have one.
    private SensorManager sensorManager;
    private Sensor baroSensor;

	// The time and value of the last pressure reading.
	private long lastPressTime = 0;
	private float lastPressValue = 0;

	// The time of the last log entry.
	private long lastLogTime = 0;

    // Values record used for logging observations.
    private ContentValues obsValues;
	
}

