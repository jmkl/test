
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


import org.hermit.provider.PassageData;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;


/**
 * This class is the activity for the crew editor.
 */
public class PassageEditor
	extends Activity
{

	// ******************************************************************** //
    // Activity Lifecycle.
    // ******************************************************************** //

	/**
	 * Called when the activity is starting.  This is where most
	 * initialization should go: calling setContentView(int) to inflate
	 * the activity's UI, etc.
	 * 
	 * You can call finish() from within this function, in which case
	 * onDestroy() will be immediately called without any of the rest of
	 * the activity lifecycle executing.
	 * 
	 * Derived classes must call through to the super class's implementation
	 * of this method.  If they do not, an exception will be thrown.
	 * 
	 * @param	icicle			If the activity is being re-initialized
	 * 							after previously being shut down then this
	 * 							Bundle contains the data it most recently
	 * 							supplied in onSaveInstanceState(Bundle).
	 * 							Note: Otherwise it is null.
	 */
    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        // Figure out what we're being asked to do here.
        final Intent intent = getIntent();
        final String action = intent.getAction();
        if (action == null) {
            Log.e(TAG, "No action specified, exiting");
            finish();
            return;
        }
        final Uri uri = intent.getData();
        if (uri == null) {
            Log.e(TAG, "No URI specified, exiting");
            finish();
            return;
        }
        
        if (action.equals(Intent.ACTION_EDIT)) {
            // Requested to edit: set that state, and the data being edited.
            inserting = false;
            passageUri = uri;
        } else if (action.equals(Intent.ACTION_INSERT)) {
            // Requested to insert: set that state, and create a new entry
            // in the container.
            inserting = true;
            passageUri = getContentResolver().insert(uri, null);

            // If we were unable to create a new note, then just finish
            // this activity.  A RESULT_CANCELED will be sent back to the
            // original activity if they requested a result.
            if (passageUri == null) {
                Log.e(TAG, "Failed to insert new passage into " + uri);
                finish();
                return;
            }

            // The new entry was created, so assume all will end well and
            // set the result to be returned.
            Intent result = new Intent();
            result.setAction(passageUri.toString());
            setResult(RESULT_OK, result);
        } else {
            Log.e(TAG, "Unknown action \"" + action + "\", exiting");
            finish();
            return;
        }

        // Create the application GUI.
        setupGui();

        // Get the passage we're editing.
        passageCursor = managedQuery(passageUri, PROJECTION, null, null, null);
    }
    
    
    /**
     * Called after onRestoreInstanceState(Bundle), onRestart(), or onPause(),
     * for your activity to start interacting with the user.  This is a good
     * place to begin animations, open exclusive-access devices (such as the
     * camera), etc.
	 * 
	 * Derived classes must call through to the super class's implementation
	 * of this method.  If they do not, an exception will be thrown.
     */
    @Override
    protected void onResume() {
        Log.i(TAG, "onResume()");
        
        super.onResume();
        
        showPassage();
    }


    /**
     * Called as part of the activity lifecycle when an activity is going
     * into the background, but has not (yet) been killed.  The counterpart
     * to onResume(). 
     * 
     * After receiving this call you will usually receive a following call
     * to onStop() (after the next activity has been resumed and displayed),
     * however in some cases there will be a direct call back to onResume()
     * without going through the stopped state. 
	 * 
	 * Derived classes must call through to the super class's implementation
	 * of this method.  If they do not, an exception will be thrown.
     */
    @Override
    protected void onPause() {
        Log.i(TAG, "onPause()");
        
        super.onPause();
        
        savePassage();
    }

    
    /**
     * Perform any final cleanup before an activity is destroyed.  This
     * can happen either because the activity is finishing (someone called
     * finish() on it, or because the system is temporarily destroying this
     * instance of the activity to save space. You can distinguish between
     * these two scenarios with the isFinishing() method.
     * 
     * Note: do not count on this method being called as a place for saving
     * data!  For example, if an activity is editing data in a content
     * provider, those edits should be committed in either onPause()
     * or onSaveInstanceState(Bundle), not here.  This method is usually
     * implemented to free resources like threads that are associated
     * with an activity, so that a destroyed activity does not leave such
     * things around while the rest of its application is still running.
     * There are situations where the system will simply kill the activity's
     * hosting process without calling this method (or any others) in it,
     * so it should not be used to do things that are intended to remain
     * around after the process goes away.
     * 
     * Derived classes must call through to the super class's implementation
     * of this method.  If they do not, an exception will be thrown.
     */
    @Override
    protected void onDestroy() {
        Log.i(TAG, "onDestroy()");
        
        super.onDestroy();
    }
    

	// ******************************************************************** //
	// User Interface.
	// ******************************************************************** //
    
    /**
     * Set up the user interface.
     */
    private void setupGui() {
        setContentView(R.layout.passage_editor);

        // Get the data fields.
        nameField = (EditText) findViewById(R.id.passage_name_field);
        fromField = (EditText) findViewById(R.id.passage_from_field);
        toField = (EditText) findViewById(R.id.passage_to_field);

        // Set up the handlers for the control buttons.
        Button done = (Button) findViewById(R.id.passage_done_button);
        done.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View arg0) {
                close();
            }
        });
    }


    // ******************************************************************** //
    // Passage Data Management.
    // ******************************************************************** //

    /**
     * Save the passage data in the editor.
     */
    private void savePassage() {
        // The user is going somewhere else, so make sure their current
        // changes are safely saved away in the provider.  We don't need
        // to do this if only editing.
    	if (passageCursor != null) {
            String name = nameField.getText().toString();
            String from = fromField.getText().toString();
            String to = toField.getText().toString();

    	    // If we are creating a new passage, then we want to also create
    	    // an initial title for it, if there isn't one.
    	    if (inserting && TextUtils.isEmpty(name)) {
    	        if (!TextUtils.isEmpty(from) && !TextUtils.isEmpty(to))
    	            name = from + " to " + to;
    	    }

    	    // Write the passage back into the provider.
    	    ContentValues values = new ContentValues();
    	    values.put(PassageData.Passages.NAME, name);
    	    values.put(PassageData.Passages.START_NAME, from);
    	    values.put(PassageData.Passages.DEST_NAME, to);

    	    // Commit all of our changes to persistent storage.  When the
    	    // update completes the content provider will notify the
    	    // cursor of the change, which will cause the UI to be updated.
    	    getContentResolver().update(passageUri, values, null, null);
    	}
    }


    /**
     * Edit the indicated passage.
     * 
     * @param	pd			The passage to edit.
     */
    private void showPassage() {
        // If we didn't have any trouble retrieving the data, it is now
        // time to get at the stuff.
        if (passageCursor != null) {
            // Make sure we are at the one and only row in the cursor.
            passageCursor.moveToFirst();

            // Modify our overall title depending on the mode we are running in.
            if (!inserting)
                setTitle(getText(R.string.passage_edit_title));
            else
                setTitle(getText(R.string.passage_new_title));

            // Set up the editing fields.
            String name = passageCursor.getString(COLUMN_NAME);
            nameField.setText(name);
            String from = passageCursor.getString(COLUMN_START_NAME);
            fromField.setText(from);
            String dest = passageCursor.getString(COLUMN_DEST_NAME);
            toField.setText(dest);
        } else {
            setTitle(getText(R.string.passage_error_title));
        }
    }


    /**
     * Close the editor.
     */
    private void close() {
        savePassage();
    	passageCursor.close();
    	finish();
    }


	// ******************************************************************** //
	// Class Data.
	// ******************************************************************** //

    // Debugging tag.
	@SuppressWarnings("unused")
	private static final String TAG = "PassageEditor";

    // Standard projection for reading a passage.
    private static final String[] PROJECTION = new String[] {
        PassageData.Passages._ID,
        PassageData.Passages.NAME,
        PassageData.Passages.START_NAME,
        PassageData.Passages.DEST_NAME,
    };
    
    // The indices of the columns in the projection.
    private static final int COLUMN_NAME = 1;
    private static final int COLUMN_START_NAME = 2;
    private static final int COLUMN_DEST_NAME = 3;


	// ******************************************************************** //
	// Private Data.
	// ******************************************************************** //

    // Input field widgets.
    private EditText nameField;
    private EditText fromField;
    private EditText toField;
    
    // URI of the passage we're currently editing.
    private Uri passageUri;

    // Cursor used to access the passage list.
    private Cursor passageCursor = null;
    
    // Flag if we are inserting a new passage, as opposed to editing an
    // existing one.
    private boolean inserting = false;

}

