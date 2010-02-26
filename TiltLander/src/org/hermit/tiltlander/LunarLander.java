
/*
 * Copyright (C) 2007 Google Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */


package org.hermit.tiltlander;


import org.hermit.android.core.MainActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.Window;
import android.widget.TextView;


/**
 * This is a simple LunarLander activity that houses a single GameView. It
 * demonstrates...
 * <ul>
 * <li>animating by calling invalidate() from draw()
 * <li>loading and drawing resources
 * <li>handling onPause() in an animation
 * </ul>
 */
public class LunarLander
	extends MainActivity
{
    
    // ******************************************************************** //
    // Activity Setup.
    // ******************************************************************** //

    /**
     * Called when the activity is starting.  This is where most
     * initialization should go: calling setContentView(int) to inflate
     * the activity's UI, etc.
     * 
     * You can call finish() from within this function, in which case
     * onDestroy() will be immediately called without any of the rest of
     * the activity lifecycle executing.
     * 
     * Derived classes must call through to the super class's implementation
     * of this method.  If they do not, an exception will be thrown.
     * 
     * @param   icicle          If the activity is being re-initialized
     *                          after previously being shut down then this
     *                          Bundle contains the data it most recently
     *                          supplied in onSaveInstanceState(Bundle).
     *                          Note: Otherwise it is null.
     */
    @Override
    protected void onCreate(Bundle icicle) {
        Log.i(TAG, "onCreate(): " +
                        (icicle == null ? "clean start" : "restart"));
    
        super.onCreate(icicle);

        // Set up the standard dialogs.
        createMessageBox(R.string.button_close);
        setAboutInfo(R.string.about_text);
        setHomeInfo(R.string.button_homepage, R.string.url_homepage);
        setLicenseInfo(R.string.button_license, R.string.url_license);

        // turn off the window's title bar
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        // tell system to use the layout defined in our XML file
        setContentView(R.layout.lunar_layout);

        // get handles to the GameView from XML.
        mLunarView = (GameView) findViewById(R.id.lunar);

        // give the LunarView a handle to the TextView used for messages
        mLunarView.setTextView((TextView) findViewById(R.id.text));

        if (icicle == null) {
            Log.w(this.getClass().getName(), "SIS is null");
        } else {
            // we are being restored: resume a previous game
            mLunarView.restoreState(icicle);
            Log.w(this.getClass().getName(), "SIS is nonnull");
        }
    }

    
    /**
     * Called after onCreate(Bundle) or onStop() when the current activity is
     * now being displayed to the user.  It will be followed by onResume()
     * if the activity comes to the foreground, or onStop() if it becomes
     * hidden.
     * 
     * Derived classes must call through to the super class's implementation
     * of this method.  If they do not, an exception will be thrown.
     */
    @Override
    protected void onStart() {
        Log.i(TAG, "onStart()");
        super.onStart();
        
        mLunarView.onStart();
    }
    

    /**
     * Called after onRestoreInstanceState(Bundle), onRestart(), or onPause(),
     * for your activity to start interacting with the user. 
     */
    @Override
    protected void onResume() {
        Log.i(TAG, "onResume()");
        
        super.onResume();
        
        mLunarView.onResume();
    }

    
    /**
     * Notification that something is about to happen, to give the Activity a
     * chance to save state.
     * 
     * @param outState a Bundle into which this Activity should save its state
     */
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        Log.i(TAG, "onSaveInstanceState()");
        
        super.onSaveInstanceState(outState);
        
        // just have the View's thread save its state into our Bundle
        mLunarView.saveState(outState);
    }


    /**
     * Invoked when the Activity loses user focus.
     */
    @Override
    protected void onPause() {
        Log.i(TAG, "onPause()");

        super.onPause();
        
        mLunarView.onPause();
    }

    
    /**
     * Called when you are no longer visible to the user.  You will next
     * receive either onStart(), onDestroy(), or nothing, depending on
     * later user activity.
     * 
     * Note that this method may never be called, in low memory situations
     * where the system does not have enough memory to keep your activity's
     * process running after its onPause() method is called.
     * 
     * Derived classes must call through to the super class's implementation
     * of this method.  If they do not, an exception will be thrown.
     */
    @Override
    protected void onStop() {
        Log.i(TAG, "onStop()");
        super.onStop();
        
        mLunarView.onStop();
    }

    
    // ******************************************************************** //
    // Menu Management.
    // ******************************************************************** //
    
    /**
     * Invoked during init to give the Activity a chance to set up its Menu.
     * 
     * @param menu the Menu to which entries may be added
     * @return true
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
     * Invoked when the user selects an item from the Menu.
     * 
     * @param item the Menu entry which was selected
     * @return true if the Menu item was legit (and we consumed it), false
     *         otherwise
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_new:
                mLunarView.doStart();
                return true;
            case R.id.menu_pause:
                mLunarView.pause();
                return true;
            case R.id.skill_easy:
                mLunarView.setDifficulty(GameView.Difficulty.EASY);
                return true;
            case R.id.skill_medium:
                mLunarView.setDifficulty(GameView.Difficulty.MEDIUM);
                return true;
            case R.id.skill_hard:
                mLunarView.setDifficulty(GameView.Difficulty.HARD);
                return true;
            case R.id.menu_invert:
                mLunarView.toggleTiltInverted();
                return true;
            case R.id.menu_help:
                // Launch the help activity as a subactivity.
                mLunarView.pause();
                Intent hIntent = new Intent();
                hIntent.setClass(this, Help.class);
                startActivity(hIntent);
                break;
            case R.id.menu_about:
                showAbout();
                break;
       }

        return false;
    }

    
    // ******************************************************************** //
    // Private Constants.
    // ******************************************************************** //

    // Debugging tag.
    private static final String TAG = "netscramble";

    
    // ******************************************************************** //
    // Private Data.
    // ******************************************************************** //
    
    // A handle to the View in which the game is running.
    private GameView mLunarView;

}

