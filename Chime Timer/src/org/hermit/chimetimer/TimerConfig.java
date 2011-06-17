
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


public class TimerConfig
{

	// ******************************************************************** //
	// Public Methods.
	// ******************************************************************** //

	/**
	 * Convert to a string.
	 */
	@Override
	public String toString() {
		return "Timer<\"" + name + "\" " + preTime + "/" + runTime + ">";
	}
	
	
	// ******************************************************************** //
	// Public Constants.
	// ******************************************************************** //

	/**
	 * Number of user-configurable timers.
	 */
	public static final int NUM_TIMERS = 4;
	
	
	// ******************************************************************** //
	// Public Data.
	// ******************************************************************** //
    
    // Timer name.
    public String name = "";
    
    // Pre-start time in ms.
    public long preTime = 0;
    
    // Bell number (1-n) of starting bell.  0 for none.
    public int startBell = 0;
    
    // Run time in ms.
    public long runTime = 5000;
    
    // Bell number (1-n) of finishing bell.  0 for none.
    public int endBell = 1;

}

