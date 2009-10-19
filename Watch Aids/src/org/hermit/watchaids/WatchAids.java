
/**
 * Watch Aids: handy reference information for sailors.
 * <br>Copyright 2009 Ian Cameron Smith
 * 
 * <p>This program simply displays various useful bits of reference
 * info.
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


package org.hermit.watchaids;


import android.app.Activity;
import android.os.Bundle;
import android.util.Log;


/**
 * The main activity for WatchAids.
 */
public class WatchAids extends Activity {

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
        Log.i(TAG, "onCreate(" + (icicle != null ? "restore" : "new") + ")");

        super.onCreate(icicle);
        setContentView(R.layout.flag_grid);
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
    }


    /**
     * Called to retrieve per-instance state from an activity before being
     * killed so that the state can be restored in onCreate(Bundle) or
     * onRestoreInstanceState(Bundle) (the Bundle populated by this method
     * will be passed to both).
     * 
     * If called, this method will occur before onStop().  There are no
     * guarantees about whether it will occur before or after onPause().
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
     * 
     * After receiving this call you will usually receive a following call
     * to onStop() (after the next activity has been resumed and displayed),
     * however in some cases there will be a direct call back to onResume()
     * without going through the stopped state. 
     * 
     * Derived classes must call through to the super class's implementation
     * of this method.  If they do not, an exception will be thrown.
     */
    @Override
    protected void onPause() {
        Log.i(TAG, "onPause()");
        
        super.onPause();
    }


    // ******************************************************************** //
    // Class Data.
    // ******************************************************************** //

    // Debugging tag.
    @SuppressWarnings("unused")
    private static final String TAG = "onwatch";
    
}

