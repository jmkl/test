
/**
 * On Watch: sailor's watchkeeping assistant.
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


package org.hermit.onwatch;

import java.util.Calendar;

import org.hermit.astro.Body;
import org.hermit.onwatch.provider.WeatherSchema;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewParent;


/**
 * This custom widget displays the recorded weather as a chart.  This
 * currently shows the recorded barometric pressure.
 *
 * @author	Ian Cameron Smith
 */
public class WeatherWidget
	extends View
{

	// ******************************************************************** //
    // Public Constants.
    // ******************************************************************** //

    // The number of hours of data displayed.
    static final int DISPLAY_HOURS = 3 * 24;
    

	// ******************************************************************** //
    // Constructors.
    // ******************************************************************** //
	
	/**
	 * Create a weather chart.
	 * 
	 * @param	context			Parent application.
	 */
	public WeatherWidget(Context context) {
		super(context);
		init(context);
	}

	
	/**
	 * Create a weather chart.
	 * 
	 * @param	context			Parent application.
	 * @param	attrs			Layout attributes.
	 */
	public WeatherWidget(Context context, AttributeSet attrs) {
		super(context, attrs);
		init(context);
	}

	
	/**
	 * Set up this widget.
	 * 
	 * @param	context			Parent application.
	 */
	private void init(Context context) {
		setMinimumWidth(MIN_WIDTH);

		charBuf = new char[20];
		graphPaint = new Paint();
		graphPaint.setAntiAlias(true);
        
        numPoints = 0;
	}

 	   
    // ******************************************************************** //
	// Geometry.
	// ******************************************************************** //
    
    /**
     * Get this widget's desired minimum width.
     * 
     * @return                  Desired minimum width.
     */
    @Override
    protected int getSuggestedMinimumWidth() {
        return DISPLAY_HOURS * HOUR_WIDTH;
    }
    
    
    /**
     * This is called during layout when the size of this view has
     * changed.  This is where we first discover our window size, so set
     * our geometry to match.
     * 
     * @param	width			Current width of this view.
     * @param	height			Current height of this view.
     * @param	oldw			Old width of this view.  0 if we were
     * 							just added.
     * @param	oldh			Old height of this view.   0 if we were
     * 							just added.
     */
	@Override
	protected void onSizeChanged(int width, int height, int oldw, int oldh) {
    	super.onSizeChanged(width, height, oldw, oldh);

    	if (width <= 0 || height <= 0)
    		return;
        
        // Get our parent window, which is likely a scroll view.
        ViewParent par = getParent();
        if (par instanceof View)
            parentScroller = (View) par;

        // Label text size, for regular and small labels.
        labelSize = (int) 20f;
        labelSmSize = (int) 16f;

        // Size of the overall bottom margin, where the time labels go.
        marginBot = (labelSize + 3) * 2;

    	dispWidth = width;
    	dispHeight = height - marginBot;
    	
    	mbHeight = (float) dispHeight / pressRange;

    	backingBitmap = Bitmap.createBitmap(width, height,
    	                                    Bitmap.Config.RGB_565);  // FIXME: config
    	backingCanvas = new Canvas(backingBitmap);
    	reDrawContent();
	}


	// ******************************************************************** //
	// Control.
	// ******************************************************************** //

	/**
	 * Update the displayed data.
	 * 
	 * @param	c		Cursor containing the weather observations to display.
	 */
	void setData(Cursor c) {
		// If there's no data, do nothing -- keep the old data.
		if (!c.moveToFirst())
			return;
		
		// Copy the data down for later use.
		numPoints = c.getCount();
		if (pointTimes == null || pointTimes.length != numPoints)
			pointTimes = new long[numPoints];
		if (pointPress == null || pointPress.length != numPoints)
			pointPress = new float[numPoints];
		
		final int ti = c.getColumnIndexOrThrow(WeatherSchema.Observations.TIME);
		final int pi = c.getColumnIndexOrThrow(WeatherSchema.Observations.PRESS);
	    pressMin = 980;
	    pressMax = 1040;
		int i = 0;
		while (!c.isAfterLast()) {
			final float p = (float) c.getDouble(pi);
			pointTimes[i] = c.getLong(ti);
			pointPress[i] = p;
			
			if (p < pressMin)
				pressMin = (int) p;
			if (p > pressMax)
				pressMax = (int) p;
			++i;
			c.moveToNext();
		}
		
		// Round the pressure limits to the pressure grid.
	    pressMin -= pressMin % PRESS_GRID;
	    pressMax = pressMax + PRESS_GRID - 1;
	    pressMax -= pressMax % PRESS_GRID;
	    pressRange = pressMax - pressMin;

		// Calculate the last time to display as the last hour for which
		// we have data.
		Calendar baseTime = Calendar.getInstance();
		baseTime.setTimeInMillis(pointTimes[numPoints - 1]);
		baseTime.add(Calendar.HOUR_OF_DAY, 1);
		baseTime.set(Calendar.MINUTE, 0);
		baseTime.set(Calendar.SECOND, 0);
		lastTime = baseTime;
		lastTimeVal = baseTime.getTimeInMillis();
		firstTimeVal = lastTimeVal - (DISPLAY_HOURS * 3600 * 1000);
		
    	reDrawContent();
	}


	// ******************************************************************** //
	// Cell Drawing.
	// ******************************************************************** //

	/**
	 * This method is called to ask the cell to draw itself.
	 * 
	 * @param	canvas		Canvas to draw into.
	 */
	@Override
	protected void onDraw(Canvas canvas) {
        // If we haven't been set up yet, do nothing.
        if (backingCanvas == null || numPoints == 0)
            return;

        // Just re-draw from our cached bitmap.
        synchronized (backingBitmap) {
            canvas.drawBitmap(backingBitmap, 0, 0, null);
        }
        
        // Draw in the pressure labels.  We do this at the current scroll
        // X position, so they stay in the same place on screen.
        graphPaint.setStyle(Paint.Style.FILL);
        graphPaint.setTextSize(labelSize);
        graphPaint.setColor(MAIN_LABEL_COL);
        float x = parentScroller != null ? parentScroller.getScrollX() : 0;
        for (int p = pressMin; p <= pressMax; p += PRESS_GRID) {
            float textOff = p == pressMin ? 0 : p == pressMax ? labelSize : labelSize / 2;
            graphPaint.setStrokeWidth(p % 100 == 0 ? 2 : 1);
        	final float y = dispHeight - (p - pressMin) * mbHeight;
            canvas.drawText("" + p, x, y + textOff - 4, graphPaint);
        }
    }
    
    
    /**
     * This method is called when some data has changed.  Re-draw the
     * widget's content to the backing bitmap.
     */
    private void reDrawContent() {
        // If we haven't been set up yet, do nothing.
        if (backingCanvas == null || numPoints == 0)
            return;
        
        synchronized (backingBitmap) {
            final Canvas canvas = backingCanvas;
            canvas.drawColor(0xff000000);

            // Draw in the pressure grid and hour labels.
            drawGrid(canvas);

            // Draw in the pressure curve.
            drawPressure(canvas);

            // Widget needs a redraw now.
            postInvalidate();
        }
    }


    /**
     * Draw in the grid and axis labels.
     */
    private void drawGrid(Canvas canvas) {
        // Draw the vertical lines and the hour labels.
        graphPaint.setStyle(Paint.Style.STROKE);
        float hourY = dispHeight + marginBot - labelSize - 8;
        float dayY = dispHeight + marginBot - 6;
        Calendar time = (Calendar) lastTime.clone();
        time.add(Calendar.HOUR_OF_DAY, -DISPLAY_HOURS);
        for (int hour = 0; hour < DISPLAY_HOURS; ++hour) {
            float x = (float) hour * (float) HOUR_WIDTH;
            int h = time.get(Calendar.HOUR_OF_DAY);

            // Verticals, with a heavy line every 4.
            graphPaint.setStrokeWidth(h % 4 == 0 ? 2 : 1);
            graphPaint.setColor(GRID_COL);
            canvas.drawLine(x, 0, x, dispHeight, graphPaint);

            // Hour labels every 4 hours.
            graphPaint.setStrokeWidth(0);
            if (h % 4 == 0) {
                int count = formatTime(h, 0);
                float dx = -count * 4;
                graphPaint.setColor(MAIN_LABEL_COL);
                graphPaint.setTextSize(labelSmSize);
                canvas.drawText(charBuf, 0, count, x + dx, hourY, graphPaint);
                
                // Day labels every noon and midnight.
                if (h % 12 == 0) {
                    int wd = time.get(Calendar.DAY_OF_WEEK);
                    graphPaint.setColor(MAIN_LABEL_COL);
                    graphPaint.setTextSize(labelSize);
                    String name = TimeModel.weekdayName(wd);
                    canvas.drawText(name, x + dx, dayY, graphPaint);
                }
            }
            
            time.add(Calendar.HOUR_OF_DAY, 1);
        }

        // Draw in the baselines.
        graphPaint.setStyle(Paint.Style.STROKE);
        graphPaint.setColor(GRID_COL);
        for (int p = pressMin; p <= pressMax; p += PRESS_GRID) {
            graphPaint.setStrokeWidth(p % 100 == 0 ? 2 : 1);
        	final float y = dispHeight - (p - pressMin) * mbHeight;
            canvas.drawLine(0, y, dispWidth, y, graphPaint);
        }

        // Draw in the standard pressure line.
        graphPaint.setStyle(Paint.Style.STROKE);
        graphPaint.setColor(GRID_HL_COL);
        graphPaint.setStrokeWidth(1);
    	final float y = dispHeight - (PRESS_STD - pressMin) * mbHeight;
        canvas.drawLine(0, y, dispWidth, y, graphPaint);
    }
    
    
    /**
     * Draw in the pressure curve.
     */
    private void drawPressure(Canvas canvas) {
        graphPaint.setStyle(Paint.Style.STROKE);
        graphPaint.setColor(CURVE_COL);
        graphPaint.setStrokeWidth(2);
        
        float px = 0;
        float py = 0;
        for (int i = 0; i < numPoints; ++i) {
        	final long t = pointTimes[i];
        	final float x = (float) (t - firstTimeVal) / 1000f / 3600f * HOUR_WIDTH;
        	
        	final float p = pointPress[i];
        	final float y = dispHeight - (p - pressMin) * mbHeight;
        	
        	if (i > 0) {
                graphPaint.setColor(CURVE_COL);
                canvas.drawLine(px, py, x, y, graphPaint);
        	}
        	graphPaint.setColor(POINT_COL);
        	canvas.drawPoint(x, y, graphPaint);
            px = x;
            py = y;
        }
    }
    

    private int formatTime(int h, int m) {
        charBuf[0] = (char) ('0' + h / 10);
        charBuf[1] = (char) ('0' + h %  10);
        charBuf[2] = ':';
        charBuf[3] = (char) ('0' + m /  10);
        charBuf[4] = (char) ('0' + m %  10);
        
        return 5;
    }
    
   
	// ******************************************************************** //
	// Class Data.
	// ******************************************************************** //

    // Debugging tag.
	@SuppressWarnings("unused")
	private static final String TAG = "onwatch";

	// Minimum display width, in pixels.
	private static final int MIN_WIDTH = 640;

	// Width of an hour on the display, in pixels.
	private static final int HOUR_WIDTH = 24;

    // Colour to draw the grid.
    private static final int GRID_COL = 0xff40a040;
    private static final int GRID_HL_COL = 0xffa0d000;
    
    // Colour to draw the object bars.
	private static final int CURVE_COL = 0xff9060d0;
	private static final int POINT_COL = 0xffc0c000;

    // Colour to draw the labels.
	private static final int MAIN_LABEL_COL = 0xffffffff;
  
    // Standard pressure.
    private static final float PRESS_STD = 1013.25f;
    
    // Spacing of the pressure grid in mb.
    private static final int PRESS_GRID = 20;


	// ******************************************************************** //
	// Private Data.
	// ******************************************************************** //

	// The scrolling window we're in.  Null if not provided by the app.
	private View parentScroller = null;

	// Size of the graph part of the display.  The time labels are
	// separately accounted for.
	private int dispWidth = 0;
	private int dispHeight = 0;

    // Label text size, for regular and small labels.
    private int labelSize = 16;
    private int labelSmSize = 12;

    // Size of the overall bottom margin, where the time labels go.
    private int marginBot = (labelSize + 5) * 2;
	
	// Height of one mb and 10 mb on the display.
	private float mbHeight;

    // Bitmap we draw the widget into, and the Canvas we draw with.
    private Bitmap backingBitmap;
    private Canvas backingCanvas;
	
	// Current observation data.
	private int numPoints;
	private long[] pointTimes;
	private float[] pointPress;
    
    // Min and max pressures we need to display, rounded to grid lines.
    private int pressMin = 980;
    private int pressMax = 1040;
    private int pressRange = pressMax - pressMin;

	// The time shown at the left and right edges of the display.
	private long firstTimeVal = 0;
	private long lastTimeVal = 0;
	private Calendar lastTime = null;
	
	// Paint used for graphics.
	private Paint graphPaint;
	
	// Char buffer used for formatting.
	private char[] charBuf;

}

