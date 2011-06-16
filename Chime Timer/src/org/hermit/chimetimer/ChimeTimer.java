

package org.hermit.chimetimer;


import org.hermit.android.core.MainActivity;
import org.hermit.chimetimer.ChimerService.ChimerBinder;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.Spinner;
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
        Log.i(TAG, "M onCreate()");
        
        super.onCreate(savedInstanceState);
        setContentView(R.layout.clock_view);
        
        // Kick off our service, if it's not running.
        launchService();

        // Set up our data.
        timerConfigs = new TimerConfig[TimerConfig.NUM_TIMERS];
        for (int i = 0; i < TimerConfig.NUM_TIMERS; ++i)
        	timerConfigs[i] = new TimerConfig();

        // We want the audio controls to control our sound volume.
        this.setVolumeControlStream(AudioManager.STREAM_MUSIC);

		// Clock

		timeText = new StringBuilder(8);
		timeText.append("00:00:00");

		// Get the relevant widgets.
        timerChoice = (Spinner) findViewById(R.id.timer_choice);
		timeField = (TextView) findViewById(R.id.timer_time);
        
        // Handle timer selections.
        timerChoice.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> a, View v, int pos, long id) {
				selectTimer(pos);
			}

			@Override
			public void onNothingSelected(AdapterView<?> a) {
			}
		});

		// Handle start/stop.
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
        
    	currentTimer = 0;
    }
    
    
    /**
     * Called after {@link #onCreate} or {@link #onStop} when the current
     * activity is now being displayed to the user.  It will
     * be followed by {@link #onRestart}.
     */
    @Override
	protected void onStart() {
        Log.i(TAG, "M onStart()");
        
        super.onStart();

        // Bind to the service.
        bindService();
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
        Log.i(TAG, "M onResume()");

        super.onResume();
    	
    	// Our configuration may have changed -- reload it.
		loadConfigs();

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
        Log.i(TAG, "M onSaveInstanceState()");
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
        Log.i(TAG, "M onPause()");

        super.onPause();
        
		// Stop the tick events.
		if (ticker != null) {
			ticker.kill();
			ticker = null;
		}
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
        Log.i(TAG, "M onStop()");
        
        super.onStop();
        
        // Unbind from the service (but don't stop it).
        unbindService();
    }


    // ******************************************************************** //
    // Service Communications.
    // ******************************************************************** //

    /**
     * Start the service, if it's not running.
     */
    private void launchService() {
        Intent intent = new Intent(this, ChimerService.class);
        startService(intent);
    }
    

    /**
     * Bind to the service.
     */
    private void bindService() {
        Intent intent = new Intent(this, ChimerService.class);
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
    }
    

    /**
     * Pause the service (e.g. for maintenance).
     */
    public void pauseService() {
    	chimerService.pause();
    }
    

    /**
     * Resume the service from a pause.
     */
    public void resumeService() {
    	chimerService.resume();
    }
    

    /**
     * Unbind from the service -- without stopping it.
     */
    private void unbindService() {
        // Unbind from the OnWatch service.
        if (chimerService != null) {
            unbindService(serviceConnection);
            chimerService = null;
        }
    }


    /**
     * Shut down the app, including the background service.
     */
    private void shutdown() {
        if (chimerService != null) {
        	chimerService.shutdown();
            unbindService(serviceConnection);
            chimerService = null;
        }
        
    	finish();
    }
    

    /**
     * Defines callbacks for service binding, passed to bindService().
     */
    private ServiceConnection serviceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            Log.i(TAG, "M onServiceConnected()");
            
            // We've bound to ChimerService; cast the IBinder to
        	// the right type.
            ChimerBinder binder = (ChimerBinder) service;
            chimerService = binder.getService();
        	
            // Start all the views and give them the service.
//    		for (ViewFragment v : childViews)
//    			v.start(onWatchService);
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            Log.i(TAG, "M onServiceDisconnected()");
            
            chimerService = null;
            
            // Stop all the views.
//    		for (ViewFragment v : childViews)
//    			v.stop();
        }
        
    };


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
            Log.i(TAG, "M Launch config");
        	Intent cint = new Intent();
        	cint.setClass(this, Configuration.class);
        	startActivity(cint);
        	break;
    	case R.id.menu_help:
            Log.i(TAG, "M Launch help");
            // TODO: Launch the help activity as a subactivity.
//            Intent hIntent = new Intent();
//            hIntent.setClass(this, Help.class);
//            startActivity(hIntent);
    		break;
    	case R.id.menu_about:
            Log.i(TAG, "M Launch about");
            showAbout();
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
//    	SharedPreferences prefs =
//    					PreferenceManager.getDefaultSharedPreferences(this);

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
	 * Load from persistent storage all the timers' configurations.
	 */
	private void loadConfigs() {
		for (int timer = 0; timer < TimerConfig.NUM_TIMERS; ++timer) {
			String base = "timer" + timer + "_";
			SharedPreferences prefs = getSharedPreferences("timers", 0);
	        TimerConfig c = timerConfigs[timer];
	        c.name = prefs.getString(base + "name", c.name);
	        c.preTime = prefs.getLong(base + "preTime", c.preTime);
	        c.startBell = prefs.getInt(base + "startBell", c.startBell);
	        c.runTime = prefs.getLong(base + "runTime", c.runTime);
	        c.endBell = prefs.getInt(base + "endBell", c.endBell);
		}
		selectTimer(currentTimer);
	}


    /**
     * Select the timer to configure.
     * 
     * @param	pos			Index of the timer to configure.
     */
	private void selectTimer(int pos) {
		currentTimer = pos;
		currentConfig = timerConfigs[currentTimer];
		updateClock(currentConfig.runTime);
	}
	

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
    private void updateClock(long msLeft) {
    	int sLeft = (int) ((msLeft + 500) / 1000);
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
    // Private Types.
    // ******************************************************************** //

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
    
    // Our service.  null if we haven't bound to it yet.
    private ChimerService chimerService = null;

    // Timer we use to generate tick events.
    private Ticker ticker = null;
	
	// Handler for updates.  We need this to get back onto
	// our thread so we can update the GUI.
	private Handler tickHandler;
    
	// Buffer we create the time display in.
	private StringBuilder timeText;

    // Timer selector spinner.
    private Spinner timerChoice;

    // Fields for displaying the date and time.
    private TextView timeField;
    
    // Start / stop button.
    private Button startButton;
    
    // Current timer configurations.
    private TimerConfig[] timerConfigs;

    // Index of the timer we're editing.
    private int currentTimer = 0;
	
	// Current timer's configuration.
	private TimerConfig currentConfig = null;

}

