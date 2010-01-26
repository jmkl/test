
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


package org.hermit.substrate.hacks;


import java.util.Random;

import org.hermit.substrate.EyeCandy;
import org.hermit.substrate.Palette;
import org.hermit.substrate.PollockPalette;

import net.goui.util.MTRandom;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.util.Log;


/**
 * Substrate: grow crystal-like lines on a computational substrate.  This
 * is a port of the code by J. Tarbell at http://complexification.net/.
 *
 * <p>Copyright © 2004 by J. Tarbell (complex@complexification.net).
 // Intersection Aggregate, {Software} Structures
 // j.tarbell   May, 2004
 // Albuquerque, New Mexico
 // complexification.net

 // commissioned by the Whitney ArtPort 
 // collaboration with Casey Reas, Robert Hodgin, William Ngan 
 * 
 *     "Modifications and extensions of these algorithms are encouraged.
 *      Please send me your experiences."
 */
public class InterAggregate
    extends EyeCandy
{

    // ******************************************************************** //
    // Public Constants.
    // ******************************************************************** //
    
    /**
     * Preferences name for preferences relating to this eye candy.
     */
    public static final String SHARED_PREFS_NAME = "interaggregate_settings";

    
    // ******************************************************************** //
    // Constructor.
    // ******************************************************************** //

    /**
     * Create a substrate drawing instance.
     */
    public InterAggregate() {
    }


    // ******************************************************************** //
    // Configuration.
    // ******************************************************************** //

    /**
     * Get the shared prefs name for this eye candy,
     * 
     * @return              Shared preferences name.
     */
    @Override
    public String getPrefsName() {
        return SHARED_PREFS_NAME;
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
    @Override
    public void onConfigurationSet(int width, int height, Bitmap.Config config) {
        colourPalette = new PollockPalette();
    }


    // ******************************************************************** //
    // Preferences.
    // ******************************************************************** //

    /**
     * Called when a shared preference is changed, added, or removed.
     * This may be called even if a preference is set to its existing value.
     *
     * @param   prefs       The SharedPreferences that received the change.
     * @param   key         The key of the preference that was changed. 
     */
    @Override
    public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
        int maxCycles = 4000;
        try {
            String sval = prefs.getString("maxCycles", "" + maxCycles);
            maxCycles = Integer.valueOf(sval);
        } catch (Exception e) {
            Log.e(TAG, "Pref: bad maxCycles");
        }
        setMaxCycles(maxCycles);
        Log.i(TAG, "Prefs: maxCycles " + maxCycles);

        try {
            String sval = prefs.getString("numDiscs", "" + numDiscs);
            numDiscs = Integer.valueOf(sval);
        } catch (Exception e) {
            Log.e(TAG, "Pref: bad numDiscs");
        }
        Log.i(TAG, "Prefs: numDiscs " + numDiscs);

        try {
            String sval = prefs.getString("discSize", "" + discMaxSize);
            discMaxSize = Integer.valueOf(sval);
        } catch (Exception e) {
            Log.e(TAG, "Pref: bad discSize");
        }
        discMinSize = discMaxSize / 15;
        Log.i(TAG, "Prefs: discSize " + discMinSize + "-" + discMaxSize);

        try {
            String sval = prefs.getString("sandGrains", "" + sandGrains);
            sandGrains = Integer.valueOf(sval);
        } catch (Exception e) {
            Log.e(TAG, "Pref: bad sandGrains");
        }
        Log.i(TAG, "Prefs: sandGrains " + sandGrains);

        reset();
    }


    // ******************************************************************** //
    // Control Methods.
    // ******************************************************************** //

    /**
     * Reset this eye candy back to a blank state.  This will be called
     * at start-up, and to reset back to an initial state when the cycle
     * limit is exceeded.
     */
    @Override
    protected void reset() {
        if (canvasWidth <= 0 || canvasHeight <= 0)
            return;

        Log.v(TAG, "Interag reset: " + numDiscs + " discs, " +
                                       discMinSize + "-" + discMaxSize);
//        framerate(30);

        if (discs == null || discs.length != numDiscs)
            discs = new Disc[numDiscs];
        
        // arrange linearly
        for (int i = 0; i < numDiscs; i++) {
            if (discs[i] == null)
                discs[i] = new Disc();
            float x = random(canvasWidth);
            float y = random(canvasHeight);
            float fy = random(-1.2f, 1.2f);
            float fx = random(-1.2f, 1.2f);
            float r = random(discMinSize, discMaxSize);
            discs[i].reset(i, x, y, fx, fy, r);
        }
        nextDisc = 0;

        // Clear to white.
        renderCanvas.drawColor(0xffffffff);
    }


    // ******************************************************************** //
    // Drawing.
    // ******************************************************************** //

    /**
     * Update this substrate into renderBitmap.
     * 
     * @return              The number of cycles completed during this update.
     *                      May be zero, one, or more.
     */
    @Override
    protected int doDraw() {
        // move discs
        long start = System.currentTimeMillis();
        long time = 0;
        int cycles = 0;
        
        while (time < RUN_TIME) {
            final int c = nextDisc;
            if (++nextDisc >= numDiscs) {
                nextDisc = 0;
                ++cycles;
            }
            
            // Update one disc.
            discs[c].move();
            discs[c].render();
            
            time = System.currentTimeMillis() - start;
        }
        
        return cycles;
    }


    // ******************************************************************** //
    // Disc Class.
    // ******************************************************************** //

    private class Disc {
        // index identifier
        private int id;
        // position
        private float x, y;
        // radius
        private float r;
        // destination radius
        private float dr;
        // velocity
        private float vx, vy;

        // sand painters
        private static final int NUM_SANDS = 3;
        private SandPainter[] sands = null;

        Disc() {
            // create sand painters
            sands = new SandPainter[NUM_SANDS];
            for (int n = 0; n < NUM_SANDS; ++n)
                sands[n] = new SandPainter();
        }

        void reset(int Id, float X, float Y, float Vx, float Vy, float R) {
            // construct
            id=Id;
            x=X;
            y=Y;
            vx=Vx;
            vy=Vy;
            r=0;
            dr=R;
            
            for (int n = 0; n < NUM_SANDS; ++n)
                sands[n].reset();
        }

        void move() {
            // grow to destination radius
            if (r < dr)
                r += 0.1f;

            // add velocity to position
            x += vx;
            y += vy;
            
            // bound check
            if (x + r < 0)
                x += canvasWidth + r + r;
            if (x - r > canvasWidth)
                x -= canvasWidth + r + r;
            if (y + r < 0)
                y += canvasHeight + r + r;
            if (y - r > canvasHeight)
                y -= canvasHeight + r + r;
        }

        void render() {
            // find intersecting points with all ascending discs
            for (int n = id + 1; n < numDiscs; n++) {
                // find distance to other disc
                final float dx = discs[n].x - x;
                final float dy = discs[n].y - y;
                final float d = (float) Math.sqrt(dx * dx + dy * dy);
                
                // Test for intersection but not complete containment.
                if (d < (discs[n].r + r) && d > Math.abs(discs[n].r - r)) {
                    // find solutions
                    final float a = (r*r - discs[n].r*discs[n].r + d*d) / (2*d);
                    final float idx = (discs[n].x - x) / d;
                    final float idy = (discs[n].y - y) / d;
                    
                    final float p2x = x + a * idx;
                    final float p2y = y + a * idy;

                    final float h = (float) Math.sqrt(r * r - a * a);

                    final float p3ax = p2x + h * idy;
                    final float p3ay = p2y - h * idx;

                    final float p3bx = p2x - h * idy;
                    final float p3by = p2y + h * idx;

                    for (int s = 0; s < NUM_SANDS; s++)
                        sands[s].render(p3ax, p3ay, p3bx, p3by);
                }
            }
        }
        
    }


    // ******************************************************************** //
    // SandPainter Class.
    // ******************************************************************** //

    private class SandPainter {

        SandPainter() {
            c = colourPalette.getRandom();
            p = random(1.0f);
            g = random(0.01f, 0.1f);
        }

        private void reset() {
            c = colourPalette.getRandom();
            p = random(1.0f);
            g = random(0.01f, 0.1f);
        }

        void render(float x, float y, float ox, float oy) {
            // modulate gain
            g += random(-0.050f, 0.050f);
            float maxg = 0.22f;
            if (g < 0)
                g = 0;
            if (g > maxg)
                g = maxg;

            p += random(-0.050f, 0.050f);
            if (p < 0)
                p = 0;
            if (p > 1.0)
                p = 1.0f;

            // draw painting sweeps
            renderPaint.setColor(c);
            float w = g / (sandGrains - 1);
            for (int i = 0; i < sandGrains; ++i) {
                final float ssiw1 = (float) Math.sin(p + Math.sin(i * w));
                final float ssiw2 = (float) Math.sin(p - Math.sin(i * w));
                final float px1 = ox + (x - ox) * ssiw1;
                final float py1 = oy + (y - oy) * ssiw1;
                final float px2 = ox + (x - ox) * ssiw2;
                final float py2 = oy + (y - oy) * ssiw2;
                final float a = 0.1f - i / (sandGrains * 10.0f);
                
                renderPaint.setAlpha(Math.round(a * 256));
                renderCanvas.drawPoint(px1, py1, renderPaint);
                renderCanvas.drawPoint(px2, py2, renderPaint);
            }
        }

        // Colour for this SandPainter.
        private int c;
        
        // 
        private float p;
        
        // Gain; used to modulate the alpha for a "fuzzy" effect.
        private float g;
    }


    // ******************************************************************** //
    // Utility Methods.
    // ******************************************************************** //

    private static final float random(float a) {
        return MT_RANDOM.nextFloat() * a;
    }

    private static final float random(float a, float b) {
        return MT_RANDOM.nextFloat() * (b - a) + a;
    }


    // ******************************************************************** //
    // Class Data.
    // ******************************************************************** //

    // Debugging tag.
    @SuppressWarnings("unused")
    private static final String TAG = "Substrate";

    // Time in ms to run for during each update.
    private static final int RUN_TIME = 80;

    // Random number generator.  We use a Mersenne Twister,
    // which is a high-quality and fast implementation of java.util.Random.
    private static final Random MT_RANDOM = new MTRandom();


    // ******************************************************************** //
    // Private Data.
    // ******************************************************************** //

    // Colour palette we're using.
    private Palette colourPalette = null;

    // The number of discs on the move.
    private int numDiscs = 70;
    
    // Min and max disc sizes.
    private int discMinSize = 20;
    private int discMaxSize = 300;

    // The discs.
    private Disc[] discs;
    
    // Index of the next disc to be updated.  We don't update all the discs
    // every time for performance reasons, so this keeps our place in the
    // list between updates.
    private int nextDisc = 0;

    // Number of grains of sand to paint.
    private int sandGrains = 11;

}

