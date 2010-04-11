
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
import android.graphics.Color;
import android.provider.Settings;
import android.util.Log;
import android.view.WindowManager;
import android.widget.RemoteViews;


/**
 * This static class provides utilities to read and write the screen
 * brightness settings in the system settings content provider.
 */
class BrightnessSettings
{

    // ******************************************************************** //
    // Public Constants.
    // ******************************************************************** //

    // BrightnessSettings values: fully off, dim, medium, fully on.
    static final float BRIGHTNESS_OFF = 0.00f;
    static final float BRIGHTNESS_MED = 0.50f;
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
                                          SCREEN_BRIGHTNESS_MODE,
                                          SCREEN_BRIGHTNESS_MODE_MANUAL);
        return mode == SCREEN_BRIGHTNESS_MODE_AUTOMATIC;
    }


    static float getBrightness(Context context) {
        int lev = Settings.System.getInt(context.getContentResolver(),
                                         Settings.System.SCREEN_BRIGHTNESS,
                                         SETTING_MAX);
        return settingToFraction(lev);
    }


    static void setWidget(Context context, RemoteViews views, int widget) {
        ContentResolver resolver = context.getContentResolver();
        
        String lab = "?";
        int col = Color.WHITE;

        int mode = Settings.System.getInt(resolver,
                                          SCREEN_BRIGHTNESS_MODE,
                                          SCREEN_BRIGHTNESS_MODE_MANUAL);
        if (mode == SCREEN_BRIGHTNESS_MODE_AUTOMATIC) {
            lab = context.getString(R.string.button_auto);
            col = 0xff00ff00;
        } else {
            int bright = Settings.System.getInt(resolver,
                                        Settings.System.SCREEN_BRIGHTNESS,
                                        SETTING_MAX);
            float frac = settingToFraction(bright);
            if (frac < 0.005f) {
                lab = context.getString(R.string.button_low);
                col = 0xffff8000;
            } else if (frac > 0.995f) {
                lab = context.getString(R.string.button_high);
                col = 0xffff00ff;
            } else {
                lab = String.valueOf(Math.round(frac * 100f)) + "%";
                col = 0xff00ffff;
            }
        }

        views.setTextViewText(R.id.brightness_ind, lab);
        views.setTextColor(R.id.brightness_ind, col);
    }


    static void setMode(Context context, boolean auto, float brightness) {
        ContentResolver resolver = context.getContentResolver();
        
        int mode = auto ? SCREEN_BRIGHTNESS_MODE_AUTOMATIC : SCREEN_BRIGHTNESS_MODE_MANUAL;
        int lev = fractionToSetting(brightness);
        
        Log.v(TAG, "save settings " + (auto ? "A " : "M ") + lev);

        Settings.System.putInt(resolver,
                               SCREEN_BRIGHTNESS_MODE, mode);
        Settings.System.putInt(resolver, 
                               Settings.System.SCREEN_BRIGHTNESS,
                               lev);
    }

    
    static final float settingToFraction(int setting) {
        float base = (float) setting / (float) SETTING_MAX;
        return (base - LEVEL_MIN) / LEVEL_RANGE;
    }
    

    static final int fractionToSetting(float frac) {
        float actual = frac * LEVEL_RANGE + LEVEL_MIN;
        return Math.round(actual * SETTING_MAX);
    }
    

    static final void fractionToParams(float frac, WindowManager.LayoutParams lp) {
        float actual = frac * LEVEL_RANGE + LEVEL_MIN;
        lp.screenBrightness = actual;
    }
    

    // ******************************************************************** //
    // Class Data.
    // ******************************************************************** //

    // Debugging tag.
    @SuppressWarnings("unused")
    private static final String TAG = DazzleProvider.TAG;

    // Minimum brightness fraction.
    private static final float LEVEL_MIN = 0.06f;
    private static final float LEVEL_RANGE = 1f - LEVEL_MIN;
  
    // BrightnessSettings values: fully off, dim, fully on.
    private static final int SETTING_MAX = 255;

    // Constants for the screen brightness settings.
    private static final String SCREEN_BRIGHTNESS_MODE = "screen_brightness_mode";
    private static final int SCREEN_BRIGHTNESS_MODE_MANUAL = 0;
    private static final int SCREEN_BRIGHTNESS_MODE_AUTOMATIC = 1;

}

