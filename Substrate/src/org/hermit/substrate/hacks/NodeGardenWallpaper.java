
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

import org.hermit.substrate.EyeCandy;
import org.hermit.substrate.EyeCandyWallpaper;

import android.content.Context;


/**
 * A live wallpaper based on the NodeGarden hack.
 */
public class NodeGardenWallpaper
    extends EyeCandyWallpaper
{

    /**
     * This method is invoked to create an instance of the eye candy
     * this wallpaper displays.  Subclasses must implement this to
     * return the appropriate hack.
     * 
     * @param  context      Our application context.
     * @return              A new instance of the eye candy to display.
     */
    @Override
    public EyeCandy onCreateHack(Context context) {
        return new NodeGarden(context);
    }

}

