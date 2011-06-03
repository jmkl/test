
/**
 * On Watch: sailor's watchkeeping assistant.
 * <br>Copyright 2009-2010 Ian Cameron Smith
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


package org.hermit.onwatch.provider;


import org.hermit.android.provider.DbSchema;
import org.hermit.android.provider.TableSchema;

import android.net.Uri;


/**
 * Class defining the schema for the passage data content provider.
 */
public final class PassageSchema
    extends DbSchema
{

    // ******************************************************************** //
    // Public Data.
    // ******************************************************************** //

    /**
     * The canonical schema instance for this provider.
     */
    public static final PassageSchema DB_SCHEMA = new PassageSchema();


    // ******************************************************************** //
    // Constant Definitions.
    // ******************************************************************** //
    
    // Database name and version.
    private static final String DB_NAME = "passages.db";
    private static final int DB_VERSION = 1;
    
    // Provider authority name.
    private static final String AUTHORITY = "org.hermit.provider.PassageData";

    
	// ******************************************************************** //
    // Passages Table Definition.
    // ******************************************************************** //

    /**
     * Schema for the passages table.
     */
    public static final class Passages
        extends TableSchema
    {
        
        // The passages table name.
        private static final String TABLE_NAME = "passages";
        
        // Basic type for this table.
        private static final String TABLE_TYPE = "vnd.org.hermit.sailing.passage";
        
        /**
         * The content URI for this table.
         */
        public static final Uri CONTENT_URI =
                        Uri.parse("content://" + AUTHORITY + "/" + TABLE_NAME);

        /**
         * The default sort order for this table.
         */
        public static final String SORT_ORDER = "start_time ASC";


        /**
         * Passages table field: the name of the passage.
         */
        public static final String NAME = "name";

        /**
         * Passages table field: the start location name for the passage.
         */
        public static final String START_NAME = "start_name";

        /**
         * Passages table field: the destination location name for the passage.
         */
        public static final String DEST_NAME = "dest_name";

        /**
         * Passages table field: the latitude in degrees of the indended
         * destination.
         */
        public static final String DEST_LAT = "dest_latitude";

        /**
         * Passages table field: the longitude in degrees of the indended
         * destination.
         */
        public static final String DEST_LON = "dest_longitude";

        /**
         * Passages table field: the timestamp for when the passage started.
         */
        public static final String START_TIME = "start_time";

        /**
         * Passages table field: the latitude in degrees where the passage started.
         */
        public static final String START_LAT = "start_latitude";

        /**
         * Passages table field: the longitude in degrees where the passage started.
         */
        public static final String START_LON = "start_longitude";

        /**
         * Passages table field: the timestamp for when the passage ended.
         */
        public static final String FINISH_TIME = "finish_time";

        /**
         * Passages table field: the latitude in degrees where the passage ended.
         */
        public static final String FINISH_LAT = "finish_latitude";

        /**
         * Passages table field: the longitude in degrees where the passage ended.
         */
        public static final String FINISH_LON = "finish_longitude";

        /**
         * Passages table field: flag if this passage is in progress.  Only
         * at most one passage should have this flag at any time.
         */
        public static final String UNDER_WAY = "under_way";

        /**
         * Passages table field: the distance in metres covered to date
         * in the passage.
         */
        public static final String DISTANCE = "distance";
        
        // Definitions of the fields.
        private static final FieldDesc[] FIELDS = {
            new FieldDesc(NAME, FieldType.TEXT),
            new FieldDesc(START_NAME, FieldType.TEXT),
            new FieldDesc(DEST_NAME, FieldType.TEXT),
            new FieldDesc(DEST_LAT, FieldType.DOUBLE),
            new FieldDesc(DEST_LON, FieldType.DOUBLE),
            new FieldDesc(START_TIME, FieldType.BIGINT),
            new FieldDesc(START_LAT, FieldType.DOUBLE),
            new FieldDesc(START_LON, FieldType.DOUBLE),
            new FieldDesc(FINISH_TIME, FieldType.BIGINT),
            new FieldDesc(FINISH_LAT, FieldType.DOUBLE),
            new FieldDesc(FINISH_LON, FieldType.DOUBLE),
            new FieldDesc(UNDER_WAY, FieldType.BOOLEAN),
            new FieldDesc(DISTANCE, FieldType.DOUBLE),
        };
        
        /**
         * A projection for this table, which returns all fields
         * (including the implicit "_id" field).
         */
        public static final String[] PROJECTION = makeProjection(FIELDS);

        /**
         * Create a passages table schema instance.
         */
        protected Passages() {
            super(TABLE_NAME, TABLE_TYPE, CONTENT_URI, SORT_ORDER, FIELDS);
        }
        
    }

    
	// ******************************************************************** //
    // Points Table Definition.
    // ******************************************************************** //

    /**
     * Schema for the points table.
     */
    public static final class Points
        extends TableSchema
    {

        // Name of the points table.
        private static final String TABLE_NAME = "points";
        
        // Base MIME type for this table.
        private static final String TABLE_TYPE = "vnd.org.hermit.sailing.point";
        
        /**
         * The content URI for this table.
         */
        public static final Uri CONTENT_URI =
                        Uri.parse("content://" + AUTHORITY + "/" + TABLE_NAME);

        /**
         * The default sort order for this table.
         */
        public static final String SORT_ORDER = "time ASC";

        
        /**
         * Passages table field: the name of the point, if any.
         */
        public static final String NAME = "name";

        /**
         * Passages table field: the ID of the passage it belongs to.
         */
        public static final String PASSAGE = "passage";

        /**
         * Passages table field: the timestamp for this point.
         */
        public static final String TIME = "time";

        /**
         * Passages table field: the latitude in degrees of this point.
         */
        public static final String LAT = "latitude";

        /**
         * Passages table field: the longitude in degrees of this point.
         */
        public static final String LON = "longitude";

        /**
         * Passages table field: the distance in metres from the previous
         * point.
         */
        public static final String DIST = "distance";

        /**
         * Passages table field: the distance in metres from the start of
         * the passage.
         */
        public static final String TOT_DIST = "tot_dist";
        
        // Definitions of the fields.
        private static final FieldDesc[] FIELDS = {
        	new FieldDesc(NAME, FieldType.TEXT),
        	new FieldDesc(PASSAGE, FieldType.BIGINT),
        	new FieldDesc(TIME, FieldType.BIGINT),
        	new FieldDesc(LAT, FieldType.DOUBLE),
        	new FieldDesc(LON, FieldType.DOUBLE),
        	new FieldDesc(DIST, FieldType.DOUBLE),
        	new FieldDesc(TOT_DIST, FieldType.DOUBLE),
        };
        
        /**
         * A projection for this table, which returns all fields
         * (including the implicit "_id" field).
         */
        public static final String[] PROJECTION = makeProjection(FIELDS);
        
        /**
         * Create a points table schema instance.
         */
        protected Points() {
            super(TABLE_NAME, TABLE_TYPE, CONTENT_URI, SORT_ORDER, FIELDS);
        }
        
    }
    
    
    // ******************************************************************** //
    // Constructor.
    // ******************************************************************** //

    /**
     * Create a passage database schema instance.
     */
    private PassageSchema() {
        super(DB_NAME, DB_VERSION, AUTHORITY, TABLE_SCHEMAS);
    }

    
    // ******************************************************************** //
    // Class Data.
    // ******************************************************************** //

    // Schemas of the tables in this provider.
    private static final TableSchema[] TABLE_SCHEMAS = {
        new Passages(),
        new Points(),
    };

}

