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

import org.hermit.tiltlander.LunarView.LunarThread;

import android.app.Activity;
import android.app.AlertDialog;
import android.hardware.SensorListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.TextView;


/**
 * This is a simple LunarLander activity that houses a single LunarView. It
 * demonstrates...
 * <ul>
 * <li>animating by calling invalidate() from draw()
 * <li>loading and drawing resources
 * <li>handling onPause() in an animation
 * </ul>
 */
public class LunarLander
	extends Activity
	implements SensorListener
{
    private static final int MENU_EASY = 1;

    private static final int MENU_HARD = 2;

    private static final int MENU_MEDIUM = 3;

    private static final int MENU_PAUSE = 4;

    private static final int MENU_RESUME = 5;

    private static final int MENU_START = 6;

    private static final int MENU_STOP = 7;

    private static final int MENU_ABOUT = 8;

    private static final int MENU_INVERT = 9;
    
    /** A handle to the thread that's actually running the animation. */
    private LunarThread mLunarThread;

    /** A handle to the View in which the game is running. */
    private LunarView mLunarView;
	
    /** The sensor manager, which we use to interface to all sensors. */
    private SensorManager sensorManager;

	// Dialog used to display about etc.
	private AlertDialog messageDialog;

    /**
     * Invoked during init to give the Activity a chance to set up its Menu.
     * 
     * @param menu the Menu to which entries may be added
     * @return true
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);

        menu.add(0, MENU_START, 0, R.string.menu_start);
        menu.add(0, MENU_STOP, 0, R.string.menu_stop);
        menu.add(0, MENU_PAUSE, 0, R.string.menu_pause);
        menu.add(0, MENU_RESUME, 0, R.string.menu_resume);
        menu.add(0, MENU_EASY, 0, R.string.menu_easy);
        menu.add(0, MENU_MEDIUM, 0, R.string.menu_medium);
        menu.add(0, MENU_HARD, 0, R.string.menu_hard);
        menu.add(0, MENU_INVERT, 0, R.string.menu_invert);
        menu.add(0, MENU_ABOUT, 0, R.string.menu_about);

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
            case MENU_START:
                mLunarThread.doStart();
                return true;
            case MENU_STOP:
                mLunarThread.setState(LunarThread.STATE_LOSE,
                                      getText(R.string.message_stopped));
                return true;
            case MENU_PAUSE:
                mLunarThread.pause();
                return true;
            case MENU_RESUME:
                mLunarThread.unpause();
                return true;
            case MENU_EASY:
                mLunarThread.setDifficulty(LunarThread.DIFFICULTY_EASY);
                return true;
            case MENU_MEDIUM:
                mLunarThread.setDifficulty(LunarThread.DIFFICULTY_MEDIUM);
                return true;
            case MENU_HARD:
                mLunarThread.setDifficulty(LunarThread.DIFFICULTY_HARD);
                return true;
            case MENU_INVERT:
                mLunarThread.toggleTiltInverted();
                return true;
            case MENU_ABOUT:
     			messageDialog.show();
                return true;
       }

        return false;
    }

    /**
     * Invoked when the Activity is created.
     * 
     * @param savedInstanceState a Bundle containing state saved from a previous
     *        execution, or null if this is a new execution
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // turn off the window's title bar
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        // tell system to use the layout defined in our XML file
        setContentView(R.layout.lunar_layout);

        messageDialog = createDialog();

        // get handles to the LunarView from XML, and its LunarThread
        mLunarView = (LunarView) findViewById(R.id.lunar);
        mLunarThread = mLunarView.getThread();

        // give the LunarView a handle to the TextView used for messages
        mLunarView.setTextView((TextView) findViewById(R.id.text));

        if (savedInstanceState == null) {
            // we were just launched: set up a new game
            mLunarThread.setState(LunarThread.STATE_READY);
            Log.w(this.getClass().getName(), "SIS is null");
        } else {
            // we are being restored: resume a previous game
            mLunarThread.restoreState(savedInstanceState);
            Log.w(this.getClass().getName(), "SIS is nonnull");
        }

        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
    }
    
    /**
     * Create the popup dialog for the help and about text.
     * 
     * @return					The new dialog.
     */
    private AlertDialog createDialog() {
    	// Create the text view.
    	LayoutInflater inflater = getLayoutInflater();
    	View tv = inflater.inflate(R.layout.dialog_text_box, null);
    	
    	// Build a dialog around that view.
    	AlertDialog.Builder builder = new AlertDialog.Builder(this);
    	builder.setTitle(R.string.about_title);
    	builder.setView(tv);
    	builder.setPositiveButton(R.string.button_ok, null);
    	
    	return builder.create();
    }


    /**
     * Invoked when the Activity loses user focus.
     */
    @Override
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);
        mLunarView.getThread().pause(); // pause game when Activity pauses
    }
    
    /**
     * Called after onRestoreInstanceState(Bundle), onRestart(), or onPause(),
     * for your activity to start interacting with the user. 
     */
    @Override
    protected void onResume() {
        super.onResume();
        
        sensorManager.registerListener(this, 
                					   SensorManager.SENSOR_ACCELEROMETER,
                					   SensorManager.SENSOR_DELAY_GAME);
    }


    /**
     * Notification that something is about to happen, to give the Activity a
     * chance to save state.
     * 
     * @param outState a Bundle into which this Activity should save its state
     */
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        // just have the View's thread save its state into our Bundle
        super.onSaveInstanceState(outState);
        mLunarThread.saveState(outState);
        Log.w(this.getClass().getName(), "SIS called");
    }
    
    
    /**
     * Called when sensor values have changed.  The length and contents
     * of the values array vary depending on which sensor is being monitored.
     *
	 * @param	sensor			The ID of the sensor being monitored.
	 * @param	values			The new values for the sensor.
     */
	public void onSensorChanged(int sensor, float[] values) {
		if (sensor != SensorManager.SENSOR_ACCELEROMETER || values.length < 3)
			return;

		// Calculate the angle of tilt in X; i.e. the elevation off the Y-Z
		// plane.  This is pretty easy; the X value is the opposite side,
		// and the absolute magnitude of the current value is the hypotenuse.
		// So sin a = x / m.
		final float x = values[0];
		final float y = values[1];
		final float z = values[2];
		double m = (float) Math.sqrt(x*x + y*y + z*z);
		double tilt = Math.asin(x / m);
		
        mLunarView.getThread().doTilt(Math.toDegrees(tilt));
	}

	@Override
	public void onAccuracyChanged(int sensor, int accuracy) {
		// Don't really need this.
	}

}
