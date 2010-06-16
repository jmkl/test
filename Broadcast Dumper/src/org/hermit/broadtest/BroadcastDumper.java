
/**
 * broadtest: system broadcast dumper.
 * <br>Copyright 2010 Ian Cameron Smith
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


package org.hermit.broadtest;


import org.hermit.broadtest.provider.BroadcastSchema;

import android.app.ListActivity;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.SimpleCursorAdapter;


/**
 * Mian broadcast dumper activity.  This class is a simple ListActivity
 * which displays broadcast events provided by the content provider.
 */
public class BroadcastDumper
	extends ListActivity
{	
    
	// ******************************************************************** //
    // Public Constants.
    // ******************************************************************** //

	/**
	 * Debugging tag for the app.
	 */
    public static final String TAG = "BroadcastDumper";

    
	// ******************************************************************** //
    // Activity Lifecycle.
    // ******************************************************************** //

    /**
     * Called when the activity is starting.  This is where most
     * initialisation should go.
     * 
     * You can call finish() from within this function, in which case
     * onDestroy() will be immediately called without any of the rest of
     * the activity lifecycle executing.
     * 
     * Derived classes must call through to the super class's implementation
     * of this method.  If they do not, an exception will be thrown.
     * 
     * @param   icicle          If the activity is being re-initialised
     *                          after previously being shut down then this
     *                          Bundle contains the data it most recently
     *                          supplied in onSaveInstanceState(Bundle).
     *                          Note: Otherwise it is null.
     */
	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		
        // Perform a managed query.  The Activity will handle closing and
		// requerying the cursor when needed.
        Cursor cursor = managedQuery(BroadcastSchema.BroadcastTable.CONTENT_URI,
        							 BroadcastSchema.BroadcastTable.PROJECTION,
        							 null, null,
        							 BroadcastSchema.BroadcastTable.SORT_ORDER);

        // Create an adaptor to map entries from the content provider to views.
        final String[] listFields = { BroadcastSchema.BroadcastTable.TITLE,
				   					  BroadcastSchema.BroadcastTable.EXTRAS };
        final int[] listIds = { android.R.id.text1,
								android.R.id.text2 };
        SimpleCursorAdapter adapter =
        			new SimpleCursorAdapter(this,
        									R.layout.list_item, cursor,
        									listFields, listIds);
        setListAdapter(adapter);
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
    
    
	// ******************************************************************** //
    // Menu and Preferences Handling.
    // ******************************************************************** //

	/**
     * Initialize the contents of the game's options menu by adding items
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
        case R.id.menu_clear:
            getContentResolver().delete(BroadcastSchema.BroadcastTable.CONTENT_URI, null, null);
        	break;
        case R.id.menu_exit:
        	finish();
        	break;
    	default:
    		return super.onOptionsItemSelected(item);
    	}
    	
    	return true;
    }
    
}

