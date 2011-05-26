
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

import org.hermit.onwatch.service.OnWatchService;


/**
 * This interface defines a fragment which forms one of the main views
 * of our app.  These views all have certain features in common, and those
 * are defined here.
 */
public interface ViewFragment
{

	/**
	 * Regular tick event, for housekeeping.  Occurs every second, as
	 * close as possible to the 1-second boundary.
	 * 
	 * @param	time			Current system time in millis.
	 */
	void tick(long time);

	/**
	 * Start this view.
	 * 
	 * @param	time			Our serivce, which is now available.
	 */
	public void start(OnWatchService service);

	/**
	 * Stop this view.  The OnWatchService is no longer usable.
	 */
	public void stop();

}

