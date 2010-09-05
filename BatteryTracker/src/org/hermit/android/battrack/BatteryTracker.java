
package org.hermit.android.battrack;

import java.util.HashMap;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;


/**
 * This class is the main Activity for Battery Tracker.
 */
public class BatteryTracker
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
	 * @param	icicle			If the activity is being re-initialized
	 * 							after previously being shut down then this
	 * 							Bundle contains the data it most recently
	 * 							supplied in onSaveInstanceState(Bundle).
	 * 							Note: Otherwise it is null.
	 */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        // Get the UI fields.
    	display_tech = (TextView) findViewById(R.id.display_tech);
    	display_present = (TextView) findViewById(R.id.display_present);
    	display_status = (TextView) findViewById(R.id.display_status);
    	display_health = (TextView) findViewById(R.id.display_health);
    	display_level = (TextView) findViewById(R.id.display_level);
    	display_voltage = (TextView) findViewById(R.id.display_voltage);
    	display_temp = (TextView) findViewById(R.id.display_temp);
    	display_plugged = (TextView) findViewById(R.id.display_plugged);
    	display_connected = (TextView) findViewById(R.id.display_connected);
    	display_warning = (TextView) findViewById(R.id.display_warning);
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

        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_BATTERY_CHANGED);
        filter.addAction(Intent.ACTION_BATTERY_LOW);
        // filter.addAction(Intent.ACTION_BATTERY_CRITICAL);
        filter.addAction(Intent.ACTION_BATTERY_OKAY);
        filter.addAction(Intent.ACTION_POWER_CONNECTED);
        filter.addAction(Intent.ACTION_POWER_DISCONNECTED);
        registerReceiver(mIntentReceiver, filter);
    }

    
    /**
     * Called as part of the activity lifecycle when an activity is going
     * into the background, but has not (yet) been killed.  The counterpart
     * to onResume(). 
	 * 
	 * Derived classes must call through to the super class's implementation
	 * of this method.  If they do not, an exception will be thrown.
     */
    @Override
    protected void onPause() {
        Log.i(TAG, "onPause()");
        
        super.onPause();
        
        unregisterReceiver(mIntentReceiver);
    }

	
	// ******************************************************************** //
    // Update Handling.
    // ******************************************************************** //

    private BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(Intent.ACTION_BATTERY_CHANGED))
                updateStatus(intent);
            else if (action.equals(Intent.ACTION_BATTERY_LOW) ||
            		// action.equals(Intent.ACTION_BATTERY_CRITICAL) ||
            		action.equals(Intent.ACTION_BATTERY_OKAY))
                updateWarning(intent);
            else if (action.equals(Intent.ACTION_POWER_CONNECTED) ||
                    action.equals(Intent.ACTION_POWER_DISCONNECTED))
                updatePlugged(intent);
        }
    };
    
    
    /**
     * Update the battery status info.
     */
    private void updateStatus(Intent intent) {
        int stat = intent.getIntExtra(BatteryManager.EXTRA_STATUS, 0);
        int health = intent.getIntExtra(BatteryManager.EXTRA_HEALTH, 0);
        boolean present = intent.getBooleanExtra(BatteryManager.EXTRA_PRESENT, false);
        int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0);
        int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100);
        int plug = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0);
        int volts = intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0);
        int temp = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0);
        String tech = intent.getStringExtra(BatteryManager.EXTRA_TECHNOLOGY);

    	display_tech.setText(tech);
    	display_present.setText(present ? "yes" : "no");
    	display_status.setText(translateToken(stat, BATTERY_STATS));
    	display_health.setText(translateToken(health, BATTERY_HEALTH));
    	display_level.setText("" + level + "/" + scale);
    	display_voltage.setText(volts / 1000 + "." + volts % 1000);
    	display_temp.setText("" + temp);
    	display_plugged.setText(translateToken(plug, BATTERY_PLUG));
    }

    
    /**
     * Update the battery warning flag.
     */
    private void updateWarning(Intent intent) {
        String action = intent.getAction();
        int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0);
        warningFlag = translateToken(action, BATTERY_WARN_INTENT);
        display_warning.setText(warningFlag + " @ " + level);
    }

    
    /**
     * Update the battery plugged flag.
     */
    private void updatePlugged(Intent intent) {
        String action = intent.getAction();
        connectedFlag = translateToken(action, BATTERY_PLUG_INTENT);
    	display_connected.setText(connectedFlag);
    }


    // ******************************************************************** //
    // Utilities.
    // ******************************************************************** //

    private String translateToken(int val, HashMap<Integer, String> map) {
    	String name = map.get(val);
    	if (name != null)
    		return name;
    	return "?<" + val + ">?";
    }


    private String translateToken(String val, HashMap<String, String> map) {
    	String name = map.get(val);
    	if (name != null)
    		return name;
    	return "?<" + val + ">?";
    }


	// ******************************************************************** //
	// Class Data.
	// ******************************************************************** //

    // Debugging tag.
	private static final String TAG = "BatTrack";

	// Mapping from battery status codes to strings.
	private static final HashMap<Integer, String> BATTERY_STATS =
										new HashMap<Integer, String>();
	static {
		BATTERY_STATS.put(BatteryManager.BATTERY_STATUS_UNKNOWN, "unknown");
		BATTERY_STATS.put(BatteryManager.BATTERY_STATUS_CHARGING, "charging");
		BATTERY_STATS.put(BatteryManager.BATTERY_STATUS_DISCHARGING, "draining");
		BATTERY_STATS.put(BatteryManager.BATTERY_STATUS_NOT_CHARGING, "not charging");
		BATTERY_STATS.put(BatteryManager.BATTERY_STATUS_FULL, "full");
	}

	// Mapping from battery health codes to strings.
	private static final HashMap<Integer, String> BATTERY_HEALTH =
										new HashMap<Integer, String>();
	static {
		BATTERY_HEALTH.put(BatteryManager.BATTERY_HEALTH_UNKNOWN, "unknown");
		BATTERY_HEALTH.put(BatteryManager.BATTERY_HEALTH_GOOD, "good");
		BATTERY_HEALTH.put(BatteryManager.BATTERY_HEALTH_OVERHEAT, "overheat");
		BATTERY_HEALTH.put(BatteryManager.BATTERY_HEALTH_DEAD, "dead");
		BATTERY_HEALTH.put(BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE, "overvolt");
		BATTERY_HEALTH.put(BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE, "failed");
	}

	// Mapping from battery health codes to strings.
	private static final HashMap<Integer, String> BATTERY_PLUG =
										new HashMap<Integer, String>();
	static {
		BATTERY_PLUG.put(0, "none");
		BATTERY_PLUG.put(BatteryManager.BATTERY_PLUGGED_AC, "AC");
		BATTERY_PLUG.put(BatteryManager.BATTERY_PLUGGED_USB, "USB");
	}

	// Mapping from battery warning intent names to strings.
	private static final HashMap<String, String> BATTERY_WARN_INTENT =
										new HashMap<String, String>();
	static {
		BATTERY_WARN_INTENT.put(Intent.ACTION_BATTERY_LOW, "low");
//		BATTERY_WARN_INTENT.put(Intent.ACTION_BATTERY_CRITICAL, "critical");
		BATTERY_WARN_INTENT.put(Intent.ACTION_BATTERY_OKAY, "OK");
	}

	// Mapping from battery connected intent names to strings.
	private static final HashMap<String, String> BATTERY_PLUG_INTENT =
										new HashMap<String, String>();
	static {
		BATTERY_PLUG_INTENT.put(Intent.ACTION_POWER_CONNECTED, "plugged in");
		BATTERY_PLUG_INTENT.put(Intent.ACTION_POWER_DISCONNECTED, "unplugged");
	}


	// ******************************************************************** //
	// Private Data.
	// ******************************************************************** //

	// Display fields.
	private TextView display_tech = null;
	private TextView display_present = null;
	private TextView display_status = null;
	private TextView display_health = null;
	private TextView display_level = null;
	private TextView display_voltage = null;
	private TextView display_temp = null;
	private TextView display_plugged = null;
	private TextView display_connected = null;
	private TextView display_warning = null;
	
	// Latest warning status, from Intent.ACTION_BATTERY_LOW and
    // Intent.ACTION_BATTERY_OKAY broadcasts.
	private String warningFlag = "?";

	// Latest connected status, from Intent.ACTION_POWER_CONNECTED and
    // Intent.ACTION_POWER_DISCONNECTED broadcasts.
	private String connectedFlag = "?";
	
}

