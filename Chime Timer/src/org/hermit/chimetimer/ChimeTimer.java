
/**
 * Chime Timer: a simple and elegant timer.
 * <br>Copyright 2011 Ian Cameron Smith
 * 
 * <p>This app is a configurable, but simple and nice countdown timer.
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
import android.widget.ArrayAdapter;
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
        setContentView(R.layout.main_view);

        // Create the message and info boxes.
        setAboutInfo(R.string.about_text);
        setHomeInfo(R.string.url_homepage);
        setLicenseInfo(R.string.url_license);

        // Set up our data.
        timerConfigs = new TimerConfig[TimerConfig.NUM_TIMERS];
        timerBaseNames = getResources().getStringArray(R.array.timer_choices);
        timerNames = new String[TimerConfig.NUM_TIMERS];
        for (int i = 0; i < TimerConfig.NUM_TIMERS; ++i) {
        	timerConfigs[i] = new TimerConfig();
        	timerNames[i] = timerBaseNames[i];
        }

        // We want the audio controls to control our sound volume.
        this.setVolumeControlStream(AudioManager.STREAM_MUSIC);

		// Clock

		timeText = new StringBuilder(8);
		timeText.append("00:00:00");

		// Get the relevant widgets.
        timerChoice = (Spinner) findViewById(R.id.timer_choice);
        preField = (TextView) findViewById(R.id.timer_pre);
		timeField = (TextView) findViewById(R.id.timer_time);
		
		// Set the content of the timer choice widget.
        timerAdapter = new ArrayAdapter<String>(this,
                		android.R.layout.simple_spinner_item, timerNames);
        timerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        timerChoice.setAdapter(timerAdapter);

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
        
        // Kick off our service, if it's not running.
        launchService();

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

        // If the service is running, unbind from it but don't stop it.
        // But if it's not running, stop it.
        if (chimerService != null) {
        	if (!chimerService.isRunning())
        		shutdownService();
        	else
        		unbindService();
        }
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
        	chimerService.unregisterClient(tickHandler);
        	
            unbindService(serviceConnection);
            chimerService = null;
        }
    }


    /**
     * Shut down the app, including the background service.
     */
    private void shutdownService() {
        if (chimerService != null) {
        	chimerService.shutdown();
        	
            unbindService(serviceConnection);
            chimerService = null;
        }
    }
    

    /**
     * Shut down the app, including the background service.
     */
    private void shutdownCompletely() {
    	shutdownService();
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
        	
            // Register for tick updates.
        	chimerService.registerClient(tickHandler);
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            Log.i(TAG, "M onServiceDisconnected()");
            
            chimerService = null;
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
    	case R.id.menu_exit:
            Log.i(TAG, "M User exit");
            shutdownCompletely();
     		break;
    	default:
    		return super.onOptionsItemSelected(item);
    	}
    	
    	return true;
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
	        c.name = prefs.getString(base + "name", c.name).trim();
	        c.preTime = prefs.getLong(base + "preTime", c.preTime);
	        c.startBell = prefs.getInt(base + "startBell", c.startBell);
	        c.runTime = prefs.getLong(base + "runTime", c.runTime);
	        c.endBell = prefs.getInt(base + "endBell", c.endBell);
	        
	        if (c.name.length() > 0)
	        	timerNames[timer] = c.name;
	        else
	        	timerNames[timer] = timerBaseNames[timer];
		}
		
        timerAdapter.notifyDataSetChanged();
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
        Log.i(TAG, "M select timer " + pos + " -> " + currentConfig);
		if (currentConfig.preTime > 0) {
			preField.setVisibility(View.VISIBLE);
			updateClock(preField, currentConfig.preTime);
		} else {
			preField.setVisibility(View.INVISIBLE);
		}
		updateClock(timeField, currentConfig.runTime);
	}


    /**
     * Toggle the running state of the timer.
     */
    private void startStop() {
    	if (chimerService == null)
    		return;
    	
		switch (chimerService.getState()) {
		case ChimerService.STATE_READY:
	        Log.i(TAG, "M button READY");
    		startTimer();
			break;
		case ChimerService.STATE_PRE:
	        Log.i(TAG, "M button PRE");
    		stopTimer();
			break;
		case ChimerService.STATE_RUNNING:
	        Log.i(TAG, "M button RUN");
    		stopTimer();
			break;
		case ChimerService.STATE_FINISHED:
	        Log.i(TAG, "M button FINISHED");
    		stopTimer();
			break;
		}
    }
    

    /**
     * Start button has been clicked; start the timer.
     */
    private void startTimer() {
    	if (chimerService == null)
    		return;
    	
    	chimerService.startTimer(currentConfig);
    }
    

    /**
     * Stop button has been clicked; stop the timer.
     */
    private void stopTimer() {
    	if (chimerService == null)
    		return;
    	
    	chimerService.stopTimer();
    }
    
	
	// Handler for updates.  We need this to get back onto
	// our thread so we can update the GUI.
	private Handler tickHandler = new Handler() {
		@Override
		public void handleMessage(Message m) {
			int state = m.what;
			switch (state) {
			case ChimerService.STATE_READY:
		        Log.i(TAG, "M tick READY");
		        selectTimer(currentTimer);
		    	startButton.setText(R.string.but_start);
		    	startButton.setBackgroundColor(BUT_READY);
				break;
			case ChimerService.STATE_PRE:
		        Log.i(TAG, "M tick PRE " + m.arg1);
				updateClock(preField, m.arg1);
		    	startButton.setText(R.string.but_stop);
		    	startButton.setBackgroundColor(BUT_PRE);
				break;
			case ChimerService.STATE_RUNNING:
		        Log.i(TAG, "M tick RUN " + m.arg1);
				updateClock(preField, 0);
				updateClock(timeField, m.arg1);
		    	startButton.setText(R.string.but_stop);
		    	startButton.setBackgroundColor(BUT_RUNNING);
				break;
			case ChimerService.STATE_FINISHED:
		        Log.i(TAG, "M tick FINISHED");
		    	startButton.setText(R.string.but_done);
		    	startButton.setBackgroundColor(BUT_FINISHED);
				break;
			}
		}
	};

	
    /**
     * Display the current date and time.
     * 
     * @param	msLeft		Number of ms left to run.  If zero, we're done.
     */
    private void updateClock(TextView field, long msLeft) {
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
    	field.setText(timeText);
	}


	// ******************************************************************** //
	// Class Data.
	// ******************************************************************** //

    // Debugging tag.
	private static final String TAG = "ChimeTimer";
	
	// Button colour for state ready.
	private static final int BUT_READY = 0xff80ff80;
	
	// Button colour for state pre-time running.
	private static final int BUT_PRE = 0xffc0c000;
	
	// Button colour for state main running.
	private static final int BUT_RUNNING = 0xffff8080;
	
	// Button colour for state finished.
	private static final int BUT_FINISHED = 0xff8080ff;


	// ******************************************************************** //
	// Private Data.
	// ******************************************************************** //
    
    // Our service.  null if we haven't bound to it yet.
    private ChimerService chimerService = null;
    
	// Buffer we create the time display in.
	private StringBuilder timeText;

    // Timer selector spinner.
    private Spinner timerChoice;

    // Fields for displaying the pre-timer and the main timer.
    private TextView preField;
    private TextView timeField;
    
    // Start / stop button.
    private Button startButton;
    
    // Current timer configurations.
    private TimerConfig[] timerConfigs;

    // Index of the timer we're editing.
    private int currentTimer = 0;

    // Basic timer names, and the current configured timer names.
    private String[] timerBaseNames;
    private String[] timerNames;
    
    // Array adapter which provides the choices of the timer names.
    private  ArrayAdapter<String> timerAdapter;

	// Current timer's configuration.
	private TimerConfig currentConfig = null;
	
}

