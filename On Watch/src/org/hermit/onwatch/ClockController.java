
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


import org.hermit.onwatch.CrewModel.Crew;
import org.hermit.utils.TimeUtils;

import android.content.Context;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;


/**
 * This class displays a watch clock.  It displays the current date and time,
 * including watch name and time of the watch.
 */
public class ClockController
{

	// ******************************************************************** //
    // Constructors.
    // ******************************************************************** //
	
	/**
	 * Create a watch clock.
	 * 
	 * @param	context			Parent application.
	 */
	public ClockController(OnWatch context) {
		appContext = context;
		timeModel = TimeModel.getInstance(context);
		crewModel = CrewModel.getInstance(context);
		
		// Register for watch crew changes.
		crewModel.listen(new CrewModel.Listener() {
			@Override
			public void watchPlanChanged() {
			}
			@Override
			public void watchChange(int day, int watch, Crew[] crew) {
				crewField.setText(crewModel.getWatchCrewNames());
		    	nextCrewField.setText("");
			}
			@Override
			public void watchAlert(Crew[] nextCrew) {
		    	nextCrewField.setText(crewModel.getNextCrewNames());
			}
		});

		// Get the relevant widgets.  Set a handler on the zone set button.
		dateField = (TextView) context.findViewById(R.id.clock_date);
		zoneChoice = (Button) context.findViewById(R.id.clock_set_zone);
		timeField = (TextView) context.findViewById(R.id.clock_time);
		watchField = (TextView) context.findViewById(R.id.clock_watch);
		crewField = (BarTextWidget) context.findViewById(R.id.clock_watchcrew);
		nextCrewField = (TextView) context.findViewById(R.id.clock_nextcrew);
		
    	zoneChoice.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				((OnWatch) appContext).requestTimezone();
			}
    	});

		timeText = new StringBuilder(8);
		timeText.append("00:00:00");
		
    	crewField.setText(crewModel.getWatchCrewNames());
    	if (crewModel.getTimeToNext() < 15)
    		nextCrewField.setText(crewModel.getNextCrewNames());
    	else
    		nextCrewField.setText("");
	}


	// ******************************************************************** //
	// Run Control.
	// ******************************************************************** //

	/**
	 * Regular tick event, for housekeeping.  Occurs every second, as
	 * close as possible to the 1-second boundary.
	 * 
	 * @param	time			Current system time in millis.
	 */
	void tick(long time) {
		update();
	}


	// ******************************************************************** //
	// Display.
	// ******************************************************************** //

    /**
     * Display the current date and time.
     */
    private void update() {
    	// Display the date, and set the timezone button.
    	String wday = timeModel.getName(TimeModel.Field.WDAY);
    	String mon = timeModel.getName(TimeModel.Field.MONTH);
    	String day = timeModel.getName(TimeModel.Field.DAY);
    	
    	String date = wday + " " + day + " " + mon;
    	String zoff = TimeUtils.intervalMsToHmsShort(timeModel.getTimezoneOffset());
    	dateField.setText(date);
    	
    	boolean naut = timeModel.isNauticalTime();
    	zoneChoice.setText((naut ? "Naut" : "Civil") + " " + zoff);
    	
    	// Display the time.
    	int hour = timeModel.get(TimeModel.Field.HOUR);
    	int min = timeModel.get(TimeModel.Field.MINUTE);
    	int sec = timeModel.get(TimeModel.Field.SECOND);
    	timeText.setCharAt(0, (char) ('0' + hour / 10));
    	timeText.setCharAt(1, (char) ('0' + hour % 10));
    	timeText.setCharAt(3, (char) ('0' + min / 10));
    	timeText.setCharAt(4, (char) ('0' + min % 10));
    	timeText.setCharAt(6, (char) ('0' + sec / 10));
    	timeText.setCharAt(7, (char) ('0' + sec % 10));
    	timeField.setText(timeText);

    	// Display the watch name and names of the on-watch crew.
    	watchField.setText(timeModel.getWatchName());
//    	crewField.setText(crewModel.getWatchCrewNames());
    	crewField.setBar(crewModel.getWatchFrac());
//    	nextCrewField.setText(crewModel.getNextCrewNames());
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
	private Context appContext;
	
	// Watch calendar, which does all our date/time calculations.
	private TimeModel timeModel;
	
	// The crew list and watch plan model.
	private CrewModel crewModel;
	
	// Buffer we create the time display in.
	private StringBuilder timeText;
	   
    // Fields for displaying the date and time.
	private TextView dateField;
    private TextView timeField;
    
    // Timezone selection button.
    private Button zoneChoice;
    
    // Field for displaying the watch name and the watch crew names.
    private TextView watchField;
    private BarTextWidget crewField;
    private TextView nextCrewField;

}

