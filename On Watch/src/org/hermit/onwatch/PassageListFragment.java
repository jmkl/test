
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


import org.hermit.onwatch.provider.PassageSchema;
import org.hermit.onwatch.service.OnWatchService;

import android.app.ListFragment;
import android.app.LoaderManager;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;


/**
 * This class controls the passage list display.
 */
public class PassageListFragment
	extends ListFragment
	implements ViewFragment
{

	// ******************************************************************** //
    // Fragment Lifecycle.
    // ******************************************************************** //

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
	@Override
	public View onCreateView(LayoutInflater inflater,
							 ViewGroup container,
							 Bundle icicle)
	{
		appContext = inflater.getContext();

		// Inflate the layout for this fragment.
        View view = inflater.inflate(R.layout.passage_list_view, container, false);

        // Get the data fields.
        nameField = (EditText) view.findViewById(R.id.passage_name_field);
        fromField = (EditText) view.findViewById(R.id.passage_from_field);
        toField = (EditText) view.findViewById(R.id.passage_to_field);

        // Set up the handlers for the control buttons.
        Button newButton = (Button) view.findViewById(R.id.passage_new_button);
        newButton.setOnClickListener(new View.OnClickListener() {
        	@Override
        	public void onClick(View v) {
        		newPassage();
        	}
        });

        Button save = (Button) view.findViewById(R.id.passage_save_button);
        save.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View arg0) {
            	savePassage();
            }
        });

        Button start = (Button) view.findViewById(R.id.passage_start_button);
        start.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View arg0) {
            	startPassage();
            }
        });

        return view;
	}


	/**
	 * Called when the fragment's activity has been created and this
	 * fragment's view hierarchy instantiated.  It can be used to do final
	 * initialization once these pieces are in place, such as retrieving
	 * views or restoring state.  It is also useful for fragments that
	 * use setRetainInstance(boolean) to retain their instance, as this
	 * callback tells the fragment when it is fully associated with the
	 * new activity instance.
	 * 
	 * @param	icicle		If the fragment is being re-created from a
	 * 						previous saved state, this is the state.
	 */
    @Override
    public void onActivityCreated(Bundle icicle) {
        super.onActivityCreated(icicle);

        // We have a context menu to show in the list.
        registerForContextMenu(getListView());
        
        // Create an empty adapter we will use to display the loaded data.
        passAdapter = new SimpleCursorAdapter(getActivity(),
                android.R.layout.simple_list_item_2, null,
                new String[] { PassageSchema.Passages.NAME, PassageSchema.Passages.DEST_NAME },
                new int[] { android.R.id.text1, android.R.id.text2 }, 0);
        setListAdapter(passAdapter);

        // Prepare the loaders.  Either re-connect with an existing one,
        // or start a new one.
        getLoaderManager().initLoader(LOADER_LIST, null, listLoaderCallbacks);
        getLoaderManager().initLoader(LOADER_ITEM, null, itemLoaderCallbacks);
    }


	// ******************************************************************** //
    // App Lifecycle.
    // ******************************************************************** //

	/**
	 * Start this view.
	 * 
	 * @param	time			Our serivce, which is now available.
	 */
	public void start(OnWatchService service) {
		onWatchService = service;
	}

	
	/**
	 * Regular tick event, for housekeeping.  Occurs every second, as
	 * close as possible to the 1-second boundary.
	 * 
	 * @param	time			Current system time in millis.
	 */
	@Override
	public void tick(long time) {
	}


	/**
	 * Stop this view.  The OnWatchService is no longer usable.
	 */
	public void stop() {
		onWatchService = null;
	}

	
	// ******************************************************************** //
    // Menu Handling.
    // ******************************************************************** //
    
    /**
     * Called when a context menu for the view is about to be shown.
     * Unlike onCreateOptionsMenu(Menu), this will be called every time
     * the context menu is about to be shown and should be populated
     * for the view (or item inside the view for AdapterView subclasses,
     * this can be found in the menuInfo)).
     * 
     * Use onContextItemSelected(android.view.MenuItem) to know when an
     * item has been selected.
     * 
     * It is not safe to hold onto the context menu after this method
     * returns.  Called when the context menu for this view is being built.
     * It is not safe to hold onto the menu after this method returns.
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
    	MenuItem item = menu.add(Menu.NONE, MENU_ITEM_EDIT, Menu.NONE, "Edit");
    	item.setIcon(android.R.drawable.ic_menu_edit);
    }
    
    
    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info;
        try {
             info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        } catch (ClassCastException e) {
            Log.e(TAG, "bad menuInfo", e);
            return false;
        }

        switch (item.getItemId()) {
        case MENU_ITEM_EDIT:
        	// Edit the passage that the context menu is for.
        	Uri uri = ContentUris.withAppendedId(
        						PassageSchema.Passages.CONTENT_URI, info.id);
        	Intent intent = new Intent(Intent.ACTION_EDIT, uri);
        	appContext.startActivity(intent);
        	return true;
        }
        
        return false;
    }


	// ******************************************************************** //
	// Data Management.
	// ******************************************************************** //

    /**
     * Create a new passage.
     */
    private void newPassage() {
	    // Create a blank new passage, and add it to the provider.
	    ContentValues values = new ContentValues();
	    values.put(PassageSchema.Passages.NAME, "New Passage");
	    passageUri = appContext.getContentResolver().insert(
	    						PassageSchema.Passages.CONTENT_URI, values);

	    // Show the new item.
        getLoaderManager().restartLoader(LOADER_ITEM, null, itemLoaderCallbacks);
    }


    /**
     * Loader callbacks, to monitor changes in the list data.
     */
    private final LoaderManager.LoaderCallbacks<Cursor> listLoaderCallbacks =
    	new LoaderManager.LoaderCallbacks<Cursor>() {

    	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
    		// First, pick the base URI to use depending on whether we are
    		// currently filtering.
    		Uri baseUri = PassageSchema.Passages.CONTENT_URI;

    		// Now create and return a CursorLoader that will take care of
    		// creating a Cursor for the data being displayed.
    		return new CursorLoader(getActivity(), baseUri,
    								PASSAGE_SUMMARY_PROJ,
    								null, null,
    								PassageSchema.Passages.SORT_ORDER);
    	}

    	public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
    		// Swap the new cursor in.  (The framework will take care
    		// of closing the old cursor once we return.)
    		passAdapter.swapCursor(data);
    	}

    	public void onLoaderReset(Loader<Cursor> loader) {
    		// This is called when the last Cursor provided to onLoadFinished()
    		// above is about to be closed.  We need to make sure we are no
    		// longer using it.
    		passAdapter.swapCursor(null);
    	}

    };


    /**
     * Loader callbacks, to monitor changes in the current item data.
     */
    private final LoaderManager.LoaderCallbacks<Cursor> itemLoaderCallbacks =
    	new LoaderManager.LoaderCallbacks<Cursor>() {

    	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
    		passageCursor = null;

    		// Get the URI of the item we're editing.
    		Uri uri;
    		if (passageUri != null)
    			uri = passageUri;
    		else
    			uri = PassageSchema.Passages.CONTENT_URI;
    		return new CursorLoader(getActivity(), uri,
    								PASSAGE_SUMMARY_PROJ,
    								null, null,
    								PassageSchema.Passages.SORT_ORDER);
    	}

    	public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
    		// Swap the new cursor in and display the passage.
    		passageCursor = data;
    		showPassage();
    	}

    	public void onLoaderReset(Loader<Cursor> loader) {
    		// This is called when the last Cursor provided to onLoadFinished()
    		// above is about to be closed.  We need to make sure we are no
    		// longer using it.
    		passageCursor = null;
    		showPassage();
    	}

    };


    /**
     * Handle an item being clicked in the passage list.
     * 
     * @param	l			The ListView widget.
     * @param	v			The list item that was clicked.
     * @param	position	The position of the selected item.
     * @param	id			The database row ID of the selected item.
     */
    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        // Set the URI for the current item, and re-load it.
        passageUri = ContentUris.withAppendedId(
        					PassageSchema.Passages.CONTENT_URI, id);
        getLoaderManager().restartLoader(LOADER_ITEM, null, itemLoaderCallbacks);
    }


    // ******************************************************************** //
    // Passage Data Editing.
    // ******************************************************************** //

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

            // Set up the editing fields.
            String name = passageCursor.getString(COLUMN_NAME);
            nameField.setText(name);
            String from = passageCursor.getString(COLUMN_START_NAME);
            fromField.setText(from);
            String dest = passageCursor.getString(COLUMN_DEST_NAME);
            toField.setText(dest);
        }
    }


    /**
     * Save the passage data in the editor.
     */
    private void savePassage() {
        // The user is going somewhere else, so make sure their current
        // changes are safely saved away in the provider.  We don't need
        // to do this if only editing.
    	if (passageUri != null) {
            String name = nameField.getText().toString();
            String from = fromField.getText().toString();
            String to = toField.getText().toString();

    	    // If we are creating a new passage, then we want to also create
    	    // an initial title for it, if there isn't one.
    	    if (TextUtils.isEmpty(name)) {
    	        if (!TextUtils.isEmpty(from) && !TextUtils.isEmpty(to))
    	            name = from + " to " + to;
    	    }

    	    // Write the passage back into the provider.
    	    ContentValues values = new ContentValues();
    	    values.put(PassageSchema.Passages.NAME, name);
    	    values.put(PassageSchema.Passages.START_NAME, from);
    	    values.put(PassageSchema.Passages.DEST_NAME, to);

    	    // Commit all of our changes to persistent storage.  When the
    	    // update completes the content provider will notify the
    	    // cursor of the change, which will cause the UI to be updated.
    	    appContext.getContentResolver().update(passageUri, values, null, null);
    	}
    }

    
	// ******************************************************************** //
	// Passage Control.
	// ******************************************************************** //

    private void startPassage() {
    	if (passageUri != null && onWatchService != null) {
    		if (onWatchService.isPassageRunning())
    			onWatchService.startPassage(passageUri);
    		else
    			onWatchService.finishPassage();
    	}
    }


	// ******************************************************************** //
	// Class Data.
	// ******************************************************************** //

    // Debugging tag.
	private static final String TAG = "onwatch";


    // These are the passages columns that we will display.
    static final String[] PASSAGE_SUMMARY_PROJ = new String[] {
    	PassageSchema.Passages._ID,
        PassageSchema.Passages.NAME,
        PassageSchema.Passages.START_NAME,
        PassageSchema.Passages.DEST_NAME,
    };
    
    // The indices of the columns in the projection.
    private static final int COLUMN_NAME = 1;
    private static final int COLUMN_START_NAME = 2;
    private static final int COLUMN_DEST_NAME = 3;

    // Menu item IDs.
    private static final int MENU_ITEM_EDIT = 1;

    // Loader IDs.
    private static final int LOADER_LIST = 1;
    private static final int LOADER_ITEM = 2;
	
    
	// ******************************************************************** //
	// Private Data.
	// ******************************************************************** //

    // Our context.
    private Context appContext;
    
    // Our OnWatch service.  null if we haven't bound to it yet.
    private OnWatchService onWatchService = null;

    // This is the Adapter being used to display the list's data.
    private SimpleCursorAdapter passAdapter;
    
    // URI of the passage we're currently editing.
    private Uri passageUri;

    // Cursor used to access the passage list.
    private Cursor passageCursor = null;

    // Input field widgets.
    private EditText nameField;
    private EditText fromField;
    private EditText toField;

}

