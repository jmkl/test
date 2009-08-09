
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


import android.view.View;


/**
 * Controller for the astronomical data view for OnWatch.  This class
 * displays interesting astronomical info such as rise and set times.
 *
 * @author	Ian Cameron Smith
 */
public class AstroController
	extends OnWatchController
{

	// ******************************************************************** //
    // Constructor.
    // ******************************************************************** //
	
	/**
	 * Create a view instance.
	 * 
	 * @param	context			Parent application.
	 */
	public AstroController(OnWatch context) {
		super(context);
		
		// Get the astro calendar widget's handle.  Tell it the ID
		// of it's parent scroller.
        astroCal = (AstroCalendarWidget) context.findViewById(R.id.astro_calendar);
        View astroScroller = context.findViewById(R.id.astro_scroller);
        astroCal.setParentScroller(astroScroller);
	}
	

	// ******************************************************************** //
	// Class Data.
	// ******************************************************************** //

    // Debugging tag.
	@SuppressWarnings("unused")
	private static final String TAG = "onwatch";


	// ******************************************************************** //
	// Private Data.
	// ******************************************************************** //

    // The astronomical calendar widget.
    private AstroCalendarWidget astroCal;

}

