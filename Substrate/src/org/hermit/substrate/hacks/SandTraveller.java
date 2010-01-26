
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
import org.hermit.substrate.SandPalette;

import net.goui.util.MTRandom;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.util.Log;


/**
 * SandTraveller: grow crystal-like lines on a computational substrate.  This
 * is a port of the code by J. Tarbell at http://complexification.net/.
 *
 // Sand Traveler 
 // Special commission for Sónar 2004, Barcelona
 // sand painter implementation of City Traveler + complexification.net

 // j.tarbell   May, 2004
 // Albuquerque, New Mexico
 // complexification.net

 // Processing 0085 Beta syntax update
 // j.tarbell   April, 2005
 * <p>Copyright © 2003 by J. Tarbell (complex@complexification.net).
 * 
 *     "Modifications and extensions of these algorithms are encouraged.
 *      Please send me your experiences."
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
     */
    public SandTraveller() {
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
        colourPalette = new SandPalette();
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
        int maxCycles = 3600;
        try {
            String sval = prefs.getString("maxCycles", "" + maxCycles);
            maxCycles = Integer.valueOf(sval);
        } catch (Exception e) {
            Log.e(TAG, "Pref: bad maxCycles");
        }
        setMaxCycles(maxCycles);
        Log.i(TAG, "Prefs: maxCycles " + maxCycles);

        try {
            sandPaint = prefs.getBoolean("sandPaint", sandPaint);
        } catch (Exception e) {
            Log.e(TAG, "Pref: bad sandPaint");
        }
        Log.i(TAG, "Prefs: sandPaint " + sandPaint);

        try {
            String sval = prefs.getString("initVelocity", "" + initVelocity);
            initVelocity = Float.valueOf(sval);
        } catch (Exception e) {
            Log.e(TAG, "Pref: bad initVelocity");
        }
        Log.i(TAG, "Prefs: initVelocity " + initVelocity);

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

        cities = new City[maxCities];

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
            final int c = nextCity;
            if (++nextCity >= numCities) {
                nextCity = 0;
                ++cycles;
            }

            // move cities
            cities[c].move();

            time = System.currentTimeMillis() - start;
        }

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

    // Convenience -- two times pi.
    private static final float TWO_PI = (float) Math.PI * 2;

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

    // Whether to use the sand painter.
    private boolean sandPaint = false;
    
    // Traveller initial velocity.
    private float initVelocity = 4.2f;

    // The maximum number of cities we can have on the go at once.
    private int maxCities = 201;

    // The cities.
    private City[] cities;

    // The number of currently-active cities.
    private int numCities = 200;

    // Index of the next city to be updated.  We don't update all the cities
    // every time for performance reasons, so this keeps our place in the
    // list between updates.
    private int nextCity = 0;

    // Number of grains of sand to paint.
    private int sandGrains = 11;

    // minimum distance to draw connections
    private int minConnection = 256;

}

