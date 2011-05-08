
/**
 * Formula: programmable custom computations.
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


package org.hermit.formula.provider;


import org.hermit.android.provider.DbSchema;
import org.hermit.android.provider.TableSchema;

import android.net.Uri;


/**
 * Class defining the schema for the formula content provider.
 */
public final class FormulaSchema
	extends DbSchema
{
	
	// ******************************************************************** //
    // Constant Definitions.
    // ******************************************************************** //
    
    // Database name and version.
    private static final String DB_NAME = "formulae.db";
    private static final int DB_VERSION = 3;
    
    // Provider authority name.
    private static final String AUTHORITY = "org.hermit.provider.Formula";
	
    
	// ******************************************************************** //
    // Formulae Table Definition.
    // ******************************************************************** //
 
    /**
     * Global definitions for accessing the formulae table.
     */
    public static final class Formulae
    	extends TableSchema
    {
        
        // The formulae table name.
        private static final String TABLE_NAME = "formulae";
        
        // Basic type for this table.
        private static final String TABLE_TYPE = "vnd.org.hermit.formula";
        
        /**
         * The content URI for this table.
         */
        public static final Uri CONTENT_URI =
                        Uri.parse("content://" + AUTHORITY + "/" + TABLE_NAME);

        /**
         * The default sort order for this table.
         */
        public static final String SORT_ORDER = "used DESC";


        /**
         * Formulae table field: the title of the formula.
         * <P>Type: TEXT</P>
         */
        public static final String TITLE = "title";

        /**
         * Formulae table field: the formula itself.
         * <P>Type: TEXT</P>
         */
        public static final String FORMULA = "formula";

        /**
         * Formulae table field: the timestamp for when the formula was created.
         * <P>Type: INTEGER (long from System.curentTimeMillis())</P>
         */
        public static final String CREATED_DATE = "created";

        /**
         * Formulae table field: the timestamp for when the formula was last used.
         * <P>Type: INTEGER (long from System.curentTimeMillis())</P>
         */
        public static final String USED_DATE = "used";

        /**
         * Formulae table field: whether the formula is syntactically valid.
         * <P>Type: INTEGER (0 = false, 1 = true)</P>
         */
        public static final String VALID = "valid";
        
        
        // Definitions of the fields.
        private static final String[][] FIELDS = {
            { TITLE, "TEXT" },
            { FORMULA, "TEXT" },
            { CREATED_DATE, "INTEGER" },
            { USED_DATE, "INTEGER" },
            { VALID, "INTEGER" },
        };
        
        /**
         * A projection for this table, which returns all fields
         * (including the implicit "_id" field).
         */
        public static final String[] PROJECTION = makeProjection(FIELDS);

        /**
         * Create a formulae table schema instance.
         */
        protected Formulae() {
            super(TABLE_NAME, TABLE_TYPE, CONTENT_URI, SORT_ORDER, FIELDS, PROJECTION);
        }

    }
    
    
    // ******************************************************************** //
    // Constructor.
    // ******************************************************************** //

    /**
     * Create a passage database schema instance.
     */
    public FormulaSchema() {
        super(DB_NAME, DB_VERSION, AUTHORITY, TABLE_SCHEMAS);
    }

    
    // ******************************************************************** //
    // Class Data.
    // ******************************************************************** //

    // Schemas of the tables in this provider.
    private static final TableSchema[] TABLE_SCHEMAS = {
        new Formulae(),
    };
    
}

