
/**
 * On Watch: sailor's watchkeeping assistant.
 * <br>Copyright 2009 Ian Cameron Smith
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


/**
 * Common interface of OnWatch views.
 */
public abstract class OnWatchController
{

	// ******************************************************************** //
    // Constructor.
    // ******************************************************************** //
	
	/**
	 * Create a view instance.
	 * 
	 * @param	context			Parent application.
	 */
	public OnWatchController(OnWatch context) {
	}
	

	// ******************************************************************** //
	// State Control.
	// ******************************************************************** //

	/**
	 * Start the application.  Called at initial start-up.
	 */
	void start() {
		
	}
	

	/**
	 * Resume the application.
	 */
	void resume() {
		
	}
	

	/**
	 * Pause the application.
	 */
	void pause() {
		
	}
	

	/**
	 * Stop the application.  Called (probably) when shutting down completely.
	 */
	void stop() {
		
	}
	

	/**
	 * Regular tick event, for housekeeping.  Occurs every second, as
	 * close as possible to the 1-second boundary.
	 * 
	 * @param	time			Current system time in millis.
	 */
	void tick(long time) {
		
	}
	
}

