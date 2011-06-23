
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


import org.hermit.android.widgets.TimeoutPicker;
import org.hermit.android.widgets.TimeoutPickerDialog;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;


public class Configuration
	extends Activity
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
        Log.i(TAG, "Config onCreate()");

        super.onCreate(savedInstanceState);
        setContentView(R.layout.config_view);

		timeText = new StringBuilder(8);
		timeText.append("00:00:00");

        // Set up our data.
        timerConfigs = new TimerConfig[TimerConfig.NUM_TIMERS];
        timerBaseNames = getResources().getStringArray(R.array.timer_choices);
        timerNames = new String[TimerConfig.NUM_TIMERS];
        for (int i = 0; i < TimerConfig.NUM_TIMERS; ++i) {
        	timerConfigs[i] = new TimerConfig();
        	timerNames[i] = timerBaseNames[i];
        }
        bellNames = getResources().getStringArray(R.array.bell_choices);

		// Get the relevant widgets.
        timerChoice = (Spinner) findViewById(R.id.timer_choice);
        timerName = (EditText) findViewById(R.id.name);
        butPreTime = (Button) findViewById(R.id.but_pre_time);
        startBell = (Spinner) findViewById(R.id.start_bell);
        butRunTime = (Button) findViewById(R.id.but_run_time);
        endBell = (Spinner) findViewById(R.id.finish_bell);
		
		// Set the content of the timer choice widget.
        timerAdapter = new ArrayAdapter<String>(this,
                						R.layout.spinner_item, timerNames);
        timerAdapter.setDropDownViewResource(R.layout.spinner_dropdown);
        timerChoice.setAdapter(timerAdapter);
		
		// Set the content of the bell choice widgets.
        ArrayAdapter<String> bellAdapter = new ArrayAdapter<String>(this,
                						R.layout.spinner_item, bellNames);
        bellAdapter.setDropDownViewResource(R.layout.spinner_dropdown);
        startBell.setAdapter(bellAdapter);
        endBell.setAdapter(bellAdapter);

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

        // Handle timer property changes.
        timerName.addTextChangedListener(new TextWatcher() {
        	@Override
        	public void onTextChanged(CharSequence s, int st, int b, int c) {
        		currentConfig.name = timerName.getText().toString().trim();
        		saveConfig();
        	}
        	@Override
        	public void beforeTextChanged(CharSequence s, int st, int c, int a) {
        	}
        	@Override
        	public void afterTextChanged(Editable s) {
        	}
        });

        butPreTime.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				preTimeDialog.updateTime(currentConfig.preTime);
				preTimeDialog.show();
			}
		});
        
        preTimeDialog = new TimeoutPickerDialog(this, new TimeoutPickerDialog.OnTimeSetListener() {
    		@Override
    		public void onTimeSet(TimeoutPicker view, long millis) {
    			updateButton(butPreTime, millis);
        		currentConfig.preTime = millis;
        		saveConfig();
    		}
    	}, 0);

        startBell.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
        	@Override
        	public void onItemSelected(AdapterView<?> a, View v, int pos, long id) {
        		if (!uiUpdating) {
        			if (pos != 0)
        				playBell(pos - 1);
        			currentConfig.startBell = pos;
        			saveConfig();
        		}
        	}

        	@Override
        	public void onNothingSelected(AdapterView<?> a) {
        	}
        });

        butRunTime.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				runTimeDialog.updateTime(currentConfig.runTime);
				runTimeDialog.show();
			}
		});
        
        runTimeDialog = new TimeoutPickerDialog(this, new TimeoutPickerDialog.OnTimeSetListener() {
    		@Override
    		public void onTimeSet(TimeoutPicker view, long millis) {
    			updateButton(butRunTime, millis);
        		currentConfig.runTime = millis;
        		saveConfig();
    		}
    	}, 0);

        endBell.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
        	@Override
        	public void onItemSelected(AdapterView<?> a, View v, int pos, long id) {
        		if (!uiUpdating) {
        			if (pos != 0)
        				playBell(pos - 1);
        			currentConfig.endBell = pos;
        			saveConfig();
        		}
        	}

        	@Override
        	public void onNothingSelected(AdapterView<?> a) {
        	}
        });

        // Set up the Done button.
        Button done = (Button) findViewById(R.id.but_done);
        done.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				finish();
			}
		});

    	currentTimer = 0;
    	loadConfigs();
    }
    
    
    /**
     * Called after {@link #onCreate} or {@link #onStop} when the current
     * activity is now being displayed to the user.  It will
     * be followed by {@link #onRestart}.
     */
    @Override
	protected void onStart() {
        Log.i(TAG, "Config onStart()");
        
        super.onStart();

        soundManager = new Sounds(this);
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
        Log.i(TAG, "Config onResume()");

        super.onResume();
        
        soundManager.resume();
        
        uiUpdating = false;
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
        Log.i(TAG, "Config onSaveInstanceState()");
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
        Log.i(TAG, "Config onPause()");
        
        super.onPause();
        
		soundManager.pause();
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
        Log.i(TAG, "Config onStop()");
        
        super.onStop();
        
		soundManager.shutdown();
    }


	// ******************************************************************** //
	// Timer Configuration.
	// ******************************************************************** //

    /**
     * Select the timer to configure.
     * 
     * @param	pos			Index of the timer to configure.
     */
	private void selectTimer(int pos) {
		currentTimer = pos;
		currentConfig = timerConfigs[currentTimer];
		
        uiUpdating = true;
		
		timerName.setText(currentConfig.name);
		updateButton(butPreTime, currentConfig.preTime);
		startBell.setSelection(currentConfig.startBell);
		updateButton(butRunTime, currentConfig.runTime);
		endBell.setSelection(currentConfig.endBell);
		
        uiUpdating = false;
	}
	    
	
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
	        
	        if (c.name.length() > 0)
	        	timerNames[timer] = c.name;
	        else
	        	timerNames[timer] = timerBaseNames[timer];
		}
		
        timerAdapter.notifyDataSetChanged();
		selectTimer(currentTimer);
	}


	/**
	 * Save to persistent storage the current timer's configuration.
	 */
	private void saveConfig() {
		String base = "timer" + currentTimer + "_";
		SharedPreferences prefs = getSharedPreferences("timers", 0);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(base + "name", currentConfig.name);
        editor.putLong(base + "preTime", currentConfig.preTime);
        editor.putInt(base + "startBell", currentConfig.startBell);
        editor.putLong(base + "runTime", currentConfig.runTime);
        editor.putInt(base + "endBell", currentConfig.endBell);
        editor.commit();
        
        if (currentConfig.name.length() > 0)
        	timerNames[currentTimer] = currentConfig.name;
        else
        	timerNames[currentTimer] = timerBaseNames[currentTimer];
        timerAdapter.notifyDataSetChanged();
	}

	
    /**
     * Display the given timeout in the specified button.
     * 
     * @param	but			Button to set.
     * @param	ms			Time in ms to display.
     */
    private void updateButton(Button but, long ms) {
    	int sLeft = (int) ((ms + 500) / 1000);
    	int hour = sLeft / 3600;
    	int min = sLeft / 60 % 60;
    	int sec = sLeft % 60;
    	timeText.setCharAt(0, (char) ('0' + hour / 10));
    	timeText.setCharAt(1, (char) ('0' + hour % 10));
    	timeText.setCharAt(3, (char) ('0' + min / 10));
    	timeText.setCharAt(4, (char) ('0' + min % 10));
    	timeText.setCharAt(6, (char) ('0' + sec / 10));
    	timeText.setCharAt(7, (char) ('0' + sec % 10));
    	but.setText(timeText);
	}


	// ******************************************************************** //
	// Bell Selection.
	// ******************************************************************** //

	private void playBell(int ordinal) {
        Log.i(TAG, "Config bell " + ordinal);
		Sounds.SoundEffect bell = Sounds.SoundEffect.valueOf(ordinal);
		if (bell != null)
			soundManager.makeSound(bell);
	}
	
	
	// ******************************************************************** //
	// Class Data.
	// ******************************************************************** //

    // Debugging tag.
	private static final String TAG = "ChimeTimer";
	 
	 
	// ******************************************************************** //
	// Private Data.
	// ******************************************************************** //
    
    // Sound manager used for sound effects.
    private Sounds soundManager = null;

    // Timer selector spinner.
    private Spinner timerChoice;
    
    // Timer configuration widgets.
    private EditText timerName;
    private Button butPreTime;
    private Spinner startBell;
    private Button butRunTime;
    private Spinner endBell;
    
    // Timeout picker dialogs.
    private TimeoutPickerDialog preTimeDialog;
    private TimeoutPickerDialog runTimeDialog;

    // Current timer configurations.
    private TimerConfig[] timerConfigs;

    // Basic timer names, and the current configured timer names.
    private String[] timerBaseNames;
    private String[] timerNames;

    // Array adapter which provides the choices of the timer names.
    private  ArrayAdapter<String> timerAdapter;

    // Bell names.
    private String[] bellNames;

    // Index of the timer we're editing.
    private int currentTimer = 0;
	
	// Current timer's configuration.
	private TimerConfig currentConfig = null;
	
	// Flag whether the UI is being updated programmatically.  It always
	// is during setup, so we start true.
	private boolean uiUpdating = true;
    
	// Buffer we create the time display in.
	private StringBuilder timeText;

}

