
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
 * This widget is the chimes on/off button.
 */
public class ChimeButton
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
	public ChimeButton(Context context, AttributeSet attrs) {
		super(context, attrs, new int[] {
	    		R.drawable.ic_ring_off, R.drawable.ic_ring_on
    	});
	}

}

