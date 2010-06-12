
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


/**
 * Class encapsulating the schema for a content provider.  Applications
 * must subclass this, and provide the necessary information in the
 * call to this base class's constructor.
 */
public abstract class DbSchema {

    // ******************************************************************** //
    // Constructor.
    // ******************************************************************** //

    /**
     * Create a database schema instance.
     * 
     * @param   name        Name for the database; e.g. "passages".
     * @param   version     Version number of the database.  The upgrade
     *                      process will be run when this increments.
     * @param   auth        Authority name for this content provider; e.g.
     *                      "org.hermit.provider.PassageData".
     * @param   tables      List of table schemas.
     */
    protected DbSchema(String name, int version, String auth, TableSchema[] tables) {
        dbName = name;
        dbVersion = version;
        dbAuth = auth;
        dbTables = tables;
        
        for (TableSchema t : getDbTables())
            t.init(this);
    }
    

    // ******************************************************************** //
    // Public Accessors.
    // ******************************************************************** //
    
    /**
     * Get the database name.
     * 
     * @return              The name of the database.
     */
    public String getDbName() {
        return dbName;
    }

    
    /**
     * Get the database version number.
     * 
     * @return              The database version number.
     */
    public int getDbVersion() {
        return dbVersion;
    }
    

    // ******************************************************************** //
    // Local Accessors.
    // ******************************************************************** //
    
    /**
     * Get the content provider authority string.
     * 
     * @return              The authority string.
     */
    String getDbAuth() {
        return dbAuth;
    }


    /**
     * Get the database table schemas.
     * 
     * @return              The table schemas.
     */
    TableSchema[] getDbTables() {
        return dbTables;
    }


    /**
     * Get the schema for a specified table.
     * 
     * @param   name            The name of the table we want.
     * @return                  The schema for the given table.
     * @throws  IllegalArgumentException  No such table.
     */
    protected TableSchema getTable(String name)
        throws IllegalArgumentException
    {
        for (TableSchema t : dbTables)
            if (t.getTableName().equals(name))
                return t;
        throw new IllegalArgumentException("No such table: " + name);
    }


    // ******************************************************************** //
    // Private Data.
    // ******************************************************************** //

    // Database name and version.
    private final String dbName;
    private final int dbVersion;
    
    // Content provider authority.
    private final String dbAuth;
    
    // Definitions of our tables.
    private final TableSchema[] dbTables;
    
}

