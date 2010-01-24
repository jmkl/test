
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


/**
 * Substrate: grow crystal-like lines on a computational substrate.  This
 * is a port of the code by J. Tarbell at http://complexification.net/.
 *
 * <p>Copyright © 2003 by J. Tarbell (complex@complexification.net).
 */
public class Substrate
    extends EyeCandy
{

    // ******************************************************************** //
    // Constructor.
    // ******************************************************************** //
    
    /**
     * Create a substrate drawing instance.
     * 
     * @param   width       The width of the substrate.
     * @param   height      The height of the substrate.
     * @param   config      Pixel configuration of the screen.
     */
    public Substrate(int width, int height, Bitmap.Config config) {
        super(width, height, config);
        
        substrateWidth = width;
        substrateHeight = height;
        
        crackGrid = new int[substrateWidth * substrateHeight];
        cracks = new Crack[MAX_CRACKS];

        colourPalette = new PollockPalette();
        
        resetSubstrate();  
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
    public void onSharedPreferenceChanged(SharedPreferences prefs,
            String key) {
//        String shape = prefs.getString("cube2_shape", "cube");

        // read the 3D model from the resource
    }

    
    // ******************************************************************** //
    // Control Methods.
    // ******************************************************************** //

    /**
     * Reset this substrate back to a blank state.
     */
    public void resetSubstrate() {
        // erase crack grid
        for (int y = 0; y < substrateHeight; ++y)
            for (int x = 0; x < substrateWidth; ++x)
                crackGrid[y * substrateWidth + x] = 10001;

        // make random crack seeds
        for (int k = 0; k < 16; k++) {
            int i = MT_RANDOM.nextInt(substrateWidth * substrateHeight - 1);
            crackGrid[i] = MT_RANDOM.nextInt(360);
        }

        // make just three cracks
        numCracks = 0;
        for (int k = 0; k < 3; k++)
            makeCrack();

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
        // crack all cracks
        for (int n = 0; n < numCracks; ++n)
            cracks[n].move();
    }


    // ******************************************************************** //
    // Private Methods.
    // ******************************************************************** //

    private void makeCrack() {
        if (numCracks < MAX_CRACKS) {
            // make a new crack instance
            cracks[numCracks] = new Crack();
            numCracks++;
        }
    }


    // ******************************************************************** //
    // Crack Class.
    // ******************************************************************** //
    
    private class Crack {
        float x, y;
        float t;    // direction of travel in degrees

        // sand painter
        SandPainter sp;

        Crack() {
            // find placement along existing crack
            findStart();
            sp = new SandPainter();
        }

        void findStart() {
            // pick random point
            int px = 0;
            int py = 0;

            // shift until crack is found
            boolean found = false;
            int timeout = 0;
            while (!found || timeout++ > 1000) {
                px = MT_RANDOM.nextInt(substrateWidth);
                py = MT_RANDOM.nextInt(substrateHeight);
                if (crackGrid[py * substrateWidth + px] < 10000)
                    found = true;
            }

            if (found) {
                // start crack
                int a = crackGrid[py*substrateWidth+px];
                if (MT_RANDOM.nextBoolean())
                    a -= 90 + irandom(-2f, 2.1f);
                else
                    a += 90 + irandom(-2f, 2.1f);

                startCrack(px, py, a);
            } else {
                //println("timeout: "+timeout);
            }
        }

        void startCrack(int X, int Y, int T) {
            x=X;
            y=Y;
            t=T;//%360;
            double tr = Math.toRadians(t);
            x += 0.61 * Math.cos(tr);
            y += 0.61 * Math.sin(tr);  
        }

        void move() {
            // continue cracking
            double tr = Math.toRadians(t);
            x += 0.42 * Math.cos(tr);
            y += 0.42 * Math.sin(tr); 

            // bound check
            float z = 0.33f;
            int cx = (int) (x + random(-z, z));  // add fuzz
            int cy = (int) (y + random(-z, z));

            // draw sand painter
            regionColor();

            // draw black crack
            renderPaint.setColor(0xff000000);
            renderPaint.setAlpha(85);
            renderCanvas.drawPoint(x + random(-z, z), y + random(-z, z), renderPaint);
            // stroke(0, 85);
            // point(x + random(-z, z), y + random(-z, z));

            if ((cx>=0) && (cx<substrateWidth) && (cy>=0) && (cy<substrateHeight)) {
                // safe to check
                if ((crackGrid[cy*substrateWidth+cx]>10000) || (Math.abs(crackGrid[cy*substrateWidth+cx]-t)<5)) {
                    // continue cracking
                    crackGrid[cy * substrateWidth + cx] = (int) t;
                } else if (Math.abs(crackGrid[cy*substrateWidth+cx]-t)>2) {
                    // crack encountered (not self), stop cracking
                    findStart();
                    makeCrack();
                }
            } else {
                // out of bounds, stop cracking
                findStart();
                makeCrack();
            }
        }

        void regionColor() {
            // start checking one step away
            float rx=x;
            float ry=y;
            boolean openspace=true;

            // find extents of open space
            while (openspace) {
                // move perpendicular to crack
                double tr = Math.toRadians(t);
                rx += 0.81 * Math.sin(tr);
                ry -= 0.81 * Math.cos(tr);
                int cx = (int) rx;
                int cy = (int) ry;
                if ((cx>=0) && (cx<substrateWidth) && (cy>=0) && (cy<substrateHeight)) {
                    // safe to check
                    if (crackGrid[cy*substrateWidth+cx]>10000) {
                        // space is open
                    } else {
                        openspace=false;
                    }
                } else {
                    openspace=false;
                }
            }
            // draw sand painter
            sp.render(rx,ry,x,y);
        }
    }


    // ******************************************************************** //
    // SandPainter Class.
    // ******************************************************************** //

    private class SandPainter {

        SandPainter() {
            c = colourPalette.getRandom();
            g = random(0.01f, 0.1f);
        }
        
        void render(float x, float y, float ox, float oy) {
            // modulate gain
            g += random(-0.050f, 0.050f);
            float maxg = 1.0f;
            if (g < 0)
                g = 0;
            if (g > maxg)
                g = maxg;

            // calculate grains by distance
            //int grains = int(sqrt((ox-x)*(ox-x)+(oy-y)*(oy-y)));
            int grains = 64;

            // lay down grains of sand (transparent pixels)
            float w = g / (grains - 1);
            for (int i = 0; i < grains; i++) {
                final float ssiw = (float) Math.sin(Math.sin(i * w));
                final float px = ox + (x - ox) * ssiw;
                final float py = oy + (y - oy) * ssiw;
                final float a = 0.1f - i / (grains * 10.0f);
                
                renderPaint.setColor(c);
                renderPaint.setAlpha(Math.round(a * 256));
                renderCanvas.drawPoint(px, py, renderPaint);
                // stroke(red(c), green(c), blue(c), a*256);
                // point(px, py);
            }
        }

        // Colour for this SandPainter.
        private int c;
        
        // Gain; used to modulate the alpha for a "fuzzy" effect.
        private float g;
    }

    
    // ******************************************************************** //
    // Utility Methods.
    // ******************************************************************** //
    
    private float random(float a, float b) {
        return MT_RANDOM.nextFloat() * (b - a) + a;
    }

    private int irandom(float a, float b) {
        return (int) random(a, b);
    }

    
    // ******************************************************************** //
    // Class Data.
    // ******************************************************************** //

    // Random number generator.  We use a Mersenne Twister,
    // which is a high-quality and fast implementation of java.util.Random.
    private static final Random MT_RANDOM = new MTRandom();
    
    // The maximum number of cracks we can have on the go at once.
    private static final int MAX_CRACKS = 100;

    
    // ******************************************************************** //
    // Private Data.
    // ******************************************************************** //

    // Colour palette we're using.
    private Palette colourPalette;
    
    // Size of this substrate.
    private final int substrateWidth;
    private final int substrateHeight;

    // The number of currently-active cracks.
    private int numCracks = 0;

    // Grid of cracks.
    private int[] crackGrid;
    private Crack[] cracks;

}

