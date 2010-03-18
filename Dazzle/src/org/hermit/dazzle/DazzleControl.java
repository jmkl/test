
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
import android.view.View;
import android.widget.Button;


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
        
        setContentView(R.layout.control_activity);
        
        // Set handlers on all the widgets.
        Button lowBut = (Button) findViewById(R.id.button_low);
        lowBut.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setMode(false, Brightness.BRIGHTNESS_OFF);
            }
        });
        
        Button medBut = (Button) findViewById(R.id.button_med);
        medBut.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setMode(false, Brightness.BRIGHTNESS_DIM);
            }
        });
        
        Button highBut = (Button) findViewById(R.id.button_high);
        highBut.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setMode(false, Brightness.BRIGHTNESS_ON);
            }
        });
        
        Button autoBut = (Button) findViewById(R.id.button_auto);
        autoBut.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setMode(true, 0);
            }
        });
    }

    
    // ******************************************************************** //
    // Input Handling.
    // ******************************************************************** //
   
    /**
     * Set the screen mode and brightness.
     * 
     * @param   mode        True for auto-brightness; false for manual.
     * @param   level       If not auto, desired level, 0-255.
     */
    private void setMode(boolean auto, int level) {
        // Don't let the user set it black and get stuck.
        if (level < Brightness.BRIGHTNESS_DIM)
            level = Brightness.BRIGHTNESS_DIM;
        
        Brightness.setMode(this, auto, level);
    }

}

