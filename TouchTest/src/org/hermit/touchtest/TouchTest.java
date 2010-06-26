
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


import org.hermit.android.core.MainActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.Window;
import android.view.WindowManager;


/**
 * Main TouchTest activity.  This is basically a wrapper around the
 * GridView class.
 */
public class TouchTest
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

        // Set up the standard dialogs.
        setAboutInfo(R.string.about_text);
        setHomeInfo(R.string.url_homepage);
        setLicenseInfo(R.string.url_license);

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
        case R.id.menu_help:
            // Launch the help activity as a subactivity.
            Intent hIntent = new Intent();
            hIntent.setClass(this, Help.class);
            startActivity(hIntent);
            break;
        case R.id.menu_about:
            showAbout();
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

