
/**
 * Substrate: grow crystal-like lines on a computational substrate
 *
 *       Lines like crystals grow on a computational substrate.  A  simple  per-
       pendicular growth rule creates intricate city-like structures.  Option-
       ally, cracks may also be circular, producing a cityscape more  familiar
       to places for which city planning is a distant, theoretical concern.

       Ported from the code by j.tarbell at http://complexification.net

       Copyright  ©  2003   by   J.   Tarbell   (complex@complexification.net,
       http://www.complexification.net).

       Ported      to      XScreensaver      2004      by     Mike     Kershaw
       (dragorn@kismetwireless.net)

AUTHOR
       J. Tarbell <complex@complexification.net>, Jun-03

       Mike Kershaw <dragorn@kismetwireless.net>, Oct-04


 * This is an Android implementation of the KDE game "knetwalk" by
 * Andi Peredri, Thomas Nagy, and Reinhold Kainhofer.
 *
 * © 2007-2010 Ian Cameron Smith <johantheghost@yahoo.com>
 *
 *   This program is free software; you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License version 2
 *   as published by the Free Software Foundation (see COPYING).
 * 
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 */


package org.hermit.substrate;


import android.app.Activity;
import android.media.AudioManager;
import android.os.Bundle;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager;


/**
 * Main activity for Zen Garden.
 * 
 * <p>This class basically sets up a ZenGarden object and lets it run.
 */
public class SubstrateApp extends Activity {

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
      
        // We want the audio controls to control our sound volume.
        this.setVolumeControlStream(AudioManager.STREAM_MUSIC);

        // Create the application GUI.
        theGarden = new SubstrateView(this);
        setContentView(theGarden);

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
        
        // Restore our app state, if this is a restart.
        if (icicle != null)
            ;
//            restoreState(icicle);

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
        theGarden.onStart();
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
        theGarden.onResume();
        
        // Just start straight away.
        theGarden.surfaceStart();
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
        
        theGarden.onPause();
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
        
        theGarden.onStop();
    }


    // ******************************************************************** //
    // Class Data.
    // ******************************************************************** //

    // Debugging tag.
    @SuppressWarnings("unused")
    private static final String TAG = "Zen";
    
    
    // ******************************************************************** //
    // Private Data.
    // ******************************************************************** //
    
    // The surface manager for the view.
    private SubstrateView theGarden = null;

}

