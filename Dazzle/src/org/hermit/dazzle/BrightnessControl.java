
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
import org.hermit.dazzle.BrightnessSettings.Mode;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.SeekBar;
import android.widget.ToggleButton;


/**
 * Class implementing the pop-up brightness control panel.  This is an
 * Activity, that can be fired off when needed.
 */
public class BrightnessControl
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
     * You can call finish() from within this function, in which case
     * onDestroy() will be immediately called without any of the rest of
     * the activity lifecycle executing.
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
        
        // Get parameters from the intent.  See if we're in one-touch mode,
        // and if we're showing the auto option.
        Intent intent = getIntent();
        isOnetouch = intent.getBooleanExtra("onetouch", false);
        showAuto = intent.getBooleanExtra("auto", false);
        
        // Get our preferences.
        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        
        // If we're not in one-touch mode, create the UI.  Otherwise,
        // make an empty UI.
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        if (!isOnetouch) {
            setContentView(R.layout.brightness_activity);

            // Set handlers on all the widgets.
            lowBut = (ToggleButton) findViewById(R.id.button_low);
            lowBut.setOnClickListener(buttonChecked);

            medBut = (ToggleButton) findViewById(R.id.button_med);
            medBut.setOnClickListener(buttonChecked);

            highBut = (ToggleButton) findViewById(R.id.button_high);
            highBut.setOnClickListener(buttonChecked);

            autoBut = (ToggleButton) findViewById(R.id.button_auto);
            if (!showAuto)
                autoBut.setVisibility(View.GONE);
            else {
                autoBut.setVisibility(View.VISIBLE);
                autoBut.setOnClickListener(buttonChecked);
            }

            levelSlider = (SeekBar) findViewById(R.id.slider);
            levelSlider.setMax(1000);
            levelSlider.setOnSeekBarChangeListener(levelChanged);

            // If we're in onetouch mode, make the window invisible.  This sucks,
            // but it seems without having the window onscreen, we can't actually
            // set the brightness.
        } else {
            Window win = getWindow();
            WindowManager.LayoutParams lp = win.getAttributes();
            lp.alpha = 0.0f;
            win.setAttributes(lp);
        }
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
        
        // Get the user-configured "medium" level.
        try {
            userLevel = sharedPrefs.getFloat("userLevel", BrightnessSettings.BRIGHTNESS_MED);
        } catch (Exception e) {
            Log.e(TAG, "Pref: bad userLevel");
        }
        if (userLevel < 0f)
            userLevel = 0f;
        else if (userLevel > 1f)
            userLevel = 1f;
        Log.i(TAG, "Prefs: userLevel " + userLevel);

        // Get the current level.
        currentMode = BrightnessSettings.getMode(this);
        currentBrightness = BrightnessSettings.getBrightness(this);

        // If this is a one-touch activation, just step the mode and we're
        // done.
        if (isOnetouch) {
            // If we're not at min or max, save the current brightness as the
            // user's "medium" level, since one-touch mode doesn't let the
            // user set it directly.
            if (currentMode == Mode.USER) {
                userLevel = currentBrightness;
                if (userLevel < 0f)
                    userLevel = 0f;
                else if (userLevel > 1f)
                    userLevel = 1f;
                SharedPreferences.Editor editor = sharedPrefs.edit();
                editor.putFloat("userLevel", userLevel);
                editor.commit();
            }

            // We need to set the mode when the window has come up.  So
            // post a delayed event to do it.  runOnUiThread doesn't
            // do it.
            Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    stepMode();
                    finish();
                }
            }, 1);
        } else {
            // Set the widgets up.
            setControls();
        }
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

        // Save the settings.
        BrightnessSettings.setMode(this, currentMode, currentBrightness);
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
                case R.id.button_low:
                    setMode(Mode.MIN, BrightnessSettings.BRIGHTNESS_OFF);
                    break;
                case R.id.button_med:
                    setMode(Mode.USER, userLevel);
                    break;
                case R.id.button_high:
                    setMode(Mode.MAX, BrightnessSettings.BRIGHTNESS_MAX);
                    break;
                case R.id.button_auto:
                    setMode(Mode.AUTO, userLevel);
                    break;
                }

                // Re-set all the controls.
                setControls();

                finish();
            } catch (Exception e) {
                Errors.reportException(BrightnessControl.this, e);
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
                    userLevel = (float) n / 1000.0f;
                    setMode(Mode.USER, userLevel, false);
                }

                // Re-set all the controls.
                setControls();
            } catch (Exception e) {
                Errors.reportException(BrightnessControl.this, e);
            }
        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            try {
                setMode(Mode.USER, userLevel);

                // Save the user's "medium" level.
                SharedPreferences.Editor editor = sharedPrefs.edit();
                editor.putFloat("userLevel", userLevel);
                editor.commit();

                finish();
            } catch (Exception e) {
                Errors.reportException(BrightnessControl.this, e);
            }
        }
    };


    /**
     * Set the controls to reflect the current state.
     */
    private void setControls() {
        lowBut.setChecked(currentMode == Mode.MIN);
        medBut.setChecked(currentMode == Mode.USER);
        highBut.setChecked(currentMode == Mode.MAX);
        if (showAuto)
            autoBut.setChecked(currentMode == Mode.AUTO);
        levelSlider.setProgress(Math.round(userLevel * 1000));
    }


    /**
     * Set the screen mode and brightness.
     * 
     * @param   mode        The mode to set.
     * @param   level       If not auto, desired level, 0-1.
     */
    private void setMode(Mode mode, float level) {
        setMode(mode, level, true);
    }


    /**
     * Set the screen mode and brightness.
     * 
     * @param   mode        The mode to set.
     * @param   level       If not auto, desired level, 0-1.
     * @param   commit      If true, save the changes.  Otherwise, just
     *                      adjust the screen -- this is useful when we
     *                      need a fast response, but you must commit at
     *                      some point.
     */
    private void setMode(Mode mode, float level, boolean commit) {
        Log.v(TAG, "set screen " + mode + "/" + level);
        
        if (commit)
            BrightnessSettings.setMode(this, mode, level);
        
        if (mode != Mode.AUTO) {
            WindowManager.LayoutParams lp = getWindow().getAttributes();
            BrightnessSettings.fractionToParams(level, lp);
            getWindow().setAttributes(lp);
        }
        
        // Signal the widget manager to update all the widgets.
        if (commit)
            DazzleProvider.updateAllWidgets(this);
        
        currentMode = mode;
        currentBrightness = level;
    }


    /**
     * Step the screen mode to the next mode.
     */
    private void stepMode() {
        Log.v(TAG, "step screen");
        
        BrightnessSettings.toggle(this, showAuto, userLevel);
        
        currentMode = BrightnessSettings.getMode(this);
        currentBrightness = BrightnessSettings.getBrightness(this);

        if (currentMode != Mode.AUTO) {
            WindowManager.LayoutParams lp = getWindow().getAttributes();
            BrightnessSettings.fractionToParams(currentBrightness, lp);
            getWindow().setAttributes(lp);
        }

        // Signal the widget manager to update all the widgets.
        DazzleProvider.updateAllWidgets(this);
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

    // Flag whether we need to do this as a one-touch control.  If true,
    // just toggle the mode and dump the UI.  We need to do this in an
    // activity in order to have a Window to set the brightness on.
    private boolean isOnetouch;

    // Flag whether we support auto mode.  If set, show an auto option;
    // else show manual settings only.
    private boolean showAuto;
    
    // Our preferences.
    private SharedPreferences sharedPrefs = null;

    // The UI widgets.
    private ToggleButton lowBut = null;
    private ToggleButton medBut = null;
    private ToggleButton highBut = null;
    private ToggleButton autoBut = null;
    private SeekBar levelSlider = null;
    
    // User-configured "medium" brightness level, 0-1.
    private float userLevel = BrightnessSettings.BRIGHTNESS_MED;

    // Current brightness mode.
    private Mode currentMode = Mode.USER;
  
    // Current brightness level, 0-1.
    private float currentBrightness = BrightnessSettings.BRIGHTNESS_MED;
    
}

