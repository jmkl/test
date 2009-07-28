
/**
 * core: basic Android utilities.
 * <br>Copyright 2009 Ian Cameron Smith
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


package org.hermit.android.core;


import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;


/**
 * Common base for applications with an animated view.  This class can be
 * used in games etc.  It handles all the setup states of a SurfaceView,
 * and provides a Thread which the app can use to manage the animation.
 * 
 * <p>When using this class in an app, the app context <b>must</b> call
 * these methods (usually from its corresponding Activity methods):
 * 
 * <ul>
 * <li>{@link #onStart()}
 * <li>{@link #onResume()}
 * <li>{@link #onPause()}
 * <li>{@link #onStop()}
 * </ul>
 */
public abstract class SurfaceRunner
	extends SurfaceView
	implements SurfaceHolder.Callback
{

    // ******************************************************************** //
    // Constructor.
    // ******************************************************************** //

	/**
	 * Create a SurfaceRunner instance.
	 * 
	 * @param	app			The application context we're running in.
	 */
    public SurfaceRunner(Context app) {
        super(app);

        // Register for events on the surface.
        surfaceHolder = getHolder();
        surfaceHolder.addCallback(this);
        
        // Make a Paint for drawing performance data.
        perfPaint = new Paint();
        perfPaint.setColor(0xffff0000);

        // Create a Handler for messages to set the text overlay.
//        Handler handler = new Handler() {
//            @Override
//            public void handleMessage(Message m) {
//            	Bundle data = m.getData();
//            	int vis = data.getInt("viz");
//            	String msg = data.getString("text");
//                Log.i(TAG, "Overlay: set vis " + vis +
//             		   				" (" + (msg == null ? "" : msg) + ")");
//            }
//        };
 
		// Make sure we get key events.  TODO: check for touch mode stuff.
        setFocusable(true);
    }
    

	// ******************************************************************** //
	// State Handling.
	// ******************************************************************** //

    /**
     * This is called immediately after the surface is first created.
     * Implementations of this should start up whatever rendering code
     * they desire.
     * 
     * Note that only one thread can ever draw into a Surface, so you
     * should not draw into the Surface here if your normal rendering
     * will be in another thread.
     * 
     * @param	holder		The SurfaceHolder whose surface is being created.
     */
    public void surfaceCreated(SurfaceHolder holder) {
        Log.i(TAG, "surfaceCreated");
        setEnable(ENABLE_SURFACE);
    }


    /**
     * This is called immediately after any structural changes (format or
     * size) have been made to the surface.  This method is always
     * called at least once, after surfaceCreated(SurfaceHolder).
     * 
     * @param	holder		The SurfaceHolder whose surface has changed.
     * @param	format		The new PixelFormat of the surface.
     * @param	width		The new width of the surface.
     * @param	height		The new height of the surface.
     */
    public void surfaceChanged(SurfaceHolder holder,
    						   int format, int width, int height)
    {
        Log.i(TAG, "set size " + width + "x" + height);
        
        setSize(format, width, height);
        setEnable(ENABLE_SIZE);
    }

    
    /**
     * This is called immediately before a surface is destroyed.
     * After returning from this call, you should no longer try to
     * access this surface.  If you have a rendering thread that directly
     * accesses the surface, you must ensure that thread is no longer
     * touching the Surface before returning from this function.
     * 
     * @param	holder		The SurfaceHolder whose surface is being destroyed.
     */
    public void surfaceDestroyed(SurfaceHolder holder) {
        Log.i(TAG, "surfaceDestroyed");
        clearEnable(ENABLE_SURFACE);
    }


    /**
     * The application is starting.
     */
    public void onStart() {
        Log.i(TAG, "onStart");
        
        // Tell the subclass to start.
        appStart();
    }


    /**
     * We're resuming the app.
     */
    public void onResume() {
        Log.i(TAG, "onResume");
        
        setEnable(ENABLE_RESUMED);
    }


    /**
     * Pause the app.
     */
    public void onPause() {
        Log.i(TAG, "onPause");
        
        clearEnable(ENABLE_RESUMED);
    }


    /**
     * The application is closing down.
     */
    public void onStop() {
        Log.i(TAG, "onStop()");
        
        // Make sure we're paused.
        onPause();
        
        // Tell the subclass.
        appStop();
    }


    /**
     * Handle changes in focus.  When we lose focus, pause the game
     * so a popup (like the menu) doesn't cause havoc.
     * 
     * @param	hasWindowFocus		True iff we have focus.
     */
	@Override
	public void onWindowFocusChanged(boolean hasWindowFocus) {
//		if (!hasWindowFocus)
//			pause();
	}

    
    /**
     * Set the given enable flag, and see if we're good to go.
     * 
     * @param   flag        The flag to set.
     */
    private void setEnable(int flag) {
        boolean enabled1 = false;
        boolean enabled2 = false;
        synchronized (surfaceHolder) {
            enabled1 = (enableFlags & ENABLE_ALL) == ENABLE_ALL;
            enableFlags |= flag;
            enabled2 = (enableFlags & ENABLE_ALL) == ENABLE_ALL;
        }

        // Are we all set?
        if (!enabled1 && enabled2)
            startRun();
    }


    /**
     * Clear the given enable flag, and see if we need to shut down.
     * 
     * @param   flag        The flag to clear.
     */
    private void clearEnable(int flag) {
        boolean enabled1 = false;
        boolean enabled2 = false;
        synchronized (surfaceHolder) {
            enabled1 = (enableFlags & ENABLE_ALL) == ENABLE_ALL;
            enableFlags &= ~flag;
            enabled2 = (enableFlags & ENABLE_ALL) == ENABLE_ALL;
        }

        // Do we need to stop?
        if (enabled1 && !enabled2)
            stopRun();
    }

    
    /**
     * Start the animation running.  All the conditions we need to
     * run are present (surface, size, resumed).
     */
    private void startRun() {
        synchronized (surfaceHolder) {
            // Tell the subclass we're running.
            animStart();
            
            if (animTicker != null && animTicker.isAlive())
                animTicker.kill();
            Log.i(TAG, "set running: start ticker");
            animTicker = new Ticker();
        }
    }


    /**
     * Stop the animation running.  Our surface may have been destroyed, so
     * stop all accesses to it.
     */
    private void stopRun() {
        // Kill the thread if it's running, and wait for it to die.
        // This is important when the surface is destroyed, as we can't
        // touch the surface after we return.
        Ticker ticker = null;
        synchronized (surfaceHolder) {
            ticker = animTicker;
        }
        if (ticker != null && ticker.isAlive())
            ticker.killAndWait();
        
        // Tell the subclass we've stopped.
        animStop();
    }
    

    /**
     * Set the size of the table.
     * 
     * @param   format      The new PixelFormat of the surface.
     * @param   width       The new width of the surface.
     * @param   height      The new height of the surface.
     */
    private void setSize(int format, int width, int height) {
        synchronized (surfaceHolder) {
            canvasWidth = width;
            canvasHeight = height;

            // Create the pixmap for the background image.
            switch (format) {
            case PixelFormat.A_8:
                canvasConfig = Bitmap.Config.ALPHA_8;
                break;
            case PixelFormat.RGBA_4444:
                canvasConfig = Bitmap.Config.ARGB_4444;
                break;
            case PixelFormat.RGBA_8888:
                canvasConfig = Bitmap.Config.ARGB_8888;
                break;
            case PixelFormat.RGB_565:
                canvasConfig = Bitmap.Config.RGB_565;
                break;
            default:
                canvasConfig = Bitmap.Config.RGB_565;
            break;
            }
            
            appSize(canvasWidth, canvasHeight, canvasConfig);
        }
    }


    // ******************************************************************** //
    // Run Control.
    // ******************************************************************** //

    private void tick() {
        long now;
        
        synchronized (surfaceHolder) {
            now = System.currentTimeMillis();
            doUpdate(now);

            // If we're tracking performance, update the metrics.
            // Count microsecs, so we can display a number more than
            // 1 per second.  The granularity sucks but hopefully
            // it averages out.
            if (showPerf) {
                physTime += (System.currentTimeMillis() - now) * 1000;
                ++physCount;
            }
        }
        
        refreshScreen(now);
    }
    
    
    /**
     * Draw the game board to the screen in its current state, as a one-off.
     * This can be used to refresh the screen.
     */
    private void refreshScreen(long now) {
        Canvas canvas = null;
        try {
            canvas = surfaceHolder.lockCanvas(null);
            synchronized (surfaceHolder) {
                doDraw(canvas, now);
                
                // Show performance data, if required.
                if (showPerf) {
                    // Count frames per second.
                    ++fpsSinceLast;
                    
                    // If it's time to make a new displayed total, tot up the figures
                    // and reset the running counts.
                    if (now - perfLastTime > 1000) {
                        fpsLastCount = fpsSinceLast;
                        fpsSinceLast = 0;
                        physLastAvg = physCount == 0 ? 0 : physTime / physCount;
                        physTime = 0;
                        physCount = 0;
                        perfLastTime = now;
                    }
                    
                    // Draw the FPS and average physics time on screen.
                    canvas.drawText("" + fpsLastCount + " fps", 4, 12, perfPaint);
                    canvas.drawText("" + physLastAvg + " µs", 4, 24, perfPaint);
                }
            }
        } finally {
            // do this in a finally so that if an exception is thrown
            // during the above, we don't leave the Surface in an
            // inconsistent state
            if (canvas != null)
                surfaceHolder.unlockCanvasAndPost(canvas);
        }
    }


    // ******************************************************************** //
    // Client Methods.
    // ******************************************************************** //

    /**
     * The application is starting.  Perform any initial set-up prior to
     * starting the application.  We may not have a screen size yet,
     * so this is not a good place to allocate resources which depend on
     * that.
     */
    protected abstract void appStart();
    

    /**
     * Set the screen size.  This is guaranteed to be called before
     * animStart(), but perhaps not before appStart().
     * 
     * @param   width       The new width of the surface.
     * @param   height      The new height of the surface.
     * @param   config      The pixel format of the surface.
     */
    protected abstract void appSize(int width, int height, Bitmap.Config config);
    

    /**
     * We are starting the animation loop.  The screen size is known.
     * 
     * <p>doUpdate() and doDraw() may be called from this point on.
     */
    protected abstract void animStart();
    

    /**
     * We are stopping the animation loop, for example to pause the app.
     * 
     * <p>doUpdate() and doDraw() will not be called from this point on.
     */
    protected abstract void animStop();
    

    /**
     * The application is closing down.  Clean up any resources.
     */
    protected abstract void appStop();
    
  
    /**
     * Update the state of the application for the current frame.
     * 
     * <p>Applications must override this, and can use it to update
     * for example the physics of a game.  This may be a no-op in some cases.
     * 
     * <p>doDraw() will always be called after this method is called;
     * however, the converse is not true, as we sometimes need to draw
     * just to update the screen.  Hence this method is useful for
     * updates which are dependent on time rather than frames.
     * 
     * @param   now         Current time in ms.
     */
    protected abstract void doUpdate(long now);

    
    /**
     * Draw the current frame of the application.
     * 
     * <p>Applications must override this, and are expected to draw the
     * entire screen into the provided canvas.
     * 
     * <p>This method will always be called after a call to doUpdate(),
     * and also when the screen needs to be re-drawn.
     * 
     * @param   canvas      The Canvas to draw into.
     * @param   now         Current time in ms.  Will be the same as that
     *                      passed to doUpdate(), if there was a preceeding
     *                      call to doUpdate().
     */
    protected abstract void doDraw(Canvas canvas, long now);


    // ******************************************************************** //
    // Client Utilities.
    // ******************************************************************** //

    /**
     * Get a Bitmap which is the same size and format as the screen.
     * This can be used to get an off-screen rendering buffer, for
     * example.
     * 
     * @return              A Bitmap which is the same size and pixel
     *                      format as the screen.
     */
    protected Bitmap getBitmap() {
        return Bitmap.createBitmap(canvasWidth, canvasHeight, canvasConfig);
    }
    

    // ******************************************************************** //
    // Debug Control.
    // ******************************************************************** //

    /**
     * Turn display of performance info on or off.
     * 
     * @param   enable      True to enable performance display.
     */
    public void setDebugPerf(boolean enable) {
        showPerf = enable;
    }
    
    
    // ******************************************************************** //
    // Save and Restore.
    // ******************************************************************** //

	/**
	 * Save the state of the game in the provided Bundle.
	 * 
     * @param   icicle      The Bundle in which we should save our state.
	 */
    protected void saveState(Bundle icicle) {
//		gameTable.saveState(icicle);
	}

	
	/**
	 * Restore the game state from the given Bundle.
	 * 
	 * @param	icicle		The Bundle containing the saved state.
	 */
    protected void restoreState(Bundle icicle) {
//		gameTable.pause();
//		gameTable.restoreState(icicle);
	}
	

    // ******************************************************************** //
    // Private Classes.
    // ******************************************************************** //

	/**
	 * Thread class we use to control the animation.  The contract of this
	 * class is:
	 *   * When created, we start at once
	 *   * While running, tick() in the outer class is called periodically
	 *   * When killAndWait() is called, we stop and return
	 */
	private class Ticker
	    extends Thread
	{

	    /**
	     * Constructor -- start at once.
	     */
	    private Ticker() {
	        Log.v(TAG, "Ticker: start");
	        enable = true;
	        start();
	    }

	    /**
	     * Stop this thread.  There will be no new calls to tick() after this.
	     */
	    private void kill() {
	        Log.v(TAG, "Ticker: kill");
	        enable = false;
	    }

	    /**
	     * Stop this thread and wait for it to die.  When we return, it is
	     * guaranteed that tick() will never be called again.
	     * 
	     * Caution: if this is called from within tick(), deadlock is
	     * guaranteed.
	     */
	    private void killAndWait() {
	        Log.v(TAG, "Ticker: killAndWait");
	        enable = false;

	        // Wait for the thread to finish.  Ignore interrupts.
	        if (isAlive()) {
	            boolean retry = true;
	            while (retry) {
	                try {
	                    join();
	                    retry = false;
	                } catch (InterruptedException e) {
	                }
	            }
	            Log.v(TAG, "Ticker: killed");
	        } else {
	            Log.v(TAG, "Ticker: was dead");
	        }
	    }

	    /**
	     * Run method for this thread -- simply call tick() a lot until
	     * enable is false.
	     */
	    @Override
	    public void run() {
	        while (enable)
	            tick();
	    }
	    
	    // Flag used to terminate this thread -- when false, we die.
	    private boolean enable = false;
	}


    // ******************************************************************** //
    // Class Data.
    // ******************************************************************** //

    // Debugging tag.
	@SuppressWarnings("unused")
	private static final String TAG = "SurfaceRunner";

    // Enable flags.  In order to run, we need onSurfaceCreated() and
    // onResume(), which can come in either order.  So we track which ones
    // we have by these flags.  When all are set, we're good to go.  Note
    // that this is distinct from the game state machine, and its pause
    // and resume actions -- the whole game is enabled by the combination
    // of these flags set in enableFlags.
    private static final int ENABLE_SURFACE = 0x01;
    private static final int ENABLE_SIZE = 0x02;
    private static final int ENABLE_RESUMED = 0x04;
    private static final int ENABLE_ALL =
                        ENABLE_SURFACE | ENABLE_SIZE | ENABLE_RESUMED;

	
	// ******************************************************************** //
	// Private Data.
	// ******************************************************************** //
    
    // The surface manager for the view.
    private SurfaceHolder surfaceHolder = null;

    // Enablement flags; see comment above.
    private int enableFlags = 0;
    
    // Width, height and pixel format of the surface.
    private int canvasWidth = 0;
    private int canvasHeight = 0;
    private Bitmap.Config canvasConfig = null;

    // The ticker thread which runs the animation.
    private Ticker animTicker = null;

    // Data for counting frames per second.  Value displayed at last
    // update, time in system ms of last update, frames since last update.
    private int fpsLastCount = 0;
    private int fpsSinceLast = 0;

    // Display performance data on-screen.
    private boolean showPerf = false;

    // Paint for drawing performance data.
    private Paint perfPaint = null;
    
    // Data for monitoring physics performance.  We count the total number
    // of ms spent doing physics since last update, and number of physics
    // passes since last update; and keep the last displayed average time.
    private long physTime = 0;
    private int physCount = 0;
    private long physLastAvg = 0;
    
    // Time of last performance display update.  Used for both FPS and physics.
    private long perfLastTime = 0;

}

