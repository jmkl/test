
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


import org.hermit.onwatch.provider.VesselSchema;

import android.app.ListActivity;
import android.app.LoaderManager;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.CursorLoader;
import android.content.Loader;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import com.commonsware.cwac.tlv.TouchListView;


/**
 * This class is the activity for the crew editor.
 */
public class CrewEditor
	extends ListActivity
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
        Log.i(TAG, "CE onCreate()");
        
        super.onCreate(icicle);
        
        // Get our content resolver.
		contentResolver = getContentResolver();

        // Create the application GUI.
        setContentView(R.layout.crew_editor);
        
        // Set a context menu on the list items.
        registerForContextMenu(getListView());
        
        // Restore the current position, if we have a saved state.
        if (icicle != null)
            mCurPosition = icicle.getInt("listPosition");
        else
        	mCurPosition = 0;
        
        // Setup the list drag handlers.
		TouchListView listView = (TouchListView) getListView();
		listView.setDropListener(onDrop);
		listView.setRemoveListener(onRemove);
		listView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
		listView.setCacheColorHint(Color.TRANSPARENT);

        // Set a list adapter which maps on to the crew names list.
        dataAdapter = new SimpleCursorAdapter(this,
        			R.layout.draggable_list_item, null,
        			new String[] { VesselSchema.Crew.NAME,
        						   VesselSchema.Crew.COLOUR },
        			new int[] { R.id.list_text1, R.id.list_text2 });
        setListAdapter(dataAdapter);
        
        // Set a custom view binder, so we can display items nicely.
        dataAdapter.setViewBinder(new SimpleCursorAdapter.ViewBinder() {
    		@Override
    		public boolean setViewValue(View v, Cursor c, int cindex) {
    			if (cindex == COLUMN_COLOUR) {
    				int col = c.getInt(COLUMN_COLOUR);
    				int pos = c.getInt(COLUMN_POSITION);
            		((TextView) v).setText("Pos: " + pos + " col: " + col);
    				return true;
    			}
    			
    			// Leave to the base class.
    			return false;
    		}
        });

        // Create an array of the names of the available watch plans.
        WatchPlan[] plans = WatchPlan.values();
        String[] names = new String[plans.length];
        for (int i = 0; i < plans.length; ++i)
        	names[i] = getString(plans[i].planNameId);

        // Set up the Spinner which selects the watch type.
        Spinner watchType = (Spinner) findViewById(R.id.watch_type);
        ArrayAdapter<String> adapter =
        	new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, names);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        watchType.setAdapter(adapter);
        watchType.setOnItemSelectedListener(new OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> view, View item,
									   int pos, long id) {
		    	setWatchPlan(WatchPlan.valueOf(pos));
			}
			@Override
			public void onNothingSelected(AdapterView<?> arg0) {
			}
        });
        
        // Set the spinner to the current value.  TODO: choose
        // the right vessel.
        WatchPlan plan = getWatchPlan();
        watchType.setSelection(plan.ordinal());
        
        // Set up the handlers for entries into the crew name field.
        nameField = (EditText) findViewById(R.id.crew_name_field);
        
        // Set up the handlers for the control buttons.
        Button add = (Button) findViewById(R.id.crew_new_button);
        add.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				addName();
			}
        });

        Button save = (Button) findViewById(R.id.crew_save_button);
        save.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				saveCrew();
			}
        });
    	
        // Prepare the data loader.  Either re-connect with an existing one,
        // or start a new one.
        getLoaderManager().initLoader(LOADER_LIST, null, listLoaderCallbacks);
        getLoaderManager().initLoader(LOADER_ITEM, null, itemLoaderCallbacks);
    }
    
    
    /**
     * Called after {@link #onCreate} or {@link #onStop} when the current
     * activity is now being displayed to the user.  It will
     * be followed by {@link #onRestart}.
     */
    @Override
	protected void onStart() {
        Log.i(TAG, "CE onStart()");
        
        super.onStart();
    	
        getLoaderManager().restartLoader(LOADER_LIST, null, listLoaderCallbacks);
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
        Log.i(TAG, "CE onResume()");
        
        super.onResume();
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
        Log.i(TAG, "CE onSaveInstanceState()");
        
        super.onSaveInstanceState(outState);
        
        // Save our current position in the crew list.
        outState.putInt("listPosition", mCurPosition);
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
        Log.i(TAG, "CE onPause()");
        
        super.onPause();
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
        Log.i(TAG, "CE onStop()");
        
        super.onStop();
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
        Log.i(TAG, "CE onDestroy()");
        
        super.onDestroy();
    }
    

	// ******************************************************************** //
	// Menu Handling.
	// ******************************************************************** //
    
    /**
     * Called when a context menu for the view is about to be shown.  Unlike
     * onCreateOptionsMenu(Menu), this will be called every time the
     * context menu is about to be shown and should be populated for the
     * view (or item inside the view for AdapterView subclasses, this can
     * be found in the menuInfo)).
     * 
     * @param	menu		The context menu that is being built.
     * @param	v			The view for which the context menu is being built.
     * @param	menuInfo	Extra information about the item for which the
     * 						context menu should be shown.  This information
     * 						will vary depending on the class of v. 
     */
    @Override
	public void onCreateContextMenu(ContextMenu menu, View v,
    							    ContextMenuInfo menuInfo)
    {
    	super.onCreateContextMenu(menu, v, menuInfo);
    	menu.add(0, MENU_DELETE, 0, R.string.menu_delete);
    }
    

    /**
     * This hook is called whenever an item in a context menu is selected.
	 * Derived classes should call through to the base class for it to
	 * perform the default menu handling.
	 * 
	 * @param	item		The context menu item that was selected.
	 * @return				False to allow normal context menu processing
	 * 						to proceed, true to consume it here.
     */
    @Override
	public boolean onContextItemSelected(MenuItem item) {
    	AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
    	switch (item.getItemId()) {
    	case MENU_DELETE:
    		Log.i(TAG, "CE Menu: Delete " + info.id);
    		deleteCrew(info.position);
    		return true;
    	default:
    		return super.onContextItemSelected(item);
    	}
    }


	// ******************************************************************** //
	// Data Management.
	// ******************************************************************** //

    /**
     * Loader callbacks, to monitor changes in the list data.
     */
    private final LoaderManager.LoaderCallbacks<Cursor> listLoaderCallbacks =
    	new LoaderManager.LoaderCallbacks<Cursor>() {

    	@Override
		public Loader<Cursor> onCreateLoader(int id, Bundle args) {
    		Log.i(TAG, "CE list onCreateLoader()");
    		
    		// Now create and return a CursorLoader that will take care of
    		// creating a Cursor for the data being displayed.
    		return new CursorLoader(CrewEditor.this,
    								VesselSchema.Crew.CONTENT_URI,
    								CREW_SUMMARY_PROJ,
    								null, null,
    								VesselSchema.Crew.POSITION + " asc");
    	}

    	@Override
		public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
    		Log.i(TAG, "CE list onLoadFinished()");
    		
    		// Swap the new cursor in.  (The framework will take care
    		// of closing the old cursor once we return.)
    		dataAdapter.swapCursor(data);
    		
    		// Re-display the current iteṁ.
    		selectPosition(mCurPosition);
    	}

    	@Override
		public void onLoaderReset(Loader<Cursor> loader) {
    		Log.i(TAG, "CE list onLoaderReset()");
    		
    		// This is called when the last Cursor provided to onLoadFinished()
    		// above is about to be closed.  We need to make sure we are no
    		// longer using it.
    		dataAdapter.swapCursor(null);
    	}

    };


    /**
     * Loader callbacks, to monitor changes in the current item data.
     */
    private final LoaderManager.LoaderCallbacks<Cursor> itemLoaderCallbacks =
    	new LoaderManager.LoaderCallbacks<Cursor>() {

    	@Override
		public Loader<Cursor> onCreateLoader(int id, Bundle args) {
    		Log.i(TAG, "CE item onCreateLoader(); passageUri=" + crewUri);
    		
    		crewCursor = null;

    		// Get the URI of the item we're editing.
    		Uri uri;
    		if (crewUri != null)
    			uri = crewUri;
    		else
    			uri = VesselSchema.Crew.CONTENT_URI;
    		return new CursorLoader(CrewEditor.this, uri,
    								CREW_SUMMARY_PROJ,
    								null, null,
    								VesselSchema.Crew.POSITION + " asc");
    	}

    	@Override
    	public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
    		Log.i(TAG, "CE item onLoadFinished(); passageUri=" + crewUri);
    		
    		// Swap the new cursor in and display the passage.
    		crewCursor = data;
    		showCrew();
    	}

    	@Override
    	public void onLoaderReset(Loader<Cursor> loader) {
    		Log.i(TAG, "CE item onLoaderReset(); passageUri=" + crewUri);
    		
    		// This is called when the last Cursor provided to onLoadFinished()
    		// above is about to be closed.  We need to make sure we are no
    		// longer using it.
    		crewCursor = null;
    		showCrew();
    	}

    };


    /**
     * Handle an item being clicked in the crew list.
     * 
     * @param	l			The ListView widget.
     * @param	v			The list item that was clicked.
     * @param	position	The position of the selected item.
     * @param	id			The database row ID of the selected item.
     */
    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        updateImage(position);
    }

    
    private void selectPosition(int position) {
        ListView lv = getListView();
        int count = dataAdapter.getCount();
        if (count == 0)
        	position = -1;
        else if (position >= count)
    		position = count - 1;
        else if (position < 0)
    		position = 0;
        
        if (position >= 0)
        	lv.setItemChecked(position, true);
        else
        	lv.clearChoices();
        
        updateImage(position);
    }


    private void updateImage(int position) {
    	// Set the URI for the current item, and re-load it.
    	if (position >= 0) {
    		long id = dataAdapter.getItemId(position);
    		crewUri = ContentUris.withAppendedId(VesselSchema.Crew.CONTENT_URI, id);
    	} else
    		crewUri = null;
    	getLoaderManager().restartLoader(LOADER_ITEM, null, itemLoaderCallbacks);
    	mCurPosition = position;
    }

    
    // ******************************************************************** //
    // Crew Data Editing.
    // ******************************************************************** //
    
    /**
     * Get the currently configured watch plan.
     * 
     * @return				The current watch plan.
     */
    private WatchPlan getWatchPlan() {
        // Set the spinner to the current value.  TODO: choose
        // the right vessel.
        Cursor c = null;
        try {
            c = contentResolver.query(VesselSchema.Vessels.CONTENT_URI,
            						  VesselSchema.Vessels.PROJECTION,
            						  null, null,
            						  VesselSchema.Vessels.SORT_ORDER);
            if (c == null || !c.moveToFirst()) {
            	// There's no vessel record.  Create one, with a default
            	// plan.  TODO: fix.
            	WatchPlan plan = WatchPlan.valueOf(0);
            	ContentValues values = new ContentValues();
            	values.put(VesselSchema.Vessels.WATCHES, plan.toString());
            	contentResolver.insert(VesselSchema.Vessels.CONTENT_URI,
            						   values);
            	return plan;
            } else {
            	// Get the plan from the vessel record.
            	int pi = c.getColumnIndexOrThrow(VesselSchema.Vessels.WATCHES);
            	String wp = c.getString(pi);
            	try {
            		return WatchPlan.valueOf(wp);
            	} catch (Exception e) {
            		return WatchPlan.valueOf(0);
            	}
            }
        } finally {
            if (c != null)
            	c.close();
        }
    }
    
    
    /**
     * Select a watch plan.
     * 
     * @param	plan			The watch plan to set.
     */
    private void setWatchPlan(WatchPlan plan) {
		Log.i(TAG, "CE setWatchPlan(" + plan + ")");
		
    	ContentValues values = new ContentValues();
    	values.put(VesselSchema.Vessels.WATCHES, plan.toString());
    	
    	// TODO:
    	//				String where = VesselSchema.Vessels._ID + "=?";
    	//				String[] wargs = new String[] { "" + vesselId };
    	
    	contentResolver.update(VesselSchema.Vessels.CONTENT_URI,
    						   values, null, null);
    }
    
    
    /**
     * Add the name in the name field to the crew list.
     */
    private void addName() {
		// Create a record for this person.
		// TODO: get a free colour.
        int count = dataAdapter.getCount();
		int col = count;
		int pos = count;

	    // Create a blank new person, and add it to the provider.
	    ContentValues values = new ContentValues();
	    values.put(VesselSchema.Crew.NAME, "New Crew");
	    values.put(VesselSchema.Crew.COLOUR, col);
	    values.put(VesselSchema.Crew.POSITION, pos);
	    crewUri = contentResolver.insert(
	    						VesselSchema.Crew.CONTENT_URI, values);

	    // Show the new item.
        getLoaderManager().restartLoader(LOADER_ITEM, null, itemLoaderCallbacks);
    }


    /**
     * Edit the indicated crew member.
     */
    private void showCrew() {
        // If we didn't have any trouble retrieving the data, it is now
        // time to get at the stuff.
        if (crewCursor != null && crewCursor.moveToFirst()) {
            // Set up the editing fields.
            String name = crewCursor.getString(COLUMN_NAME);
            nameField.setText(name);
        }
        
        updateEditorControls();
    }


    /**
     * Save the passage data in the editor.
     */
    private void saveCrew() {
        // The user is going somewhere else, so make sure their current
        // changes are safely saved away in the provider.  We don't need
        // to do this if only editing.
    	if (crewUri != null) {
            String name = nameField.getText().toString().trim();

    	    // If we are creating a new person, then we want to also create
    	    // an initial title for him, if there isn't one.
    	    if (name.length() == 0)
    	    	name = "<new crew>";

    	    // Write the passage back into the provider.
    	    ContentValues values = new ContentValues();
    	    if (name.length() > 0)
    	    	values.put(VesselSchema.Crew.NAME, name);

    	    // Commit all of our changes to persistent storage.  When the
    	    // update completes the content provider will notify the
    	    // cursor of the change, which will cause the UI to be updated.
    	    contentResolver.update(crewUri, values, null, null);
    	}
    }
    
    
    private void updateEditorControls() {
//    	// Set the "Passage Start" button appropriately.
//    	if (!onWatchService.isAnyPassageRunning()) {
//    		startButton.setVisibility(View.VISIBLE);
//    		startButton.setText(R.string.passage_start);
//    	} else if (passageUri != null && onWatchService.isRunning(passageUri)) {
//    		startButton.setVisibility(View.VISIBLE);
//    		startButton.setText(R.string.passage_stop);
//    	} else {
//    		startButton.setVisibility(View.INVISIBLE);
//    		startButton.setText(R.string.passage_stop);
//    	}
    }


	// ******************************************************************** //
	// Input Handling.
	// ******************************************************************** //

    /**
     * Delete a specified crew member from the crew list.
     * 
     * @param	pos			The position of the person to delete.
     */
    private void deleteCrew(int pos) {
		Log.i(TAG, "CE delete " + pos);
		
        int count = dataAdapter.getCount();
		if (pos < 0 || pos >= count)
			throw new IllegalArgumentException("Invalid offset in delete: " +
											   pos + "; num=" + count);

    	long id = dataAdapter.getItemId(pos);
    	Uri uri = ContentUris.withAppendedId(VesselSchema.Crew.CONTENT_URI, id);
    	contentResolver.delete(uri, null, null);
    	
    	// Update the positions of all subsequent people.
		try {
			ContentValues values = new ContentValues();
			String where = VesselSchema.Crew.POSITION + "=?";
			String[] wargs = new String[] { "" };
			
			for (int p = pos + 1; p < count; ++p) {
				wargs[0] = "" + p;
				values.put(VesselSchema.Crew.POSITION, p - 1);
				contentResolver.update(VesselSchema.Crew.CONTENT_URI, values, where, wargs);
			}
		} catch (Exception e) {
		}

    	// Re-select the current item in case the selected item was deleted.
    	// This will take care of going off the end of the list.
    	selectPosition(mCurPosition);
    }


    /**
     * Move a specified crew member within the crew list.
     *
     * @param	from		Position of the person to move.
     * @param	to			Position to move them to.
     */
	public void moveCrew(int from, int to) {
		Log.i(TAG, "CE move " + from + " -> " + to);
		
		if (from == to)
			return;
		
        int count = dataAdapter.getCount();
		if (from < 0 || to < 0 || from >= count || to >= count)
			throw new IllegalArgumentException("Invalid offset in move: " +
											   from + " -> " + to +
											   "; num=" + count);

		try {
			ContentValues values = new ContentValues();
			String where = VesselSchema.Crew.POSITION + "=?";
			String[] wargs = new String[] { "" };
			
			// First set the from person to an invalid position, so we
			// can move the others around with no overlap.
			wargs[0] = "" + from;
			values.put(VesselSchema.Crew.POSITION, -10);
			contentResolver.update(VesselSchema.Crew.CONTENT_URI, values, where, wargs);
			
			if (to > from) {
				for (int p = from + 1; p <= to; ++p) {
					wargs[0] = "" + p;
					values.put(VesselSchema.Crew.POSITION, p - 1);
					contentResolver.update(VesselSchema.Crew.CONTENT_URI, values, where, wargs);
				}
			} else {
				for (int p = from - 1; p >= to; --p) {
					wargs[0] = "" + p;
					values.put(VesselSchema.Crew.POSITION, p + 1);
					contentResolver.update(VesselSchema.Crew.CONTENT_URI, values, where, wargs);
				}
			}
			
			// Now move the from person to to.
			wargs[0] = "" + -10;
			values.put(VesselSchema.Crew.POSITION, to);
			contentResolver.update(VesselSchema.Crew.CONTENT_URI, values, where, wargs);
		} catch (Exception e) {
		}
		
		// Notify the observers that we changed.
    	getLoaderManager().restartLoader(LOADER_ITEM, null, itemLoaderCallbacks);
    	selectPosition(to);
	}


	// ******************************************************************** //
	// Drag-And-Drop Handling.
	// ******************************************************************** //

    /**
     * Handle a crew member being moved in the crew list.
     * 
     * @param	id			The database ID of the person to delete.
     */
	private TouchListView.DropListener onDrop = new TouchListView.DropListener() {
		@Override
		public void drop(int from, int to) {
	    	moveCrew(from, to);
		}
	};
	
	
    /**
     * Handle a crew member being dragged off from the crew list.
     * 
     * @param	id			The database ID of the person to delete.
     */
	private TouchListView.RemoveListener onRemove = new TouchListView.RemoveListener() {
		@Override
		public void remove(int which) {
	    	deleteCrew(which);
		}
	};
	

	// ******************************************************************** //
	// Class Data.
	// ******************************************************************** //

    // Debugging tag.
	private static final String TAG = "onwatch";

	// Menu item IDs for the context menu.
	private static final int MENU_DELETE = 1;

    // These are the crew columns that we will display.
	private static final String[] CREW_SUMMARY_PROJ = new String[] {
		VesselSchema.Crew._ID,
    	VesselSchema.Crew.NAME,
    	VesselSchema.Crew.COLOUR,
    	VesselSchema.Crew.POSITION,
    };

    // The indices of the columns in the projection.
    private static final int COLUMN_NAME = 1;
    private static final int COLUMN_COLOUR = 2;
    private static final int COLUMN_POSITION = 3;

     // Loader IDs.
    private static final int LOADER_LIST = 1;
    private static final int LOADER_ITEM = 2;


	// ******************************************************************** //
	// Private Data.
	// ******************************************************************** //
    
    // Our ContentResolver.
    private ContentResolver contentResolver;

    // Adapter which maps on to the crew list.
    private SimpleCursorAdapter dataAdapter;
    
    // Position of the crew member we're currently editing.  -1 indicates
    // nothing in the list.
    private int mCurPosition = 0;

    // URI of the crew member we're currently editing.  null indicates
    // nothing in the list.
    private Uri crewUri;

    // Cursor used to access the crew list.
    private Cursor crewCursor = null;

	// Edit field used to enter the crew names.
    private EditText nameField;

}

