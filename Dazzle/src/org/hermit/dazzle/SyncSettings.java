
/**
 * Dazzle: a screen brightness control widget for Android.
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


package org.hermit.dazzle;


import android.content.ContentResolver;
import android.content.Context;
import android.net.ConnectivityManager;
import android.util.Log;
import android.widget.RemoteViews;


/**
 * This static class provides utilities to manage the sync state.
 */
public class SyncSettings
{

    // ******************************************************************** //
    // Constructor.
    // ******************************************************************** //

    /**
     * Constructor -- hidden, as this class is non-instantiable.
     */
    private SyncSettings() {
    }
    

    // ******************************************************************** //
    // Status Handling.
    // ******************************************************************** //

    /**
     * Toggle the current state.
     * 
     * @param   context     The context we're running in.
     */
    static void toggle(Context context) {
        Log.i(TAG, "toggle Sync");
        
        // Just toggle sync.
        boolean sync = ContentResolver.getMasterSyncAutomatically();
        ContentResolver.setMasterSyncAutomatically(!sync);
    }


    /**
     * Set the indicator widget to represent our current state.
     * 
     * @param   context     The context we're running in.
     * @param   views       The widget view to modify.
     * @param   widget      The ID of the indicator widget.
     */
    static void setWidget(Context context, RemoteViews views, int widget) {
        // boolean backgroundData = getBackgroundDataState(context);
        boolean sync = ContentResolver.getMasterSyncAutomatically();
        
        int image = sync ? R.drawable.green : R.drawable.grey;

        views.setImageViewResource(widget, image);
    }

    
    /**
     * Gets the state of background data.
     *
     * @param context
     * @return true if enabled
     */
    private static boolean getBackgroundDataState(Context context) {
        ConnectivityManager connManager =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        return connManager.getBackgroundDataSetting();
    }

    
    // ******************************************************************** //
    // Class Data.
    // ******************************************************************** //

    // Debugging tag.
    @SuppressWarnings("unused")
    private static final String TAG = DazzleProvider.TAG;

}

