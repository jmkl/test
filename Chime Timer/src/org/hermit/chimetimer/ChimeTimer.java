

package org.hermit.chimetimer;


import java.util.prefs.Preferences;

import org.hermit.android.core.MainActivity;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;


public class ChimeTimer
	extends MainActivity
{

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
	 * @param	icicle			If the activity is being re-initialised
	 * 							after previously being shut down then this
	 * 							Bundle contains the data it most recently
	 * 							supplied in onSaveInstanceState(Bundle).
	 * 							Note: Otherwise it is null.
	 */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.clock_view);
        
        // We want the audio controls to control our sound volume.
        this.setVolumeControlStream(AudioManager.STREAM_MUSIC);
        
        // Load the sounds.
        soundPool = createSoundPool();

		// Clock

		timeText = new StringBuilder(8);
		timeText.append("00:00:00");

		// Get the relevant widgets.
		timeField = (TextView) findViewById(R.id.timer_time);

		// Get the relevant widgets.
		startButton = (Button) findViewById(R.id.start_button);
		startButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				startStop();
			}
		});
		
		// Create a handler for tick events.
		tickHandler = new Handler() {
			@Override
			public void handleMessage(Message m) {
				updateClock(m.what);
			}
		};
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
     // FIXME: showFirstEula();
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
//        saveState(outState);
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
    	case R.id.menu_configure:
        	// Launch the configuration activity as a subactivity, so we
        	// know when it returns.
        	Intent cint = new Intent();
        	cint.setClass(this, Configuration.class);
        	startActivityForResult(cint, new MainActivity.ActivityListener() {
				@Override
				public void onActivityResult(int resultCode, Intent data) {
		            updatePreferences();
				}
        	});
        	break;
    	case R.id.menu_help:
            // TODO: Launch the help activity as a subactivity.
//            Intent hIntent = new Intent();
//            hIntent.setClass(this, Help.class);
//            startActivity(hIntent);
    		break;
    	case R.id.menu_about:
    		// TODO: show the about box.
//            showAbout();
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

//    	boolean nautTime = false;
//    	try {
//    		nautTime = prefs.getBoolean("nautTime", false);
//    	} catch (Exception e) {
//    		Log.i(TAG, "Pref: bad nautTime");
//    	}
//    	Log.i(TAG, "Prefs: nautTime " + nautTime);
//    	timeModel.setNauticalTime(nautTime);
//
//    	boolean debugSpace = false;
//    	try {
//    		debugSpace = prefs.getBoolean("debugSpace", false);
//    	} catch (Exception e) {
//    		Log.i(TAG, "Pref: bad debugSpace");
//    	}
//    	Log.i(TAG, "Prefs: debugSpace " + debugSpace);
//
//    	boolean debugTime = false;
//    	try {
//    		debugTime = prefs.getBoolean("debugTime", false);
//    	} catch (Exception e) {
//    		Log.i(TAG, "Pref: bad debugTime");
//    	}
//    	Log.i(TAG, "Prefs: debugTime " + debugTime);
    }
    
    
	// ******************************************************************** //
	// State Control.
	// ******************************************************************** //

    /**
     * Start/stop button has been clicked; take the appropriate action.
     */
    private void startStop() {
    	if (ticker == null) {
    		// Start the tick events.
    		ticker = new Ticker(System.currentTimeMillis(), 5000);
    		startButton.setText(R.string.but_stop);
    	} else {
    		// Start the tick events.
    		ticker.kill();
    		ticker = null;
    		startButton.setText(R.string.but_start);
    	}
    }
    
    
    /**
     * Display the current date and time.
     * 
     * @param	msLeft		Number of ms left to run.  If zero, we're done.
     */
    private void updateClock(int msLeft) {
    	int sLeft = (msLeft + 500) / 1000;
    	int hour = sLeft / 3600;
    	int min = sLeft / 60 % 60;
    	int sec = sLeft % 60;
    	timeText.setCharAt(0, (char) ('0' + hour / 10));
    	timeText.setCharAt(1, (char) ('0' + hour % 10));
    	timeText.setCharAt(3, (char) ('0' + min / 10));
    	timeText.setCharAt(4, (char) ('0' + min % 10));
    	timeText.setCharAt(6, (char) ('0' + sec / 10));
    	timeText.setCharAt(7, (char) ('0' + sec % 10));
    	timeField.setText(timeText);
	}


	// ******************************************************************** //
	// Sound Playing.
	// ******************************************************************** //
    
    /**
     * Create a SoundPool containing the app's sound effects.
     */
    private SoundPool createSoundPool() {
        SoundPool pool = new SoundPool(3, AudioManager.STREAM_MUSIC, 0);
        for (SoundEffect sound : SoundEffect.values())
            sound.soundId = pool.load(this, sound.soundRes, 1);
        
        return pool;
    }

    
    /**
     * Make a sound.  Play it immediately.  Don't touch the queue.
     * 
     * @param	which			ID of the sound to play.
     */
    private void makeSound(SoundEffect which) {
        float vol = 1.0f;
        soundPool.play(which.soundId, vol, vol, 1, 0, 1f);
	}


    // ******************************************************************** //
    // Private Types.
    // ******************************************************************** //

    /**
     * The sounds that we make.
     */
	private static enum SoundEffect {
    	BELL_1(R.raw.s0),
    	BELL_2(R.raw.s1),
    	BELL_3(R.raw.s2),
    	BELL_4(R.raw.s3),
    	BELL_5(R.raw.s4),
    	BELL_6(R.raw.s5),
    	BELL_7(R.raw.s7);
	
    	private SoundEffect(int res) {
    		soundRes = res;
    	}
    	
    	// Resource ID for the sound file.
    	private final int soundRes;
    	
    	// Sound ID for playing.
        private int soundId = 0;        
    }


	/**
	 * Class which generates our ticks.
	 */
	private class Ticker extends Thread {
		public Ticker(long now, long durn) {
			endTime = now + durn;
			enable = true;
			start();
		}

		public void kill() {
			enable = false;
		}

		@Override
		public void run() {
			while (enable) {
				int remain = (int) (endTime - System.currentTimeMillis());
				if (remain < 0)
					remain = 0;
		    	tickHandler.sendEmptyMessage(remain);
				if (remain == 0)
					break;
				
				// Try to sleep up to the next 1-second boundary, so we
				// tick just about on the second.
				try {
					sleep(remain % 1000);
				} catch (InterruptedException e) {
					enable = false;
				}
			}
			enable = false;
    		ticker = null;
		}
		
		private final long endTime;
		private boolean enable;
	}


	// ******************************************************************** //
	// Class Data.
	// ******************************************************************** //

    // Debugging tag.
	private static final String TAG = "ChimeTimer";
	

	// ******************************************************************** //
	// Private Data.
	// ******************************************************************** //

    // Timer we use to generate tick events.
    private Ticker ticker = null;
	
	// Handler for updates.  We need this to get back onto
	// our thread so we can update the GUI.
	private Handler tickHandler;
    
	// Buffer we create the time display in.
	private StringBuilder timeText;
	   
    // Fields for displaying the date and time.
    private TextView timeField;
    
    // Start / stop button.
    private Button startButton;
    
    // Sound pool used for sound effects.
    private SoundPool soundPool = null;

}

