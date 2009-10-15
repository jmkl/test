
/**
 * Plughole: a rolling-ball accelerometer game.
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


package org.hermit.plughole;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.TextView;


/**
 * The view on a playing table.
 */
class TableView
	extends SurfaceView
	implements SurfaceHolder.Callback
{

    // ******************************************************************** //
    // Constructor.
    // ******************************************************************** //

	/**
	 * Create a TableView instance.
	 * 
	 * @param	app			The application context we're running in.
	 */
    public TableView(Plughole app, LevelManager lman) {
        super(app);

        // Register for events on the surface.
        SurfaceHolder surfaceHolder = getHolder();
        surfaceHolder.addCallback(this);
        
        // Create a Handler for messages to set the text overlay.
        Handler handler = new Handler() {
            @Override
            public void handleMessage(Message m) {
            	Bundle data = m.getData();
            	int vis = data.getInt("viz");
            	String msg = data.getString("text");
                Log.i(TAG, "Overlay: set vis " + vis +
             		   				" (" + (msg == null ? "" : msg) + ")");
                textOverlay.setVisibility(vis);
                textOverlay.setText(msg);
            }
        };
 
		// Create the table.
		gameTable = new Table(app, surfaceHolder, lman, handler);
		
		// Make sure we get key events.  TODO: check for touch mode stuff.
        setFocusable(true);
    }
    

    /**
     * Installs a pointer to the text view used for messages.
     * 
     * TODO: just move the handler up.
     */
    public void setTextView(TextView textView) {
        textOverlay = textView;
    }
    

	// ******************************************************************** //
	// Surface State Handling.
	// ******************************************************************** //

    /**
     * This is called immediately after the surface is first created.
     * Implementations of this should start up whatever rendering code
     * they desire.
     * 
     * Note that only one thread can ever draw into a Surface, so you
     * should not draw into the Surface here if your normal rendering
     * will be in another thread.
     * 
     * @param	holder		The SurfaceHolder whose surface is being created.
     */
    public void surfaceCreated(SurfaceHolder holder) {
    	gameTable.surfaceCreated();
    }


    /**
     * This is called immediately after any structural changes (format or
     * size) have been made to the surface.  This method is always
     * called at least once, after surfaceCreated(SurfaceHolder).
     * 
     * @param	holder		The SurfaceHolder whose surface has changed.
     * @param	format		The new PixelFormat of the surface.
     * @param	width		The new width of the surface.
     * @param	height		The new height of the surface.
     */
    public void surfaceChanged(SurfaceHolder holder,
    						   int format, int width, int height)
    {
    	gameTable.setSurfaceSize(format, width, height);
    }

    
    /**
     * This is called immediately before a surface is destroyed.
     * After returning from this call, you should no longer try to
     * access this surface.  If you have a rendering thread that directly
     * accesses the surface, you must ensure that thread is no longer
     * touching the Surface before returning from this function.
     * 
     * @param	holder		The SurfaceHolder whose surface is being destroyed.
     */
    public void surfaceDestroyed(SurfaceHolder holder) {
    	gameTable.surfaceDestroyed();
    }


    // ******************************************************************** //
    // Game State Control.
    // ******************************************************************** //

    /**
     * We're resuming the app.
     */
    void onResume() {
    	gameTable.onResume();
    }


    /**
     * Pause the app.
     */
    public void onPause() {
    	gameTable.onPause();
    }


	/**
	 * Enable or disable the display of performance stats on-screen.
	 * 
	 * @param	show			Show stats iff true.
	 */
	void setShowPerf(boolean show) {
		gameTable.setShowPerf(show);
	}
	

    /**
     * Restart the level.
     */
    public void restart() {
    	gameTable.restart();
    }
    

    // TODO: kill or fix
	Table getTable() {
		return gameTable;
	}
	
	
    /**
     * Handle changes in focus.  When we lose focus, pause the game
     * so a popup (like the menu) doesn't cause havoc.
     * 
     * @param	hasWindowFocus		True iff we have focus.
     */
	@Override
	public void onWindowFocusChanged(boolean hasWindowFocus) {
		if (!hasWindowFocus)
			gameTable.pause();
	}


	// ******************************************************************** //
	// Input Handling.
	// ******************************************************************** //

    /**
	 * Handle key input.
	 * 
     * @param	keyCode		The key code.
     * @param	event		The KeyEvent object that defines the
     * 						button action.
     * @return				True if the event was handled, false otherwise.
	 */
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
    	return false;
    }
    
    
    /**
	 * Handle touchscreen input.
	 * 
     * @param	event		The MotionEvent object that defines the action.
     * @return				True if the event was handled, false otherwise.
	 */
    @Override
    public boolean onTouchEvent(MotionEvent event) {
    	int action = event.getAction();
    	switch (action) {
    	case MotionEvent.ACTION_DOWN:
    		gameTable.pausePlay();
    		return true;
    	case MotionEvent.ACTION_UP:
    	case MotionEvent.ACTION_MOVE:
    	case MotionEvent.ACTION_CANCEL:
    	default:
    		break;
    	}
    	
		return false;
    }


	/**
	 * Handle notification of the tilt of the device.
	 * 
	 * @param xaccel		Acceleration in the X direction.  -1g .. 1g.
	 * @param yaccel		Acceleration in the Y direction.  -1g .. 1g.
	 */
	void setTilt(float xaccel, float yaccel) {
		gameTable.setTilt(xaccel, yaccel);
	}
	

    // ******************************************************************** //
    // Save and Restore.
    // ******************************************************************** //

	/**
	 * Save the state of the game in the provided Bundle.
	 * 
	 * @return				The Bundle with this view's state saved to it.
	 */
	public Bundle saveState(Bundle icicle) {
		gameTable.saveState(icicle);
		return icicle;
	}

	
	/**
	 * Restore the game state from the given Bundle.
	 * 
	 * @param	icicle		The Bundle containing the saved state.
	 */
	public synchronized void restoreState(Bundle icicle) {
		gameTable.pause();
		gameTable.restoreState(icicle);
	}
	

    // ******************************************************************** //
    // Class Data.
    // ******************************************************************** //

    // Debugging tag.
	@SuppressWarnings("unused")
	private static final String TAG = "plughole";

	
	// ******************************************************************** //
	// Private Data.
	// ******************************************************************** //
	
    // The actual game table.
    private Table gameTable;

    // The text view we use to display the game state when paused etc.
    private TextView textOverlay;

}

