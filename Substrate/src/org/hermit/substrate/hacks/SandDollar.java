
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
 * SandDollar: an iteratively constructed radial form.  This
 * is a port of the code by J. Tarbell at http://complexification.net/.
 *
 * <p>By j.tarbell, March, 2004<br>
 * Albuquerque, New Mexico<br>
 * complexification.net<br>
 * Copyright © 2003 by J. Tarbell (complex@complexification.net).
 * 
 * <p>"Modifications and extensions of these algorithms are encouraged.
 * Please send me your experiences."
 */
public class SandDollar
extends EyeCandy
{

    // ******************************************************************** //
    // Public Constants.
    // ******************************************************************** //

    /**
     * Preferences name for preferences relating to this eye candy.
     */
    public static final String SHARED_PREFS_NAME = "dollar_settings";


    // ******************************************************************** //
    // Constructor.
    // ******************************************************************** //

    /**
     * Create an instance.
     * 
     * @param  context      Our application context.
     */
    public SandDollar(Context context) {
        super(context);
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
        setMaxCycles((int) (drag * 1.1));
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

        // bp is number of petals
        final int bp = random(13) + 3;
        final float sx = canvasWidth / 2;
        final float sy = canvasHeight / 2;
        final int diameter = Math.min(canvasWidth, canvasHeight);
        final float radius = diameter * 0.0339f;
        sandDollar = new Dollar(sx, sy, 0, -HALF_PI, radius, bp);
        sandDollar.render();
                
        // Clear.
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
        final int c = nextDollar;

            sandDollar.swim();
//            sandDollars[c].swim();

        if (++nextDollar >= 1) {
            nextDollar = 0;
            ++cycles;
        }

        return cycles;
    }


    // ******************************************************************** //
    // SandDollar Class.
    // ******************************************************************** //

    private class Dollar {
        // feet
        private int depth;
        private int limbs;
        private int petals;

        private float time, timev;
        private float x, y;
        private float ox, oy;
        private float radius;
        private float theta, ptheta;


        private int numsp = 1;
        private int maxsp = 13;
        private SandPainter[] sp = new SandPainter[maxsp];

        private Dollar[] mysandDollars = new Dollar[2];

        Dollar(float X, float Y, int Depth, float Theta, float Radius, int Petals) {
            // init
            ox = x = X;
            oy = y = Y;
            ptheta = Theta;
            radius = Radius;
            depth = Depth;
            petals = Petals;

            limbs = 0;
            time = 0;
            timev = petals*TWO_PI/drag;
            if (random(100)>50) timev*=-1;

            // add sweeps
            numsp = (int) (2 + random(depth / 2.0f));
            for (int n=0;n<numsp;n++) {
                sp[n] = new SandPainter();
            }
        }

        void render() {
            theta = random(-HALF_PI / 3, HALF_PI / 3);
            radius *= random(1.02f, 1.20f);

            // set next radial point
            x = ox + radius* (float) Math.cos(theta);
            y = oy + radius*(float) Math.sin(theta);

            if (depth < maxdepth) {
                int lnum = 1;
                if (random(100) > 90 - depth)
                    ++lnum;
                for (int n = 0; n < lnum; ++n) {
                    int bp = petals * (random(3) + 1);
                    mysandDollars[n] = new Dollar(x, y, depth + 1, theta, radius, bp);
                    mysandDollars[n].render();
                    limbs++;
                }
            }
        }

        void swim() {
            // move through time
            time += timev;
            final float sint = (float) Math.sin(time);
            final float cost = (float) Math.cos(time);
            
            // spin in sinusoidal waves
            if (depth == 0) {
                theta += TWO_PI / drag;
            } else {
                theta += sint / 1640f;
                radius += depth % 2 == 0 ? sint * 0.22f : cost * 0.22f;
            }

            // set next radius point
            x = ox + radius * (float) Math.cos(theta + ptheta);
            y = oy + radius * (float) Math.sin(theta + ptheta);

            // render sand painters
            for (int n = 0; n < numsp; n++)
                sp[n].render(x, y, ox, oy);

            // draw child limbs
            for (int n = 0; n < limbs; n++) {
                mysandDollars[n].setOrigin(x, y, theta + ptheta);
                mysandDollars[n].swim();
            }
        }

        void setOrigin(float X, float Y, float Theta) {
            ox = X;
            oy = Y;
            ptheta = Theta;
        }

    }


    // ******************************************************************** //
    // SandPainter Class.
    // ******************************************************************** //

    private class SandPainter {

        SandPainter() {
            reset();
        }

        void reset() {
            c = colourPalette.getRandom();
            p = random(1.0f);
            g = random(0.01f, 0.1f);
        }

        void render(final float x, final float y, final float ox, final float oy) {
            final float dx = x - ox;
            final float dy = y - oy;
            
            // draw painting sweeps
            renderPaint.setColor(c);
            renderPaint.setAlpha(22);
            renderCanvas.drawPoint(ox + dx * (float) Math.sin(p),
                                   oy + dy * (float) Math.sin(p),
                                   renderPaint);

            g += random(-0.050f, 0.050f);
            float maxg = 0.22f;
            if (g < -maxg)
                g = -maxg;
            else if (g > maxg)
                g = maxg;

            float w = g / (sandGrains - 1);
            for (int i = 1; i < sandGrains; ++i) {
                final float siw = (float) Math.sin(i * w);
                final float ssiw1 = (float) Math.sin(p + siw);
                final float ssiw2 = (float) Math.sin(p - siw);
                final float px1 = ox + dx * ssiw1;
                final float py1 = oy + dy * ssiw1;
                final float px2 = ox + dx * ssiw2;
                final float py2 = oy + dy * ssiw2;
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

    // The actual sand dollar object.
    private Dollar sandDollar;

    // Index of the next Dollar to be updated.  We don't update all the Dollars
    // every time for performance reasons, so this keeps our place in the
    // list between updates.
    private int nextDollar = 0;

    // maxdepth keeps the tree structures reasonable
    private int maxdepth = 7;
    
    // drag is the number of segments within a full revolution
    private int drag = 2048;
    
    // Number of grains of sand to paint.
    private int sandGrains = 7;

}

