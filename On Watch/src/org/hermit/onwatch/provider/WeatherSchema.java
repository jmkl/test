
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
 * Class defining the schema for the weather data content provider.
 */
public final class WeatherSchema
    extends DbSchema
{

    // ******************************************************************** //
    // Constant Definitions.
    // ******************************************************************** //
    
    // Database name and version.
    private static final String DB_NAME = "weather.db";
    private static final int DB_VERSION = 2;
    
    // Provider authority name.
    private static final String AUTHORITY = "org.hermit.provider.WeatherData";

    
	// ******************************************************************** //
    // Points Table Definition.
    // ******************************************************************** //

    /**
     * Schema for the points table.
     */
    public static final class Observations
        extends TableSchema
    {

        // Name of the points table.
        private static final String TABLE_NAME = "observations";
        
        // Base MIME type for this table.
        private static final String TABLE_TYPE = "vnd.org.hermit.sailing.observation";
        
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
         * Passages table field: the timestamp for this observation.
         */
        public static final String TIME = "time";

        /**
         * Passages table field: the barometric pressure, in mb.
         * -1 indicates not available.
         */
        public static final String PRESS = "pressure";
        
        // Definitions of the fields.
        private static final String[][] FIELDS = {
            { TIME, "INTEGER" },
            { PRESS, "REAL" },
        };
        
        /**
         * A projection for this table, which returns all fields
         * (including the implicit "_id" field).
         */
        public static final String[] PROJECTION = makeProjection(FIELDS);
        
        /**
         * Create a points table schema instance.
         */
        protected Observations() {
            super(TABLE_NAME, TABLE_TYPE, CONTENT_URI, SORT_ORDER, FIELDS, PROJECTION);
        }
        
    }
    
    
    // ******************************************************************** //
    // Constructor.
    // ******************************************************************** //

    /**
     * Create a passage database schema instance.
     */
    public WeatherSchema() {
        super(DB_NAME, DB_VERSION, AUTHORITY, TABLE_SCHEMAS);
    }

    
    // ******************************************************************** //
    // Class Data.
    // ******************************************************************** //

    // Schemas of the tables in this provider.
    private static final TableSchema[] TABLE_SCHEMAS = {
        new Observations(),
    };

}

