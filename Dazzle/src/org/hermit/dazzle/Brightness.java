
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


import android.appwidget.AppWidgetProvider;
import android.content.ContentResolver;
import android.content.Context;
import android.provider.Settings;


/**
 * This static class provides utilities to read and control the screen
 * brightness.
 */
class Brightness
    extends AppWidgetProvider
{

    // ******************************************************************** //
    // Public Constants.
    // ******************************************************************** //

    // Brightness values: fully off, dim, fully on.
    static final int BRIGHTNESS_OFF = 0;
    static final int BRIGHTNESS_DIM = 20;
    static final int BRIGHTNESS_ON = 255;
    

    // ******************************************************************** //
    // Constructor.
    // ******************************************************************** //

    /**
     * Constructor -- hidden, as this class is non-instantiable.
     */
    private Brightness() {
    }
    

    // ******************************************************************** //
    // Screen Brightness Handling.
    // ******************************************************************** //

    static boolean isAuto(Context context) {
        int mode = Settings.System.getInt(context.getContentResolver(),
                SCREEN_BRIGHTNESS_MODE, SCREEN_BRIGHTNESS_MODE_MANUAL);
        return mode == SCREEN_BRIGHTNESS_MODE_AUTOMATIC;
    }


    static int getBrightness(Context context) {
        return Settings.System.getInt(context.getContentResolver(),
                Settings.System.SCREEN_BRIGHTNESS, BRIGHTNESS_DIM);
    }


    static String getModeString(Context context) {
        ContentResolver resolver = context.getContentResolver();
        
        int mode = Settings.System.getInt(resolver,
                                          SCREEN_BRIGHTNESS_MODE,
                                          SCREEN_BRIGHTNESS_MODE_MANUAL);
        boolean auto = mode == SCREEN_BRIGHTNESS_MODE_AUTOMATIC;
        int bright = Settings.System.getInt(resolver,
                                            Settings.System.SCREEN_BRIGHTNESS,
                                            BRIGHTNESS_DIM);
        
        return auto ? "A" : "" + bright;
    }


    static void setMode(Context context, boolean auto, int brightness) {
        ContentResolver resolver = context.getContentResolver();
        
        int mode = auto ? SCREEN_BRIGHTNESS_MODE_AUTOMATIC : SCREEN_BRIGHTNESS_MODE_MANUAL;
        Settings.System.putInt(resolver,
                               SCREEN_BRIGHTNESS_MODE, mode);
        Settings.System.putInt(resolver, 
                               Settings.System.SCREEN_BRIGHTNESS,
                               brightness);
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

