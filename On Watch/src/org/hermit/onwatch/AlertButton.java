
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


import org.hermit.android.widgets.MultistateImageButton;

import android.content.Context;
import android.util.AttributeSet;


/**
 * This widget is the alert time setting button.
 *
 * @author	Ian Cameron Smith
 */
public class AlertButton
	extends MultistateImageButton
{

	// ******************************************************************** //
    // Constructors.
    // ******************************************************************** //
	
	/**
	 * Create a multistate image button with a specified set of image
	 * resource IDs.
	 * 
	 * @param	context			Parent application.
	 * @param	attrs			Layout attributes.
	 */
	public AlertButton(Context context, AttributeSet attrs) {
		super(context, attrs, new int[] {
	    		R.drawable.ic_sound_off, R.drawable.ic_sound5,
	    		R.drawable.ic_sound10, R.drawable.ic_sound15
    	});
	}

}

