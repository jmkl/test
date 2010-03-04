
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

import android.net.Uri;
import android.provider.BaseColumns;


/**
 * Convenience definitions for the passage data content provider.
 */
public final class PassageData {
    
    /**
     * Overall authority.
     */
    public static final String AUTHORITY = "org.hermit.provider.PassageData";

    
    /**
     * Configuration table.
     */
    public static final class Config implements BaseColumns {

        /**
         * The content:// style URL for this table
         */
        public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/config");

        /**
         * Basic type for this table.
         */
        public static final String BASE_TYPE = "vnd.hermit.org.passage.config";

        /**
         * The MIME type of {@link #CONTENT_URI} providing a directory of notes.
         */
        public static final String CONTENT_TYPE = "vnd.android.cursor.dir/" + BASE_TYPE;

        /**
         * The MIME type of a {@link #CONTENT_URI} sub-directory of a single note.
         */
        public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/" + BASE_TYPE;

        /**
         * The default sort order for this table.
         */
        public static final String DEFAULT_SORT_ORDER = "current ASC";

        /**
         * The ID of the current passage.
         * <P>Type: INTEGER (long)</P>
         */
        public static final String CURRENT = "current";
    }


    /**
     * Passages table.
     */
    public static final class Passages implements BaseColumns {
        
        /**
         * The content:// style URL for this table.
         */
        public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/passage");

        /**
         * Basic type for this table.
         */
        public static final String BASE_TYPE = "vnd.hermit.org.passage.passage";

        /**
         * The MIME type of {@link #CONTENT_URI} providing a directory of notes.
         */
        public static final String CONTENT_TYPE = "vnd.android.cursor.dir/" + BASE_TYPE;

        /**
         * The MIME type of a {@link #CONTENT_URI} sub-directory of a single note.
         */
        public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/" + BASE_TYPE;

        /**
         * The default sort order for this table.
         */
        public static final String DEFAULT_SORT_ORDER = "start_time ASC";

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
    }


    /**
     * Passages table.
     */
    public static final class Points implements BaseColumns {
        
        /**
         * The content:// style URL for this table.
         */
        public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/point");

        /**
         * Basic type for this table.
         */
        public static final String BASE_TYPE = "vnd.hermit.org.passage.point";

        /**
         * The MIME type of {@link #CONTENT_URI} providing a directory of notes.
         */
        public static final String CONTENT_TYPE = "vnd.android.cursor.dir/" + BASE_TYPE;

        /**
         * The MIME type of a {@link #CONTENT_URI} sub-directory of a single note.
         */
        public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/" + BASE_TYPE;

        /**
         * The default sort order for this table.
         */
        public static final String DEFAULT_SORT_ORDER = "time ASC";

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
    }

}

