
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


import org.hermit.geo.Distance;
import org.hermit.geo.Position;
import org.hermit.onwatch.LocationModel.GpsState;
import org.hermit.onwatch.TimeModel.Field;
import org.hermit.onwatch.provider.PassageSchema;
import org.hermit.onwatch.service.PassageRecord;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.Button;
import android.widget.SimpleCursorAdapter;
import android.widget.Spinner;
import android.widget.TextView;


/**
 * This class controls the passage data display.
 * 
 * Main pane
 * 
 * [   Passage list   ]    -- select passage; doesn't affect running passage if any
 * passage data
 * . . .
 * < [New] [Edit] [Del] >  -- New or Edit pops up editor; warning if running
 * < [      Start     ] >  -- Start/Stop controls passage
 *                        
 * Edit window
 * 
 * passage data form
 * . . .
 * <        Done        >  -- Commit
 */
public class PassageFragment
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
        appContext = (OnWatch) container.getContext();
        View view = inflater.inflate(R.layout.passage_view, container, false);
        
		contentResolver = appContext.getContentResolver();
		
        // Perform a managed query to get the passage list from the content
		// provider.  The Activity will handle closing and requerying the
		// cursor when needed.
		allPassagesCursor =
			appContext.managedQuery(PassageSchema.Passages.CONTENT_URI,
		                         PassageSchema.Passages.PROJECTION,
		                         null, null,
		                         PassageSchema.Passages.START_TIME + " DESC");

        // Perform a managed query to get the currently running passage,
		// if any, from the content provider.
        openPassageCursor =
        	appContext.managedQuery(PassageSchema.Passages.CONTENT_URI,
                                 PassageSchema.Passages.PROJECTION,
                                 "? != 0",
                                 new String[] { PassageSchema.Passages.UNDER_WAY },
                                 PassageSchema.Passages.SORT_ORDER);
        
		// Get the passage selector widget.  Give it an adapter which maps
		// on to the passage names list.
		passagePicker = (Spinner) view.findViewById(R.id.passage_picker);
		passageAdapter =
            new SimpleCursorAdapter(appContext,
                                    android.R.layout.simple_spinner_item,
                                    allPassagesCursor,
                                    new String[] { PassageSchema.Passages.NAME },
                                    new int[] { android.R.id.text1 });
		passageAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		passagePicker.setAdapter(passageAdapter);

		// Set a selection handler on the picker.
        passagePicker.setOnItemSelectedListener(new OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> view, View item,
									   int pos, long id) {
				selectPassage(id);
			}
			@Override
			public void onNothingSelected(AdapterView<?> arg0) {
			}
        });

		// Get the relevant widgets.  Set a handlers on the buttons.
        startPlaceField = (TextView) view.findViewById(R.id.pass_start_place);
        startTimeField = (TextView) view.findViewById(R.id.pass_start_time);
        endPlaceField = (TextView) view.findViewById(R.id.pass_end_place);
        endTimeField = (TextView) view.findViewById(R.id.pass_end_time);
		statusDescField = (TextView) view.findViewById(R.id.pass_stat_desc);
		statusAuxField = (TextView) view.findViewById(R.id.pass_stat_aux);
		
        Button newButton = (Button) view.findViewById(R.id.passage_new_button);
        newButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View arg0) {
                newPassage();
            }
        });
        
        // Add the handler to the edit button.
        Button editButton = (Button) view.findViewById(R.id.passage_edit_button);
        editButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View arg0) {
                editPassage();
            }
        });

        Button deleteButton = (Button) view.findViewById(R.id.passage_delete_button);
        deleteButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View arg0) {
                deletePassage();
            }
        });

        startButton = (Button) view.findViewById(R.id.passage_start_button);
		startButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
			    startOrFinishPassage();
			}
		});
        
        // Get the time model.  Ask it to ping us each bell.
        timeModel = TimeModel.getInstance(appContext);
        timeModel.listen(TimeModel.Field.MINUTE, new TimeModel.Listener() {
            @Override
            public void change(Field field, int value, long time) {
                // FIXME: do we need this?
            }
        });

        // Get our location model.  Ask it to keep us up to date.
        locationModel = LocationModel.getInstance(appContext);
        locationModel.listen(new LocationModel.Listener() {
            @Override
            public void posChange(GpsState state, String stateMsg,
                                  Position pos, String locMsg) {
                // Add the point to the points log, if we're in a passage.
                if (passageData != null && passageData.isRunning()) {
                    long time = System.currentTimeMillis();
                    logPoint(pos, "", time);
                }
            }
        });

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
	// Run Control.
	// ******************************************************************** //

	/**
	 * Regular tick event, for housekeeping.  Occurs every second, as
	 * close as possible to the 1-second boundary.
	 * 
	 * @param	time			Current system time in millis.
	 */
	@Override
	void tick(long time) {
		update();
	}


	// ******************************************************************** //
	// Passage Data Management.
	// ******************************************************************** //

    /**
     * Find an active -- i.e. under-way -- passage in the database.  If it
     * exists, copy its data into passageData.
     * 
     * @return              True if we found an open passage and copied it
     *                      into passageData.  False if we didn't, and
     *                      passageData is unchanged.
     */
    public boolean loadOpenPassage() {
        openPassageCursor.requery();
        if (!openPassageCursor.moveToFirst())
            return false;
        
        loadPassage(openPassageCursor);
        return true;
    }


    /**
     * Find the newest passage in the database, by its start date.  If it
     * exists, copy its data into passageData.
     * 
     * @return              True if we found a newest passage and copied it
     *                      into passageData.  False if we didn't, and
     *                      passageData is unchanged.
     */
    public boolean loadNewestPassage() {
    	allPassagesCursor.requery();
    	if (!allPassagesCursor.moveToFirst())
    		return false;

    	loadPassage(allPassagesCursor);
    	return true;
    }


    /**
     * Find the default passage and load it.  This will be the open passage
     * if there is one, or else newest passage in the database, by its start
     * date.  If we find a passage, copy its data into passageData.
     * 
     * @return              True if we found a default passage and copied it
     *                      into passageData.  False if we didn't, and
     *                      passageData is unchanged.
     */
    public boolean loadDefaultPassage() {
        if (loadOpenPassage())
            return true;
        return loadNewestPassage();
    }


    /**
     * Load a passage from a given cursor.
     * 
     * @param   c           The Cursor to load from.
     * @return              True if we found the given passage and copied it
     *                      into passageData.  False if we didn't, and
     *                      passageData is unchanged.
     */
    private void loadPassage(Cursor c) {
        // Query for the number of points, and load the latest point.
        int ii = c.getColumnIndexOrThrow(PassageSchema.Passages._ID);
        long id = c.getLong(ii);
        Uri uri = ContentUris.withAppendedId(PassageSchema.Passages.CONTENT_URI, id);

        Cursor c2 =
        	contentResolver.query(uri,
        						  PassageSchema.Points.PROJECTION,
        						  null,
        						  null, PassageSchema.Points.TIME + " DESC");
        passageData = new PassageRecord(c, c2);
        passageUri = uri;
    }


    /**
     * Find the specified passage and load it into passageData.
     * 
     * @param   id          ID of the passage to load.
     * @return              True if we found the given passage and copied it
     *                      into passageData.  False if we didn't, and
     *                      passageData is unchanged.
     */
    private boolean loadPassage(long id) {
        Cursor c = null;
        Cursor c2 = null;
        boolean found = false;
        String[] idParam = new String[] { "" + id };

        try {
            Uri uri = ContentUris.withAppendedId(PassageSchema.Passages.CONTENT_URI, id);
            c = contentResolver.query(uri, PassageSchema.Passages.PROJECTION,
                                      null, null, null);
            if (c != null && c.moveToFirst()) {
            	for (String s: PassageSchema.Points.PROJECTION)
                    Log.w(TAG, "TP " + s);

                // Query for the number of points, and load the latest point.
                c2 = contentResolver.query(PassageSchema.Points.CONTENT_URI,
                                           PassageSchema.Points.PROJECTION,
                                           PassageSchema.Points.PASSAGE + "=?",
                                           idParam,
                                           PassageSchema.Points.TIME + " DESC");
                
                passageData = new PassageRecord(c, c2);
                passageUri = uri;
                found = true;
            }
        } finally {
            c.close();
            if (c2 != null)
            	c2.close();
        }

        return found;
    }


    /**
     * Select the indicated passage.
     * 
     * @param	id			ID of the passage to load.
     */
    private void selectPassage(long id) {
    	// Load and display the requested passage.
    	if (!loadPassage(id))
    	    throw new IllegalArgumentException("Loading invalid passage ID " + id);
    	showPassage();
    }


    /**
     * Create a new passage.
     */
    private void newPassage() {
        Uri uri = PassageSchema.Passages.CONTENT_URI;
        Intent intent = new Intent(Intent.ACTION_INSERT, uri);
        appContext.startActivity(intent);
    }


    /**
     * Edit the current passage.
     */
    private void editPassage() {
        if (passageData == null)
            return;
		Intent intent = new Intent(Intent.ACTION_EDIT, passageUri);
		appContext.startActivity(intent);
    }


    /**
     * Delete the currently-editing passage.
     */
    private void deletePassage() {
        if (passageData == null)
            return;
    	long id = passageData.getId();

        // Delete all points belonging to the passage.
        int count1 = contentResolver.delete(PassageSchema.Points.CONTENT_URI,
                                            PassageSchema.Points.PASSAGE + "=" + id,
                                            null);

        // Delete the passage record.
        int count2 = contentResolver.delete(passageUri, null, null);

        // FIXME: do we need this?
        // allPassagesCursor.requery();

        passageData = null;
        passageUri = null;
        showPassage();
    }


    /**
     * Start or finish the current passage.
     */
    private void startOrFinishPassage() {
    	if (passageData == null)
    		return;

    	if (!passageData.isRunning())
            startPassage();
    	else
    	    finishPassage();

    	showPassage();
    }


    /**
     * Start (or restart) the current passage.  Does nothing if there
     * is no current passage, or if it is already started.
     */
    public void startPassage() {
        if (passageData == null)
            return;

        Position pos = locationModel.getCurrentPos();
        long time = System.currentTimeMillis();

        passageData.startPassage(time, pos);

        // Add the starting point to the points log.  This will update
        // the database record for this passage.
        logPoint(pos, passageData.getStart(), time);

        // Notify the observers that we changed.
        // crewChanged();
    }


    /**
     * Finish the current passage.  Does nothing if there is no current
     * passage, or if it is not started or already finished.
     */
    public void finishPassage() {
        if (passageData == null || !passageData.isRunning())
            return;

        Position pos = locationModel.getCurrentPos();
        long time = System.currentTimeMillis();

        // Add the ending point to the points log.
        logPoint(pos, passageData.getDest(), time);
        passageData.finishPassage(time, pos);
        passageData.saveData(contentResolver, passageUri);
    }


    /**
     * Display the current passage.
     */
    private void showPassage() {
    	// Set the fields.
    	if (passageData != null) {
    		int status, button;
    		if (!passageData.isStarted()) {
    			status = R.string.lab_passage_not_started;
    			button = R.string.passage_start;
    		} else if (passageData.isRunning()) {
    			status = R.string.lab_passage_under_way;
    			button = R.string.passage_stop;
    		} else {
       			status = R.string.lab_passage_finished;
    			button = R.string.passage_start;
    		}
    		
    		Distance dist = passageData.getDistance();
    		String distStr = dist.describeNautical() +
    									" (" + passageData.getNumPoints() + ")";

    		startButton.setEnabled(true);
    		startButton.setText(button);
            startPlaceField.setText(passageData.getStart());
            endPlaceField.setText(passageData.getDest());
            statusDescField.setText(status);
    		statusAuxField.setText(distStr);
    	} else {
    		startButton.setEnabled(false);
    		startButton.setText(R.string.passage_start);
    		startPlaceField.setText("--");
    		endPlaceField.setText("--");
            statusDescField.setText(R.string.lab_no_passage);
        	statusAuxField.setText("");
    	}
    }


    // ******************************************************************** //
    // Track Management.
    // ******************************************************************** //

    /**
     * Add the given point to the track.
     * 
     * @param   pos         The point to log.
     * @param   name        A name for the point, if we have one; else null.
     * @param   time        Time in ms at which we arrived there.
     */
    private void logPoint(Position pos, String name, long time) {
        // Get the distance from the previous point.  Add this to the passage.
    	Distance dist = passageData.logPoint(pos);
        Log.i(TAG, "Passage point: dist=" + dist.formatM() +
                    " tot=" + passageData.getDistance().formatM());
        
        // Create a Point record, and add it to the database.
        ContentValues values = new ContentValues();
        values.put(PassageSchema.Points.PASSAGE, passageData.getId());
        values.put(PassageSchema.Points.NAME, name);
        values.put(PassageSchema.Points.TIME, time);
        if (pos != null) {
            values.put(PassageSchema.Points.LAT, pos.getLatDegs());
            values.put(PassageSchema.Points.LON, pos.getLonDegs());
        }
        values.put(PassageSchema.Points.DIST, dist.getMetres());
        values.put(PassageSchema.Points.TOT_DIST, passageData.getDistance().getMetres());
        contentResolver.insert(PassageSchema.Points.CONTENT_URI, values);
        
        // Update the database record for this passage.
        passageData.saveData(contentResolver, passageUri);
    }
    

	// ******************************************************************** //
	// Display.
	// ******************************************************************** //

    /**
     * Display the current date and time.
     */
    private void update() {
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
	
	// Our content resolver.
	private ContentResolver contentResolver;

    // The time and location models.
    private TimeModel timeModel;
    private LocationModel locationModel;

	// The passage model.
//	private PassageModel passageModel;

    // Cursor used to access the passage list.
    private Cursor allPassagesCursor = null;

    // Cursor which selects the currently running passage, if any.
    private Cursor openPassageCursor = null;

    // The passage selector widget.
    private Spinner passagePicker;
    
    // The data adapter used to map the passage list from the database onto
    // the passage picker.
    private SimpleCursorAdapter passageAdapter;

    // Fields for displaying the passage start and end, and current status.
	private TextView startPlaceField;
    private TextView startTimeField;
    private TextView endPlaceField;
    private TextView endTimeField;
	private TextView statusDescField;
    private TextView statusAuxField;
    
    // Control buttons.  We need to keep startButton to change its text.
	private Button startButton;

    // URI of the passage we're currently showing.  null if no passage.
    private Uri passageUri = null;
    
    // Information on the passage we're currently showing.  Null if no passage.
	private PassageRecord passageData = null;
	
}

