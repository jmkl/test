
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

import android.view.View;
import android.widget.LinearLayout;


/**
 * The tricorder navigation control, consisting of the buttons at the left.
 */
class NavigationBar
	extends LinearLayout
{

	// ******************************************************************** //
	// Public Types.
	// ******************************************************************** //
	
	/**
	 * Set up this view.
	 * 
	 * @param	parent			Parent application context.
	 * @param	width			Width available for the nav bar.
	 * @param	height			Height available for the nav bar.
	 */
	NavigationBar(Tricorder parent, int width, int height) {
		super(parent);
		
		parentApp = parent;
		
		createGui(parent, width, height);
	}


    /**
     * Create the GUI for this view.
     * 
     * @param	parent			Parent application context.
	 * @param	width			Width available for the nav bar.
	 * @param	height			Height available for the nav bar.
     */
    private void createGui(Tricorder parent, int width, int height) {
        setOrientation(LinearLayout.VERTICAL);
    	final int FPAR = LinearLayout.LayoutParams.FILL_PARENT;
        LinearLayout.LayoutParams layout;
        
        final int numViews = ViewDefinition.values().length;
        
        // Get the inter-button gap.
        int gap = Gauge.getInnerGap();

        // Work out a good button height.
        int h = height / numViews - gap;
        if (h > width * 3 / 4)
        	h = width * 3 / 4;

        // Figure out the remaining space.  Divide it between top padding
        // and bottom padding.
        final int rem = height - (h + gap) * numViews;
        int top = rem / 2;
        if (top > h / 2)
        	top = h / 2;
        int bottom = rem - top;
        
        // Add some top padding, if we're in portrait mode.
        topPad = new View(parent);
        layout = new LinearLayout.LayoutParams(FPAR, top);
        layout.setMargins(0, 0, 0, 0);
        addView(topPad, layout);

        for (ViewDefinition bdef : ViewDefinition.values()) {
        	NavButton but = new NavButton(parent, bdef);
            layout = new LinearLayout.LayoutParams(FPAR, h);
            layout.setMargins(0, gap, 0, 0);
            addView(but, layout);
            but.setOnClickListener(new View.OnClickListener() {
            	@Override
            	public void onClick(View arg0) { clicked((NavButton) arg0); }
            });
        }

        // If there's space add the bottom fill, which blends into the swoop
    	// background.
        if (bottom > 0) {
        	bottomPad = new View(parent);
        	layout = new LinearLayout.LayoutParams(FPAR, FPAR, 1);
        	layout.setMargins(0, gap, 0, 0);
        	addView(bottomPad, layout);
        }
    }
    
    
    /**
     * A nav button was clicked.
     * 
     * @param	but				The button.
     */
    private void clicked(NavButton but) {
        try {
            parentApp.selectDataView(but.getViewDef());
        } catch (Exception e) {
            parentApp.reportException(e);
        }
    }


	// ******************************************************************** //
	// Data Management.
	// ******************************************************************** //

    /**
     * Select and display the given view.
     * 
     * @param	viewDef			View definition of the view to show.
     */
    void selectDataView(ViewDefinition viewDef) {
    	if (topPad != null)
    		topPad.setBackgroundColor(viewDef.bgColor);
    	if (bottomPad != null)
    		bottomPad.setBackgroundColor(viewDef.bgColor);
    }

    
    // ******************************************************************** //
    // Class Data.
    // ******************************************************************** //

    // Debugging tag.
	@SuppressWarnings("unused")
	private static final String TAG = "tricorder";

	
	// ******************************************************************** //
	// Private Data.
	// ******************************************************************** //

	// Parent application context.
	private Tricorder parentApp;

	// Top and bottom padding sections.  Null if not used.
	private View topPad = null;
	private View bottomPad = null;

}

