
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


import org.hermit.chimetimer.Sounds.SoundEffect;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
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
	 * Timer state in tick messages: not started yet.
	 */
	static final int STATE_READY = 1;
	
	/**
	 * Timer state in tick messages: pre-timer running.
	 */
	static final int STATE_PRE = 2;
	
	/**
	 * Timer state in tick messages: main timer running.
	 */
	static final int STATE_RUNNING = 3;
	
	/**
	 * Timer state in tick messages: finished.
	 */
	static final int STATE_FINISHED = 4;
	
	
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

        soundManager = new Sounds(this);
        
        // Set myself up as a foreground service.
        Notification n = new Notification(R.drawable.bell0,
        								  getText(R.string.service_notif),
        								  System.currentTimeMillis());
        Intent ni = new Intent(this, ChimeTimer.class);
        PendingIntent pi = PendingIntent.getActivity(this, 0, ni, 0);
        n.setLatestEventInfo(this, getText(R.string.service_notif),
        					 getText(R.string.service_notif), pi);
        startForeground(1, n);

        currentState = STATE_READY;
        
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
     * Register a client for this service.  Send ticks to the
     * supplied handler.
     */
    void registerClient(Handler handler) {
    	tickHandler = handler;
    	
    	// Tell the new client where we are.
    	signalClients(lastState, lastRemain);
    }
    

    /**
     * Un-register the given client handler for this service.
     */
    void unregisterClient(Handler handler) {
    	tickHandler = null;
    }
    

    /**
     * Resume the service from a pause.
     */
    public void resume() {
        Log.i(TAG, "Svc resume()");
        
        soundManager.resume();
    }
    

    /**
     * Pause the service (e.g. for maintenance).
     */
    public void pause() {
        Log.i(TAG, "Svc pause()");
        
		// Stop the tick events.
		if (ticker != null) {
			ticker.kill();
			ticker = null;
		}
        
		soundManager.pause();
    }
    

    /**
     * Shut down the service.
     */
    public void shutdown() {
        Log.i(TAG, "Svc shutdown()");
        
        pause();
		soundManager.shutdown();
    	stopSelf();
    }
    

	// ******************************************************************** //
	// Timer Control.
	// ******************************************************************** //

    /**
     * Start a timer with the given configuration.  Send ticks to the
     * supplied handler.
     */
    void startTimer(TimerConfig config) {
    	if (ticker != null) {
    		ticker.kill();
    		ticker = null;
    	}

    	// Start the tick events.
    	ticker = new Ticker(config);
    }
    

    /**
     * Stop the current timer.
     */
    void stopTimer() {
    	if (ticker != null) {
    		ticker.kill();
    		ticker = null;
    	}
        currentState = STATE_READY;
        signalClients(currentState, 0);
    }
    
    
    /**
     * Tell whether the timer is running.
     * 
     * @return				true iff the timer is running.
     */
    boolean isRunning() {
    	return currentState == STATE_PRE || currentState == STATE_RUNNING;
    }
	
    
    /**
     * Get the current state of the timer.
     * 
     * @return				Current timer state.
     */
    int getState() {
    	return currentState;
    }
	
  
    /**
     * Tell the client where we are.
     * 
     * @param state
     * @param time
     */
	void signalClients(int state, int time) {
		if (tickHandler != null) {
			Message msg = tickHandler.obtainMessage(state, time, 0);
			tickHandler.sendMessage(msg);
		}
		lastState = state;
		lastRemain = time;
	}


    // ******************************************************************** //
    // Private Types.
    // ******************************************************************** //

	/**
	 * Class which generates our ticks.
	 */
	private class Ticker extends Thread {
		public Ticker(TimerConfig config) {
			timerConfig = config;
			startTime = System.currentTimeMillis();

			enable = true;
			start();
		}

		public void kill() {
			enable = false;
			interrupt();
		}

		@Override
		public void run() {
			long base = startTime;
			
			if (enable && timerConfig.preTime > 0) {
				currentState = STATE_PRE;
				base += timerConfig.preTime;
				runSegment(base);
			}
			
			if (enable && timerConfig.startBell != 0) {
				SoundEffect bell = SoundEffect.valueOf(timerConfig.startBell - 1);
				soundManager.makeSound(bell);
			}
			
			if (enable && timerConfig.runTime > 0) {
				base += timerConfig.runTime;
				currentState = STATE_RUNNING;
				runSegment(base);
			}
			
			currentState = STATE_FINISHED;
			signalClients(STATE_FINISHED, 0);

			if (enable && timerConfig.endBell != 0) {
				SoundEffect bell = SoundEffect.valueOf(timerConfig.endBell - 1);
				soundManager.makeSound(bell);
			}
			
			enable = false;
			ticker = null;
		}

		// Time up to the given end time.  Ping the handler when we start and
		// then every whole second up to the end time, and return when
		// we get there.
		private void runSegment(long endTime) {
			while (enable) {
				int remain = (int) (endTime - System.currentTimeMillis());
				if (remain < 0)
					remain = 0;
				signalClients(currentState, remain);
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
		}

		private final TimerConfig timerConfig;
		private final long startTime;
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
	
    // The Binder given to clients.
    private IBinder serviceBinder = null;
    
    // Sound manager used for sound effects.
    private Sounds soundManager = null;

    // Timer we use to generate tick events.
    private Ticker ticker = null;
	
	// Handler for updates, supplied by the client.  If null, no updates are
    // generated.
	private Handler tickHandler = null;

	// Current timer state.
	private int currentState = STATE_READY;

	// Last state data sent to clients.
	private int lastState = STATE_READY;
	private int lastRemain = 0;

}

