
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


import org.hermit.astro.Instant;
import org.hermit.geo.Distance;
import org.hermit.geo.Position;
import org.hermit.onwatch.LocationModel.GpsState;
import org.hermit.onwatch.TimeModel.Field;
import org.hermit.onwatch.provider.PassageSchema;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.net.Uri;
import android.provider.BaseColumns;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.SimpleCursorAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.AdapterView.OnItemSelectedListener;


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
public class PassageController
	extends OnWatchController
{

    // ******************************************************************** //
    // Public Classes.
    // ******************************************************************** //
    
    /**
     * Record of data on a particular passage.  This class encapsulates
     * all the data on a passage, and provides methods to convert
     * to and from database records.
     */
    public static final class PassageRecord {
        
        PassageRecord(String name, String from, String to, Position dest) {
            rowValues = new ContentValues();
            rowValues.put(PassageSchema.Passages.NAME, name);
            rowValues.put(PassageSchema.Passages.START_NAME, from);
            rowValues.put(PassageSchema.Passages.DEST_NAME, to);
            rowValues.put(PassageSchema.Passages.DEST_LAT, dest.getLatDegs());
            rowValues.put(PassageSchema.Passages.DEST_LON, dest.getLonDegs());
            rowValues.put(PassageSchema.Passages.DISTANCE, 0.0);
            
            init();
        }
        
        private PassageRecord() {
            rowValues = new ContentValues();
            
            init();
        }
        
        PassageRecord(Cursor c) {
            rowValues = new ContentValues();
            DatabaseUtils.cursorRowToContentValues(c, rowValues);
            
            init();
        }
        
        private void init() {
            Double dist = rowValues.getAsDouble(PassageSchema.Passages.DISTANCE);
            if (dist == null || dist == 0)
                distance = Distance.ZERO;
            else
                distance = new Distance(dist);
            
            // Get the start pos if present.
            Double slat = rowValues.getAsDouble(PassageSchema.Passages.START_LAT);
            Double slon = rowValues.getAsDouble(PassageSchema.Passages.START_LON);
            if (slat == null || slon == null)
                startPos = null;
            else
                startPos = Position.fromDegrees(slat, slon);
            
            // Get the end pos if present.
            Double elat = rowValues.getAsDouble(PassageSchema.Passages.DEST_LAT);
            Double elon = rowValues.getAsDouble(PassageSchema.Passages.DEST_LON);
            if (elat == null || elon == null)
                destPos = null;
            else
                destPos = Position.fromDegrees(elat, elon);
            
            // See if we have start or end times.
            Long stime = rowValues.getAsLong(PassageSchema.Passages.START_TIME);
            if (stime == null || stime == 0)
                startTime = null;
            else
                startTime = new Instant(stime);
            Long etime = rowValues.getAsLong(PassageSchema.Passages.DEST_TIME);
            if (etime == null || etime == 0)
                destTime = null;
            else
                destTime = new Instant(etime);
        }

        /**
         * Save the contents of this row to the given ContentValues.
         * 
         * @param   values          Object to write to.
         */
        void getValues(ContentValues values) {
            values.putAll(rowValues);
        }
        
        boolean isStarted() {
            return startTime != null;
        }
        
        boolean isRunning() {
            return startTime != null && destTime == null;
        }
        
        boolean isFinished() {
            return destTime != null;
        }
        
        // The passage ID, name, from point name, and to name.
        private long id = -1;
        String name = null;
        String start = null;
        String dest = null;

        // The Java time and the Julian day number on which the current passage
        // started and finished.  0 if not set.
        Instant startTime = null;
        Instant destTime = null;
        
        // The start and end points of this passage.  Null if not set.
        Position startPos = null;
        Position destPos = null;
        
        // Distance in metres covered on this passage (to date).
        Distance distance = Distance.ZERO;
        
        // Number of points recorded for this passage.  (Not saved in DB.)
        int numPoints = 0;
        
        // Most recently recorded position.  (Not saved in DB.)
        Position lastPos = null;
        
        // The values of the fields in this row.
        private final ContentValues rowValues;
    }
    

	// ******************************************************************** //
    // Constructors.
    // ******************************************************************** //
	
	/**
	 * Create a watch clock.
	 * 
	 * @param	context			Parent application.
	 */
	public PassageController(OnWatch context) {
		super(context);
		
		appContext = context;
		contentResolver = appContext.getContentResolver();
		
        // Perform a managed query to get the passage list from the content
		// provider.  The Activity will handle closing and requerying the
		// cursor when needed.
		allPassagesCursor = context.managedQuery(PassageSchema.Passages.CONTENT_URI,
		                                         PassageSchema.Passages.PROJECTION,
		                                         null, null,
		                                         PassageSchema.Passages.START_TIME + " DESC");

        // Perform a managed query to get the currently running passage,
		// if any, from the content provider.
        openPassageCursor = context.managedQuery(PassageSchema.Passages.CONTENT_URI,
                                                 PassageSchema.Passages.PROJECTION,
                                                 "? != 0",
                                                 new String[] { PassageSchema.Passages.UNDER_WAY },
                                                 PassageSchema.Passages.SORT_ORDER);
        
		// Get the passage selector widget.  Give it an adapter which maps
		// on to the passage names list.
		passagePicker = (Spinner) context.findViewById(R.id.passage_picker);
		passageAdapter =
                    new SimpleCursorAdapter(context,
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
        startPlaceField = (TextView) context.findViewById(R.id.pass_start_place);
        startTimeField = (TextView) context.findViewById(R.id.pass_start_time);
        endPlaceField = (TextView) context.findViewById(R.id.pass_end_place);
        endTimeField = (TextView) context.findViewById(R.id.pass_end_time);
		statusDescField = (TextView) context.findViewById(R.id.pass_stat_desc);
		statusAuxField = (TextView) context.findViewById(R.id.pass_stat_aux);
		
        Button newButton = (Button) context.findViewById(R.id.passage_new_button);
        newButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View arg0) {
                newPassage();
            }
        });
        
        // Add the handler to the edit button.
        Button editButton = (Button) context.findViewById(R.id.passage_edit_button);
        editButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View arg0) {
                editPassage();
            }
        });

        Button deleteButton = (Button) context.findViewById(R.id.passage_delete_button);
        deleteButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View arg0) {
                deletePassage();
            }
        });

        startButton = (Button) context.findViewById(R.id.passage_start_button);
		startButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
			    startOrFinishPassage();
			}
		});
        
        // Get the time model.  Ask it to ping us each bell.
        timeModel = TimeModel.getInstance(context);
        timeModel.listen(TimeModel.Field.MINUTE, new TimeModel.Listener() {
            @Override
            public void change(Field field, int value, long time) {
                // FIXME: do we need this?
            }
        });

        // Get our location model.  Ask it to keep us up to date.
        locationModel = LocationModel.getInstance(context);
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
        if (openPassageCursor.moveToFirst()) {
            passageData = new PassageRecord(openPassageCursor);
            return true;
        }

        return false;
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
        // FIXME: do we need this?
        // allPassagesCursor.requery();
        
        if (allPassagesCursor.moveToFirst()) {
            passageData = new PassageRecord(allPassagesCursor);
            return true;
        }

        return false;
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
        
        try {
            c = contentResolver.query(PassageSchema.Passages.CONTENT_URI,
                                      PassageSchema.Passages.PROJECTION,
                                      BaseColumns._ID + "=" + id, null, null);
            if (c != null && c.moveToFirst()) {
                passageData = new PassageRecord(c);
                
                // Query for the number of points, and load the latest point.
                c2 = contentResolver.query(PassageSchema.Points.CONTENT_URI,
                                           PassageSchema.Points.PROJECTION,
                                           PassageSchema.Points.PASSAGE + "=" + id,
                                           null, PassageSchema.Points.TIME + " DESC");
                if (c2 != null) {
                    passageData.put("num_points", c2.getCount());
                    if (c2.moveToFirst()) {
                        passagePointData.clear();
                        DatabaseUtils.cursorRowToContentValues(c2, passagePointData);
                    }
                }
                passageId = id;
                found = true;
            }
        } finally {
            c.close();
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
        Uri uri = ContentUris.withAppendedId(PassageSchema.Passages.CONTENT_URI, passageId);
		Intent intent = new Intent(Intent.ACTION_EDIT, uri);
		appContext.startActivity(intent);
    }


    /**
     * Delete the currently-editing passage.
     */
    private void deletePassage() {
        if (passageId < 0)
            return;
        
        // Delete all points belonging to the passage.
        int count1 = contentResolver.delete(PassageSchema.Points.CONTENT_URI,
                                            PassageSchema.Points.PASSAGE + "=" + passageId,
                                            null);
  
        // Delete the passage record.
        int count2 = contentResolver.delete(PassageSchema.Passages.CONTENT_URI,
                                            BaseColumns._ID + "=" + passageId,
                                            null);
  
        // FIXME: do we need this?
//        allPassagesCursor.requery();

        passageId = 0;
        passageData = null;
        showPassage();
    }


    /**
     * Start or finish the current passage.
     */
    private void startOrFinishPassage() {
    	if (passageData == null)
    		return;

    	int running = passageData.getAsInteger(PassageSchema.Passages.UNDER_WAY);
    	if (running == 0)
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

        passageData.put(PassageSchema.Passages.START_LAT, pos.getLatDegs());
        passageData.put(PassageSchema.Passages.START_LON, pos.getLonDegs());
        passageData.put(PassageSchema.Passages.START_TIME, time);
        passageData.put(PassageSchema.Passages.DISTANCE, 0l);
        contentResolver.update(passageUri, passageData, null, null);

        // Add the starting point to the points log.  This will update
        // the database record for this passage.
        logPoint(pos, passageData.start, time);

        // Notify the observers that we changed.
        // crewChanged();
    }


    /**
     * Finish the current passage.  Does nothing if there is no current
     * passage, or if it is not started or already finished.
     */
    public void finishPassage() {
        if (passageData == null)
            return;
        int running = passageData.getAsInteger(PassageSchema.Passages.UNDER_WAY);
        if (running == 0)
            return;

        Position pos = locationModel.getCurrentPos();
        long time = System.currentTimeMillis();

        // Add the ending point to the points log.
        logPoint(pos, passageData.dest, time);

        passageData.put(PassageSchema.Passages.DEST_LAT, pos.getLatDegs());
        passageData.put(PassageSchema.Passages.DEST_LON, pos.getLonDegs());
        passageData.put(PassageSchema.Passages.DEST_TIME, time);
        contentResolver.update(passageUri, passageData, null, null);
    }


    /**
     * Display the current passage.
     */
    private void showPassage() {
    	// Set the fields.
    	if (passageData != null) {
    		int status, button;
    		if (!passageData.containsKey(PassageSchema.Passages.START_TIME)) {
    			status = R.string.lab_passage_not_started;
    			button = R.string.passage_start;
    		} else if (!passageData.containsKey(PassageSchema.Passages.DEST_TIME)) {
    			status = R.string.lab_passage_under_way;
    			button = R.string.passage_stop;
    		} else {
       			status = R.string.lab_passage_finished;
    			button = R.string.passage_start;
    		}
    		
    		Distance dist = new Distance(passageData.getAsLong(PassageSchema.Passages.DISTANCE));
    		String distStr = dist.describeNautical() +
    							" (" + passageData.getAsInteger("num_points") + ")";

    		startButton.setEnabled(true);
    		startButton.setText(button);
            startPlaceField.setText(passageData.getAsString(PassageSchema.Passages.START_NAME));
            endPlaceField.setText(passageData.getAsString(PassageSchema.Passages.DEST_NAME));
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
        Distance dist = Distance.ZERO;
        if (passagePointData != null && pos != null) {
            dist = pos.distance(passageData.lastPos);
            passageData.distance = passageData.distance.add(dist);
        }
        passageData.lastPos = pos;
        ++passageData.numPoints;
        Log.i(TAG, "Passage point: dist=" + dist.formatM() +
                    " tot=" + passageData.distance.formatM());
        
        // Create a Point record, and add it to the database.
        ContentValues values = new ContentValues();
        values.put(PassageSchema.Points.PASSAGE, passageId);
        values.put(PassageSchema.Points.NAME, name);
        values.put(PassageSchema.Points.TIME, time);
        if (pos != null) {
            values.put(PassageSchema.Points.LAT, pos.getLatDegs());
            values.put(PassageSchema.Points.LON, pos.getLonDegs());
        }
        values.put(PassageSchema.Points.DIST, dist.getMetres());
        values.put(PassageSchema.Points.TOT_DIST, passageData.getAsLong(PassageSchema.Passages.DISTANCE));
        contentResolver.insert(PassageSchema.Points.CONTENT_URI, values);
        
        // Update the database record for this passage.
        contentResolver.update(passageUri, passageData, null, null);
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
	@SuppressWarnings("unused")
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
    
	// ID of the passage we're currently showing.  -1 if no passage.
	private long passageId = -1;

    // URI of the passage we're currently showing.  null if no passage.
    private Uri passageUri = null;
    
    // Information on the passage we're currently showing.  Null if no passage.
	private PassageRecord passageData = null;
	
	// The last point of the current passage.  Null if no passage,
	// or if it has no points.
	private ContentValues passagePointData = null;
	
}

