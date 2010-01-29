
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

import java.util.Random;

import net.goui.util.MTRandom;


/**
 * Interface defining a colour palette.
 */
public abstract class Palette {
    
    /**
     * Set up this instance.  Subclasses must pass in the array of colours.
     * We take care of everything else.
     * 
     * @param   colours     The colours that make up this palette.
     */
    protected Palette(int[] colours) {
        colourList = colours;
    }

    /**
     * Get the number of colours in this palette.
     * 
     * @return              The number of colours in this palette.
     */
    public int size() {
        return colourList.length;
    }

    /**
     * Get a specified colour from this palette.
     * 
     * @param   index       Index of the colour to get.
     * @return              The selected colour.
     * @throws  ArrayIndexOutOfBoundsException Invalid index.
     */
    public int get(int index) throws ArrayIndexOutOfBoundsException {
        return colourList[index];
    }

    /**
     * Get a random colour from this palette.
     * 
     * @return              A radomly selected colour.
     */
    public int getRandom() {
        return colourList[rng.nextInt(colourList.length)];
    }


    // ******************************************************************** //
    // Class Data.
    // ******************************************************************** //

    // Random number generator.  We use a Mersenne Twister,
    // which is a high-quality and fast implementation of java.util.Random.
    private static final Random rng = new MTRandom();


    // ******************************************************************** //
    // Private Data.
    // ******************************************************************** //

    // The palette data.
    private final int[] colourList;

}

