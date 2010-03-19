
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


import android.app.Activity;
import android.os.Bundle;
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
public class DazzleControl
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
        
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        setContentView(R.layout.control_activity);
        
        // Set handlers on all the widgets.
        lowBut = (ToggleButton) findViewById(R.id.button_low);
        lowBut.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setMode(false, BrightnessSettings.BRIGHTNESS_OFF);
            }
        });
        
        medBut = (ToggleButton) findViewById(R.id.button_med);
        medBut.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setMode(false, BrightnessSettings.BRIGHTNESS_DIM);
            }
        });
        
        highBut = (ToggleButton) findViewById(R.id.button_high);
        highBut.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setMode(false, BrightnessSettings.BRIGHTNESS_MAX);
            }
        });
        
        autoBut = (ToggleButton) findViewById(R.id.button_auto);
        autoBut.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setMode(true, 0);
            }
        });
        
        levelSlider = (SeekBar) findViewById(R.id.slider);
        levelSlider.setMax(1000);
        levelSlider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) { }
        
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) { }
        
            @Override
            public void onProgressChanged(SeekBar bar, int n, boolean user) {
                setMode(false, (float) n / 1000.0f);
            }
        });
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
        
        // Get the current level.
        isAuto = BrightnessSettings.isAuto(this);
        currentBrightness = BrightnessSettings.getBrightness(this);
        
        // Set the widgets up.
        lowBut.setChecked(!isAuto && currentBrightness == 0f);
        medBut.setChecked(!isAuto && currentBrightness > 0f && currentBrightness < 1f);
        highBut.setChecked(!isAuto && currentBrightness == 1f);
        autoBut.setChecked(isAuto);
        levelSlider.setProgress(Math.round(currentBrightness * 1000));
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
        BrightnessSettings.setMode(this, isAuto, currentBrightness);
    }


    // ******************************************************************** //
    // Input Handling.
    // ******************************************************************** //
   
    /**
     * Set the screen mode and brightness.
     * 
     * @param   mode        True for auto-brightness; false for manual.
     * @param   level       If not auto, desired level, 0-1.
     */
    private void setMode(boolean auto, float level) {
        // Don't let the user set it black and get stuck.
        if (level < BrightnessSettings.BRIGHTNESS_DIM)
            level = BrightnessSettings.BRIGHTNESS_DIM;
        
        BrightnessSettings.setMode(this, auto, level);
        if (!auto) {
            WindowManager.LayoutParams lp = getWindow().getAttributes();
            lp.screenBrightness = level;
            getWindow().setAttributes(lp);
        }
        
        // Signal the widget manager to update all the widgets.
        DazzleProvider.updateWidgets(this);
        
        isAuto = auto;
        currentBrightness = level;
    }


    // ******************************************************************** //
    // Class Data.
    // ******************************************************************** //

    // Debugging tag.
    @SuppressWarnings("unused")
    private static final String TAG = "DazzleProvider";


    // ******************************************************************** //
    // Private Data.
    // ******************************************************************** //

    // The UI widgets.
    private ToggleButton lowBut = null;
    private ToggleButton medBut = null;
    private ToggleButton highBut = null;
    private ToggleButton autoBut = null;
    private SeekBar levelSlider = null;

    // Current auto flag.
    private boolean isAuto = false;
    
    // Current brightness level, 0-1.
    private float currentBrightness = BrightnessSettings.BRIGHTNESS_DIM;
    
}

