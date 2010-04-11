
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

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.util.Log;


/**
 * A BroadcastReceiver that listens for relevant system updates.  This
 * receiver starts off disabled, and we only enable it when there is a widget
 * instance created, in order to only receive notifications when we need them.
 */
public class SystemBroadcastReceiver
    extends BroadcastReceiver
{

    // ******************************************************************** //
    // Public Constants.
    // ******************************************************************** //

    /**
     * Our component name.
     */
    static ComponentName COMP_NAME =
            new ComponentName("org.hermit.dazzle",
                              "org.hermit.dazzle.SystemBroadcastReceiver");

    
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
        Log.d(TAG, "SystemB/C intent=" + intent);

        // For any broadcast we're registered for, just update all the widgets.
        DazzleProvider.updateAllWidgets(context);
    }
    
    
    // ******************************************************************** //
    // Class Data.
    // ******************************************************************** //

    // Debugging tag.
    @SuppressWarnings("unused")
    private static final String TAG = DazzleProvider.TAG;

}

