
/**
 * Tricorder: turn your phone into a tricorder.
 * 
 * This is an Android implementation of a Star Trek tricorder, based on
 * the phone's own sensors.  It's also a demo project for sensor access.
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


package org.hermit.tricorder;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;


/**
 * The main tricorder view.
 */
class TricorderView
	extends SurfaceView
	implements SurfaceHolder.Callback
{

	// ******************************************************************** //
	// Public Types.
	// ******************************************************************** //

	/**
	 * Define the connected direction combinations.  The enum is
	 * carefully organised so that the ordinal() of each value is a
	 * bitmask representing the connected directions.  This allows us
	 * to manipulate directions more easily.
	 * 
	 * Each enum value also stores the ID of the bitmap representing
	 * it, or zero if none.  Note that this is the bitmap for
	 * the cabling layer, not the background or foreground (terminal etc).
	 */
	public static enum ViewDefinition {
		GRA(R.string.nav_gra, R.string.title_gra, 0, 0xffdbae6b),
		MAG(R.string.nav_mag, R.string.title_mag, 0, 0xff5dc0d3),
		ENV(R.string.nav_env, R.string.title_env, 0, 0xff9c6b6e),
		GEO(R.string.nav_geo, R.string.title_geo, 0, 0xffb9a9c4),
		COM(R.string.nav_com, R.string.title_com, R.string.lab_wifi_power, 0xffff9c63),
		SOL(R.string.nav_sol, R.string.title_sol, R.string.lab_alt_mode, 0xffff9900);

		ViewDefinition(int lab, int tit, int aux, int col) {
			labelId = lab;
			titleId = tit;
			auxId = aux;
			bgColor = col;
		}
		
		public final int labelId, titleId, auxId;
		public final int bgColor;
		public DataView view = null;
	}


	// ******************************************************************** //
	// Constructor.
	// ******************************************************************** //

	/**
	 * Set up this view.
	 * 
	 * @param	context			Parent application context.
	 */
	public TricorderView(Tricorder context) {
		super(context);

        // Register for events on the surface.
        surfaceHolder = getHolder();
        surfaceHolder.addCallback(this);

		currentView = null;
		
		createGui(context, surfaceHolder);

		// Make sure we get key events.  TODO: check for touch mode stuff.
        setFocusable(true);
        setFocusableInTouchMode(true);
	}


    /**
     * Create the GUI for this view.
     * 
     * @param	context			Parent application context.
     * @param	sh				SurfaceHolder we're drawing in.
     */
    private void createGui(Tricorder context, SurfaceHolder sh) {
     	// Get the main sensor manager, as several views use it.
    	SensorManager sm = (SensorManager)
    					context.getSystemService(Context.SENSOR_SERVICE);

    	// Create the views.
    	for (ViewDefinition vdef : ViewDefinition.values()) {
    		switch (vdef) {
    		case GRA:
    			float gravUnit = SensorManager.STANDARD_GRAVITY;
    	    	vdef.view = new TridataView(context, sh, sm,
    	    								Sensor.TYPE_ACCELEROMETER,
    	    								gravUnit, 2.2f,
    	    							    0xffccccff, 0xffff9e63,
    	    							    0xff50d050, 0xffff9e63);
    	    	break;
    		case MAG:
    	        float magUnit = SensorManager.MAGNETIC_FIELD_EARTH_MAX;
    	        vdef.view = new TridataView(context, sh, sm,
											Sensor.TYPE_MAGNETIC_FIELD,
											magUnit, 2.2f,
					    					0xff6666ff, 0xffffcc00,
					    					0xffcc6666, 0xffffcc00);
    	    	break;
    		case ENV:
    	    	vdef.view = new MultiGraphView(context, sh, sm);
    	        break;
    		case GEO:
    	    	vdef.view = new GeoView(context, sh);
    	        break;
    		case COM:
    	    	vdef.view = new CommView(context, sh);
    	        break;
    		case SOL:
    	    	vdef.view = new SolarView(context, sh);
    	        break;
    		}
    	}
    }


	// ******************************************************************** //
	// App State Management.
	// ******************************************************************** //

	/**
	 * Start this tricorder.  This notifies the view that it should start
	 * receiving and displaying data.
	 */
	public void doOnResume() {
    	Log.i(TAG, "TV: doOnResume");
    	
		synchronized (surfaceHolder) {
			appRunning = true;
			setState();
		}
	}
	

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
    	Log.i(TAG, "TV: surfaceCreated");
    	
        // start the thread here so that we don't busy-wait in run()
        // waiting for the surface to be created
		synchronized (surfaceHolder) {
			surfaceReady = true;
			setState();
		}
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
    	Log.i(TAG, "TV: surfaceChanged: " + width + "x" + height);
    	
    	Rect bounds = new Rect(0, 0, width, height);
		synchronized (surfaceHolder) {
	    	for (ViewDefinition vdef : ViewDefinition.values())
	    		vdef.view.setGeometry(bounds);
		}
		
		surfaceSized = true;
		setState();
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
    	Log.i(TAG, "TV: surfaceDestroyed");
    	
		synchronized (surfaceHolder) {
			surfaceReady = false;
			setState();
		}
    }


	/**
	 * Stop this tricorder.  This notifies the view that it should stop
	 * receiving and displaying data, and generally stop using
	 * resources.
	 */
	public void doOnPause() {
    	Log.i(TAG, "TV: doOnPause");
    	
		synchronized (surfaceHolder) {
			appRunning = false;
			setState();
		}
	}
	
	
	/**
	 * Set the application state based on whether the app is running
	 * and whether our surface is created.
	 */
	private void setState() {
		// See if the state actually changed.
		boolean oldEnabled = enabled;
		enabled = appRunning && surfaceReady && surfaceSized;
		if (enabled == oldEnabled)
			return;

		if (enabled)
			start();
		else
			stop();
	}
	
	
	/**
	 * Start everything running.
	 */
	private void start() {
    	Log.i(TAG, "TV: start everything");
    	
		// Give all views an app starting notification.
		for (ViewDefinition vdef : ViewDefinition.values())
			vdef.view.appStart();

		// Start the front view.
		if (currentView != null)
			currentView.view.start();

		// Start the 1-second tick events.
		if (hasFocus())
			resume();
	}
	
	
	/**
	 * Stop the main ticker.
	 */
	private void pause() {
    	Log.i(TAG, "TV: pause");
    	
		// Stop the tick events.
		if (ticker != null) {
			ticker.kill();
			ticker = null;
		}
	}

	
	/**
	 * Start everything running.
	 */
	private void resume() {
    	Log.i(TAG, "TV: resume");
    	
		// Start the 1-second tick events.
		if (ticker != null)
			ticker.kill();
		ticker = new Ticker();
	}
	

	/**
	 * Stop everything running.
	 */
	private void stop() {
    	Log.i(TAG, "TV: stop everything");
    	
		// Stop the tick events.
    	pause();

		// Stop the front view.
		if (currentView != null)
			currentView.view.stop();
		
		// Give all views an "app stopping" notification.
		for (ViewDefinition vdef : ViewDefinition.values())
			vdef.view.appStop();
	}

	
    /**
     * Handle changes in focus.  When we lose focus, pause the display
     * so a popup (like the menu) doesn't cause havoc.
     * 
     * @param	hasWindowFocus		True iff we have focus.
     */
    @Override
    public void onWindowFocusChanged(boolean hasWindowFocus) {
    	synchronized (surfaceHolder) {
    		if (enabled) {
    	    	Log.i(TAG, "TV: focus: " + hasWindowFocus);
    	    	
    			if (!hasWindowFocus)
    				pause();
    			else
    				resume();
    		}
    	}
    }


    /**
     * Select and display the given view.
     * 
     * @param	viewDef			View definition of the view to show.
     */
    void selectView(ViewDefinition viewDef) {
    	synchronized (surfaceHolder) {
    		if (currentView != null)
    			currentView.view.stop();
    		currentView = viewDef;
    		if (enabled && currentView != null)
    			currentView.view.start();
    	}
    }


	// ******************************************************************** //
	// Input.
	// ******************************************************************** //

    /**
     * Handle touch screen motion events.
     * 
     * @param	event			The motion event.
     * @return					True if the event was handled, false otherwise.
     */
    @Override
	public boolean onTouchEvent(MotionEvent event) {
		if (currentView != null)
			return currentView.view.handleTouchEvent(event);
		return false;
    }


	// ******************************************************************** //
	// Animation.
	// ******************************************************************** //

	/**
	 * Set whether we should simulate data for missing sensors.
	 * 
	 * @param	fakeIt			If true, sensors that aren't equipped will
	 * 							have simulated data displayed.  If false,
	 * 							they will show "No Data".
	 */
	void setSimulateMode(boolean fakeIt) {
		for (ViewDefinition vdef : ViewDefinition.values())
			vdef.view.setSimulateMode(fakeIt);
	}
	
	
	/**
	 * Step the application state.  Redraw the current view.
	 */
    private void step() {
    	Canvas canvas = null;

    	// Pass it to the current front view.
    	if (enabled) try {
			DataView view = currentView.view;
			long now = System.currentTimeMillis();
			
			// If 1 second has passed, give the view a 1-sec tick.
    		if (now - lastTick > 1000) {
    			synchronized (surfaceHolder) {
    				view.tick(now);
    				lastTick = now;
    			}
    		}
    		
    		// And re-draw the view.
    		canvas = surfaceHolder.lockCanvas(null);
    		synchronized (surfaceHolder) {
    			canvas.drawColor(Tricorder.COL_BG);
    			view.draw(canvas, now);
    		}
    		
    		// If we're tracking performance, draw in the FPS.
    		if (false) {
    			++fpsFrameCount;
    			if (now - fpsLastTime >= 1000) {
    				fpsLastCount = fpsFrameCount;
    				fpsFrameCount = 0;
    				fpsLastTime = now;
    			}
    			fpsPaint.setColor(0xff000000);
    			canvas.drawRect(0, 0, 50, 22, fpsPaint);
    			fpsPaint.setColor(0xffff0000);
    			canvas.drawText("" + fpsLastCount + " fps", 5, 14, fpsPaint);
    		}
    	} finally {
    		// do this in a finally so that if an exception is thrown
    		// during the above, we don't leave the Surface in an
    		// inconsistent state
    		if (canvas != null)
    			surfaceHolder.unlockCanvasAndPost(canvas);
    	}
    }


    // ******************************************************************** //
    // Private Types.
    // ******************************************************************** //

	/**
	 * Class which generates our ticks.
	 */
	private class Ticker extends Thread {
		public Ticker() {
			enable = true;
			start();
		}

		public void kill() {
			enable = false;
		}

		@Override
		public void run() {
			while (enable) {
				step();
				try {
					sleep(10);
				} catch (InterruptedException e) {
					enable = false;
				}
			}
		}
		private boolean enable;
	}
	

    // ******************************************************************** //
    // Class Data.
    // ******************************************************************** //

    // Debugging tag.
	@SuppressWarnings("unused")
	private static final String TAG = "tricorder";

	
	// ******************************************************************** //
	// Private Data.
	// ******************************************************************** //

	// The surface holder for our surface.
	private SurfaceHolder surfaceHolder;

    // The currently selected view.
    private ViewDefinition currentView = null;
    
    // Is the application running?  This is set in onResume() and cleared
    // on onPause().  If false, don't do anything.
    private boolean appRunning = false;
    
    // Is the surface ready?  This is set in surfaceCreated() and cleared
    // on surfaceDestroyed().  If false, don't do anything.
    private boolean surfaceReady = false;
    
    // Flag whether we have the surface size yet.
    private boolean surfaceSized = false;
    
    // Overall enabled flag.
    private boolean enabled = false;
	
    // Timer we use to generate tick events.
    private Ticker ticker = null;
 
    // Last time we passed a 1-second tick down to the view.
    private long lastTick = 0;
    
    // Data for counting and displaying frames per second.
	private int fpsFrameCount = 0;
	private int fpsLastCount = 0;
	private long fpsLastTime = 0;
	private Paint fpsPaint = new Paint();
	{
		fpsPaint.setColor(0xffff0000);
		fpsPaint.setTextSize(14);
	}

}

