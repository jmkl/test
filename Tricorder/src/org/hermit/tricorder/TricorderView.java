
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


import org.hermit.android.core.SurfaceRunner;
import org.hermit.android.sound.Effect;
import org.hermit.android.sound.Player;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;
import android.view.MotionEvent;
import android.view.WindowManager;


/**
 * The main tricorder view.
 */
class TricorderView
	extends SurfaceRunner
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
		AUD(R.string.nav_aud, R.string.title_aud, 0, 0xff9c6b6e),
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
		appContext = context;

		currentView = null;
		
		createGui(context);

		// Set the animation delay in ms. so we don't just continuously
		// whack the battery.
	    setDelay(10);
	    
        setDebugPerf(SHOW_FPS);
	}


    /**
     * Create the GUI for this view.
     * 
     * @param	context			Parent application context.
     */
    private void createGui(Tricorder context) {
     	// Get the main sensor manager, as several views use it.
    	SensorManager sm = (SensorManager)
    					context.getSystemService(Context.SENSOR_SERVICE);

        // Get the sound effects for scanners.
        Player player = context.getSoundPlayer();
        Effect gravSound = player.addEffect(R.raw.scan_low, 0.85f);
        Effect magSound = player.addEffect(R.raw.scan_high, 0.65f);

    	// Create the views.
    	for (ViewDefinition vdef : ViewDefinition.values()) {
    		switch (vdef) {
    		case GRA:
    			float gravUnit = SensorManager.STANDARD_GRAVITY;
    			gravView = new TridataView(context, this, sm,
                                           Sensor.TYPE_ACCELEROMETER,
                                           gravUnit, 2.2f,
                                           0xffccccff, 0xffff9e63,
                                           0xff50d050, 0xffff9e63,
                                           gravSound);
    	    	vdef.view = gravView;
    	    	break;
    		case MAG:
    	        float magUnit = SensorManager.MAGNETIC_FIELD_EARTH_MAX;
    	        magView = new TridataView(context, this, sm,
										  Sensor.TYPE_MAGNETIC_FIELD,
										  magUnit, 2.2f,
										  0xff6666ff, 0xffffcc00,
										  0xffcc6666, 0xffffcc00,
										  magSound);
                vdef.view = magView;
    	    	break;
    		case AUD:
    	    	vdef.view = new AudioView(context, this);
    	        break;
    		case GEO:
    		    geoView = new GeoView(context, this, sm);
    	    	vdef.view = geoView;
    	        break;
    		case COM:
    	    	vdef.view = new CommView(context, this);
    	        break;
    		case SOL:
    	    	vdef.view = new SolarView(context, this);
    	        break;
    		}
    	}
    }


	// ******************************************************************** //
	// Accessors.
	// ******************************************************************** //
    
    /**
     * Get the app's audio view.
     * 
     * @return          The audio view.
     */
    AudioView getAudioView() {
        return (AudioView) ViewDefinition.AUD.view;
    }


    // ******************************************************************** //
    // App State Management.
    // ******************************************************************** //

    /**
     * The application is starting.  Perform any initial set-up prior to
     * starting the application.  We may not have a screen size yet,
     * so this is not a good place to allocate resources which depend on
     * that.
     */
    @Override
    protected void appStart() {
    }


    /**
     * Set the screen size.  This is guaranteed to be called before
     * animStart(), but perhaps not before appStart().
     * 
     * @param   width       The new width of the surface.
     * @param   height      The new height of the surface.
     * @param   config      The pixel format of the surface.
     */
    @Override
    protected void appSize(int width, int height, Bitmap.Config config) {
        Log.i(TAG, "TV: surfaceChanged: " + width + "x" + height);
        
        Rect bounds = new Rect(0, 0, width, height);
        for (ViewDefinition vdef : ViewDefinition.values())
            vdef.view.setGeometry(bounds);

        // Tell the Grav and Mag views our orientation, so that they
        // can adjust the sensor axes to match the screen axes.
    	WindowManager wm = (WindowManager) appContext.getSystemService(Context.WINDOW_SERVICE);
    	Display disp = wm.getDefaultDisplay();
        gravView.setRotation(disp.getOrientation());
        magView.setRotation(disp.getOrientation());
        geoView.setRotation(disp.getOrientation());
    }
    

    /**
     * We are starting the animation loop.  The screen size is known.
     * 
     * <p>doUpdate() and doDraw() may be called from this point on.
     */
    @Override
    protected void animStart() {
        Log.i(TAG, "TV: start everything");
        
        // Give all views an app starting notification.
        for (ViewDefinition vdef : ViewDefinition.values())
            vdef.view.appStart();

        // Start the front view.
        if (currentView != null)
            currentView.view.start();
        
        enabled = true;
    }
    

    /**
     * We are stopping the animation loop, for example to pause the app.
     * 
     * <p>doUpdate() and doDraw() will not be called from this point on.
     */
    @Override
    protected void animStop() {
        Log.i(TAG, "TV: stop everything");
        
        enabled = false;

        // Stop the front view.
        if (currentView != null)
            currentView.view.stop();
        
        // Give all views an "app stopping" notification.
        for (ViewDefinition vdef : ViewDefinition.values())
            vdef.view.appStop();
    }
    

    /**
     * The application is closing down.  Clean up any resources.
     */
    @Override
    protected void appStop() {
    }
    

    /**
     * Stop the current view (if any) and de-select it, so we have no
     * current view.
     */
    void deselectView() {
        synchronized (this) {
            if (currentView != null) {
                currentView.view.stop();
                currentView = null;
            }
        }
    }


    /**
     * Select and display the given view.  You should call stopView() first.
     * 
     * @param	viewDef			View definition of the view to show.
     */
    void selectView(ViewDefinition viewDef) {
    	synchronized (this) {
    		if (currentView != null)
    			currentView.view.stop();
    		currentView = viewDef;
    		if (enabled && currentView != null)
    			currentView.view.start();
    	}
    }


    /**
     * Unbind any statically-bound resources which could leak memory.
     * This is typically used when the app is being destroyed, possibly
     * as a result of a device orientation change.  If we have static data
     * which links to the activity, the activity will be leaked (i.e.
     * prevented from being garbage collected).  Hence unbind it here.
     */
	protected void unbindResources() {
        // Pass it on to all our views.
        for (ViewDefinition vdef : ViewDefinition.values())
            vdef.view.unbindResources();
    }
    

    // ******************************************************************** //
    // Configuration.
    // ******************************************************************** //

    /**
     * Set the general scanning mode.  This affects whichever views support
     * it.
     * 
     * @param   continuous      If true, scan all the time.  Otherwise,
     *                          scan only under user control.
     */
    void setScanMode(boolean continuous) {
        for (ViewDefinition vdef : ViewDefinition.values())
            vdef.view.setScanMode(continuous);
    }


    /**
     * Set the general scanning mode.  This affects whichever views support
     * it.
     * 
     * @param   enable          If true, play a sound while scanning
     *                          under user control.  Else don't.
     */
    void setScanSound(boolean enable) {
        for (ViewDefinition vdef : ViewDefinition.values())
            vdef.view.setScanSound(enable);
    }


    /**
     * Set the units in which to display numeric data.
     * 
     * @param   unit            Units to display.
     */
    void setDataUnits(Tricorder.Unit unit) {
        for (ViewDefinition vdef : ViewDefinition.values())
            vdef.view.setDataUnits(unit);
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
     * Update the state of the application for the current frame.
     * 
     * <p>Applications must override this, and can use it to update
     * for example the physics of a game.  This may be a no-op in some cases.
     * 
     * <p>doDraw() will always be called after this method is called;
     * however, the converse is not true, as we sometimes need to draw
     * just to update the screen.  Hence this method is useful for
     * updates which are dependent on time rather than frames.
     * 
     * @param   now         Nominal time of the current frame in ms.
     */
    @Override
    protected void doUpdate(long now) {
        // If 1 second has passed, give the view a 1-sec tick.
        if (currentView != null && now - lastTick > 1000) {
            currentView.view.tick(now);
            lastTick = now;
        }
    }
    
    
    /**
     * Draw the current frame of the application.
     * 
     * <p>Applications must override this, and are expected to draw the
     * entire screen into the provided canvas.
     * 
     * <p>This method will always be called after a call to doUpdate(),
     * and also when the screen needs to be re-drawn.
     * 
     * @param   canvas      The Canvas to draw into.
     * @param   now         Nominal time of the current frame in ms.
     */
    @Override
    protected void doDraw(Canvas canvas, long now) {
        if (currentView != null) {
            canvas.drawColor(Tricorder.COL_BG);
            currentView.view.draw(canvas, now, true);
        }
    }


    // ******************************************************************** //
    // Save and Restore.
    // ******************************************************************** //

    /**
     * Save the state of the application in the provided Bundle.
     * 
     * @param   icicle      The Bundle in which we should save our state.
     */
    protected void saveState(Bundle icicle) {
        // The current view is saved and restored at the top level.
        
        for (ViewDefinition vdef : ViewDefinition.values())
            vdef.view.saveState(icicle);
    }


    /**
     * Restore the application state from the given Bundle.
     * 
     * @param   icicle      The Bundle containing the saved state.
     */
    protected void restoreState(Bundle icicle) {
        // The current view is saved and restored at the top level.
        
        for (ViewDefinition vdef : ViewDefinition.values())
            vdef.view.restoreState(icicle);
    }
    

    // ******************************************************************** //
    // Class Data.
    // ******************************************************************** //

    // Debugging tag.
	private static final String TAG = "tricorder";
	
	// Set to true to display the FPS counter.
	private static final boolean SHOW_FPS = false;

	
	// ******************************************************************** //
	// Private Data.
	// ******************************************************************** //

	// Out application context.
	private Tricorder appContext;
    
    // The Grav, Mag and Geo views.
    private TridataView gravView = null;
    private TridataView magView = null;
    private GeoView geoView;

    // True if we are running.
    private boolean enabled = false;
    
    // The currently selected view.
    private ViewDefinition currentView = null;
	
    // Last time we passed a 1-second tick down to the view.
    private long lastTick = 0;

}

