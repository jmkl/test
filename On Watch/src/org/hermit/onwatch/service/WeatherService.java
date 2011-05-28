
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
	 * @param	ticker         Ticker instance to use for time management.
	 */
	private WeatherService(Context context, Ticker ticker) {
        appContext = context;
        timeTicker = ticker;
		contentResolver = appContext.getContentResolver();
	}
	
	
	/**
	 * Get the weather service instance, creating it if it doesn't exist.
	 * 
	 * @param	context        Parent application.
	 * @param	ticker         Ticker instance to use for time management.
	 * @return                 The weather service instance.
	 */
	public static WeatherService getInstance(Context context, Ticker ticker) {
		if (serviceInstance == null)
			serviceInstance = new WeatherService(context, ticker);
		
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
        	// Ask the ticker to ping us.
        	timeTicker.listen(INTERVAL_SECS, tickHandler);

        	// Register for sensor updates.
        	sensorManager.registerListener(baroListener, baroSensor, BARO_SENSOR_DELAY);

        	obsValues = new ContentValues();
        }
	}

	
	private Ticker.Listener tickHandler = new Ticker.Listener() {
		@Override
		public void tick(long time, int daySecs) {
			logWeather(time);
		}
	};
	

	void close() {
        if (baroSensor != null)
        	sensorManager.unregisterListener(baroListener);
        
        timeTicker.unlisten(tickHandler);
	}


    // ******************************************************************** //
    // Track Management.
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
	
	
    /**
     * Add the current weather to the log.
     * 
     * @param   time        Current time in ms.
     */
    private void logWeather(long time) {
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

    // The ticker we use for timing.
	private Ticker timeTicker;

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

