
/**
 * org.hermit.android.provider: classes for building content providers.
 * 
 * These classes are designed to help build content providers in Android.
 *
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


package org.hermit.android.provider;


import java.util.HashMap;

import android.content.ContentValues;
import android.net.Uri;
import android.provider.BaseColumns;


/**
 * Class encapsulating the schema for a table within a content provider.
 * Applications must subclass this, and provide the necessary information
 * in the call to this base class's constructor.
 * 
 * <p>An application's subclass will typically provide the following:
 * 
 * <ul>
 * <li>A <code>public static final Uri CONTENT_URI</code> field, defining
 *     the content URI for the table.
 * <li>A <code>public static final String SORT_ORDER</code> field, defining
 *     the default sort clause for the table.
 * <li>For each column in the table, a <code>public static final String</code>
 *     field defining the column's database name.
 * <li>A <code>public static final String[] PROJECTION</code> field, defining
 *     the default projection for the table.
 * <li>A constructor which calls this class's constructor, passing the
 *     required information.
 * </ul>
 */
public abstract class TableSchema
    implements BaseColumns
{

    // ******************************************************************** //
    // Constructor.
    // ******************************************************************** //

    /**
     * Create a table schema instance.
     * 
     * @param   name        Name for the table; e.g. "points".
     * @param   type        Base MIME type identifying the content of this
     *                      table; e.g. "vnd.hermit.org.passage.point".
     * @param   uri         Content URI for this table.
     * @param   sort        Default sort order for this table; e.g.
     *                      "time ASC".
     * @param   fields      List of field definitions.  Each one is two
     *                      strings, being the field name and type.  E.g.
     *                      { { "name", "TEXT" }, { "time", "INTEGER" }}.
     *                      The standard ID field "_id" will be prepended
     *                      automatically.
     * @param   projection  Default projection for this table.
     */
    protected TableSchema(String name, String type,
                          Uri uri, String sort,
                          String[][] fields, String[] projection)
    {
        tableName = name;
        itemType = type;
        contentUri = uri;
        sortOrder = sort;
        fieldDefs = fields;
        defProjection = projection;
    }


    // ******************************************************************** //
    // Setup.
    // ******************************************************************** //

    /**
     * Init function called when this table has been added to a database.
     * 
     * @param   db          Parent database.
     */
    void init(DbSchema db) {
        // Create the projection map, and all-fields projection.
        projectionMap = new HashMap<String, String>();

        // Add the implicit ID field.
        projectionMap.put(BaseColumns._ID, BaseColumns._ID);

        for (String[] field : fieldDefs)
            projectionMap.put(field[0], field[0]);
    }


    // ******************************************************************** //
    // Utilities.
    // ******************************************************************** //

    /**
     * This method creates a projection from a set of field definitions.
     * It can be used by subclasses to set up a default projection.  The
     * returned projection includes all fields, including the implicit
     * "_id" field, which should <b>not</b> be in the supplied field list.
     * 
     * @param   fields      List of field definitions.  Each one is two
     *                      strings, being the field name and type.  E.g.
     *                      { { "name", "TEXT" }, { "time", "INTEGER" }}.
     *                      The standard ID field "_id" will be prepended
     *                      automatically.
     * @return              An all-fields projection for the given fields
     *                      list.
     */
    protected static String[] makeProjection(String[][] fields) {
        String[] projection = new String[fields.length + 1];
        int np = 0;
        
        projection[np++] = BaseColumns._ID;
        for (String[] field : fields)
            projection[np++] = field[0];
        
        return projection;
    }
    

    // ******************************************************************** //
    // Public Accessors.
    // ******************************************************************** //

    /**
     * Get the table name.
     * 
     * @return          The table's name in the database.
     */
    public String getTableName() {
        return tableName;
    }


    /**
     * Get the table's content URI.
     * 
     * @return          The "content://" content URI for this table. 
     */
    public Uri getContentUri() {
        return contentUri;
    }


    /**
     * Get the MIME type for the table as a whole.
     * 
     * @return          The "vnd.android.cursor.dir/" MIME type for the table.
     */
    public String getTableType() {
        return "vnd.android.cursor.dir/" + itemType;
    }


    /**
     * Get the MIME type for the items in the table.
     * 
     * @return          The "vnd.android.cursor.item/" MIME type for the items.
     */
    public String getItemType() {
        return "vnd.android.cursor.item/" + itemType;
    }


    /**
     * Get the default projection.  The returned projection includes all
     * fields, including the implicit "_id" field.
     * 
     * @return              An all-fields projection for this table.
     */
    public String[] getDefaultProjection() {
        return defProjection;
    }
    

    // ******************************************************************** //
    // Event Handlers.
    // ******************************************************************** //

    /**
     * This method is called when a new row is added into this table.
     * Subclasses can override this to fill in any missing values.
     * 
     * @param    values      The fields being added.
     */
    public void onInsert(ContentValues values) {

    }


    // ******************************************************************** //
    // Local Accessors.
    // ******************************************************************** //

    /**
     * @return the tableFields
     */
    String[][] getTableFields() {
        return fieldDefs;
    }


    /**
     * Get the table's default sort order.
     * 
     * @return           Default sort order.
     */
    String getSortOrder() {
        return sortOrder;
    }


    /**
     * Get the table's null hack field.
     * 
     * @return           A field which can safely be set to NULL if no
     *                   fields at all are present.
     */
    String getNullHack() {
        return getTableFields()[0][0];
    }


    /**
     * Get the table's projection map.
     * 
     * @return           Projection map.
     */
    HashMap<String, String> getProjectionMap() {
        return projectionMap;
    }


    // ******************************************************************** //
    // Private Data.
    // ******************************************************************** //

    // Table's name, item type, and sort order.
    private final String tableName;
    private final String itemType;
    private final String sortOrder;

    // Content URI for this table.
    private final Uri contentUri;

    // Definitions of the fields.
    private final String[][] fieldDefs;

    // The default projection for this table.
    private final String[] defProjection;

    // Projection map for this table.
    private HashMap<String, String> projectionMap;

}

