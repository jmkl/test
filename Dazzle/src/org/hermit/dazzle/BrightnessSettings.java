
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

    // Enumeration of brightness modes.
    enum Mode {
        MIN,
        USER,
        MAX,
        AUTO;
    }
    
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
    // Screen Brightness Settings Handling.
    // ******************************************************************** //

    /**
     * Get the current brightness mode.
     * 
     * @param   context     Current context.
     * @return              The brightness mode.
     */
    static Mode getMode(Context context) {
        int mode = Settings.System.getInt(context.getContentResolver(),
                                          SCREEN_BRIGHTNESS_MODE,
                                          SCREEN_BRIGHTNESS_MODE_MANUAL);
        if (mode == SCREEN_BRIGHTNESS_MODE_AUTOMATIC)
            return Mode.AUTO;
        int lev = Settings.System.getInt(context.getContentResolver(),
                                         Settings.System.SCREEN_BRIGHTNESS,
                                         SETTING_MAX);
        float base = (float) lev / (float) SETTING_MAX;
        if (base < LEVEL_MIN_THRESH)
            return Mode.MIN;
        else if (base > LEVEL_MAX_THRESH)
            return Mode.MAX;
        return Mode.USER;
    }


    /**
     * Get the current brightness level.
     * 
     * @param   context     Current context.
     * @return              The brightness level.  The range
     *                      0 - 1 maps to the allowable user settings; so
     *                      if negative or > 1, the setting is outside this
     *                      range.
     */
    static float getBrightness(Context context) {
        int lev = Settings.System.getInt(context.getContentResolver(),
                                         Settings.System.SCREEN_BRIGHTNESS,
                                         SETTING_MAX);
        return settingToFraction(lev);
    }


    /**
     * Set the current brightness mode and level.
     * 
     * @param   context     Current context.
     * @param   mode        The desired mode.
     * @param   brightness  The desired brightness level.  This is only
     *                      relevant in Mode.USER, or if AUTO fails,
     *                      and the range is 0 - 1.
     */
    static void setMode(Context context, Mode mode, float brightness) {
        ContentResolver resolver = context.getContentResolver();
        
        int mval = mode == Mode.AUTO ? SCREEN_BRIGHTNESS_MODE_AUTOMATIC :
                                       SCREEN_BRIGHTNESS_MODE_MANUAL;
        int lev = SETTING_MAX;
        switch (mode) {
        case MIN:
            lev = SETTING_MIN;
            break;
        case USER:
        case AUTO:
            lev = fractionToSetting(brightness);
            break;
        case MAX:
            lev = SETTING_MAX;
            break;
        }
        
        Log.v(TAG, "save settings " + mode + "/" + lev);

        Settings.System.putInt(resolver,
                               SCREEN_BRIGHTNESS_MODE, mval);
        Settings.System.putInt(resolver, 
                               Settings.System.SCREEN_BRIGHTNESS,
                               lev);
    }


    /**
     * Toggle the current state.
     * 
     * <p>NOTE: this by itself won't change the current visible brightness,
     * it just saves the settings.  To make the brightness visible, you need:
     * 
     * <pre>
     *     WindowManager.LayoutParams lp = getWindow().getAttributes();
     *     BrightnessSettings.fractionToParams(level, lp);
     *     getWindow().setAttributes(lp);
     * </pre>
     * 
     * @param   context     The context we're running in.
     * @param   auto        If true, include auto as a possible state.
     * @param   medium      The brightness level to use for "medium".
     */
    static void toggle(Context context, boolean auto, float medium) {
        Log.i(TAG, "toggle Brightness " + (auto ? "with" : "no") + " auto");
        
        // Step to the next state.  (Not really a toggle.)
        Mode mode = getMode(context);
        switch (mode) {
        case MIN:
            mode = Mode.USER;
            break;
        case USER:
            mode = Mode.MAX;
            break;
        case MAX:
            mode = auto ? Mode.AUTO : Mode.MIN;
            break;
        case AUTO:
            mode = Mode.MIN;
            break;
        }
        setMode(context, mode, medium);
    }


    // ******************************************************************** //
    // GUI Handling.
    // ******************************************************************** //

    /**
     * Set the indicator widget to represent our current state.
     * 
     * @param   context     The context we're running in.
     * @param   views       The widget view to modify.
     * @param   widget      The ID of the indicator widget.
     */
    static void setWidget(Context context, RemoteViews views, int widget) {
        String lab = "?";
        int col = Color.WHITE;

        Mode mode = getMode(context);
        switch (mode) {
        case MIN:
            lab = context.getString(R.string.button_low);
            col = 0xffff8000;
            break;
        case USER:
            float frac = getBrightness(context);
            lab = String.valueOf(Math.round(frac * 100f)) + "%";
            col = 0xff00ffff;
            break;
        case MAX:
            lab = context.getString(R.string.button_high);
            col = 0xffff00ff;
            break;
        case AUTO:
            lab = context.getString(R.string.button_auto);
            col = 0xff00ff00;
            break;
        }

        views.setTextViewText(widget, lab);
        views.setTextColor(widget, col);
    }


    // ******************************************************************** //
    // Conversion Utilities.
    // ******************************************************************** //

    /**
     * Convert a brightness setting as stored in the system to a fraction
     * representing a user brightness level.
     * 
     * @param   setting     Integer system brightness setting.
     * @return              Equivalent user brightness level.  The range
     *                      0 - 1 maps to the allowable user settings; so
     *                      if negative or > 1, the setting is outside this
     *                      range.
     */
    static final float settingToFraction(int setting) {
        float base = (float) setting / (float) SETTING_MAX;
        return (base - LEVEL_USER_MIN) / LEVEL_USER_RANGE;
    }
    

    /**
     * Convert a user brightness level to a system setting.
     * 
     * @param   frac        User brightness level, in range 0 - 1.
     * @return              Equivalent integer system brightness setting.
     */
    static final int fractionToSetting(float frac) {
        float actual = frac * LEVEL_USER_RANGE + LEVEL_USER_MIN;
        return Math.round(actual * SETTING_MAX);
    }
    

    static final void fractionToParams(float frac, WindowManager.LayoutParams lp) {
        float actual = frac * LEVEL_USER_RANGE + LEVEL_USER_MIN;
        lp.screenBrightness = actual;
    }
    

    // ******************************************************************** //
    // Class Data.
    // ******************************************************************** //

    // Debugging tag.
    @SuppressWarnings("unused")
    private static final String TAG = DazzleProvider.TAG;

    // Minimum brightness fraction.  We don't go lower than this to
    // prevent the user being stuck with a black screen.
    private static final float LEVEL_MIN = 0.06f;
    private static final float LEVEL_MIN_THRESH = 0.07f;
    private static final float LEVEL_MAX_THRESH = 0.99f;
    
    // Minimum and maximum user-settable brightness fractions.  Outside
    // this range we're either MIN or MAX.
    private static final float LEVEL_USER_MIN = 0.08f;
    private static final float LEVEL_USER_MAX = 0.98f;
    private static final float LEVEL_USER_RANGE = LEVEL_USER_MAX - LEVEL_USER_MIN;
  
    // BrightnessSettings values: fully off, dim, fully on.
    private static final int SETTING_MAX = 255;
    private static final int SETTING_MIN = Math.round(SETTING_MAX * LEVEL_MIN);

    // Constants for the screen brightness settings.
    private static final String SCREEN_BRIGHTNESS_MODE = "screen_brightness_mode";
    private static final int SCREEN_BRIGHTNESS_MODE_MANUAL = 0;
    private static final int SCREEN_BRIGHTNESS_MODE_AUTOMATIC = 1;

}

