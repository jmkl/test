
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


import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;


/**
 * Base class for algorithms which draw pretty pictures on screen.
 * This class is abstracted (as much as possible) to just draw into a Bitmap,
 * so that EyeCandy objects can be re-used in standalone apps, as live
 * wallpapers, etc.
 */
public abstract class EyeCandy
    implements SharedPreferences.OnSharedPreferenceChangeListener
{

    // ******************************************************************** //
    // Constructor.
    // ******************************************************************** //

	/**
	 * Create an EyeCandy instance.
	 */
    public EyeCandy() {
    }


    // ******************************************************************** //
    // Configuration.
    // ******************************************************************** //

    /**
     * Get the shared prefs name for this eye candy.
     * 
     * @return              Shared preferences name.
     */
    public abstract String getPrefsName();
    

    /**
     * Set the drawing canvas configuration.  This specifies the logical
     * wallpaper size, which may not match the screen size.
     * 
     * @param   width       The width of the canvas.
     * @param   height      The height of the canvas.
     * @param   config      Pixel configuration of the canvas.
     */
    void setConfiguration(int width, int height, Bitmap.Config config) {
        canvasWidth = width;
        canvasHeight = height;
        canvasConfig = config;
        
        // Create our backing bitmap.
        renderBitmap = Bitmap.createBitmap(width, height, config);
        
        // Create a Canvas for drawing, and a Paint to hold the drawing state.
        renderCanvas = new Canvas(renderBitmap);
        renderPaint = new Paint();
        
        onConfigurationSet(width, height, config);
        
        reset();
        numCycles = 0;
    }
    

    /**
     * This method is called to notify subclasses that the canvas
     * configuration has changed.  This specifies the logical wallpaper
     * size, which may not match the screen size.
     * 
     * @param   width       The width of the canvas.
     * @param   height      The height of the canvas.
     * @param   config      Pixel configuration of the canvas.
     */
    public abstract void onConfigurationSet(int width, int height, Bitmap.Config config);

    
    /**
     * Set the number of cycles this hack will run for before resetting.
     * 
     * @param   num         Maximum number of cycles to run for.  Zero means
     *                      run forever; maybe the hack will reset itself,
     *                      or doesn't need to.
     */
    void setMaxCycles(int num) {
        maxCycles = num;
    }

    
    // ******************************************************************** //
    // Animation Rendering.
    // ******************************************************************** //
    
    /**
     * Reset this eye candy back to a blank state.  This will be called
     * at start-up, and to reset back to an initial state when the cycle
     * limit is exceeded.
     */
    public abstract void reset();
    

    /**
     * Advance this eye candy, updating its state in renderBitmap.
     */
    public void update() {
        // If not set up yet, ignore it.
        if (renderBitmap == null)
            return;
        
        // See if we need to start over.
        if (numCycles++ >= maxCycles) {
            reset();
            numCycles = 0;
        }
   
        // Update the screen hack.
        doDraw();
    }


    /**
     * Update this screen hack into renderBitmap.
     */
    protected abstract void doDraw();


    /**
     * Draw the current frame of the application onto the screen.
     * 
     * <p>Applications must override this, and are expected to draw their
     * entire state into the provided canvas at the given offset.
     * 
     * @param   canvas      The Canvas to draw into.  We must re-draw the
     *                      whole screen.
     * @param   xoff        X offset to draw at, in pixels.
     * @param   yoff        Y offset to draw at, in pixels.
     */
    public void render(Canvas canvas, int xoff, int yoff) {
        canvas.drawBitmap(renderBitmap, xoff, yoff, null);
    }


    // ******************************************************************** //
    // Subclass Accessible Data.
    // ******************************************************************** //
    
    /**
     * The width of the screen we're drawing into.  Zero if not known yet.
     */
    protected int canvasWidth = 0;
    
    /**
     * The height of the screen we're drawing into.  Zero if not known yet.
     */
    protected int canvasHeight = 0;
    
    /**
     * Screen pixel configuration.  null if not known yet.
     */
    protected Bitmap.Config canvasConfig;

    /**
     * Bitmap in which we maintain the current image of the garden,
     * and the Canvas for drawing into it.  This is accessible to subclasses.
     */
    protected Bitmap renderBitmap = null;
    
    /**
     * A canvas initialized to draw into renderBitmap.  This is accessible
     * to subclasses.
     */
    protected Canvas renderCanvas = null;
    
    /**
     * A Paint made available as a convenience to subclasses.
     */
    protected Paint renderPaint = null;


    // ******************************************************************** //
    // Class Data.
    // ******************************************************************** //

    // Debugging tag.
	@SuppressWarnings("unused")
	private static final String TAG = "Substrate";


    // ******************************************************************** //
    // Private Data.
    // ******************************************************************** //
   
    // Number of cycles before we reset.
    private int maxCycles = 10000;
    
    // Number of cycles we've done in this run.
    private int numCycles = 0;

}

