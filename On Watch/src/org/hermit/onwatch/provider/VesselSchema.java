
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
 * Class defining the schema for the vessel and crew data content provider.
 */
public final class VesselSchema
    extends DbSchema
{

    // ******************************************************************** //
    // Public Data.
    // ******************************************************************** //

    // Schemas of the tables in this provider.
    private static final TableSchema[] TABLE_SCHEMAS = {
        new Vessels(),
        new Crew(),
    };


    /**
     * The canonical schema instance for this provider.
     */
    public static final VesselSchema DB_SCHEMA = new VesselSchema();


    // ******************************************************************** //
    // Constant Definitions.
    // ******************************************************************** //
    
    // Database name and version.
    private static final String DB_NAME = "vessels.db";
    private static final int DB_VERSION = 2;
    
    // Provider authority name.
    private static final String AUTHORITY = "org.hermit.provider.VesselData";

    
	// ******************************************************************** //
    // Vessels Table Definition.
    // ******************************************************************** //

    /**
     * Schema for the vessels table.
     */
    public static final class Vessels
        extends TableSchema
    {
        
        // The vessels table name.
        private static final String TABLE_NAME = "vessels";
        
        // Basic type for this table.
        private static final String TABLE_TYPE = "vnd.org.hermit.sailing.vessel";
        
        /**
         * The content URI for this table.
         */
        public static final Uri CONTENT_URI =
                        Uri.parse("content://" + AUTHORITY + "/" + TABLE_NAME);

        /**
         * The default sort order for this table.
         */
        public static final String SORT_ORDER = "name ASC";


        /**
         * Vessels table field: the name of the vessel.
         */
        public static final String NAME = "name";
        
        /**
         * Vessels table field: the watch plan in use on the vessel.
         * This is the name of one of the WatchPlan enum constants.
         */
        public static final String WATCHES = "watches";
        
        
        // Definitions of the fields.
        private static final FieldDesc[] FIELDS = {
        	new FieldDesc(NAME, FieldType.TEXT),
        	new FieldDesc(WATCHES, FieldType.TEXT),
        };
        
        /**
         * A projection for this table, which returns all fields
         * (including the implicit "_id" field).
         */
        public static final String[] PROJECTION = makeProjection(FIELDS);

        /**
         * Create a vessels table schema instance.
         */
        protected Vessels() {
            super(TABLE_NAME, TABLE_TYPE, CONTENT_URI, SORT_ORDER, FIELDS);
        }
        
    }

    
	// ******************************************************************** //
    // Crew Table Definition.
    // ******************************************************************** //

    /**
     * Schema for the crew table.
     */
    public static final class Crew
        extends TableSchema
    {
        
        // The vessels table name.
        private static final String TABLE_NAME = "crew";
        
        // Basic type for this table.
        private static final String TABLE_TYPE = "vnd.org.hermit.sailing.crew";
        
        /**
         * The content URI for this table.
         */
        public static final Uri CONTENT_URI =
                        Uri.parse("content://" + AUTHORITY + "/" + TABLE_NAME);

        /**
         * The default sort order for this table.
         */
        public static final String SORT_ORDER = "name ASC";


        /**
         * Crew table field: the name of the crew member.
         */
        public static final String NAME = "name";

        /**
         * Crew table field: the ID of the vessel he/she belongs to.
         */
        public static final String VESSEL = "vessel";

        /**
         * Crew table field: the index of the colour assigned to the crew.
         */
        public static final String COLOUR = "colour";

        /**
         * Crew table field: the crew's position in the watch plan;
         * negative if not set.
         */
        public static final String POSITION = "position";

        
        // Definitions of the fields.
        private static final FieldDesc[] FIELDS = {
        	new FieldDesc(NAME, FieldType.TEXT),
        	new FieldDesc(VESSEL, FieldType.BIGINT),
        	new FieldDesc(COLOUR, FieldType.INT),
        	new FieldDesc(POSITION, FieldType.INT),
        };
        
        /**
         * A projection for this table, which returns all fields
         * (including the implicit "_id" field).
         */
        public static final String[] PROJECTION = makeProjection(FIELDS);

        /**
         * Create a vessels table schema instance.
         */
        protected Crew() {
            super(TABLE_NAME, TABLE_TYPE, CONTENT_URI, SORT_ORDER, FIELDS);
        }
        
    }


    // ******************************************************************** //
    // Constructor.
    // ******************************************************************** //

    /**
     * Create a vessel database schema instance.
     */
    private VesselSchema() {
        super(DB_NAME, DB_VERSION, AUTHORITY, TABLE_SCHEMAS);
    }

}

