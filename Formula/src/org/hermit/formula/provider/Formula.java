
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


import android.net.Uri;
import android.provider.BaseColumns;


/**
 * Convenience definitions for FormulaProvider.
 */
public final class Formula {
	
	// ******************************************************************** //
    // Public Constants.
    // ******************************************************************** //

	/**
	 * Authority name for this content provider.
	 */
    public static final String AUTHORITY = "org.hermit.provider.Formula";
	
    
	// ******************************************************************** //
    // Formulae Table Definitions.
    // ******************************************************************** //
 
    /**
     * Global definitions for accessing the formulae table.
     */
    public static final class Formulae implements BaseColumns {
    	
    	/** This class cannot be instantiated. */
        private Formulae() { }

        
        /**
         * The content:// style URL for this table.
         */
        public static final Uri CONTENT_URI =
        					Uri.parse("content://" + AUTHORITY + "/formulae");

        /**
         * The MIME type of {@link #CONTENT_URI} providing a directory of formulae.
         */
        public static final String CONTENT_TYPE =
        					"vnd.android.cursor.dir/vnd.org.hermit.formula";

        /**
         * The MIME type of a {@link #CONTENT_URI} sub-directory of a single formula.
         */
        public static final String CONTENT_ITEM_TYPE =
        					"vnd.android.cursor.item/vnd.org.hermit.formula";

        /**
         * The title of the formula.
         * <P>Type: TEXT</P>
         */
        public static final String TITLE = "title";

        /**
         * The formula itself.
         * <P>Type: TEXT</P>
         */
        public static final String FORMULA = "formula";

        /**
         * The timestamp for when the formula was created.
         * <P>Type: INTEGER (long from System.curentTimeMillis())</P>
         */
        public static final String CREATED_DATE = "created";

        /**
         * The timestamp for when the formula was last used.
         * <P>Type: INTEGER (long from System.curentTimeMillis())</P>
         */
        public static final String USED_DATE = "used";

        /**
         * Whether the formula is syntactically valid.
         * <P>Type: INTEGER (0 = false, 1 = true)</P>
         */
        public static final String VALID = "valid";

        /**
         * The default sort order for this table.
         */
        public static final String DEFAULT_SORT_ORDER = USED_DATE + " DESC";
    }
    
    
	// ******************************************************************** //
    // Constructors.
    // ******************************************************************** //

	/**
	 * This class cannot be instantiated.
	 */
    private Formula() { }
    
}

