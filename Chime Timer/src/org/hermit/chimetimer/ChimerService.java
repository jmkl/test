
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


package org.hermit.chimetimer;


import org.hermit.android.sound.Effect;
import org.hermit.android.sound.Player;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;


/**
 * This class is the background service for ChimeTimer.
 */
public class ChimerService
	extends Service
{

	// ******************************************************************** //
    // Shared Types.
    // ******************************************************************** //

    /**
     * Class used for the client Binder.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with IPC.
     */
    class ChimerBinder extends Binder {
    	public ChimerService getService() {
            // Return this instance of the service so clients can call
    		// our public methods.
            return ChimerService.this;
        }
    }


    /**
     * The sounds that we make.
     */
	static enum SoundEffect {
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
    	
    	void play() {
    		if (playerEffect == null)
    			throw new IllegalStateException("tried to play before player" +
    											" was initialised");
    		playerEffect.play();
    	}
        
        static final SoundEffect[] VALUES = values();

    	// Resource ID for the sound file.
    	private final int soundRes;
    	
    	// Effect object representing this sound.
        private Effect playerEffect = null;
    }


	// ******************************************************************** //
    // Service Lifecycle.
    // ******************************************************************** //

	/**
	 * Called by the system when the service is first created.
	 */
    @Override
    public void onCreate() {
    	Log.i(TAG, "Svc onCreate()");
        super.onCreate();
        
        // Load the sounds.
        soundPlayer = createSoundPlayer();

        // Set myself up as a foreground service.
        Notification n = new Notification(R.drawable.bell0,
        								  getText(R.string.service_notif),
        								  System.currentTimeMillis());
        Intent ni = new Intent(this, ChimeTimer.class);
        PendingIntent pi = PendingIntent.getActivity(this, 0, ni, 0);
        n.setLatestEventInfo(this, getText(R.string.service_notif),
        					 getText(R.string.service_notif), pi);
        startForeground(1, n);
		
		// Set ourselves running.
		resume();
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
    @Override
	public int onStartCommand(Intent intent, int flags, int startId) {
    	String action = intent == null ? null : intent.getAction();
    	Log.i(TAG, "Svc onStartCommand: " + action);
    	
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
    @Override
	public IBinder onBind(Intent intent) {
    	if (serviceBinder == null)
    		serviceBinder = new ChimerBinder();
        
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
    @Override
	public void onDestroy() {
        Log.i(TAG, "Svc onDestroy()");
        
        super.onDestroy();
        
        // Shut down.
        pause();
    }


	// ******************************************************************** //
	// Run Control.
	// ******************************************************************** //

    /**
     * Pause the service (e.g. for maintenance).
     */
    public void pause() {
        Log.i(TAG, "Svc pause()");
        
        soundPlayer.suspend();
    }
    

    /**
     * Resume the service from a pause.
     */
    public void resume() {
        Log.i(TAG, "Svc resume()");
        
        soundPlayer.resume();
    }
    

    /**
     * Shut down the service.
     */
    public void shutdown() {
        Log.i(TAG, "Svc shutdown()");
        
        pause();
    	stopSelf();
    }
    

	// ******************************************************************** //
	// Sound Playing.
	// ******************************************************************** //
    
    /**
     * Create a sound player containing the app's sound effects.
     */
    private Player createSoundPlayer() {
    	Player player = new Player(this, 3);
        for (SoundEffect sound : SoundEffect.VALUES)
            sound.playerEffect = player.addEffect(sound.soundRes, 1);
        
        return player;
    }

    
    /**
     * Make a sound.  Play it immediately.  Don't touch the queue.
     * 
     * @param	which			ID of the sound to play.
     */
    void makeSound(SoundEffect which) {
        Log.i(TAG, "Svc play " + which);
        which.play();
	}


	// ******************************************************************** //
	// Class Data.
	// ******************************************************************** //

    // Debugging tag.
	private static final String TAG = "ChimeTimer";

	
	// ******************************************************************** //
	// Private Data.
	// ******************************************************************** //
	
    // The Binder given to clients.
    private IBinder serviceBinder = null;
    
    // Sound player used for sound effects.
    private Player soundPlayer = null;

}

