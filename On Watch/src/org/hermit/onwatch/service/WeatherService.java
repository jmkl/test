
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
import org.hermit.onwatch.provider.WeatherSchema;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
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
		
		// Create the chimer.
		soundService = SoundService.getInstance(appContext);
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

	synchronized void open() {
        // Open the barometer, if we have one.
        sensorManager = (SensorManager)
        				appContext.getSystemService(Context.SENSOR_SERVICE);
        baroSensor = sensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE);

    	obsValues = new ContentValues();
    	
    	// Set up the stored recent observations.
    	recentTimes = new long[NUM_RECENT_OBS];
    	recentPress = new double[NUM_RECENT_OBS];
    	recentCount = 0;
    	recentIndex = 0;
    	
		// Query the database for recent observations.
    	long time = System.currentTimeMillis();
    	long baseTime = time - RECENT_OBS_TIME;
    	Cursor c = contentResolver.query(WeatherSchema.Observations.CONTENT_URI,
    									 WeatherSchema.Observations.PROJECTION,
    									 WeatherSchema.Observations.TIME + ">=?",
    									 new String[] { "" + baseTime },
    									 WeatherSchema.Observations.TIME + " asc");
    	if (c.moveToFirst()) {
    		final int ti = c.getColumnIndexOrThrow(WeatherSchema.Observations.TIME);
    		final int pi = c.getColumnIndexOrThrow(WeatherSchema.Observations.PRESS);

    		// Copy the data down for later use.
    		while (!c.isAfterLast() && recentCount < NUM_RECENT_OBS) {
    			final float p = (float) c.getDouble(pi);
    			recentTimes[recentCount] = c.getLong(ti);
    			recentPress[recentCount] = p;
    			++recentCount;
    			if (++recentIndex >= NUM_RECENT_OBS)
    				recentIndex = 0;
    			c.moveToNext();
    		}
    	}
        Log.i(TAG, "Weather service opened: history " + recentCount);
    	
		checkTrends(time);
	}


	void close() {
        Log.i(TAG, "Weather service close");
        if (baroSensor != null)
        	sensorManager.unregisterListener(baroListener);
	}


	// ******************************************************************** //
	// Weather Data Access.
	// ******************************************************************** //

	/**
	 * Get the current weather message text, if any.
	 * 
	 * @return				Current weather message; null if none.
	 */
	String getWeatherMessage() {
		return weatherMessage;
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
			synchronized (WeatherService.this) {
				if (wantObservation) {
					recordObservation(event.timestamp, event.values[0]);
					wantObservation = false;
				}
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
		@Override
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
				msgHandler.postDelayed(finishObservation, 15 * 1000);
			}
		}
	};
	
	private void recordObservation(long time, double value) {
		// The timestamp in the event is garbage; replace it.
        time = System.currentTimeMillis();

        Log.i(TAG, "Weather record " + value);
		// Create an Observation record, and add it to the database.
		obsValues.put(WeatherSchema.Observations.TIME, time);
		obsValues.put(WeatherSchema.Observations.PRESS, value);
		contentResolver.insert(WeatherSchema.Observations.CONTENT_URI, obsValues);

		// Store for trend analysis.
        Log.i(TAG, "Weather record trend #" + recentCount);
		recentTimes[recentIndex] = time;
		recentPress[recentIndex] = value;
		if (recentCount < NUM_RECENT_OBS)
			++recentCount;
		if (++recentIndex >= NUM_RECENT_OBS)
			recentIndex = 0;

		// And do the analysis.
		checkTrends(time);
		
		if (value < 900)
			soundService.textAlert("Are we up a mountain or what?",
								   finishObservation);
		else
			finishObservation.run();
	}

	private Runnable finishObservation = new Runnable() {
		@Override
		public void run() {
			synchronized (WeatherService.this) {
				msgHandler.removeCallbacks(this);

				alarmHandler.done();
		    	sensorManager.unregisterListener(baroListener);
			}
		}
	};


	// ******************************************************************** //
	// Trend Analysis.
	// ******************************************************************** //

	/**
	 * Check for interesting trends in our recently logged data.  Raise
	 * alarms as appropriate.
	 * 
	 * @param	time		Current time.
	 */
	private void checkTrends(long time) {
    	long baseTime = time - RECENT_OBS_TIME;
    	long lateBaseTime = time - CURRENT_OBS_TIME;
    	
		long prevTime = 0;
		double prevPress = 0;
		int upCount = 0;
		int downCount = 0;
		int turn = 0;
		long turnTime = 0;
		double turnPress = 0;
		for (int i = 0; i < recentCount; ++i) {
			int ix = recentIndex - recentCount + i;
			if (ix < 0)
				ix += NUM_RECENT_OBS;
			
			// Get the values.  If too old, ignore.
			long t = recentTimes[ix];
			double p = recentPress[ix];
			if (t < baseTime)
				continue;
			
			if (prevTime == 0) {
				turnTime = t;
				turnPress = p;
			} else {
				if (p > prevPress) {
					++upCount;
					if (upCount > 1) {
						downCount = 0;
						if (turn == -1) {
							turn = 1;
							turnTime = prevTime;
							turnPress = prevPress;
						}
					}
				} else if (p < prevPress) {
					++downCount;
					if (downCount > 1) {
						upCount = 0;
						if (turn == 1) {
							turn = -1;
							turnTime = prevTime;
							turnPress = prevPress;
						}
					}
				}
			}
			
			prevTime = t;
			prevPress = p;
		}
		
		// Derive some results.
		double change = prevPress - turnPress;
		double rate = change / (prevTime - turnTime) * 1000d * 3600d;
		
        Log.i(TAG, "Weather analysis: " + recentCount + " records; turn=" +
        		   turn + "@" + turnPress);
        Log.i(TAG, "==> press=" + prevPress + "; up=" + upCount +
        		   "; dn=" + downCount + "; ch=" + change + "; rate=" + rate);

		int stateMsg;
		if (recentCount < 3)
			stateMsg = R.string.weather_nodata;
		else if (turn == 1 && upCount > 2)
			stateMsg = R.string.weather_turn_rising;
		else if (turn == -1 && downCount > 2)
			stateMsg = R.string.weather_turn_falling;
		else if (upCount > 2 || rate > 0.2)
			stateMsg = R.string.weather_rising;
		else if (downCount > 2 || rate < -0.2)
			stateMsg = R.string.weather_falling;
		else
			stateMsg = R.string.weather_steady;

		// Give a rate message, if there has been a sustained trend.
		int rateMsg = 0;
		if (turnTime < lateBaseTime) {
			double absRate = Math.abs(rate);
			if (absRate > 10)
				rateMsg = R.string.weather_quick_5;
			else if (absRate > 5)
				rateMsg = R.string.weather_quick_4;
			else if (absRate > 2)
				rateMsg = R.string.weather_quick_3;
			else if (absRate > 1)
				rateMsg = R.string.weather_quick_2;
			else if (absRate > 0.5)
				rateMsg = R.string.weather_quick_1;
		}

		// Make a pressure message.
		int pressMsg = 0;
		if (prevPress < 850)
			pressMsg = R.string.weather_low_5;
		else if (prevPress < 870)
			pressMsg = R.string.weather_low_4;
		else if (prevPress < 900)
			pressMsg = R.string.weather_low_3;
		else if (prevPress < 940)
			pressMsg = R.string.weather_low_2;
		else if (prevPress < 980)
			pressMsg = R.string.weather_low_1;
		else if (prevPress > 1080)
			pressMsg = R.string.weather_high_4;
		else if (prevPress > 1060)
			pressMsg = R.string.weather_high_3;
		else if (prevPress > 1040)
			pressMsg = R.string.weather_high_2;
		else if (prevPress > 1020)
			pressMsg = R.string.weather_high_1;
		
		// Produce the combined weather message.
		String msg = appContext.getString(stateMsg);
		if (rateMsg != 0)
			msg += " " + appContext.getString(rateMsg);
		if (pressMsg != 0)
			msg += "; " + appContext.getString(pressMsg);
		weatherMessage = msg;
	}
	
	
	// ******************************************************************** //
	// Class Data.
	// ******************************************************************** //

    // Debugging tag.
	private static final String TAG = "onwatch";

	// The instance of the weather service; null if not created yet.
	private static WeatherService serviceInstance = null;
	
	// Maximum number of recent observations to use for trend analysis.
	private static final int NUM_RECENT_OBS = 60;
	
	// Time in ms over which we analyse recent observations.
	private static final int RECENT_OBS_TIME = NUM_RECENT_OBS * 60 * 1000;
	
	// Time in ms which is considered very recent, for rate analysis.
	private static final int CURRENT_OBS_TIME = 30 * 60 * 1000;


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

	// Our sound service.
	private SoundService soundService = null;

    // Values record used for logging observations.
    private ContentValues obsValues;

    // Flag true if an observation is wanted; if so, we need to call
    // done() on our alarm handler.
	private boolean wantObservation = false;
	
	// Handler for messages.
	private Handler msgHandler = null;
	
	// Times and values of our last half hours' worth of readings,
	// for trend analysis.  These arrays operate as a cyclic buffer,
	// with recentCount being the number of valid entries, and recentIndex
	// being the index where the next entry will be written (the oldest entry
	// if we have wrapped).
	private long[] recentTimes = null;
	private double[] recentPress = null;
	private int recentCount = 0;
	private int recentIndex = 0;
	
	// Current weather status message.
	private String weatherMessage = null;
	
}

