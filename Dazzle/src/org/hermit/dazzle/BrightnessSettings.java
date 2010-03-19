
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
import android.provider.Settings;


/**
 * This static class provides utilities to read and write the screen
 * brightness settings in the system settings content provider.
 */
class BrightnessSettings
{

    // ******************************************************************** //
    // Public Constants.
    // ******************************************************************** //

    // BrightnessSettings values: fully off, dim, fully on.
    private static final int SETTING_DIM = 20;
    private static final int SETTING_MAX = 255;

    // BrightnessSettings values: fully off, dim, fully on.
    static final float BRIGHTNESS_OFF = 0.00f;
    static final float BRIGHTNESS_DIM = 0.08f;
    static final float BRIGHTNESS_MAX = 1.00f;
    

    // ******************************************************************** //
    // Constructor.
    // ******************************************************************** //

    /**
     * Constructor -- hidden, as this class is non-instantiable.
     */
    private BrightnessSettings() {
    }
    

    // ******************************************************************** //
    // Screen BrightnessSettings Handling.
    // ******************************************************************** //

    static boolean isAuto(Context context) {
        int mode = Settings.System.getInt(context.getContentResolver(),
                SCREEN_BRIGHTNESS_MODE, SCREEN_BRIGHTNESS_MODE_MANUAL);
        return mode == SCREEN_BRIGHTNESS_MODE_AUTOMATIC;
    }


    static float getBrightness(Context context) {
        int lev = Settings.System.getInt(context.getContentResolver(),
                                         Settings.System.SCREEN_BRIGHTNESS,
                                         SETTING_DIM);
        return (float) lev / (float) SETTING_MAX;
    }


    static String getModeString(Context context) {
        ContentResolver resolver = context.getContentResolver();
        
        int mode = Settings.System.getInt(resolver,
                                          SCREEN_BRIGHTNESS_MODE,
                                          SCREEN_BRIGHTNESS_MODE_MANUAL);
        boolean auto = mode == SCREEN_BRIGHTNESS_MODE_AUTOMATIC;
        int bright = Settings.System.getInt(resolver,
                                            Settings.System.SCREEN_BRIGHTNESS,
                                            SETTING_DIM);
        
        bright = Math.round((float) bright / (float) SETTING_MAX * 100f);
        return auto ? "A" : "" + bright + "%";
    }


    static void setMode(Context context, boolean auto, float brightness) {
        ContentResolver resolver = context.getContentResolver();
        
        int mode = auto ? SCREEN_BRIGHTNESS_MODE_AUTOMATIC : SCREEN_BRIGHTNESS_MODE_MANUAL;
        Settings.System.putInt(resolver,
                               SCREEN_BRIGHTNESS_MODE, mode);
        Settings.System.putInt(resolver, 
                               Settings.System.SCREEN_BRIGHTNESS,
                               Math.round(brightness * (float) SETTING_MAX));
    }

    
    // ******************************************************************** //
    // Class Data.
    // ******************************************************************** //

    // Debugging tag.
    @SuppressWarnings("unused")
    private static final String TAG = "DazzleProvider";
    
    // Constants for the screen brightness settings.
    private static final String SCREEN_BRIGHTNESS_MODE = "screen_brightness_mode";
    private static final int SCREEN_BRIGHTNESS_MODE_MANUAL = 0;
    private static final int SCREEN_BRIGHTNESS_MODE_AUTOMATIC = 1;

}

