
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


import org.hermit.substrate.EyeCandy;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;


/**
 * Guts: a composition of many hundreds of gut instances rendered
 * simultaneously in a radial fashion.  This is a port of the code
 * by J. Tarbell at http://complexification.net/.
 *
 * <p>By j.tarbell, July, 2004<br>
 * Albuquerque, New Mexico<br>
 * complexification.net<br>
 * Copyright © 2003 by J. Tarbell (complex@complexification.net).
 * 
 * <p>"Modifications and extensions of these algorithms are encouraged.
 * Please send me your experiences."
 */
public class Guts
    extends EyeCandy
{

    // ******************************************************************** //
    // Public Constants.
    // ******************************************************************** //

    /**
     * Preferences name for preferences relating to this eye candy.
     */
    public static final String SHARED_PREFS_NAME = "guts_settings";


    // ******************************************************************** //
    // Constructor.
    // ******************************************************************** //

    /**
     * Create an instance.
     * 
     * @param  context      Our application context.
     */
    public Guts(Context context) {
        super(context);

        setCycles(2000, 4000, 8000);
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
    protected String getPrefsName() {
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
    protected void onConfigurationSet(int width, int height, Bitmap.Config config) {
    }


    // ******************************************************************** //
    // Preferences.
    // ******************************************************************** //

    /**
     * Read our shared preferences from the given preferences object.
     * Subclasses must implement this to read their own preferences.
     *
     * @param   prefs       The SharedPreferences to read.
     * @param   key         The key of the preference that was changed. 
     */
    @Override
    protected void readPreferences(SharedPreferences prefs, String key) {
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

        // Make the diameter the average of the screen dimensions.
        diameter = (canvasWidth + canvasHeight) / 2;

        // Make or re-initialize all of the paths.
        if (cpaths == null || cpaths.length != numPaths) {
            cpaths = new CPath[numPaths];
            for (int i = 0; i < numPaths; ++i)
                cpaths[i] = new CPath(i);
        }

        for (int i = 0; i < numPaths; ++i)
            cpaths[i].reset();
        nextPath = 0;

        // Clear to white.
        renderCanvas.drawColor(backgroundColor);
    }


    // ******************************************************************** //
    // Drawing.
    // ******************************************************************** //

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
    @Override
    protected int iterate(int cycles) {
        final int c = nextPath;
        if (++nextPath >= numPaths) {
            nextPath = 0;
            ++cycles;
        }

        cpaths[c].grow();

        return cycles;
    }


    // ******************************************************************** //
    // CPath Class.
    // ******************************************************************** //

    private class CPath {

        CPath(int Id) {
            // construct
            id = Id;

            // create sand painters
            sandGut = new SandPainterOne();
            for (int s = 0; s < NUM_SANDS; ++s) {
                sandsLeft[s] = new SandPainterInside();
                sandsRight[s] = new SandPainterInside();
                sandsCenter[s] = new SandPainterOutside();
            }
        }

        void reset() {
            // Reset the sand painters except the center sands.
            sandGut.reset(0xff000000);
            for (int s = 0; s < NUM_SANDS; ++s) {
                sandsLeft[s].reset(0xff000000);
                sandsRight[s].reset(0xff000000);
            }

            // Set up our radius, and set up the center sand painter.
            float d = random(diameter / 2);
            float t = random(TWO_PI);
            x = d * (float) Math.cos(t);
            y = d * (float) Math.sin(t);
            int ci = (int) (colourPalette.size() * 2.0f * d / diameter);
            for (int s = 0; s < NUM_SANDS; s++)
                sandsCenter[s].reset(colourPalette.get(ci));

            v = 0.5f;
            a = random(TWO_PI);
            grth = 0.1f;
            gv = 1.2f;
            pt = (int) Math.pow(2, 1 + id % 3);
            time = 0;
            tdv = random(0.1f, 0.5f);
            tdvm = random(1.0f, 100.0f);
        }

        void grow() {
            time += random(4.0f);
            x += v * (float) Math.cos(a);
            y += v * (float) Math.sin(a);

            // rotational meander
            av = 0.1f * (float) Math.sin(time * tdv) +
                 0.1f * (float) Math.sin(time * tdv / tdvm);
            while (Math.abs(av) > HALF_PI / grth)
                av *= 0.73;
            a += av;

            // randomly increase and decrease in girth (thickness)      
            grth += gv;
            gv += random(-0.15f, 0.12f);
            if (grth < 6) {
                grth = 6;
                gv *= 0.9f;
            } else if (grth > 26) {
                grth = 26;
                gv *= 0.8f;
            }
            draw();
        }

        void draw() {
            // calculate distance
            final float d = (float) Math.sqrt(x * x + y * y);
            final float t = (float) Math.atan2(y, x);

            // draw each petal
            for (int p = 0; p < pt; ++p) {
                // calculate actual angle
                final float pang = p * (TWO_PI / pt);
                final float at = t + pang;
                final float ad = a + pang;

                // calculate actual xy
                final float ax = canvasWidth / 2 + d * (float) Math.cos(at);
                final float ay = canvasHeight / 2 + d * (float) Math.sin(at);

                // calculate girth markers
                final float cx = 0.5f * grth * (float) Math.cos(ad - HALF_PI);
                final float cy = 0.5f * grth * (float) Math.sin(ad - HALF_PI);

                // draw points
                // paint background white
                renderPaint.setColor(0xffffffff);
                for (int s = 0; s < grth * 2; ++s) {
                    float dd = random(-0.9f, 0.9f);
                    renderCanvas.drawPoint(ax + dd * cx, ay + dd * cy, renderPaint);
                }
                for (int s = 0; s < NUM_SANDS; ++s) {
                    final float px1 = ax + cx * 0.6f;
                    final float py1 = ay + cy * 0.6f;
                    final float px2 = ax - cx * 0.6f;
                    final float py2 = ay - cy * 0.6f;
                    sandsCenter[s].render(px1, py1, px2, py2);
                    sandsLeft[s].render(px1, py1, ax + cx, ay + cy);
                    sandsRight[s].render(px2,  py2, ax - cx, ay - cy);
                }
                
                // paint crease enhancement
                sandGut.render(ax + cx, ay + cy, ax - cx, ay - cy);
            }
        }

        // index identifier
        private int id;
        // position
        private float x,y;
        // angle
        private float a, av;
        private float v;
        private float tdv, tdvm;
        private int time;
        // petals
        private int pt;
        // girth
        private float grth, gv;
        // sand painters
        private static final int NUM_SANDS = 3;
        private SandPainter[] sandsCenter = new SandPainter[NUM_SANDS];
        private SandPainter[] sandsLeft = new SandPainter[NUM_SANDS];
        private SandPainter[] sandsRight = new SandPainter[NUM_SANDS];
        private SandPainter sandGut;
    }


    // ******************************************************************** //
    // SandPainter Class.
    // ******************************************************************** //

    private abstract class SandPainter {

        SandPainter() {
        }

        void reset(int C) {
            c = C;
            g = random(0f, HALF_PI);
        }

        void render(float x, float y, float ox, float oy) {
            // modulate gain
            modulateGain();
            if (g < 0.0f)
                g = 0.0f;
            else if (g > HALF_PI)
                g = HALF_PI;

            renderBody(x, y, ox, oy);
        }

        void modulateGain() {
            g += random(-0.050f, 0.050f);
        }
        
        abstract void renderBody(float x, float y, float ox, float oy);
        
        int c;
        float g;
        
    }


    private class SandPainterOne extends SandPainter {

        SandPainterOne() {
        }

        @Override
        void modulateGain() {
            g += random(-0.9f, 0.5f);
        }
        
        @Override
        void renderBody(float x, float y, float ox, float oy) {
            // calculate grains by distance
            //int grains = int(sqrt((ox-x)*(ox-x)+(oy-y)*(oy-y)));
            int grains = 15;

            // lay down grains of sand (transparent pixels)
            renderPaint.setColor(c);
            float w = g / (grains - 1);
            for (int i = 0; i < grains; ++i) {
                final float a = 0.15f - i / (grains * 10.0f + 10f);

                // paint one side
                final float tex = (float) Math.sin(i * w); //HALF_PI*(cos(i*w*PI)+1);
                final float lex = (float) Math.sin(tex); //(cos(tex*PI)+1)*0.5;
                final float px = ox + (x - ox) * lex;
                final float py = oy + (y - oy) * lex;
                renderPaint.setAlpha(Math.round(a * 256));
                renderCanvas.drawPoint(px, py, renderPaint);
            }
        }
        
    }


    private class SandPainterInside extends SandPainter {

        SandPainterInside() {
        }

        @Override
        void renderBody(float x, float y, float ox, float oy) {
            // calculate grains by distance
            //int grains = int(sqrt((ox-x)*(ox-x)+(oy-y)*(oy-y)));
            int grains = 6;

            // lay down grains of sand (transparent pixels)
            renderPaint.setColor(c);
            float w = g / (grains - 1);
            for (int i = 0; i < grains; ++i) {
                final float a = 0.15f - i / (grains * 10.0f + 10f);

                // paint one side
                final float tex = (float) Math.sin(i * w); //HALF_PI*(cos(i*w*PI)+1);
                final float lex = 0.5f * (float) Math.sin(tex); //(cos(tex*PI)+1)*0.5;
                final float px1 = ox + (x - ox) * (0.5f + lex);
                final float py1 = oy + (y - oy) * (0.5f + lex);
                final float px2 = ox + (x - ox) * (0.5f - lex);
                final float py2 = oy + (y - oy) * (0.5f - lex);

                renderPaint.setAlpha(Math.round(a * 256));
                renderCanvas.drawPoint(px1, py1, renderPaint);
                renderCanvas.drawPoint(px2, py2, renderPaint);
            }
        }
        
    }


    private class SandPainterOutside extends SandPainter {

        SandPainterOutside() {
        }

        @Override
        void renderBody(float x, float y, float ox, float oy) {
            // calculate grains by distance
            //int grains = int(sqrt((ox-x)*(ox-x)+(oy-y)*(oy-y)));
            int grains = 11;

            // lay down grains of sand (transparent pixels)
            renderPaint.setColor(c);
            float w = g / (grains - 1);
            for (int i = 0; i < grains; ++i) {
                final float a = 0.15f - i / (grains * 10.0f + 10f);

                // paint one side
                final float tex = (float) Math.sin(i * w); //HALF_PI*(cos(i*w*PI)+1);
                final float lex = 0.5f * (float) Math.sin(tex); //(cos(tex*PI)+1)*0.5;
                final float px1 = ox + (x - ox) * lex;
                final float py1 = oy + (y - oy) * lex;
                final float px2 = x + (ox - x) * lex;
                final float py2 = y + (oy - y) * lex;

                renderPaint.setAlpha(Math.round(a * 256));
                renderCanvas.drawPoint(px1, py1, renderPaint);
                renderCanvas.drawPoint(px2, py2, renderPaint);
            }
        }
        
    }


    // ******************************************************************** //
    // Class Data.
    // ******************************************************************** //

    // Debugging tag.
    @SuppressWarnings("unused")
    private static final String TAG = "Substrate";

    // Handy constants.
    private static final float TWO_PI = (float) Math.PI * 2f;
    private static final float HALF_PI = (float) Math.PI / 2f;


    // ******************************************************************** //
    // Private Data.
    // ******************************************************************** //

    private int diameter;

    // The number of currently-active paths.
    private int numPaths = 8;

    // object array
    private CPath[] cpaths;

    // Index of the next path to be updated.  We don't update all the path
    // every time for performance reasons, so this keeps our place in the
    // list between updates.
    private int nextPath = 0;

}

