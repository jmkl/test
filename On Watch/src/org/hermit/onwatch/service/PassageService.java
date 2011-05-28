
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
import org.hermit.onwatch.provider.PassageSchema;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
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
		locationManager =
			(LocationManager) appContext.getSystemService(Context.LOCATION_SERVICE);
        
        // If there's an open passage, get it now, and start logging
        // position updates to it.
        if (loadOpenPassage()) {
        	locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
        										   LOC_INTERVAL, LOC_DIST,
        										   locationListener);
        }
	}


	void close() {
		locationManager.removeUpdates(locationListener);
	}

	
	LocationListener locationListener = new LocationListener() {
		@Override
		public void onStatusChanged(String provider, int status, Bundle extras) {
		}
		
		@Override
		public void onProviderEnabled(String provider) {
		}
		
		@Override
		public void onProviderDisabled(String provider) {
		}
		
		@Override
		public void onLocationChanged(Location loc) {
            // Add the point to the points log, if we're in a passage.
            if (passageData != null && passageData.isRunning()) {
                long time = System.currentTimeMillis();
                Position pos = Position.fromDegrees(loc.getLatitude(),
                		 							loc.getLongitude());
                logPoint(pos, "", time);
            }
		}
	};
	

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


    /**
     * Determine whether any passage is currently running.
     */
    public boolean isAnyPassageRunning() {
        return passageData != null && passageData.isRunning();
    }


    /**
     * Determine whether a specified passage is currently running.
     * 
     * @param   uri         Database URI of the passage to check.
     */
    public boolean isRunning(Uri uri) {
        return passageData != null && passageUri.equals(uri);
    }


    /**
     * Start (or restart) the specified passage.  Does nothing if there
     * is no current passage, or if it is already started.
     * 
     * @param   uri         Database URI of the passage to start.
     */
    public void startPassage(Uri uri) {
    	// We will need location updates.
    	locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
				   							   LOC_INTERVAL, LOC_DIST,
				   							   locationListener);

        if (passageData != null && passageData.isRunning())
            throw new IllegalStateException("a passage is already running");

        if (!loadPassage(uri))
            throw new IllegalArgumentException("no passage with the given ID");
        
        // Start the passage and save the update.
        passageData.startPassage();
        passageData.saveData(contentResolver, passageUri);
    }


    /**
     * Finish the current passage.  Does nothing if there is no current
     * passage, or if it is not started or already finished.
     */
    public void finishPassage() {
        // No more location updates.
		locationManager.removeUpdates(locationListener);

		if (passageData == null)
            throw new IllegalStateException("must load a passage" +
											" to call finishPassage");
        if (!passageData.isRunning())
            throw new IllegalStateException("passage is not running");

        Location loc =
        	locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        Position pos = Position.fromDegrees(loc.getLatitude(),
											loc.getLongitude());
        long time = System.currentTimeMillis();

        // Add the ending point to the points log.
        logPoint(pos, passageData.getDest(), time);
        passageData.finishPassage(time, pos);
        passageData.saveData(contentResolver, passageUri);
        
        // We're done with this passage.
        passageData = null;
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
	
	// Minimum time in ms between required location updates.
	private static final long LOC_INTERVAL = 60 * 1000;
	
	// Minimum distance in metres between required location updates.
	private static final int LOC_DIST = 20;

	// The instance of the passage service; null if not created yet.
	private static PassageService serviceInstance = null;


	// ******************************************************************** //
	// Private Data.
	// ******************************************************************** //

	// Parent app we're running in.
	private Context appContext;
	
	// Our content resolver.
	private ContentResolver contentResolver;
	
	// Our location manager.
	private LocationManager locationManager;

    // URI of the currently selected passage.  null if no passage.
    private Uri passageUri = null;
    
    // Information on the currently selected passage.  Null if no passage.
	private PassageRecord passageData = null;
	
}

