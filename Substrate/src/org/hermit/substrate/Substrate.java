
package org.hermit.substrate;


import java.util.Random;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;

import net.goui.util.MTRandom;


/**
 * Main Substrate drawing class.  This class is abstracted (as much as
 * possible) to just draw into a Bitmap.
 */
public class Substrate {

    // ******************************************************************** //
    // Constructor.
    // ******************************************************************** //
    
    /**
     * Create a substrate drawing instance. We are given the Bitmap
     * we have to render into.
     * 
     * @param   width       The width of the substrate.
     * @param   height      The height of the substrate.
     * @param   bitmap      The Bitmap to render into.
     */
    public Substrate(int width, int height, Bitmap bitmap) {
        substrateWidth = width;
        substrateHeight = height;
        
        // Create a Canvas for drawing, and a Paint to hold the drawing state.
        renderCanvas = new Canvas(bitmap);
        renderPaint = new Paint();
        
        crackGrid = new int[substrateWidth * substrateHeight];
        cracks = new Crack[MAX_CRACKS];

        colourPalette = new PollockPalette();
        
        resetSubstrate();  
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
     * Update this substrate into its rendering bitmap.
     * 
     * @param   now         Current time in ms.  Will be the same as that
     *                      passed to doUpdate(), if there was a preceeding
     *                      call to doUpdate().
     */
    public void doDraw(long now) {
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
     
    // Canvas used for drawing into the Bitmap, and a Paint.
    private final Canvas renderCanvas;
    private final Paint renderPaint;

    // The number of currently-active cracks.
    private int numCracks = 0;

    // Grid of cracks.
    private int[] crackGrid;
    private Crack[] cracks;

}

