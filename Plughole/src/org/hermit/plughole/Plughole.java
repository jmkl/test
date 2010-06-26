
/**
 * Plughole: a rolling-ball accelerometer game.
 * <br>Copyright 2008-2010 Ian Cameron Smith
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


package org.hermit.plughole;

import org.hermit.android.core.MainActivity;
import org.hermit.plughole.LevelReader.LevelException;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.PowerManager;
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
	extends MainActivity
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

        // Get our power manager for wake locks.
        powerManager = (PowerManager) getSystemService(POWER_SERVICE);

        // Create the message and info boxes.
        setAboutInfo(R.string.about_text);
        setHomeInfo(R.string.url_homepage);
        setLicenseInfo(R.string.url_license);

        // We don't want a title bar or status bar.
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                		     WindowManager.LayoutParams.FLAG_FULLSCREEN);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        
        // Create a level manager.
        levelManager = new LevelManager(this);

        // Use the layout defined in XML.
        setContentView(createGui());

        // Give the TableView a handle to the TextView used for messages.
        tableView.setTextView(overlayText);

        // Restore our preferences.
        updatePreferences();

        if (icicle != null) {
            Log.w(TAG, "Restore a saved state");
        	tableView.restoreState(icicle);
        } else
            Log.w(TAG, "New start");

        vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
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
        
        tableView.onStart();
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
        
        // Take the wake lock if we want it.
        if (wakeLock != null && !wakeLock.isHeld())
            wakeLock.acquire();

        tableView.onResume();
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
        
        // Pause the game.
        tableView.onPause();

        // Let go the wake lock if we have it.
        if (wakeLock != null && wakeLock.isHeld())
            wakeLock.release();
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

        tableView.onStop();
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
            startActivityForResult(pIntent, new MainActivity.ActivityListener() {
                @Override
                public void onActivityFinished(int resultCode, Intent data) {
                    updatePreferences();
                }
            });
        	break;
    	case R.id.menu_help:
            // Launch the help activity as a subactivity.
//            Intent hIntent = new Intent();
//            hIntent.setClass(this, Help.class);
//            startActivity(hIntent);
    	    break;
    	case R.id.menu_about:
    	    showAbout();
     		break;
        case R.id.menu_exit:
        	finish();
        	break;
        }

        return false;
    }


    /**
     * Read our application preferences and configure ourself appropriately.
     */
    private void updatePreferences() {
    	SharedPreferences prefs =
    					PreferenceManager.getDefaultSharedPreferences(this);
    	
        // See if sounds are enabled and how.
    	soundMode = SoundMode.FULL;
    	try {
    		String smode = prefs.getString("soundMode", soundMode.toString());
    		soundMode = SoundMode.valueOf(smode);
    	} catch (Exception e) {
    		Log.i(TAG, "Pref: bad soundMode");
    	}
    	Log.i(TAG, "Prefs: sound " + soundMode);

        // See if the vibrator is enabled.
    	vibeEnable = true;
    	try {
    		vibeEnable = prefs.getBoolean("vibeEnable", vibeEnable);
    	} catch (Exception e) {
    		Log.i(TAG, "Pref: bad vibeEnable");
    	}
    	Log.i(TAG, "Prefs: vibeEnable " + vibeEnable);

    	// See whether the user wants the screen kept awake.
        boolean keepAwake = false;
        try {
            keepAwake = prefs.getBoolean("keepAwake", keepAwake);
        } catch (Exception e) {
            Log.e(TAG, "Pref: bad keepAwake");
        }
        if (keepAwake) {
            Log.i(TAG, "Prefs: keepAwake true: take the wake lock");
            if (wakeLock == null)
                wakeLock = powerManager.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, "My Tag");
            if (!wakeLock.isHeld())
                wakeLock.acquire();
        } else {
            Log.i(TAG, "Prefs: keepAwake false: release the wake lock");
            if (wakeLock != null && wakeLock.isHeld())
                wakeLock.release();
            wakeLock = null;
        }

        // Look for a recline angle.
    	int reclineMode = 0;
    	try {
    		String rmode = prefs.getString("reclineMode", "" + reclineMode);
    		reclineMode = Integer.valueOf(rmode);
    	} catch (Exception e) {
    		Log.i(TAG, "Pref: bad reclineMode");
    	}
    	Log.i(TAG, "Prefs: recline " + reclineMode);
    	tableView.setReclineAngle(reclineMode);

    	boolean showPerf = true;
    	try {
    		showPerf = prefs.getBoolean("showPerf", showPerf);
    	} catch (Exception e) {
    		Log.i(TAG, "Pref: bad showPerf");
    	}
    	Log.i(TAG, "Prefs: showPerf " + showPerf);
    	tableView.setShowPerf(showPerf);

    	boolean userLevels = true;
    	try {
    		userLevels = prefs.getBoolean("userLevels", userLevels);
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
	private static final String TAG = "plughole";
    
    
	// ******************************************************************** //
	// Private Data.
	// ******************************************************************** //
    
    // Our power manager.
    private PowerManager powerManager = null;
    
    // Wake lock used to keep the screen alive.  Null if we aren't going
    // to take a lock; non-null indicates that the lock should be taken
    // while we're actually running.
    private PowerManager.WakeLock wakeLock = null;

	// Application resources for this app.
	private Resources appResources;

    // The View in which the game is running.
    private TableView tableView;

    // The overlay text widget.
    private TextView overlayText;
    
    // Level manager.
    private LevelManager levelManager;
    
    // The vibrator.
    private Vibrator vibrator;

	// Current sound mode.
	private SoundMode soundMode;
	
	// True to enable the vibrator.
	private boolean vibeEnable;
	
}

