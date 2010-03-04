
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


import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;
import android.util.Log;


/**
 * This class helps open, create, and upgrade the database file.
 */
public class DatabaseHelper
    extends SQLiteOpenHelper
{
    
    public DatabaseHelper(Context context, DbSchema schema) {
        super(context, schema.getDbName(), null, schema.getDbVersion());
        dbSchema = schema;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        StringBuilder qb = new StringBuilder();
        for (TableSchema t : dbSchema.getDbTables()) {
            qb.setLength(0);
            qb.append("CREATE TABLE " + t.getTableName() + " ( ");
            qb.append(BaseColumns._ID + " INTEGER PRIMARY KEY");
            for (String[] field : t.getTableFields()) {
                qb.append(", ");
                qb.append(field[0]);
                qb.append(" ");
                qb.append(field[1]);
            }
            qb.append(");");
            db.execSQL(qb.toString());
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.w(TableProvider.TAG, "Upgrading database from version " + oldVersion + " to "
                + newVersion + ", which will destroy all old data");
        for (TableSchema t : dbSchema.getDbTables())
            db.execSQL("DROP TABLE IF EXISTS " + t.getTableName());
        onCreate(db);
    }
    
    private final DbSchema dbSchema;
    
}

