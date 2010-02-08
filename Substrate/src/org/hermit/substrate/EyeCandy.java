
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


import java.util.Random;

import org.hermit.substrate.palettes.BluesPalette;
import org.hermit.substrate.palettes.CaramelPalette;
import org.hermit.substrate.palettes.FirecodePalette;
import org.hermit.substrate.palettes.FlowerPalette;
import org.hermit.substrate.palettes.OrnatePalette;
import org.hermit.substrate.palettes.PollockPalette;
import org.hermit.substrate.palettes.SandPalette;
import org.hermit.substrate.palettes.TropicalPalette;

import net.goui.util.MTRandom;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.LightingColorFilter;
import android.graphics.Paint;
import android.util.Log;


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
    // Public Constants.
    // ******************************************************************** //
    
    /**
     * Preferences name for the common EyeCandy preferences.
     */
    public static final String COMMON_PREFS_NAME = "eyecandy_settings";

    
    // ******************************************************************** //
    // Constructor.
    // ******************************************************************** //

	/**
	 * Create an EyeCandy instance.
	 * 
	 * @param  context      Our application context.
	 */
    protected EyeCandy(Context context) {
        appResources = context.getResources();

        // Register for changes in the subclass's preferences.
        SharedPreferences prefs = context.getSharedPreferences(getPrefsName(), 0);
        prefs.registerOnSharedPreferenceChangeListener(this);
        onSharedPreferenceChanged(prefs, null);
    }


    // ******************************************************************** //
    // Preferences.
    // ******************************************************************** //

    /**
     * Handle changes in our preferences.
     *
     * @param   prefs       The SharedPreferences to read.
     * @param   key         The key of the preference that was changed. 
     */
    @Override
    public void onSharedPreferenceChanged(SharedPreferences prefs,
            String key)
    {
        Log.i(TAG, "Child Prefs: " + key);

        if (key == null || key.equals("bgColour")) try {
            String dflt = String.format("#%08x", backgroundColor);
            String sval = prefs.getString("bgColour", dflt);
            backgroundColor = Color.parseColor(sval);
            Log.i(TAG, "Prefs: bgColour " + String.format("#%08x", backgroundColor));
        } catch (Exception e) {
            Log.e(TAG, "Pref: bad bgColour");
        }

        if (key == null || key.equals("colourPalette")) try {
            String sval = prefs.getString("colourPalette", "" + colourPaletteId);
            colourPaletteId = Integer.parseInt(sval);
            Log.i(TAG, "Prefs: colourPalette " + colourPaletteId);
        } catch (Exception e) {
            Log.e(TAG, "Pref: bad colourPalette");
        }

        if (key == null || key.equals("animSpeed")) try {
            String sval = prefs.getString("animSpeed", null);
            int ival = Integer.parseInt(sval);
            runTime = 25 + (2 - ival) * 10;
            sleepTime = 20 + ival * 20;
            Log.i(TAG, "Prefs: animSpeed: run=" + runTime + " sleep=" + sleepTime);
        } catch (Exception e) {
            Log.e(TAG, "Pref: bad animSpeed");
        }

        // Tell the subclass to read its prefs.
        readPreferences(prefs, key);

        restartHack();
    }


    /**
     * Read our shared preferences from the given preferences object.
     * Subclasses must implement this to read their own preferences.
     *
     * @param   prefs       The SharedPreferences to read.
     * @param   key         The key of the preference that was changed. 
     */
    protected abstract void readPreferences(SharedPreferences prefs, String key);
    

    // ******************************************************************** //
    // Configuration.
    // ******************************************************************** //

    /**
     * Get the shared prefs name for this eye candy.
     * 
     * @return              Shared preferences name.
     */
    protected abstract String getPrefsName();
    

    /**
     * Set the drawing canvas configuration.  This specifies the logical
     * wallpaper size, which may not match the screen size.
     * 
     * <p>This method is used by our framework to tell us our config.
     * Subclasses override
     * {@link #onConfigurationSet(int, int, android.graphics.Bitmap.Config)}.
     * 
     * @param   width       The width of the canvas.
     * @param   height      The height of the canvas.
     * @param   config      Pixel configuration of the canvas.
     */
    final void setConfiguration(int width, int height, Bitmap.Config config) {
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
        
        restartHack();
    }
    

    /**
     * This method is called to notify subclasses that the canvas
     * configuration has changed.  This specifies the logical wallpaper
     * size, which may not match the screen size.
     * 
     * <p>Subclasses should implement this to be informed of their canvas
     * size.
     * 
     * @param   width       The width of the canvas.
     * @param   height      The height of the canvas.
     * @param   config      Pixel configuration of the canvas.
     */
    protected abstract void onConfigurationSet(int width, int height, Bitmap.Config config);

    
    /**
     * Get the time in ms to sleep between updates.
     * 
     * @return              Sleep time in ms.
     */
    protected long getSleepTime() {
        return sleepTime;
    }
    

    /**
     * Set the number of cycles this hack will run for before resetting.
     * Subclasses can call this to tell us how long they wish to run
     * for.
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
    private void restartHack() {
        // Set the colour palette for this run.
        int i = colourPaletteId < 0 ?
                random(COLOUR_PALETTES.length) : colourPaletteId;
        colourPalette = COLOUR_PALETTES[i];
                
        reset();
        
        numCycles = 0;
        fadeCycles = 0;
        lastCycles = 0;
    }
    

    /**
     * Reset this eye candy back to a blank state.  This will be called
     * at start-up, and to reset back to an initial state when the cycle
     * limit is exceeded.
     */
    protected abstract void reset();
    

    /**
     * Advance this eye candy, updating its state in renderBitmap.
     */
    final void update() {
        // If not set up yet, ignore it.
        if (renderBitmap == null)
            return;
   
        // If we're fading out, do nothing.
        if (fadeCycles > 0)
            return;
        
        // Update the screen hack.  Run as many mini-iterations as we can in a
        // reasonable time, so we don't block the home screen's responsiveness.
        long start = System.currentTimeMillis();
        long time = 0;
        while (time < runTime) {
            numCycles = iterate(numCycles);
            time = System.currentTimeMillis() - start;
            if (numCycles / 100 > lastCycles) {
                Log.i(TAG, "C: " + numCycles);
                lastCycles = numCycles / 100;
            }
        }
        
        // See if we need to start fading out the image.
        if (maxCycles > 0 && numCycles >= maxCycles)
            fadeCycles = FADE_CYCLES;
    }

    private int lastCycles;


    /**
     * Run one iteration of this screen hack, updating its appearance
     * into renderBitmap.  The work done should be restricted to a small
     * unit of work, ideally less than RUN_TIME, in order to not affect
     * the responsiveness of the home screen.
     * 
     * <p>This method will be called multiple times, to accumulate about
     * RUN_TIME ms of work per update.  Hence each call need only do one
     * small work unit.
     * 
     * @param   cycles      The total number of complete algorithm cycles
     *                      completed to date.
     * @return              The number of complete algorithm cycles
     *                      completed following this update.
     *                      May or may not be more than cycles.
     */
    protected abstract int iterate(int cycles);


    /**
     * Draw the current frame of the application onto the screen.
     * We are expected to draw our entire state into the provided
     * canvas at the given offset.
     * 
     * @param   canvas      The Canvas to draw into.  We must re-draw the
     *                      whole screen.
     * @param   xoff        X offset to draw at, in pixels.
     * @param   yoff        Y offset to draw at, in pixels.
     */
    final void render(Canvas canvas, int xoff, int yoff) {
        if (fadeCycles > 0) {
            if (fadeCycles > FADE_CYCLES / 2)
                canvas.drawBitmap(renderBitmap, xoff, yoff, null);
            else {
                float alpha = (float) fadeCycles / ((float) FADE_CYCLES / 2);
                screenPaint.setColorFilter(fadeFilter(alpha));
                canvas.drawBitmap(renderBitmap, xoff, yoff, screenPaint);
            }
            
            // If we're done fading, start the next iteration.
            if (--fadeCycles == 0)
                restartHack();
        } else
            canvas.drawBitmap(renderBitmap, xoff, yoff, null);
    }
    
    
    /**
     * Create a ColorFilter which fades an image to backgroundColor
     * according to the given alpha.
     * 
     * @param   alpha       Amount of the original image to preserve; 1
     *                      means full original, 0 means all backgroundColor.
     * @return              The created ColorFilter.
     */
    private final ColorFilter fadeFilter(float alpha) {
        int mul = Math.round(255 * alpha);
        
        int addR = Math.round(Color.red(backgroundColor) * (1 - alpha));
        int addG = Math.round(Color.green(backgroundColor) * (1 - alpha));
        int addB = Math.round(Color.blue(backgroundColor) * (1 - alpha));
        return new LightingColorFilter(
                        Color.rgb(mul, mul, mul), Color.rgb(addR, addG, addB));
    }


    // ******************************************************************** //
    // Utility Methods.
    // ******************************************************************** //

    /**
     * Return a random integer in the range [0-a[.
     * 
     * @param   a           Upper (non-inclusive) bound.
     * @return              A random int in [0-a[.
     */
    protected final int random(int a) {
        return randomGen.nextInt(a);
    }


    /**
     * Return a random float in the range [0-a[.
     * 
     * @param   a           Upper (non-inclusive) bound.
     * @return              A random float in [0-a[.
     */
    protected final float random(float a) {
        return randomGen.nextFloat() * a;
    }


    /**
     * Return a random float in the range [a-b[.
     * 
     * @param   a           Lower (inclusive) bound.
     * @param   b           Upper (non-inclusive) bound.
     * @return              A random float in [a-b[.
     */
    protected final float random(float a, float b) {
        return randomGen.nextFloat() * (b - a) + a;
    }


    /**
     * Return a random boolean.
     * 
     * @return              A random boolean.
     */
    protected final boolean brandom() {
        return randomGen.nextBoolean();
    }


    /**
     * Load an image from resources.
     * 
     * @param   resId       Resource ID of the image to load.
     * @return              The image.
     */
    protected final Bitmap loadImage(int resId) {
        return BitmapFactory.decodeResource(appResources, resId);
    }


    // ******************************************************************** //
    // Class Data.
    // ******************************************************************** //

    // Debugging tag.
    @SuppressWarnings("unused")
    private static final String TAG = "Substrate";

    // The number of cycles over which to fade the image out when restarting.
    // For the first 50% of the time, we show the image static, so the user
    // can admire it.
    private static final int FADE_CYCLES = 300;
    
    // Our colour palettes.
    private Palette[] COLOUR_PALETTES = {
            new PollockPalette(),
            new BluesPalette(),
            new CaramelPalette(),
            new FirecodePalette(),
            new FlowerPalette(),
            new OrnatePalette(),
            new SandPalette(),
            new TropicalPalette(),
    };


    // ******************************************************************** //
    // Subclass Accessible Data.
    // ******************************************************************** //

    /**
     * Random number generator, made available as a convenience to
     * subclasses.  We use a Mersenne Twister, which is a high-quality
     * and fast implementation of java.util.Random.
     */
    protected static final Random randomGen = new MTRandom();

    /**
     * The width of the canvas we're drawing into.  Typically not the
     * physical screen size, to allow for panning.  Zero if not known yet.
     */
    protected int canvasWidth = 0;
    
    /**
     * The height of the canvas we're drawing into.  Typically not the
     * physical screen size, to allow for panning.  Zero if not known yet.
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
    
    /**
     * The background color for this hack.
     */
    protected int backgroundColor = 0xffffffff;

    /**
     * The colour palette index for this hack.
     */
    protected int colourPaletteId = 0;

    /**
     * The colour palette for this hack.
     */
    protected Palette colourPalette = COLOUR_PALETTES[0];


    // ******************************************************************** //
    // Private Data.
    // ******************************************************************** //

    // Application resources.
    private final Resources appResources;

    // Time in ms to run for during each update.
    private long runTime = 35;

    // The time in ms to sleep between updates.
    private long sleepTime = 40;

    // Number of cycles before we reset.  Zero means run forever.
	// Subclasses should generally override this with something appropriate.
    private int maxCycles = 10000;
    
    // Number of cycles we've done in this run.
    private int numCycles = 0;

    // Paint we use for drawing the renderBitmap to the screen.
    private Paint screenPaint = null;

    // If we're fading out, this is the number of cycles remaining
    // in the fade.  If zero, we're not fading.
    private int fadeCycles = 0;

}

