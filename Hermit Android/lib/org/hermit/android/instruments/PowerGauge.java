
/**
 * org.hermit.android.instrument: graphical instruments for Android.
 * <br>Copyright 2009 Ian Cameron Smith
 * 
 * <p>These classes provide input and display functions for creating on-screen
 * instruments of various kinds in Android apps.
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


package org.hermit.android.instruments;


import org.hermit.android.core.SurfaceRunner;
import org.hermit.utils.CharFormatter;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Paint.Style;


/**
 * A graphical display which displays the signal power in dB from an
 * {@link AudioAnalyser} instrument.  This class cannot be instantiated
 * directly; get an instance by calling
 * {@link AudioAnalyser#getPowerGauge(SurfaceRunner)}.
 */
public class PowerGauge
    extends Gauge
{

	// ******************************************************************** //
	// Constructor.
	// ******************************************************************** //
	
	/**
	 * Create a PowerGauge.  This constructor is package-local, as
	 * public users get these from an {@link AudioAnalyser} instrument.
	 * 
	 * @param	parent			Parent surface.
	 */
	PowerGauge(SurfaceRunner parent) {
	    super(parent);
        
        meterPeaks = new float[METER_PEAKS];
        meterPeakTimes = new long[METER_PEAKS];
        powerHistory = new float[METER_AVERAGE_COUNT];
        dbBuffer = "-100.0dB".toCharArray();
	}


	// ******************************************************************** //
	// Geometry.
	// ******************************************************************** //

    /**
     * This is called during layout when the size of this element has
     * changed.  This is where we first discover our size, so set
     * our geometry to match.
     * 
	 * @param	bounds		The bounding rect of this element within
	 * 						its parent View.
     */
	@Override
    public void setGeometry(Rect bounds) {
	    super.setGeometry(bounds);
	    
	    dispX = bounds.left;
	    dispY = bounds.top;
	    dispWidth = bounds.width();
	    dispHeight = bounds.height();
        
        // Do some layout within the meter.
        int mw = dispWidth;
        int mh = dispHeight;
        meterBarY = 0;
        meterBarHeight = (int) (mh / 6f + 16f);
        meterBarGap = meterBarHeight / 4;
        meterLabSize = Math.min(meterBarHeight / 2, mw / 24);
        meterLabY = meterBarY + meterBarHeight + meterLabSize + 2;
        meterTextSize = Math.min(mh - (meterBarHeight + meterLabSize + 2), mw / 6);
        if (meterTextSize > 64)
            meterTextSize = 64;
        meterTextY = meterLabY + meterTextSize + 4;
        meterBarMargin = meterLabSize;
        
        // Create the bitmap for the background,
        // and the Canvas for drawing into it.
        backgroundBitmap = getSurface().getBitmap(dispWidth, dispHeight);
        backgroundCanvas = new Canvas(backgroundBitmap);
        
        drawBackgroundBody(backgroundCanvas, getPaint());
	}


    // ******************************************************************** //
    // Background Drawing.
    // ******************************************************************** //
    
    /**
     * Do the subclass-specific parts of drawing the background
     * for this element.  Subclasses should override
     * this if they have significant background content which they would
     * like to draw once only.  Whatever is drawn here will be saved in
     * a bitmap, which will be rendered to the screen before the
     * dynamic content is drawn.
     * 
     * <p>Obviously, if implementing this method, don't clear the screen when
     * drawing the dynamic part.
     * 
     * @param   canvas      Canvas to draw into.
     * @param   paint       The Paint which was set up in initializePaint().
     */
    @Override
    protected void drawBackgroundBody(Canvas canvas, Paint paint) {
        canvas.drawColor(0xff000000);
        
        paint.setColor(0xffffff00);
        paint.setStyle(Style.STROKE);

        // Draw the grid.
        final int mx = 0 + meterBarMargin;
        final int mw = dispWidth - meterBarMargin * 2;
        final int by = 0 + meterBarY;
        final int bh = meterBarHeight;
        final float bw = mw - 1f;
        final float gw = bw / 10f;
        canvas.drawRect(mx, by, mx + bw - 1, by + bh - 1, paint);
        for (float i = 0f; i < bw; i += gw) {
            canvas.drawLine(mx + i + 1, by, mx + i + 1, by + bh - 1, paint);
        }

        // Draw the labels below the grid.
        final int ly = 0 + meterLabY;
        final int ls = meterLabSize;
        paint.setTextSize(ls);
        for (int i = 0; i <= 10; ++i) {
            String text = "" + (i * 10 - 100);
            float tw = paint.measureText(text);
            float lx = mx + i * gw + 1 - (tw / 2);
            canvas.drawText(text, lx, ly, paint);
        }
    }


    // ******************************************************************** //
    // Data Updates.
    // ******************************************************************** //
    
	/**
	 * New data from the instrument has arrived.  This method is called
	 * on the thread of the instrument.
	 * 
     * @param   power       The current instantaneous signal power level.
	 */
    final void update(float power) {
        synchronized (this) {
            // Save the current level.
            currentPower = power;

            // Get the previous power value, and add the new value into the
            // history buffer.  Re-calculate the rolling average power value.
            if (++historyIndex >= powerHistory.length)
                historyIndex = 0;
            prevPower = powerHistory[historyIndex];
            powerHistory[historyIndex] = power;
            averagePower -= prevPower / METER_AVERAGE_COUNT;
            averagePower += power / METER_AVERAGE_COUNT;
        }
    }

    
	// ******************************************************************** //
	// View Drawing.
	// ******************************************************************** //
	
	/**
	 * Do the subclass-specific parts of drawing for this element.
	 * This method is called on the thread of the containing SuraceView.
	 * 
	 * <p>Subclasses should override this to do their drawing.
	 * 
	 * @param	canvas		Canvas to draw into.
	 * @param	paint		The Paint which was set up in initializePaint().
     * @param   now         Nominal system time in ms. of this update.
	 */
	@Override
    protected final void drawBody(Canvas canvas, Paint paint, long now) {
	    synchronized (this) {
	        // Re-calculate the peak markers.
	        calculatePeaks(now, currentPower, prevPower);

	        paint.setColor(0xffffff00);
	        paint.setStyle(Style.STROKE);

	        // Position parameters.
	        final int mx = dispX + meterBarMargin;
	        final int mw = dispWidth - meterBarMargin * 2;
	        final int by = dispY + meterBarY;
	        final int bh = meterBarHeight;
	        final float gap = meterBarGap;
	        final float bw = mw - 2f;
	        
	        canvas.drawBitmap(backgroundBitmap, dispX, dispY, paint);
	        
	        // Draw the average bar.
	        final float pa = averagePower * bw;
	        paint.setStyle(Style.FILL);
	        paint.setColor(METER_AVERAGE_COL);
	        canvas.drawRect(mx + 1, by + 1, mx + pa + 1, by + bh - 1, paint);

	        // Draw the power bar.
	        final float p = currentPower * bw;
	        paint.setStyle(Style.FILL);
	        paint.setColor(METER_POWER_COL);
	        canvas.drawRect(mx + 1, by + gap, mx + p + 1, by + bh - gap, paint);

	        // Now, draw in the peaks.
	        paint.setStyle(Style.FILL);
	        for (int i = 0; i < METER_PEAKS; ++i) {
	            if (meterPeakTimes[i] != 0) {
	                // Fade the peak according to its age.
	                long age = now - meterPeakTimes[i];
	                float fac = 1f - ((float) age / (float) METER_PEAK_TIME);
	                int alpha = (int) (fac * 255f);
	                paint.setColor(METER_PEAK_COL | (alpha << 24));
	                // Draw it in.
	                final float pp = meterPeaks[i] * bw;
	                canvas.drawRect(mx + pp - 1, by + gap,
	                        mx + pp + 3, by + bh - gap, paint);
	            }
	        }

	        // Draw the text below the meter.
	        final int ty = dispY + meterTextY;
	        final int ts = meterTextSize;
	        final float dB = (averagePower - 1f) * 100f;
	        CharFormatter.formatFloat(dbBuffer, 0, dB, 6, 1);
	        paint.setStyle(Style.STROKE);
	        paint.setColor(0xff00ffff);
	        paint.setTextSize(ts);

	        canvas.drawText(dbBuffer, 0, dbBuffer.length, mx, ty, paint);
	    }
	}
	

    /**
     * Re-calculate the positions of the peak markers in the VU meter.
     */
    private final void calculatePeaks(long now, float power, float prev) {
        // First, delete any that have been passed or have timed out.
        for (int i = 0; i < METER_PEAKS; ++i) {
            if (meterPeakTimes[i] != 0 &&
                    (meterPeaks[i] < power ||
                     now - meterPeakTimes[i] > METER_PEAK_TIME))
                meterPeakTimes[i] = 0;
        }
        
        // If the meter has gone up, set a new peak, if there's an empty
        // slot.  If there isn't, don't bother, because we would be kicking
        // out a higher peak, which we don't want.
        if (power > prev) {
            boolean done = false;
            
            // First, check for a slightly-higher existing peak.  If there
            // is one, just bump its time.
            for (int i = 0; i < METER_PEAKS; ++i) {
                if (meterPeakTimes[i] != 0 && meterPeaks[i] - power < 0.025) {
                    meterPeakTimes[i] = now;
                    done = true;
                    break;
                }
            }
            
            if (!done) {
                // Now scan for an empty slot.
                for (int i = 0; i < METER_PEAKS; ++i) {
                    if (meterPeakTimes[i] == 0) {
                        meterPeaks[i] = power;
                        meterPeakTimes[i] = now;
                        break;
                    }
                }
            }
        }
    }
    

	// ******************************************************************** //
	// Class Data.
	// ******************************************************************** //

    // Debugging tag.
	@SuppressWarnings("unused")
	private static final String TAG = "instrument";

    // Number of peaks we will track in the VU meter.
    private static final int METER_PEAKS = 4;

    // Time in ms over which peaks in the VU meter fade out.
    private static final int METER_PEAK_TIME = 4000;

    // Number of updates over which we average the VU meter to get
    // a rolling average.  32 is about 2 seconds.
    private static final int METER_AVERAGE_COUNT = 32;

    // Colours for the meter power bar and average bar and peak marks.
    // In METER_PEAK_COL, alpha is set dynamically in the code.
    private static final int METER_POWER_COL = 0xff0000ff;
    private static final int METER_AVERAGE_COL = 0xa0ff9000;
    private static final int METER_PEAK_COL = 0x00ff0000;


	// ******************************************************************** //
	// Private Data.
	// ******************************************************************** //
    
	// Display position and size within the parent view.
    private int dispX = 0;
    private int dispY = 0;
	private int dispWidth = 0;
	private int dispHeight = 0;

    // Layout parameters for the VU meter.  Position and size for the
    // bar itself; position and size for the bar labels; position
    // and size for the main readout text.
    private int meterBarY = 0;
    private int meterBarHeight = 0;
    private int meterBarGap = 0;
    private int meterLabY = 0;
    private int meterLabSize = 0;
    private int meterTextY = 0;
    private int meterTextSize = 0;
    private int meterBarMargin = 0;

    // Bitmap in which we draw the gauge background,
    // and the Canvas and Paint for drawing into it.
    private Bitmap backgroundBitmap = null;
    private Canvas backgroundCanvas = null;

    // Current and previous power levels.
    private float currentPower = 0f;
    private float prevPower = 0f;
    
    // Buffered old meter levels, used to calculate the rolling average.
    // Index of the most recent value.
    private float[] powerHistory = null;
    private int historyIndex = 0;
    
    // Rolling average power value,  calculated from the history buffer.
    private float averagePower = 0f;
    
    // Peak markers in the VU meter, and the times for each one.  A zero
    // time indicates a peak not set.
    private float[] meterPeaks = null;
    private long[] meterPeakTimes = null;

    // Buffer for displayed dB value text.
    private char[] dbBuffer = null;

}

