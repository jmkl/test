
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
import org.hermit.onwatch.service.SoundService.Alert;
import org.hermit.onwatch.service.SoundService.Sound;

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
    // Public Types.
    // ******************************************************************** //

	/**
	 * Enum representing the state of the barometer in terms of how
	 * it's rising or falling.
	 */
	public enum ChangeState {
		NO_DATA(R.string.weather_nodata),
		STEADY(R.string.weather_steady),
		CHANGING(R.string.weather_changing),
		TURN_RISE(R.string.weather_turn_rising),
		TURN_FALL(R.string.weather_turn_falling),
		RISING(R.string.weather_rising),
		FALLING(R.string.weather_falling);
		
		ChangeState(int msg) {
			textId = msg;
		}
		
		private final int textId;
	}
	

	/**
	 * Enum representing the rate of change of the barometer.
	 */
	public enum ChangeRate {
		NO_DATA(R.string.weather_nodata),
		NIL(R.string.weather_rate_0),
		FALL_SLOW(R.string.weather_rate_slow),
		FALL_MOD(R.string.weather_rate_mod),
		FALL_FAST(R.string.weather_rate_quick,
				   new Sound(Alert.WARNING, R.string.weather_v_fall_quick)),
		FALL_VERY_FAST(R.string.weather_rate_very,
				   new Sound(Alert.ALARM, R.string.weather_v_fall_very)),
		FALL_EXTREME(R.string.weather_rate_extreme,
				   new Sound(Alert.DANGER, R.string.weather_v_fall_extreme)),
		FALL_INSANE(R.string.weather_rate_insane,
				   new Sound(Alert.TYPHOON, R.string.weather_v_fall_insane)),
		RISE_SLOW(R.string.weather_rate_slow),
		RISE_MOD(R.string.weather_rate_mod),
		RISE_FAST(R.string.weather_rate_quick),
		RISE_VERY_FAST(R.string.weather_rate_very),
		RISE_EXTREME(R.string.weather_rate_extreme),
		RISE_INSANE(R.string.weather_rate_insane);
	
		ChangeRate(int msg) {
			textId = msg;
			alertSound = null;
		}

		ChangeRate(int msg, Sound alert) {
			textId = msg;
			alertSound = alert;
		}

		final int textId;
		final Sound alertSound;
	}
	

	/**
	 * Enum representing the current barometric pressure state.
	 */
	public enum PressState {
		LOW_INSANE(R.string.weather_low_5,
				   new Sound(Alert.SPACE, R.string.weather_v_low_5)),
		LOW_TORNADO(R.string.weather_low_4,
				   new Sound(Alert.TYPHOON, R.string.weather_v_low_4)),
		LOW_HURRICANE(R.string.weather_low_3,
				   new Sound(Alert.DANGER, R.string.weather_v_low_3)),
		LOW_STORM(R.string.weather_low_2,
				   new Sound(Alert.ALARM, R.string.weather_v_low_2)),
		LOW_DEPRESSION(R.string.weather_low_1),
		NORMAL(R.string.weather_low_0),
		HIGH_MILD(R.string.weather_high_1),
		HIGH_VERY(R.string.weather_high_2,
				   new Sound(Alert.WARNING, R.string.weather_v_high_2)),
		HIGH_EXTREME(R.string.weather_high_3,
				   new Sound(Alert.WARNING, R.string.weather_v_high_3)),
		HIGH_INSANE(R.string.weather_high_4,
				   new Sound(Alert.WARNING, R.string.weather_v_high_4));

		PressState(int msg) {
			textId = msg;
			alertSound = null;
		}

		PressState(int msg, Sound alert) {
			textId = msg;
			alertSound = alert;
		}
		
		final int textId;
		final Sound alertSound;
	}
	

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
//		contentResolver.delete(WeatherSchema.Observations.CONTENT_URI, null, null);

    	obsValues = new ContentValues();
    	
    	// Set up the stored recent observations.
    	recentTimes = new long[NUM_RECENT_OBS];
    	recentPress = new double[NUM_RECENT_OBS];
    	recentCount = 0;
    	recentIndex = 0;
    	
		// Query the database for recent observations.
    	long time = System.currentTimeMillis();
    	long baseTime = time - RECENT_OBS_TIME;
    	Cursor c = null;
    	try {
    		c = contentResolver.query(WeatherSchema.Observations.CONTENT_URI,
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
    	} finally {
    		if (c != null)
    			c.close();
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
	private final class BaroListener implements SensorEventListener {
		@Override
		public void onAccuracyChanged(Sensor s, int accuracy) {
			// Nothing much to do here.
		}

		@Override
		public void onSensorChanged(SensorEvent event) {
			synchronized (WeatherService.this) {
				if (obsRequest > 0) {
					obsTotal += event.values[0];
					if (++obsCount >= obsRequest) {
						float val = obsTotal / obsCount;
						obsRequest = 0;
						obsCount = 0;
						obsTotal = 0f;
						
						// The timestamp in the event is garbage; replace it.
				        long time = System.currentTimeMillis();
						recordObservation(time, val);
					}
				}
			}
		}
		
		/**
		 * Request an observation.
		 * 
		 * @param	req			Number of values (> 0) to average together
		 * 						to make the observation.
		 */
		void requestObservation(int req) {
			obsRequest = req;
			obsCount = 0;
			obsTotal = 0f;
		}

	    // If an observation is wanted, this is the number of readings we want
	    // to take and average together.  Zero if no observation is required.
		private int obsRequest = 0;
		
		// Count and total of readings taken.
		private int obsCount = 0;
		private float obsTotal = 0f;
	}
	
	private BaroListener baroListener = new BaroListener();

	
    // ******************************************************************** //
    // Sensor Simulation.
    // ******************************************************************** //

	/**
	 * Simulator for barometer events.
	 */
	private final class SimuListener {
		/**
		 * Request an observation.
		 * 
		 * @param	req			Number of values (> 0) to average together
		 * 						to make the observation.
		 */
		void requestObservation(int req) {
			long time = System.currentTimeMillis();
			float target = SIMU_PROGRAM[simStep][0];
			float rate = SIMU_PROGRAM[simStep][1];
			
			if (prevTime == 0) {
				simPress = target;
				++simStep;
			} else {
				float hours = (time - prevTime) / 1000f / 3600f;
				float step = rate * hours;
				if (step == 0 || step > Math.abs(target - simPress)) {
					simPress = target;
					++simStep;
				} else {
					simPress += step * Math.signum(target - simPress);
				}
			}
			
			if (simStep >= SIMU_PROGRAM.length)
				simStep = 0;
			prevTime = time;
			
			synchronized (WeatherService.this) {
				recordObservation(time, simPress);
			}
		}
		
		private int simStep = 0;
		private float simPress = 1013.0f;
		private long prevTime = 0;
	}
	
	// List of { target pressure, rate }.
	private static final float[][] SIMU_PROGRAM = {
		{ 1000.0f, 0f	},
		{ 999.9f, 0.4f	},
		{ 999.7f, 0.8f	},
		{ 999.3f, 1.6f	},
		{ 970.0f, 0f	},
		{ 930.0f, 0f	},
		{ 890.0f, 0f	},
		{ 860.0f, 0f	},
		{ 830.0f, 0f	},
		{ 860.0f, 0f	},
		{ 890.0f, 0f	},
		{ 930.0f, 0f	},
		{ 970.0f, 0f	},
		{ 999.3f, 1.6f	},
		{ 999.7f, 0.8f	},
		{ 999.9f, 0.4f	},
		{ 1000.0f, 0.1f	},
	};

	private SimuListener simuListener = new SimuListener();


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
//			if (baroSensor == null) {
//				done();
//				return;
//			}
			
			// Register for barometer updates.  If we don't have a
			// barometer, fake it.
			if (baroSensor != null) {
				sensorManager.registerListener(baroListener,
						baroSensor,
						SensorManager.SENSOR_DELAY_NORMAL);

				synchronized (WeatherService.this) {
					// In 15 seconds, give up.
					msgHandler.postDelayed(finishObservation, 15 * 1000);

					baroListener.requestObservation(5);
				}
			} else {
				simuListener.requestObservation(5);
			}
		}
	};

	
	/**
	 * Record the given observation.  When we're done, the alarm manager
	 * must be notified; we will do this by invoking
	 * {@link #finishObservation}.
	 * 
	 * @param	time		Time in ms of the sensor input.
	 * @param	press		Pressure value in millibars.
	 */
	private void recordObservation(long time, double press) {
        Log.i(TAG, "Weather record " + press);
		// Create an Observation record, and add it to the database.
		obsValues.put(WeatherSchema.Observations.TIME, time);
		obsValues.put(WeatherSchema.Observations.PRESS, press);
		contentResolver.insert(WeatherSchema.Observations.CONTENT_URI, obsValues);

		// Store for trend analysis.
        Log.i(TAG, "Weather record trend #" + recentCount);
		recentTimes[recentIndex] = time;
		recentPress[recentIndex] = press;
		if (recentCount < NUM_RECENT_OBS)
			++recentCount;
		if (++recentIndex >= NUM_RECENT_OBS)
			recentIndex = 0;

		// And do the analysis.
		checkTrends(time);
		
		/**
		 * If we have alerts to sound, run them, and pass finishObservation
		 * as the completion handler.  Else just run finishObservation now.
		 */
		Sound cs = changeRate.alertSound;
		Sound ps = pressState.alertSound;
		if (cs != null || ps != null) {
			if (cs != null)
				soundService.playSound(cs, ps != null ? null : finishObservation);
			if (ps != null)
				soundService.playSound(ps, finishObservation);
		} else
			finishObservation.run();
	}

	
	/**
	 * Runnable used to close out an observation.  Main thing is to
	 * tell the alarm manager we're finished, so it can release its
	 * wake lock.
	 */
	private Runnable finishObservation = new Runnable() {
		@Override
		public void run() {
			synchronized (WeatherService.this) {
				msgHandler.removeCallbacks(this);

				alarmHandler.done();
		        if (baroSensor != null)
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
    	
		int trend = 0;
		int trendCount = 0;
		long prevTime = 0;
		double prevPress = 0;
		long turnTime = 0;
		double turnPress = 0;
		int prevTrend = 0;
		int prevCount = 0;
		double rateTot = 0;
		int rateCount = 0;
		for (int i = 0; i < recentCount; ++i) {
			int ix = recentIndex - recentCount + i;
			if (ix < 0)
				ix += NUM_RECENT_OBS;
			
			// Get the values.  If too old, ignore.
			long t = recentTimes[ix];
			double p = recentPress[ix];
			if (t < baseTime)
				continue;
			
			if (i >= recentCount - TREND_OBS && prevTime != 0) {
				double r = (p - prevPress) / (t - prevTime) * 1000d * 3600d;
				rateTot += r;
				++rateCount;
			}
			
			if (turnTime == 0) {
				turnTime = t;
				turnPress = p;
				trend = 0;
				trendCount = 0;
			} else {
				int sampleTrend = (int) Math.signum(p - turnPress);
				double sampleDelta = Math.abs(p - turnPress);

				// If we've moved by more than TREND_TOLERANCE, that's
				// a new trend.
				boolean change;
				int next;
				if (trend == 0) {
					change = sampleDelta > TREND_TOLERANCE;
					next = sampleTrend;
				} else {
					change = sampleTrend != trend;
					next = 0;
				}
				
				if (change) {
					if (trendCount >= TREND_OBS && trend != prevTrend) {
						prevTrend = trend;
						prevCount = trendCount;
					}
					turnTime = t;
					turnPress = p;
					trend = next;
					trendCount = 0;
				} else {
					++trendCount;
				}
			}

			prevTime = t;
			prevPress = p;
		}
		
		double rate = rateTot / rateCount;
		
        Log.i(TAG, "Weather analysis: " + recentCount + " records; trend=" +
        		   trend + "(" + trendCount + ") from " +
        		   prevTrend + "(" + prevCount + ")");
        Log.i(TAG, "==> rate=" + rateTot + "(" + rateCount + ")");

		if (recentCount < TREND_OBS)
			changeState = ChangeState.NO_DATA;
		else if (trendCount < TREND_OBS) {
			if (prevCount >= TREND_OBS)
				changeState = ChangeState.CHANGING;
			else
				changeState = ChangeState.STEADY;
		} else {
			if (prevCount >= TREND_OBS) {
				if (trend == 1)
					changeState = ChangeState.TURN_RISE;
				else if (trend == -1)
					changeState = ChangeState.TURN_FALL;
				else
					changeState = ChangeState.STEADY;
			} else {
				if (trend == 1)
					changeState = ChangeState.RISING;
				else if (trend == -1)
					changeState = ChangeState.FALLING;
				else
					changeState = ChangeState.STEADY;
			}
		}

		// Give a rate message, if there has been a sustained trend.
		if (trend != 0 && trendCount >= TREND_OBS && rateCount >= TREND_OBS) {
			if (rate > 10)
				changeRate = ChangeRate.RISE_INSANE;
			else if (rate > 5)
				changeRate = ChangeRate.RISE_EXTREME;
			else if (rate > 2)
				changeRate = ChangeRate.RISE_VERY_FAST;
			else if (rate > 1)
				changeRate = ChangeRate.RISE_FAST;
			else if (rate > 0.5)
				changeRate = ChangeRate.RISE_MOD;
			else if (rate > 0.0)
				changeRate = ChangeRate.RISE_SLOW;
			else if (rate < -10)
				changeRate = ChangeRate.FALL_INSANE;
			else if (rate < -5)
				changeRate = ChangeRate.FALL_EXTREME;
			else if (rate < -2)
				changeRate = ChangeRate.FALL_VERY_FAST;
			else if (rate < -1)
				changeRate = ChangeRate.FALL_FAST;
			else if (rate < -0.5)
				changeRate = ChangeRate.FALL_MOD;
			else if (rate < -0.0)
				changeRate = ChangeRate.FALL_SLOW;
			else
				changeRate = ChangeRate.NIL;
		} else
			changeRate = ChangeRate.NO_DATA;

		// Make a pressure message.
		if (prevPress < 850)
			pressState = PressState.LOW_INSANE;
		else if (prevPress < 870)
			pressState = PressState.LOW_TORNADO;
		else if (prevPress < 900)
			pressState = PressState.LOW_HURRICANE;
		else if (prevPress < 940)
			pressState = PressState.LOW_STORM;
		else if (prevPress < 980)
			pressState = PressState.LOW_DEPRESSION;
		else if (prevPress > 1080)
			pressState = PressState.HIGH_INSANE;
		else if (prevPress > 1060)
			pressState = PressState.HIGH_EXTREME;
		else if (prevPress > 1040)
			pressState = PressState.HIGH_VERY;
		else if (prevPress > 1020)
			pressState = PressState.HIGH_MILD;
		else
			pressState = PressState.NORMAL;
			
		// Produce the combined weather message.
		String msg = appContext.getString(changeState.textId);
		if (changeRate != ChangeRate.NO_DATA && changeRate != ChangeRate.NIL)
			msg += " " + appContext.getString(changeRate.textId);
		if (pressState != PressState.NORMAL)
			msg += "; " + appContext.getString(pressState.textId);
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
	private static final int NUM_RECENT_OBS = 50;
	
	// Number of contiguous observations which can indicate a trend.
	private static final int TREND_OBS = 6;

	// Time in ms over which we analyse recent observations.
	private static final int RECENT_OBS_TIME = NUM_RECENT_OBS * 5 * 60 * 1000;
	
	// Amount in mb by which pressure has to move to start a new trend.
	private static final float TREND_TOLERANCE = 1f;


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
	
	// Current barometer state; change rate; and pressure state.
	private ChangeState changeState;
	private ChangeRate changeRate;
	private PressState pressState;

	// Current weather status message.
	private String weatherMessage = null;
	
}

