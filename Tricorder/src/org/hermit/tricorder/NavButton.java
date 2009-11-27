
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

import org.hermit.android.instruments.Gauge;
import org.hermit.tricorder.TricorderView.ViewDefinition;

import android.graphics.Typeface;
import android.util.TypedValue;
import android.view.Gravity;
import android.widget.TextView;


/**
 * A button in the navigation bar.  We don't want lots of fancy button
 * decorations, just a flat coloured rectangle, so we use TextView
 * as the base class.  The click handler passes the event up to the
 * parent app.
 */
class NavButton
	extends TextView
{

	// ******************************************************************** //
	// Constructor.
	// ******************************************************************** //

	NavButton(Tricorder parent, ViewDefinition def) {
		super(parent);

		setTextColor(0xff000000);
        setTextSize(TypedValue.COMPLEX_UNIT_PX, Gauge.getHeadTextSize());
		setTextScaleX(0.5f);
		setGravity(Gravity.BOTTOM | Gravity.RIGHT);
		setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
		setPadding(0, 0, 4, 0);
		
		if (def != null)
			setViewDef(def);
	}


	/**
	 * Set the view definition this button is assigned to.
	 * 
	 * @param  vdef		The view definition this button is assigned to.
	 */
	void setViewDef(ViewDefinition vdef) {
		setViewDef(vdef, vdef.labelId);
	}
	

	/**
	 * Set the view definition this button is assigned to.
	 * 
     * @param  vdef     The view definition this button is assigned to.
     * @param  text     Text to display in this button.
	 */
	void setViewDef(ViewDefinition vdef, int text) {
		viewDefinition = vdef;
		setText(text);
		setBackgroundColor(viewDefinition.bgColor);
	}
	
	
	/**
	 * Get the view definition this button is assigned to.
	 * 
	 * @return			The view definition this button is assigned to.
	 */
	ViewDefinition getViewDef() {
		return viewDefinition;
	}
	
	
	// ******************************************************************** //
	// Private Data.
	// ******************************************************************** //

	// View definition this button is linked to.
	private ViewDefinition viewDefinition;

}

