
/**
 * Substrate: a collection of eye candies for Android.  Various screen
 * hacks from the xscreensaver collection can be viewed standalone, or
 * set as live wallpapers.
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


package org.hermit.substrate;


import org.hermit.android.core.SurfaceRunner;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;


/**
 * An eye candy view; displays and runs a screen hack.  This class relies
 * on the parent SurfaceRunner class to do the bulk of the animation control.
 */
public class EyeCandyView
	extends SurfaceRunner
{

    // ******************************************************************** //
    // Constructor.
    // ******************************************************************** //

	/**
	 * Create a SurfaceRunner instance.
	 * 
	 * @param	app			The application context we're running in.
	 */
    public EyeCandyView(Context app) {
        super(app, SURFACE_DYNAMIC);
        
        setDelay(10);
    }


    // ******************************************************************** //
    // Run Control.
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
        // Ignore junk updates.  Do nothing if already set.
        if (width <= 0 || height <= 0)
            return;
        if (width == screenWidth && height == screenHeight && config == screenConfig)
            return;
        
        screenWidth = width;
        screenHeight = height;
        screenConfig = config;

        if (eyeCandy != null)
            eyeCandy.setConfiguration(screenWidth, screenHeight, screenConfig);
    }
    

    /**
     * We are starting the animation loop.  The screen size is known.
     * 
     * <p>doUpdate() and doDraw() may be called from this point on.
     */
    @Override
    protected void animStart() {
    }
    

    /**
     * We are stopping the animation loop, for example to pause the app.
     * 
     * <p>doUpdate() and doDraw() will not be called from this point on.
     */
    @Override
    protected void animStop() {
    }
    

    /**
     * The application is closing down.  Clean up any resources.
     */
    @Override
    protected void appStop() {
    }
    

    // ******************************************************************** //
    // Hack Control.
    // ******************************************************************** //

    /**
     * Set the hack to be displayed in this view.  This can be called
     * multiple times to change hacks.
     * 
     * @param   hack        The EyeCandy to display.
     */
    void setHack(EyeCandy hack) {
        eyeCandy = hack;
        
        if (screenWidth > 0 && screenHeight > 0)
            eyeCandy.setConfiguration(screenWidth, screenHeight, screenConfig);
    }
    
    
    // ******************************************************************** //
    // Animation Rendering.
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
     * @param   now         Current time in ms.
     */
    @Override
    protected void doUpdate(long now) {
        eyeCandy.update();
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
     * @param   now         Current time in ms.  Will be the same as that
     *                      passed to doUpdate(), if there was a preceeding
     *                      call to doUpdate().
     */
    @Override
    protected void doDraw(Canvas canvas, long now) {
        // Render the hack into the given canvas.
        eyeCandy.render(canvas, 0, 0);
    }


    // ******************************************************************** //
    // Class Data.
    // ******************************************************************** //

    // Debugging tag.
	@SuppressWarnings("unused")
	private static final String TAG = "Substrate";

	
	// ******************************************************************** //
	// Private Data.
	// ******************************************************************** //
    
    // Screen size and pixel configuration.
    private int screenWidth = 0;
    private int screenHeight = 0;
    private Bitmap.Config screenConfig = null;
    
	// The screen hack we're displaying.
	private EyeCandy eyeCandy = null;
    
}

