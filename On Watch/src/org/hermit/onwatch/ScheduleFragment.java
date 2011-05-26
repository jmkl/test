
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

import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.Button;


/**
 * This view displays the watch schedule.  It also allows schedules to
 * be created and edited.
 */
public class ScheduleFragment
	extends Fragment
	implements ViewFragment
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
	@Override
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
	@Override
	public View onCreateView(LayoutInflater inflater,
							 ViewGroup container,
							 Bundle icicle)
	{
		Log.i(TAG, "onCreateView(" + (icicle != null ? "icicle" : "null") + ")");
		
        // Inflate the layout for this fragment
        appContext = (OnWatch) container.getContext();
        View view = inflater.inflate(R.layout.schedule_view, container, false);
		
		// Add the handler to the edit button.
		Button editCrew = (Button) view.findViewById(R.id.sched_edit_button);
		editCrew.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				Intent intent = new Intent();
				intent.setClass(appContext, CrewEditor.class);
				appContext.startActivity(intent);
			}
		});

        return view;
	}

	
	/**
	 * Called when the fragment is visible to the user and actively running.
	 * This is generally tied to Activity.onResume() of the containing
	 * Activity's lifecycle.
	 */
	@Override
	public void onResume () {
		Log.i(TAG, "onResume()");
		
		super.onResume();
	}

	
	/**
	 * Called when the Fragment is no longer resumed.  This is generally
	 * tied to Activity.onPause of the containing Activity's lifecycle.
	 */
	@Override
	public void onPause() {
		Log.i(TAG, "onPause()");
		
		super.onPause();
	}


	// ******************************************************************** //
    // App Lifecycle.
    // ******************************************************************** //

	/**
	 * Start this view.
	 * 
	 * @param	time			Our serivce, which is now available.
	 */
	public void start(OnWatchService service) {
		
	}

	
	/**
	 * Regular tick event, for housekeeping.  Occurs every second, as
	 * close as possible to the 1-second boundary.
	 * 
	 * @param	time			Current system time in millis.
	 */
	@Override
	public void tick(long time) {
	}


	/**
	 * Stop this view.  The OnWatchService is no longer usable.
	 */
	public void stop() {
		
	}


	// ******************************************************************** //
	// Class Data.
	// ******************************************************************** //

    // Debugging tag.
	private static final String TAG = "onwatch";


	// ******************************************************************** //
	// Private Data.
	// ******************************************************************** //

	// Parent app we're running in.
	private OnWatch appContext;

}

