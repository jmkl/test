
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
 * Controller for the astronomical data view for OnWatch.  This class
 * displays interesting astronomical info such as rise and set times.
 * 
 * <p>Currently nothing is needed here.  However, it remains for completeness
 * of the controller architecture, and as a placeholder for future
 * functionality.
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
	}
	
}

