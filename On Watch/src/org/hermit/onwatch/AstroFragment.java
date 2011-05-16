
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

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;


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
public class AstroFragment
	extends ViewFragment
{

	// ******************************************************************** //
    // Fragment Lifecycle.
    // ******************************************************************** //
	
	/**
	 * Called to do initial creation of a fragment. This is called after
	 * onAttach(Activity) and before
	 * onCreateView(LayoutInflater, ViewGroup, Bundle).
	 * 
	 * Note that this can be called while the fragment's activity is still
	 * in the process of being created.  As such, you can not rely on things
	 * like the activity's content view hierarchy being initialized at this
	 * point.  If you want to do work once the activity itself is created,
	 * see onActivityCreated(Bundle).
	 * 
	 * @param	icicle		If the fragment is being re-created from a
	 * 						previous saved state, this is the state.
	 */
	public void onCreate(Bundle icicle) {
		Log.i(TAG, "onCreate(" + (icicle != null ? "icicle" : "null") + ")");
		
		super.onCreate(icicle);
	}


	/**
	 * Called to have the fragment instantiate its user interface view.
	 * This is optional, and non-graphical fragments can return null
	 * (which is the default implementation).  This will be called between
	 * onCreate(Bundle) and onActivityCreated(Bundle).
	 *
	 * If you return a View from here, you will later be called in
	 * onDestroyView() when the view is being released.
	 *
	 * @param	inflater	The LayoutInflater object that can be used to
	 * 						inflate any views in the fragment.
	 * @param	container	If non-null, this is the parent view that the
	 * 						fragment's UI should be attached to.  The
	 * 						fragment should not add the view itself, but
	 * 						this can be used to generate the LayoutParams
	 * 						of the view.
	 * @param	icicle		If non-null, this fragment is being re-constructed
	 * 						from a previous saved state as given here.
	 * @return				The View for the fragment's UI, or null.
	 */
	public View onCreateView(LayoutInflater inflater,
							 ViewGroup container,
							 Bundle icicle)
	{
		Log.i(TAG, "onCreateView(" + (icicle != null ? "icicle" : "null") + ")");
		
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.astro_view, container, false);

        return view;
	}

	
	/**
	 * Called when the fragment is visible to the user and actively running.
	 * This is generally tied to Activity.onResume() of the containing
	 * Activity's lifecycle.
	 */
	public void onResume () {
		Log.i(TAG, "onResume()");
		
		super.onResume();
	}

	
	/**
	 * Called when the Fragment is no longer resumed.  This is generally
	 * tied to Activity.onPause of the containing Activity's lifecycle.
	 */
	public void onPause() {
		Log.i(TAG, "onPause()");
		
		super.onPause();
	}


	// ******************************************************************** //
	// Class Data.
	// ******************************************************************** //

    // Debugging tag.
	private static final String TAG = "onwatch";
    
}

