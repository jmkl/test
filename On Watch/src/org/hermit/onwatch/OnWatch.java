
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

import java.util.ArrayList;

import org.hermit.android.core.MainActivity;
import org.hermit.android.core.SplashActivity;
import org.hermit.android.widgets.TimeZoneActivity;
import org.hermit.onwatch.service.Chimer;
import org.hermit.onwatch.service.OnWatchService;
import org.hermit.onwatch.service.OnWatchService.OnWatchBinder;

import android.app.ActionBar;
import android.app.AlarmManager;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.app.ActionBar.Tab;
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

        // Kick off our service, if it's not running.
        Intent intent = new Intent(this, OnWatchService.class);
        startService(intent);

        // Create our EULA box.
        createEulaBox(R.string.eula_title, R.string.eula_text, R.string.button_close);       

        // Set up the standard dialogs.
        setAboutInfo(R.string.about_text);
        setHomeInfo(R.string.url_homepage);
        setLicenseInfo(R.string.url_license);

        // Create the time and location models.
        locationModel = LocationModel.getInstance(this);
        timeModel = TimeModel.getInstance(this);
 
        // Create the application GUI.
        setContentView(R.layout.on_watch);

        // Set up our Action Bar for tabs.
        final ActionBar actionBar = getActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
        
        // Remove the activity title to make space for tabs.
        actionBar.setDisplayShowTitleEnabled(false);

        // Add the view fragments to the tab bar.
        childViews = new ArrayList<ViewFragment>();
        addChild(actionBar, new HomeFragment(), R.string.tab_location);
        addChild(actionBar, new PassageFragment(), R.string.tab_passage);
        addChild(actionBar, new ScheduleFragment(), R.string.tab_watch);
        addChild(actionBar, new AstroFragment(), R.string.tab_astro);

		// Create a handler for tick events.
		tickHandler = new Handler() {
			@Override
			public void handleMessage(Message m) {
				long time = System.currentTimeMillis();
				timeModel.tick(time);
				locationModel.tick(time);
				for (ViewFragment v : childViews)
					v.tick(time);
			}
		};
        
        // We want the audio controls to control our sound volume.
        this.setVolumeControlStream(AudioManager.STREAM_MUSIC);

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


    private void addChild(ActionBar bar, ViewFragment frag, int label) {
    	ActionBar.Tab tab = bar.newTab();
    	tab.setText(label);
    	tab.setTabListener(new WatchTabListener(frag, label));
        bar.addTab(tab);
        childViews.add(frag);
    }


    private class WatchTabListener implements ActionBar.TabListener {
        // Called to create an instance of the listener when adding a new tab
        public WatchTabListener(Fragment fragment, int label) {
            theFragment = fragment;
            tabName = getString(label);
        }

        public void onTabSelected(Tab tab, FragmentTransaction ft) {
            Log.i(TAG, "TabOpened(" + tabName + ")");
            ft.add(R.id.main_view, theFragment, null);
        }

        public void onTabUnselected(Tab tab, FragmentTransaction ft) {
            Log.i(TAG, "TabClosed(" + tabName + ")");
            ft.remove(theFragment);
        }

        public void onTabReselected(Tab tab, FragmentTransaction ft) {
            // do nothing
        }

        private Fragment theFragment;
        private String tabName;
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

        // Bind to the OnWatch service.
        Intent intent = new Intent(this, OnWatchService.class);
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);

        // FIXME: do we need this?
//		for (ViewFragment v : childViews)
//			v.start();
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
        
        // FIXME: do we need this?
//		for (ViewFragment v : childViews)
//			v.resume();
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
        
        // FIXME: do we need this?
//		for (ViewFragment v : childViews)
//			v.pause();
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
        
        // Unbind from the OnWatch service.
        if (mService != null) {
            unbindService(mConnection);
            mService = null;
        }

        // FIXME: do we need this?
//		for (ViewFragment v : childViews)
//			stop();
    }


    // ******************************************************************** //
    // Service Communications.
    // ******************************************************************** //

    /**
     * Defines callbacks for service binding, passed to bindService().
     */
    private ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            // We've bound to OnWatchService; cast the IBinder to
        	// the right type.
            OnWatchBinder binder = (OnWatchBinder) service;
            mService = binder.getService();
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mService = null;
        }
        
    };


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
        
//      icicle.putInt("currentView", viewFlipper.getDisplayedChild());

        // Now save our sub-components.
        // FIXME: do so.
    }


    /**
     * Restore the application's state from the given Bundle.
     * 
     * @param   icicle          The app's saved state.
     */
    public void restoreState(Bundle icicle) {
        shownSplash = icicle.getBoolean("shownSplash");
        
//      viewFlipper.setDisplayedChild(icicle.getInt("currentView"));

        // Now restore our sub-components.
        // FIXME: do so.
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
        
        // Get the menu items we need to control.
        chimeMenuItem = menu.findItem(R.id.menu_chimes);
        boolean chimeWatch = mService.getChimeEnable();
		chimeMenuItem.setIcon(chimeWatch ? R.drawable.ic_menu_chimes_on :
	               						   R.drawable.ic_menu_chimes_off);
		
        alertsMenuItem = menu.findItem(R.id.menu_alerts);
        Chimer.AlertMode alertMode = mService.getRepeatAlert();
    	alertsMenuItem.setIcon(alertMode.icon);
        
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
    	case android.R.id.home:
    		// App icon has been pressed.
    		// FIXME: do something.
    		break;
    	case R.id.menu_chimes:
    		setChimes(!mService.getChimeEnable());
    		break;
    	case R.id.menu_alerts:
    		setAlarms(mService.getRepeatAlert().next());
    		break;
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
            // Launch the help activity as a subactivity.
            Intent hIntent = new Intent();
            hIntent.setClass(this, Help.class);
            startActivity(hIntent);
    		break;
    	case R.id.menu_about:
            showAbout();
     		break;
    	case R.id.menu_eula:
    	    showEula();
     		break;
        case R.id.menu_exit:
        	shutdown();
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

    	// FIXME:
//    	viewController.setDebug(debugSpace, debugTime);
   }


	// ******************************************************************** //
	// Shutdown.
	// ******************************************************************** //

    /**
     * Shut down the app, including the background service.
     */
    private void shutdown() {
        mService.shutdown();
    	finish();
    }
    
    
	// ******************************************************************** //
	// Alert Controls Handling.
	// ******************************************************************** //

    /**
     * Set the half-hourly watch chimes on or off.
     * 
     * @param	enable				Requested state.
     */
    private void setChimes(boolean enable) {
        mService.setChimeEnable(enable);
        chimeMenuItem.setIcon(enable ? R.drawable.ic_menu_chimes_on :
        					  		   R.drawable.ic_menu_chimes_off);
    }
    

    /**
     * Set the repeating alarm on or off.
     * 
     * @param	mode				Requested alert mode.
     */
    private void setAlarms(Chimer.AlertMode mode) {
        mService.setRepeatAlert(mode);
        alertsMenuItem.setIcon(mode.icon);
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
	private static final String TAG = "onwatch";
	
	// Time in ms for which the splash screen is displayed.
	private static final long SPLASH_TIME = 5000;


	// ******************************************************************** //
	// Private Data.
	// ******************************************************************** //
    
    // Menu items for the chimes and alerts controls.
    private MenuItem chimeMenuItem = null;
    private MenuItem alertsMenuItem = null;
  
	// The views we display in our tabs.
	private ArrayList<ViewFragment> childViews;
    
    // Our OnWatch service.  null if we haven't bound to it yet.
    private OnWatchService mService = null;

	// The time model we use for all our timekeeping.
	private TimeModel timeModel;

	// The location model we use for all our positioning.
	private LocationModel locationModel;

    // Timer we use to generate tick events.
    private Ticker ticker = null;
	
	// Handler for updates.  We need this to get back onto
	// our thread so we can update the GUI.
	private Handler tickHandler;

    // Log whether we showed the splash screen yet this run.
    private boolean shownSplash = false;

}

