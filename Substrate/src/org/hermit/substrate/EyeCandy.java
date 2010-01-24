
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


import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;


/**
 * Base class for algorithms which draw pretty pictures on screen.
 */
public abstract class EyeCandy
{

    // ******************************************************************** //
    // Constructor.
    // ******************************************************************** //

	/**
	 * Create an EyeCandy instance.
     * 
     * @param   width       The width of the substrate.
     * @param   height      The height of the substrate.
     * @param   config      Pixel configuration of the screen.
	 */
    public EyeCandy(int width, int height, Bitmap.Config config) {
        // Create our backing bitmap.
        renderBitmap = Bitmap.createBitmap(width, height, config);
        
        // Create a Canvas for drawing, and a Paint to hold the drawing state.
        renderCanvas = new Canvas(renderBitmap);
        renderPaint = new Paint();
    }


    // ******************************************************************** //
    // Animation Rendering.
    // ******************************************************************** //
    
    /**
     * Draw the current frame of the application.
     * 
     * <p>Applications must override this, and are expected to draw the
     * entire screen into the provided canvas.
     * 
     * @param   canvas      The Canvas to draw into.  We must re-draw the
     *                      whole screen.
     */
    public void render(Canvas canvas) {
        // Update the screen hack.
        doDraw();
        
        // Draw the surface's bitmap into the screen.
        canvas.drawBitmap(renderBitmap, 0, 0, null);
    }


    /**
     * Update this screen hack into renderBitmap.
     */
    protected abstract void doDraw();


    // ******************************************************************** //
    // Subclass Accessible Data.
    // ******************************************************************** //
   
    /**
     * Bitmap in which we maintain the current image of the garden,
     * and the Canvas for drawing into it.  This is accessible to subclasses.
     */
    protected Bitmap renderBitmap = null;
    
    /**
     * A canvas initialized to draw into renderBitmap.  This is accessible
     * to subclasses.
     */
    protected final Canvas renderCanvas;
    
    /**
     * A Paint made available as a convenience to subclasses.
     */
    protected final Paint renderPaint;


    // ******************************************************************** //
    // Class Data.
    // ******************************************************************** //

    // Debugging tag.
	@SuppressWarnings("unused")
	private static final String TAG = "Substrate";

}

