
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


import org.hermit.geo.Distance;
import org.hermit.geo.Position;
import org.hermit.onwatch.provider.PassageSchema;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.net.Uri;
import android.util.Log;


/**
 * Record of data on a particular passage.  This class encapsulates
 * all the data on a passage, and provides methods to convert
 * to and from database records.
 */
class PassageRecord {

    // ******************************************************************** //
    // Constructors.
    // ******************************************************************** //

    /**
	 * Create a passage record from a set of specified values.  The passage
	 * will be set up as new, not started.
	 * 
	 * @param	name		The name for this passage.
	 * @param	from		The name of the starting location.
	 * @param	to			The name of the destination.
	 * @param	dest		The position of the destination; null if not known.
	 */
    PassageRecord(String name, String from, String to, Position dest) {
        rowValues = new ContentValues();
        rowValues.put(PassageSchema.Passages.NAME, name);
        rowValues.put(PassageSchema.Passages.START_NAME, from);
        rowValues.put(PassageSchema.Passages.DEST_NAME, to);
        if (dest != null) {
        	rowValues.put(PassageSchema.Passages.DEST_LAT, dest.getLatDegs());
        	rowValues.put(PassageSchema.Passages.DEST_LON, dest.getLonDegs());
        }
        rowValues.put(PassageSchema.Passages.DISTANCE, 0.0);
        
        numPoints = 0;
        lastPos = null;
        
        init();
    }
    
    
    /**
	 * Load a passage record from a given cursor.
	 * 
	 * @param	passCursor	The cursor to load the passage data from.
	 * @param	pointCursor	The cursor to load the points data from; null
	 * 						if we have no points data for this passage.
	 */
    PassageRecord(Cursor passCursor, Cursor pointCursor) {
        rowValues = new ContentValues();
        DatabaseUtils.cursorRowToContentValues(passCursor, rowValues);
    	
    	// The last point of the current passage.  Null if no passage,
    	// or if it has no points.
        if (pointCursor != null) {
        	numPoints = pointCursor.getCount();
            if (pointCursor.moveToFirst()) {
                int lati = pointCursor.getColumnIndexOrThrow(PassageSchema.Points.LAT);
                int loni = pointCursor.getColumnIndexOrThrow(PassageSchema.Points.LON);
                double lat = pointCursor.getDouble(lati);
                double lon = pointCursor.getDouble(loni);
                lastPos = Position.fromDegrees(lat, lon);
            }
        }
        
        init();
    }
    
    
    /**
     * Initialize this passage record from rowValues.
     */
    private void init() {
        name = rowValues.getAsString(PassageSchema.Passages.NAME);
        start = rowValues.getAsString(PassageSchema.Passages.START_NAME);
        dest = rowValues.getAsString(PassageSchema.Passages.DEST_NAME);
        
        // Get the destination pos if present.
        Double dlat = rowValues.getAsDouble(PassageSchema.Passages.DEST_LAT);
        Double dlon = rowValues.getAsDouble(PassageSchema.Passages.DEST_LON);
        if (dlat == null || dlon == null)
            destPos = null;
        else
            destPos = Position.fromDegrees(dlat, dlon);

        // See if we have start data.
        Long stime = rowValues.getAsLong(PassageSchema.Passages.START_TIME);
        if (stime == null || stime == 0)
            startTime = 0;
        else
            startTime = stime;

        // Get the start pos if present.
        Double slat = rowValues.getAsDouble(PassageSchema.Passages.START_LAT);
        Double slon = rowValues.getAsDouble(PassageSchema.Passages.START_LON);
        if (slat == null || slon == null)
            startPos = null;
        else
            startPos = Position.fromDegrees(slat, slon);

        Double dist = rowValues.getAsDouble(PassageSchema.Passages.DISTANCE);
        if (dist == null || dist == 0)
            distance = Distance.ZERO;
        else
        	distance = new Distance(dist);
        
        // See if we have end data.
        Long etime = rowValues.getAsLong(PassageSchema.Passages.FINISH_TIME);
        if (etime == null || etime == 0)
            finishTime = 0;
        else
            finishTime = etime;
        
        // Get the end pos if present.
        Double flat = rowValues.getAsDouble(PassageSchema.Passages.FINISH_LAT);
        Double flon = rowValues.getAsDouble(PassageSchema.Passages.FINISH_LON);
        if (flat == null || flon == null)
            finishPos = null;
        else
        	finishPos = Position.fromDegrees(dlat, dlon);
    }

    
    // ******************************************************************** //
    // Persistence.
    // ******************************************************************** //

    /**
     * Save this passage to the given content resolver.
     * 
     * @param   cr      	ContentResolver to save to.
     * @param   curir      	URI specifying the key to save.
     */
    void saveData(ContentResolver cr, Uri uri) {
    	Log.i(TAG, "save " + uri);
    	cr.update(uri, rowValues, null, null);
    }

    
    // ******************************************************************** //
    // Accessors.
    // ******************************************************************** //

    /**
	 * Get the id of this passage.
	 *
	 * @return			The id of this passage.
	 */
	long getId() {
		return id;
	}


	/**
	 * Get the name of this passage.
	 *
	 * @return			The name of this passage.
	 */
	String getName() {
		return name;
	}


	/**
	 * Set name of this passage.
	 *
	 * @param	name	The passage name to set.
	 */
	void setName(String name) {
		this.name = name;
		rowValues.put(PassageSchema.Passages.NAME, name);
	}


	/**
	 * Get the name of the start location.
	 *
	 * @return			The name of the start location.
	 */
	String getStart() {
		return start;
	}


	/**
	 * Set the name of the start location.
	 *
	 * @param	start	The start location name to set.
	 */
	void setStart(String start) {
		this.start = start;
		rowValues.put(PassageSchema.Passages.START_NAME, start);
	}


	/**
	 * Get the name of the destination.
	 *
	 * @return			The destination name.
	 */
	String getDest() {
		return dest;
	}


	/**
	 * Set the name of the destination.
	 *
	 * @param	dest	The destination name to set.
	 */
	void setDest(String dest) {
		this.dest = dest;
		rowValues.put(PassageSchema.Passages.DEST_NAME, dest);
	}


	/**
	 * Get the intended position of the destination.
	 *
	 * @return			The position we are intending to sail to; null if
	 * 					not known.
	 */
	Position getDestPos() {
		return destPos;
	}


	/**
	 * Set the intended position of the destination.
	 *
	 * @param	pos		The position we are intending to sail for.
	 */
	void setDestPos(Position pos) {
		destPos = pos;
		rowValues.put(PassageSchema.Passages.DEST_LAT, pos.getLatDegs());
		rowValues.put(PassageSchema.Passages.DEST_LON, pos.getLonDegs());
	}


	/**
     * Determine whether this passage has started.
     * 
     * @return			true if this passage has started.  Note
     * 					that it may also have finished.
     */
    boolean isStarted() {
        return startTime != 0;
    }
    

    /**
     * Determine whether this passage is under way.
     * 
     * @return				true if this passage has started and not
     * 						yet finished.
     */
    boolean isRunning() {
        return startTime != 0 && finishTime == 0;
    }
    

    /**
     * Determine whether this passage is finished.
     * 
     * @return				true if this passage has finished.
     */
    boolean isFinished() {
        return finishTime != 0;
    }
    

	/**
	 * Get the start time of the passage.
	 *
	 * @return			The start time of the passage; 0 if the passage is
	 * 					not started yet.
	 */
	long getStartTime() {
		return startTime;
	}

	
	/**
	 * Get the position where the passage started.
	 *
	 * @return			The starting position; null if the passage is
	 * 					not started yet.
	 */
	Position getStartPos() {
		return startPos;
	}

	
	/**
	 * Get the distance covered in this passage to date.
	 *
	 * @return			The distance covered to date; 0 if the passage is
	 * 					not started yet.  If the passage is finished,
	 * 					this is the total distance.
	 */
	Distance getDistance() {
		return distance;
	}

	
	/**
	 * Get the number of points logged in this passage.
	 *
	 * @return			The number of points logged in this passage.
	 */
	int getNumPoints() {
		return numPoints;
	}


	/**
	 * Get the time at which we arrived at the destination.
	 *
	 * @return			The passage finish time; 0 if the passage is
	 * 					not finished yet.
	 */
	long getFinishTime() {
		return finishTime;
	}

	
	/**
	 * Get the position where we finished the passage.
	 *
	 * @return			The position we fetched up at; null if the passage is
	 * 					not finished yet.
	 */
	Position getFinishPos() {
		return finishPos;
	}


    // ******************************************************************** //
    // Passage Control.
    // ******************************************************************** //
	
	/**
	 * Set the start time of the passage.
	 *
	 * @param	time		The time we got under way.
	 * @param	pos			The position we sailed from.
	 */
	void startPassage(long time, Position pos) {
		startTime = time;
		startPos = pos;
		distance = Distance.ZERO;
		
		rowValues.put(PassageSchema.Passages.START_TIME, time);
		rowValues.put(PassageSchema.Passages.DISTANCE, 0l);
		rowValues.put(PassageSchema.Passages.START_LAT, pos.getLatDegs());
		rowValues.put(PassageSchema.Passages.START_LON, pos.getLonDegs());
	}


	/**
	 * Add a point to this passage.
	 *
	 * @param	pos			The point to log.
	 * @return				The distance covered from the previously
	 * 						logged point; Distance.ZERO if this is the first
	 * 						point.
	 */
	Distance logPoint(Position pos) {
        Distance dist = Distance.ZERO;
        if (lastPos != null) {
            dist = pos.distance(lastPos);
            distance = distance.add(dist);
        }
		++numPoints;
		lastPos = pos;
		
		rowValues.put(PassageSchema.Passages.DISTANCE, distance.getMetres());
		
		return dist;
	}


	/**
	 * Finish this passage.
	 *
	 * @param	time		The time at which we finished.
	 * @param	pos			The position we fetched up at.
	 */
	void finishPassage(long time, Position pos) {
		finishTime = time;
		finishPos = pos;
		
		rowValues.put(PassageSchema.Passages.FINISH_TIME, time);
		rowValues.put(PassageSchema.Passages.FINISH_LAT, pos.getLatDegs());
		rowValues.put(PassageSchema.Passages.FINISH_LON, pos.getLonDegs());
	}


	// ******************************************************************** //
	// Class Data.
	// ******************************************************************** //

    // Debugging tag.
	private static final String TAG = "onwatch";
    

    // ******************************************************************** //
    // Private Data.
    // ******************************************************************** //
	
    // The passage ID, name, from point name, and to name.
    private long id = -1;
    private String name = null;
    private String start = null;
    private String dest = null;
    
    // The target destination position; i.e. where we intend to go.
    // null if not known.
    private Position destPos = null;

    // The Java time and the Julian day number on which the current passage
    // started and finished.  0 if not set.
    private long startTime = 0;
    private long finishTime = 0;
    
    // The actual start and end points of this passage.  Null if not set.
    private Position startPos = null;
    private Position finishPos = null;
    
    // Distance covered on this passage (to date).
    private Distance distance = Distance.ZERO;
    
    // Number of points recorded for this passage.  (Not saved in DB.)
    private int numPoints = 0;
    
    // Most recently recorded position.  (Not saved in DB.)
    private Position lastPos = null;
    
    // The values of the fields in this row.
    private final ContentValues rowValues;
    
}

