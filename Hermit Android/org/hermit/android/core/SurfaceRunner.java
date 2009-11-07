
/**
 * org.hermit.android.core: useful Android foundation classes.
 * 
 * These classes are designed to help build various types of application.
 *
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


import org.hermit.utils.CharFormatter;
import org.hermit.utils.CharFormatter.OverflowException;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Typeface;
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
 * 
 * <p>The surface is enabled once it is created and sized, and
 * {@link #onStart()} and {@link #onResume()} have been called.  You then
 * start and stop it by calling {@link #surfaceStart()} and
 * {@link #surfaceStop()}.
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
     * Start the surface running.
     */
    public void surfaceStart() {
        Log.i(TAG, "surfaceStart");
        
        setEnable(ENABLE_STARTED);
    }


    /**
     * Stop the surface running.
     */
    public void surfaceStop() {
        Log.i(TAG, "surfaceStop");
        
        clearEnable(ENABLE_STARTED);
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
		if (!hasWindowFocus)
		    clearEnable(ENABLE_FOCUSED);
		else
            setEnable(ENABLE_FOCUSED);
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
        
        // Do the stats setup.
        statsInit();
    }

    
    // ******************************************************************** //
    // Run Control.
    // ******************************************************************** //

    private void tick() {
        // Do the application's physics.
        long now = System.currentTimeMillis();
        doUpdate(now);
        if (showPerf)
            statsTimeInt(1, (System.currentTimeMillis() - now) * 1000);
        
        // And update the screen.
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
                long drawStart = System.currentTimeMillis();
                doDraw(canvas, now);
                if (showPerf) {
                    long drawEnd = System.currentTimeMillis();
                    statsTimeInt(2, (drawEnd - drawStart) * 1000);
                }

                // Show performance data, if required.
                if (showPerf) {
                    // Count frames per second.
                    statsCountInt(0, 1);
                    
                    // If it's time to make a new displayed total, tot up
                    // the figures and reset the running counts.
                    if (now - perfLastTime > 1000) {
                        statsDraw();
                        perfLastTime = now;
                    }

                    // Draw the stats on screen.
                    canvas.drawBitmap(perfBitmap, 0, 0, null);
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
    

    /**
     * Get a Bitmap of a given size, in the same format as the screen.
     * This can be used to get an off-screen rendering buffer, for
     * example.
     * 
     * @param   w           Desired width in pixels.
     * @param   h           Desired height in pixels.
     * @return              A Bitmap which is the same size and pixel
     *                      format as the screen.
     */
    protected Bitmap getBitmap(int w, int h) {
        return Bitmap.createBitmap(w, h, canvasConfig);
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
    // Stats Handling.
    // ******************************************************************** //

    /**
     * Reserve space in the stats display for some application performance
     * stats.  The given labels will become stats which will be displayed if
     * {@link #setDebugPerf(boolean enable)} is passed true.  Each stat is
     * subsequently referred to by its index in this labels array.
     * 
     * <p>This method must be called before appStart() in order for the
     * app's stats to be displayed.  After appStart() is called, the stats
     * content is frozen until the next appStop() / appStart().
     * Typically the app should invoke this method from its constructor.
     * However this method is, of course, optional.
     * 
     * @param   labels          Labels for the app's stats, one label
     *                          per stat.  Labels need to be 7 chars or less.
     */
    protected void statsCreate(String[] labels) {
        perfAppLabels = labels;
    }
    
    
    /**
     * Set up the stats display for the next run.  At this point the stats
     * the app has prepared by calling statsCreate() are frozen.
     */
    private void statsInit() {
        // Make a Paint for drawing performance data.
        perfPaint = new Paint();
        perfPaint.setColor(0xffff0000);
        perfPaint.setTypeface(Typeface.MONOSPACE);
        
        // Set up buffers for the performance data OSD.  Make space for the
        // app's stats as well as our own.
        int nstats = 3;
        if (perfAppLabels != null)
            nstats += perfAppLabels.length;
        
        perfBuffers = new char[nstats][];
        int i;
        for (i = 0; i < nstats; ++i)
            perfBuffers[i] = new char[14];
        i = 0;
        CharFormatter.formatString(perfBuffers[i++], 6, " fps", 8);
        CharFormatter.formatString(perfBuffers[i++], 6, " µs phys", 8);
        CharFormatter.formatString(perfBuffers[i++], 6, " µs draw", 8);
        if (perfAppLabels != null)
            for (String alab : perfAppLabels)
                CharFormatter.formatString(perfBuffers[i++], 6, " " + alab, 8);
        
        // Now make a bitmap for the stats.
        perfBitmap = Bitmap.createBitmap(100, nstats * 12 + 4, canvasConfig);
        perfCanvas = new Canvas(perfBitmap);
        
        // Make the values and counts arrays.
        perfStats = new int[nstats];
        perfCounts = new int[nstats];
    }
    
    
    /**
     * Increment a performance counter.  This method is used for counts
     * of specific quantities, which will be displayed as counts per second;
     * for example frames per second.
     * 
     * @param   index       Index of the stat to bump (its index in the
     *                      "labels" argument to
     *                      {@link #statsCreate(String[] labels)}).
     * @param   val         Amount to add to the counter.
     */
    protected void statsCount(int index, int val) {
        statsCountInt(index + 3, val);
    }
    
    
    /**
     * Increment a performance counter.  This method is used for counts
     * of specific quantities, which will be displayed as counts per second;
     * for example frames per second.
     * 
     * @param   index       Index of the stat to bump (its absolute index,
     *                      which includes internal stats).
     * @param   val         Amount to add to the counter.
     */
    private void statsCountInt(int index, int val) {
        if (val < 0)
            return;
        if (showPerf && index >= 0 && index < perfStats.length)
            perfStats[index] += val;
    }
    

    /**
     * Record a performance timer.  This method is used for timings
     * of specific activities; the average of the recorded values will 
     * be displayed.
     * 
     * @param   index       Index of the stat to record (its index in the
     *                      "labels" argument to
     *                      {@link #statsCreate(String[] labels)}).
     * @param   val         The time value for this iteration.
     */
    protected void statsTime(int index, long val) {
        statsTimeInt(index + 3, val);
    }
    
    
    /**
     * Record a performance timer.  This method is used for timings
     * of specific activities; the average of the recorded values will 
     * be displayed.
     * 
     * @param   index       Index of the stat to record (its absolute index,
     *                      which includes internal stats).
     * @param   val         The time value for this iteration.
     */
    private void statsTimeInt(int index, long val) {
        if (val < 0)
            return;
        if (showPerf && index >= 0 && index < perfStats.length) {
            perfStats[index] += (int) val;
            ++perfCounts[index];
        }
    }
    
   
    /**
     * Draw the stats into perfBitmap.
     */
    private void statsDraw() {
        // Format all the values we have.
        for (int i = 0; i < perfStats.length && i < perfBuffers.length; ++i) {
            try {
                int v = perfStats[i];
                int c = perfCounts[i];
                if (c != 0)
                    v /= c;
                CharFormatter.formatInt(perfBuffers[i], 0, v, 6, false);
            } catch (OverflowException e) {
                Log.e(TAG, "Formatting Perf: " + e.getMessage());
            }
        }
        
        // Draw the stats into the canvas.
        perfCanvas.drawColor(0xff000000);
        for (int i = 0; i < perfBuffers.length; ++i)
            perfCanvas.drawText(perfBuffers[i], 0, perfBuffers[i].length,
                                0, i * 12 + 12, perfPaint);
        
        // Reset all stored stats.
        for (int i = 0; i < perfStats.length && i < perfBuffers.length; ++i) {
            perfStats[i] = 0;
            perfCounts[i] = 0;
        }
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
	        super("Surface Runner");
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
    private static final int ENABLE_STARTED = 0x08;
    private static final int ENABLE_FOCUSED = 0x10;
    private static final int ENABLE_ALL =
               ENABLE_SURFACE | ENABLE_SIZE | ENABLE_RESUMED |
               ENABLE_STARTED | ENABLE_FOCUSED;

	
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

    // Display performance data on-screen.
    private boolean showPerf = false;

    // Labels for app-supplied performance stats.
    private String[] perfAppLabels = null;
    
    // Stored perf stats values for all stats.  Counters for each stat.
    private int[] perfStats = null;
    private int[] perfCounts = null;

    // Character buffers for performance / stats annotations.
    private char[][] perfBuffers;

    // Bitmap for drawing the stats into.  Canvas for drawing into it.
    private Bitmap perfBitmap = null;
    private Canvas perfCanvas = null;
    
    // Paint for drawing performance data.
    private Paint perfPaint = null;
    
    // Time of last performance display update.
    private long perfLastTime = 0;

}

