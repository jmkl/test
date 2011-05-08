
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


import org.hermit.android.core.AppUtils;
import org.hermit.android.core.MainActivity;
import org.hermit.android.notice.InfoBox;
import org.hermit.formula.provider.FormulaSchema;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;


/**
 * Main activity for Formula.
 */
public class FormulaRunner
	extends MainActivity
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
        
        // Get the URI of the formula we're running; this will be null
        // if we were started from the launcher.
        formulaUri = getIntent().getData();
        
        // Create the application GUI.
        setContentView(R.layout.formula_runner);
        
        // Get the formula view widget.
        formulaWidget = (FormulaView) findViewById(R.id.runner_view);

        // Create the EULA dialog.
        createEulaBox(R.string.eula_title, R.string.eula_text, R.string.button_close);

        // Create the dialog we use for help and about.
        AppUtils autils = AppUtils.getInstance(this);
        messageDialog = new InfoBox(this, R.string.button_close);
        String version = autils.getVersionString();
		messageDialog.setTitle(version);

        // Restore our preferences.
        updatePreferences();
    }
    

    /**
     * Called after {@link #onCreate} or {@link #onStop} when the current
     * activity is now being displayed to the user.  It will
     * be followed by {@link #onRestart}.
     */
    @Override
	protected void onStart() {
        Log.i(TAG, "onStart()");
        
        super.onStart();
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
        
        // First time round, show the EULA.
        showFirstEula();

        // If we have a URI, get a cursor on the formula.  If we don't have
        // one, then query for any formula, in order of when used,
        // so we get the most recent one.
        if (formulaUri != null)
        	formulaCursor = managedQuery(formulaUri, BODY_PROJECTION, null, null, null);
        else {
        	formulaCursor = managedQuery(FormulaSchema.Formulae.CONTENT_URI,
        								 BODY_PROJECTION,
        								 FormulaSchema.Formulae.VALID + ">0",
        								 null,
        								 FormulaSchema.Formulae.USED_DATE + " DESC");
        }

        // If we have a matching formula, display it.  Otherwise
        // display a hint.
        if (formulaCursor != null && formulaCursor.moveToFirst()) {
        	// If we don't have a URI, sort it out.
        	if (formulaUri == null) {
                int iindex = formulaCursor.getColumnIndex(FormulaSchema.Formulae._ID);
                long rowId = formulaCursor.getLong(iindex);
            	formulaUri = ContentUris.withAppendedId(FormulaSchema.Formulae.CONTENT_URI, rowId);
        	}

            // Get the formula and display it.
            int tindex = formulaCursor.getColumnIndex(FormulaSchema.Formulae.TITLE);
            String title = formulaCursor.getString(tindex);
            int findex = formulaCursor.getColumnIndex(FormulaSchema.Formulae.FORMULA);
            String text = formulaCursor.getString(findex);
            setFormula(title, text);
            
        	// Update the last-used date.
        	ContentValues values = new ContentValues();
        	values.put(FormulaSchema.Formulae.USED_DATE, System.currentTimeMillis());
        	getContentResolver().update(formulaUri, values, null, null);
        } else {
        	// TODO: inform the user.
            formulaWidget.clearFormula();
        }
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
	 * @param	outState		A Bundle in which to place any state
	 * 							information you wish to save.
     */
    @Override
    public void onSaveInstanceState(Bundle outState) {
        Log.i(TAG, "onSaveInstanceState()");
        
        super.onSaveInstanceState(outState);
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
        
        formulaCursor.close();
    }


    /**
     * Called when you are no longer visible to the user.  You will next
     * receive either {@link #onStart}, {@link #onDestroy}, or nothing,
     * depending on later user activity.
     * 
     * <p>Note that this method may never be called, in low memory situations
     * where the system does not have enough memory to keep your activity's
     * process running after its {@link #onPause} method is called.
     */
    @Override
	protected void onStop() {
        Log.i(TAG, "onStop()");
        
        super.onStop();
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
    	// We must call through to the base implementation.
    	super.onCreateOptionsMenu(menu);
    	
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);

        return true;
    }
    
    
    /**
     * Prepare the options menu to be displayed.  This is called right
     * before the menu is shown, every time it is shown.
     * 
     * @param	menu			The options menu as last shown or first
     * 							initialized by onCreateOptionsMenu().
     * @return					You must return true for the menu to be
     * 							displayed; if you return false it will
     * 							not be shown.
     */
    @Override
	public boolean onPrepareOptionsMenu(Menu menu) {
    	super.onPrepareOptionsMenu(menu);
    	
    	// Disable edit if we have nothing to edit.
    	MenuItem edit = menu.findItem(R.id.menu_edit);
    	edit.setEnabled(formulaUri != null);
    	 
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
    	switch (item.getItemId()) {
        case R.id.menu_select:
        	Intent pick = new Intent(Intent.ACTION_VIEW,
					   				 FormulaSchema.Formulae.CONTENT_URI);
    		startActivity(pick);
        	break;
        case R.id.menu_edit:
        	if (formulaUri != null) {
        		Intent edit = new Intent(Intent.ACTION_EDIT, formulaUri);
        		startActivity(edit);
        	}
        	break;
        case R.id.menu_new:
        	Intent insert = new Intent(Intent.ACTION_INSERT,
        							   FormulaSchema.Formulae.CONTENT_URI);
        	startActivity(insert);
        	break;
        case R.id.menu_prefs:
        	// Launch the preferences activity as a subactivity, so we
        	// know when it returns.
        	Intent pint = new Intent();
        	pint.setClass(this, Preferences.class);
        	startActivityForResult(pint, new MainActivity.ActivityListener() {
				@Override
				public void onActivityFinished(int resultCode, Intent data) {
		            updatePreferences();
				}
        	});
        	break;
    	case R.id.menu_help:
    		messageDialog.setLinkButton(1, R.string.button_homepage,
    								    R.string.url_homepage);
    		messageDialog.setLinkButton(2, R.string.button_manual,
    									R.string.url_manual);
 			messageDialog.show(R.string.help_text);
    		break;
    	case R.id.menu_about:
    		messageDialog.setLinkButton(1, R.string.button_homepage,
				    					R.string.url_homepage);
    		messageDialog.setLinkButton(2, R.string.button_license,
										R.string.url_license);
 			messageDialog.show(R.string.about_text);
     		break;
    	case R.id.menu_eula:
            showEula();
     		break;
        case R.id.menu_exit:
        	finish();
        	break;
    	default:
    		return super.onOptionsItemSelected(item);
    	}
    	
    	return true;
    }
    

    /**
     * Read our application preferences and configure ourself appropriately.
     */
    private void updatePreferences() {
    	SharedPreferences prefs =
    					PreferenceManager.getDefaultSharedPreferences(this);

    	shadowSd = false;
    	try {
    		shadowSd = prefs.getBoolean("shadowSd", false);
    	} catch (Exception e) {
    		Log.i(TAG, "Pref: bad shadowSd");
    	}
    	Log.i(TAG, "Prefs: shadowSd " + shadowSd);
    }


	// ******************************************************************** //
	// Formula Handling.
	// ******************************************************************** //

    private void setFormula(String title, String formula) {
    	String app = getString(R.string.app_name);
    	setTitle(app + ": " + title);
    	formulaWidget.setFormula(formula);
    }
    

	// ******************************************************************** //
	// Class Data.
	// ******************************************************************** //

    // Debugging tag.
	private static final String TAG = "formula";

    // Projection to select the text of a formula.
    private static final String[] BODY_PROJECTION = new String[] {
            FormulaSchema.Formulae._ID,
            FormulaSchema.Formulae.TITLE,
            FormulaSchema.Formulae.FORMULA,
    };

    
	// ******************************************************************** //
	// Private Data.
	// ******************************************************************** //

	// Dialog used to display about etc.
	private InfoBox messageDialog;

    // The URI, cursor and text of the current formula.  If we don't have
    // one, these will all be blank.
	private Uri formulaUri;
    private Cursor formulaCursor = null;
    
    // The formula view widget.
    private FormulaView formulaWidget = null;
    
    // If true, shadow all formulae to the SD card.
    private boolean shadowSd = false;

}

