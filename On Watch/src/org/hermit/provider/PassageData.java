
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


package org.hermit.provider;


import org.hermit.android.provider.TableProvider;


/**
 * Convenience definitions for the passage data content provider.
 */
public final class PassageData
    extends TableProvider.Db
{
    
    /**
     * Configuration table.
     */
    public static final class Config
        extends TableProvider.Table
    {

        /**
         * The ID of the current passage.
         * <P>Type: INTEGER (long)</P>
         */
        public static final String CURRENT = "current";
        
        // Definitions of the fields.
        private static final String[][] FIELDS = {
            { CURRENT, "INTEGER" },
        };
        
        // The database table name.
        private static final String TABLE_NAME = "config";
        
        // Basic type for this table.
        private static final String BASE_TYPE = "vnd.hermit.org.passage.config";

        // The default sort order for this table.
        private static final String SORT_ORDER = "current ASC";
        
        private Config() {
            super(TABLE_NAME, BASE_TYPE, SORT_ORDER, FIELDS);
        }
    }


    /**
     * Passages table.
     */
    public static final class Passages
        extends TableProvider.Table
    {

        /**
         * The name of the passage.
         * <P>Type: TEXT</P>
         */
        public static final String NAME = "name";

        /**
         * The start location name for the passage.
         * <P>Type: TEXT</P>
         */
        public static final String START_NAME = "start_name";

        /**
         * The timestamp for when the passage started.
         * <P>Type: INTEGER (long)</P>
         */
        public static final String START_TIME = "start_time";

        /**
         * The latitude where the passage started.
         * <P>Type: REAL</P>
         */
        public static final String START_LAT = "start_latitude";

        /**
         * The longitude where the passage started.
         * <P>Type: REAL</P>
         */
        public static final String START_LON = "start_longitude";

        /**
         * The destination location name for the passage.
         * <P>Type: TEXT</P>
         */
        public static final String DEST_NAME = "dest_name";

        /**
         * The timestamp for when the passage ended.
         * <P>Type: INTEGER (long)</P>
         */
        public static final String DEST_TIME = "dest_time";

        /**
         * The latitude where the passage ended.
         * <P>Type: REAL</P>
         */
        public static final String DEST_LAT = "dest_latitude";

        /**
         * The longitude where the passage ended.
         * <P>Type: REAL</P>
         */
        public static final String DEST_LON = "dest_longitude";

        /**
         * The distance in metres covered to date in the passage.
         * <P>Type: INTEGER (long)</P>
         */
        public static final String DISTANCE = "distance";
        
        // Definitions of the fields.
        private static final String[][] FIELDS = {
            { NAME, "TEXT" },
            { START_NAME, "TEXT" },
            { START_TIME, "INTEGER" },
            { START_LAT, "REAL" },
            { START_LON, "REAL" },
            { DEST_NAME, "TEXT" },
            { DEST_TIME, "INTEGER" },
            { DEST_LAT, "REAL" },
            { DEST_LON, "REAL" },
            { DISTANCE, "INTEGER" },
        };
        
        // The database table name.
        private static final String TABLE_NAME = "passages";
        
        // Basic type for this table.
        private static final String BASE_TYPE = "vnd.hermit.org.passage.passage";

        // The default sort order for this table.
        private static final String SORT_ORDER = "start_time ASC";
        
        private Passages() {
            super(TABLE_NAME, BASE_TYPE, SORT_ORDER, FIELDS);
        }
    }


    /**
     * Points table.
     */
    public static final class Points
        extends TableProvider.Table
    {

        /**
         * The name of the point, if any.
         * <P>Type: TEXT</P>
         */
        public static final String NAME = "name";

        /**
         * The ID of the passage it belongs to.
         * <P>Type: INTEGER (long)</P>
         */
        public static final String PASSAGE = "passage";

        /**
         * The timestamp for this point.
         * <P>Type: INTEGER (long)</P>
         */
        public static final String TIME = "time";

        /**
         * The latitude of this point.
         * <P>Type: REAL</P>
         */
        public static final String LAT = "latitude";

        /**
         * The longitude of this point.
         * <P>Type: REAL</P>
         */
        public static final String LON = "longitude";

        /**
         * The distance in metres from the previous point.
         * <P>Type: INTEGER (long)</P>
         */
        public static final String DIST = "distance";

        /**
         * The distance in metres from the start of the passage.
         * <P>Type: INTEGER (long)</P>
         */
        public static final String TOT_DIST = "tot_dist";
        
        // Definitions of the fields.
        private static final String[][] FIELDS = {
            { NAME, "TEXT" },
            { PASSAGE, "INTEGER" },
            { TIME, "INTEGER" },
            { LAT, "REAL" },
            { LON, "REAL" },
            { DIST, "INTEGER" },
            { TOT_DIST, "INTEGER" },
        };
        
        // The database table name.
        private static final String TABLE_NAME = "points";
        
        // Basic type for this table.
        private static final String BASE_TYPE = "vnd.hermit.org.passage.point";

        // The default sort order for this table.
        private static final String SORT_ORDER = "time ASC";
        
        private Points() {
            super(TABLE_NAME, BASE_TYPE, SORT_ORDER, FIELDS);
        }
    }
    
    // Database name and version.
    private static final String DB_NAME = "passages.db";
    private static final int DB_VER = 1;
    
    // Overall authority.
    private static final String AUTHORITY = "org.hermit.provider.PassageData";

    // Tables in this provider.
    private static final TableProvider.Table[] TABLES = {
        new Config(),
    };
    
    /**
     * Construct an instance of this schema.
     */
    public PassageData() {
        super(DB_NAME, DB_VER, AUTHORITY, TABLES);
    }

}

