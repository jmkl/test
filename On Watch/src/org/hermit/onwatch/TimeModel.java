
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

import java.text.DateFormatSymbols;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.TimeZone;

import android.app.AlarmManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;


/**
 * This class implements a watch clock.  It provides similar functionality
 * to java.util.Calendar, in terms of being able to access the components of
 * the current date/time -- hour, minute, month, etc.  However, it adds
 * fields specific to watchkeeping -- the watch and bell number.  See
 * {@link Field} and {@link get get()} below.
 */
public class TimeModel
{

    // ******************************************************************** //
    // Public Types and Constants.
    // ******************************************************************** //

	/**
	 * This enumeration defines one of the traditional watches.
	 */
	public enum Watch {
		FIRST(R.string.watch_20, 20, 4),
		MIDDLE(R.string.watch_00, 0, 4),
		MORNING(R.string.watch_04, 4, 4),
		FORENOON(R.string.watch_08, 8, 4),
		AFTERNOON(R.string.watch_12, 12, 4),
		DOG1(R.string.watch_16, 16, 2),
		DOG2(R.string.watch_18, 18, 2);

		Watch(int nId, int start, int len) {
			nameId = nId;
			startHour = start;
			endHour = start + len - 1;
			numBells = len * 2;
		}
		
		/**
		 * Get the watch for a given hour.
		 */
		public static Watch forHour(int hour) {
			for (Watch w : values())
				if (hour >= w.startHour && hour <= w.endHour)
					return w;
			return null;
		}
		
		
		/**
		 * Get the watch for a given Watch ordinal value.
		 */
		public static Watch forOrdinal(int ord) {
			return WATCH_NAMES[ord];
		}
		
		
		/**
		 * Resource ID of the watch name.
		 */
		public final int nameId;
		
		/**
		 * Starting hour of this watch.
		 */
		public final int startHour;
		
		/**
		 * Last hour in this watch (inclusive).
		 */
		public final int endHour;
		
		/**
		 * Number of bells in this watch.
		 */
		public final int numBells;
	    
	    /**
	     * The string name of the watch.
	     */
		public String name;
		
		// Flag whether we've set up the watch names yet.
		private static boolean watchNamesSet = false;
	    
	    // Array of all the Watch values; for converting an ordinal to
	    // a Watch.
	    private static final Watch[] WATCH_NAMES = Watch.values();
	}


	/**
	 * This enumeration describes a field of the date / time managed
	 * by this clock.
	 */
	// Each field is tagged with its corresponding Calendar field, if any.
	public enum Field {
	    /** The current year number. */
		YEAR(Calendar.YEAR),
		
        /** Month, 0=January. */
		MONTH(Calendar.MONTH),
		
        /** Day number in the month. */
		DAY(Calendar.DAY_OF_MONTH),
		
        /** Day of the week, 1=Sun .. 7=Sat. */
		WDAY(Calendar.DAY_OF_WEEK),
		
        /** Watch number; ordinal of the Watch enum value. */
		WATCH(0),
		
        /** Hour of the day, 24-hour. */
		HOUR(Calendar.HOUR_OF_DAY),
		
        /** The number of minutes into the current watch. */
		WATCHMIN(0),
		
        /**
         * The number of bells to sound at the *start* of the
         * present half-hour, with proper bells for dog watches.
         */
		BELLS(0),
		
        /**
         * Exactly the same as BELL, but changes to zero bells (4 in the
         * last dog watch) one minute into a watch.  This can be used
         * to display the number of bells currently ringing.
         */
		CHIMING(0),
		
        /** The current minute. */
		MINUTE(Calendar.MINUTE),
		
        /** The current second. */
		SECOND(Calendar.SECOND);

		Field(int cal) {
			calField = cal;
		}

		private final int calField;
	}

	
	/**
	 * This listener class is used to receive notifications about time
	 * changes.
	 */
	public static interface Listener {
	    /**
	     * This method is called when the time changes.  It is called
	     * for each field on which this listener is registered that has changed.
	     * 
	     * <p>Note that a change in a high-order field is assumed to affect
	     * all lower-order fields; so a change in the hour will cause the
	     * seconds to be considered changed, even if the numeric value is
	     * unchanged.
	     * 
	     * @param  field       The field that has changed.
	     * @param  value       The new field value.
	     * @param  time        The new time in ms.
	     */
		public void change(Field field, int value, long time);
	}
	
	
	// ******************************************************************** //
    // Constructors.
    // ******************************************************************** //
	
	/**
	 * Create a watch clock.
	 * 
	 * @param	context			Parent application.
	 * @param	locationModel	App's location model.
	 */
	private TimeModel(Context context) {
		appContext = context;

		// Set up the watch names, if not done yet.
		if (!Watch.watchNamesSet) {
			for (Watch w : Watch.values())
				w.name = appContext.getString(w.nameId);
			Watch.watchNamesSet = true;
		}
		
		// Allocate our stored time, and zero it to force an update when
		// we get the real time.
		int nf = Field.values().length;
		timeFields = new int[nf];
		timeChanged = new boolean[nf];
		for (int i = 0; i < nf; ++i) {
			timeFields[i] = 0;
			timeChanged[i] = true;
		}
		
		// Set up the listeners array.
		setupListenersArray(nf);
		
        // Register for the intents that tell us about timezone changes.
        IntentFilter tzFilter = new IntentFilter();
        tzFilter.addAction(Intent.ACTION_TIMEZONE_CHANGED);
        appContext.registerReceiver(tzListener, tzFilter);

		// Get our location model.  Ask it to keep us up to date.
		LocationModel locationModel = LocationModel.getInstance(context);
		locationModel.listenTimezone(new LocationModel.ZoneListener() {
			@Override
			public void tzChange(int zone) {
				if (nauticalTimezone != zone) {
					nauticalTimezone = zone;
					if (nauticalTime)
						setupTimezone();
				}
			}
		});
	
        // Set up the time zone.
		setupTimezone();
		
		// Get our initial time.
		getTime(System.currentTimeMillis());
	}

	
	/*
	 * A wee bit ugly...
	 */
	@SuppressWarnings("unchecked")
	private void setupListenersArray(int nf) {
		timeListeners = (ArrayList<Listener>[]) new ArrayList<?>[nf];
	}
	
	
	/**
	 * Get the time model, creating it if it doesn't exist.
	 * 
	 * @param	context      Parent application.
	 * @return               The model instance.
	 */
	public static TimeModel getInstance(Context context) {
		if (modelInstance == null)
			modelInstance = new TimeModel(context);
		
		return modelInstance;
	}
	

	// ******************************************************************** //
	// Listeners.
	// ******************************************************************** //
	
	/**
	 * Listen for time changes, specifically changes in a specified field
	 * of the time.  The listener will be invoked whenever that field
	 * changes.
	 * 
	 * A listener may be assigned to multiple fields; bear in mind, though,
	 * that it will be invoked once for each field that has changed.
	 * 
	 * @param	field			The field to listen for changes to.
	 * @param	listener		The Listener to call when that field changes.
	 */
	public void listen(Field field, Listener listener) {
		int o = field.ordinal();
		if (timeListeners[o] == null)
			timeListeners[o] = new ArrayList<Listener>();
		
		timeListeners[o].add(listener);
	}


	// ******************************************************************** //
	// Run Control.
	// ******************************************************************** //
	
	/**
	 * Start the clock running.
	 */
	void resume() {
	}
	

	/**
	 * Stop the clock.
	 */
	void pause() {
	}


	/**
	 * Regular tick event, for housekeeping.  Occurs every second, as
	 * close as possible to the 1-second boundary.
	 * 
	 * @param	time			Current system time in millis.
	 */
	public void tick(long time) {
    	getTime(time + timeOffset);
	}


	// ******************************************************************** //
	// Configuration.
	// ******************************************************************** //

	/**
	 * Set whether we use nautical time.
	 * 
	 * @param	naut			true to use nautical time, based on longitude;
	 * 							false to use the system timezone.
	 */
	public void setNauticalTime(boolean naut) {
		Log.i("TimeModel", "Set " + (naut ? "nautical" : "landlubber") + " time");
    	nauticalTime = naut;
    	if (nauticalTime)
    		setupTimezone();
    	
    	// If not nautical time, a real timezone update is coming.
    }


	/**
	 * The BroadcastReceiver receives the intent
	 * Intent.ACTION_TIMEZONE_CHANGED, as set up in our
	 * constructor.  This tells us when the system time zone changes.
	 */
	private BroadcastReceiver tzListener = new BroadcastReceiver() {
		@Override
		public void onReceive(Context c, Intent i) {
			final String action = i.getAction();

			if (action.equals(Intent.ACTION_TIMEZONE_CHANGED)) {
				// If we're following nautical time, then we set the timezone
				// ourselves.
				Log.i("TimeModel", "TZ change: in " + (nauticalTime ? "nautical" : "landlubber") + " time");
				if (!nauticalTime)
					setupTimezone();
			}
		}
	};


	/**
	 * Set up our time zone and basic calendar.
	 */
	private void setupTimezone() {
		if (nauticalTime) {
			Log.i("TimeModel", "setupTimezone: in nautical zone " + nauticalTimezone);
			if (nauticalTimezone == LocationModel.TZ_UNKNOWN)
				return;
			TimeZone z = findRawZone(nauticalTimezone * 3600000);
			if (z == null)
				return;
			if (timeZone != z) {
				timeZone = z;
				Log.i("TimeModel", "Set nautical timezone: " + timeZone.getID());
	    		AlarmManager alarm = (AlarmManager)
	    					appContext.getSystemService(Context.ALARM_SERVICE);
	    		alarm.setTimeZone(timeZone.getID());
			}
		} else {
			timeZone = TimeZone.getDefault();
			Log.i("TimeModel", "setupTimezone: set civil timezone: " + timeZone.getID());
		}
		
		watchCal = Calendar.getInstance(timeZone);
		
		// Changing timezone invalidates everything.
		int nf = Field.values().length;
		for (int i = 0; i < nf; ++i)
			timeChanged[i] = true;
	}
	
	
	private TimeZone findRawZone(int offset) {
		String[] zones = TimeZone.getAvailableIDs(offset);
		for (String zone : zones)
			if (zone.startsWith("GMT") || zone.startsWith("Etc/GMT") ||
					zone.startsWith("UTC") || zone.startsWith("Etc/UTC"))
				return TimeZone.getTimeZone(zone);
		
		return null;
	}
	

	// ******************************************************************** //
	// Utility Methods.
	// ******************************************************************** //
   
    /**
     * Get the name of the given day of the week.
     * 
     * @param	wday			The day of the week number, as returned by
     * 							get(Field.WDAY).
     * @return					The name of the specified day of the week.
     */
    public static String weekdayName(int wday) {
    	return DAY_NAMES[wday];
    }
    
    
    /**
     * Get the short name of the given day of the week.
     * 
     * @param	wday			The day of the week number, as returned by
     * 							get(Field.WDAY).
     * @return					The short name of the specified day of the week.
     */
    public static String weekdayShortName(int wday) {
    	return DAY_ABBREVS[wday];
    }
    
    
    /**
     * Get the name of the given month.
     * 
     * @param	month			The month number, as returned by
     * 							get(Field.MONTH).
     * @return					The name of the specified month.
     */
    public static String monthName(int month) {
    	return MONTH_NAMES[month];
    }
    
 
    /**
     * Determine whether a specified date/time field changed in the most
     * recent update.
     * 
     * @param	field			Which field to get.
     * @return					True if the field changed.
     */
    public boolean changed(Field field) {
    	return timeChanged[field.ordinal()];
    }
    

    /**
     * Get the value of a specified date/time field.
     * 
     * @param	field			Which field to get.
     * @return					The field's current value.
     */
    public int get(Field field) {
    	return timeFields[field.ordinal()];
    }
    

    /**
     * Get the name of the value of a specified date/time field.
     * 
     * @param	field			Which field to get.
     * @return					The field's current value in textual form;
     * 							for example, day or month name,
     */
    public String getName(Field field) {
    	int val = timeFields[field.ordinal()];
    	if (field == Field.WDAY)
    		return DAY_NAMES[val];
    	if (field == Field.MONTH)
    		return MONTH_NAMES[val];
    	if (field == Field.WATCH)
    		return Watch.forOrdinal(val).name;
    	return String.valueOf(val);
    }
    
    
    /**
     * Get the watch for the current time of this watch clock.
     * 
     * @return					The current watch.
     */
    public Watch getWatch() {
    	return Watch.forOrdinal(timeFields[Field.WATCH.ordinal()]);
    }
    
    
    /**
     * Get the name of the watch for the current time of this watch clock.
     * 
     * @return					The name of the current watch.
     */
    public String getWatchName() {
    	return Watch.forOrdinal(timeFields[Field.WATCH.ordinal()]).name;
    }
    

    /**
     * Get the current timezone's offset from UTC for the current date.
     * 
     * @return					The time in milliseconds to add to UTC
     * 							to get local time.
     */
    public int getTimezoneOffset() {
    	return timeZone.getOffset(milliTime);
    }
    

    /**
     * Determine whether we are currently following nautical time.
     * 
     * @return					True if we are following nautical time.
     */
    public boolean isNauticalTime() {
    	return nauticalTime;
    }
    

	// ******************************************************************** //
	// Time Management.
	// ******************************************************************** //
    
    /**
     * Get the current time and set up our local time fields.
     * 
	 * @param	time			Current system time in millis.
     */
    private void getTime(long time) {
    	milliTime = time;
    	watchCal.setTimeInMillis(time);
	
    	// Get all the fields.  Mark whether they've changed.  A change
    	// in a high-order field is assumed to affect all lower-order
    	// fields.
    	boolean change = false;
    	for (Field f : Field.values()) {
    		if (f.calField == 0)
    			continue;
    		
    		int i = f.ordinal();
    		int val = watchCal.get(f.calField);
    		
    		timeChanged[i] = change || val != timeFields[i];
    		change = timeChanged[i];
    		timeFields[i] = val;
    	}
    	
    	// Get the derived fields.
    	int hour = timeFields[Field.HOUR.ordinal()];
    	int min = timeFields[Field.MINUTE.ordinal()];
    	
    	// Watch number.  We add one from the second dog watch on.
    	int windex = Field.WATCH.ordinal();
    	Watch watch = Watch.forHour(hour);
    	int wnum = watch.ordinal();
		timeChanged[windex] = wnum != timeFields[windex];
    	timeFields[windex] = wnum;
    	
    	// Minutes in the watch.
    	int mindex = Field.WATCHMIN.ordinal();
    	int wmin = (hour - watch.startHour) * 60 + min;
		timeChanged[mindex] = wmin != timeFields[mindex];
    	timeFields[mindex] = wmin;
   	
    	// We set BELLS to the bells at the *start* of this half hour - 1 to 8.
    	// Special for the dog watches -- first dog watch has 8 bells at the
    	// end, second goes 5, 6, 7, 8.
    	int bindex = Field.BELLS.ordinal();
    	int bell = (hour * 2 + min / 30) % 8;
    	if (bell == 0 || (hour == 18 && bell == 4))
    		bell = 8;
		timeChanged[bindex] = bell != timeFields[bindex];
    	timeFields[bindex] = bell;
    	
    	// Now update CHIMING if we're a minute into the watch.
    	int cindex = Field.CHIMING.ordinal();
    	int cbell = bell;
    	if (cbell == 8 && min != 0 && min != 30)
    		cbell = watch == Watch.DOG2 ? 4 : 0;
    	timeChanged[cindex] = cbell != timeFields[cindex];
    	timeFields[cindex] = cbell;
    	
    	// Now invoke all the listeners for changed time fields.
    	for (Field f : Field.values()) {
    		int i = f.ordinal();
    		if (timeChanged[i]) {
        		int val = timeFields[i];
    			ArrayList<Listener> lists = timeListeners[i];
    			if (lists != null)
    				for (Listener list : lists)
    					list.change(f, val, milliTime);
    		}
    	}
    }

	
	// ******************************************************************** //
	// Debug Control.
	// ******************************************************************** //

	/**
	 * Fast-forward the clock by bumping up the time offset.  Used
	 * for debugging.
	 * 
	 * @param	step			1 = go to next minute minus 2 secs;
	 * 							2 = go to next half-hour minus 2 secs;
	 * 							3 = go to next even hour minus 2 secs.
	 */
	void adjust(int step) {
		// Round to how many ms?
		long round = step == 1 ? 60 : step == 2 ? 30 * 60 : 2 * 3600;
		round *= 1000;
		
		long rem = milliTime % round;
		timeOffset += round - rem - 3000;
	}


	/**
	 * Reset the time offset.  Used to cancel debugging.
	 */
	void adjustReset() {
		timeOffset = 0;
	}


	// ******************************************************************** //
	// Class Data.
	// ******************************************************************** //

    // Month and day names.
    private static final DateFormatSymbols DATE_SYMBOLS = new DateFormatSymbols();
    private static final String[] DAY_NAMES = DATE_SYMBOLS.getWeekdays();
    private static final String[] DAY_ABBREVS = DATE_SYMBOLS.getShortWeekdays();
    private static final String[] MONTH_NAMES = DATE_SYMBOLS.getShortMonths();

	// The instance of the time model; null if not created yet.
	private static TimeModel modelInstance = null;


	// ******************************************************************** //
	// Private Data.
	// ******************************************************************** //

	// Parent app we're running in.
	private Context appContext;

	// True to use nautical time; false for the system timezone.
	private boolean nauticalTime = false;
	
	// Current nautical timezone, as hours offset from UTC.  Only valid when
	// we have a location; otherwise set to LocationModel.TZ_UNKNOWN.
	private int nauticalTimezone = LocationModel.TZ_UNKNOWN;
	
	// Time zone we're configured for.
	private TimeZone timeZone;

	// Current time in millis.
	private long milliTime;
	
	// Calendar used for time conversions.
	private Calendar watchCal;
	
	// Our time fields, copied from the calendar.  Flags for each field
	// whether they changed in the last update.
	private int timeFields[];
	private boolean timeChanged[];
	
	// Listeners registered for changes in specific fields of the time.
	// Each entry in the array relates to a time field, and is the list
	// of listeners registered for changes in that field.
	private ArrayList<Listener>[] timeListeners = null;

	// Debug: offset in millis to add to the correct time.  Used for
    // "fast-forwarding" the clock.
	private long timeOffset = 0;
	
}

