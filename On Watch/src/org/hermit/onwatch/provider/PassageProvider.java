
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


import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.provider.BaseColumns;
import android.text.TextUtils;
import android.util.Log;

import java.util.HashMap;

import org.hermit.provider.PassageData;


/**
 * Provides access to a database of notes. Each note has a title, the note
 * itself, a creation date and a modified data.
 */
public class PassageProvider extends ContentProvider {

    // ******************************************************************** //
    // Initialization.
    // ******************************************************************** //

    /**
     * Called when the provider is being started.
     * 
     * @return          true if the provider was successfully loaded,
     *                  false otherwise.
     */
    @Override
    public boolean onCreate() {
        mOpenHelper = new DatabaseHelper(getContext());
        return true;
    }

    
    // ******************************************************************** //
    // Data Access.
    // ******************************************************************** //
    
    /**
     * Return the MIME type of the data at the given URI.  This should
     * start with vnd.android.cursor.item/ for a single record, or
     * vnd.android.cursor.dir/ for multiple items.
     * 
     * @param   uri         The URI to query.
     * @return              MIME type string for the given URI, or null 
     *                      if there is no type. 
     */
    @Override
    public String getType(Uri uri) {
        switch (sUriMatcher.match(uri)) {
        case CONFIG:
            return PassageData.Config.CONTENT_TYPE;

        case CONFIG_ID:
            return PassageData.Config.CONTENT_ITEM_TYPE;

        case PASSAGE:
            return PassageData.Passages.CONTENT_TYPE;

        case PASSAGE_ID:
            return PassageData.Passages.CONTENT_ITEM_TYPE;

        case POINT:
            return PassageData.Points.CONTENT_TYPE;

        case POINT_ID:
            return PassageData.Points.CONTENT_ITEM_TYPE;

        default:
            throw new IllegalArgumentException("Unknown URI " + uri +
                                               " in getType()");
        }
    }
    

    /**
     * Receives a query request from a client in a local process, and
     * returns a Cursor.  This is called internally by the ContentResolver.
     * 
     * @param   uri         The URI to query.  This will be the full URI
     *                      sent by the client; if the client is requesting
     *                      a specific record, the URI will end in a record
     *                      number that the implementation should parse and
     *                      add to a WHERE or HAVING clause, specifying that
     *                      _id value.
     * @param   projection  The list of columns to put into the cursor.
     *                      If null all columns are included.
     * @param   where       A selection criteria to apply when filtering
     *                      rows.  If null then all rows are included.
     * @param   whereArgs   You may include ?s in selection, which will
     *                      be replaced by the values from selectionArgs,
     *                      in order that they appear in the selection.
     *                      The values will be bound as Strings.
     * @param   sortOrder   How the rows in the cursor should be sorted.
     *                      If null then the provider is free to define the
     *                      sort order.
     * @return              A Cursor or null. 
     */
    @Override
    public Cursor query(Uri uri, String[] projection,
                        String where, String[] whereArgs,
                        String sortOrder)
    {
        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();

        String torder;
        switch (sUriMatcher.match(uri)) {
        case CONFIG:
            qb.setTables(CONFIG_TABLE_NAME);
            qb.setProjectionMap(configProjectionMap);
            torder = PassageData.Config.DEFAULT_SORT_ORDER;
            break;

        case CONFIG_ID:
            qb.setTables(CONFIG_TABLE_NAME);
            qb.setProjectionMap(configProjectionMap);
            qb.appendWhere(BaseColumns._ID + "=" + uri.getPathSegments().get(1));
            torder = PassageData.Config.DEFAULT_SORT_ORDER;
            break;

        case PASSAGE:
            qb.setTables(PASSAGES_TABLE_NAME);
            qb.setProjectionMap(passagesProjectionMap);
            torder = PassageData.Passages.DEFAULT_SORT_ORDER;
            break;

        case PASSAGE_ID:
            qb.setTables(PASSAGES_TABLE_NAME);
            qb.setProjectionMap(passagesProjectionMap);
            qb.appendWhere(BaseColumns._ID + "=" + uri.getPathSegments().get(1));
            torder = PassageData.Passages.DEFAULT_SORT_ORDER;
            break;

        case POINT:
            qb.setTables(POINTS_TABLE_NAME);
            qb.setProjectionMap(pointsProjectionMap);
            torder = PassageData.Points.DEFAULT_SORT_ORDER;
            break;

        case POINT_ID:
            qb.setTables(POINTS_TABLE_NAME);
            qb.setProjectionMap(pointsProjectionMap);
            qb.appendWhere(BaseColumns._ID + "=" + uri.getPathSegments().get(1));
            torder = PassageData.Points.DEFAULT_SORT_ORDER;
            break;

        default:
            throw new IllegalArgumentException("Unknown URI " + uri);
        }

        // If no sort order is specified, use the default.
        String orderBy;
        if (TextUtils.isEmpty(sortOrder))
            orderBy = torder;
        else
            orderBy = sortOrder;

        // Get the database and run the query.
        SQLiteDatabase db = mOpenHelper.getReadableDatabase();
        Cursor c = qb.query(db, projection,
                            where, whereArgs,
                            null, null, orderBy);

        // Tell the cursor what uri to watch, so it knows when its
        // source data changes.
        c.setNotificationUri(getContext().getContentResolver(), uri);
        return c;
    }


    // ******************************************************************** //
    // Data Insertion.
    // ******************************************************************** //
    
    /**
     * Implement this to insert a new row.  As a courtesy, call
     * notifyChange() after inserting.
     * 
     * @param   uri         The content:// URI of the insertion request.
     * @param   initValues  A set of column_name/value pairs to add to
     *                      the database.
     * @return              The URI for the newly inserted item. 
     */
    @Override
    public Uri insert(Uri uri, ContentValues initValues) {
        // Copy the values so we can add to it.  Create it if needed.
        ContentValues values;
        if (initValues != null)
            values = new ContentValues(initValues);
        else
            values = new ContentValues();
        
        // Now, do type-specific setup, and fill in any missing values.
        String table = null;
        Uri tableUri = null;
        String nullHack = null;
        switch (sUriMatcher.match(uri)) {
        case CONFIG:
            table = CONFIG_TABLE_NAME;
            tableUri = PassageData.Config.CONTENT_URI;
            nullHack = PassageData.Config.CURRENT;
            initConfig(values);
            break;
        case PASSAGE:
            table = PASSAGES_TABLE_NAME;
            tableUri = PassageData.Passages.CONTENT_URI;
            nullHack = PassageData.Passages.NAME;
            initPassage(values);
            break;
        case POINT:
            table = POINTS_TABLE_NAME;
            tableUri = PassageData.Points.CONTENT_URI;
            nullHack = PassageData.Points.NAME;
            initPoint(values);
            break;
        default:
            throw new IllegalArgumentException("Unknown URI " + uri +
                                               " in insert()");
        }

        // Insert the new row.
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        long rowId = db.insert(table, nullHack, values);
        if (rowId <= 0)
            throw new SQLException("Failed to insert row into " + uri);
        
        // Inform everyone about the change.
        Uri noteUri = ContentUris.withAppendedId(tableUri, rowId);
        getContext().getContentResolver().notifyChange(noteUri, null);
        
        return noteUri;
    }
    

    private void initConfig(ContentValues values) {
        if (values.containsKey(PassageData.Config.CURRENT) == false)
            values.put(PassageData.Config.CURRENT, 0l);
    }
    

    private void initPassage(ContentValues values) {
        Resources r = Resources.getSystem();
        if (values.containsKey(PassageData.Passages.NAME) == false)
            values.put(PassageData.Passages.NAME,
                       r.getString(android.R.string.untitled));
        if (values.containsKey(PassageData.Passages.DISTANCE) == false)
            values.put(PassageData.Passages.DISTANCE, 0l);
    }
    

    private void initPoint(ContentValues values) {
        Resources r = Resources.getSystem();
        if (values.containsKey(PassageData.Points.NAME) == false)
            values.put(PassageData.Points.NAME,
                       r.getString(android.R.string.untitled));
    }
    

    // ******************************************************************** //
    // Data Deletion.
    // ******************************************************************** //
    
    /**
     * A request to delete one or more rows.  The selection clause is
     * applied when performing the deletion, allowing the operation to
     * affect multiple rows in a directory.  As a courtesy, call
     * notifyDelete() after deleting.
     * 
     * The implementation is responsible for parsing out a row ID at the
     * end of the URI, if a specific row is being deleted.  That is, the
     * client would pass in content://contacts/people/22 and the
     * implementation is responsible for parsing the record number (22)
     * when creating an SQL statement.
     * 
     * @param   uri         The full URI to delete, including a row ID
     *                      (if a specific record is to  be deleted).
     * @param   where       An optional restriction to apply to rows when
     *                      deleting.
     * @param   whereArgs   You may include ?s in where, which will
     *                      be replaced by the values from whereArgs.
     * @return              The number of rows affected.
     * @throws  SQLException    Database error.
     */
    @Override
    public int delete(Uri uri, String where, String[] whereArgs) {
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        String rowId;
        int count = 0;
        switch (sUriMatcher.match(uri)) {
        case CONFIG:
            count = db.delete(CONFIG_TABLE_NAME, where, whereArgs);
            break;
        case CONFIG_ID:
            rowId = uri.getPathSegments().get(1);
            count = db.delete(CONFIG_TABLE_NAME, BaseColumns._ID + "=" + rowId +
                    (!TextUtils.isEmpty(where) ? " AND (" + where + ')' : ""), whereArgs);
            break;
        case PASSAGE:
            count = db.delete(PASSAGES_TABLE_NAME, where, whereArgs);
            break;
        case PASSAGE_ID:
            rowId = uri.getPathSegments().get(1);
            count = db.delete(PASSAGES_TABLE_NAME, BaseColumns._ID + "=" + rowId +
                    (!TextUtils.isEmpty(where) ? " AND (" + where + ')' : ""), whereArgs);
            break;
        case POINT:
            count = db.delete(POINTS_TABLE_NAME, where, whereArgs);
            break;
        case POINT_ID:
            rowId = uri.getPathSegments().get(1);
            count = db.delete(POINTS_TABLE_NAME, BaseColumns._ID + "=" + rowId +
                    (!TextUtils.isEmpty(where) ? " AND (" + where + ')' : ""), whereArgs);
            break;
        default:
            throw new IllegalArgumentException("Unknown URI " + uri);
        }

        getContext().getContentResolver().notifyChange(uri, null);
        return count;
    }


    // ******************************************************************** //
    // Data Updating.
    // ******************************************************************** //
    
    /**
     * Update a content URI.  All rows matching the optionally provided
     * selection will have their columns listed as the keys in the values
     * map with the values of those keys.  As a courtesy, call notifyChange()
     * after updating.
     * 
     * @param   uri         The URI to update.  This can potentially have a
     *                      record ID if this is an update request for a
     *                      specific record.
     * @param   values      A Bundle mapping from column names to new column
     *                      values (NULL is a valid value).
     * @param   where       An optional restriction to apply to rows when
     *                      updating.
     * @param   whereArgs   You may include ?s in where, which will
     *                      be replaced by the values from whereArgs.
     * @return              The number of rows affected.
     */
    @Override
    public int update(Uri uri, ContentValues values, String where, String[] whereArgs) {
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        String rowId;
        int count = 0;
        switch (sUriMatcher.match(uri)) {
        case CONFIG:
            count = db.update(CONFIG_TABLE_NAME, values, where, whereArgs);
            break;
        case CONFIG_ID:
            rowId = uri.getPathSegments().get(1);
            count = db.update(CONFIG_TABLE_NAME, values, BaseColumns._ID + "=" + rowId +
                    (!TextUtils.isEmpty(where) ? " AND (" + where + ')' : ""), whereArgs);
            break;
        case PASSAGE:
            count = db.update(PASSAGES_TABLE_NAME, values, where, whereArgs);
            break;
        case PASSAGE_ID:
            rowId = uri.getPathSegments().get(1);
            count = db.update(PASSAGES_TABLE_NAME, values, BaseColumns._ID + "=" + rowId +
                    (!TextUtils.isEmpty(where) ? " AND (" + where + ')' : ""), whereArgs);
            break;
        case POINT:
            count = db.update(POINTS_TABLE_NAME, values, where, whereArgs);
            break;
        case POINT_ID:
            rowId = uri.getPathSegments().get(1);
            count = db.update(POINTS_TABLE_NAME, values, BaseColumns._ID + "=" + rowId +
                    (!TextUtils.isEmpty(where) ? " AND (" + where + ')' : ""), whereArgs);
            break;
        default:
            throw new IllegalArgumentException("Unknown URI " + uri);
        }

        getContext().getContentResolver().notifyChange(uri, null);
        return count;
    }

    
    // ******************************************************************** //
    // URI Matcher.
    // ******************************************************************** //

    // Defines for the thing being accessed.
    private static final int CONFIG = 1;
    private static final int CONFIG_ID = 2;
    private static final int PASSAGE = 3;
    private static final int PASSAGE_ID = 4;
    private static final int POINT = 5;
    private static final int POINT_ID = 6;

    // The URI matcher determines, for a given URI, what is being accessed.
    private static final UriMatcher sUriMatcher;
    static {
        sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        sUriMatcher.addURI(PassageData.AUTHORITY, "config", CONFIG);
        sUriMatcher.addURI(PassageData.AUTHORITY, "config/#", CONFIG_ID);
        sUriMatcher.addURI(PassageData.AUTHORITY, "passage", PASSAGE);
        sUriMatcher.addURI(PassageData.AUTHORITY, "passage/#", PASSAGE_ID);
        sUriMatcher.addURI(PassageData.AUTHORITY, "point", POINT);
        sUriMatcher.addURI(PassageData.AUTHORITY, "point/#", POINT_ID);
    }
    
    
    // ******************************************************************** //
    // Class Data.
    // ******************************************************************** //

    // Debugging tag.
    @SuppressWarnings("unused")
    private static final String TAG = "onwatch";

    // Database name and version.
    private static final String DATABASE_NAME = "on_watch.db";
    private static final int DATABASE_VERSION = 1;
    
    // Config table.
    private static final String CONFIG_TABLE_NAME = "config";

    // Projection for the config table.
    private static HashMap<String, String> configProjectionMap;
    static {
        configProjectionMap = new HashMap<String, String>();
        configProjectionMap.put(PassageData.Config._ID, PassageData.Config._ID);
        configProjectionMap.put(PassageData.Config.CURRENT, PassageData.Config.CURRENT);
    }
    
    // Passages table.
    private static final String PASSAGES_TABLE_NAME = "passages";

    // Projection for the config table.
    private static HashMap<String, String> passagesProjectionMap;
    static {
        passagesProjectionMap = new HashMap<String, String>();
        passagesProjectionMap.put(PassageData.Passages._ID, PassageData.Passages._ID);
        passagesProjectionMap.put(PassageData.Passages.NAME, PassageData.Passages.NAME);
        passagesProjectionMap.put(PassageData.Passages.START_NAME, PassageData.Passages.START_NAME);
        passagesProjectionMap.put(PassageData.Passages.START_TIME, PassageData.Passages.START_TIME);
        passagesProjectionMap.put(PassageData.Passages.START_LAT, PassageData.Passages.START_LAT);
        passagesProjectionMap.put(PassageData.Passages.START_LON, PassageData.Passages.START_LON);
        passagesProjectionMap.put(PassageData.Passages.DEST_NAME, PassageData.Passages.DEST_NAME);
        passagesProjectionMap.put(PassageData.Passages.DEST_TIME, PassageData.Passages.DEST_TIME);
        passagesProjectionMap.put(PassageData.Passages.DEST_LAT, PassageData.Passages.DEST_LAT);
        passagesProjectionMap.put(PassageData.Passages.DEST_LON, PassageData.Passages.DEST_LON);
        passagesProjectionMap.put(PassageData.Passages.DISTANCE, PassageData.Passages.DISTANCE);
    }

    // Points table.
    private static final String POINTS_TABLE_NAME = "points";

    // Projection for the config table.
    private static HashMap<String, String> pointsProjectionMap;
    static {
        pointsProjectionMap = new HashMap<String, String>();
        pointsProjectionMap.put(PassageData.Points._ID, PassageData.Points._ID);
        pointsProjectionMap.put(PassageData.Points.NAME, PassageData.Points.NAME);
        pointsProjectionMap.put(PassageData.Points.PASSAGE, PassageData.Points.PASSAGE);
        pointsProjectionMap.put(PassageData.Points.TIME, PassageData.Points.TIME);
        pointsProjectionMap.put(PassageData.Points.LAT, PassageData.Points.LAT);
        pointsProjectionMap.put(PassageData.Points.LON, PassageData.Points.LON);
        pointsProjectionMap.put(PassageData.Points.DIST, PassageData.Points.DIST);
        pointsProjectionMap.put(PassageData.Points.TOT_DIST, PassageData.Points.TOT_DIST);
    }

    
    // ******************************************************************** //
    // Database Helper.
    // ******************************************************************** //

    /**
     * This class helps open, create, and upgrade the database file.
     */
    private static class DatabaseHelper extends SQLiteOpenHelper {

        DatabaseHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            // Create the config table.
            db.execSQL("CREATE TABLE " + CONFIG_TABLE_NAME + " (" +
                       PassageData.Config._ID + " INTEGER PRIMARY KEY," +
                       PassageData.Config.CURRENT + " INTEGER" +
                        ");");
            
            // Create the passages table.
            db.execSQL("CREATE TABLE " + PASSAGES_TABLE_NAME + " (" +
                       PassageData.Passages._ID + " INTEGER PRIMARY KEY," +
                       PassageData.Passages.NAME + " TEXT," +
                       PassageData.Passages.START_NAME + " TEXT," +
                       PassageData.Passages.START_TIME + " INTEGER," +
                       PassageData.Passages.START_LAT + " REAL," +
                       PassageData.Passages.START_LON + " REAL," +
                       PassageData.Passages.DEST_NAME + " TEXT," +
                       PassageData.Passages.DEST_TIME + " INTEGER," +
                       PassageData.Passages.DEST_LAT + " REAL," +
                       PassageData.Passages.DEST_LON + " REAL," +
                       PassageData.Passages.DISTANCE + " INTEGER" +
                        ");");
            
            // Create the points table.
            db.execSQL("CREATE TABLE " + POINTS_TABLE_NAME + " (" +
                       PassageData.Points._ID + " INTEGER PRIMARY KEY," +
                       PassageData.Points.NAME + " TEXT," +
                       PassageData.Points.PASSAGE + " INTEGER," +
                       PassageData.Points.TIME + " INTEGER," +
                       PassageData.Points.LAT + " REAL," +
                       PassageData.Points.LON + " REAL," +
                       PassageData.Points.DIST + " INTEGER," +
                       PassageData.Points.TOT_DIST + " INTEGER" +
                        ");");
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            Log.w(TAG, "Upgrading database from version " + oldVersion + " to "
                    + newVersion + ", which will destroy all old data");
            db.execSQL("DROP TABLE IF EXISTS " + CONFIG_TABLE_NAME);
            db.execSQL("DROP TABLE IF EXISTS " + PASSAGES_TABLE_NAME);
            db.execSQL("DROP TABLE IF EXISTS " + POINTS_TABLE_NAME);
            onCreate(db);
        }
    }

    
    // ******************************************************************** //
    // Private Data.
    // ******************************************************************** //

    // This content provider's database helper.
    private DatabaseHelper mOpenHelper;

}

