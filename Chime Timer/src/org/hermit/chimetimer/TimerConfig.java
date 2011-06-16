

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

