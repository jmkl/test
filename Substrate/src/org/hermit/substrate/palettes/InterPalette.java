
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


package org.hermit.substrate.palettes;


import java.util.Random;

import org.hermit.substrate.Palette;

import net.goui.util.MTRandom;


/**
 * A colour palette that came in the InterAggregate hack.
 */
public class InterPalette
    implements Palette
{

    // ******************************************************************** //
    // Public Methods.
    // ******************************************************************** //

    /**
     * Get a random colour from this palette.
     * 
     * @return          A radomly selected colour.
     */
    public int getRandom() {
        return INTERAGGREGATE[MT_RANDOM.nextInt(INTER_SIZE)];
    }


    // ******************************************************************** //
    // Class Data.
    // ******************************************************************** //

    // Random number generator.  We use a Mersenne Twister,
    // which is a high-quality and fast implementation of java.util.Random.
    private static final Random MT_RANDOM = new MTRandom();

    // The palette data.
    private static final int[] INTERAGGREGATE = {
        0xFFFFFF,
        0x000000,
        0x000000,
        0x4e3e2e,
        0x694d35,
        0xb0a085,
        0xe6d3ae,
    };
    private static final int INTER_SIZE = INTERAGGREGATE.length;

}

