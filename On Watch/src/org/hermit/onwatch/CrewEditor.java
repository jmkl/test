
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


import android.app.ListActivity;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.SimpleCursorAdapter;
import android.widget.Spinner;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemSelectedListener;

import com.commonsware.cwac.tlv.TouchListView;


/**
 * This class is the activity for the crew editor.
 */
public class CrewEditor
	extends ListActivity
	implements OnKeyListener
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

        // We don't want a title bar or status bar.
//        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
//                		     WindowManager.LayoutParams.FLAG_FULLSCREEN);
//        requestWindowFeature(Window.FEATURE_NO_TITLE);

        // Create the application GUI.
        setContentView(R.layout.crew_editor);
        
        // Set a context menu on the list items.
        registerForContextMenu(getListView());
        
        // Get or create the crew model.
        crewModel = CrewModel.getInstance(this);
        crewCursor = crewModel.getCrewCursor();

        // Set a list adapter which maps on to the crew names list.
        dataAdapter = new SimpleCursorAdapter(this,
        							R.layout.draggable_list_item,
        							crewCursor,
        							new String[] { "name", "colour" },
        							new int[] { R.id.list_text1, R.id.list_text2 });
        setListAdapter(dataAdapter);
        
        // Se tup the list drag handlers.
		TouchListView listView = (TouchListView) getListView();
		listView.setDropListener(onDrop);
		listView.setRemoveListener(onRemove);

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
				crewModel.setWatchPlan(WatchPlan.values()[pos]);
			}
			@Override
			public void onNothingSelected(AdapterView<?> arg0) {
			}
        });
        
        // Set the spinner to the current value.
        WatchPlan plan = crewModel.getWatchPlan();
        watchType.setSelection(plan.ordinal());

        // Set up the handlers for entries into the crew name field,
        // including the "Add" button.
        nameField = (EditText) findViewById(R.id.crew_name_field);
        nameField.setOnKeyListener(this);
        Button add = (Button) findViewById(R.id.crew_add_button);
        add.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				addName();
			}
        });

        // Set up the handlers for entries into the crew name field.
        Button save = (Button) findViewById(R.id.crew_save_button);
        save.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				finish();
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
        
        crewCursor.close();
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
    		Log.i(TAG, "Menu: Delete " + info.id);
    		deleteCrew(info.position);
    		return true;
    	default:
    		return super.onContextItemSelected(item);
    	}
    }


	// ******************************************************************** //
	// Input Handling.
	// ******************************************************************** //

    /**
     * Handle a key press in the name field by adding the name to the
     * crew list.
     */
    public boolean onKey(View v, int keyCode, KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            switch (keyCode) {
                case KeyEvent.KEYCODE_DPAD_CENTER:
                case KeyEvent.KEYCODE_ENTER:
                    addName();
                    return true;
            }
        }
        return false;
    }


    /**
     * Add the name in the name field to the crew list.
     */
    private void addName() {
        String name = nameField.getText().toString();
        nameField.setText(null);

        name = name.trim();
        if (name.length() > 0) {
        	crewModel.addName(name);
        	crewCursor.requery();
        }
    }


    /**
     * Delete a specified crew member from the crew list.
     * 
     * @param	pos			The position of the person to delete.
     */
    private void deleteCrew(int pos) {
    	crewModel.deleteCrew(pos);
    	crewCursor.requery();
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
	    	crewModel.moveCrew(from, to);
	    	crewCursor.requery();
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
	    	crewModel.deleteCrew(which);
	    	crewCursor.requery();
		}
	};
	

	// ******************************************************************** //
	// Class Data.
	// ******************************************************************** //

    // Debugging tag.
	private static final String TAG = "onwatch";

	// Menu item IDs for the context menu.
	private static final int MENU_DELETE = 1;


	// ******************************************************************** //
	// Private Data.
	// ******************************************************************** //
	
    // The crew data model.
    private CrewModel crewModel = null;

    // Cursor used to access the crew list.
    private Cursor crewCursor = null;

    // Adapter which maps on to the crew list.
    private SimpleCursorAdapter dataAdapter;
    
	// Edit field used to enter the crew names.
    private EditText nameField;

}

