
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

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.PixelFormat;
import android.os.Handler;
import android.service.wallpaper.WallpaperService;
import android.util.Log;
import android.view.SurfaceHolder;


/**
 * Eye candy wallpaper.  This class provides the base infrastructure for
 * using an EyeCandy object as a live wallpaper.
 */
public abstract class EyeCandyWallpaper
    extends WallpaperService
{

    // ******************************************************************** //
    // Service Methods.
    // ******************************************************************** //

    /**
     * This method is invoked to create an Engine instance.
     * 
     * @return          A new instance of this wallpaper's engine.
     */
    @Override
    public Engine onCreateEngine() {
        return new EyeCandyEngine();
    }


    /**
     * This method is invoked to create an instance of the eye candy
     * this wallpaper displays.  Subclasses must implement this to
     * return the appropriate hack.
     * 
     * @param  context      Our application context.
     * @return              A new instance of the eye candy to display.
     */
    public abstract EyeCandy onCreateHack(Context context);
    

    // ******************************************************************** //
    // Wallpaper Engine.
    // ******************************************************************** //

    /**
     * Wallpaper engine for displaying an eye candy.
     */
    class EyeCandyEngine
        extends Engine 
    {

        /**
         * Create an Engine instance.
         */
        EyeCandyEngine() {
            eyeCandy = onCreateHack(EyeCandyWallpaper.this);
        }


        /**
         * Called once to initialize the engine.  After returning, the
         * engine's surface will be created by the framework.
         * 
         * @param   surfaceHolder       Surface we're running in.
         */
        @Override
        public void onCreate(SurfaceHolder surfaceHolder) {
            super.onCreate(surfaceHolder);
            setTouchEventsEnabled(true);
        }

        
        /**
         * Called right before the engine is going away.  After this the
         * surface will be destroyed and this Engine object is no longer valid. 
         */
        @Override
        public void onDestroy() {
            super.onDestroy();
            mHandler.removeCallbacks(mDrawCube);
        }


        /**
         * This is called immediately after the surface is first created.
         * Implementations of this should start up whatever rendering code
         * they desire.  Note that only one thread can ever draw into a
         * Surface, so you should not draw into the Surface here if your
         * normal rendering will be in another thread.
         * 
         * @param   holder  The SurfaceHolder whose surface is being created. 
         */
        @Override
        public void onSurfaceCreated(SurfaceHolder holder) {
            super.onSurfaceCreated(holder);
        }

        
        /**
         * This is called immediately after any structural changes (format
         * or size) have been made to the surface.  You should at this point
         * update the imagery in the surface.  This method is always called
         * at least once, after surfaceCreated(SurfaceHolder).
         *
         * @param   holder  The SurfaceHolder whose surface has changed.
         * @param   format  The new PixelFormat of the surface.
         * @param   width   The new width of the surface.
         * @param   height  The new height of the surface. 
         */
        @Override
        public void onSurfaceChanged(SurfaceHolder holder, int format,
                                     int width, int height)
        {
            super.onSurfaceChanged(holder, format, width, height);
            
            // Create the pixmap for the background image.
            Bitmap.Config bitmapConfig;
            switch (format) {
            case PixelFormat.A_8:
                bitmapConfig = Bitmap.Config.ALPHA_8;
                break;
            case PixelFormat.RGBA_4444:
                bitmapConfig = Bitmap.Config.ARGB_4444;
                break;
            case PixelFormat.RGBA_8888:
                bitmapConfig = Bitmap.Config.ARGB_8888;
                break;
            case PixelFormat.RGB_565:
                bitmapConfig = Bitmap.Config.RGB_565;
                break;
            default:
                bitmapConfig = Bitmap.Config.RGB_565;
                break;
            }
            
            Log.v(TAG, "Ssize: " + width + "," +  height);
            screenWidth = width;
            screenHeight = height;

            wallpaperWidth = width * WALLPAPER_MULT_X;
            wallpaperHeight = height * WALLPAPER_MULT_Y;
            Log.v(TAG, "Wsize: " + wallpaperWidth + "," +  wallpaperHeight);

            eyeCandy.setConfiguration(wallpaperWidth, wallpaperHeight, bitmapConfig);
            
            // Draw the eye candy to set up the screen.
            drawFrame();
        }

        
        /**
         * This is called immediately before a surface is being destroyed.
         * After returning from this call, you should no longer try to access
         * this surface.  If you have a rendering thread that directly
         * accesses the surface, you must ensure that thread is no longer
         * touching the Surface before returning from this function.
         * 
         * @param   holder  The SurfaceHolder whose surface is being destroyed. 
         */
        @Override
        public void onSurfaceDestroyed(SurfaceHolder holder) {
            super.onSurfaceDestroyed(holder);
            mVisible = false;
            mHandler.removeCallbacks(mDrawCube);
        }
        
        
        /**
         * Called to inform you of the wallpaper becoming visible or hidden.
         * 
         * <p><i>It is very important that a wallpaper only use CPU while it
         * is visible.</i>
         */
        @Override
        public void onVisibilityChanged(boolean visible) {
            mVisible = visible;
            if (visible) {
                drawFrame();
            } else {
                mHandler.removeCallbacks(mDrawCube);
            }
        }


        /**
         * Called to inform you of the wallpaper's offsets changing within
         * its container.  The X and Y offsets are floating point numbers
         * ranging from 0 to 1, representing where the wallpaper should be
         * positioned within the screen space.  These only make sense when
         * the wallpaper is larger than the screen.
         * 
         * @param   xOffset     The offset along the X dimension, from 0 to
         *                      1, representing where the wallpaper should be
         *                      positioned within the screen space.  These
         *                      only make sense when the wallpaper is larger
         *                      than the screen.  Zero indicates the user
         *                      has panned to the left screen; 1 is fully right.
         * @param   yOffset     The offset along the Y dimension, from 0 to 1. 
         * @param   xStep       The X offset delta from one screen to the
         *                      next one.  For example, if the launcher has
         *                      3 virtual screens, it would specify an xStep
         *                      of 0.5, since the X offset for those screens
         *                      are 0.0, 0.5 and 1.0.
         * @param   yStep       The Y offset delta from one screen to the next.
         * @param   xPixels     X pixel offset.  Zero indicates the user
         *                      has panned to the left screen; -screenwidth
         *                      is fully right.
         * @param   yPixels     Y pixel offset.
         */
        @Override
        public void onOffsetsChanged(float xOffset, float yOffset,
                                     float xStep, float yStep,
                                     int xPixels, int yPixels)
        {
            // Set the pixel offsets to draw at based on the panning offset.
            drawXOff = Math.round((wallpaperWidth - screenWidth) * -xOffset);
            drawYOff = Math.round((wallpaperHeight - screenHeight) * -yOffset);

            drawFrame();
        }

        
        /*
         * Draw one frame of the animation. This method gets called repeatedly
         * by posting a delayed Runnable. You can do any drawing you want in
         * here. This example draws a wireframe cube.
         */
        private void drawFrame() {
            final SurfaceHolder holder = getSurfaceHolder();

            Canvas c = null;
            try {
                c = holder.lockCanvas();
                if (c != null)
                    eyeCandy.render(c, drawXOff, drawYOff);
            } finally {
                if (c != null)
                    holder.unlockCanvasAndPost(c);
            }

            mHandler.removeCallbacks(mDrawCube);
            if (mVisible)
                mHandler.postDelayed(mDrawCube, eyeCandy.getSleepTime());
        }


        private final Handler mHandler = new Handler();
        
        private final Runnable mDrawCube = new Runnable() {
            public void run() {
                eyeCandy.update();
                drawFrame();
            }
        };
        
        
        // ******************************************************************** //
        // Class Data.
        // ******************************************************************** //

        // Debugging tag.
        @SuppressWarnings("unused")
        private static final String TAG = "Substrate";

        // Amount by which the wallpaper is bigger than the screen.
        private static final int WALLPAPER_MULT_X = 2;
        private static final int WALLPAPER_MULT_Y = 1;
        

        // ******************************************************************** //
        // Private Data.
        // ******************************************************************** //

        // The size of the screen we're drawing into.  Zero if not known yet.
        private int screenWidth = 0;
        private int screenHeight = 0;

        // The desired size for the wallpaper.
        private int wallpaperWidth = 0;
        private int wallpaperHeight = 0;
        
        // Drawing offsets in pixels, representing where the wallpaper should
        // be positioned within the screen space.  These only make sense when
        // the wallpaper is larger than the screen.
        private int drawXOff = 0;
        private int drawYOff = 0;

        // The screen hack to be displayed.
        private EyeCandy eyeCandy = null;
        
        private boolean mVisible;
        
    }

 }

