
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


package org.hermit.onwatch.service;


import java.util.LinkedList;

import org.hermit.android.utils.Ticker;
import org.hermit.onwatch.OnWatch;
import org.hermit.onwatch.R;
import org.hermit.onwatch.TimeModel;
import org.hermit.onwatch.service.Chimer.AlertMode;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.SoundPool;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.preference.PreferenceManager;
import android.util.Log;


/**
 * This class is the main activity for OnWatch.
 */
public class OnWatchService
	extends Service
{

	// ******************************************************************** //
	// Public Types.
	// ******************************************************************** //

    /**
     * Class used for the client Binder.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with IPC.
     */
    public class OnWatchBinder extends Binder {
    	public OnWatchService getService() {
            // Return this instance of the service so clients can call
    		// our public methods.
            return OnWatchService.this;
        }
    }


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
    	
    	private int soundRes;           // Resource ID for the sound file.
        private int soundId = 0;        // Sound ID for playing.
    }


	// ******************************************************************** //
    // Service Lifecycle.
    // ******************************************************************** //

	/**
	 * Called by the system when the service is first created.
	 */
    @Override
    public void onCreate() {
    	Log.i(TAG, "onCreate()");
        super.onCreate();

        // Set myself up as a foreground service.
        Notification n = new Notification(R.drawable.android,
        								  getText(R.string.service_notif),
        								  System.currentTimeMillis());
        Intent ni = new Intent(this, OnWatch.class);
        PendingIntent pi = PendingIntent.getActivity(this, 0, ni, 0);
        n.setLatestEventInfo(this, getText(R.string.service_notif),
        					 getText(R.string.service_notif), pi);
        startForeground(1, n);
        
        final TimeModel timeModel = TimeModel.getInstance(this);

        // Load the sounds.
        soundPool = createSoundPool();

		// Create the sound queue and a handler to launch sounds.
		soundQueue = new LinkedList<Sound>();
		soundHandler = new Handler() {
			@Override
			public void handleMessage(Message m) {
				playQueuedSound();
			}
		};
		soundPlaying = false;
		
		// Create the chimer.
		bellChime = Chimer.getInstance(this);

        // Restore our preferences.
        updatePreferences();

        // Start the 1-minute tick events.
		ticker = new Ticker();
		ticker.listen(60, new Ticker.Listener() {
			@Override
			public void tick(long time, int daySecs) {
				timeModel.tick(time);
			}
		}); 
		
		ticker.start();
    }


    /**
     * Called by the system every time a client explicitly starts the service
     * by calling startService(Intent), providing the arguments it supplied
     * and a unique integer token representing the start request.
     * 
     * Note that the system calls this on your service's main thread.  A
     * service's main thread is the same thread where UI operations take
     * place for Activities running in the same process.
     * 
     * @param	intent		The Intent supplied to startService(Intent),
     * 						as given.  This may be null if the service is
     * 						being restarted after its process has gone away,
     * 						and it had previously returned anything except
     * 						START_STICKY_COMPATIBILITY.
     * @param	flags		Additional data about this start request.
     * 						Currently either 0, START_FLAG_REDELIVERY, or
     * 						START_FLAG_RETRY.
     * @param	startId		A unique integer representing this specific
     * 						request to start.  Use with stopSelfResult(int).
     * @return				May be one of the constants associated with
     * 						the START_CONTINUATION_MASK bits.  The value
     * 						indicates what semantics the system should use
     * 						for the service's current started state.
     */
    public int onStartCommand(Intent intent, int flags, int startId) {
    	return START_STICKY;
    }


    /**
     * Return the communication channel to the service.  May return null if
     * clients can not bind to the service.  The returned IBinder is usually
     * for a complex interface that has been described using aidl.
     * 
     * Note that unlike other application components, calls on to the
     * IBinder interface returned here may not happen on the main thread of
     * the process.
     * 
     * @param	intent		The Intent that was used to bind to this service,
     * 						as given to Context.bindService. Note that any
     * 						extras that were included with the Intent at that
     * 						point will not be seen here.
     * @return				An IBinder through which clients can call the
     * 						service.
     */
    public IBinder onBind(Intent intent) {
        serviceBinder = new OnWatchBinder();
        
    	return serviceBinder;
    }
    
    
    /**
     * Called by the system to notify a Service that it is no longer used
     * and is being removed.  The service should clean up an resources it
     * holds (threads, registered receivers, etc) at this point.
     * 
     * Upon return, there will be no more calls in to this Service object
     * and it is effectively dead.
     */
    public void onDestroy() {
        Log.i(TAG, "onDestroy()");
        
        super.onDestroy();

		// Stop the tick events.
		if (ticker != null) {
			ticker.stop();
			ticker = null;
		}
    }


	// ******************************************************************** //
	// Preferences Handling.
	// ******************************************************************** //

    /**
     * Read our application preferences and configure ourself appropriately.
     */
    private void updatePreferences() {
    	SharedPreferences prefs =
    					PreferenceManager.getDefaultSharedPreferences(this);

    	boolean chimeWatch = true;
    	try {
    		chimeWatch = prefs.getBoolean("chimeWatch", true);
    	} catch (Exception e) {
    		Log.i(TAG, "Pref: bad chimeWatch");
    	}
    	Log.i(TAG, "Prefs: chimeWatch " + chimeWatch);
    	setChimeEnable(chimeWatch);

    	Chimer.AlertMode alertMode = Chimer.AlertMode.OFF;
    	try {
    		String mval = prefs.getString("alertMode", "OFF");
    		alertMode = Chimer.AlertMode.valueOf(mval);
    	} catch (Exception e) {
    		Log.i(TAG, "Pref: bad alertMode");
    	}
    	Log.i(TAG, "Prefs: alertMode " + alertMode);
    	setRepeatAlert(alertMode);
   }


	// ******************************************************************** //
	// Alert Controls Handling.
	// ******************************************************************** //

	/**
	 * Query whether the half-hour watch chimes are enabled.
	 * 
	 * @return					true iff the chimes are enabled.
	 */
	public boolean getChimeEnable() {
    	return bellChime.getChimeEnable();
    }


	/**
	 * Enable or disable the half-hour watch chimes.
	 * 
	 * @param	enable			true to enable chimes, false to disable.
	 */
	public void setChimeEnable(boolean enable) {
    	SharedPreferences prefs =
			PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean("chimeWatch", enable);
        editor.commit();

        bellChime.setChimeEnable(enable);
    }

    
    /**
     * Get the current repeating alert mode.
     * 
     * @return					The current mode.
     */
    public AlertMode getRepeatAlert() {
    	return bellChime.getRepeatAlert();
    }
    

    /**
     * Set up a repeating alert.
     * 
     * @param	interval		Desired alert mode.
     */
    public void setRepeatAlert(AlertMode mode) {
    	SharedPreferences prefs =
			PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("alertMode", mode.toString());
        editor.commit();
        
        bellChime.setRepeatAlert(mode);
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
        float vol = 1.0f;
        soundPool.play(which.soundId, vol, vol, 1, 0, 1f);
	}
	

	// ******************************************************************** //
	// Class Data.
	// ******************************************************************** //

    // Debugging tag.
	private static final String TAG = "onwatchsvc";


	// ******************************************************************** //
	// Private Data.
	// ******************************************************************** //
	
    // The Binder given to clients.
    private IBinder serviceBinder = null;
	
	// Chimer.
	private Chimer bellChime;

    // Timer we use to generate tick events.
    private Ticker ticker = null;

	// Handler for sounds.  We need this to get back onto the main thread.
	private Handler soundHandler;
	
	// Queue of sounds to be played.
	private LinkedList<Sound> soundQueue;
    
    // Sound pool used for sound effects.
    private SoundPool soundPool;

	// True if a queued sound is currently playing.
	private boolean soundPlaying;

}

