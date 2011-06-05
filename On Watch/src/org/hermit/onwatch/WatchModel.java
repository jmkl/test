
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


import java.util.ArrayList;

import org.hermit.astro.Instant;
import org.hermit.onwatch.TimeModel.Field;
import org.hermit.onwatch.provider.VesselSchema;

import android.app.LoaderManager;
import android.content.ContentValues;
import android.content.CursorLoader;
import android.content.Loader;
import android.database.Cursor;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.style.StyleSpan;
import android.util.Log;


/**
 * This class implements a GPS view.  It displays the current location info
 * and GPS state.
 */
public class WatchModel
{

	// ******************************************************************** //
    // Public Classes.
    // ******************************************************************** //
	
	/**
	 * Class representing a crew member.
	 */
	public static final class Crew {
		public Crew(String n, int col, int pos) {
			name = n;
			colour = col;
			position = pos;
		}
		
		public ContentValues getValues() {
			ContentValues values = new ContentValues();
			values.put(VesselSchema.Crew.NAME, name);
			values.put(VesselSchema.Crew.COLOUR, colour);
			values.put(VesselSchema.Crew.POSITION, position);
			return values;
		}
		
		public int getColour() {
			return CREW_COLS[colour % CREW_COLS.length];
		}

		/**
		 * The name of this crew member.
		 */
		public final String name;
		
		/**
		 * The colour assigned to this person.  Doesn't change as their
		 * watch position changes.
		 */
		public final int colour;
		
		/**
		 * The position of this person in the watch order.
		 */
		public int position;
	}
	
	
	/**
	 * This listener class is used to receive notifications about watch
	 * changes.
	 */
	public static interface Listener {
		public void watchPlanChanged();
		public void watchAlert(Crew[] nextCrew);
		public void watchChange(int day, int watch, Crew[] crew);
	}
	

	// ******************************************************************** //
    // Constructors.
    // ******************************************************************** //
	
	/**
	 * Create a crew model.  Private since there is only one instance.
	 * 
	 * @param	context			Parent application.
	 */
	private WatchModel(OnWatch context) {
		Log.i(TAG, "WPM constructor");
		
		appContext = context;
		
		// Set up the client list.
		watchListeners = new ArrayList<Listener>();

		// Get our time model.  Ask it to ping us each bell.
		timeModel = TimeModel.getInstance(context);
		timeModel.listen(TimeModel.Field.MINUTE, new TimeModel.Listener() {
			@Override
			public void change(Field field, int value, long time) {
				recalcWatch();
			}
		});
		
		// Get and cache the crew list out of the database.
	    // Find and cache the index of the next open watch slot.
		// reOrderCrew() does both of these.
		crewList = new ArrayList<Crew>();
    	
        // Prepare the data loader.  Either re-connect with an existing one,
        // or start a new one.
		appContext.getLoaderManager().initLoader(
						LOADER_PLAN, null, planLoaderCallbacks);
		appContext.getLoaderManager().initLoader(
						LOADER_LIST, null, listLoaderCallbacks);
	}


	/**
	 * Get the crew model, creating it if it doesn't exist.
	 * 
	 * @param	context			Parent application.
	 */
	public static WatchModel getInstance(OnWatch context) {
		if (modelInstance == null)
			modelInstance = new WatchModel(context);
		
		return modelInstance;
	}
	

	// ******************************************************************** //
	// Data Management.
	// ******************************************************************** //

    /**
     * Loader callbacks, to monitor changes in the list data.
     */
    private final LoaderManager.LoaderCallbacks<Cursor> planLoaderCallbacks =
    						new LoaderManager.LoaderCallbacks<Cursor>() {

    	@Override
		public Loader<Cursor> onCreateLoader(int id, Bundle args) {
    		Log.i(TAG, "WPM plan onCreateLoader()");
    		
    		// Now create and return a CursorLoader that will take care of
    		// creating a Cursor for the data being displayed.
    		return new CursorLoader(appContext,
    								VesselSchema.Vessels.CONTENT_URI,
    								VESSEL_SUMMARY_PROJ,
    								null, null,
    								VesselSchema.Vessels.NAME + " asc");
    	}

    	@Override
		public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
    		Log.i(TAG, "WPM plan onLoadFinished(): " + data.getCount() + " records");
    		
    		if (data.moveToFirst()) {
    			// Get the vessel's configured watch plan.
    			String pname = data.getString(COL_V_WATCHES);
    			try {
    				watchPlan = WatchPlan.valueOf(pname);
    			} catch (Exception e) {
    				watchPlan = WatchPlan.FOUR_HOUR_D;
    			}
    		}
    		
    		crewChanged();
    	}

    	@Override
		public void onLoaderReset(Loader<Cursor> loader) {
    		Log.i(TAG, "WPM plan onLoaderReset()");
    		
    		// Reset to default.
			watchPlan = WatchPlan.FOUR_HOUR_D;
    		
			crewChanged();
    	}

    };


    /**
     * Loader callbacks, to monitor changes in the list data.
     */
    private final LoaderManager.LoaderCallbacks<Cursor> listLoaderCallbacks =
    						new LoaderManager.LoaderCallbacks<Cursor>() {

    	@Override
		public Loader<Cursor> onCreateLoader(int id, Bundle args) {
    		Log.i(TAG, "WPM list onCreateLoader()");
    		
    		// Now create and return a CursorLoader that will take care of
    		// creating a Cursor for the data being displayed.
    		return new CursorLoader(appContext,
    								VesselSchema.Crew.CONTENT_URI,
    								CREW_SUMMARY_PROJ,
    								null, null,
    								VesselSchema.Crew.POSITION + " asc");
    	}

    	@Override
		public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
    		Log.i(TAG, "WPM list onLoadFinished(): " + data.getCount() + " records");

    		// Load a cached copy of the crew list.
    		crewList.clear();
    		for (data.moveToFirst(); !data.isAfterLast(); data.moveToNext()) {
    			String name = data.getString(COL_C_NAME);
    			int col = data.getInt(COL_C_COLOUR);
    			int pos = data.getInt(COL_C_POSITION);
    			
    			// Add this person to the crew list.
    			crewList.add(new Crew(name, col, pos));
    		}
    		
    		crewChanged();
    	}

    	@Override
		public void onLoaderReset(Loader<Cursor> loader) {
    		Log.i(TAG, "WPM list onLoaderReset()");
    		
    		// Reset to default.
    		crewList.clear();
    		
    		crewChanged();
    	}

    };


	// ******************************************************************** //
    // Registered Listeners.
    // ******************************************************************** //
	
	/**
	 * Register a Handler to be called when the location or location state
	 * changes.
	 */
	void listen(Listener handler) {
		watchListeners.add(handler);
	}
	

    // ******************************************************************** //
    // Watch Calculation.
    // ******************************************************************** //
	
	/**
	 * The crew list has changed; notify all who need to know.
	 */
	private void crewChanged() {
		Log.i(TAG, "WPM crewChanged()");
		
		// Clear out the current watch info, as it's not valid now.
		currentDay = -1;
		currentWatch = -1;
		nextWatchDay = -1;
		nextWatch = -1;
		currentWatchCrew = null;
		nextWatchCrew = null;

		// Update the current watch data.
		recalcWatch();
		
		// Notify the observers that we changed.
		for (Listener listener : watchListeners)
			listener.watchPlanChanged();
	}


	/**
	 * Update the crew model for the current bell.
	 * 
	 * @return				true if the on-watch crew has changed.
	 */
	private void recalcWatch() {
		if (watchPlan == null || crewList == null || crewList.isEmpty()) {
			Log.i(TAG, "WPM recalcWatch(): no data");
			return;
		}
		
		Log.i(TAG, "WPM recalcWatch()");
		
		// Get the passage day number.
		int day = getPassageDay();
		
		// Get the current time.
		float hour = timeModel.get(TimeModel.Field.HOUR);
		int min = timeModel.get(TimeModel.Field.MINUTE);
		hour += (float) min / 60.0;
		
		// Get the current watch number.  Note this is the watch number
		// of the configured watch plan, not the traditional watch tracked
		// by the time model.
		float[] times = watchPlan.planTimes;
		int wpd = times.length;
		int watch = 0;
		for (int i = 0; i < wpd; ++i) {
			if (times[i] > hour)
				break;
			watch = i;
		}
		
		currentWatchMins = (int) (hour * 60f - times[watch] * 60f);
		
		// If we're approaching a crew change, tell everyone.
		if (currentWatchLen - currentWatchMins <= 15 && alertedWatch != currentWatch) {
			alertedWatch = currentWatch;
			for (Listener listener : watchListeners)
				listener.watchAlert(nextWatchCrew);
		}
			
		// If the day or watch hasn't changed, there's nothing else to do.
		if (day == currentDay && watch == currentWatch)
			return;
		
		Log.i(TAG, "WPM recalcWatch(): watch changed");
		
		currentDay = day;
		currentWatch = watch;
		nextWatchDay = day;
		nextWatch = watch + 1;
		if (nextWatch >= wpd) {
			nextWatch = 0;
			++nextWatchDay;
		}
		currentWatchCrew = calculateWatchCrew(day, watch);
		nextWatchCrew = calculateWatchCrew(nextWatchDay, nextWatch);
		float next = watch < wpd - 1 ? times[watch + 1] : times[0] + 24f;
		currentWatchLen = (int) (next * 60f - times[watch] * 60f);

		// Notify all the registered clients that we have an update.
		for (Listener listener : watchListeners)
			listener.watchChange(currentDay, currentWatch, currentWatchCrew);
	}
	

	/**
	 * Return the currently on-watch crew.
	 * 
	 * @return				An array listing the on-watch crew.  null if
	 * 						there are no crew.
	 */
	private Crew[] calculateWatchCrew(int day, int watch) {
		// Get the number of watches per day.
		int wpd = watchPlan.planTimes.length;
		int onwatch = watchPlan.planPly;

		// Now get the number of watches we're offset from the basic
		// schedule start.
		int numCrew = crewList.size();
		watch += (day * wpd) % numCrew;

		// Finally make up the list for this day.
		Crew[] list = new Crew[onwatch];
		for (int p = 0; p < onwatch; ++p)
			list[p] = crewList.get((watch + numCrew - p) % numCrew);

		return list;
	}
	

	// ******************************************************************** //
    // Watch Plan Access.
    // ******************************************************************** //

	/**
	 * Return the start times of all the watches in a day.
	 * 
	 * @return				An array listing the start times of each watch.
	 * 						Each entry is decimal hours; e.g. 16.5
	 * 						for 16:30.
	 */
	public float[] getWatchTimes() {
		return watchPlan == null ? null : watchPlan.planTimes;
	}
	

	/**
	 * Get the watch plan the schedule is based on.
	 * 
	 * @return				The watch plan.
	 */
	public WatchPlan getWatchPlan() {
		return watchPlan;
	}


	/**
	 * Return the watch schedule for a given number of days of a passage,
	 * starting at the current day.
	 * 
	 * @param	numDays		The number of days of schedule we want.
	 * @return				An array of the watch positions (primary,
	 * 						secondary, etc.), each member being an array of
	 * 						the crew assigned to each time in that position.
	 * 						Combine this with getWatchTimes() to get the
	 * 						schedule.  Returns null if there are no crew.
	 */
	public Crew[][] getWatchSchedule(int numDays) {
		if (watchPlan == null || crewList == null || crewList.isEmpty()) {
			Log.i(TAG, "WPM getWatchSchedule(): no data");
			return null;
		}

		int firstDay = getPassageDay();
		
		// Get the watch times, and the number of watches per day.
		float[] watchTimes = getWatchTimes();
		int wpd = watchTimes.length;
		int onwatch = watchPlan.planPly;
		
		// Now get the number of watches we're offset from the basic
		// schedule start.
		int numCrew = crewList.size();
		int offset = (firstDay * wpd) % numCrew;
		
		// Finally make up the list for this day.
		Crew[][] list = new Crew[onwatch][wpd * numDays];
		for (int p = 0; p < onwatch; ++p)
			for (int i = 0; i < wpd * numDays; ++i)
				list[p][i] = crewList.get((i + offset - p + numCrew) % numCrew);

		return list;
	}
	

	/**
	 * Get the day number of the current passage.
	 * 
	 * @return				The day number of the current passage.
	 * 						0 = first day.  -1 if we're not in a passage.
	 */
	public int getPassageDay() {
		// Get the julian day number.
		long time = System.currentTimeMillis();
		int today = (int) Instant.javaToJulian(time);
		return today - passageStart;
	}


	/**
	 * Return how far we are through the current watch.
	 * 
	 * @return				How far through the current watch we are,
	 * 						as a fraction of the watch.
	 */
	public float getWatchFrac() {
		return (float) currentWatchMins / (float) currentWatchLen;
	}
	

	/**
	 * Get the time remaining to the next change of watch.
	 * 
	 * @return				The time remaining to the next change of
	 * 						watch, in minutes.
	 */
	public int getTimeToNext() {
		return currentWatchLen - currentWatchMins;
	}
	
	
	/**
	 * Return the currently on-watch crew.
	 * 
	 * @return				An array listing the on-watch crew.
	 */
	public Crew[] getWatchCrew() {
		return currentWatchCrew;
	}
	

	/**
	 * Return the names of the currently on-watch crew.
	 * 
	 * @return				The names of the on-watch crew, formatted for
	 * 						display with the primary crew in bold.
	 */
	public SpannableStringBuilder getWatchCrewNames() {
		return formatCrewNames(currentWatchCrew, "");
	}
	
	
	/**
	 * Return the next on-watch crew.
	 * 
	 * @return				An array listing the on-watch crew for the next
	 * 						watch.
	 */
	public Crew[] getNextCrew() {
		return nextWatchCrew;
	}
	

	/**
	 * Return the names of the next on-watch crew.
	 * 
	 * @return				The names of the next on-watch crew, formatted for
	 * 						display with the primary crew in bold.
	 */
	public SpannableStringBuilder getNextCrewNames() {
		return formatCrewNames(nextWatchCrew, "⇒");
	}
	

	/**
	 * Create a String representation of a list of crew.
	 * 
	 * @param	crew		List of crew to be displayed.
	 * @param	pref		Prefix for the output.
	 * @return				The names of the crew, formatted for
	 * 						display with the primary crew in bold.
	 */
	private SpannableStringBuilder formatCrewNames(Crew[] crew, String pref) {
		if (crew == null || crew.length == 0)
			return new SpannableStringBuilder("");

		SpannableStringBuilder res = new SpannableStringBuilder();
		res.append(pref);
		res.append(crew[0].name);
		res.setSpan(new StyleSpan(Typeface.BOLD),
					pref.length(), res.length(),
					SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE);
		for (int i = 1; i < crew.length; ++i)
			res.append(", "  + crew[i].name);
		
		return res;
	}
	

	// ******************************************************************** //
	// Private Constants.
	// ******************************************************************** //

    // Debugging tag.
	private static final String TAG = "onwatch";
	 
	
	// ******************************************************************** //
	// Class Data.
	// ******************************************************************** //

	// The instance of the crew model; null if not created yet.
	private static WatchModel modelInstance = null;

    // Colours to draw the crew.
	private static final int[] CREW_COLS = {
		0xffff5558, // Pale red
		0xffaee00b, // Lime
		0xff756eff, // Blue
		0xffffc000, // Yellow
		0xfff77deb, // Violet
		0xfff79c78, // Pink
		0xff56e04c, // Green
		0xff7be0de, // Cyan
	};
	
	
	// These are the vessel columns that we will display.
	private static final String[] VESSEL_SUMMARY_PROJ = new String[] {
    	VesselSchema.Vessels.WATCHES,
    };

    // The indices of the columns in the projection.
    private static final int COL_V_WATCHES = 0;

    // These are the crew columns that we will display.
	private static final String[] CREW_SUMMARY_PROJ = new String[] {
		VesselSchema.Crew._ID,
    	VesselSchema.Crew.NAME,
    	VesselSchema.Crew.COLOUR,
    	VesselSchema.Crew.POSITION,
    };

    // The indices of the columns in the projection.
    private static final int COL_C_NAME = 1;
    private static final int COL_C_COLOUR = 2;
    private static final int COL_C_POSITION = 3;

     // Loader IDs.
    private static final int LOADER_PLAN = 1;
    private static final int LOADER_LIST = 2;

	
	// ******************************************************************** //
	// Private Data.
	// ******************************************************************** //

	// Parent app we're running in.
	private OnWatch appContext;

	// Watch calendar, which does all our date/time calculations.
	private TimeModel timeModel;

	// List of listeners registered to be called when the location or
	// location state changes.
	private ArrayList<Listener> watchListeners = null;

    // The watch plan which the schedule is based on, cached from the database.
    private WatchPlan watchPlan = WatchPlan.FOUR_HOUR_D;
    
    // The crew names, cached from the database.
    private ArrayList<Crew> crewList = null;

    // The Julian day number on which the current passage started.
    private int passageStart = 7;

	// The current day of the passage, and current watch number.  Note
    // this is the watch number of the configured watch plan, not the
	// traditional watch tracked by the time model.
	private int currentDay = -1;
	private int currentWatch = -1;
	
	// The next watch day and watch number.
	private int nextWatchDay = -1;
	private int nextWatch = -1;
	
	// The current watch length in minutes.
	private int currentWatchLen = 0;
	
	// How far we are into the current watch in minutes.
	private int currentWatchMins = 0;

	// The current and next on-watch crew.
	private Crew[] currentWatchCrew = null;
	private Crew[] nextWatchCrew = null;

	// ID of the watch for which we have issued a crew change alert.
	private int alertedWatch = -1;

}

