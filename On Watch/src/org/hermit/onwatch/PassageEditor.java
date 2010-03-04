
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

import android.app.Activity;
import android.database.Cursor;
import android.os.Bundle;
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

        // Create the application GUI.
        setContentView(R.layout.passage_editor);

        // Get or create the passage model.
		passageModel = PassageModel.getInstance(this);
        passageModel.open();
        passageCursor = passageModel.getPassageCursor();

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
        
        passageModel.close();
    }
    

	// ******************************************************************** //
	// Passage Data Management.
	// ******************************************************************** //

    /**
     * Save the passage data in the editor.
     */
    private void savePassage() {
        String name = nameField.getText().toString();
        String from = fromField.getText().toString();
        String to = toField.getText().toString();
    	if (passageData == null) {
			Log.i(TAG, "PEdit: new: " + name);
    		passageData = new PassageData(name, from, to, null);
    	} else {
			Log.i(TAG, "PEdit: Update: " + passageData.name + " -> " + name);
    		passageData.name = name;
    		passageData.start = from;
    		passageData.dest = to;
    	}
        passageModel.savePassage(passageData);
    	passageCursor.requery();
    }


    /**
     * Edit the indicated passage.
     * 
     * @param	pd			The passage to edit.
     */
    private void showPassage(PassageData pd) {
    	passageData = pd;
    	
    	// Set the fields.
        nameField.setText(passageData.name);
        fromField.setText(passageData.start);
        toField.setText(passageData.dest);
    }


    /**
     * Close the editor.
     */
    private void close() {
    	// Save the current passage, if any.
    	if (passageData != null)
    		savePassage();
    	
    	passageCursor.close();
    	finish();
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
	
	// The passage model.
	private PassageModel passageModel;

    // Cursor used to access the passage list.
    private Cursor passageCursor = null;

    // Input field widgets.
    private EditText nameField;
    private EditText fromField;
    private EditText toField;
    
    // Information on the passage we're currently editing.  Null if no passage.
	private PassageData passageData = null;

}

