
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


import org.hermit.android.core.SurfaceRunner;
import org.hermit.android.instruments.Gauge;

import android.app.Activity;
import android.os.Bundle;
import android.view.MotionEvent;


/**
 * A view which displays tricorder data.
 */
abstract class DataView
	extends Gauge
{

	// ******************************************************************** //
	// Constructor.
	// ******************************************************************** //

	/**
	 * Set up this view.
	 * 
	 * @param	context			Parent application context.
     * @param   parent          Parent surface.
	 */
	DataView(Activity context, SurfaceRunner parent) {
		super(parent);
	}
	

	// ******************************************************************** //
	// Configuration.
	// ******************************************************************** //

    /**
     * Set the general scanning mode.  This affects whichever views support
     * it.
     * 
     * @param   continuous      If true, scan all the time.  Otherwise,
     *                          scan only under user control.
     */
    void setScanMode(boolean continuous) {
    }


    /**
     * Set the general scanning mode.  This affects whichever views support
     * it.
     * 
     * @param   enable          If true, play a sound while scanning
     *                          under user control.  Else don't.
     */
    void setScanSound(boolean enable) {
    }


    /**
     * Set the units in which to display numeric data.
     * 
     * @param   unit            Units to display.
     */
    void setDataUnits(Tricorder.Unit unit) {
    }


	// ******************************************************************** //
	// State Management.
	// ******************************************************************** //

	/**
	 * Notification that the overall application is starting (possibly
	 * resuming from a pause).  This does not mean that this view is visible.
	 * Views can use this to kick off long-term data gathering, but they
	 * should not use this to begin any CPU-intensive work; instead,
	 * wait for start().
	 */
	void appStart() {
	}
	

	/**
	 * Start this view.  This notifies the view that it should start
	 * receiving and displaying data.  The view will also get tick events
	 * starting here.
	 */
	abstract void start();
	

	/**
	 * A 1-second tick event.  Can be used for housekeeping and
	 * async updates.
	 * 
	 * @param	time				The current time in millis.
	 */
	void tick(long time) {
	}
	
	
	/**
	 * This view's aux button has been clicked.
	 */
	void auxButtonClick() {
	}
	

	/**
	 * Stop this view.  This notifies the view that it should stop
	 * receiving and displaying data, and generally stop using
	 * resources.
	 */
	abstract void stop();
	

	/**
	 * Notification that the overall application is stopping (possibly
	 * to pause).  Views can use this to stop any long-term activity.
	 */
	void appStop() {
	}


    /**
     * Unbind any statically-bound resources which could leak memory.
     * This is typically used when the app is being destroyed, possibly
     * as a result of a device orientation change.  If we have static data
     * which links to the activity, the activity will be leaked (i.e.
     * prevented from being garbage collected).  Hence unbind it here.
     */
	protected void unbindResources() {
    }
    

    // ******************************************************************** //
    // Data Handling.
    // ******************************************************************** //

    /**
     * Called when sensor values have changed.  The length and contents
     * of the values array vary depending on which sensor is being monitored.
     *
     * @param   sensor          The ID of the sensor being monitored.
     * @param   values          The new values for the sensor.
     */
    public void onSensorData(int sensor, float[] values) { }


	// ******************************************************************** //
	// Input.
	// ******************************************************************** //

    /**
     * Handle touch screen motion events.
     * 
     * @param	event			The motion event.
     * @return					True if the event was handled, false otherwise.
     */
	public boolean handleTouchEvent(MotionEvent event) {
		return false;
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
    }


    /**
     * Restore the application state from the given Bundle.
     * 
     * @param   icicle      The Bundle containing the saved state.
     */
    protected void restoreState(Bundle icicle) {
    }
    
}

