
/**
 * Plughole: a rolling-ball accelerometer game.
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


package org.hermit.plughole;

import android.util.Log;


/**
 * Thread class we use to control the animation.  The contract of this
 * class is:
 *   * When created, we start at once
 *   * While running, tick() in the outer class is called periodically
 *   * When killAndWait() is called, we stop and return
 */
class Ticker
	extends Thread
{

    // ******************************************************************** //
    // Constructor.
    // ******************************************************************** //

	/**
	 * Constructor -- start at once.
	 */
	public Ticker(Table user) {
        Log.v(TAG, "Ticker: start");

		this.user = user;
		enable = true;
		start();
	}

	
    // ******************************************************************** //
    // State Control.
    // ******************************************************************** //

	/**
	 * Stop this thread.  There will be no new calls to tick() after this.
	 */
	public void kill() {
		Log.v(TAG, "Ticker: kill");

		enable = false;
	}


	/**
	 * Stop this thread and wait for it to die.  When we return, it is
	 * guaranteed that tick() will never be called again.
	 * 
	 * Caution: if this is called from within tick(), deadlock is
	 * guaranteed.
	 */
	public void killAndWait() {
		Log.v(TAG, "Ticker: killAndWait");

		enable = false;

		// Wait for the thread to finish.  Ignore interrupts.
		if (isAlive()) {
			boolean retry = true;
			while (retry) {
				try {
					join();
					retry = false;
				} catch (InterruptedException e) {
				}
			}
			Log.v(TAG, "Ticker: killed");
		} else {
			Log.v(TAG, "Ticker: was dead");
		}
	}

	
    // ******************************************************************** //
    // Thread Body.
    // ******************************************************************** //

	/**
	 * Run method for this thread -- simply call tick() a lot until
	 * enable is false.
	 */
	@Override
	public void run() {
		while (enable) {
//			Log.v(TAG, "Ticker: tick");
			user.tick();
//			try {
//				Thread.sleep(100);
//			} catch (InterruptedException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
//			Log.v(TAG, "Ticker: tock");
		}
	}
	

    // ******************************************************************** //
    // Class Data.
    // ******************************************************************** //

    // Debugging tag.
	@SuppressWarnings("unused")
	private static final String TAG = "tiltball";

	
	// ******************************************************************** //
	// Private Data.
	// ******************************************************************** //
	
	// The Table which is using this ticker.
	private Table user = null;
	
	// Flag used to terminate this thread -- when false, we die.
	private boolean enable = false;

}

