
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


import org.hermit.android.core.Errors;

import android.app.Activity;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.SeekBar;
import android.widget.ToggleButton;


/**
 * Class implementing the pop-up ringer control panel.  This is an
 * Activity, that can be fired off when needed.
 */
public class RingerControl
    extends Activity
{

    // ******************************************************************** //
    // Activity Lifecycle.
    // ******************************************************************** //
   
    /**
     * Called when the activity is starting.  This is where most
     * initialisation should go: calling setContentView(int) to inflate
     * the activity's UI, etc.
     * 
     * Derived classes must call through to the super class's implementation
     * of this method.  If they do not, an exception will be thrown.
     * 
     * @param   icicle          If the activity is being re-initialised
     *                          after previously being shut down then this
     *                          Bundle contains the data it most recently
     *                          supplied in onSaveInstanceState(Bundle).
     *                          Note: Otherwise it is null.
     */
    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        
        // Get our preferences.
        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        
        // Create the UI.
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.ringer_activity);
        
        // Set handlers on all the widgets.
        maxBut = (ToggleButton) findViewById(R.id.button_max);
        maxBut.setOnClickListener(buttonChecked);
        
        onBut = (ToggleButton) findViewById(R.id.button_on);
        onBut.setOnClickListener(buttonChecked);
        
        vibeBut = (ToggleButton) findViewById(R.id.button_vibe);
        vibeBut.setOnClickListener(buttonChecked);
        
        silentBut = (ToggleButton) findViewById(R.id.button_silent);
        silentBut.setOnClickListener(buttonChecked);
        
        levelSlider = (SeekBar) findViewById(R.id.ringer_slider);
        levelSlider.setMax(1000);
        levelSlider.setOnSeekBarChangeListener(levelChanged);
    }


    /**
     * Called after onRestoreInstanceState(Bundle), onRestart(), or onPause(),
     * for your activity to start interacting with the user.  This is a good
     * place to begin animations, open exclusive-access devices (such as the
     * camera), etc.
     * 
     * Derived classes must call through to the super class's implementation
     * of this method.  If they do not, an exception will be thrown.
     */
    @Override
    protected void onResume() {
        Log.i(TAG, "onResume()");

        super.onResume();
        
        // Get the user-configured volume.
        try {
            userVolume = sharedPrefs.getFloat("ringerLevel", 0.5f);
        } catch (Exception e) {
            Log.e(TAG, "Pref: bad ringerLevel");
        }
        if (userVolume < 0f)
            userVolume = 0f;
        else if (userVolume > 1f)
            userVolume = 1f;
        Log.i(TAG, "Prefs: ringerLevel " + userVolume);
        
        // Get the current settings.
        ringerMode = RingerSettings.getMode(this);
        currentVolume = RingerSettings.getVolume(this);
        
        // Set the widgets up.
        setControls();
    }

    
    /**
     * Called as part of the activity lifecycle when an activity is going
     * into the background, but has not (yet) been killed.  The counterpart
     * to onResume(). 
     */
    @Override
    protected void onPause() {
        Log.i(TAG, "onPause()");

        super.onPause();
    }


    // ******************************************************************** //
    // Input Handling.
    // ******************************************************************** //
   
    private View.OnClickListener buttonChecked = new View.OnClickListener() {
        @Override
        public void onClick(View but) {
            try {
                // Set the appropriate mode.  We do this any time the user
                // taps the button, even if it was already checked.
                switch (but.getId()) {
                case R.id.button_max:
                    setMode(AudioManager.RINGER_MODE_NORMAL, 1.0f);
                    break;
                case R.id.button_on:
                    setMode(AudioManager.RINGER_MODE_NORMAL, userVolume);
                    break;
                case R.id.button_vibe:
                    setMode(AudioManager.RINGER_MODE_VIBRATE, userVolume);
                    break;
                case R.id.button_silent:
                    setMode(AudioManager.RINGER_MODE_SILENT, userVolume);
                    break;
                }

                // Re-set all the controls.
                setControls();

                finish();
            } catch (Exception e) {
                Errors.reportException(RingerControl.this, e);
            }
        }
    };


    private SeekBar.OnSeekBarChangeListener levelChanged =
        new SeekBar.OnSeekBarChangeListener() {
        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
        }

        @Override
        public void onProgressChanged(SeekBar bar, int n, boolean user) {
            try {
                if (user) {
                    userVolume = (float) n / 1000.0f;
                    setMode(AudioManager.RINGER_MODE_NORMAL, userVolume, false);
                }

                // Re-set all the controls.
                setControls();
            } catch (Exception e) {
                Errors.reportException(RingerControl.this, e);
            }
        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            try {
                setMode(AudioManager.RINGER_MODE_NORMAL, userVolume);

                // Save the user's "medium" level.
                SharedPreferences.Editor editor = sharedPrefs.edit();
                editor.putFloat("ringerLevel", userVolume);
                editor.commit();

                finish();
            } catch (Exception e) {
                Errors.reportException(RingerControl.this, e);
            }
        }
    };

    
    /**
     * Set the controls to reflect the current state.
     */
    private void setControls() {
        boolean max = currentVolume > 0.995f;
        maxBut.setChecked(ringerMode == AudioManager.RINGER_MODE_NORMAL && max);
        onBut.setChecked(ringerMode == AudioManager.RINGER_MODE_NORMAL && !max);
        vibeBut.setChecked(ringerMode == AudioManager.RINGER_MODE_VIBRATE);
        silentBut.setChecked(ringerMode == AudioManager.RINGER_MODE_SILENT);
        levelSlider.setProgress(Math.round(userVolume * 1000));
    }


    /**
     * Set the ringer mode and volume.
     * 
     * @param   mode        The ringer mode to set.  One of
     *                      AudioManager.RINGER_MODE_NORMAL,
     *                      AudioManager.RINGER_MODE_SILENT, or
     *                      AudioManager.RINGER_MODE_VIBRATE.
     * @param   level       If not silenced, desired level, 0-1.
    */
    private void setMode(int mode, float level) {
        setMode(mode, level, true);
    }


    /**
     * Set the ringer mode and volume.
     * 
     * @param   mode        The ringer mode to set.  One of
     *                      AudioManager.RINGER_MODE_NORMAL,
     *                      AudioManager.RINGER_MODE_SILENT, or
     *                      AudioManager.RINGER_MODE_VIBRATE.
     * @param   level       If not silenced, desired level, 0-1.
     * @param   commit      If true, save the changes.  Otherwise, just
     *                      adjust the screen -- this is useful when we
     *                      need a fast response, but you must commit at
     *                      some point.
    */
    private void setMode(int mode, float level, boolean commit) {
        Log.v(TAG, "set ringer " + mode + "/" + level);
        
        RingerSettings.setMode(this, mode, level);
        
        // Signal the widget manager to update all the widgets.
        if (commit)
            DazzleProvider.updateAllWidgets(this);
        
        ringerMode = mode;
        currentVolume = mode == AudioManager.RINGER_MODE_NORMAL ? level : 0;
    }


    // ******************************************************************** //
    // Class Data.
    // ******************************************************************** //

    // Debugging tag.
    @SuppressWarnings("unused")
    private static final String TAG = DazzleProvider.TAG;


    // ******************************************************************** //
    // Private Data.
    // ******************************************************************** //
    
    // Our preferences.
    private SharedPreferences sharedPrefs = null;

    // The UI widgets.
    private ToggleButton maxBut = null;
    private ToggleButton onBut = null;
    private ToggleButton vibeBut = null;
    private ToggleButton silentBut = null;
    private SeekBar levelSlider = null;

    // Current ringer mode.
    private int ringerMode = AudioManager.RINGER_MODE_NORMAL;
    
    // User-configured "medium" volume level, 0-1.
    private float userVolume = 0.5f;
    
    // The current volume level, 0-1.
    private float currentVolume = 0.5f;
   
}

