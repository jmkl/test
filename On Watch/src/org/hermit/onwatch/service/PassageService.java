
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


import org.hermit.geo.Distance;
import org.hermit.geo.Position;
import org.hermit.onwatch.LocationModel;
import org.hermit.onwatch.TimeModel;
import org.hermit.onwatch.LocationModel.GpsState;
import org.hermit.onwatch.TimeModel.Field;
import org.hermit.onwatch.provider.PassageSchema;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;


/**
 * This class manages the passage data.
 */
public class PassageService
{

	// ******************************************************************** //
    // Constructor.
    // ******************************************************************** //

	/**
	 * Create a PassageService.  As a singleton we have a private
	 * constructor.
	 */
	private PassageService(Context context) {
        appContext = context;
		contentResolver = appContext.getContentResolver();
	}
	
	
	/**
	 * Get the passage service instance, creating it if it doesn't exist.
	 * 
	 * @param	context        Parent application.
	 * @return                 The passage service instance.
	 */
	public static PassageService getInstance(Context context) {
		if (serviceInstance == null)
			serviceInstance = new PassageService(context);
		
		return serviceInstance;
	}
	

	// ******************************************************************** //
	// Run Control.
	// ******************************************************************** //

	void open() {
        // Get the time model.  Ask it to ping us each bell.
        timeModel = TimeModel.getInstance(appContext);
        timeModel.listen(TimeModel.Field.MINUTE, new TimeModel.Listener() {
            @Override
            public void change(Field field, int value, long time) {
                // FIXME: do we need this?
            }
        });

        // Get our location model.  Ask it to keep us up to date.
        locationModel = LocationModel.getInstance(appContext);
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
        
        // If there's an open passage, get it now.
        loadOpenPassage();
	}


	void close() {
		// FIXME: timeModel.unlisten();
		// FIXME: locationModel();
	}


	// ******************************************************************** //
	// Passage Data Management.
	// ******************************************************************** //

    /**
     * Find an active -- i.e. under-way -- passage in the database.  If it
     * exists, copy its data into passageData.
     * 
     * @return              True if we found an open passage and copied it
     *                      into passageData.  False if we didn't, and
     *                      passageData is unchanged.
     */
	private boolean loadOpenPassage() {
        return loadPassage(PassageSchema.Passages.CONTENT_URI,
				   		   PassageSchema.Passages.UNDER_WAY + "!=0", null);
    }


    /**
     * Find the specified passage and load it into passageData.
     * 
     * @param   id          ID of the passage to load.
     * @return              True if we found the given passage and copied it
     *                      into passageData.  False if we didn't, and
     *                      passageData is unchanged.
     */
    private boolean loadPassage(long id) {
        String[] idParam = new String[] { "" + id };
        return loadPassage(PassageSchema.Passages.CONTENT_URI,
        				   PassageSchema.Passages._ID + "=?", idParam);
    }


    /**
     * Find the specified passage and load it into passageData.
     * 
     * @param   uri         Database URI of the passage to load.
     * @return              True if we found the given passage and copied it
     *                      into passageData.  False if we didn't, and
     *                      passageData is unchanged.
     */
    private boolean loadPassage(Uri uri) {
        return loadPassage(uri, null, null);
    }


    /**
     * Find the specified passage and load it into passageData.
     * 
     * @param   uri         Base database URI to load from.
     * @param   where       Where clause specifying the passage to load.
     * @param   wargs       Parameters for the where clause.
     * @return              True if we found the given passage and copied it
     *                      into passageData.  False if we didn't, and
     *                      passageData is unchanged.
     */
    private boolean loadPassage(Uri uri, String where, String[] wargs) {
        Cursor c = null;
        Cursor c2 = null;
        boolean found = false;

        try {
            c = contentResolver.query(uri,
            						  PassageSchema.Passages.PROJECTION,
            						  where, wargs,
            						  PassageSchema.Passages.SORT_ORDER);
            if (c != null && c.moveToFirst()) {
                // Query for the number of points, and load the latest point.
                int ii = c.getColumnIndexOrThrow(PassageSchema.Passages._ID);
                long id = c.getLong(ii);
                c2 = contentResolver.query(PassageSchema.Points.CONTENT_URI,
                                           PassageSchema.Points.PROJECTION,
                                           PassageSchema.Points.PASSAGE + "=?",
                                           new String[] { "" + id },
                                           PassageSchema.Points.TIME + " DESC");
                
                passageData = new PassageRecord(c, c2);
                passageUri = ContentUris.withAppendedId(PassageSchema.Passages.CONTENT_URI, id);
                found = true;
            }
        } finally {
            if (c != null)
            	c.close();
            if (c2 != null)
            	c2.close();
        }

        return found;
    }


//    /**
//     * Update the current passage with the given information.
//	 * 
//	 * @param	name		The name for this passage.
//	 * @param	from		The name of the starting location.
//	 * @param	to			The name of the destination.
//	 * @param	dest		The position of the destination; null if not known.
//     */
//    void updatePassage(String name, String from, String to, Position dest) {
//        if (passageData == null)
//            throw new IllegalStateException("must load a passage" +
//            								" to call updatePassage");
//        
//        passageData.setName(name);
//        passageData.setStart(from);
//        passageData.setDest(to);
//        passageData.setDestPos(dest);
//        
//        passageData.saveData(contentResolver, passageUri);
//    }
//
//
//    /**
//     * Delete the current passage.
//     */
//    public void deletePassage() {
//        if (passageData == null)
//            throw new IllegalStateException("must load a passage" +
//											" to call deletePassage");
//
//        // Delete all points belonging to the passage.
//    	long id = passageData.getId();
//        contentResolver.delete(PassageSchema.Points.CONTENT_URI,
//                               PassageSchema.Points.PASSAGE + "=" + id,
//                               null);
//
//        // Delete the passage record.
//        contentResolver.delete(passageUri, null, null);
//
//        passageData = null;
//        passageUri = null;
    //}


    /**
     * Determine whether a passage is currently running.
     */
    public boolean isRunning() {
        return passageData != null && passageData.isRunning();
    }


    /**
     * Start (or restart) the specified passage.  Does nothing if there
     * is no current passage, or if it is already started.
     * 
     * @param   uri         Database URI of the passage to start.
     */
    public void startPassage(Uri uri) {
        if (passageData != null && passageData.isRunning())
            throw new IllegalStateException("a passage is already running");

        if (!loadPassage(uri))
            throw new IllegalArgumentException("no passage with the given ID");
        
        Position pos = locationModel.getCurrentPos();
        long time = System.currentTimeMillis();

        passageData.startPassage(time, pos);

        // Add the starting point to the points log.  This will update
        // the database record for this passage.
        logPoint(pos, passageData.getStart(), time);

        // Notify the observers that we changed.
        // crewChanged();
    }


    /**
     * Finish the current passage.  Does nothing if there is no current
     * passage, or if it is not started or already finished.
     */
    public void finishPassage() {
        if (passageData == null)
            throw new IllegalStateException("must load a passage" +
											" to call finishPassage");
        if (!passageData.isRunning())
            throw new IllegalStateException("passage is not running");

        Position pos = locationModel.getCurrentPos();
        long time = System.currentTimeMillis();

        // Add the ending point to the points log.
        logPoint(pos, passageData.getDest(), time);
        passageData.finishPassage(time, pos);
        passageData.saveData(contentResolver, passageUri);
    }


    // ******************************************************************** //
    // Track Management.
    // ******************************************************************** //

    /**
     * Add the given point to the track.
     * 
     * @param   pos         The point to log.
     * @param   name        A name for the point, if we have one; else null.
     * @param   time        Time in ms at which we arrived there.
     */
    private void logPoint(Position pos, String name, long time) {
        // Get the distance from the previous point.  Add this to the passage.
    	Distance dist = passageData.logPoint(pos);
        Log.i(TAG, "Passage point: dist=" + dist.formatM() +
                    " tot=" + passageData.getDistance().formatM());
        
        // Create a Point record, and add it to the database.
        ContentValues values = new ContentValues();
        values.put(PassageSchema.Points.PASSAGE, passageData.getId());
        values.put(PassageSchema.Points.NAME, name);
        values.put(PassageSchema.Points.TIME, time);
        if (pos != null) {
            values.put(PassageSchema.Points.LAT, pos.getLatDegs());
            values.put(PassageSchema.Points.LON, pos.getLonDegs());
        }
        values.put(PassageSchema.Points.DIST, dist.getMetres());
        values.put(PassageSchema.Points.TOT_DIST, passageData.getDistance().getMetres());
        contentResolver.insert(PassageSchema.Points.CONTENT_URI, values);
        
        // Update the database record for this passage.
        passageData.saveData(contentResolver, passageUri);
    }
    

	// ******************************************************************** //
	// Class Data.
	// ******************************************************************** //

    // Debugging tag.
	private static final String TAG = "onwatchsvc";

	// The instance of the passage service; null if not created yet.
	private static PassageService serviceInstance = null;


	// ******************************************************************** //
	// Private Data.
	// ******************************************************************** //

	// Parent app we're running in.
	private Context appContext;
	
	// Our content resolver.
	private ContentResolver contentResolver;

    // The time and location models.
    private TimeModel timeModel;
    private LocationModel locationModel;

    // URI of the currently selected passage.  null if no passage.
    private Uri passageUri = null;
    
    // Information on the currently selected passage.  Null if no passage.
	private PassageRecord passageData = null;
	
}

