
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


import org.hermit.geo.Distance;
import org.hermit.onwatch.provider.PassageSchema;
import org.hermit.onwatch.service.OnWatchService;

import android.app.ListFragment;
import android.app.LoaderManager;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Loader;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;


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
		Log.i(TAG, "PLF onCreateView()");
		
		appContext = inflater.getContext();

		// Inflate the layout for this fragment.
        View view = inflater.inflate(R.layout.passage_list_view, container, false);

        // Get the data fields.
        nameField = (EditText) view.findViewById(R.id.passage_name_field);
        fromField = (EditText) view.findViewById(R.id.passage_from_field);
        toField = (EditText) view.findViewById(R.id.passage_to_field);
        statField = (TextView) view.findViewById(R.id.passage_stat_field);
        distField = (TextView) view.findViewById(R.id.passage_dist_field);

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

        startButton = (Button) view.findViewById(R.id.passage_start_button);
        startButton.setOnClickListener(new OnClickListener() {
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
		Log.i(TAG, "PLF onActivityCreated()");
		
        super.onActivityCreated(icicle);

        // Restore the current position, if we have a saved state.
        if (icicle != null)
            mCurPosition = icicle.getInt("listPosition");
        else
        	mCurPosition = 0;
        
        ListView lv = getListView();
        lv.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        lv.setCacheColorHint(Color.TRANSPARENT);
        
        // Create an empty adapter we will use to display the loaded data.
        passAdapter = new SimpleCursorAdapter(getActivity(),
                R.layout.passage_list_item, null,
                new String[] { PassageSchema.Passages.NAME,
        					   PassageSchema.Passages.DEST_NAME,
        					   PassageSchema.Passages.UNDER_WAY },
                new int[] { R.id.name, R.id.description, R.id.icon }, 0);
        setListAdapter(passAdapter);
        
        // Set a custom view binder, so we can set the indicator icon
        // appropriately.
        passAdapter.setViewBinder(new SimpleCursorAdapter.ViewBinder() {
    		@Override
    		public boolean setViewValue(View v, Cursor c, int cindex) {
    			if (cindex == COLUMN_NAME && v instanceof TextView) {
    				String name = c.getString(cindex);
    				if (name != null)
    					name = name.trim();
    				if (name == null || name.length() == 0)
    					name = "<unnamed>";
    				((TextView) v).setText(name);
    				return true;
    			} else if (cindex == COLUMN_DEST_NAME && v instanceof TextView) {
    				String start = c.getString(COLUMN_START_NAME);
    				String dest = c.getString(COLUMN_DEST_NAME);
    				if (start != null)
        				if (dest != null)
            				((TextView) v).setText(start + " to " + dest);
        				else
            				((TextView) v).setText("from " + start);
    				else
        				if (dest != null)
            				((TextView) v).setText("to " + dest);
        				else
            				((TextView) v).setText("");
    				return true;
    			} else if (cindex == COLUMN_UNDER_WAY && v instanceof ImageView) {
    				ImageView iv = (ImageView) v;
    				int valid = c.getInt(cindex);
    				int icon;
    				switch (valid) {
    				case 0:
    					icon = R.drawable.indicator_error;
    					break;
    				default:
    					icon = R.drawable.indicator_ok;
    					break;
    				}
    				iv.setImageResource(icon);
    				return true;
    			}
    			
    			// Leave to the base class.
    			return false;
    		}
        });
    	
        // Prepare the loaders.  Either re-connect with an existing one,
        // or start a new one.
        getLoaderManager().initLoader(LOADER_LIST, null, listLoaderCallbacks);
        getLoaderManager().initLoader(LOADER_ITEM, null, itemLoaderCallbacks);
    }

    
    /**
     * Called when the Fragment is visible to the user.  This is generally
     * tied to Activity.onStart() of the containing Activity's lifecycle.
     */
    @Override
	public void onStart() {
		Log.i(TAG, "PLF onStart()");
		
    	super.onStart();
    	
        getLoaderManager().restartLoader(LOADER_LIST, null, listLoaderCallbacks);
    }


	/**
	 * Called when the fragment is visible to the user and actively running.
	 * This is generally tied to Activity.onResume() of the containing
	 * Activity's lifecycle.
	 */
	@Override
	public void onResume () {
		Log.i(TAG, "PLF onResume()");
		
		super.onResume();
	}


	/**
	 * Called to ask the fragment to save its current dynamic state,
	 * so it can later be reconstructed in a new instance of its process
	 * is restarted.  If a new instance of the fragment later needs to be
	 * created, the data you place in the Bundle here will be available
	 * in the Bundle given to onCreate(), onCreateView(), and
	 * onActivityCreated().
	 * 
	 * @param	outState	Bundle in which to place the saved state.
	 */
    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        
        outState.putInt("listPosition", mCurPosition);
    }

    
	/**
	 * Called when the Fragment is no longer resumed.  This is generally
	 * tied to Activity.onPause of the containing Activity's lifecycle.
	 */
	@Override
	public void onPause() {
		Log.i(TAG, "PLF onPause()");
		
		super.onPause();
	}


    /**
     * Called when the Fragment is no longer started.  This is generally
     * tied to Activity.onStop() of the containing Activity's lifecycle.
     */
    @Override
    public void onStop() {
		Log.i(TAG, "PLF onStop()");
		
    	super.onStop();
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
    		Log.i(TAG, "PLF list onCreateLoader()");
    		
    		// First, pick the base URI to use depending on whether we are
    		// currently filtering.
    		Uri baseUri = PassageSchema.Passages.CONTENT_URI;

    		// Now create and return a CursorLoader that will take care of
    		// creating a Cursor for the data being displayed.
    		return new CursorLoader(getActivity(), baseUri,
    								PASSAGE_SUMMARY_PROJ,
    								null, null,
    								PassageSchema.Passages.START_TIME + " desc");
    	}

    	public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
    		Log.i(TAG, "PLF list onLoadFinished()");
    		
    		// Swap the new cursor in.  (The framework will take care
    		// of closing the old cursor once we return.)
    		passAdapter.swapCursor(data);
    		
    		// Re-display the current iteṁ.
    		selectPosition(mCurPosition);
    	}

    	public void onLoaderReset(Loader<Cursor> loader) {
    		Log.i(TAG, "PLF list onLoaderReset()");
    		
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
    		Log.i(TAG, "PLF item onCreateLoader(); passageUri=" + passageUri);
    		
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
    		Log.i(TAG, "PLF item onLoadFinished(); passageUri=" + passageUri);
    		
    		// Swap the new cursor in and display the passage.
    		passageCursor = data;
    		showPassage();
    	}

    	public void onLoaderReset(Loader<Cursor> loader) {
    		Log.i(TAG, "PLF item onLoaderReset(); passageUri=" + passageUri);
    		
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
        updateImage(position);
    }

    
    private void selectPosition(int position) {
        ListView lv = getListView();
        lv.setItemChecked(position, true);
        updateImage(position);
    }


    private void updateImage(int position) {
        // Set the URI for the current item, and re-load it.
    	long id = passAdapter.getItemId(position);
        passageUri = ContentUris.withAppendedId(
        					PassageSchema.Passages.CONTENT_URI, id);
        getLoaderManager().restartLoader(LOADER_ITEM, null, itemLoaderCallbacks);
        mCurPosition = position;
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

            // Set up the info display fields.
            Long stime = passageCursor.getLong(COLUMN_START_TIME);
            Long ftime = passageCursor.getLong(COLUMN_FINISH_TIME);
            String stat;
            if (stime == null || stime == 0)
            	stat = getString(R.string.lab_passage_not_started);
            else if (ftime == null || ftime == 0) {
            	stat = getString(R.string.lab_passage_started_at) + " " + stime;
            } else {
            	stat = getString(R.string.lab_passage_finished_at) + " " + ftime;
            }
            statField.setText(stat);

            Double dist = passageCursor.getDouble(COLUMN_DISTANCE);
            if (stime != null && dist != null)
            	distField.setText(Distance.describeNautical(dist));
            else
            	distField.setText("");
        }
        
        updateEditorControls();
    }


    /**
     * Save the passage data in the editor.
     */
    private void savePassage() {
        // The user is going somewhere else, so make sure their current
        // changes are safely saved away in the provider.  We don't need
        // to do this if only editing.
    	if (passageUri != null) {
            String name = nameField.getText().toString().trim();
            String from = fromField.getText().toString().trim();
            String to = toField.getText().toString().trim();

    	    // If we are creating a new passage, then we want to also create
    	    // an initial title for it, if there isn't one.
    	    if (name.length() == 0)
    	    	name = "<new passage>";

    	    // Write the passage back into the provider.
    	    ContentValues values = new ContentValues();
    	    if (name.length() > 0)
    	    	values.put(PassageSchema.Passages.NAME, name);
    	    if (from.length() > 0)
    	    	values.put(PassageSchema.Passages.START_NAME, from);
    	    if (to.length() > 0)
    	    	values.put(PassageSchema.Passages.DEST_NAME, to);

    	    // Commit all of our changes to persistent storage.  When the
    	    // update completes the content provider will notify the
    	    // cursor of the change, which will cause the UI to be updated.
    	    appContext.getContentResolver().update(passageUri, values, null, null);
    	}
    }
    
    
    private void updateEditorControls() {
    	// Set the "Passage Start" button appropriately.
    	if (!onWatchService.isAnyPassageRunning()) {
    		startButton.setVisibility(View.VISIBLE);
    		startButton.setText(R.string.passage_start);
    	} else if (passageUri != null && onWatchService.isRunning(passageUri)) {
    		startButton.setVisibility(View.VISIBLE);
    		startButton.setText(R.string.passage_stop);
    	} else {
    		startButton.setVisibility(View.INVISIBLE);
    		startButton.setText(R.string.passage_stop);
    	}
    }

    
	// ******************************************************************** //
	// Passage Control.
	// ******************************************************************** //

    private void startPassage() {
    	if (passageUri != null && onWatchService != null) {
    		if (!onWatchService.isAnyPassageRunning())
    			onWatchService.startPassage(passageUri);
    		else if (passageUri != null && onWatchService.isRunning(passageUri))
    			onWatchService.finishPassage();
    		
    		updateEditorControls();
    	}
    }


	// ******************************************************************** //
	// Class Data.
	// ******************************************************************** //

    // Debugging tag.
	private static final String TAG = "onwatch";


    // These are the passages columns that we will display.
	private static final String[] PASSAGE_SUMMARY_PROJ = new String[] {
    	PassageSchema.Passages._ID,
        PassageSchema.Passages.NAME,
        PassageSchema.Passages.START_NAME,
        PassageSchema.Passages.DEST_NAME,
        PassageSchema.Passages.UNDER_WAY,
        PassageSchema.Passages.START_TIME,
        PassageSchema.Passages.FINISH_TIME,
        PassageSchema.Passages.DISTANCE,
    };
    
    // The indices of the columns in the projection.
    private static final int COLUMN_NAME = 1;
    private static final int COLUMN_START_NAME = 2;
    private static final int COLUMN_DEST_NAME = 3;
    private static final int COLUMN_UNDER_WAY = 4;
    private static final int COLUMN_START_TIME = 5;
    private static final int COLUMN_FINISH_TIME = 6;
    private static final int COLUMN_DISTANCE = 7;

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
    
    // Position of the passage we're currently editing.
    private int mCurPosition = 0;

    // URI of the passage we're currently editing.
    private Uri passageUri;

    // Cursor used to access the passage list.
    private Cursor passageCursor = null;

    // Input field widgets.
    private EditText nameField;
    private EditText fromField;
    private EditText toField;
    private TextView statField;
    private TextView distField;
    
    // Control buttons.
    private Button startButton;

}

