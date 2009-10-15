
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


import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;


/**
 * Provides access to a database of formulae.  Each formula has a title,
 * the formula itself, a creation date and a modified date.
 */
public class FormulaProvider extends ContentProvider {

	// ******************************************************************** //
	// Lifecycle.
	// ******************************************************************** //

    /**
     * Called when the provider is being started.
     *
     * @return				True if the provider was successfully loaded,
     * 						false otherwise.
     */
    @Override
    public boolean onCreate() {
        mOpenHelper = new DatabaseHelper(getContext());
        return true;
    }


	// ******************************************************************** //
	// Content Access Methods.
	// ******************************************************************** //
    
    /**
     * Return the MIME type of the data at the given URI.
     *
     * @param	uri			The URI to query.
     * @return				A MIME type string, or null if there is no type.
     */
    @Override
    public String getType(Uri uri) {
        switch (sUriMatcher.match(uri)) {
        case FORMULAE:
            return Formula.Formulae.CONTENT_TYPE;
        case FORMULA_ID:
            return Formula.Formulae.CONTENT_ITEM_TYPE;
        default:
            throw new IllegalArgumentException("Unknown URI " + uri);
        }
    }
    

    /**
     * Receives a query request from a client in a local process, and
     * returns a Cursor.
     *
     * @param	uri			The URI to query.  This will be the full URI sent
     * 						by the client.
     * @param	projection	The list of columns to put into the cursor.  If
     *      				null all columns are included.
     * @param	selection	A selection criteria to apply when filtering rows.
     *      				If null then all rows are included.
     * @param	sortOrder	How the rows in the cursor should be sorted.
     *        				If null then the provider is free to define
     *        				the sort order.
     * @return				A Cursor or null.
     */
    @Override
    public Cursor query(Uri uri, String[] projection,
    					String selection, String[] selectionArgs,
    					String sortOrder)
    {
        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();

        switch (sUriMatcher.match(uri)) {
        case FORMULAE:
            qb.setTables(FORMULAE_TABLE_NAME);
//            qb.setProjectionMap(formulaeProjectionMap);
            break;

        case FORMULA_ID:
            qb.setTables(FORMULAE_TABLE_NAME);
//            qb.setProjectionMap(formulaeProjectionMap);
            qb.appendWhere(Formula.Formulae._ID + "=" + uri.getPathSegments().get(1));
            break;

        default:
            throw new IllegalArgumentException("Unknown URI " + uri);
        }

        // If no sort order is specified, use the default,
        String orderBy;
        if (TextUtils.isEmpty(sortOrder))
            orderBy = Formula.Formulae.DEFAULT_SORT_ORDER;
        else
            orderBy = sortOrder;

        // Get the database and run the query.
        SQLiteDatabase db = mOpenHelper.getReadableDatabase();
        Cursor c = qb.query(db, projection,
        					selection, selectionArgs,
        				    null, null, orderBy);

        // Tell the cursor what uri to watch, so it knows when its
        // source data changes.
        c.setNotificationUri(getContext().getContentResolver(), uri);
        return c;
    }


    /**
     * Implement this to insert a new row.
     * 
     * @param	uri				The content:// URI of the insertion request.
     * @param	initialValues	A set of column_name/value pairs to add to the
     * 							database.
     * @return					The URI for the newly inserted item.
     */
    @Override
    public Uri insert(Uri uri, ContentValues initialValues) {
        // Validate the requested uri.
        if (sUriMatcher.match(uri) != FORMULAE)
            throw new IllegalArgumentException("Unknown URI " + uri);

        ContentValues values;
        if (initialValues != null)
            values = new ContentValues(initialValues);
        else
            values = new ContentValues();

        Long now = Long.valueOf(System.currentTimeMillis());

        // Make sure that the fields are all set.
        if (!values.containsKey(Formula.Formulae.CREATED_DATE))
            values.put(Formula.Formulae.CREATED_DATE, now);
        if (!values.containsKey(Formula.Formulae.USED_DATE))
            values.put(Formula.Formulae.USED_DATE, now);
        if (!values.containsKey(Formula.Formulae.VALID))
            values.put(Formula.Formulae.VALID, false);
        if (!values.containsKey(Formula.Formulae.TITLE))
            values.put(Formula.Formulae.TITLE, "");
        if (!values.containsKey(Formula.Formulae.FORMULA))
            values.put(Formula.Formulae.FORMULA, "");

        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        long rowId = db.insert(FORMULAE_TABLE_NAME, Formula.Formulae.FORMULA, values);
        if (rowId < 0)
        	throw new SQLException("Failed to insert row into " + uri);

        Uri formUri = ContentUris.withAppendedId(Formula.Formulae.CONTENT_URI, rowId);
        getContext().getContentResolver().notifyChange(formUri, null);
        return formUri;
    }

    
    /**
     * A request to delete one or more rows.  The selection clause is
     * applied when performing the deletion, allowing the operation to
     * affect multiple rows in a directory.
     *
     * @param	uri				The full URI to query, including a row ID
     * 							(if a specific record is requested).
     * @param	where			An optional restriction to apply to rows
     * 							when deleting.
     * @return					The number of rows affected.
     * @throws	SQLException	Some database error occurred.
     */
    @Override
    public int delete(Uri uri, String where, String[] whereArgs) {
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        int count;
        switch (sUriMatcher.match(uri)) {
        case FORMULAE:
            count = db.delete(FORMULAE_TABLE_NAME, where, whereArgs);
            break;
        case FORMULA_ID:
            String formId = uri.getPathSegments().get(1);
            count = db.delete(FORMULAE_TABLE_NAME,
            				  Formula.Formulae._ID + "=" + formId +
            				  (!TextUtils.isEmpty(where) ? " AND (" + where + ')' : ""),
            				  whereArgs);
            break;
        default:
            throw new IllegalArgumentException("Unknown URI " + uri);
        }

        getContext().getContentResolver().notifyChange(uri, null);
        return count;
    }

    
    /**
     * Update a content URI.  All rows matching the optionally provided
     * selection will have their columns listed as the keys in the values
     * map with the values of those keys.
     *
     * @param	uri				The URI to update.  This can potentially have
     * 							a record ID if this is an update request for
     * 							a specific record.
     * @param	values			A ContentValues containing the new column
     * 							values (NULL is a valid value).
     * @param	where			An optional filter to match rows to update.
     * return					The number of rows affected.
     */
    @Override
    public int update(Uri uri, ContentValues values, String where, String[] whereArgs) {
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        int count;
        switch (sUriMatcher.match(uri)) {
        case FORMULAE:
            count = db.update(FORMULAE_TABLE_NAME, values, where, whereArgs);
            break;
        case FORMULA_ID:
            String formId = uri.getPathSegments().get(1);
            count = db.update(FORMULAE_TABLE_NAME,
            				  values,
            				  Formula.Formulae._ID + "=" + formId +
            				  (!TextUtils.isEmpty(where) ? " AND (" + where + ')' : ""),
            				  whereArgs);
            break;
        default:
            throw new IllegalArgumentException("Unknown URI " + uri);
        }

        getContext().getContentResolver().notifyChange(uri, null);
        return count;
    }

    
	// ******************************************************************** //
	// Private Classes.
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
            db.execSQL("CREATE TABLE " + FORMULAE_TABLE_NAME + " (" +
                       Formula.Formulae._ID + " INTEGER PRIMARY KEY," +
                       Formula.Formulae.TITLE + " TEXT," +
                       Formula.Formulae.FORMULA + " TEXT," +
                       Formula.Formulae.CREATED_DATE + " INTEGER," +
                       Formula.Formulae.USED_DATE + " INTEGER," +
                       Formula.Formulae.VALID + " INTEGER" +
                       ");");
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            Log.w(TAG, "Upgrading database from version " + oldVersion + " to "
                    + newVersion + ", which will destroy all old data");
            db.execSQL("DROP TABLE IF EXISTS " + FORMULAE_TABLE_NAME);
            onCreate(db);
        }
    }


	// ******************************************************************** //
	// Private Constants.
	// ******************************************************************** //

    // Debugging tag.
	@SuppressWarnings("unused")
	private static final String TAG = "formula";
    
    private static final String DATABASE_NAME = "formulae.db";
    private static final int DATABASE_VERSION = 3;
    private static final String FORMULAE_TABLE_NAME = "formulae";

//    private static HashMap<String, String> formulaeProjectionMap;
//    static {
//        formulaeProjectionMap = new HashMap<String, String>();
//        formulaeProjectionMap.put(Formula.Formulae._ID, Formula.Formulae._ID);
//        formulaeProjectionMap.put(Formula.Formulae.TITLE, Formula.Formulae.TITLE);
//        formulaeProjectionMap.put(Formula.Formulae.FORMULA, Formula.Formulae.FORMULA);
//        formulaeProjectionMap.put(Formula.Formulae.CREATED_DATE, Formula.Formulae.CREATED_DATE);
//        formulaeProjectionMap.put(Formula.Formulae.MODIFIED_DATE, Formula.Formulae.MODIFIED_DATE);
//    }

    private static final int FORMULAE = 1;
    private static final int FORMULA_ID = 2;

    private static final UriMatcher sUriMatcher;
    static {
        sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        sUriMatcher.addURI(Formula.AUTHORITY, "formulae", FORMULAE);
        sUriMatcher.addURI(Formula.AUTHORITY, "formulae/#", FORMULA_ID);
    }


	// ******************************************************************** //
	// Private Data.
	// ******************************************************************** //
    
    private DatabaseHelper mOpenHelper;

}

