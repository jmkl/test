
/**
 * Touch Test: a multi-touch test app for Android.
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


package org.hermit.touchtest;


import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.Window;
import android.view.WindowManager;


/**
 * Main TouchTest activity.  This is basically a wrapper around the
 * GridView class.
 */
public class TouchTest
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
        Window win = getWindow();
        win.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                     WindowManager.LayoutParams.FLAG_FULLSCREEN);
        win.requestFeature(Window.FEATURE_NO_TITLE);
		
        // Create the application GUI.  The version depends on whether
        // our host supports multi-touch.
        Class<MotionEvent> clazz = MotionEvent.class;
        try {
            clazz.getMethod("getPointerCount");
            gridView = new MtGridView(this);
        } catch (NoSuchMethodException e) {
            gridView = new StGridView(this);
        }

		setContentView(gridView);
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
        
        gridView.onStart();
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

        gridView.onResume();

        // Just start straight away.
        gridView.surfaceStart();
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
        
        gridView.onPause();
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

        gridView.onStop();
    }
    

	// ******************************************************************** //
	// Class Data.
	// ******************************************************************** //

    // Debugging tag.
	private static final String TAG = "TouchTest";
    
	
	// ******************************************************************** //
	// Private Data.
	// ******************************************************************** //

	// The main view.
    private GridView gridView;
    
}

