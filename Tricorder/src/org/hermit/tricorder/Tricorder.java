
/**
 * Tricorder: turn your phone into a tricorder.
 * 
 * This is an Android implementation of a Star Trek tricorder, based on
 * the phone's own sensors.  It's also a demo project for sensor access.
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


package org.hermit.tricorder;


import org.hermit.android.core.AppUtils;
import org.hermit.android.notice.InfoBox;
import org.hermit.tricorder.TricorderView.ViewDefinition;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Display;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.RelativeLayout;


/**
 * A tricorder application for your phone.
 * 
 * This class is the main Activity for Tricorder.
 */
public class Tricorder
	extends Activity
{

    // ******************************************************************** //
    // Public Types and Constants.
    // ******************************************************************** //
	
	// Main window background color.
	static final int COL_BG = 0xff000000;
	
	// Main window text color.
	static final int COL_TEXT = 0xff000000;
	
	/**
	 * The colour for graph and guage pointers.
	 */
	static final int COL_POINTER = 0xffff0000;

    /**
     * The sounds that we make.
     */
    static enum Sound {
    	PING(R.raw.ping),
    	BEEP_BOOP(R.raw.beep_boop),
    	BOOP_BEEP(R.raw.boop_beep),
    	CHIRP_LOW(R.raw.chirp_low),
    	HU(R.raw.hu),
    	HMU(R.raw.hmu),
    	HMLU(R.raw.hmlu);
    	
    	private Sound(int res) {
    		soundRes = res;
    	}
    	
        private final int soundRes;     // Resource ID for the sound file.
        private int soundId = 0;        // Sound ID for playing.
    }

    /**
     * Sound play mode.
     */
    static enum SoundMode {
    	NONE,
    	QUIET,
    	FULL;
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

        // We don't want a title bar or status bar.
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                		     WindowManager.LayoutParams.FLAG_FULLSCREEN);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        
        // We want the audio controls to control our sound volume.
        this.setVolumeControlStream(AudioManager.STREAM_MUSIC);
        
        // Load the sounds.
        soundPool = createSoundPool();

        // Create the application GUI.
        setContentView(createGui());

        // Restore our preferences.
        updatePreferences();

        // Create the dialog we use for help and about.
        AppUtils autils = AppUtils.getInstance(this);
        messageDialog = new InfoBox(this, R.string.button_close);
        String version = autils.getVersionString();
		messageDialog.setTitle(version);
         
        // Set the initial view.  This also starts it so it gets updates.
		selectDataView(ViewDefinition.GRA);
    }

 
    /**
     * This method is called after onStart() when the activity is being
     * re-initialized from a previously saved state, given here in state.
     * Most implementations will simply use onCreate(Bundle) to restore
     * their state, but it is sometimes convenient to do it here after
     * all of the initialization has been done or to allow subclasses
     * to decide whether to use your default implementation.  The default
     * implementation of this method performs a restore of any view
     * state that had previously been frozen by onSaveInstanceState(Bundle).
     * 
     * This method is called between onStart() and onPostCreate(Bundle).
     * 
	 * @param	inState			The data most recently supplied in
	 * 							onSaveInstanceState(Bundle).
     */
    @Override
    protected void onRestoreInstanceState(Bundle inState) {
        Log.i(TAG, "onRestoreInstanceState()");
        
        super.onRestoreInstanceState(inState);
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
        
        mainView.doOnResume();
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
        
        mainView.doOnPause();
    }

     
    /**
     * Perform any final cleanup before an activity is destroyed.  This can
     * happen either because the activity is finishing (someone called
     * finish() on it, or because the system is temporarily destroying this
     * instance of the activity to save space.  You can distinguish between
     * these two scenarios with the isFinishing() method.
	 * 
	 * Derived classes must call through to the super class's implementation
	 * of this method.  If they do not, an exception will be thrown.
     */
    @Override
	protected void onDestroy() {
        Log.i(TAG, "onDestroy()");
        
    	super.onDestroy();
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
    	final WindowManager wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
    	final Display disp = wm.getDefaultDisplay();
    	final int width = disp.getWidth();
    	final int height = disp.getHeight();
    	final int minDim = width < height ? width : height;

    	// Much of the layout size is constrained by the text size.
    	// Calculate the base text size from the screen size.
    	baseTextSize = (float) minDim * 0.075f;
		if (baseTextSize < 10)
			baseTextSize = 10;
		else if (baseTextSize > 32)
			baseTextSize = 32;
		
		navBarWidth = (int) ((float) minDim * 0.22f);
		topBarHeight = (int) ((float) minDim * 0.15f);
		topTitleHeight = (int) baseTextSize + 4;
		viewSidebar =  minDim / 64;
		interPadding = minDim / 40;
		innerGap = minDim / 100;
		if (innerGap < 1)
			innerGap = 1;

    	final int FPAR = RelativeLayout.LayoutParams.FILL_PARENT;

        // Create a layout to hold the board and status bar.
    	RelativeLayout mainLayout = new RelativeLayout(this);
    	mainLayout.setBackgroundColor(COL_BG);

		RelativeLayout.LayoutParams layout;

    	swoopCorner = new HeaderBar(this, navBarWidth, topTitleHeight);
    	swoopCorner.setId(2);
        layout = new RelativeLayout.LayoutParams((int) (navBarWidth * 1.5f),
        										 topBarHeight);
		layout.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
		layout.addRule(RelativeLayout.ALIGN_PARENT_TOP);
		mainLayout.addView(swoopCorner, layout);
		
		int bwidth = (int) ((width - navBarWidth * 1.5) / 1.75);

    	topLabel = new NavButton(this, null);
    	topLabel.setId(4);
        layout = new RelativeLayout.LayoutParams(bwidth, topTitleHeight);
		layout.addRule(RelativeLayout.RIGHT_OF, 2);
		layout.addRule(RelativeLayout.ALIGN_PARENT_TOP);
        layout.setMargins(innerGap, 0, 0, 0);
		mainLayout.addView(topLabel, layout);
		
		topButton = new NavButton(this, null);
		topButton.setId(5);
        layout = new RelativeLayout.LayoutParams(FPAR, topTitleHeight);
		layout.addRule(RelativeLayout.RIGHT_OF, 4);
		layout.addRule(RelativeLayout.ALIGN_PARENT_TOP);
        layout.setMargins(innerGap, 0, 0, 0);
		mainLayout.addView(topButton, layout);
		topButton.setOnClickListener(new View.OnClickListener() {
        	@Override
        	public void onClick(View arg0) { viewButtonClicked(); }
        });

		navBar = new NavigationBar(this, navBarWidth, height - topBarHeight);
    	navBar.setId(3);
		layout = new RelativeLayout.LayoutParams(navBarWidth, FPAR);
		layout.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
		layout.addRule(RelativeLayout.BELOW, 2);
		mainLayout.addView(navBar, layout);
		
        // Add the main Tricorder data view.
        mainView = new TricorderView(this);
		layout = new RelativeLayout.LayoutParams(FPAR, FPAR);
		layout.addRule(RelativeLayout.RIGHT_OF, 3);
		layout.addRule(RelativeLayout.BELOW, 2);
        mainLayout.addView(mainView, layout);
  
        return mainLayout;
    }

    
    /**
     * Get the base size for text based on this screen's size.
     * 
     * @return				Base text size for the app.
     */
    float getBaseTextSize() {
    	return baseTextSize;
    }

    
    /**
     * Get the calculated sidebar width.
     * 
	 * @return				The calculated sidebar width.
	 */
	int getSidebarWidth() {
		return viewSidebar;
	}


	// ******************************************************************** //
    // Menu and Preferences Handling.
    // ******************************************************************** //

	/**
	 * Get the amount of padding between major elements in a view.
	 * 
	 * @return			The amount of padding between major elements in a view.
	 */
	int getInterPadding() {
		return interPadding;
	}


	/**
	 * Get the amount of padding within atoms within an element.  Specifically
	 * the small gaps in side bars.
	 * 
	 * @return			The amount of padding within atoms within an element
	 */
	int getInnerGap() {
		return innerGap;
	}


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
        	Intent pIntent = new Intent();
        	pIntent.setClass(this, Preferences.class);
        	startActivityForResult(pIntent, SUBACTIVITY_PREFERENCES);
        	break;
    	case R.id.menu_help:
            messageDialog.setLinkButton(1, R.string.button_homepage, R.string.url_homepage);
            messageDialog.setLinkButton(2, R.string.button_manual, R.string.url_manual);
            messageDialog.show(R.string.help_text);
    		break;
    	case R.id.menu_about:
            messageDialog.setLinkButton(1, R.string.button_homepage, R.string.url_homepage);
            messageDialog.setLinkButton(2, R.string.button_license, R.string.url_license);
            messageDialog.show(R.string.about_text);
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
    	soundMode = SoundMode.FULL;
    	try {
    		String smode = prefs.getString("soundMode", null);
    		soundMode = SoundMode.valueOf(smode);
    	} catch (Exception e) {
    		Log.e(TAG, "Pref: bad soundMode");
    	}
    	Log.i(TAG, "Prefs: soundMode " + soundMode);

    	wifiPing = false;
    	try {
    		wifiPing = prefs.getBoolean("wifiPing", false);
    	} catch (Exception e) {
    		Log.e(TAG, "Pref: bad wifiPing");
    	}
    	Log.i(TAG, "Prefs: wifiPing " + wifiPing);

        // Get the desired orientation.
        int orientMode = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED;
        try {
            String omode = prefs.getString("orientMode", null);
            orientMode = Integer.valueOf(omode);
        } catch (Exception e) {
            Log.e(TAG, "Pref: bad orientMode");
        }
        Log.i(TAG, "Prefs: orientMode " + orientMode);
        setRequestedOrientation(orientMode);
        
    	boolean fakeMissingData = false;
    	try {
    		fakeMissingData = prefs.getBoolean("fakeMissingData", false);
    	} catch (Exception e) {
    		Log.e(TAG, "Pref: bad fakeMissingData");
    	}
    	Log.i(TAG, "Prefs: fakeMissingData " + fakeMissingData);
    	mainView.setSimulateMode(fakeMissingData);
    }


    // ******************************************************************** //
    // Display Management.
    // ******************************************************************** //

    /**
     * Select and display the given view.
     * 
     * @param	viewDef			View definition of the view to show.
     */
    void selectDataView(ViewDefinition viewDef) {
    	currentView = viewDef;
    	mainView.selectView(currentView);
   	
    	// Set the bar colours to match.
    	swoopCorner.selectDataView(currentView);
    	topLabel.setViewDef(currentView, currentView.titleId);
    	int aux = currentView.auxId;
    	if (aux == 0)
    		aux = R.string.lab_blank;
    	topButton.setViewDef(currentView, aux);
    	navBar.selectDataView(currentView);
    	
    	postSound(Sound.HU);
    }
    
 
    /**
     * The aux button has been clicked; pass it to the view.
     */
    void viewButtonClicked() {
    	if (currentView != null)
    		currentView.view.auxButtonClick();
    }
    
    
	// ******************************************************************** //
	// Sound.
	// ******************************************************************** //
    
    /**
     * Create a SoundPool containing the app's sound effects.
     */
    private SoundPool createSoundPool() {
        SoundPool pool = new SoundPool(3, AudioManager.STREAM_MUSIC, 0);
        for (Sound sound : Sound.values())
            sound.soundId = pool.load(this, sound.soundRes, 1);
        
        return pool;
    }
    

    /**
     * Post a sound to be played on the main app thread.
     * 
     * @param   which           ID of the sound to play.
     */
    void postSound(final Sound which) {
        postSound(which, 1f);
    }


    /**
     * Post a sound to be played on the main app thread.
     * 
     * @param   which           ID of the sound to play.
     * @param   rvol            Relative volume for this sound, 0 - 1.
     */
    void postSound(final Sound which, float rvol) {
//        Message msg = soundHandler.obtainMessage();
//        Bundle b = new Bundle();
//        b.putInt("which", which.soundId);
//        b.putFloat("rvol", rvol);
//        msg.setData(b);
//        soundHandler.sendMessage(msg);
        
        makeSound(which.soundId, rvol);
    }


    /**
     * Post a sound to be played on the main app thread.
     * 
     * @param   strength        Signal strength as a percentage.
     */
    void postPing(final int strength) {
        if (wifiPing) {
            if (pinger != null && pinger.isAlive())
                pinger.kill();
            pinger = new Pinger();
            pinger.start(strength);
        }
    }


    /**
     * Make a sound.
     * 
     * @param   soundId         ID of the sound to play.
     */
    void makeSound(int soundId) {
        makeSound(soundId, 1);
    }


    /**
     * Make a sound.
     * 
     * @param   soundId         ID of the sound to play.
     * @param   rvol            Relative volume for this sound, 0 - 1.
     */
    void makeSound(int soundId, float rvol) {
        if (soundMode == SoundMode.NONE)
            return;
        
        float vol = 1.0f;
        if (soundMode == SoundMode.QUIET)
            vol = 0.3f;
        if (rvol < 1f)
            vol *= rvol;
        soundPool.play(soundId, vol, vol, 1, 0, 1f);
    }
	

	private final class Pinger extends Thread {
		public void start(int str) {
			this.str = str;
			running = true;
			start();
		}
		public void kill() {
			running = false;
		}
		@Override
		public void run() {
			if (!running) return;
			postSound(Sound.PING);
			if (str != 0) try {
			    int del = 2000 - (str * 20) + 50;
			    if (del > 10)
			        sleep(del);
				if (!running) return;
				postSound(Sound.PING, (float) str / 100f);
			} catch (InterruptedException e) { }
		}
		public int str;
		public boolean running;
	}
	

	// ******************************************************************** //
	// Class Data.
	// ******************************************************************** //

    // Debugging tag.
	private static final String TAG = "tricorder";

	// Request codes used to identify requests to sub-activities.
	// Display and edit preferences.
    private static final int SUBACTIVITY_PREFERENCES = 1;


	// ******************************************************************** //
	// Private Data.
	// ******************************************************************** //

    // The main data display window.
    private TricorderView mainView;
    
    // The currently selected view.
    private ViewDefinition currentView = null;

    // The base size for all text, based on screen size.
    private float baseTextSize = 0f;

	// Vertical navigation bar width, top bar height.
	private int navBarWidth;
	private int topBarHeight;
	private int topTitleHeight;
	
	// The thickness of a side bar in a view element.
	private int viewSidebar;
	
	// The amount of padding between major elements in a view.
	private int interPadding;
	
	// The amount of padding within atoms within an element.  Specifically
	// the small gaps in side bars.
	private int innerGap;

    // The top header bar.
    private HeaderBar swoopCorner;

    // The side navigation bar.
    private NavigationBar navBar;
    
    // Top button, used by the current data view.
	private NavButton topButton;
    
    // Top label which identifies the current view.
	private NavButton topLabel;

	// Dialog used to display about etc.
	private InfoBox messageDialog;

	// Current sound mode.
	private SoundMode soundMode;
    
    // Sound pool used for sound effects.
    private SoundPool soundPool;

	// Whether to ping for WiFi scans.
	private boolean wifiPing = false;
	
	// Current ping effect; null if none.
	private Pinger pinger = null;

}

