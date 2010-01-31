
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
import android.util.Log;


/**
 * SandTraveller: a rendering of 1,000 traveling particles, each in pursuit
 * of another.  This is a port of the code by J. Tarbell at
 * http://complexification.net/.
 *
 * <p>By j.tarbell, May, 2004<br>
 * Albuquerque, New Mexico<br>
 * complexification.net<br>
 * Copyright © 2003 by J. Tarbell (complex@complexification.net).
 * 
 * <p>"Modifications and extensions of these algorithms are encouraged.
 * Please send me your experiences."
 */
public class SandTraveller
    extends EyeCandy
{

    // ******************************************************************** //
    // Public Constants.
    // ******************************************************************** //

    /**
     * Preferences name for preferences relating to this eye candy.
     */
    public static final String SHARED_PREFS_NAME = "sandtrav_settings";


    // ******************************************************************** //
    // Constructor.
    // ******************************************************************** //

    /**
     * Create a substrate drawing instance.
     * 
     * @param  context      Our application context.
     */
    public SandTraveller(Context context) {
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
        int maxCycles = 2000;
        if (key == null || key.equals("maxCycles")) try {
            String sval = prefs.getString("maxCycles", "" + maxCycles);
            maxCycles = Integer.valueOf(sval);
            setMaxCycles(maxCycles);
            Log.i(TAG, "Prefs: maxCycles " + maxCycles);
        } catch (Exception e) {
            Log.e(TAG, "Pref: bad maxCycles");
        }

        if (key == null || key.equals("sandPaint")) try {
            sandPaint = prefs.getBoolean("sandPaint", sandPaint);
            Log.i(TAG, "Prefs: sandPaint " + sandPaint);
        } catch (Exception e) {
            Log.e(TAG, "Pref: bad sandPaint");
        }

        if (key == null || key.equals("initVelocity")) try {
            String sval = prefs.getString("initVelocity", "" + initVelocity);
            initVelocity = Float.valueOf(sval);
            Log.i(TAG, "Prefs: initVelocity " + initVelocity);
        } catch (Exception e) {
            Log.e(TAG, "Pref: bad initVelocity");
        }

        if (key == null || key.equals("sandGrains")) try {
            String sval = prefs.getString("sandGrains", "" + sandGrains);
            sandGrains = Integer.valueOf(sval);
            Log.i(TAG, "Prefs: sandGrains " + sandGrains);
        } catch (Exception e) {
            Log.e(TAG, "Pref: bad sandGrains");
        }
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

        cities = new City[numCities];

        float vt = initVelocity;
        float vvt = 0.2f;
        float ot = random(TWO_PI);
        for (int t = 0; t < numCities; ++t) {
            float tinc = ot + (1.1f - t / numCities) * 2 * t * TWO_PI / numCities;
            float vx = vt * (float) Math.sin(tinc);
            float vy = vt * (float) Math.cos(tinc);
            cities[t] = new City(canvasWidth / 2 + vx * 2, canvasHeight / 2 + vy * 2, vx, vy, t);
            vvt -= 0.00033f;
            vt += vvt;
        }

        for (int t = 0; t < numCities; ++t)
            cities[t].findFriend();

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
        final int c = nextCity;
        if (++nextCity >= numCities) {
            nextCity = 0;
            ++cycles;
        }

        // move cities
        cities[c].move();

        return cycles;
    }


    // ******************************************************************** //
    // Private Methods.
    // ******************************************************************** //

    private float citydistance(int a, int b) {
        if (a != b) {
            // calculate and return distance between cities
            float dx = cities[b].x - cities[a].x;
            float dy = cities[b].y - cities[a].y;
            float d = (float) Math.sqrt(dx * dx + dy * dy);
            return d;
        } else {
            return 0.0f;
        }
    }


    // ******************************************************************** //
    // City Class.
    // ******************************************************************** //

    private class City {

        float x,y;
        int friend;
        float vx, vy;
        int idx;
        int myc = colourPalette.getRandom();

        // sand painters
        int numsands = 3;
        SandPainter[] sands = new SandPainter[numsands];

        City(float Dx, float Dy, float Vx, float Vy, int Idx) {
            // position
            x = Dx;
            y = Dy;
            vx = Vx;
            vy = Vy;
            idx = Idx;

            // create sand painters
            for (int n = 0; n < numsands; ++n)
                sands[n] = new SandPainter();
        }

        void move() {
            vx+=(cities[friend].x-x)/1000;
            vy+=(cities[friend].y-y)/1000;

            vx*=.936;
            vy*=.936;
            x+=vx;
            y+=vy;

            if (!sandPaint) {
                drawTravelers();
            } else {
                if (citydistance(idx, friend) < minConnection)
                    drawSandPainters();
            }
        }


        void findFriend() {
            // pick a node to follow just out ahead
            int off = (int) random(numCities / 5) + 1;
            friend = (idx + off) % numCities;
        }


        void drawTravelers() {
            int nt = 11;
            for (int i=0;i<nt;i++) {
                // pick random distance between city
                final float t = random(TWO_PI);
                final float sint = (float) Math.sin(t);
                
                // draw traveler      
                float dx = sint*(x-cities[friend].x)/2+(x+cities[friend].x)/2;
                float dy = sint*(y-cities[friend].y)/2+(y+cities[friend].y)/2;
                if (random(1000)>990) {
                    // noise
                    dx+=random(3)-random(3);
                    dy+=random(3)-random(3);
                }
                renderPaint.setColor(cities[friend].myc);
                renderPaint.setAlpha(48);
                renderCanvas.drawPoint(dx, dy, renderPaint);
                // draw anti-traveler
                dx = -1*sint*(x-cities[friend].x)/2+(x+cities[friend].x)/2;
                dy = -1*sint*(y-cities[friend].y)/2+(y+cities[friend].y)/2;
                if (random(1000)>990) {
                    // noise
                    dx+=random(3)-random(3);
                    dy+=random(3)-random(3);
                }
                renderCanvas.drawPoint(dx, dy, renderPaint);
            }
        }

        void drawSandPainters() {
            for (int s=0;s<numsands;s++) {
                sands[s].render(x,y,cities[friend].x,cities[friend].y);
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

        void render(float x, float y, float ox, float oy) {
            // draw painting sweeps
            renderPaint.setColor(c);
            renderPaint.setAlpha(28);
            renderCanvas.drawPoint(ox+(x-ox)*(float) Math.sin(p),oy+(y-oy)*(float) Math.sin(p), renderPaint);

            g+=random(-0.050f, 0.050f);
            float maxg = 0.22f;
            if (g<-maxg) g=-maxg;
            if (g>maxg) g=maxg;
            p+=random(-0.050f, 0.050f);
            if (p<0) p=0;
            if (p>1.0) p=1.0f;

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
    // Class Data.
    // ******************************************************************** //

    // Debugging tag.
    @SuppressWarnings("unused")
    private static final String TAG = "Substrate";

    // Convenience -- two times pi.
    private static final float TWO_PI = (float) Math.PI * 2;


    // ******************************************************************** //
    // Private Data.
    // ******************************************************************** //

    // Whether to use the sand painter.
    private boolean sandPaint = false;
    
    // Traveller initial velocity.
    private float initVelocity = 4.2f;

    // The cities.
    private City[] cities;

    // The number of cities.
    private int numCities = 100;

    // Index of the next city to be updated.  We don't update all the cities
    // every time for performance reasons, so this keeps our place in the
    // list between updates.
    private int nextCity = 0;

    // Number of grains of sand to paint.
    private int sandGrains = 11;

    // minimum distance to draw connections
    private int minConnection = 128;

}

