
/**
 * Substrate: a collection of eye candies for Android.  Various screen
 * hacks from the xscreensaver collection can be viewed standalone, or
 * set as live wallpapers.
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


package org.hermit.substrate;


import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager;


/**
 * A standalone activity which displays fullscreen eye candy.  Use
 * this to hypnotize your children, or something.  Mainly it gives the user
 * something to do when they don't know how to set up live wallpapers yet.
 * 
 * <p>This class basically sets up an EyeCandy object and lets it run.
 */
public class EyeCandyApp
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

        // We don't want a title bar or status bar.
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                             WindowManager.LayoutParams.FLAG_FULLSCREEN);
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        // Create the application GUI.
        eyeCandyView = new EyeCandyView(this);
        setContentView(eyeCandyView);

        // Create the EULA dialog.
//        eulaDialog = new EulaBox(this, R.string.eula_title,
//                                 R.string.eula_text,R.string.button_close);
        
        // Create the dialog we use for help and about.
//        AppUtils autils = AppUtils.getInstance(this);
//        messageDialog = new InfoBox(this, R.string.button_close);
//        String version = autils.getVersionString();
//        messageDialog.setTitle(version);

        // Restore our preferences.
//        updatePreferences();
        
        // First time, show the splash screen.
//        if (!shownSplash) {
//            SplashActivity.launch(this, R.drawable.splash_screen, SPLASH_TIME);
//            shownSplash = true;
//        }
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
        eyeCandyView.onStart();
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
//        eulaDialog.showFirstTime();
        
        eyeCandyView.onResume();
        
        // Just start straight away.
        eyeCandyView.surfaceStart();
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
        
        eyeCandyView.onPause();
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
        
        eyeCandyView.onStop();
    }


    // ******************************************************************** //
    // Class Data.
    // ******************************************************************** //

    // Debugging tag.
    @SuppressWarnings("unused")
    private static final String TAG = "Substrate";
    
    
    // ******************************************************************** //
    // Private Data.
    // ******************************************************************** //
    
    // The surface manager for the view.
    private EyeCandyView eyeCandyView = null;

}

