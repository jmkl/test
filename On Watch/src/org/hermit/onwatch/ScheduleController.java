
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

import android.content.Intent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;


/**
 * This view displays the watch schedule.  It also allows schedules to
 * be created and edited.
 */
public class ScheduleController
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
	public ScheduleController(OnWatch context) {
		super(context);
		
		appContext = context;
		
		// Add the handler to the edit button.
		Button editCrew = (Button) context.findViewById(R.id.sched_edit_button);
		editCrew.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				Intent intent = new Intent();
				intent.setClass(appContext, CrewEditor.class);
				appContext.startActivity(intent);
			}
		});
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

	// Parent app we're running in.
	private OnWatch appContext;

}

