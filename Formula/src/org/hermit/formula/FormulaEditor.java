
/**
 * Formula: programmable custom computations.
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


package org.hermit.formula;


import org.hermit.android.notice.ErrorDialog;
import org.hermit.android.notice.TextInputDialog;
import org.hermit.android.notice.YesNoDialog;
import org.hermit.formula.provider.FormulaSchema;

import android.app.Activity;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;


/**
 * The formula editing activity.  This can be used to edit a formula, or
 * to create a new one.
 */
public class FormulaEditor
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
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        // Set the layout for this activity.  You can find it in res/layout/note_editor.xml
        setContentView(R.layout.formula_editor);
        
        // Get the title widget, text view widget, and save button.
        textWidget = (EditText) findViewById(R.id.editor_text);
        saveButton = (Button) findViewById(R.id.editor_done_button);
        
        // Set a handler on the save button.
        saveButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				requestSave(true);
			}
        });
        
        // Create an input dialog and an error dialog.
        inputDialog = new TextInputDialog(this, R.string.button_ok,
        										R.string.button_cancel);
        invalidInputDialog = new YesNoDialog(this, R.string.editor_invalid_yes,
									  			   R.string.editor_invalid_no);
        errorDialog = new ErrorDialog(this, R.string.button_ok);
        
        // Set up our data.
        setupSubject();
        if (isFinishing())
        	return;

        // If an instance of this activity had previously stopped, restore
        // its previous state.  Otherwise initialise the widgets from the
        // database.
        String text;
        if (icicle != null) {
        	formulaTitle = icicle.getString("title");
        	text = icicle.getString("text");
        } else {
        	formulaCursor.moveToFirst();
        	int tindex = formulaCursor.getColumnIndex(FormulaSchema.Formulae.TITLE);
        	int findex = formulaCursor.getColumnIndex(FormulaSchema.Formulae.FORMULA);
        	formulaTitle = formulaCursor.getString(tindex);
        	text = formulaCursor.getString(findex);
        }
        if (formulaTitle == null)
        	formulaTitle = "";
        
        setWindowTitle();
        textWidget.setText(text);
    }
    
    
    private void setWindowTitle() {
        // Modify our overall title depending on the mode we are running in.
        CharSequence titleBase;
        if (editMode == Mode.EDIT)
        	titleBase = getText(R.string.editor_title_edit);
        else
        	titleBase = getText(R.string.editor_title_create);
        
    	setTitle(titleBase + ": " + formulaTitle);
    }

    
    private void setupSubject() {
        final Intent intent = getIntent();

        // Do some setup based on the action being performed.
        final String action = intent.getAction();
        if (Intent.ACTION_EDIT.equals(action)) {
            // Requested to edit: set that state, and the data being edited.
            editMode = Mode.EDIT;
            formulaUri = intent.getData();
        } else if (Intent.ACTION_INSERT.equals(action)) {
            // Requested to insert: set that state, and create a new entry
            // in the container.
            editMode = Mode.INSERT;
            formulaUri = getContentResolver().insert(intent.getData(), null);

            // If we were unable to create a new note, then just finish
            // this activity.  A RESULT_CANCELED will be sent back to the
            // original activity if they requested a result.
            if (formulaUri == null) {
            	errorDialog.show("Formula editor failed to insert the formula, exiting.");
                finish();
                return;
            }
        } else {
            // Whoops, unknown action!  Bail.
        	errorDialog.show("Formula editor invoked with an unknown action, exiting.");
            finish();
            return;
        }
        
        // Get a cursor on the formula's database entry.
        formulaCursor = managedQuery(formulaUri, BODY_PROJECTION, null, null, null);
        if (formulaCursor == null || formulaCursor.getCount() == 0) {
        	errorDialog.show("Formula editor failed to find the formula, exiting.");
            finish();
            return;
        }
    }


    /**
     * Called after onRestoreInstanceState(Bundle), onRestart(), or onPause(),
     * for your activity to start interacting with the user.
	 * 
	 * Derived classes must call through to the super class's implementation
	 * of this method.  If they do not, an exception will be thrown.
     */
    @Override
    protected void onResume() {
        super.onResume();
        
        if (formulaTitle == null || formulaTitle.length() == 0)
        	renameFormula(true);
    }
    

    /**
     * Called to retrieve per-instance state from an activity before being
     * killed so that the state can be restored in onCreate(Bundle) or
     * onRestoreInstanceState(Bundle) (the Bundle populated by this method
     * will be passed to both).
     * 
     * If called, this method will occur before onStop().  There are no
     * guarantees about whether it will occur before or after onPause().
	 * 
	 * @param	icicle			A Bundle in which to place any state
	 * 							information you wish to save.
     */
    @Override
    protected void onSaveInstanceState(Bundle icicle) {
        // Save away the edited title and text.
        icicle.putString("text", textWidget.getText().toString());
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
        super.onPause();
    }
    

	// ******************************************************************** //
    // Menu and Preferences Handling.
    // ******************************************************************** //

	/**
     * Initialize the contents of the options menu by adding items
     * to the given menu.
     * 
     * This is only called once, the first time the options menu is displayed.
     * To update the menu every time it is displayed, see
     * onPrepareOptionsMenu(Menu).
     * 
     * When we add items to the menu, we can either supply a Runnable to
     * receive notification of selection, or we can implement the Activity's
     * onOptionsItemSelected(Menu.Item) method to handle them there.
     * 
     * @param	menu			The options menu in which we should
     * 							place our items.  We can safely hold on this
     * 							(and any items created from it), making
     * 							modifications to it as desired, until the next
     * 							time onCreateOptionsMenu() is called.
     * @return					true for the menu to be displayed; false
     * 							to suppress showing it.
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        
        MenuInflater inflater = getMenuInflater();

        // Build the menus relevant to what we're doing.
        if (editMode == Mode.EDIT)
            inflater.inflate(R.menu.editor_edit_menu, menu);
        else
            inflater.inflate(R.menu.editor_create_menu, menu);

        // Generate any additional actions that can be performed on the
        // overall list.  In a normal install, there are no additional
        // actions found here, but this allows other applications to extend
        // our menu with their own actions.
        Intent intent = new Intent(null, getIntent().getData());
        intent.addCategory(Intent.CATEGORY_ALTERNATIVE);
        menu.addIntentOptions(Menu.CATEGORY_ALTERNATIVE, 0, 0,
                new ComponentName(this, FormulaList.class), null, intent, 0, null);

        return true;
    }

    
    /**
     * This hook is called whenever an item in your options menu is selected.
     * Derived classes should call through to the base class for it to
     * perform the default menu handling.  (True?)
     *
     * @param	item			The menu item that was selected.
     * @return					false to have the normal processing happen.
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle all of the possible menu actions.
    	switch (item.getItemId()) {
    	case R.id.menu_rename:
    		renameFormula(false);
    		break;
    	case R.id.menu_revert:
    		cancelFormula();
    		break;
    	case R.id.menu_delete:
    		deleteFormula();
    		finish();
    		break;
    	case R.id.menu_discard:
    		cancelFormula();
    		break;
    	default:
    		return super.onOptionsItemSelected(item);
    	}

    	return true;
    }


	// ******************************************************************** //
    // Formula Management.
    // ******************************************************************** //
    
    /**
     * Rename the formula.  Prompts the user for a new name.
     * 
     * @param	init			If true, this is the initial title; if false,
     * 							a rename.
     */
    private final void renameFormula(boolean init) {
    	// Show an input dialog to get the new name.  When the user presses
    	// OK, we proceed with the rename.  On cancel, nothing happens.
    	inputDialog.setOnOkListener(new TextInputDialog.OnOkListener() {
			@Override
			public void onOk(CharSequence title) {
				renameFormula(title);
			}
    	});
    	int tindex = formulaCursor.getColumnIndex(FormulaSchema.Formulae.TITLE);
    	String title = formulaCursor.getString(tindex);
    	int head = init ? R.string.editor_title_init_head :
    					  R.string.editor_title_head;
    	int prompt = init ? R.string.editor_title_init_prompt :
    						R.string.editor_title_prompt;
    	inputDialog.show(head, prompt, title);
    }

    
    /**
     * Rename the formula with a given new name.
     * 
     * @param	title			The new name for the formula.
     */
    private final void renameFormula(CharSequence title) {
        // Insist on a title.
        if (title.length() == 0) {
        	errorDialog.show("You must enter a valid title.");
            return;
        }

    	// Save the title and text.
        formulaTitle = title.toString();
    	ContentValues values = new ContentValues();
    	values.put(FormulaSchema.Formulae.TITLE, title.toString());
    	
    	// Commit all of our changes to persistent storage.  When the
    	// update completes the content provider will notify the cursor
    	// of the change, which will cause the UI to be updated.
    	getContentResolver().update(formulaUri, values, null, null);
    	
    	// Set the window title.
    	setWindowTitle();
    }
    
    
    /**
     * Try to save the formula.
     * 
     * @param	exit			If true, exit after saving successfully.
     */
    private final void requestSave(final boolean exit) {
    	// Get the program text.
    	String text = textWidget.getText().toString();

    	// Determine whether the program is valid.  If not, prompt the
    	// user whether to save anyway.
    	String error = Interpreter.verifyFormula(text);
    	if (error != null) {
    		invalidInputDialog.setOnOkListener(new YesNoDialog.OnOkListener() {
				@Override
				public void onOk() {
					doSave(exit);
				}
    		});
    		String head = getString(R.string.editor_invalid_title);
    		String msg = getString(R.string.editor_invalid_prompt1) +
    					 ": " + error + "\n" +
    					 getString(R.string.editor_invalid_prompt2);
    		invalidInputDialog.show(head, msg);
    	} else
    		doSave(exit);
    }
    
    
    /**
     * Save the formula as it is.
     * 
     * @param	exit			If true, exit after saving.
     */
    private final void doSave(boolean exit) {
    	// Get the program text.
    	String text = textWidget.getText().toString();

    	// Determine whether the program is valid.
    	boolean valid = Interpreter.verifyFormula(text) == null;
    	
    	// Save the text.
    	ContentValues values = new ContentValues();
    	values.put(FormulaSchema.Formulae.FORMULA, text);

    	// Bump the modification time to now.  Leave the used time alone.
    	values.put(FormulaSchema.Formulae.CREATED_DATE, System.currentTimeMillis());

    	// Save the "valid" flag.
    	values.put(FormulaSchema.Formulae.VALID, valid);

    	// Commit all of our changes to persistent storage. When the update completes
    	// the content provider will notify the cursor of the change, which will
    	// cause the UI to be updated.
    	getContentResolver().update(formulaUri, values, null, null);
    	
    	if (exit)
    		finish();
    }


    /**
     * Take care of cancelling work on a formula.  Deletes the formula if we
     * had created it, otherwise reverts to the original text.
     */
    private final void cancelFormula() {
        if (formulaCursor != null) {
            if (editMode == Mode.EDIT) {
                // Put the original note text back into the database
                formulaCursor.close();
                formulaCursor = null;
                ContentValues values = new ContentValues();
//                values.put(Formula.Formulae.FORMULA, mOriginalContent);
                getContentResolver().update(formulaUri, values, null, null);
            } else if (editMode == Mode.INSERT) {
                // We inserted an empty note, make sure to delete it
                deleteFormula();
            }
        }
        setResult(RESULT_CANCELED);
        finish();
    }

    
    /**
     * Take care of deleting a formula.  Simply deletes the entry.
     */
    private final void deleteFormula() {
        if (formulaCursor != null) {
            formulaCursor.close();
            formulaCursor = null;
            getContentResolver().delete(formulaUri, null, null);
            textWidget.setText("");
        }
    }

    
	// ******************************************************************** //
	// Class Data.
	// ******************************************************************** //

    // Debugging tag.
	@SuppressWarnings("unused")
	private static final String TAG = "formula";

    // The different distinct states the activity can be run in.
    private enum Mode {
    	EDIT, INSERT;
    }

    // Projection to select the text of a formula.
    private static final String[] BODY_PROJECTION = new String[] {
            FormulaSchema.Formulae._ID,
            FormulaSchema.Formulae.TITLE,
            FormulaSchema.Formulae.FORMULA,
    };
    
    
	// ******************************************************************** //
	// Private Data.
	// ******************************************************************** //

    // The current mode of this widget.
    private Mode editMode;
    
    // The URI of the formula we're editing, and a cursor on its
    // database entry.
    private Uri formulaUri;
    private Cursor formulaCursor;
    
    // The title input field, text input field, and done button.
    private EditText textWidget;
    private Button saveButton;
    
    // Dialog for text input.
    private TextInputDialog inputDialog;

    // Dialog for error messages.
    private YesNoDialog invalidInputDialog;

    // Dialog for error messages.
    private ErrorDialog errorDialog;

    // Current title for the formula.
    private String formulaTitle = "";
    
}

