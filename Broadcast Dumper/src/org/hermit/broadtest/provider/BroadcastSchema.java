
/**
 * broadtest: system broadcast dumper.
 * <br>Copyright 2010 Ian Cameron Smith
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


package org.hermit.broadtest.provider;


import org.hermit.android.provider.DbSchema;
import org.hermit.android.provider.TableSchema;

import android.net.Uri;


/**
 * Class defining the schema for the broadcast content provider.
 */
public class BroadcastSchema
	extends DbSchema
{

    // ******************************************************************** //
    // Public Types.
    // ******************************************************************** //

	private static final String DATABASE_NAME = "broadcasts";

    // Current schema version.
	private static final int SCHEMA_VERSION = 2;
	
    // Provider authority name.
	private static final String AUTHORITY = "com.bn.provider.Broadcast";

	/**
	 * Schema for the broadcast table.
	 */
	public static final class BroadcastTable
		extends TableSchema
	{
		
	    // Name of the broadcasts table.
		private static final String TABLE_NAME = "broadcasts";

	    // Base MIME type for the broadcasts table.
		private static final String TABLE_TYPE = "vnd.bn.broadcast";
	    
	    /**
	     * The content:// style URL for the broadcasts table.
	     */
		public static final Uri CONTENT_URI =
	    				Uri.parse("content://" + AUTHORITY + "/broadcasts");

	    /**
	     * The default sort order for the broadcasts table.
	     */
		public static final String SORT_ORDER = "time DESC";

	    /**
	     * Broadcast table field: the title of the broadcast.
	     */
		public static final String TITLE = "title";

	    /**
	     * Broadcast table field: any extras that came with the broadcast.
	     */
		public static final String EXTRAS = "extras";

	    /**
	     * Broadcast table field: the timestamp for when the broadcast was
	     * received.
	     */
		public static final String TIME = "time";

	    // Definitions of the fields.
		private static final String[][] FIELDS = {
	    	{ TITLE, "TEXT" },
	    	{ EXTRAS, "TEXT" },
	    	{ TIME, "INTEGER" },
	    };

		/**
		 * A default projection which gets all the fields of the broadcast
		 * table.
		 */
		public static final String[] PROJECTION = makeProjection(FIELDS);
		
	    /**
	     * Create a broadcasts table schema instance.
	     */
	    protected BroadcastTable() {
	    	super(TABLE_NAME, TABLE_TYPE, CONTENT_URI, SORT_ORDER, FIELDS);
	    }

	}
	
	
    // ******************************************************************** //
    // Constructor.
    // ******************************************************************** //

    /**
     * Create a broadcast database schema instance.
     */
    protected BroadcastSchema() {
    	super(DATABASE_NAME, SCHEMA_VERSION, AUTHORITY, TABLE_SCHEMAS);
    }
    

    // ******************************************************************** //
    // Class Data.
    // ******************************************************************** //

    // Our table schemas.
    private static final TableSchema[] TABLE_SCHEMAS = {
    	new BroadcastTable(),
    };
    
}

