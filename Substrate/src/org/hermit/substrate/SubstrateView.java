
/**
 * Zen: create a zen garden on your phone.
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


package org.hermit.substrate;


import org.hermit.android.core.SurfaceRunner;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.view.KeyEvent;
import android.view.MotionEvent;


/**
 * A Zen garden view.  This class relies on the parent SurfaceRunner
 * class to do the bulk of the animation control.
 */
public class SubstrateView
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
    public SubstrateView(Context app) {
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
        // Create the bitmap we actually render to.  Make the Canvas which
        // draws into it -- used for drawing in the finger path.
        renderBitmap = getBitmap();

        // Create the drawing surface.
        surface = new Substrate(width, height, renderBitmap);
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
        // Update the surface.
        surface.doDraw(now);
        
        // Draw the surface's bitmap into the screen.
        canvas.drawBitmap(renderBitmap, 0, 0, null);
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
		return false;
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
 	
	// The substrate surface, where all the drawing happens.
	private Substrate surface = null;
    
    // Bitmap in which we maintain the current image of the garden,
	// and the Canvas for drawing into it.
	private Bitmap renderBitmap = null;
    
}

