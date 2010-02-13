
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
import org.hermit.substrate.R;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.util.Log;


/**
 * NodeGarden: nodes instantiated on irregular curving lines are
 * connected together to form a Node Garden.  This is a port of the
 * code by J. Tarbell at http://complexification.net/.
 *
 * <p>By j.tarbell, August, 2004<br>
 * Albuquerque, New Mexico<br>
 * complexification.net<br>
 * Copyright © 2003 by J. Tarbell (complex@complexification.net).
 * 
 * <p>"Modifications and extensions of these algorithms are encouraged.
 * Please send me your experiences."
 */
public class NodeGarden
extends EyeCandy
{

    // ******************************************************************** //
    // Public Constants.
    // ******************************************************************** //

    /**
     * Preferences name for preferences relating to this eye candy.
     */
    public static final String SHARED_PREFS_NAME = "nodes_settings";


    // ******************************************************************** //
    // Constructor.
    // ******************************************************************** //

    /**
     * Create an instance.
     * 
     * @param  context      Our application context.
     */
    public NodeGarden(Context context) {
        super(context);

        // For this hack, a single cycle renders the whole thing.
        setCycles(1);
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
        // load node icon   
        nodeIcoBase = loadImage(R.drawable.node_base);
        nodeIcoDark = loadImage(R.drawable.node_dark);
        nodeIcoSpec = loadImage(R.drawable.node_specular);

        nodeRectBase = new Rect(0, 0, nodeIcoBase.getWidth(), nodeIcoBase.getHeight());
        nodeRectDark = new Rect(0, 0, nodeIcoDark.getWidth(), nodeIcoDark.getHeight());
        nodeRectSpec = new Rect(0, 0, nodeIcoSpec.getWidth(), nodeIcoSpec.getHeight());
        
        // create all nodes
        gnodes = new GNode[maxNodes];

        // create all spines
        spines = new Spine[maxSpines];
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
        try {
            String sval = prefs.getString("nodePattern", "" + nodePattern);
            nodePattern = Integer.valueOf(sval);
        } catch (Exception e) {
            Log.e(TAG, "Pref: bad nodePattern");
        }
        Log.i(TAG, "Prefs: nodePattern " + nodePattern);
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

        // reset object counters
        numSpines = 0;
        numNodes = 0;

        diameter = Math.min(canvasWidth, canvasHeight);
        if (nodePattern == 0 || (nodePattern <= 0 && brandom())) {
            // arrange spines in line
            for (int i = 0; i < maxSpines; ++ i) {
                float x = canvasWidth / 4f + i * canvasWidth / (maxSpines - 1);
                float y = canvasHeight / 2f;
                float mt = 420; 
                makeSpine(x, y, -HALF_PI, mt);
                makeSpine(x, y, HALF_PI, mt);
            }
        } else {
            // arrange spines in circle
            for (int i = 0; i < maxSpines; ++i) {
                float a = TWO_PI * i / (maxSpines - 1);
                float x = canvasWidth / 2f + 0.15f * diameter * (float) Math.cos(a);
                float y = canvasHeight / 2f + 0.15f * diameter * (float) Math.sin(a);
                float mt = random(11f, 140f); 
                makeSpine(x, y, a, mt);

                // make a second spine in opposite direction
                //          makeSpine(x,y,a+PI,mt);
            }
        }

        // begin step one of rendering process  
        updateState = 0;
        
        nextNode = 0;

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
        final int c = nextNode;

        // move friends to happy places
        switch (updateState) {
        case 0:
            for (int k = 0; k < 5; ++k)
                gnodes[random(numNodes)].findNearConnection();
            gnodes[c].calcHidden();
            gnodes[c].drawNodeDark();
            break;
        case 1:
            gnodes[c].drawConnections();
            break;
        case 2:
            gnodes[c].drawNodeBase();
            break;
        case 3:
            gnodes[c].drawNodeSpecular();
            break;
        }

        // If we've done all the nodes in this state, move to the next state.
        if (++nextNode >= numNodes) {
            nextNode = 0;
            ++updateState;
            
            // If we've done all the states, then the cycle is over.  Note
            // that in this hack, we only do one cycle, so this terminates
            // this instance of the hack.
            if (updateState > 3) {
                updateState = 0;
                ++cycles;
            }
        }
        return cycles;
    }


    // ******************************************************************** //
    // Private Methods.
    // ******************************************************************** //

    private void makeNode(float X, float Y, float M) {
        if (numNodes < maxNodes) {
            gnodes[numNodes] = new GNode(numNodes);
            gnodes[numNodes].setPosition(X,Y);
            gnodes[numNodes].setMass(M);
            numNodes++;
        }
    }

    private void makeSpine(float X, float Y, float T, float MTime) {
        if (numSpines < maxSpines) {
            spines[numSpines] = new Spine(numSpines);
            spines[numSpines].setPosition(X,Y);
            spines[numSpines].setTheta(T);
            spines[numSpines].traceInto(MTime);
            numSpines++;
        }
    }


    // ******************************************************************** //
    // Spine Class.
    // ******************************************************************** //

    private class Spine {
        float x,y;
        float xx,yy;

        float step;

        float theta;
        float time;

        int depth = 1;
        float[] t = new float[depth];
        float[] amp = new float[depth];

        Spine(int Id) {
            init();
        }

        void init() {
            step = random(2.0f, 7.3f);
            theta = random(TWO_PI);
            for (int n = 0; n < depth; ++n) {
                amp[n] = random(0.01f, 0.3f);
                t[n] = random(0.01f, 0.2f);
            }
        }

        void setPosition(float X, float Y) {
            x = X;
            y = Y;
        }

        void setTheta(float T) {
            theta = T;
        }

        void traceInto(float MT) {
            // skip into the future
            for (time = random(MT); time < MT * 2; time += random(0.1f, 2.0f))
                grow();
        }

        void grow() {
            // save last position
            xx = x;
            yy = y;

            // calculate new position
            x += step * (float) Math.cos(theta);
            y += step * (float) Math.sin(theta);

            // rotational meander
            float thetav = 0.0f;
            for (int n = 0; n < depth; ++n) {
                thetav += amp[n] * (float) Math.sin(time * t[n]);
                amp[n] *= 0.9998f;
                t[n] *= 0.998f;
            }

            step *= 1.005f;
            //     step*=0.995;
            //     step+=0.01;
            theta += thetav;

            // render    
            draw();

            // place node?
            if (random(1000) < 61) {
                float m = random(3.21f, 5 + 500 / (1 + time));
                makeNode(x, y, m);
            }
        }  

        void draw() {
            renderPaint.setColor(0x1a555555);
            renderCanvas.drawLine(x, y, xx, yy, renderPaint);
        }
    }


    // ******************************************************************** //
    // GNode Class.
    // ******************************************************************** //

    private class GNode {
        int id;
        float x, y;
        float mass;

        // connections
        int numcons;
        int maxcons = 11;
        int[] cons;

        boolean hidden;

        int myc;

        GNode(int Id) {
            // set identification number
            id = Id;
            // create connection list
            cons = new int[maxcons];
            // initialize one time
            initSelf();
        }

        void initSelf() { 
            // initialize connections
            initConnections();
            // pick color
            myc = colourPalette.getRandom();
            hidden = false;
        }

        void initConnections() {
            // set number of connections to zero
            numcons=0;
        }

        void calcHidden() {
            // determine if hidden by larger gnode
            for (int n = 0; n < numNodes; ++n) {
                if (n != id) {
                    GNode node = gnodes[n];
                    if (node.mass > mass) {
                        float d = dist(x, y, node.x, node.y);
                        if (d < Math.abs(mass * 0.321 - node.mass * 0.321)) {
                            hidden = true;
                            break;
                        }
                    }
                }
            }
        }

        void setPosition(float X, float Y) {
            // position self
            x = X;
            y = Y;
        }

        void setMass(float Sz) {
            // set size
            mass = Sz;
        }


        void findNearConnection() {
            // find closest node
            if (numcons < maxcons && numcons < mass) {
                // sample 5% of nodes for near connection
                float dd = diameter;
                int dcid = -1;
                for (int k = 0; k < numNodes / 20; ++k) {
                    int cid = random(numNodes - 1);
                    GNode n = gnodes[cid];
                    float d = dist(x, y, n.x, n.y);
                    if (d < dd && d < mass * 6) {
                        // closer gnode has been found
                        dcid = cid;
                        dd = d;
                    }
                }

                if (dcid >= 0) {
                    // close node has been found, connect to it
                    connectTo(dcid);
                }
            }
        }

        void connectTo(int Id) {
            if (numcons < maxcons) {
                boolean duplicate = false;
                for (int n = 0; n < numcons; ++n) {
                    if (cons[n] == Id) {
                        duplicate = true;
                    }
                }
                if (!duplicate) {
                    cons[numcons] = Id;
                    numcons++;  
                }
            }
        }

        void drawNodeDark() {
            // stamp node icon down
            if (!hidden) {
                float half_mass = mass/2;
                blend(nodeIcoDark, nodeRectDark,
                        (int)(x-half_mass),(int)(y-half_mass),
                        (int)(mass),(int)(mass),
                        PorterDuff.Mode.DARKEN);  
            }
        }

        void drawNodeSpecular() {
            // stamp node specular
            if (!hidden) {
                float half_mass = mass/2;
                blend(nodeIcoSpec, nodeRectSpec,
                        (int)(x-half_mass),(int)(y-half_mass),
                        (int)(mass),(int)(mass),
                        PorterDuff.Mode.LIGHTEN);  
            }
        }

        void drawNodeBase() {
            // stamp node base
            if (!hidden) {
                float half_mass = mass/2;
                blend(nodeIcoBase, nodeRectBase,
                        (int)(x-half_mass),(int)(y-half_mass),
                        (int)(mass),(int)(mass),
                        PorterDuff.Mode.DARKEN);  
            }
        }


        void drawConnections() {
            for (int n = 0; n < numcons; n++) {
                // calculate connection distance
                float d = 4 * dist(x, y, gnodes[cons[n]].x, gnodes[cons[n]].y);
                for (int i=0;i<d;i++) {
                    // draw several points between connected gnodes  
                    float a = i/d;
                    // fuzz
                    float fx = random(-0.42f, 0.42f);
                    float fy = random(-0.42f, 0.42f);
                    float cx = fx + x+(gnodes[cons[n]].x-x) * a;
                    float cy = fy + y+(gnodes[cons[n]].y-y) * a;

                    float alpha = 0.05f + (1 - (float) Math.sin(a * PI)) * 0.16f;
                    renderPaint.setColor(myc);
                    renderPaint.setAlpha((int) (256 * alpha));
                    renderCanvas.drawPoint(cx, cy, renderPaint);
                }
            }   
        }  
    }


    // ******************************************************************** //
    // Utilities.
    // ******************************************************************** //

    private static final float dist(float x1, float y1, float x2, float y2) {
        final float dx = x2 - x1;
        final float dy = y2 - y1;
        return (float) Math.sqrt(dx * dx + dy * dy);
    }


    /**
     * Blends a region of pixels from one image into another (or in itself
     * again) with full alpha channel support.
     *
     * @param   srcImg      The source image.
     * @param   dx          X coordinate of the destinations's upper left corner
     * @param   dy          Y coordinate of the destinations's upper left corner
     * @param   dwidth      Destination image width
     * @param   dheight     Destination image height
     * @param   mode        The blending mode.
     */
    private void blend(Bitmap srcImg, Rect srcRect,
                       int dx, int dy, int dwidth, int dheight,
                       PorterDuff.Mode mode)
    {
        renderPaint.setAlpha(255);
        renderPaint.setXfermode(new PorterDuffXfermode(mode));
        Rect dst = new Rect(dx, dy, dx + dwidth, dy + dheight);
        renderCanvas.drawBitmap(srcImg, srcRect, dst, renderPaint);
        renderPaint.setXfermode(null);
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
    private static final float HALF_PI = (float) Math.PI / 2f;


    // ******************************************************************** //
    // Private Data.
    // ******************************************************************** //
    
    // Node placement pattern.  -1 means randomly select each time.
    private int nodePattern = -1;

    private int diameter;
    private int numSpines;
    private int maxSpines = 200;
    private int numNodes;
    private int maxNodes = 5000;

    private Bitmap nodeIcoDark;
    private Rect nodeRectDark;
    private Bitmap nodeIcoSpec;
    private Rect nodeRectSpec;
    private Bitmap nodeIcoBase;
    private Rect nodeRectBase;

    // collection of nodes
    private GNode[] gnodes;

    // collection of spines
    private Spine[] spines;

    // Current processing state.  Used to progress through the various
    // phases of processing we do.
    private int updateState = 0;

    // Index of the next node to be updated.  We don't update all the nodes
    // every time for performance reasons, so this keeps our place in the
    // list between updates.
    private int nextNode = 0;

}

