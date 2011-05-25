
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


import org.hermit.onwatch.provider.VesselSchema;

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
class CrewRecord {

    // ******************************************************************** //
    // Constructors.
    // ******************************************************************** //

    /**
	 * Create a passage record from a set of specified values.  The passage
	 * will be set up as new, not started.
	 * 
	 * @param	name		The name for this person.
	 * @param	col			The colour number allocated to this person.
	 * @param	pos			The position of this hand in the watch schedule.
	 */
    CrewRecord(String name, int col, int pos) {
        rowValues = new ContentValues();
        rowValues.put(VesselSchema.Crew.NAME, name);
        rowValues.put(VesselSchema.Crew.VESSEL, -1);
        rowValues.put(VesselSchema.Crew.COLOUR, col);
        rowValues.put(VesselSchema.Crew.POSITION, pos);
        
        init();
    }
    
    
    /**
	 * Load a hand's record from a given cursor.
	 * 
	 * @param	crewCursor	The cursor to load the hand's data from.
	 */
    CrewRecord(Cursor crewCursor) {
        rowValues = new ContentValues();
        DatabaseUtils.cursorRowToContentValues(crewCursor, rowValues);
        
        init();
    }
    
    
    /**
     * Initialize this passage record from rowValues.
     */
    private void init() {
        name = rowValues.getAsString(VesselSchema.Crew.NAME);
        colour = rowValues.getAsInteger(VesselSchema.Crew.COLOUR);
        position = rowValues.getAsInteger(VesselSchema.Crew.POSITION);
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
	 * Get the id of this hand.
	 *
	 * @return			The id of this hand.
	 */
	long getId() {
		return id;
	}


	/**
	 * Get the name of this hand.
	 *
	 * @return			The name of this hand.
	 */
	String getName() {
		return name;
	}


	/**
	 * Set name of this hand.
	 *
	 * @param	name	The hand's name to set.
	 */
	void setName(String name) {
		this.name = name;
		rowValues.put(VesselSchema.Crew.NAME, name);
	}


	/**
	 * Get the hand's allocated colour.
	 *
	 * @return			The hand's allocated colour.
	 */
	int getColour() {
		return colour;
	}


	/**
	 * Set the hand's allocated colour.
	 *
	 * @param	colour	The colour to set.
	 */
	void setColour(int colour) {
		this.colour = colour;
		rowValues.put(VesselSchema.Crew.COLOUR, colour);
	}


	/**
	 * Get the hand's watch position.
	 *
	 * @return			The hand's watch position.
	 */
	int getPosition() {
		return position;
	}


	/**
	 * Set the hand's watch position.
	 *
	 * @param	colour	The watch position to set.
	 */
	void setPosition(int position) {
		this.position = position;
		rowValues.put(VesselSchema.Crew.POSITION, position);
	}


	// ******************************************************************** //
	// Class Data.
	// ******************************************************************** //

    // Debugging tag.
	private static final String TAG = "onwatch";
    

    // ******************************************************************** //
    // Private Data.
    // ******************************************************************** //
	
    // The passage ID, name, colour index, and watch position.
    private long id = -1;
    private String name = null;
    private int colour;
    private int position;
    
    // The values of the fields in this row.
    private final ContentValues rowValues;
    
}

