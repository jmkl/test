
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


import android.content.Context;
import android.graphics.Color;
import android.media.AudioManager;
import android.util.Log;
import android.widget.RemoteViews;


/**
 * This static class provides utilities to manage the ringer
 * mode and volume.
 */
class RingerSettings
{

    // ******************************************************************** //
    // Constructor.
    // ******************************************************************** //

    /**
     * Constructor -- hidden, as this class is non-instantiable.
     */
    private RingerSettings() {
    }
    

    // ******************************************************************** //
    // Screen Brightness Settings Handling.
    // ******************************************************************** //

    /**
     * Toggle the current state.
     * 
     * @param   context     The context we're running in.
     */
    static void toggle(Context context) {
        Log.i(TAG, "toggle Ringer");
        
        // Step to the next state.  (Not really a toggle.)
        AudioManager am =
            (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        int mode = am.getRingerMode();
        if (mode == AudioManager.RINGER_MODE_SILENT)
            mode = AudioManager.RINGER_MODE_VIBRATE;
        else if (mode == AudioManager.RINGER_MODE_VIBRATE)
            mode = AudioManager.RINGER_MODE_NORMAL;
        else
            mode = AudioManager.RINGER_MODE_SILENT;
        am.setRingerMode(mode);
    }


    /**
     * Get the current ringer mode.
     * 
     * @param   context     The context we're running in.
     * @return              The current mode.  One of
     *                      AudioManager.RINGER_MODE_NORMAL,
     *                      AudioManager.RINGER_MODE_SILENT, or
     *                      AudioManager.RINGER_MODE_VIBRATE.
     */
    static int getMode(Context context) {
        AudioManager am =
                (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        return am.getRingerMode();
    }


    /**
     * Get the current ringer volume.
     * 
     * @param   context     The context we're running in.
     * @return              The current volume as a fraction, 0-1.
     */
    static float getVolume(Context context) {
        AudioManager am =
            (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        int max = am.getStreamMaxVolume(AudioManager.STREAM_RING);
        int vol = am.getStreamVolume(AudioManager.STREAM_RING);
        return (float) vol / (float) max;
    }


    /**
     * Set the current ringer mode and volume.
     * 
     * @param   context     The context we're running in.
     * @param   mode        The mode to set.  One of
     *                      AudioManager.RINGER_MODE_NORMAL,
     *                      AudioManager.RINGER_MODE_SILENT, or
     *                      AudioManager.RINGER_MODE_VIBRATE.
     * @param   volume      The volume to set, as a fraction, 0-1.
     */
    static void setMode(Context context, int mode, float volume) {
        AudioManager am =
            (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        int max = am.getStreamMaxVolume(AudioManager.STREAM_RING);
        int vol = Math.round(max * volume);
        am.setRingerMode(mode);
        if (mode == AudioManager.RINGER_MODE_NORMAL)
            am.setStreamVolume(AudioManager.STREAM_RING, vol, 0);
    }


    /**
     * Set the indicator widget to represent our current state.
     * 
     * @param   context     The context we're running in.
     * @param   views       The widget view to modify.
     * @param   widget      The ID of the indicator widget.
     */
    static void setTextWidget(Context context, RemoteViews views, int widget) {
        AudioManager am =
            (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        
        String lab = "?";
        int col = Color.WHITE;

        int mode = am.getRingerMode();
        if (mode == AudioManager.RINGER_MODE_SILENT) {
            lab = context.getString(R.string.ringer_silent);
            col = 0xffff0000;
        } else if (mode == AudioManager.RINGER_MODE_VIBRATE) {
            lab = context.getString(R.string.ringer_vibe);
            col = 0xffff8000;
        } else {
            int max = am.getStreamMaxVolume(AudioManager.STREAM_RING);
            int vol = am.getStreamVolume(AudioManager.STREAM_RING);
            float frac = (float) vol / (float) max;

            if (frac > 0.995f) {
                lab = context.getString(R.string.ringer_max);
                col = 0xffff00ff;
            } else {
                lab = String.valueOf(Math.round(frac * 100f)) + "%";
                col = 0xff00ffff;
            }
        }

        views.setTextViewText(widget, lab);
        views.setTextColor(widget, col);
    }


    /**
     * Set the indicator widget to represent our current state.
     * 
     * @param   context     The context we're running in.
     * @param   views       The widget view to modify.
     * @param   widget      The ID of the indicator widget.
     */
    static void setWidget(Context context, RemoteViews views, int widget) {
        AudioManager am =
            (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        
        int image = R.drawable.grey;

        int mode = am.getRingerMode();
        if (mode == AudioManager.RINGER_MODE_SILENT)
            image = R.drawable.red;
        else if (mode == AudioManager.RINGER_MODE_VIBRATE)
            image = R.drawable.orange;
        else if (mode == AudioManager.RINGER_MODE_NORMAL)
            image = R.drawable.green;

        views.setImageViewResource(widget, image);
    }

    
    // ******************************************************************** //
    // Class Data.
    // ******************************************************************** //

    // Debugging tag.
    @SuppressWarnings("unused")
    private static final String TAG = DazzleProvider.TAG;

}

