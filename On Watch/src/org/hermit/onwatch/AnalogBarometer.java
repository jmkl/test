
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

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;


/**
 * This widget display an analog barometer with a pressure history
 * indication.
 */
public class AnalogBarometer
	extends View
{

	// ******************************************************************** //
    // Constructors.
    // ******************************************************************** //

	/**
	 * Create a barometer.
	 * 
	 * @param	context			Parent application.
	 */
    public AnalogBarometer(Context context) {
        this(context, null);
    }

    
	/**
	 * Create a barometer.
	 * 
	 * @param	context			Parent application.
	 * @param	attrs			Layout attributes.
	 */
    public AnalogBarometer(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }
    

	/**
	 * Create a barometer.
	 * 
	 * @param	context			Parent application.
	 * @param	attrs			Layout attributes.
	 * @param	defStyle		Default style.
	 */
    public AnalogBarometer(Context context, AttributeSet attrs, int defStyle) {
    	super(context, attrs, defStyle);
    	
    	// Get the dial and hand images.
    	Resources r = context.getResources();
		for (DialMode m : DialMode.VALUES)
			m.dialImage = r.getDrawable(m.resId);
    	baroHand = r.getDrawable(R.drawable.baro_hand);

    	// Figure out the base size of the dial.
    	dialWidth = DialMode.STANDARD.dialImage.getIntrinsicWidth();
    	dialHeight = DialMode.STANDARD.dialImage.getIntrinsicHeight();
    	
		graphPaint = new Paint();
		graphPaint.setAntiAlias(true);
    }

    
    // ******************************************************************** //
	// Display Setup.
	// ******************************************************************** //

    /**
     * Measure the view and its content to determine the measured width
     * and the measured height.  This method is invoked by measure(int, int)
     * and should be overriden by subclasses to provide accurate and
     * efficient measurement of their contents.
     * 
     * CONTRACT: When overriding this method, you must call
     * setMeasuredDimension(int, int) to store the measured width and
     * height of this view.  Failure to do so will trigger an
     * IllegalStateException, thrown by measure(int, int).  Calling the
     * superclass' onMeasure(int, int) is a valid use.
     * 
     * The base class implementation of measure defaults to the background
     * size, unless a larger size is allowed by the MeasureSpec.  Subclasses
     * should override onMeasure(int, int) to provide better measurements
     * of their content.
     * 
     * If this method is overridden, it is the subclass's responsibility
     * to make sure the measured height and width are at least the view's
     * minimum height and width (getSuggestedMinimumHeight() and
     * getSuggestedMinimumWidth()).
     * 
     * @param	widthMeasureSpec	Horizontal space requirements as imposed by
     * 								the parent, encoded with View.MeasureSpec.
     * @param	heightMeasureSpec	Vertical space requirements as imposed by
     * 								the parent, encoded with View.MeasureSpec.
     */
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int widthSize =  MeasureSpec.getSize(widthMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        int heightSize =  MeasureSpec.getSize(heightMeasureSpec);

        float hScale = 1.0f;
        float vScale = 1.0f;

        if (widthMode != MeasureSpec.UNSPECIFIED && widthSize < dialWidth)
            hScale = (float) widthSize / (float) dialWidth;

        if (heightMode != MeasureSpec.UNSPECIFIED && heightSize < dialHeight)
            vScale = (float )heightSize / (float) dialHeight;

        float scale = Math.min(hScale, vScale);

        int hSize = resolveSize((int) (dialWidth * scale), widthMeasureSpec);
        int vSize = resolveSize((int) (dialHeight * scale), heightMeasureSpec);
        setMeasuredDimension(hSize, vSize);
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

    	dispWidth = width;
    	dispHeight = height;

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
	 * @param	times	Times of all the observations.
	 * @param	press	Pressure values of all the observations.
	 * @param	t		Time of the latest observation.
	 * @param	p		Pressure value of the latest observation.
	 * @param	min		Lowest pressure value.
	 * @param	max		Highest pressure value.
	 * @param	msg		Current weather message; null if none.
	 */
	void setData(long[] times, float[] press, long t, float p,
				 float min, float max, String msg)
	{
		// Copy the data down for later use.
		pointTimes = times;
		pointPress = press;
		numPoints = pointTimes.length;

		pressNow = p;
		pressMin = min;
		pressMax = max;

		// Find a dial mode that accommodates the pressure range.
		DialMode dial = DialMode.forRange(pressMin, pressMax);
		if (dial != dialMode)
			dialMode = dial;
		
		reDrawContent();
	}


	// ******************************************************************** //
	// Content Drawing.
	// ******************************************************************** //
	
    /**
     * This method is called when some data has changed.  Re-draw the
     * widget's content to the backing bitmap.
     */
    private void reDrawContent() {
        // If we haven't been set up yet, do nothing.
        if (backingCanvas == null)
            return;
        
        synchronized (backingBitmap) {
            final Canvas canvas = backingCanvas;
            canvas.drawColor(0xff000000);

            // Draw the barometer into the canvas.
            drawBaro(canvas);

            // Widget needs a redraw now.
            postInvalidate();
        }
    }


	/**
	 * Draw the barometer into the given canvas.
	 * 
	 * @param	canvas		Canvas to draw into.
	 */
    protected void drawBaro(Canvas canvas) {
        Log.i(TAG, "Draw baro: " + dialMode);
        
        int cx = dispWidth / 2;
        int cy = dispHeight / 2;
        
        final Drawable dial = dialMode.dialImage;
        int dw = dial.getIntrinsicWidth();
        int dh = dial.getIntrinsicHeight();

        boolean scaled = false;

        if (dispWidth < dw || dispHeight < dh) {
            float scale = Math.min((float) dispWidth / (float) dw,
                                   (float) dispHeight / (float) dh);
            canvas.save();
            canvas.scale(scale, scale, cx, cy);
            scaled = true;
        }

        dial.setBounds(cx - (dw / 2), cy - (dh / 2), cx + (dw / 2), cy + (dh / 2));
        dial.draw(canvas);

        if (numPoints > 0) {
        	drawHand(canvas, cx, cy);
        	drawHistory(canvas, cx, cy, dw / 2);
        }

        if (scaled)
            canvas.restore();
    }
    

	/**
	 * Draw the barometer hand into the given canvas.
	 * 
	 * @param	canvas		Canvas to draw into.
	 * @param	cx			Centre x co-ordinate.
	 * @param	cy			Centre y co-ordinate.
	 */
    protected void drawHand(Canvas canvas, int cx, int cy) {
        canvas.save();
        canvas.rotate(dialMode.angle(pressNow), cx, cy);
        
        final Drawable hand = baroHand;
        int hw = hand.getIntrinsicWidth();
        int hh = hand.getIntrinsicHeight();
        hand.setBounds(cx - (hw / 2), cy - (hh / 2), cx + (hw / 2), cy + (hh / 2));
        hand.draw(canvas);
        
        canvas.restore();
    }
    

	/**
	 * Draw the barometer history curve into the given canvas.
	 * 
	 * @param	canvas		Canvas to draw into.
	 * @param	cx			Centre x co-ordinate.
	 * @param	cy			Centre y co-ordinate.
	 * @param	radius		Radius for the plot.
	 */
    protected void drawHistory(Canvas canvas, int cx, int cy, float radius) {
        long now = System.currentTimeMillis();

        // We render the points into an array, and the curve into a path,
        // for drawing later.
        Path path = new Path();
        float[] points = new float[numPoints * 2];
        
        // Number of points too old to draw.
        int skip = 0;

        // Calculate the width of the polar plot, and the width of an
        // hour on the plot.
        radius *= HISTORY_DIAL_FRAC;
        final float hourWidth = radius / HISTORY_HOURS;
        
        // Previous radius and angle.
        float prevRad = 0;
        float prevAng = 0;
        boolean havePrev = false;
        
        for (int i = 0; i < numPoints; ++i) {
        	// Get the time, pressure and age of this point.  If it's
        	// too old, skip it.
        	final long t = pointTimes[i];
        	final float p = pointPress[i];
        	final float age = (now - t) / 1000f / 3600f;
        	if (age > HISTORY_HOURS) {
        		++skip;
        		continue;
        	}

        	// Calculate the polar co-ordinates of this point.
        	final float r = radius - age * hourWidth;
        	final float a = (float) Math.toRadians(dialMode.angle(p));
        	final float span = a - prevAng;

        	// Calculate the Cartesian co-ordinates.
        	final float gx = r * (float) Math.sin(a);
        	final float gy = r * (float) -Math.cos(a);
        	
        	// Plot this point.
        	points[i * 2] = cx + gx;
        	points[i * 2 + 1] = cy + gy;

        	// Plot the segment of the curve.  If it's too long,
        	// break it into smaller segments.
        	if (!havePrev) {
        		path.moveTo(cx + gx, cy + gy);
            	havePrev = true;
        	} else if (Math.abs(span) < ARC_SEG_MAX) {
        		path.lineTo(cx + gx, cy + gy);
        	} else {
        		int stepCount = (int) Math.ceil(Math.abs(span) / ARC_SEG_MAX);
        		float stepSize = span / stepCount;
        		float rStep = (r - prevRad) / stepCount;
        		for (int s = 1; s <= stepCount; ++s) {
        			final float sa = prevAng + s * stepSize;
        			final float sr = prevRad + s * rStep;
        			final float sx = sr * (float) Math.sin(sa);
        			final float sy = sr * (float) -Math.cos(sa);
        			path.lineTo(cx + sx, cy + sy);
        		}
        	}

        	prevRad = r;
        	prevAng = a;
        }
        
        // Now actually draw the curve.
        graphPaint.setStyle(Paint.Style.STROKE);
        graphPaint.setColor(CURVE_COL);
        graphPaint.setStrokeWidth(2);
        canvas.drawPath(path, graphPaint);
       
        // And draw the points on top.
        graphPaint.setColor(POINT_COL);
        canvas.drawPoints(points, skip * 2, (numPoints - skip) * 2, graphPaint);
    }
    

	// ******************************************************************** //
	// Screen Drawing.
	// ******************************************************************** //

	/**
	 * This method is called to ask the widget to draw itself.
	 * 
	 * @param	canvas		Canvas to draw into.
	 */
	@Override
	protected void onDraw(Canvas canvas) {
        // If we haven't been set up yet, do nothing.
        if (backingCanvas == null)
            return;

        // Just re-draw from our cached bitmap.
        synchronized (backingBitmap) {
            canvas.drawBitmap(backingBitmap, 0, 0, null);
        }
	}
    
    
	// ******************************************************************** //
	// Class Data.
	// ******************************************************************** //

    // Debugging tag.
	private static final String TAG = "onwatch";
    
    // Colour to draw the pressure line.
	private static final int CURVE_COL = 0xff9060d0;
	private static final int POINT_COL = 0xffff0040;

	// Number of hours of history to display in the dial.
	private static final int HISTORY_HOURS = 24;
	
	// Fraction of the width of the dial to use for displaying history.
	private static final float HISTORY_DIAL_FRAC = 0.93f;

	// Maximum span, in radians, of an arc on the history plot that
	// we won't break into line segments.
    private static final float ARC_SEG_MAX = (float) Math.toRadians(2);

	// Normal dial:
	//            1000
	//              .  
	//       995 *     * 1005
	//     990 *         * 1010
	//    985 *           * 1015
	//     980 *         * 1020
	//       975 *     * 1025
	//
	// Extended dial:
	//            1000
	//              .  
	//       990 *     * 1010
	//     980 *         * 1020
	//    970 *           * 1030
	//     960 *         * 1040
	//       950 *  .
	//             940
	//
	// Full dial:
	//            1000
	//              .  
	//       980 *     * 1020
	//     960 *         * 1040
	//    940 *           * 1060
	//     920 *         * 1080
	//       900 *  .
	//             880

	// Enum defining the dial modes, with the min and max pressure
	// displayable, and pressure increment per "hour", on each dial.
	private enum DialMode {
		STANDARD(R.drawable.baro_dial_normal, 974, 1026, 5),
		EXTENDED(R.drawable.baro_dial_extended, 939, 1041, 10),
		FULL(R.drawable.baro_dial_full, 860, 1100, 20);
		
		DialMode(int res, float min, float max, float incr) {
			resId = res;
			minPress = min;
			maxPress = max;
			pressIncr = incr;
		}
	    
	    float angle(float press) {
	    	return (press - 1000f) / pressIncr / 12f * 360.0f;
	    }
	    
	    static DialMode forRange(float min, float max) {
			for (DialMode m : VALUES)
				if (min >= m.minPress && max <= m.maxPress)
					return m;
			return FULL;
	    }
	    
		private static final DialMode[] VALUES = values();
		
		private final int resId;
		private final float minPress;
		private final float maxPress;
		private final float pressIncr;
		
	    private Drawable dialImage;
	}
	

	// ******************************************************************** //
	// Private Data.
	// ******************************************************************** //

	// Images for the hand and the dial -- the dial has a normal version,
	// and an extended version for when the pessure is extreme.
    private Drawable baroHand;

    // Base size of the dial images.
    private int dialWidth;
    private int dialHeight;

	// Size of the display.
	private int dispWidth = 0;
	private int dispHeight = 0;
	
	// Current observation data.
	private int numPoints = 0;
	private long[] pointTimes = null;
	private float[] pointPress = null;

    // Min and max pressures in the actual data.
    private float pressMin = 0;
    private float pressMax = 0;
    
    // True if we need the extended dial.
    private DialMode dialMode = DialMode.STANDARD;
    
    // Latest pressure time and value; 0 if not known.
    private float pressNow = 0;

    // Bitmap we draw the widget into, and the Canvas we draw with.
    private Bitmap backingBitmap;
    private Canvas backingCanvas;
	
	// Paint used for graphics.
	private Paint graphPaint;

}

