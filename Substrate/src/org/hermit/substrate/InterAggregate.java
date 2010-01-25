
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

import net.goui.util.MTRandom;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Paint;
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
        int maxCycles = 10000;
        try {
            String sval = prefs.getString("maxCycles", null);
            maxCycles = Integer.valueOf(sval);
        } catch (Exception e) {
            Log.e(TAG, "Pref: bad maxCycles");
        }
        setMaxCycles(maxCycles);
        Log.i(TAG, "Prefs: maxCycles " + maxCycles);

        try {
            String sval = prefs.getString("sandGrains", null);
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
    public void reset() {
        if (canvasWidth <= 0 || canvasHeight <= 0)
            return;

//        framerate(30);

        discs = new Disc[numDiscs];

        // arrange linearly
        for (int i = 0; i < numDiscs; i++) {
            float x = random(canvasWidth);
            float y = random(canvasHeight);
            float fy = random(-1.2f, 1.2f);
            float fx = random(-1.2f, 1.2f);
            float r = random(minSize, maxSize);
            discs[i] = new Disc(i, x, y, fx, fy, r);
        }

        // Clear to white.
        renderCanvas.drawColor(0xffffffff);
    }


    // ******************************************************************** //
    // Drawing.
    // ******************************************************************** //

    /**
     * Update this substrate into renderBitmap.
     */
    @Override
    protected void doDraw() {
        // move discs
        for (int c = 0; c < numDiscs; c++) {
            discs[c].move();
            discs[c].render();
        }
    }


    // ******************************************************************** //
    // Disc Class.
    // ******************************************************************** //

    private class Disc {
        // index identifier
        int id;
        // position
        float x, y;
        // radius
        float r;
        // destination radius
        float dr;
        // velocity
        float vx, vy;

        // sand painters
        int numsands = 3;
        SandPainter[] sands = new SandPainter[numsands];

        Disc(int Id, float X, float Y, float Vx, float Vy, float R) {
            // construct
            id=Id;
            x=X;
            y=Y;
            vx=Vx;
            vy=Vy;
            r=0;
            dr=R;

            // create sand painters
            for (int n=0;n<numsands;n++) {
                sands[n] = new SandPainter();
            }
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
        }

        void draw() {
            renderPaint.setColor(0xff000000);
            renderPaint.setAlpha(50);
            renderPaint.setStyle(Paint.Style.STROKE);
            renderCanvas.drawCircle(x, y, r, renderPaint);
        }

        void render() {
            // find intersecting points with all ascending discs
            for (int n=id+1;n<numDiscs;n++) {
                // find distance to other disc
                float dx = discs[n].x-x;
                float dy = discs[n].y-y;
                float d = (float) Math.sqrt(dx*dx+dy*dy);
                // intersection test
                if (d < (discs[n].r + r)) {
                    // complete containment test
                    if (d > Math.abs(discs[n].r-r)) {
                        // find solutions
                        float a = (r*r - discs[n].r*discs[n].r + d*d ) / (2*d);

                        float p2x = x + a*(discs[n].x - x)/d;
                        float p2y = y + a*(discs[n].y - y)/d;

                        float h = (float) Math.sqrt(r*r - a*a);

                        float p3ax = p2x + h*(discs[n].y - y)/d;
                        float p3ay = p2y - h*(discs[n].x - x)/d;

                        float p3bx = p2x - h*(discs[n].y - y)/d;
                        float p3by = p2y + h*(discs[n].x - x)/d;

                        for (int s = 0; s < numsands; s++)
                            sands[s].render(p3ax, p3ay, p3bx, p3by);
                    }
                }
            }
        }

        void move() {
            // add velocity to position
            x += vx;
            y += vy;
            
            // grow to destination radius
            if (r < dr)
                r += 0.1;

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
    }


    // ******************************************************************** //
    // SandPainter Class.
    // ******************************************************************** //

    class SandPainter {

        SandPainter() {
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
            float w = g / (sandGrains - 1);
            for (int i = 0; i < sandGrains; ++i) {
                final float ssiw1 = (float) Math.sin(p + Math.sin(i * w));
                final float ssiw2 = (float) Math.sin(p - Math.sin(i * w));
                final float px1 = ox + (x - ox) * ssiw1;
                final float py1 = oy + (y - oy) * ssiw1;
                final float px2 = ox + (x - ox) * ssiw2;
                final float py2 = oy + (y - oy) * ssiw2;
                final float a = 0.1f - i / (sandGrains * 10.0f);
                
                renderPaint.setColor(c);
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

    private float random(float a) {
        return MT_RANDOM.nextFloat() * a;
    }

    private float random(float a, float b) {
        return MT_RANDOM.nextFloat() * (b - a) + a;
    }


    // ******************************************************************** //
    // Class Data.
    // ******************************************************************** //

    // Debugging tag.
    @SuppressWarnings("unused")
    private static final String TAG = "Substrate";

    // Random number generator.  We use a Mersenne Twister,
    // which is a high-quality and fast implementation of java.util.Random.
    private static final Random MT_RANDOM = new MTRandom();


    // ******************************************************************** //
    // Private Data.
    // ******************************************************************** //

    // Colour palette we're using.
    private Palette colourPalette = null;

    // Number of grains of sand to paint.
    private int sandGrains = 11;

    // The number of discs on the move.
    private int numDiscs = 100;
    
    // Min and max disc sizes.
    private int minSize = 40;
    private int maxSize = 600;

    // The discs.
    private Disc[] discs;

}

