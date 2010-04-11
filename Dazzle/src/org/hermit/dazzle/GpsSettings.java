
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


import android.content.Context;
import android.widget.RemoteViews;


/**
 * This static class provides utilities to manage the GPS state.
 */
public class GpsSettings
{

    // ******************************************************************** //
    // Constructor.
    // ******************************************************************** //

    /**
     * Constructor -- hidden, as this class is non-instantiable.
     */
    private GpsSettings() {
    }
    

    // ******************************************************************** //
    // Status Handling.
    // ******************************************************************** //

    static void setWidget(Context context, RemoteViews views, int widget) {
        // Sadly, there's nothing we can do here.
    }


    // ******************************************************************** //
    // Class Data.
    // ******************************************************************** //

    // Debugging tag.
    @SuppressWarnings("unused")
    private static final String TAG = DazzleProvider.TAG;

}

