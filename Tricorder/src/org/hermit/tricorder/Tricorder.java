
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


import org.hermit.android.core.MainActivity;
import org.hermit.android.core.OneTimeDialog;
import org.hermit.android.instruments.AudioAnalyser;
import org.hermit.android.instruments.Gauge;
import org.hermit.android.sound.Effect;
import org.hermit.android.sound.Player;
import org.hermit.tricorder.TricorderView.ViewDefinition;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.graphics.Typeface;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.PowerManager;
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
	extends MainActivity
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
     * Sound play mode.
     */
    static enum SoundMode {
    	NONE(0f),
    	QUIET(0.3f),
    	FULL(1f);
    	SoundMode(float g) {
    	    this.gain = g;
    	}
    	final float gain;
    }

    /**
     * Data units.
     */
    static enum Unit {
        SI, IMPERIAL;
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

        // Get our power manager for wake locks.
        powerManager = (PowerManager) getSystemService(POWER_SERVICE);

        // Create the message and info boxes.
        setAboutInfo(R.string.about_text);
        setHomeInfo(R.string.url_homepage);
        setLicenseInfo(R.string.url_license);
        
        // Create our "new in this version" dialog.
        versionDialog = new OneTimeDialog(this, "new",
                                          R.string.newf_title,
                                          R.string.newf_text,
                                          R.string.button_close);

        // We don't want a title bar or status bar.
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                		     WindowManager.LayoutParams.FLAG_FULLSCREEN);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        
        // We want the audio controls to control our sound volume.
        this.setVolumeControlStream(AudioManager.STREAM_MUSIC);
        
        // Create our sound player.
        effectsPlayer = new Player(this);
        switchSound = effectsPlayer.addEffect(R.raw.hu);
        pingSound = effectsPlayer.addEffect(R.raw.ping);
        activateSound = effectsPlayer.addEffect(R.raw.boop_beep);
        deactivateSound = effectsPlayer.addEffect(R.raw.beep_boop);
        secondarySound = effectsPlayer.addEffect(R.raw.chirp_low);

        // Create the application GUI.
        setContentView(createGui());

        // Restore our preferences.
        updatePreferences();
        
        // Get ready to go to the first screen.
        pendingView = ViewDefinition.GRA;
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
        
        mainView.onStart();
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
        restoreState(inState);
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
        
        effectsPlayer.resume();
        
        // Show the "new features" dialog.
        versionDialog.showFirst();
        
        // Take the wake lock if we want it.
        if (wakeLock != null && !wakeLock.isHeld())
            wakeLock.acquire();
 
        mainView.onResume();

        // Just start straight away.
        mainView.surfaceStart();
        
        // If this is the first time through, set the initial view.
        // This also starts it so it gets updates.
        if (pendingView != null) {
            selectDataView(pendingView);
            pendingView = null;
        }
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
        
        mainView.onPause();

        // Let go the wake lock if we have it.
        if (wakeLock != null && wakeLock.isHeld())
            wakeLock.release();
        
        effectsPlayer.suspend();
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
        
        mainView.onStop();
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
        
        mainView.unbindResources();
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
    	float baseTextSize = (float) minDim * 0.0635f;
		Gauge.setTextTypeface(FONT_FACE);
		Gauge.setBaseTextSize(baseTextSize);
        Gauge.setMiniTextSize(baseTextSize * 0.85f);
        Gauge.setTinyTextSize(baseTextSize * 0.7f);
		Gauge.setTextScaleX(FONT_SCALEX);
		
		navBarWidth = (int) ((float) minDim * 0.22f);
		topBarHeight = (int) ((float) minDim * 0.15f);
		topTitleHeight = (int) (Gauge.getHeadTextSize() * 1.2f);
		Gauge.setSidebarWidth(minDim / 64);
		Gauge.setInterPadding(minDim / 40);
		float innerGap = minDim / 100;
		if (innerGap < 1)
			innerGap = 1;
		Gauge.setInnerGap((int) innerGap);
		
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
        layout.setMargins((int) innerGap, 0, 0, 0);
		mainLayout.addView(topLabel, layout);
		
		topButton = new NavButton(this, null);
		topButton.setId(5);
        layout = new RelativeLayout.LayoutParams(FPAR, topTitleHeight);
		layout.addRule(RelativeLayout.RIGHT_OF, 4);
		layout.addRule(RelativeLayout.ALIGN_PARENT_TOP);
        layout.setMargins((int) innerGap, 0, 0, 0);
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
            Intent hIntent = new Intent();
            hIntent.setClass(this, Help.class);
            startActivity(hIntent);
    	    break;
    	case R.id.menu_about:
    	    showAbout();
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

        boolean scanContinuous = false;
        try {
            scanContinuous = prefs.getBoolean("scanContinuous", false);
        } catch (Exception e) {
            Log.e(TAG, "Pref: bad scanContinuous");
        }
        Log.i(TAG, "Prefs: scanContinuous " + scanContinuous);
        mainView.setScanMode(scanContinuous);

        // See if sounds are enabled and how.
    	SoundMode soundMode = SoundMode.FULL;
    	try {
    		String smode = prefs.getString("soundMode", null);
    		soundMode = SoundMode.valueOf(smode);
    	} catch (Exception e) {
    		Log.e(TAG, "Pref: bad soundMode");
    	}
    	Log.i(TAG, "Prefs: soundMode " + soundMode);
    	effectsPlayer.setGain(soundMode.gain);

    	boolean scanSound = true;
    	try {
    	    scanSound = prefs.getBoolean("scanSound", true);
    	} catch (Exception e) {
    		Log.e(TAG, "Pref: bad scanSound");
    	}
    	Log.i(TAG, "Prefs: scanSound " + scanSound);
        mainView.setScanSound(scanSound);

        wifiPing = false;
        try {
            wifiPing = prefs.getBoolean("wifiPing", false);
        } catch (Exception e) {
            Log.e(TAG, "Pref: bad wifiPing");
        }
        Log.i(TAG, "Prefs: wifiPing " + wifiPing);

        // Get the desired units.
        Unit dataUnits = Unit.SI;
        try {
            String sval = prefs.getString("unitsMode", String.valueOf(dataUnits));
            dataUnits = Unit.valueOf(sval);
        } catch (Exception e) {
            Log.e(TAG, "Pref: bad unitsMode");
        }
        Log.i(TAG, "Prefs: unitsMode " + dataUnits);
        mainView.setDataUnits(dataUnits);

        // Get the desired orientation.
        int orientMode = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
        try {
            String omode = prefs.getString("orientationMode", null);
            orientMode = Integer.valueOf(omode);
        } catch (Exception e) {
            Log.e(TAG, "Pref: bad orientationMode");
        }
        Log.i(TAG, "Prefs: orientationMode " + orientMode);
        setRequestedOrientation(orientMode);

        boolean keepAwake = false;
        try {
            keepAwake = prefs.getBoolean("keepAwake", false);
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

    	// Options for the audio analyser.
    	AudioAnalyser analyser = mainView.getAudioView().getAudioAnalyser();
    	
        // Get the desired sample rate.
        int sampleRate = 8000;
        try {
            String srate = prefs.getString("sampleRate", null);
            sampleRate = Integer.valueOf(srate);
        } catch (Exception e) {
            Log.e(TAG, "Pref: bad sampleRate");
        }
        if (sampleRate < 8000)
            sampleRate = 8000;
        Log.i(TAG, "Prefs: sampleRate " + sampleRate);
        analyser.setSampleRate(sampleRate);
        
        // Get the desired block size.
        int blockSize = 256;
        try {
            String bsize = prefs.getString("blockSize", null);
            blockSize = Integer.valueOf(bsize);
        } catch (Exception e) {
            Log.e(TAG, "Pref: bad blockSize");
        }
        Log.i(TAG, "Prefs: blockSize " + blockSize);
        analyser.setBlockSize(blockSize);
        
        // Get the desired window function.
        org.hermit.dsp.Window.Function windowFunc =
                            org.hermit.dsp.Window.Function.BLACKMAN_HARRIS;
        try {
            String func = prefs.getString("windowFunc", null);
            windowFunc = org.hermit.dsp.Window.Function.valueOf(func);
        } catch (Exception e) {
            Log.e(TAG, "Pref: bad windowFunc");
        }
        Log.i(TAG, "Prefs: windowFunc " + windowFunc);
        analyser.setWindowFunc(windowFunc);

        // Get the desired decimation.
        int decimateRate = 2;
        try {
            String drate = prefs.getString("decimateRate", null);
            decimateRate = Integer.valueOf(drate);
        } catch (Exception e) {
            Log.e(TAG, "Pref: bad decimateRate");
        }
        Log.i(TAG, "Prefs: decimateRate " + decimateRate);
        analyser.setDecimation(decimateRate);
        
        // Get the desired histogram smoothing window.
        int averageLen = 4;
        try {
            String alen = prefs.getString("averageLen", null);
            averageLen = Integer.valueOf(alen);
        } catch (Exception e) {
            Log.e(TAG, "Pref: bad averageLen");
        }
        Log.i(TAG, "Prefs: averageLen " + averageLen);
        analyser.setAverageLen(averageLen);
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
        // Stop the old view.
    	mainView.deselectView();
    	
        currentView = viewDef;
        
    	// Set the bar colours to match.
    	swoopCorner.selectDataView(currentView);
    	topLabel.setViewDef(currentView, currentView.titleId);
    	int aux = currentView.auxId;
    	if (aux == 0)
    		aux = R.string.lab_blank;
    	topButton.setViewDef(currentView, aux);
    	navBar.selectDataView(currentView);
    	
        mainView.selectView(currentView);

    	switchSound.play();
    }
    
 
    /**
     * The aux button has been clicked; pass it to the view.
     */
    void viewButtonClicked() {
        try {
            if (currentView != null)
                currentView.view.auxButtonClick();
        } catch (Exception e) {
            reportException(e);
        }
    }
    

    /**
     * Set the text displayed in the aux button.
     * 
     * @param   textId          Resource ID of the text to show.
     */
    void setAuxButton(int textId) {
        topButton.setText(textId);
    }
    

	// ******************************************************************** //
	// Sound.
	// ******************************************************************** //
    
    /**
     * Get the app's sound effects player.
     * 
     * @return              This app's sound player.
     */
    Player getSoundPlayer() {
        return effectsPlayer;
    }
    

    /**
     * Make an "activate" sound.
     */
    void soundActivate() {
        activateSound.play();
    }


    /**
     * Make a "deactivate" sound.
     */
    void soundDeactivate() {
        deactivateSound.play();
    }


    /**
     * Make a "secondary activation" sound.
     */
    void soundSecondary() {
        secondarySound.play();
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
			pingSound.play();
			if (str != 0) try {
			    int del = 2000 - (str * 20) + 50;
			    if (del > 10)
			        sleep(del);
				if (!running) return;
	            pingSound.play((float) str / 100f);
			} catch (InterruptedException e) { }
		}
		public int str;
		public boolean running;
	}
	

    // ******************************************************************** //
    // Save and Restore.
    // ******************************************************************** //

    /**
     * Save the state of the application in the provided Bundle.
     * 
     * @param   icicle      The Bundle in which we should save our state.
     */
    protected void saveState(Bundle icicle) {
        icicle.putString("currentView", currentView.toString());
        
        mainView.saveState(icicle);
    }


    /**
     * Restore the application state from the given Bundle.
     * 
     * @param   icicle      The Bundle containing the saved state.
     */
    protected void restoreState(Bundle icicle) {
        String v = icicle.getString("currentView");
        try {
            ViewDefinition vdef = ViewDefinition.valueOf(v);
            pendingView = vdef;
        } catch (IllegalArgumentException e) { }
        
        mainView.restoreState(icicle);
    }
    

	// ******************************************************************** //
	// Class Data.
	// ******************************************************************** //

    // Debugging tag.
	private static final String TAG = "tricorder";

    // Typeface for text.
    private static final Typeface FONT_FACE = Typeface.MONOSPACE;

    // Horizontal scaling of the font; used to produce a tall, thin font.
    private static final float FONT_SCALEX = 0.6f;


	// ******************************************************************** //
	// Private Data.
	// ******************************************************************** //
    
    // Our power manager.
    private PowerManager powerManager = null;
    
    // Dialog used for "new in this version" messages.
    private OneTimeDialog versionDialog = null;

    // The main data display window.
    private TricorderView mainView;
    
    // The currently selected view.
    private ViewDefinition currentView = null;

    // If not null, the view which needs to be selected when we get resumed.
    private ViewDefinition pendingView = null;

	// Vertical navigation bar width, top bar height.
	private int navBarWidth;
	private int topBarHeight;
	private int topTitleHeight;

    // The top header bar.
    private HeaderBar swoopCorner;

    // The side navigation bar.
    private NavigationBar navBar;
    
    // Top button, used by the current data view.
	private NavButton topButton;
    
    // Top label which identifies the current view.
	private NavButton topLabel;
    
    // Wake lock used to keep the screen alive.  Null if we aren't going
    // to take a lock; non-null indicates that the lock should be taken
    // while we're actually running.
    private PowerManager.WakeLock wakeLock = null;
    
    // Sound player; sound played when we change views; ping sound.
    private Player effectsPlayer = null;
    private Effect switchSound = null;
    private Effect pingSound = null;
    private Effect activateSound = null;
    private Effect deactivateSound = null;
    private Effect secondarySound = null;

    // Whether to ping for WiFi scans.
    private boolean wifiPing = false;
    
	// Current ping effect; null if none.
	private Pinger pinger = null;

}

