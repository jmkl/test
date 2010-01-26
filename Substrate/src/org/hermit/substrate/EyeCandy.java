
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
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.LightingColorFilter;
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
        
        // Create a Canvas for the subclass to draw in, and a Paint to
        // hold the drawing state.
        renderCanvas = new Canvas(renderBitmap);
        renderPaint = new Paint();

        // Our own Paint for drawing to the screen.
        screenPaint = new Paint();

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
    protected void setMaxCycles(int num) {
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
    protected abstract void reset();
    

    /**
     * Advance this eye candy, updating its state in renderBitmap.
     */
    protected void update() {
        // If not set up yet, ignore it.
        if (renderBitmap == null)
            return;
   
        // If we're fading out, do nothing.
        if (fadeCycles > 0)
            return;
        
        // Update the screen hack.
        numCycles += doDraw();
        
        // See if we need to start fading out the image.
        if (numCycles >= maxCycles)
            fadeCycles = FADE_CYCLES;
    }


    /**
     * Update this screen hack into renderBitmap.
     * 
     * @return              The number of cycles completed during this update.
     *                      May be zero, one, or more.
     */
    protected abstract int doDraw();


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
    protected void render(Canvas canvas, int xoff, int yoff) {
        if (fadeCycles > 0) {
            float frac = (float) fadeCycles / (float) FADE_CYCLES;
            int mul = Math.round(255 * frac);
            int add = 255 - mul;
            ColorFilter filter = new LightingColorFilter(
                            Color.rgb(mul, mul, mul), Color.rgb(add, add, add));
            screenPaint.setColorFilter(filter);
            canvas.drawBitmap(renderBitmap, xoff, yoff, screenPaint);
            
            // If we're done fading, start the next iteration.
            if (--fadeCycles == 0) {
                reset();
                numCycles = 0;
            }
        } else
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
	
	// The number of cycles over which to fade the image out when restarting.
	private static final int FADE_CYCLES = 150;


    // ******************************************************************** //
    // Private Data.
    // ******************************************************************** //
   
    // Number of cycles before we reset.
    private int maxCycles = 10000;
    
    // Number of cycles we've done in this run.
    private int numCycles = 0;

    // Paint we use for drawing the renderBitmap to the screen.
    private Paint screenPaint = null;

    // If we're fading out, this is the number of cycles remaining
    // in the fade.  If zero, we're not fading.
    private int fadeCycles = 0;

}

