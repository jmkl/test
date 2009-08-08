
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

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.graphics.Typeface;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.style.StyleSpan;
import android.util.Log;


/**
 * This class implements a GPS view.  It displays the current location info
 * and GPS state.
 */
public class CrewModel
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
			values.put(CREW_NAME, name);
			values.put(CREW_COLOUR, colour);
			values.put(CREW_POS, position);
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
	private CrewModel(Context context) {
		appContext = context;
		
		// Set up the client list.
		watchListeners = new ArrayList<Listener>();

		// Get our time model.  Ask it to ping us each bell.
		timeModel = TimeModel.getInstance(context);
		timeModel.listen(TimeModel.Field.MINUTE, new TimeModel.Listener() {
			@Override
			public void change(Field field, long time, int value) {
				updateWatch();
			}
		});

		// Open the database.
		databaseHelper = new DbHelper(appContext);
	}

	
	/**
	 * Get the crew model, creating it if it doesn't exist.
	 * 
	 * @param	context			Parent application.
	 */
	public static CrewModel getInstance(Context context) {
		if (modelInstance == null)
			modelInstance = new CrewModel(context);
		
		return modelInstance;
	}
	

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
    // Run Control.
    // ******************************************************************** //

	/**
	 * Open the database.
	 */
	void open() {
		// Calling getWritableDatabase() gets the helper to open (and maybe
		// create / update) the DB.
		database = databaseHelper.getWritableDatabase();
		
		// Get and cache the watch parameters out of the database.
		String[] fields = new String[] { WATCH_ID, WATCH_PLAN };
		Cursor c = database.query(WATCH_TABLE, fields, null, null,
								  null, null, null);
		if (c.moveToFirst()) {
			// Get that person's position in the watch order.
			int pind = c.getColumnIndexOrThrow(WATCH_PLAN);
			String pname = c.getString(pind);
			try {
				watchPlan = WatchPlan.valueOf(pname);
			} catch (IllegalArgumentException e) {
				watchPlan = WatchPlan.FOUR_HOUR_D;
			}
		}
		c.close();
		
		// Get and cache the crew list out of the database.
	    // Find and cache the index of the next open watch slot.
		// reOrderCrew() does both of these.
		crewList = new ArrayList<Crew>();
		nextFreeSlot = reOrderCrew();
		
		// Update the watch crew.
		updateWatch();
	}
	

	/**
	 * Close the database.
	 */
	void close() {
		databaseHelper.close();
		database = null;
	}


	// ******************************************************************** //
    // Crew List Management.
    // ******************************************************************** //

	/**
	 * Get a Cursor on the list of crew names.
	 * 
	 * @return				A Cursor over the crew list, ordered by watch
	 * 						order.
	 */
	Cursor getCrewCursor() {
		if (database == null)
			return null;
		return database.query(CREW_TABLE,
							  new String[] { CREW_ID, CREW_NAME, CREW_COLOUR, CREW_POS },
							  null, null, null, null, CREW_POS + " ASC");
	}


	/**
	 * Add the given crew name to the model.
	 * 
	 * @param	name		Crew name to add to the database.
	 * @return				The row ID of the newly inserted row,
	 * 						or -1 if an error occurred.
	 */
	public long addName(String name) {
		// Create a record for this person.
		// TODO: get a free colour.
		int col = nextFreeSlot;
		int pos = nextFreeSlot++;
		Crew crew = new Crew(name, col, pos);
		
		// Add the new crew record to the database.  Set their watch
		// position to the next available watch slot.
		ContentValues values = crew.getValues();
		long id = database.insert(CREW_TABLE, CREW_NAME, values);
		
		// Add to the cached crew list too.
		if (id > 0)
			crewList.add(crew);
		
		// Notify the observers that we changed.
		crewChanged();
				
		return id;
	}


    /**
     * Move a specified crew member within the crew list.
     * 
     * @param	id			The database ID of the person to move.
     * @param	offset		The number of places to move the person down
     * 						the list.  Negative to move up.  Currently must
     * 						be -1 or 1.
     */
	public void moveCrew(long id, int offset) {
		if (offset == 0)
			return;
		if (offset < -1 || offset > 1)
			throw new IllegalArgumentException("CrewModel.moveCrew can only move by 1");
		
		// Find the person we're moving.
		String[] fields = new String[] { CREW_ID, CREW_POS };
		Cursor c = database.query(CREW_TABLE, fields, CREW_ID + "=" + id, null,
								  null, null, CREW_POS + " ASC");
		if (!c.moveToFirst())
			throw new IllegalArgumentException("Crew id " + id + " is not in database.");
		
		// Get that person's position in the watch order.
		int pind = c.getColumnIndexOrThrow(CREW_POS);
		int cpos = c.getInt(pind);
		c.close();
		
		// Now get the position we want to move to.  If it's off the end
		// of the list, forget it.
		int npos = cpos + offset;
		if (npos < 0 || npos >= nextFreeSlot)
			return;
		
		// Since we're only moving by 1, it's a straight swap.  First
		// move the other person to this position.
		ContentValues values = new ContentValues();
		values.put(CREW_POS, cpos);
		database.update(CREW_TABLE, values, CREW_POS + "=" + npos, null);
		
		// Now move the first person to that position.
		values.put(CREW_POS, npos);
		database.update(CREW_TABLE, values, CREW_ID + "=" + id, null);
		
		// Adjust the cache too.
		Crew crew = crewList.remove(cpos);
		crew.position = npos;
		crewList.add(npos, crew);
		crew = crewList.get(cpos);
		crew.position = cpos;
		
		// Notify the observers that we changed.
		crewChanged();
    }


    /**
     * Delete a specified crew member from the model.
     * 
     * @param	id			The database ID of the person to delete.
     * @return				The number of rows affected.
     */
	public int deleteCrew(long id) {
    	int stat = database.delete(CREW_TABLE, CREW_ID + "=" + id, null);
    	
    	// Move the remaining crew up in the watch order.  Re-set the
    	// cached crew list.
    	nextFreeSlot = reOrderCrew();
		
		// Notify the observers that we changed.
		crewChanged();

    	return stat;
    }


	/**
	 * Re-set the watch order for the crew, filling in any blanks in the
	 * order by moving the following crew up.  Cache the crew list in
	 * crewList.
	 * 
	 * @return				The first watch order position (counting
	 * 						from 0) which is not assigned to a crew member.
	 */
	private int reOrderCrew() {
		// Clear the crew list.
		crewList.clear();
		
		// Set up the crew query.
		String[] fields = new String[] { CREW_ID, CREW_NAME, CREW_COLOUR, CREW_POS };
		Cursor c = database.query(CREW_TABLE, fields, null, null,
								  null, null, CREW_POS + " ASC");
		
		ContentValues values = new ContentValues();
		int i;
		for (i = 0, c.moveToFirst(); !c.isAfterLast(); ++i, c.moveToNext()) {
			int iind = c.getColumnIndexOrThrow(CREW_ID);
			int id = c.getInt(iind);
			int nind = c.getColumnIndexOrThrow(CREW_NAME);
			String name = c.getString(nind);
			int cind = c.getColumnIndexOrThrow(CREW_COLOUR);
			int col = c.getInt(cind);
			int pind = c.getColumnIndexOrThrow(CREW_POS);
			int pos = c.getInt(pind);
			if (pos != i) {
				values.put(CREW_POS, i);
				database.update(CREW_TABLE, values, CREW_ID + "=" + id, null);
			}
			
			// Add this person to the crew list.
			crewList.add(new Crew(name, col, pos));
		}
		c.close();
		
		// Return the next available watch slot.
		return i;
	}


	// ******************************************************************** //
    // Watch Plan Management.
    // ******************************************************************** //

	/**
	 * Return the start times of all the watches in a day.
	 * 
	 * @return				An array listing the start times of each watch.
	 * 						Each entry is decimal hours; e.g. 16.5
	 * 						for 16:30.
	 */
	public float[] getWatchTimes() {
		return watchPlan.planTimes;
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
	 * Set the watch plan to base the schedule on.
	 * 
	 * @param	plan		The new watch plan to use.
	 */
	public void setWatchPlan(WatchPlan plan) {
		watchPlan = plan;
		
		// Update the watch configuration record.
		ContentValues values = new ContentValues();
		values.put(WATCH_PLAN, watchPlan.toString());
		database.update(WATCH_TABLE, values, null, null);
		
		crewChanged();
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
		int firstDay = getPassageDay();
		
		// Get the watch times, and the number of watches per day.
		float[] watchTimes = getWatchTimes();
		int wpd = watchTimes.length;
		int onwatch = watchPlan.planPly;
		
		// Now get the number of watches we're offset from the basic
		// schedule start.
		int numCrew = crewList.size();
		if (numCrew == 0)
			return null;
		int offset = (firstDay * wpd) % numCrew;
		
		// Finally make up the list for this day.
		Crew[][] list = new Crew[onwatch][wpd * numDays];
		for (int p = 0; p < onwatch; ++p)
			for (int i = 0; i < wpd * numDays; ++i)
				list[p][i] = crewList.get((i + offset - p + numCrew) % numCrew);

		return list;
	}
	
	
	/**
	 * The crew list has changed; notify all who need to know.
	 */
	private void crewChanged() {
		// Clear out the current watch info, as it's not valid now.
		currentDay = -1;
		currentWatch = -1;
		nextWatchDay = -1;
		nextWatch = -1;
		currentWatchCrew = null;
		nextWatchCrew = null;

		// Notify the observers that we changed.
		for (Listener listener : watchListeners)
			listener.watchPlanChanged();
		
		// Update the current watch data.
		updateWatch();
	}


    // ******************************************************************** //
    // Watch Management.
    // ******************************************************************** //
	
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
    // Watch Tracking.
    // ******************************************************************** //
	
	/**
	 * Update the crew model for the current bell.  See if the on-watch
	 * crew has changed, and tell people if so.
	 */
	private void updateWatch() {
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
			
		// If the day or watch has changed, tell everyone.
		if (day != currentDay || watch != currentWatch) {
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
		if (numCrew == 0)
			return null;
		watch += (day * wpd) % numCrew;

		// Finally make up the list for this day.
		Crew[] list = new Crew[onwatch];
		for (int p = 0; p < onwatch; ++p)
			list[p] = crewList.get((watch + numCrew - p) % numCrew);

		return list;
	}
	

    // ******************************************************************** //
    // Private Classes.
    // ******************************************************************** //

	/**
     * This class helps open, create, and upgrade the crew database.
     */
    private static final class DbHelper extends SQLiteOpenHelper {
    	DbHelper(Context context) {
            super(context, DB_NAME, null, DB_VER);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            Log.i(TAG, "CrewModel: create tables");

    		String create = "CREATE TABLE " + WATCH_TABLE + " (";
    		create += WATCH_ID + " INTEGER PRIMARY KEY";
    		create += "," + WATCH_PLAN + " TEXT";
    		create += ");";
    		db.execSQL(create);

    		create = "CREATE TABLE " + CREW_TABLE + " (";
    		create += CREW_ID + " INTEGER PRIMARY KEY";
    		create += "," + CREW_NAME + " TEXT";
    		create += "," + CREW_COLOUR + " INTEGER";
    		create += "," + CREW_POS + " INTEGER";
    		create += ");";
    		db.execSQL(create);
    		
			// Add a default watch configuration to the watch table.
			ContentValues values = new ContentValues();
			values.put(WATCH_PLAN, WatchPlan.FOUR_HOUR_D.toString());
			db.insert(WATCH_TABLE, WATCH_PLAN, values);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldV, int newV) {
            Log.i(TAG, "CrewModel: upgrade tables");
            
            // We can simply dump all the old data.
        	db.execSQL("DROP TABLE IF EXISTS " + WATCH_TABLE);
        	db.execSQL("DROP TABLE IF EXISTS " + CREW_TABLE);
        	onCreate(db);
        }

        @Override
		public void onOpen(SQLiteDatabase db) {
            Log.i(TAG, "CrewModel: database opened");
        }
    }
    

	// ******************************************************************** //
	// Private Constants.
	// ******************************************************************** //

    // Debugging tag.
	@SuppressWarnings("unused")
	private static final String TAG = "onwatch";
	 
	// Database for crew data.  Bump the version number to nuke the data
	// and start over.
	private static final String DB_NAME = "CrewData";
	private static final int DB_VER = 7;

	// Table of the watch settings.
	private static final String WATCH_TABLE = "watch_config";
	private static final String WATCH_ID = "_id";
	private static final String WATCH_PLAN = "plan";

	// Table of the crew's names.
	private static final String CREW_TABLE = "crew_names";
	private static final String CREW_ID = "_id";
	private static final String CREW_NAME = "name";
	private static final String CREW_COLOUR = "colour";
	private static final String CREW_POS = "watch_pos";

	
	// ******************************************************************** //
	// Class Data.
	// ******************************************************************** //

	// The instance of the crew model; null if not created yet.
	private static CrewModel modelInstance = null;

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

	
	// ******************************************************************** //
	// Private Data.
	// ******************************************************************** //

	// Parent app we're running in.
	private Context appContext;

	// Watch calendar, which does all our date/time calculations.
	private TimeModel timeModel;

	// Database open/close helper.
	private DbHelper databaseHelper = null;

    // The database we use for storing our data.  Null if not open.
    private SQLiteDatabase database = null;

	// List of listeners registered to be called when the location or
	// location state changes.
	private ArrayList<Listener> watchListeners = null;

    // The next free position in the watch order.
    private int nextFreeSlot = -1;

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

