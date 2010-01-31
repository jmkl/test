
/**
 * Substrate: a collection of eye candies for Android.  Various screen
 * hacks from the xscreensaver collection can be viewed standalone, or
 * set as live wallpapers.
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


package org.hermit.substrate.hacks;


import org.hermit.substrate.R;

import android.os.Bundle;
import android.preference.PreferenceActivity;


/**
 * Preferences activity for the NodeGarden live wallpaper.
 */
public class NodeGardenPreferences
    extends PreferenceActivity
{

    /**
     * Called when the activity is starting.  This is where most
     * initialization should go: calling setContentView(int) to inflate
     * the activity's UI, etc.
     * 
     * @param   icicle          Activity's saved state, if any.
     */
    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        
        // Set up the key we're using for the wallpaper prefs.
        getPreferenceManager().setSharedPreferencesName(
                                                Substrate.SHARED_PREFS_NAME);
        
        // Load the preferences from XML resources -- both common and
        // hack-specific settings.
        addPreferencesFromResource(R.xml.eyecandy_settings);
        addPreferencesFromResource(R.xml.nodes_settings);
    }

}

