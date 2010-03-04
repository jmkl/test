
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


import org.hermit.onwatch.PassageModel.PassageData;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
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
		passageModel = PassageModel.getInstance(context);
        passageModel.open();
        passageCursor = passageModel.getPassageCursor();

        // Get the passage selector widget.  Give it an adapter which maps
        // on to the passage names list.
        passagePicker = (Spinner) context.findViewById(R.id.passage_picker);
        passageAdapter = new SimpleCursorAdapter(context,
        							android.R.layout.simple_spinner_item,
        							passageCursor,
        							new String[] { "name" },
        							new int[] { android.R.id.text1 });
        passageAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        passagePicker.setAdapter(passageAdapter);

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
				startPassage();
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
     * Select the indicated passage.
     * 
     * @param	id			ID of the passage to load.
     */
    private void selectPassage(long id) {
    	// Load and display the requested passage.
    	passageData = passageModel.selectPassage(id);
    	showPassage();
    }


    /**
     * Create a new passage.
     */
    private void newPassage() {
        // Save the current passage, if any.
        if (passageData != null) {
            savePassage();
            passageData = null;
        }
        
        // Clear the fields and let the user start typing.  Save will
        // create the new passage.
        nameField.setText("");
        fromField.setText("");
        toField.setText("");
    }


    /**
     * Edit the current passage.
     */
    private void editPassage() {
		Intent intent = new Intent();
		intent.setClass(appContext, PassageEditor.class);
		appContext.startActivity(intent);
    }


    /**
     * Delete the currently-editing passage.
     */
    private void deletePassage() {
        if (passageData == null)
            return;
        
        passageModel.deletePassage(passageData);
        passageCursor.requery();

        // Clear the fields.
        nameField.setText("");
        fromField.setText("");
        toField.setText("");
     }


    /**
     * Start or finish the current passage.
     */
    private void startPassage() {
    	if (passageData == null)
    		return;

    	if (passageData.isRunning())
    		passageModel.finishPassage();
    	else
    		passageModel.startPassage();

    	showPassage();
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
    		} else if (!passageData.isFinished()) {
    			status = R.string.lab_passage_under_way;
    			button = R.string.passage_stop;
    		} else {
       			status = R.string.lab_passage_finished;
    			button = R.string.passage_start;
    		}
    		
    		String dist = passageData.distance.describeNautical() +
    							" (" + passageData.numPoints + ")";

    		startButton.setEnabled(true);
    		startButton.setText(button);
            startPlaceField.setText(passageData.start);
            endPlaceField.setText(passageData.dest);
            statusDescField.setText(status);
    		statusAuxField.setText(dist);
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
	private Context appContext;
	
	// The passage model.
	private PassageModel passageModel;

    // Cursor used to access the passage list.
    private Cursor passageCursor = null;

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
    
    // Information on the passage we're currently editing.  Null if no passage.
	private PassageData passageData = null;

}

