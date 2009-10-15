
/**
 * Plughole: a rolling-ball accelerometer game.
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


package org.hermit.plughole;

import org.hermit.android.AppUtils;
import org.hermit.android.InfoBox;
import org.hermit.plughole.LevelReader.LevelException;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.SensorListener;
import android.hardware.SensorManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;


/**
 * This is the main activity for the Plughole game.
 */
public class Plughole
	extends Activity
	implements SensorListener
{

    // ******************************************************************** //
    // Class Data.
    // ******************************************************************** //

    /**
     * The sounds that we make.
     */
    static enum Sound {
    	BEEP_BOOP(R.raw.beep_boop),
    	BOOP_BEEP(R.raw.boop_beep),
    	CHIRP_LOW(R.raw.chirp_low),
    	HMLU(R.raw.hmlu);
    	
    	private Sound(int res) {
    		soundRes = res;
    	}
    	
     	int soundRes;			// Resource ID for the sound file.
    }

    /**
     * Sound play mode.
     */
    static enum SoundMode {
    	NONE(0),
    	QUIET(0),
    	FULL(0);
    	
    	private SoundMode(int res) {
    		menuId = res;
    	}
    	
    	int menuId;				// ID of the corresponding menu item.
    }


	// ******************************************************************** //
    // Activity Lifecycle.
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
	 * @param	icicle			If the activity is being re-initialized
	 * 							after previously being shut down then this
	 * 							Bundle contains the data it most recently
	 * 							supplied in onSaveInstanceState(Bundle).
	 * 							Note: Otherwise it is null.
	 */
    @Override
    protected void onCreate(Bundle icicle) {
        Log.i(TAG, "onCreate(): " +
    			(icicle == null ? "clean start" : "restart"));
    
        super.onCreate(icicle);
        appResources = getResources();
        
        // We don't want a title bar or status bar.
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                		     WindowManager.LayoutParams.FLAG_FULLSCREEN);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        
        // Create a level manager.
        levelManager = new LevelManager(this);

        // Use the layout defined in XML.
        setContentView(createGui());

        // Create the dialog we use for help and about.
        AppUtils autils = new AppUtils(this);
        messageDialog = new InfoBox(this, R.string.button_close);
		messageDialog.setLinkButtons(new int[] {
				R.string.button_homepage,
				R.string.button_license
			},
			new int[] {
    			R.string.url_homepage,
    			R.string.url_license
		});
        String version = autils.getVersionString();
		messageDialog.setTitle(version);

        // Give the TableView a handle to the TextView used for messages.
        tableView.setTextView(overlayText);

        // Restore our preferences.
        updatePreferences();

        if (icicle != null) {
            Log.w(TAG, "Restore a saved state");
        	tableView.restoreState(icicle);
        } else
            Log.w(TAG, "New start");

        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
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
        
        // Resume the game, or at least enable the game to resume.
        // The state we actually go to depends on saved state.
        tableView.onResume();

        // Register for sensor updates.
        sensorManager.registerListener(this, 
                					   SensorManager.SENSOR_ACCELEROMETER,
                					   SensorManager.SENSOR_DELAY_GAME);
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
	 * @param	outState		A Bundle in which to place any state
	 * 							information you wish to save.
     */
    @Override
    public void onSaveInstanceState(Bundle outState) {
        Log.i(TAG, "onSaveInstanceState()");
        
        super.onSaveInstanceState(outState);

        tableView.saveState(outState);
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

        // Stop sensor updates.
        sensorManager.unregisterListener(this);
        
        // Pause the game.
        tableView.onPause();
    }
    

    // ******************************************************************** //
    // GUI Creation.
    // ******************************************************************** //
    
    /**
     * Create the GUI for the game.  We basically create a board which
     * fills the screen; but we also create a text display for status
     * messages, which covers the board when visible.
     * 
     * @return					The game GUI's top-level view.
     */
    private View createGui() {
//    	WindowManager wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
//    	Display disp = wm.getDefaultDisplay();
//    	int width = disp.getWidth();
//    	int height = disp.getHeight();

    	final int FPAR = FrameLayout.LayoutParams.FILL_PARENT;
    	final int WCON = FrameLayout.LayoutParams.WRAP_CONTENT;

        // Create a layout to hold the board and status bar.
    	FrameLayout mainLayout = new FrameLayout(this);
    	FrameLayout.LayoutParams layout;

    	// Create the main playing table view.
    	tableView = new TableView(this, levelManager);
        layout = new FrameLayout.LayoutParams(FPAR, FPAR);
		mainLayout.addView(tableView, layout);

		// Create the text overlay within a RelativeLayout.
		RelativeLayout rl = new RelativeLayout(this);
		rl.setGravity(Gravity.CENTER);
        layout = new FrameLayout.LayoutParams(FPAR, FPAR);
		mainLayout.addView(rl, layout);
		
		// Create the overlay text itself.
		overlayText = new TextView(this);
		overlayText.setTextColor(0xff00ffff);
		overlayText.setTextSize(24);
		overlayText.setGravity(Gravity.CENTER);
		RelativeLayout.LayoutParams rlp =
							new RelativeLayout.LayoutParams(WCON, WCON);
		rl.addView(overlayText, rlp);
		
        return mainLayout;
    }


	// ******************************************************************** //
    // Menu and Preferences Handling.
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
     * @param	menu			The options menu in which we should
     * 							place our items.  We can safely hold on this
     * 							(and any items created from it), making
     * 							modifications to it as desired, until the next
     * 							time onCreateOptionsMenu() is called.
     * @return					true for the menu to be displayed; false
     * 							to suppress showing it.
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
     * @param	item			The menu item that was selected.
     * @return					false to have the normal processing happen.
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
//            case MENU_START:
//            	tableView.doStart();
//                return true;
//            case MENU_STOP:
//            	tableView.getTable().setState(Table.State.LOSE,
//                        					getText(R.string.message_stopped));
//                return true;
        case R.id.menu_restart:
        	tableView.restart();
        	return true;
        case R.id.menu_goto:
        	tableView.restart();
        	return true;
        case R.id.menu_prefs:
        	// Launch the preferences activity as a subactivity, so we
        	// know when it returns.
        	Intent pIntent = new Intent();
        	pIntent.setClass(this, Preferences.class);
        	startActivityForResult(pIntent, SUBACTIVITY_PREFERENCES);
        	break;
        case R.id.menu_help:
        	messageDialog.show(R.string.help_text);
        	break;
        case R.id.menu_about:
        	messageDialog.show(R.string.about_text);
        	break;
        case R.id.menu_exit:
        	finish();
        	break;
        }

        return false;
    }

    
    /**
     * Called when an activity you launched exits, giving you the
     * requestCode you started it with, the resultCode it returned,
     * and any additional data from it.  The resultCode will be
     * RESULT_CANCELED if the activity explicitly returned that, didn't
     * return any result, or crashed during its operation.
     * 
     * You will receive this call immediately before onResume() when your
     * activity is re-starting.
     * 
     * @param	requestCode		The integer request code.
     * @param	resultCode		The integer result code returned by the child
     * 							activity through its setResult().
     * @param	data			An Intent with possible extra data.
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        // If this is a result we requested, handle it.
        if (requestCode == SUBACTIVITY_PREFERENCES) {
            // Our preferences have been updated; re-read them.
            updatePreferences();
        }
    }


    /**
     * Read our application preferences and configure ourself appropriately.
     */
    private void updatePreferences() {
    	SharedPreferences prefs =
    					PreferenceManager.getDefaultSharedPreferences(this);
    	
        // See if sounds are enabled and how.
    	soundMode = SoundMode.QUIET;
    	try {
    		String smode = prefs.getString("soundMode", null);
    		soundMode = SoundMode.valueOf(smode);
    	} catch (Exception e) {
    		Log.i(TAG, "Pref: bad soundMode");
    	}
    	Log.i(TAG, "Prefs: sound " + soundMode);

    	vibeEnable = true;
    	try {
    		vibeEnable = prefs.getBoolean("vibeEnable", true);
    	} catch (Exception e) {
    		Log.i(TAG, "Pref: bad vibeEnable");
    	}
    	Log.i(TAG, "Prefs: vibeEnable " + vibeEnable);

        // Look for a recline angle.
    	reclineMode = 0;
    	try {
    		String rmode = prefs.getString("reclineMode", null);
    		reclineMode = Integer.valueOf(rmode);
    	} catch (Exception e) {
    		Log.i(TAG, "Pref: bad reclineMode");
    	}
    	Log.i(TAG, "Prefs: recline " + reclineMode);

    	boolean showPerf = false;
    	try {
    		showPerf = prefs.getBoolean("showPerf", false);
    	} catch (Exception e) {
    		Log.i(TAG, "Pref: bad showPerf");
    	}
    	Log.i(TAG, "Prefs: showPerf " + showPerf);
    	tableView.setShowPerf(showPerf);

    	boolean userLevels = false;
    	try {
    		userLevels = prefs.getBoolean("userLevels", false);
    	} catch (Exception e) {
    		Log.i(TAG, "Pref: bad userLevels");
    	}
    	Log.i(TAG, "Prefs: userLevels " + userLevels);
    	try {
    		levelManager.setLoadUserLevels(userLevels);
    	} catch (LevelException e) {
    		reportError(e);
    	}
    }

    
    // ******************************************************************** //
    // Sensor Handling.
    // ******************************************************************** //

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
		
		// Create a vector from Y and Z representing the "up/down" tilt,
		// then rotate that vector according to the user's recline prefs.
		Vector yVec = new Vector(values[1], values[2]);
		yVec = yVec.rotate(reclineMode);

		// Pass the X and Y accelerations to the table.
        tableView.setTilt(values[0], (float) yVec.x);
	}

	
	/**
	 * Called when the accuracy of a sensor has changed.
	 * 
	 * @param	sensor			The ID of the sensor being monitored.
	 * @param	accuracy		The new accuracy of this sensor.
	 */
	@Override
	public void onAccuracyChanged(int sensor, int accuracy) {
		// Don't need this.
	}


	// ******************************************************************** //
    // Resources.
    // ******************************************************************** //

	/**
	 * Get the value of a string from the resources.
	 * 
	 * @param	resId			Resource ID of the string.
	 * @return					The string value.
	 */
	String getResString(int resId) {
		return (String) appResources.getText(resId);
	}
	
	
	/**
	 * Load a bitmap and scale it to a specified size.
	 * 
	 * @param	res				Resource ID of the bitmap.
	 * @param	width			Desired width.
	 * @param	height			Desired height.
	 * @return					The bitmap.
	 */
	Bitmap getScaledBitmap(int res, int width, int height) {
		return getScaledBitmap(res, width, height, Matrix.ORotate.NONE);
	}
	
	
	/**
	 * Load a bitmap and scale it to a specified size.
	 * 
	 * @param	res				Resource ID of the bitmap.
	 * @param	width			Desired width.
	 * @param	height			Desired height.
	 * @return					The bitmap.
	 */
	Bitmap getScaledBitmap(int res, int width, int height, Matrix.ORotate rot) {
		Bitmap bmp = BitmapFactory.decodeResource(appResources, res);
		int w = bmp.getWidth();
		int h = bmp.getHeight();
		
		// Fix the size if required.
		if (w != width || h != height)
			bmp = Bitmap.createScaledBitmap(bmp, width, height, true);
		
		// Rotate if required.
		if (rot != Matrix.ORotate.NONE) {
			android.graphics.Matrix m = new android.graphics.Matrix();
			m.setRotate(rot.degrees, width / 2, height / 2);
			bmp = Bitmap.createBitmap(bmp, 0, 0, width, height, m, true);
		}
		
		return bmp;
	}


	// ******************************************************************** //
	// Sound / Vibration.
	// ******************************************************************** //
	
	/**
	 * Make a sound.
	 * 
	 * @param	which			ID of the sound to play.
     */
	void makeSound(Sound which) {
		if (soundMode == SoundMode.NONE)
			return;

		try {
			MediaPlayer mp = MediaPlayer.create(this, which.soundRes);
			mp.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
				public void onPrepared(MediaPlayer mp) { mp.start(); }
			});
			mp.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
				public void onCompletion(MediaPlayer mp) { mp.release(); }
			});
			
			float vol = 1.0f;
			if (soundMode == SoundMode.QUIET)
				vol = 0.3f;
			mp.setVolume(vol, vol);
			
			mp.prepareAsync();
		} catch (Exception e) {
			Log.d(TAG, e.toString());
		}
	}
	

	/**
	 * Make a "kick" with the vibrator.
	 * 
	 * @param	level			Strength of the desired kick, 0 - 1.
     */
	void kickVibe(double level) {
		if (!vibeEnable || vibrator == null)
			return;

		try {
			long time = (long) (level * 30);
			vibrator.vibrate(Math.min(time, 25));
		} catch (Exception e) {
			Log.d(TAG, e.toString());
		}
	}
	

    // ******************************************************************** //
    // Error Handling.
    // ******************************************************************** //

	/**
	 * Report an error to the user.
	 * 
	 * @param	e			Exception representing the error.
	 */
	public void reportError(Exception e) {
	   	new AlertDialog.Builder(this).
	   							setIcon(android.R.drawable.ic_dialog_alert).
	   							setTitle(R.string.title_level_error).
	   							setMessage(e.getMessage()).
	   							setPositiveButton(R.string.button_close, null).
	   							show();
	}
	
	
    // ******************************************************************** //
    // Class Data.
    // ******************************************************************** //

    // Debugging tag.
	@SuppressWarnings("unused")
	private static final String TAG = "plughole";

	// Request codes used to identify requests to sub-activities.
	// Display and edit preferences.
    private static final int SUBACTIVITY_PREFERENCES = 1;
    
    
	// ******************************************************************** //
	// Private Data.
	// ******************************************************************** //

	// Application resources for this app.
	private Resources appResources;

    // The View in which the game is running.
    private TableView tableView;

    // The overlay text widget.
    private TextView overlayText;
    
    // Level manager.
    private LevelManager levelManager;

    /** The sensor manager, which we use to interface to all sensors. */
    private SensorManager sensorManager;
    
    // The vibrator.
    private Vibrator vibrator;

	// Dialog used to display about etc.
	private InfoBox messageDialog;

	// Current sound mode.
	private SoundMode soundMode;
	
	// True to enable the vibrator.
	private boolean vibeEnable;
	
	// Current recline mode.  This re-calibrates our idea of horizontal, so
	// the player can hold the device tilted back at the set angle in degrees.
	private int reclineMode = 0;
	
}

