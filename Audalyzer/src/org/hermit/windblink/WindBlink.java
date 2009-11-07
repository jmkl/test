
/**
 * Wind Blink: a wind meter for Android.
 * <br>Copyright 2009 Ian Cameron Smith
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


package org.hermit.windblink;


import org.hermit.android.core.MainActivity;

import android.content.Intent;
import android.media.AudioManager;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.Window;
import android.view.WindowManager;


/**
 * Main activity for Wind Blink.
 * 
 * <p>This class basically sets up a WindMeter object and lets it run.
 */
public class WindBlink
    extends MainActivity
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
        
        // Create our EULA box.
        createEulaBox(R.string.eula_title, R.string.eula_text, R.string.button_close);       

        // Set up the standard dialogs.
        createMessageBox(R.string.button_close);
        setAboutInfo(R.string.about_text, R.string.help_text);
        setHomeInfo(R.string.button_homepage, R.string.url_homepage);
        setManualInfo(R.string.button_manual, R.string.url_manual);
        setLicenseInfo(R.string.button_license, R.string.url_license);

        // We don't want a title bar or status bar.
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                             WindowManager.LayoutParams.FLAG_FULLSCREEN);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
      
        // We want the audio controls to control our sound volume.
        this.setVolumeControlStream(AudioManager.STREAM_MUSIC);

        // Create the application GUI.
        windMeter = new WindMeter(this);
        setContentView(windMeter);
        
        // Restore our preferences.
        updatePreferences();
        
        // Restore our app state, if this is a restart.
        if (icicle != null)
            ;
//            restoreState(icicle);
    }
    

    /**
     * Called after {@link #onCreate} or {@link #onStop} when the current
     * activity is now being displayed to the user.  It will
     * be followed by {@link #onRestart}.
     */
    @Override
    protected void onStart() {
        Log.i(TAG, "onStart()");
        
        super.onStart();
        windMeter.onStart();
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
        
        // First time round, show the EULA.
        showFirstEula();
    
        windMeter.onResume();
        
        // Just start straight away.
        windMeter.surfaceStart();
    }


    /**
     * Called to retrieve per-instance state from an activity before being
     * killed so that the state can be restored in onCreate(Bundle) or
     * onRestoreInstanceState(Bundle) (the Bundle populated by this method
     * will be passed to both).
     * 
     * @param   outState        A Bundle in which to place any state
     *                          information you wish to save.
     */
    @Override
    public void onSaveInstanceState(Bundle outState) {
        Log.i(TAG, "onSaveInstanceState()");
        super.onSaveInstanceState(outState);
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
        
        windMeter.onPause();
    }


    /**
     * Called when you are no longer visible to the user.  You will next
     * receive either {@link #onStart}, {@link #onDestroy}, or nothing,
     * depending on later user activity.
     */
    @Override
    protected void onStop() {
        Log.i(TAG, "onStop()");
        super.onStop();
        
        windMeter.onStop();
    }


    // ******************************************************************** //
    // Menu Handling.
    // ******************************************************************** //

    /**
     * Initialize the contents of the game's options menu by adding items
     * to the given menu.
     * 
     * This is only called once, the first time the options menu is displayed.
     * To update the menu every time it is displayed, see
     * onPrepareOptionsMenu(Menu).
     * 
     * When we add items to the menu, we can either supply a Runnable to
     * receive notification of selection, or we can implement the Activity's
     * onOptionsItemSelected(Menu.Item) method to handle them there.
     * 
     * @param   menu            The options menu in which we should
     *                          place our items.  We can safely hold on this
     *                          (and any items created from it), making
     *                          modifications to it as desired, until the next
     *                          time onCreateOptionsMenu() is called.
     * @return                  true for the menu to be displayed; false
     *                          to suppress showing it.
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // We must call through to the base implementation.
        super.onCreateOptionsMenu(menu);
        
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);

        return true;
    }
    
    
    /**
     * This hook is called whenever an item in your options menu is selected.
     * Derived classes should call through to the base class for it to
     * perform the default menu handling.  (True?)
     *
     * @param   item            The menu item that was selected.
     * @return                  false to have the normal processing happen.
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.menu_prefs:
            // Launch the preferences activity as a subactivity, so we
            // know when it returns.
            Intent pIntent = new Intent();
            pIntent.setClass(this, Preferences.class);
            startActivityForResult(pIntent, new MainActivity.ActivityListener() {
                @Override
                public void onActivityFinished(int resultCode, Intent data) {
                    updatePreferences();
                }
            });
            break;
        case R.id.menu_help:
            showHelp();
            break;
        case R.id.menu_about:
            showAbout();
            break;
        case R.id.menu_eula:
            showEula();
            break;
        case R.id.menu_exit:
            finish();
            break;
        default:
            return super.onOptionsItemSelected(item);
        }
        
        return true;
    }
    

    // ******************************************************************** //
    // Preferences Handling.
    // ******************************************************************** //

    /**
     * Read our application preferences and configure ourself appropriately.
     */
    private void updatePreferences() {
//        SharedPreferences prefs =
//                        PreferenceManager.getDefaultSharedPreferences(this);
//        
//        // See if sounds are enabled and how.
//        soundMode = SoundMode.FULL;
//        try {
//            String smode = prefs.getString("soundMode", null);
//            soundMode = SoundMode.valueOf(smode);
//        } catch (Exception e) {
//            Log.e(TAG, "Pref: bad soundMode");
//        }
//        Log.i(TAG, "Prefs: soundMode " + soundMode);
//
//        wifiPing = false;
//        try {
//            wifiPing = prefs.getBoolean("wifiPing", false);
//        } catch (Exception e) {
//            Log.e(TAG, "Pref: bad wifiPing");
//        }
//        Log.i(TAG, "Prefs: wifiPing " + wifiPing);
//
//        // Get the desired orientation.
//        int orientMode = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED;
//        try {
//            String omode = prefs.getString("orientMode", null);
//            orientMode = Integer.valueOf(omode);
//        } catch (Exception e) {
//            Log.e(TAG, "Pref: bad orientMode");
//        }
//        Log.i(TAG, "Prefs: orientMode " + orientMode);
//        setRequestedOrientation(orientMode);
//        
//        boolean fakeMissingData = false;
//        try {
//            fakeMissingData = prefs.getBoolean("fakeMissingData", false);
//        } catch (Exception e) {
//            Log.e(TAG, "Pref: bad fakeMissingData");
//        }
//        Log.i(TAG, "Prefs: fakeMissingData " + fakeMissingData);
//        mainView.setSimulateMode(fakeMissingData);
    }


    // ******************************************************************** //
    // Class Data.
    // ******************************************************************** //

    // Debugging tag.
    @SuppressWarnings("unused")
    private static final String TAG = "WindMeter";
    
    
    // ******************************************************************** //
    // Private Data.
    // ******************************************************************** //
    
    // The surface manager for the view.
    private WindMeter windMeter = null;

}

