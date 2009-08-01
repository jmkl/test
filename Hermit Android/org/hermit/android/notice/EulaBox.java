
/**
 * org.hermit.android.notice: various notice dialogs for Android.
 *
 *   This program is free software; you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License version 2
 *   as published by the Free Software Foundation (see COPYING).
 * 
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 */


package org.hermit.android.notice;

import org.hermit.android.core.AppUtils;

import android.app.Activity;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;


/**
 * This class implements a popup info box (a subclass of AlertDialog)
 * which displays a license or disclaimer when the program first runs.
 * 
 * Note: this makes use of a preference to track whether it has been
 * shown, so there can only be one EulaBox per app.
 */
public class EulaBox
	extends InfoBox
{
	
	// ******************************************************************** //
	// Constructor.
	// ******************************************************************** //

	/**
	 * Create an info box.
	 * 
	 * @param parent		Parent application context.
	 * @param title			Resource ID of the dialog title.
	 * @param text			Resource ID of the main message text.
	 * @param button		Resource ID of the text for the OK button.
	 */
	public EulaBox(Activity parent, int title, int text, int button) {
		super(parent, button);
		
		eulaTitle = title;
		messageText = text;
		
		appPrefs = PreferenceManager.getDefaultSharedPreferences(parent);
        appUtils = AppUtils.getInstance(parent);
	}

 
    // ******************************************************************** //
    // Dialog control.
    // ******************************************************************** //

    /**
     * If the user has not seen this dialog for the current version of the
     * app, then start the dialog and display it on screen.  Once the user
     * accepts the dialog, it will not be shown again until a new version of
     * the app is installed.
     * 
     * Note: this makes use of a preference to track whether it has been
     * shown, so there can only be one EulaBox per app.
     */
	public void showFirstTime() {
		if (!accepted())
			show(eulaTitle, messageText);
    }
    

    /**
     * Unconditionally start the dialog and display it on screen.
     * Once the user accepts the dialog, showFirstTime() will not show
     * it again until a new version of the app is installed.
     * 
     * Note: this makes use of a preference to track whether it has been
     * shown, so there can only be one EulaBox per app.
     */
	public void showNow() {
		show(eulaTitle, messageText);
    }
    

    /**
     * Query whether this dialog has been shown to the user and accepted.
     * 
     * @return				True iff the user has seen this dialog and
     * 						clicked "OK".
     */
    public boolean accepted() {
    	AppUtils.Version version = appUtils.getAppVersion();
    	
    	int seen = -1;
    	try {
    		seen = appPrefs.getInt(PREF_NAME, seen);
    	} catch (Exception e) { }
    	
    	// We consider the EULA accepted if the version seen by the user
    	// most recently is the current version.
    	return seen == version.versionCode;
    }


    /**
     * Flag that this dialog has been seen, by setting a preference.
     */
    private void setSeen() {
    	AppUtils.Version version = appUtils.getAppVersion();
    	
        SharedPreferences.Editor editor = appPrefs.edit();
        editor.putInt(PREF_NAME, version.versionCode);
        editor.commit();
    }
    

    // ******************************************************************** //
    // Input Handling.
    // ******************************************************************** //

    /**
     * Called when the OK button is clicked.
     */
    @Override
	void okButtonPressed() {
    	setSeen();
    	super.okButtonPressed();
    }
    

	// ******************************************************************** //
	// Private Constants.
	// ******************************************************************** //

    // Name of the preference used to flag that this dialog has been seen.
    private static final String PREF_NAME = "org.hermit.android.notice.eulaVersion";
    
    
	// ******************************************************************** //
	// Private Data.
	// ******************************************************************** //
    
	// App utilities for this app.  Used to get the version.
	private AppUtils appUtils;
	
	// Dialog title text resource ID.
	private int eulaTitle;

	// Message text resource ID.
	private int messageText;
	
	// Application's default shared preferences.
	private SharedPreferences appPrefs = null;

}

