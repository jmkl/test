
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
 * Happy.Place: renders the resulting configuration of a system of
 * friendly nodes.  This is a port of the code by J. Tarbell at
 * http://complexification.net/.
 *
 * <p>By j.tarbell, March, 2004<br>
 * Albuquerque, New Mexico<br>
 * complexification.net<br>
 * Copyright © 2003 by J. Tarbell (complex@complexification.net).
 * 
 * <p>"Modifications and extensions of these algorithms are encouraged.
 * Please send me your experiences."
 */
public class HappyPlace
    extends EyeCandy
{

    // ******************************************************************** //
    // Public Constants.
    // ******************************************************************** //

    /**
     * Preferences name for preferences relating to this eye candy.
     */
    public static final String SHARED_PREFS_NAME = "happy_settings";


    // ******************************************************************** //
    // Constructor.
    // ******************************************************************** //

    /**
     * Create an instance.
     * 
     * @param  context      Our application context.
     */
    public HappyPlace(Context context) {
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
        int maxCycles = 6000;
        try {
            String sval = prefs.getString("maxCycles", "" + maxCycles);
            maxCycles = Integer.valueOf(sval);
        } catch (Exception e) {
            Log.e(TAG, "Pref: bad maxCycles");
        }
        setMaxCycles(maxCycles);
        Log.i(TAG, "Prefs: maxCycles " + maxCycles);
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

        if (friends == null || friends.length != numFriends)
            friends = new Friend[numFriends];

        // make some friend entities
        final int dim = Math.min(canvasWidth, canvasHeight);
        for (int i = 0; i < numFriends; ++i) {
            final float az = TWO_PI * i / numFriends;
            float fx = canvasWidth / 2 + 0.4f * dim * (float) Math.cos(az);
            float fy = canvasHeight / 2 + 0.4f * dim * (float) Math.sin(az);
            if (friends[i] == null)
                friends[i] = new Friend(fx, fy);
            else
                friends[i].reset(fx, fy);
        }

        // make some random friend connections
        for (int k=0;k<numFriends*2.2;k++) {
            int a = (int)((float) Math.floor(random(numFriends)));
            int b = (int)((float) Math.floor(a+random(22f))%numFriends);
            if (b>=numFriends) {
                b=0;
            } else if (b<0) {
                b=0;
            }
            if (a!=b) {
                friends[a].connectTo(b);
                friends[b].connectTo(a);
            }
        }

        // Clear to white.
        renderCanvas.drawColor(backgroundColor);

        nextFriend = 0;
        updateState = 0;
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
        final int c = nextFriend;

        // move friends to happy places
        switch (updateState) {
        case 0:
            friends[c].move();
            break;
        case 1:
            friends[c].expose();
            friends[c].exposeConnections();
            break;
        case 2:
            friends[c].findHappyPlace();
            break;
        }

        if (++nextFriend >= numFriends) {
            nextFriend = 0;
            ++updateState;
            if (updateState > 2 || (updateState > 1 && cycles % 2 == 1)) {
                updateState = 0;
                ++cycles;
            }
        }
        return cycles;
    }


    // ******************************************************************** //
    // Friend Class.
    // ******************************************************************** //

    private class Friend {
        float x, y;
        float vx, vy;

        int numcon;
        int maxcon = 10;
        int lencon = 10 + random(50);
        int[] connections = null;

        // sand painters
        int numsands = 3;
        SandPainter[] sands = new SandPainter[numsands];

        Friend(float X, float Y) {
            connections = new int[maxcon];
            for (int n = 0; n < numsands; ++n)
                sands[n] = new SandPainter();
            reset(X, Y);
        }
        
        void reset(float X, float Y) {
            // position
            x = X;
            y = Y;
            numcon = 0;

            for (int i = 0; i < maxcon; ++i)
                connections[i] = -1;
            for (int n = 0; n < numsands; ++n)
                sands[n].reset();
        }

        void expose() {
            for (int dx = -2; dx <= 2; ++dx) {
                float a = 0.5f - Math.abs(dx) / 5.0f;
                renderPaint.setColor(0xff000000);
                renderPaint.setAlpha(Math.round(256 * a));
                renderCanvas.drawPoint(x + dx, y, renderPaint);
                renderPaint.setColor(0xffffffff);
                renderPaint.setAlpha(Math.round(256 * a));
                renderCanvas.drawPoint(x + dx - 1, y - 1, renderPaint);
            }
            for (int dy = -2; dy <= 2; ++dy) {
                float a = 0.5f - Math.abs(dy) / 5.0f;
                renderPaint.setColor(0xff000000);
                renderPaint.setAlpha(Math.round(256 * a));
                renderCanvas.drawPoint(x, y + dy, renderPaint);
                renderPaint.setColor(0xffffffff);
                renderPaint.setAlpha(Math.round(256 * a));
                renderCanvas.drawPoint(x - 1, y + dy - 1, renderPaint);
            }
        }

        void exposeConnections() {
            // draw connection lines to all friends
            for (int n=0;n<numcon;n++) {
                // find axis distances
                float ox = friends[connections[n]].x;
                float oy = friends[connections[n]].y;

                for (int s=0;s<numsands;s++) {
                    sands[s].render(x,y,ox,oy);
                }
            }
        }


        void move() {
            // add velocity to position
            x += vx;
            y += vy;

            //friction
            vx *= 0.92;
            vy *= 0.92;
        }

        void connectTo(int f) {
            // connect to friend f

            // is there room for more friends?
            if (numcon < maxcon) {
                // already connected to friend?
                if (!friendOf(f)) {
                    connections[numcon] = f;
                    numcon++;
                }
            }
        }

        boolean friendOf(int x) {
            for (int n = 0; n < numcon; ++n)
                if (connections[n] == x)
                    return true;

            return false;
        }

        void findHappyPlace() {
            // set destination to a happier place
            // (closer to friends, further from others)
            float ax = 0.0f;
            float ay = 0.0f;

            // find mean average of all friends and non-friends
            for (int n = 0; n < numFriends; ++n) {
                if (friends[n] != this) {
                    // find distance
                    float ddx = friends[n].x - x;
                    float ddy = friends[n].y - y;
                    float d = (float) Math.sqrt(ddx * ddx + ddy * ddy);
                    float t = (float) Math.atan2(ddy, ddx);

                    boolean friend = false;
                    for (int j = 0; j < numcon; ++j) {
                        if (connections[j] == n) {
                            friend = true;
                            break;
                        }
                    }
                    if (friend) {
                        // attract
                        if (d>lencon) {
                            ax += 4.0*(float) Math.cos(t);
                            ay += 4.0*(float) Math.sin(t);
                        }
                    } else {
                        // repulse
                        if (d<lencon) {
                            ax += (lencon-d)*(float) Math.cos(t+PI);
                            ay += (lencon-d)*(float) Math.sin(t+PI);
                        }
                    }
                }
            }

            vx+=ax/42.22;
            vy+=ay/42.22;
        }
    }

    class SandPainter {

        SandPainter() {
            reset();
        }

        void reset() {
            c = colourPalette.getRandom();
            p = random(1.0f);
            sinp = (float) Math.sin(p);
            g = random(0.01f, 0.1f);
        }

        void render(float x, float y, float ox, float oy) {
            renderPaint.setColor(c);
            renderPaint.setAlpha(28);
            renderCanvas.drawPoint(ox + (x - ox) * sinp, oy + (y - oy) * sinp, renderPaint);

            g += random(-0.050f, 0.050f);
            float maxg = 0.22f;
            if (g < -maxg)
                g = -maxg;
            if (g > maxg)
                g = maxg;

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
        private float sinp;

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
    private static final float PI = (float) Math.PI;
    private static final float TWO_PI = (float) Math.PI * 2f;


    // ******************************************************************** //
    // Private Data.
    // ******************************************************************** //

    // Number of friends we'll process.
    private int numFriends = 40;

    // The list of friends.
    private Friend[] friends;

    // Index of the next friend to be updated.  We don't update all the friends
    // every time for performance reasons, so this keeps our place in the
    // list between updates.
    private int nextFriend = 0;

    // Current processing state.  Used to progress through the various
    // phases of processing we do.
    private int updateState = 0;

    // Number of grains of sand to paint.
    private int sandGrains = 11;

}

