
/**
 * org.hermit.android.provider: classes for building content providers.
 * 
 * These classes are designed to help build content providers in Android.
 *
 * <br>Copyright 2010-2011 Ian Cameron Smith
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

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import org.hermit.android.provider.TableSchema.FieldDesc;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;


/**
 * Class encapsulating the schema for a content provider.  Applications
 * must subclass this, and provide the necessary information in the
 * call to this base class's constructor.
 * 
 * <p>An application's subclass will typically provide the following:
 * 
 * <ul>
 * <li>Inner classes which are subclasses of {@link TableSchema},
 *     defining the schemas of the individual tables.
 * <li>A constructor which calls this class's constructor, passing the
 *     required information.
 * </ul>
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
	// Backup and Restore.
    // ******************************************************************** //

    public void backupDb(Context c, File where)
    	throws FileNotFoundException, IOException
    {
    	File bak = new File(where, dbName + ".bak");

    	FileOutputStream fos = null;
    	DataOutputStream dos = null;
    	try {
    		fos = new FileOutputStream(bak);
    		dos = new DataOutputStream(fos);
    		
    		ContentResolver cr = c.getContentResolver();
    		TableSchema[] tables = getDbTables();
    		for (TableSchema t : tables)
    			backupTable(cr, t, dos);
    	} finally {
    		if (dos != null) try {
    			dos.close();
    		} catch (IOException e) { }
    		if (fos != null) try {
    			fos.close();
    		} catch (IOException e) { }
    	}
    }


    private void backupTable(ContentResolver cr,
    						 TableSchema ts, DataOutputStream dos)
    	throws IOException
    {
		// Create a where clause based on the backup mode.
		String where = null;
		String[] wargs = null;

		// Query for the records to back up.
		Cursor c = null;
		try {
			c = cr.query(ts.getContentUri(), ts.getDefaultProjection(),
						 where, wargs, ts.getSortOrder());
			
			// If there's no data, do nothing.
			if (!c.moveToFirst())
				return;
			
			// Get the column indices for all the columns.
			FieldDesc[] fields = ts.getTableFields();
			int[] cols = new int[fields.length];
			for (int i = 0; i < fields.length; ++i)
				cols[i] = c.getColumnIndex(fields[i].name);
			
			// Save all the rows.
			while (!c.isAfterLast()) {
				for (int i = 0; i < fields.length; ++i) {
					TableSchema.FieldType t = fields[i].type;
					switch (t) {
					case BIGINT:
						long lv = c.getLong(cols[i]);
						dos.writeLong(lv);
						break;
					case INT:
						int iv = c.getInt(cols[i]);
						dos.writeInt(iv);
						break;
					case DOUBLE:
					case FLOAT:
						double dv = c.getDouble(cols[i]);
						dos.writeDouble(dv);
						break;
					case REAL:
						float fv = c.getFloat(cols[i]);
						dos.writeFloat(fv);
						break;
					case BOOLEAN:
						boolean bv = c.getInt(cols[i]) != 0;
						dos.writeBoolean(bv);
						break;
					case TEXT:
						String sv = c.getString(cols[i]);
						dos.writeUTF(sv);
						break;
					}
				}
				
				c.moveToNext();
			}
		} finally {
			c.close();
		}
	}
	

    private void restoreTable(ContentResolver cr,
    						 TableSchema ts, DataInputStream dis)
    	throws IOException
    {
    	// Get the column indices for all the columns.
    	FieldDesc[] fields = ts.getTableFields();

    	// Save all the rows.
    	ContentValues values = new ContentValues();
    	while (!false) { // FIXME
    		for (int i = 0; i < fields.length; ++i) {
    			TableSchema.FieldType t = fields[i].type;
    			switch (t) {
    			case BIGINT:
    				values.put(fields[i].name, dis.readLong());
    				break;
    			case INT:
    				values.put(fields[i].name, dis.readInt());
    				break;
    			case DOUBLE:
    			case FLOAT:
    				values.put(fields[i].name, dis.readDouble());
    				break;
    			case REAL:
    				values.put(fields[i].name, dis.readFloat());
    				break;
    			case BOOLEAN:
    				values.put(fields[i].name, dis.readBoolean());
    				break;
    			case TEXT:
    				values.put(fields[i].name, dis.readUTF());
    				break;
    			}
    		}

    		cr.insert(ts.getContentUri(), values);
    	}
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

