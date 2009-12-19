
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


import org.hermit.astro.Instant;
import org.hermit.geo.Distance;
import org.hermit.geo.Position;
import org.hermit.onwatch.LocationModel.GpsState;
import org.hermit.onwatch.TimeModel.Field;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;


/**
 * Model of the passage.  This class embodies all data about the current
 * passage.  The data is persisted in a database.
 */
public class PassageModel
{

	// ******************************************************************** //
    // Public Classes.
    // ******************************************************************** //

    /**
     * Record of data on a particular passage.  This class encapsulates
     * all the data on a passage, and provides methods to convert
     * to and from database records.
     */
	static final class PassageData {
		PassageData(String name, String from, String to, Position dest) {
			this.name = name;
			this.start = from;
			this.dest = to;
			this.startPos = null;
			this.destPos = dest;
			this.distance = Distance.ZERO;
		}
		
		private PassageData() {
		}
		
		static PassageData fromCursor(Cursor c) {
			PassageData pd = new PassageData();
			int i;
			
			i = c.getColumnIndexOrThrow(PassField.PASS_ID.name);
			pd.id = c.getLong(i);
			i = c.getColumnIndexOrThrow(PassField.PASS_NAME.name);
			pd.name = c.getString(i);
			i = c.getColumnIndexOrThrow(PassField.PASS_START_NAME.name);
			pd.start = c.getString(i);
			i = c.getColumnIndexOrThrow(PassField.PASS_START_TIME.name);
			pd.startTime = c.isNull(i) ? 0 : c.getLong(i);
			i = c.getColumnIndexOrThrow(PassField.PASS_DEST_NAME.name);
			pd.dest = c.getString(i);
			i = c.getColumnIndexOrThrow(PassField.PASS_DEST_TIME.name);
			pd.destTime = c.isNull(i) ? 0 : c.getLong(i);
			i = c.getColumnIndexOrThrow(PassField.PASS_DISTANCE.name);
			if (c.isNull(i))
				pd.distance = Distance.ZERO;
			else
				pd.distance = new Distance(c.getInt(i));
			
			// Get the start pos if present.
			int islat = c.getColumnIndexOrThrow(PassField.PASS_START_LAT.name);
			int islon = c.getColumnIndexOrThrow(PassField.PASS_START_LON.name);
			if (c.isNull(islat) || c.isNull(islon))
				pd.startPos = null;
			else
				pd.startPos = Position.fromDegrees(c.getDouble(islat),
												   c.getDouble(islon));
			
			// Get the end pos if present.
			int ielat = c.getColumnIndexOrThrow(PassField.PASS_DEST_LAT.name);
			int ielon = c.getColumnIndexOrThrow(PassField.PASS_DEST_LON.name);
			if (c.isNull(ielat) || c.isNull(ielon))
				pd.destPos = null;
			else
				pd.destPos = Position.fromDegrees(c.getDouble(ielat),
												 c.getDouble(ielon));
			
			// If we have start or end times, work out the day numbers.
			if (pd.startTime == 0)
				pd.startDay = 0;
			else
				pd.startDay = (int) (Instant.javaToJulian(pd.startTime) + 0.5);
			
			return pd;
		}

		void toContentValues(ContentValues values) {
			// Write all the fields we have except the ID: that must be
			// handled separately.
			values.put(PassField.PASS_NAME.name, name);
			values.put(PassField.PASS_START_NAME.name, start);
			if (startTime != 0)
				values.put(PassField.PASS_START_TIME.name, startTime);
			if (startPos != null) {
				values.put(PassField.PASS_START_LAT.name, startPos.getLatDegs());
				values.put(PassField.PASS_START_LON.name, startPos.getLonDegs());
			}
			values.put(PassField.PASS_DEST_NAME.name, dest);
			if (destTime != 0)
				values.put(PassField.PASS_DEST_TIME.name, destTime);
			if (destPos != null) {
				values.put(PassField.PASS_DEST_LAT.name, destPos.getLatDegs());
				values.put(PassField.PASS_DEST_LON.name, destPos.getLonDegs());
			}
			values.put(PassField.PASS_DISTANCE.name, distance.getMetres());
		}
		
		boolean isStarted() {
			return startTime != 0;
		}
		
		boolean isRunning() {
			return startTime != 0 && destTime == 0;
		}
		
		boolean isFinished() {
			return destTime != 0;
		}
		
		// The passage ID, name, from point name, and to name.
		private long id = -1;
		String name = null;
		String start = null;
		String dest = null;

	    // The Java time and the Julian day number on which the current passage
	    // started and finished.  0 if not set.
	    long startTime = 0;
	    int startDay = 0;
	    long destTime = 0;
	    
	    // The start and end points of this passage.  Null if not set.
	    Position startPos = null;
	    Position destPos = null;
	    
	    // Distance in metres covered on this passage (to date).
	    Distance distance = Distance.ZERO;
	    
	    // Number of points recorded for this passage.  (Not saved in DB.)
	    int numPoints = 0;
	    
	    // Most recently recorded position.  (Not saved in DB.)
	    Position lastPos = null;
	}
	

	// ******************************************************************** //
    // Constructors.
    // ******************************************************************** //

	/**
	 * Create a crew model.  Private since there is only one instance.
	 * 
	 * @param	context			Parent application.
	 */
	private PassageModel(Context context) {
		appContext = context;
		
		// Get the time model.  Ask it to ping us each bell.
		timeModel = TimeModel.getInstance(context);
		timeModel.listen(TimeModel.Field.MINUTE, new TimeModel.Listener() {
			@Override
			public void change(Field field, int value, long time) {
				updateWatch();
			}
		});

		// Get our location model.  Ask it to keep us up to date.
		locationModel = LocationModel.getInstance(context);
		locationModel.listen(new LocationModel.Listener() {
			@Override
			public void posChange(GpsState state, String stateMsg,
								  Position pos, String locMsg) {
				// Add the point to the points log, if we're in a passage.
				if (passageData != null && passageData.isRunning()) {
					long time = System.currentTimeMillis();
					logPoint(pos, "", time);
				}
			}
		});

		// Open the database.
		databaseHelper = new DbHelper(appContext);
	}

	
	/**
	 * Get the passage model, creating it if it doesn't exist.
	 * 
	 * @param	context        Parent application.
	 * @return                 The global passage model.
	 */
	public static PassageModel getInstance(Context context) {
		if (modelInstance == null)
			modelInstance = new PassageModel(context);
		
		return modelInstance;
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

		// Get and cache the global parameters out of the database.
		long currentPassage = 0;
		String[] fields = new String[] { DATA_ID, DATA_CURRENT_PASS };
		Cursor c = database.query(DATA_TABLE, fields, null, null,
								  null, null, null);
		if (c.moveToFirst()) {
			// Get the current passage ID.
			int pind = c.getColumnIndexOrThrow(DATA_CURRENT_PASS);
			currentPassage = c.getLong(pind);
		}
		c.close();
		
		// Now if we have a passage, get its data.
		if (currentPassage != 0)
			passageData = loadPassage(currentPassage);
		else
			passageData = null;

		// TODO: Update the something.
//		updateWatch();
	}

	
	/**
	 * Close the database.
	 */
	void close() {
		databaseHelper.close();
		database = null;
	}


	// ******************************************************************** //
    // Passage Data Management.
    // ******************************************************************** //

	/**
	 * Get a Cursor on the list of passages.
	 * 
	 * @return				A Cursor over the passage list, ordered by date.
	 */
	Cursor getPassageCursor() {
		if (database == null)
			return null;
		return database.query(PASS_TABLE,
							  PassField.fields,
							  null, null, null, null,
							  PassField.PASS_START_TIME.name + " ASC");
	}


    /**
     * Load and return the indicated passage.
     * 
     * @param	id			ID of the passage to load.
     * @return              The passage data for the passage with the
     *                      given ID.
     */
    public PassageData loadPassage(long id) {
		// Get the data for the given passage.
		Cursor c = database.query(PASS_TABLE,
				  				  PassField.fields,
				  				  PassField.PASS_ID.name + "=" + id,
				  				  null, null, null, null);
		if (!c.moveToFirst())
			throw new IllegalArgumentException("Loading invalid passage ID " + id);
		PassageData pd = PassageData.fromCursor(c);
		c.close();
		
		// Get the number of recorded points, and the most recent position.
		c = database.query(POINT_TABLE, PointField.fields,
						   PointField.POINT_PASSAGE_ID.name + "=" + pd.id,
						   null, null, null,
						   PointField.POINT_TIME.name + " DESC");
		pd.numPoints = c.getCount();
		if (c.moveToFirst()) {
			int latInd = c.getColumnIndexOrThrow(PointField.POINT_LAT.name);
			int lonInd = c.getColumnIndexOrThrow(PointField.POINT_LON.name);
			double lat = c.getDouble(latInd);
			double lon = c.getDouble(lonInd);
			pd.lastPos = Position.fromDegrees(lat, lon);
		} else
			pd.lastPos = null;
		c.close();

		return pd;
    }


    /**
     * Load and return the newest passage in the database.
     * 
     * @return              The passage data for the most recent passage.
     */
    public PassageData loadNewestPassage() {
    	// Query for the newest passage.
    	Cursor c = database.query(PASS_TABLE,
    							  PassField.fields,
    							  null, null, null, null,
    							  PassField.PASS_START_TIME.name + " DESC", "1");
    	PassageData pd = null;
    	if (c.moveToFirst())
    		pd = PassageData.fromCursor(c);
    	c.close();

    	return pd;
    }


	/**
	 * Save the given passage in the model.  If it's an existing passage,
	 * update it in the database, otherwise create it.
	 * 
	 * @param	pd			The passage data to save.
	 * @return				The row ID of the newly inserted or updated
	 * 						passage, or -1 if an error occurred.
	 */
	public long savePassage(PassageData pd) {
		ContentValues values = new ContentValues();
		pd.toContentValues(values);
		
		// If this is a new passage, add it to the database, else update
		// the existing database record.
		if (pd.id < 0) {
			Log.i(TAG, "Passage: New: " + pd.name);
			pd.id = database.insert(PASS_TABLE, PassField.PASS_NAME.name, values);
		} else {
			Log.i(TAG, "Passage: Update: " + pd.name);
			database.update(PASS_TABLE, values,
							PassField.PASS_ID.name + "=" + pd.id,
							null);
		}
		
		return pd.id;
	}
	

	/**
	 * Delete the given passage from the model.
	 * 
	 * @param	pd			The passage data to delete.
	 */
	public void deletePassage(PassageData pd) {
		// Delete all points belonging to the passage.
    	database.delete(POINT_TABLE,
    					PointField.POINT_PASSAGE_ID.name + "=" + pd.id,
    					null);
	
		// Nuke the passage record.
    	database.delete(PASS_TABLE,
    					PassField.PASS_ID.name + "=" + pd.id,
    					null);
    	
    	// If that was the current passage, then we have no current passage.
    	if (passageData.id == pd.id)
    		passageData = null;
	}


	/**
	 * Select the given passage to be the current passage.  The passage
	 * data for that passage is returned.
	 * 
	 * @param  id           The ID of the desired passage.
	 * @return				The passage data for the selected passage.
	 */
	public PassageData selectPassage(long id) {
		// Get the data for the given passage.
		passageData = loadPassage(id);

		// Update the configuration record.
		ContentValues values = new ContentValues();
		values.put(DATA_CURRENT_PASS, id);
		database.update(DATA_TABLE, values, null, null);
		
		return passageData;
	}


	/**
	 * Start (or restart) the current passage.  Does nothing if there
	 * is no current passage, or if it is already started.
	 */
	public void startPassage() {
		if (passageData == null || passageData.isRunning())
			return;

		Position pos = locationModel.getCurrentPos();
		long time = System.currentTimeMillis();

		passageData.startPos = pos;
		passageData.startTime = time;
		passageData.startDay = (int) (Instant.javaToJulian(time) + 0.5);
		passageData.destPos = null;
		passageData.destTime = 0;
		passageData.distance = Distance.ZERO;

		// Add the starting point to the points log.  This will update
		// the database record for this passage.
		logPoint(pos, passageData.start, time);

		// Notify the observers that we changed.
		// crewChanged();
	}


	/**
	 * Finish the current passage.  Does nothing if there is no current
	 * passage, or if it is not started or already finished.
	 */
	public void finishPassage() {
		if (passageData == null || !passageData.isRunning())
			return;

		Position pos = locationModel.getCurrentPos();
		long time = System.currentTimeMillis();

		// Finalize the passage data.
		passageData.destPos = pos;
		passageData.destTime = time;

		// Add the ending point to the points log.  This will update
		// the database record for this passage.
		logPoint(pos, passageData.dest, time);
	}


	// ******************************************************************** //
	// Track Management.
	// ******************************************************************** //

	/**
	 * Add the given point to the track.
	 * 
	 * @param	pos				The point to log.
	 */
	private void logPoint(Position pos, String name, long time) {
		// Get the distance from the previous point.  Add this to the passage.
		Distance dist = Distance.ZERO;
		if (passageData.lastPos != null && pos != null) {
			dist = pos.distance(passageData.lastPos);
			passageData.distance = passageData.distance.add(dist);
		}
		passageData.lastPos = pos;
		++passageData.numPoints;
		Log.i(TAG, "Passage point: dist=" + dist.formatM() +
					" tot=" + passageData.distance.formatM());
		
		// Create a Point record, and add it to the database.
		ContentValues values = new ContentValues();
		values.put(PointField.POINT_PASSAGE_ID.name, passageData.id);
		values.put(PointField.POINT_NAME.name, name);
		values.put(PointField.POINT_TIME.name, time);
		if (pos != null) {
		    values.put(PointField.POINT_LAT.name, pos.getLatDegs());
		    values.put(PointField.POINT_LON.name, pos.getLonDegs());
		}
		values.put(PointField.POINT_DIST.name, dist.getMetres());
		values.put(PointField.POINT_TOT_DIST.name, passageData.distance.getMetres());
		database.insert(POINT_TABLE, PointField.POINT_NAME.name, values);
		
		// Update the database record for this passage.
		values.clear();
		passageData.toContentValues(values);
		database.update(PASS_TABLE, values,
						PassField.PASS_ID.name + "=" + passageData.id,
						null);
	}
	

    // ******************************************************************** //
    // Passage Data Access.
    // ******************************************************************** //

	/**
	 * Get the name of the current passage.
	 * 
	 * @return				The current passage name.  Null if we're not
	 * 						in a passage.
	 */
	public String getPassageName() {
		if (passageData == null)
			return null;
		return passageData.name;
	}
	

	/**
	 * Get the distance travelled to date in the current passage.
	 * 
	 * @return				The distance travelled in this passage.
	 * 						Null if we're not in a passage.
	 */
	public Distance getPassageDistance() {
		if (passageData == null)
			return null;
		return passageData.distance;
	}
	
	
	/**
	 * Get a Cursor on the list of points traversed in a given passage.
	 * 
	 * @return				A Cursor over the points list, ordered by time.
	 */
	Cursor getPointsCursor(long passageId) {
		if (database == null)
			return null;
		
		return database.query(POINT_TABLE, PointField.fields,
							  PointField.POINT_PASSAGE_ID.name + "=" + passageId,
							  null, null, null,
							  PointField.POINT_TIME.name + " ASC");
	}


	/**
	 * Get the day number of the current passage.
	 * 
	 * @return				The day number of the current passage.
	 * 						0 = first day.  -1 if we're not in a passage.
	 */
	public int getPassageDay() {
		if (passageData == null || !passageData.isRunning())
			return -1;

		// Get the julian day number.
		long time = System.currentTimeMillis();
		int today = (int) (Instant.javaToJulian(time) + 0.5);
		return today - passageData.startDay;
	}


    // ******************************************************************** //
    // Watch Tracking.
    // ******************************************************************** //
	
	/**
	 * Update the crew model for the current bell.  See if the on-watch
	 * crew has changed, and tell people if so.
	 */
	private void updateWatch() {
//		// Get the passage day number.
//		int day = getPassageDay();
//			
//		// If the day or watch has changed, tell everyone.
//		if (true) {
//			// Notify all the registered clients that we have an update.
//			for (Listener listener : watchListeners)
//				listener.watchChange(currentDay, currentWatch, currentWatchCrew);
//		}
	}
	

    // ******************************************************************** //
    // Private Classes.
    // ******************************************************************** //

	/**
     * This class helps open, create, and upgrade the passage database.
     */
    private static final class DbHelper extends SQLiteOpenHelper {
    	DbHelper(Context context) {
            super(context, DB_NAME, null, DB_VER);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            Log.i(TAG, "PassageModel: create tables");

        	String create = "CREATE TABLE " + DATA_TABLE + " (";
    		create += DATA_ID + " INTEGER PRIMARY KEY";
    		create += "," + DATA_CURRENT_PASS + " INTEGER";
     		create += ");";
    		db.execSQL(create);

    		create = "CREATE TABLE " + PASS_TABLE + " (";
    		for (PassField field : PassField.values()) {
    			if (field != PassField.PASS_ID)
    				create += ", ";
    			create += field.name + " " + field.type;
    		}
    		create += ");";
    		db.execSQL(create);

    		create = "CREATE TABLE " + POINT_TABLE + " (";
    		for (PointField field : PointField.values()) {
    			if (field != PointField.POINT_ID)
    				create += ", ";
    			create += field.name + " " + field.type;
    		}
    		create += ");";
    		db.execSQL(create);

			// Add a default configuration to the config table.
			ContentValues values = new ContentValues();
			values.put(DATA_CURRENT_PASS, 0);
			db.insert(DATA_TABLE, DATA_CURRENT_PASS, values);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldV, int newV) {
            Log.i(TAG, "PassageModel: upgrade tables");
            
            // We can simply dump all the old data.
        	db.execSQL("DROP TABLE IF EXISTS " + DATA_TABLE);
        	db.execSQL("DROP TABLE IF EXISTS " + PASS_TABLE);
        	db.execSQL("DROP TABLE IF EXISTS " + POINT_TABLE);
        	onCreate(db);
        }
    	
        @Override
		public void onOpen(SQLiteDatabase db) {
            Log.i(TAG, "PassageModel: database opened");
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
	private static final String DB_NAME = "PassageData";
	private static final int DB_VER = 5;

	// Configuration data table.
	private static final String DATA_TABLE = "config";
	private static final String DATA_ID = "_id";
	private static final String DATA_CURRENT_PASS = "current_Passage";

	// Table of overall data about each passage.
	private static final String PASS_TABLE = "passages";
	private enum PassField {
		PASS_ID("_id", "INTEGER PRIMARY KEY"),
		PASS_NAME("name", "TEXT"),
		PASS_START_NAME("start_name", "TEXT"),
		PASS_START_TIME("start_time", "INTEGER"),
		PASS_START_LAT("start_latitude", "REAL"),
		PASS_START_LON("start_longitude", "REAL"),
		PASS_DEST_NAME("dest_name", "TEXT"),
		PASS_DEST_TIME("dest_time", "INTEGER"),
		PASS_DEST_LAT("dest_latitude", "REAL"),
		PASS_DEST_LON("dest_longitude", "REAL"),
		PASS_DISTANCE("distance", "INTEGER");
		
		PassField(String name, String type) {
			this.name = name;
			this.type = type;
		}
		
		final String name;
		final String type;
		
		static final String[] fields;
		static {
			PassField[] values = values();
			fields = new String[values.length];
			for (int i = 0; i < values.length; ++i)
				fields[i] = values[i].name;
		}
	}

	// Table of the points we passed through.
	private static final String POINT_TABLE = "points";
	private enum PointField {
		POINT_ID("_id", "INTEGER PRIMARY KEY"),
		POINT_NAME("name", "TEXT"),
		POINT_PASSAGE_ID("data_id", "INTEGER"),
		POINT_TIME("time", "INTEGER"),
		POINT_LAT("latitude", "REAL"),
		POINT_LON("longitude", "REAL"),
		POINT_DIST("dist", "INTEGER"),
		POINT_TOT_DIST("tot_dist", "INTEGER");
		
		PointField(String name, String type) {
			this.name = name;
			this.type = type;
		}
		
		final String name;
		final String type;
		
		static final String[] fields;
		static {
			PointField[] values = values();
			fields = new String[values.length];
			for (int i = 0; i < values.length; ++i)
				fields[i] = values[i].name;
		}
	}

	
	// ******************************************************************** //
	// Class Data.
	// ******************************************************************** //

	// The instance of the crew model; null if not created yet.
	private static PassageModel modelInstance = null;


	// ******************************************************************** //
	// Private Data.
	// ******************************************************************** //

	// Parent app we're running in.
	private Context appContext;

	// The time and location models.
	private TimeModel timeModel;
	private LocationModel locationModel;

	// Database open/close helper.
	private DbHelper databaseHelper = null;

    // The database we use for storing our data.  Null if not open.
    private SQLiteDatabase database = null;
    
    // Information on the current passage.  Null if no passage.
	private PassageData passageData = null;
 
}

