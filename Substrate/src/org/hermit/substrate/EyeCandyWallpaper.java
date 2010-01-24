
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

import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.PixelFormat;
import android.os.Handler;
import android.service.wallpaper.WallpaperService;
import android.view.MotionEvent;
import android.view.SurfaceHolder;


/**
 * Eye candy wallpaper.  This class provides the base infrastructure for
 * using an EyeCandy object as a live wallpaper.
 */
public class EyeCandyWallpaper
    extends WallpaperService
{

    public static final String SHARED_PREFS_NAME = "substrate_settings";

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public Engine onCreateEngine() {
        return new SubstrateEngine();
    }

    class SubstrateEngine
        extends Engine 
        implements SharedPreferences.OnSharedPreferenceChangeListener
    {

        SubstrateEngine() {
            mPrefs = EyeCandyWallpaper.this.getSharedPreferences(SHARED_PREFS_NAME, 0);
            mPrefs.registerOnSharedPreferenceChangeListener(this);
            onSharedPreferenceChanged(mPrefs, null);
        }

        public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
            String shape = prefs.getString("cube2_shape", "cube");

            // read the 3D model from the resource
        }


        @Override
        public void onCreate(SurfaceHolder surfaceHolder) {
            super.onCreate(surfaceHolder);
            setTouchEventsEnabled(true);
        }

        @Override
        public void onDestroy() {
            super.onDestroy();
            mHandler.removeCallbacks(mDrawCube);
        }

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
        public void onSurfaceChanged(SurfaceHolder holder, int format, int width, int height) {
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

            // Create a Substrate of the right size.
            substrate = new Substrate(width, height, bitmapConfig);
            
            // Draw the eye candy to set up the screen.
            drawFrame();
        }

        @Override
        public void onSurfaceCreated(SurfaceHolder holder) {
            super.onSurfaceCreated(holder);
        }

        @Override
        public void onSurfaceDestroyed(SurfaceHolder holder) {
            super.onSurfaceDestroyed(holder);
            mVisible = false;
            mHandler.removeCallbacks(mDrawCube);
        }

        @Override
        public void onOffsetsChanged(float xOffset, float yOffset,
                float xStep, float yStep, int xPixels, int yPixels) {
//            mOffset = xOffset;
            drawFrame();
        }

        /*
         * Store the position of the touch event so we can use it for drawing later
         */
        @Override
        public void onTouchEvent(MotionEvent event) {
            if (event.getAction() == MotionEvent.ACTION_MOVE) {
//                mTouchX = event.getX();
//                mTouchY = event.getY();
            } else {
//                mTouchX = -1;
//                mTouchY = -1;
            }
            super.onTouchEvent(event);
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
                if (c != null) {
                    // draw something
                    substrate.render(c);
                }
            } finally {
                if (c != null)
                    holder.unlockCanvasAndPost(c);
            }

            mHandler.removeCallbacks(mDrawCube);
            if (mVisible) {
                mHandler.postDelayed(mDrawCube, 1000 / 25);
            }
        }


        private final Handler mHandler = new Handler();

        private Substrate substrate = null;
        
        private final Runnable mDrawCube = new Runnable() {
            public void run() {
                drawFrame();
            }
        };
        
        private boolean mVisible;
        
        private SharedPreferences mPrefs;
        
    }
    
}

