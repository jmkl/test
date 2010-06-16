
/**
 * broadtest: system broadcast dumper.
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


package org.hermit.broadtest;

import java.util.Set;

import org.hermit.broadtest.provider.BroadcastSchema;


import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;


/**
 * A BroadcastReceiver that listens for relevant system updates and
 * saves them to the content provider.
 */
public class SystemBroadcastReceiver
    extends BroadcastReceiver
{
    
    // ******************************************************************** //
    // Broadcast Handling.
    // ******************************************************************** //

    /**
     * Receives and processes a broadcast intent.
     *
     * @param   context     Our context.
     * @param   intent      The intent.
     */
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "B/C intent=" + intent);
        
        ContentValues fields = new ContentValues();
        fields.put(BroadcastSchema.BroadcastTable.TITLE, intent.getAction());
        Bundle extras = intent.getExtras();
        fields.put(BroadcastSchema.BroadcastTable.EXTRAS, extrasToString(extras));
        fields.put(BroadcastSchema.BroadcastTable.TIME, System.currentTimeMillis());
        
        ContentResolver resolver = context.getContentResolver();
        resolver.insert(BroadcastSchema.BroadcastTable.CONTENT_URI, fields);
    }
    
    
    private static final String extrasToString(Bundle extras) {
    	if (extras == null)
    		return "";
    	
    	String val = "";
    	Set<String> keys = extras.keySet();
    	for (String key : keys)
    		val += key + "=" + extras.get(key);
    	
    	return val;
    }
    
    
    // ******************************************************************** //
    // Class Data.
    // ******************************************************************** //

    // Debugging tag.
    private static final String TAG = BroadcastDumper.TAG;

}

