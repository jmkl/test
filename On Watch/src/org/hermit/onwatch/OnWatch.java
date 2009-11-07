
/**
 * On Watch: sailor's watchkeeping assistant.
 * <br>Copyright 2009 Ian Cameron Smith
 * 
 * <p>This program acts as a bridge buddy for a cruising sailor on watch.
 * It displays time and navigation data, sounds chimes and alarms, etc.
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


package org.hermit.onwatch;

import java.util.LinkedList;

import org.hermit.android.core.MainActivity;
import org.hermit.android.core.SplashActivity;
import org.hermit.android.widgets.TimeZoneActivity;

import android.app.AlarmManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;


/**
 * This class is the main activity for OnWatch.
 */
public class OnWatch
	extends MainActivity
{

    // ******************************************************************** //
    // Public Types and Constants.
    // ******************************************************************** //

    /**
     * The sounds that we make.
     */
    static enum Sound {
    	/** A single bell. */
    	BELL1(R.raw.sad_bell),
    	
    	/** Two bells. */
    	BELL2(R.raw.two_bells),
    	
    	/** An alert sound. */
    	RINGRING(R.raw.ring_ring);
    	
    	private Sound(int res) {
    		soundRes = res;
    	}
    	
     	/** Resource ID for the sound file. */
     	int soundRes;
    }


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
	 * @param	icicle			If the activity is being re-initialised
	 * 							after previously being shut down then this
	 * 							Bundle contains the data it most recently
	 * 							supplied in onSaveInstanceState(Bundle).
	 * 							Note: Otherwise it is null.
	 */
    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        
        // We want the audio controls to control our sound volume.
        this.setVolumeControlStream(AudioManager.STREAM_MUSIC);
        
        // Create our EULA box.
        createEulaBox(R.string.eula_title, R.string.eula_text, R.string.button_close);       

        // Set up the standard dialogs.
        createMessageBox(R.string.button_close);
        setAboutInfo(R.string.about_text, R.string.help_text);
        setHomeInfo(R.string.button_homepage, R.string.url_homepage);
        setManualInfo(R.string.button_manual, R.string.url_manual);
        setLicenseInfo(R.string.button_license, R.string.url_license);

        // Create the time and location models.
        locationModel = LocationModel.getInstance(this);
        timeModel = TimeModel.getInstance(this);
 
        // Create the application GUI.
        setContentView(R.layout.on_watch);
        viewController = new MainController(this);
        
		// Create a handler for tick events.
		tickHandler = new Handler() {
			@Override
			public void handleMessage(Message m) {
				long time = System.currentTimeMillis();
				timeModel.tick(time);
				locationModel.tick(time);
		        viewController.tick(time);
			}
		};

		// Create the sound queue and a handler to launch sounds.
		soundQueue = new LinkedList<Sound>();
		soundHandler = new Handler() {
			@Override
			public void handleMessage(Message m) {
				playQueuedSound();
			}
		};
		soundPlaying = false;

        // Restore our preferences.
        updatePreferences();
        
        // Restore our app state, if this is a restart.
        if (icicle != null)
            restoreState(icicle);

        // First time, show the splash screen.
        if (!shownSplash) {
            SplashActivity.launch(this, R.drawable.splash_screen, SPLASH_TIME);
            shownSplash = true;
        }
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
        
        viewController.start();
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
        
        viewController.resume();
        locationModel.resume();

        // Start the 1-second tick events.
		if (ticker != null)
			ticker.kill();
		ticker = new Ticker();
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
        
        // Save our state.
        saveState(outState);
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
        
		// Stop the tick events.
		if (ticker != null) {
			ticker.kill();
			ticker = null;
		}
		
        locationModel.pause();
        viewController.pause();
    }


    /**
     * Called when you are no longer visible to the user.  You will next
     * receive either {@link #onStart}, {@link #onDestroy}, or nothing,
     * depending on later user activity.
     * 
     * <p>Note that this method may never be called, in low memory situations
     * where the system does not have enough memory to keep your activity's
     * process running after its {@link #onPause} method is called.
     */
    @Override
	protected void onStop() {
        Log.i(TAG, "onStop()");
        
        super.onStop();
		
        viewController.stop();
    }


    // ******************************************************************** //
    // Save and Restore.
    // ******************************************************************** //

    /**
     * Save the application's state to the given Bundle.
     * 
     * @param   icicle          A Bundle in which the app's state should
     *                          be saved.
     */
    public void saveState(Bundle icicle) {
        icicle.putBoolean("shownSplash", shownSplash);
        
        // Now save our sub-components.
        viewController.saveState(icicle);
    }


    /**
     * Restore the application's state from the given Bundle.
     * 
     * @param   icicle          The app's saved state.
     */
    public void restoreState(Bundle icicle) {
        shownSplash = icicle.getBoolean("shownSplash");
        
        // Now restore our sub-components.
        viewController.restoreState(icicle);
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
        case R.id.menu_prefs:
        	// Launch the preferences activity as a subactivity, so we
        	// know when it returns.
        	Intent pint = new Intent();
        	pint.setClass(this, Preferences.class);
        	startActivityForResult(pint, new MainActivity.ActivityListener() {
				@Override
				public void onActivityResult(int resultCode, Intent data) {
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
    

    /**
     * Read our application preferences and configure ourself appropriately.
     */
    private void updatePreferences() {
    	SharedPreferences prefs =
    					PreferenceManager.getDefaultSharedPreferences(this);

    	boolean nautTime = false;
    	try {
    		nautTime = prefs.getBoolean("nautTime", false);
    	} catch (Exception e) {
    		Log.i(TAG, "Pref: bad nautTime");
    	}
    	Log.i(TAG, "Prefs: nautTime " + nautTime);
    	timeModel.setNauticalTime(nautTime);

    	boolean debugSpace = false;
    	try {
    		debugSpace = prefs.getBoolean("debugSpace", false);
    	} catch (Exception e) {
    		Log.i(TAG, "Pref: bad debugSpace");
    	}
    	Log.i(TAG, "Prefs: debugSpace " + debugSpace);

    	boolean debugTime = false;
    	try {
    		debugTime = prefs.getBoolean("debugTime", false);
    	} catch (Exception e) {
    		Log.i(TAG, "Pref: bad debugTime");
    	}
    	Log.i(TAG, "Prefs: debugTime " + debugTime);
    	
    	viewController.setDebug(debugSpace, debugTime);
   }


	// ******************************************************************** //
	// Timezone Handling.
	// ******************************************************************** //

    /**
     * Ask the user to pick a new timezone.
     */
    void requestTimezone() {
    	Intent intent = new Intent();
    	intent.setClass(this, TimeZoneActivity.class);
    	intent.putExtra("addZoneId", getString(R.string.timezone_naut));
    	intent.putExtra("addZoneOff", getString(R.string.timezone_naut_off));

    	startActivityForResult(intent, new MainActivity.ActivityListener() {
			@Override
			public void onActivityResult(int resultCode, Intent data) {
	            if (resultCode == RESULT_OK)
	            	setTimezone(data);
			}
    	});
    }
    

    /**
     * The user has selected a new timezone; set it up.
     * 
     * @param	data			Data returned from the timezone picker.
     */
    private void setTimezone(Intent data) {
    	String zoneId = data.getStringExtra("zoneId");
    	Log.i(TAG, "Set timezone " + zoneId);
    	
    	// Is this nautical time?
    	boolean nautTime = zoneId.equals(getString(R.string.timezone_naut));
    	
    	// Save the nautical time preference.
    	SharedPreferences prefs =
			PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean("nautTime", nautTime);
        editor.commit();

    	// Set up the time model.  If this is nautical time, the time model
        // will take care of the actual timezone; otherwise set it now.
		timeModel.setNauticalTime(nautTime);
    	if (!nautTime) {
    		timeModel.setNauticalTime(false);
    		AlarmManager alarm = (AlarmManager) getSystemService(ALARM_SERVICE);
    		alarm.setTimeZone(zoneId);
    	}
    }
    

	// ******************************************************************** //
	// Sound.
	// ******************************************************************** //

    /**
     * Sound watch bells.
     * 
     * @param	count			How many bells to sound.
     */
    void soundBells(int count) {
    	while (count > 0) {
    		if (count >= 2) {
    	    	queueSound(Sound.BELL2);
    			count -= 2;
    		} else {
    	    	queueSound(Sound.BELL1);
    			count -= 1;
    		}
    	}
    }
    

    /**
     * Add a sound to the queue of sounds to be played.  Play at once
     * if the queue is empty.
     * 
     * @param	which			ID of the sound to queue for play.
     */
    void queueSound(Sound which) {
		synchronized (soundQueue) {
			soundQueue.add(which);
			soundHandler.sendEmptyMessage(0);
		}
    }

    
    /**
     * Play a sound from the queue.
     */
    private void playQueuedSound() {
    	synchronized (soundQueue) {
    		try {
    			// If we're already playing, wait.
    			if (soundPlaying)
    				return;
    			
    			// See if there's a queued sound to play.
    			Sound which = soundQueue.poll();
    			if (which == null)
    				return;
    			soundPlaying = true;

    			MediaPlayer mp = MediaPlayer.create(this, which.soundRes);
    			mp.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
    				public void onPrepared(MediaPlayer mp) { mp.start(); }
    			});
    			mp.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
    				public void onCompletion(MediaPlayer mp) {
    					mp.release();
    					synchronized (soundQueue) {
    						soundPlaying = false;
        					playQueuedSound();
    					}
    				}
    			});

    			mp.prepareAsync();
    			//			mp.start();
    		} catch (Exception e) {
    			Log.d(TAG, "Sound queue play error: " + e.getMessage());
    		}
    	}
    }


    /**
     * Make a sound.  Play it immediately.  Don't touch the queue.
     * 
     * @param	which			ID of the sound to play.
     */
    void makeSound(Sound which) {
		try {
			MediaPlayer mp = MediaPlayer.create(this, which.soundRes);
			mp.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
				public void onPrepared(MediaPlayer mp) { mp.start(); }
			});
			mp.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
				public void onCompletion(MediaPlayer mp) { mp.release(); }
			});
			
			mp.prepareAsync();
//			mp.start();
		} catch (Exception e) {
            Log.d(TAG, "Sound play error: " + e.getMessage());
		}
	}
	

    // ******************************************************************** //
    // Private Types.
    // ******************************************************************** //

	/**
	 * Class which generates our ticks.
	 */
	private class Ticker extends Thread {
		public Ticker() {
			enable = true;
			start();
		}

		public void kill() {
			enable = false;
		}

		@Override
		public void run() {
			while (enable) {
		    	tickHandler.sendEmptyMessage(1);
				
				// Try to sleep up to the next 1-second boundary, so we
				// tick just about on the second.
				try {
					long time = System.currentTimeMillis();
					sleep(1000 - time % 1000);
				} catch (InterruptedException e) {
					enable = false;
				}
			}
		}
		
		private boolean enable;
	}


	// ******************************************************************** //
	// Class Data.
	// ******************************************************************** //

    // Debugging tag.
	@SuppressWarnings("unused")
	private static final String TAG = "onwatch";
	
	// Time in ms for which the splash screen is displayed.
	private static final long SPLASH_TIME = 5000;


	// ******************************************************************** //
	// Private Data.
	// ******************************************************************** //

	// The time model we use for all our timekeeping.
	private TimeModel timeModel;

	// The location model we use for all our positioning.
	private LocationModel locationModel;

	// View controller used to manage switching between the various views.
	private MainController viewController = null;

    // Timer we use to generate tick events.
    private Ticker ticker = null;
	
	// Handler for updates.  We need this to get back onto
	// our thread so we can update the GUI.
	private Handler tickHandler;

	// Handler for sounds.  We need this to get back onto the main thread.
	private Handler soundHandler;
	
	// Queue of sounds to be played.
	private LinkedList<Sound> soundQueue;
    
    // Log whether we showed the splash screen yet this run.
    private boolean shownSplash = false;

	// True if a queued sound is currently playing.
	private boolean soundPlaying;

}

