
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
import android.content.Intent;
import android.provider.Settings;
import android.util.Log;
import android.widget.RemoteViews;


/**
 * This static class provides utilities to manage airplane mode.
 */
public class AirplaneSettings
{

    // ******************************************************************** //
    // Constructor.
    // ******************************************************************** //

    /**
     * Constructor -- hidden, as this class is non-instantiable.
     */
    private AirplaneSettings() {
    }
    

    // ******************************************************************** //
    // Status Handling.
    // ******************************************************************** //

    /**
     * Called after onRestoreInstanceState(Bundle), onRestart(), or onPause(),
     * for your activity to start interacting with the user.  This is a good
     * place to begin animations, open exclusive-access devices (such as the
     * camera), etc.
     * 
     * Derived classes must call through to the super class's implementation
     * of this method.  If they do not, an exception will be thrown.
     */
    static void toggle(Context context) {
        Log.i(TAG, "toggle airplane");

        // We need to toggle the mode and also broadcast the fact.
        ContentResolver cr = context.getContentResolver();
        boolean on = Settings.System.getInt(cr, Settings.System.AIRPLANE_MODE_ON, 0) != 0;
        Settings.System.putInt(cr,
                               Settings.System.AIRPLANE_MODE_ON, 
                               on ? 0 : 1);

        // Post the intent.
        Intent intent = new Intent(Intent.ACTION_AIRPLANE_MODE_CHANGED);
        intent.putExtra("state", !on);
        context.sendBroadcast(intent);
    }


    static void setWidget(Context context, RemoteViews views, int widget) {
        ContentResolver cr = context.getContentResolver();
        boolean on = Settings.System.getInt(cr, Settings.System.AIRPLANE_MODE_ON, 0) != 0;
        int image = on ? R.drawable.red : R.drawable.grey;
        views.setImageViewResource(widget, image);
    }


    // ******************************************************************** //
    // Class Data.
    // ******************************************************************** //

    // Debugging tag.
    @SuppressWarnings("unused")
    private static final String TAG = DazzleProvider.TAG;

}

