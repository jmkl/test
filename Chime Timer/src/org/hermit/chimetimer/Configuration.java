

package org.hermit.chimetimer;


import org.hermit.android.widgets.TimeoutPicker;
import org.hermit.chimetimer.ChimerService.ChimerBinder;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.IBinder;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
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
        
        // Kick off our service, if it's not running.
        launchService();

        // Set up our data.
        timerConfigs = new TimerConfig[TimerConfig.NUM_TIMERS];
        for (int i = 0; i < TimerConfig.NUM_TIMERS; ++i)
        	timerConfigs[i] = new TimerConfig();

		// Get the relevant widgets.
        timerChoice = (Spinner) findViewById(R.id.timer_choice);
        timerName = (EditText) findViewById(R.id.name);
        preTime = (TimeoutPicker) findViewById(R.id.pre_time);
        startBell = (Spinner) findViewById(R.id.start_bell);
        runTime = (TimeoutPicker) findViewById(R.id.run_time);
        endBell = (Spinner) findViewById(R.id.finish_bell);
        
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
				currentConfig.name = timerName.getText().toString();
				saveConfig();
			}
			@Override
			public void beforeTextChanged(CharSequence s, int st, int c, int a) {
			}
			@Override
			public void afterTextChanged(Editable s) {
			}
		});
        
        preTime.setOnTimeChangedListener(new TimeoutPicker.OnTimeChangedListener() {
			@Override
			public void onTimeChanged(TimeoutPicker view, long millis) {
				currentConfig.preTime = millis;
				saveConfig();
			}
		});
        
        startBell.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> a, View v, int pos, long id) {
				if (pos != 0)
					playBell(pos - 1);
				currentConfig.startBell = pos;
				saveConfig();
			}

			@Override
			public void onNothingSelected(AdapterView<?> a) {
			}
		});
        
        runTime.setOnTimeChangedListener(new TimeoutPicker.OnTimeChangedListener() {
			@Override
			public void onTimeChanged(TimeoutPicker view, long millis) {
				currentConfig.runTime = millis;
				saveConfig();
			}
		});
        
        endBell.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> a, View v, int pos, long id) {
				if (pos != 0)
					playBell(pos - 1);
				currentConfig.endBell = pos;
				saveConfig();
			}

			@Override
			public void onNothingSelected(AdapterView<?> a) {
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
        Log.i(TAG, "Config onResume()");

        super.onResume();
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
     * Defines callbacks for service binding, passed to bindService().
     */
    private ServiceConnection serviceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            Log.i(TAG, "Config onServiceConnected()");
            
            // We've bound to ChimerService; cast the IBinder to
        	// the right type.
            ChimerBinder binder = (ChimerBinder) service;
            chimerService = binder.getService();
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            Log.i(TAG, "Config onServiceDisconnected()");
            
            chimerService = null;
        }
        
    };


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
		timerName.setText(currentConfig.name);
		preTime.setMillis(currentConfig.preTime);
		startBell.setSelection(currentConfig.startBell);
		runTime.setMillis(currentConfig.runTime);
		endBell.setSelection(currentConfig.endBell);
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
		}
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
	}


	// ******************************************************************** //
	// Bell Selection.
	// ******************************************************************** //

	private void playBell(int pos) {
		if (chimerService == null) {
            Log.i(TAG, "Config bell: no service");
			return;
		}
		
        Log.i(TAG, "Config bell " + pos);
		ChimerService.SoundEffect[] bells = ChimerService.SoundEffect.VALUES;
		if (pos < 0 || pos >= bells.length)
			return;
		chimerService.makeSound(bells[pos]);
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

    // Timer selector spinner.
    private Spinner timerChoice;
    
    // Timer configuration widgets.
    private EditText timerName;
    private TimeoutPicker preTime;
    private Spinner startBell;
    private TimeoutPicker runTime;
    private Spinner endBell;
    
    // Current timer configurations.
    private TimerConfig[] timerConfigs;
    
    // Index of the timer we're editing.
    private int currentTimer = 0;
	
	// Current timer's configuration.
	private TimerConfig currentConfig = null;

}

