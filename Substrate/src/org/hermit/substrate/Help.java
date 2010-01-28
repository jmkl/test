
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


package org.hermit.substrate;


import org.hermit.android.core.HelpActivity;

import android.os.Bundle;


/**
 * Simple help activity for Substrate.
 */
public class Help
	extends HelpActivity
{

    /**
     * Called when the activity is starting.  This is where most
     * initialization should go: calling setContentView(int) to inflate
     * the activity's UI, etc.
     */
    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        
        // Load the preferences from an XML resource
        addHelpFromArrays(R.array.help_titles, R.array.help_texts);
    }

}

