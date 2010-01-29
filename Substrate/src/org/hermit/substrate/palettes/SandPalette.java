
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


import org.hermit.substrate.Palette;


/**
 * A colour palette that came in the Sand Traveller hack.
 */
public class SandPalette
    extends Palette
{

    // ******************************************************************** //
    // Constructor.
    // ******************************************************************** //

    /**
     * Create an instance of this palette.
     */
    public SandPalette() {
        super(SAND_TRAVELLER);
    }


    // ******************************************************************** //
    // Class Data.
    // ******************************************************************** //

    // The palette data.
    private static final int[] SAND_TRAVELLER = {
        0x3a242b, 0x3b2426, 0x352325, 0x836454, 0x7d5533,
        0x8b7352, 0xb1a181, 0xa4632e, 0xbb6b33, 0xb47249,
        0xca7239, 0xd29057, 0xe0b87e, 0xd9b166, 0xf5eabe,
        0xfcfadf, 0xd9d1b0, 0xfcfadf, 0xd1d1ca, 0xa7b1ac,
        0x879a8c, 0x9186ad, 0x776a8e, 0x000000, 0x000000,
        0x000000, 0x000000, 0x000000, 0xFFFFFF, 0xFFFFFF,
        0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0x000000, 0x000000,
        0x000000, 0x000000, 0x000000, 0xFFFFFF, 0xFFFFFF,
        0xFFFFFF, 0xFFFFFF, 0xFFFFFF,
    };

}

