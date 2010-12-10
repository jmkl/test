
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

import org.hermit.dazzle.DazzleProvider.Control;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.preference.PreferenceManager;
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

        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
        	// Update system settings from shadow copy, no other way to do it.
        	// For now we have only radio/mobile data settings.
        	if (Build.VERSION.SDK_INT < Build.VERSION_CODES.GINGERBREAD
        			&& hasRadioControlsEnabled(context)) {
        		Log.d(TAG, "Restoring radio/mobile data settings from shadow copy");
        		PhoneRadioSettings.onBoot(context, intent);
        	}
        }
        // For any broadcast we're registered for, just update all the widgets.
        DazzleProvider.updateAllWidgets(context);
    }
    
    private boolean hasRadioControlsEnabled(final Context context) {
    	boolean hasRadioControls = false;
    	final SharedPreferences prefs
    			= PreferenceManager.getDefaultSharedPreferences(context);
    	for (final String key : prefs.getAll().keySet()) {
    		if (key.startsWith(Control.MOBILE_DATA.pref)
    				|| key.startsWith(Control.PHONE_RADIO.pref)) {
    			hasRadioControls = true;
    		}
    	}
    	return hasRadioControls;
    }

    // ******************************************************************** //
    // Class Data.
    // ******************************************************************** //

    // Debugging tag.
    @SuppressWarnings("unused")
    private static final String TAG = DazzleProvider.TAG;

}

