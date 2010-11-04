
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
import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;


/**
 * The preferences activity for Dazzle.  Since this is the first thing the
 * user sees when she adds the widget, it needs a "Save" button to make it
 * clear how to continue.  So, we can't use PreferenceActivity; we need to
 * build our own.
 */
public class Preferences
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
     * @param   icicle          Activity's saved state, if any.
     */
    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        // Set the view layout.
        setContentView(R.layout.preferences);
        
        // Can only do Bluetooth and auto-brightness from Eclair on.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.ECLAIR) {
            CheckBox btCheck = (CheckBox) findViewById(R.id.dazzle_bluetooth);
            btCheck.setEnabled(false);
            TextView btHelp = (TextView) findViewById(R.id.bluetooth_help);
            btHelp.setText(R.string.prefs_bluetooth_summary_nobt);
            
            CheckBox syCheck = (CheckBox) findViewById(R.id.dazzle_sync);
            syCheck.setEnabled(false);
            TextView syHelp = (TextView) findViewById(R.id.sync_help);
            syHelp.setText(R.string.prefs_sync_summary_nosync);
            
            CheckBox baCheck = (CheckBox) findViewById(R.id.dazzle_brightauto);
            baCheck.setEnabled(false);
            TextView baHelp = (TextView) findViewById(R.id.brightauto_help);
            baHelp.setText(R.string.prefs_brightauto_summary_noauto);
            
            CheckBox boCheck = (CheckBox) findViewById(R.id.dazzle_otbrightauto);
            boCheck.setEnabled(false);
            TextView boHelp = (TextView) findViewById(R.id.otbrightauto_help);
            boHelp.setText(R.string.prefs_otbrightauto_summary_noauto);
        }

        // Tethering exists since Froyo
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.FROYO) {
            findViewById(R.id.dazzle_wifi_ap).setEnabled(false);
            ((TextView) findViewById(R.id.prefs_wifi_ap_help))
                    .setText(R.string.prefs_wifi_ap_summary_froyo);
        }

        // Add a handler to the save button.
        Button saveBut = (Button) findViewById(R.id.save_button);
        saveBut.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                savePrefs();
            }
        });
        
        // Get the app widget ID from the intent.
        Intent intent = getIntent();
        Bundle extras = intent.getExtras();
        if (extras != null) {
            widgetId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, 
                                     AppWidgetManager.INVALID_APPWIDGET_ID);
        }
        
        // Set a "canceled" result as the default.  If the user hits Save,
        // we'll change this in savePrefs().
        setResult(RESULT_CANCELED);
    }

    
    /**
     * Called when the activity has detected the user's press of the back key.
     * The default implementation simply finishes the current activity;
     * we override this to update the widget. 
     */
    private void savePrefs() {
        // Read the widget settings, and save them to the prefs.
        int count = 0;
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor edit = prefs.edit();
        
        // Set all the controls.
        for (DazzleProvider.Control c : DazzleProvider.Control.CONTROLS) {
            CheckBox checkbox = (CheckBox) findViewById(c.id);
            boolean enable = checkbox.isChecked();
            edit.putBoolean(c.pref + "-" + widgetId, enable);
            if (enable)
                ++count;
        }

        // If we had no controls selected, put in a brightness control.
        if (count == 0) {
            DazzleProvider.Control c;
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.ECLAIR)
                c = DazzleProvider.Control.BRIGHTNESS;
            else
                c = DazzleProvider.Control.BRIGHTAUTO;
            edit.putBoolean(c.pref + "-" + widgetId, true);
        }
        
        edit.commit();

        // Update the widget.
        DazzleProvider.updateAllWidgets(this);
        
        // Now send the result intent.  It needs the widget ID.
        Intent result = new Intent();
        result.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId);
        setResult(RESULT_OK, result);
        finish();
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

    // The ID of the app widget.
    private int widgetId = AppWidgetManager.INVALID_APPWIDGET_ID;
    
}

