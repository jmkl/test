
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

import org.hermit.astro.AstroError;
import org.hermit.astro.Body;
import org.hermit.astro.Observation;
import org.hermit.geo.Position;
import org.hermit.onwatch.LocationModel.GpsState;
import org.hermit.onwatch.TimeModel.Field;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;


/**
 * This custom widget displays an astronomical calendar.  This shows the
 * altitudes of the planets throughout the night.
 *
 * @author	Ian Cameron Smith
 */
public class AstroCalendarWidget
	extends View
{

	// ******************************************************************** //
    // Constructors.
    // ******************************************************************** //
	
	/**
	 * Create a watch clock.
	 * 
	 * @param	context			Parent application.
	 */
	public AstroCalendarWidget(Context context) {
		super(context);
		init(context);
	}

	
	/**
	 * Create a watch clock.
	 * 
	 * @param	context			Parent application.
	 * @param	attrs			Layout attributes.
	 */
	public AstroCalendarWidget(Context context, AttributeSet attrs) {
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

        // Get the time model.  Get a callback every 10 minutes to update
        // the display.
        timeModel = TimeModel.getInstance(context);
        timeModel.listen(TimeModel.Field.HOUR, new TimeModel.Listener() {
            @Override
            public void change(Field field, int value, long time) {
                Log.v(TAG, "Astro: time change");
                update();
            }
        });

        // Get the location model.  Be notified when we get a location.
        locationModel = LocationModel.getInstance(context);
        locationModel.listen(new LocationModel.Listener() {
            /**
             * Invoked when a position is acquired after not having one,
             * either because we're starting or because the GPS has been off.
             * 
             * @param  state           GPS state.
             * @param  stateMsg        GPS state message.
             * @param  pos             Current position.
             * @param  locMsg          Message describing the position.
             */
            @Override
            public void newPos(GpsState state, String stateMsg,
                               Position pos, String locMsg) {
                Log.v(TAG, "Astro: newPos");
                update();
            }
        });
        
        // Create an astro observation for later use.
        astroObservation = new Observation();
        
        // Get our initial data set up.
        update();
	}

 	   
    // ******************************************************************** //
	// Geometry.
	// ******************************************************************** //
	
    /**
     * Called to pass us the handle of our parent scroller.
     * 
     * @param   astroScroller           Parent scroll widget.
     */
    void setParentScroller(View astroScroller) {
        parentScroller = astroScroller;
    }

    
    /**
     * Get this widget's desired minimum width.
     * 
     * @return                  Desired minimum width.
     */
    @Override
    protected int getSuggestedMinimumWidth() {
        return (CALC_HOURS - 2) * 16;
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

        // Label text size, for regular and small labels.
        labelSize = (int) (height / 32f);
        labelSmSize = (int) (height / 44f);

        // Size of the margin below the planet bars, where we show rise
        // and set times.
        barMargin = labelSize + 3;

        // Size of the overall bottom margin, where the time labels go.
        marginBot = (labelSize + 3) * 2;

    	dispWidth = width;
    	dispHeight = height - marginBot;
    	hourWidth = 16;
    	bodyHeight = (float) dispHeight / (float) NUM_DISP_BODIES;
    	bodyBarHeight = bodyHeight - barMargin;
    	
    	backingBitmap = Bitmap.createBitmap(width, height,
    	                                    Bitmap.Config.RGB_565);
    	backingCanvas = new Canvas(backingBitmap);
    	reDrawContent();
	}


	// ******************************************************************** //
	// Control.
	// ******************************************************************** //

	/**
	 * Update the displayed data.
	 */
	void update() {
	    long now = System.currentTimeMillis();
	    
	    synchronized (this) {
	        // If we're already doing it, just continue.
	        if (calcThread != null)
	            return;

	        // Need a position to continue.
	        Position pos = locationModel.getCurrentPos();
	        if (pos == null) {
	            Log.v(TAG, "Astro: update: no position");
	            return;
	        }

	        Calendar baseTime = Calendar.getInstance();
	        baseTime.setTimeInMillis(now);
	        int baseHour = baseTime.get(Calendar.HOUR_OF_DAY);
	        if (baseHour % 4 <= 2)
	            baseHour -= 4;
	        baseHour -= baseHour % 4;
	        baseTime.set(Calendar.HOUR_OF_DAY, baseHour);
	        baseTime.set(Calendar.MINUTE, 0);
	        baseTime.set(Calendar.SECOND, 0);
	        if (altitudeTable != null && leftTime != null && leftTime.equals(baseTime))
	            return;
	        leftTime = baseTime;
	        basePosition = pos;

	        calcThread = new Thread(calcRunner);
	        calcThread.start();
	    }
	}

	
	private Runnable calcRunner = new Runnable() {
        @Override
        public void run() {
            // Create the data tables.  Note that since we index them
            // by ordinal, they need to be big enough for all bodies, not
            // just the ones we display.
            if (altitudeTable == null || azimuthTable == null || magnitudeTable == null) {
                altitudeTable = new float[Body.NUM_BODIES][CALC_HOURS];
                azimuthTable = new float[Body.NUM_BODIES][CALC_HOURS];
                magnitudeTable = new float[Body.NUM_BODIES][CALC_HOURS];
            }

            // Get the tables of altitudes for all the bodies for
            // the next HOURS hours.
            long baseMillis = leftTime.getTimeInMillis();
            astroObservation.setObserverPosition(basePosition);
            for (int hour = 0; hour < CALC_HOURS; ++hour) {
                long time = baseMillis + 3600000 * hour;
                astroObservation.setJavaTime(time);
                
                synchronized (altitudeTable) {
                    for (Body.Name n : CALC_BODIES) {
                        Body b = astroObservation.getBody(n);
                        try {
                            float alt = (float) b.get(Body.Field.LOCAL_ALTITUDE);
                            altitudeTable[n.ordinal()][hour] = alt;
                            float az = (float) b.get(Body.Field.LOCAL_AZIMUTH);
                            azimuthTable[n.ordinal()][hour] = az;
                            float mag = (float) b.get(Body.Field.MAGNITUDE);
                            magnitudeTable[n.ordinal()][hour] = mag;
                        } catch (AstroError e) {
                            Log.e(TAG, "Astro: Get data for " + n + ": " + e.getMessage());
                        }
                    }
                }

                try {
                    Thread.sleep(25);
                } catch (InterruptedException e) { }
            }
            
            reDrawContent();
        }
	};
	

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
        if (backingCanvas == null || altitudeTable == null)
            return;

        // Just re-draw from our cached bitmap.
        synchronized (backingBitmap) {
            canvas.drawBitmap(backingBitmap, 0, 0, null);
        }
        
        // Draw in the body labels.  We do this at the current scroll
        // X position, so they stay in the same place on screen.
        graphPaint.setStyle(Paint.Style.STROKE);
        graphPaint.setTextSize(labelSize);
        graphPaint.setColor(MAIN_LABEL_COL);
        float textOff = labelSize + 6;
        float x = parentScroller.getScrollX();
        float bodyY = 0;
        for (Body.Name n : DISP_BODIES) {
            canvas.drawText(n.name, x, bodyY + textOff, graphPaint);
            bodyY += bodyHeight;
        }
    }
    
    
    /**
     * This method is called when some data has changed.  Re-draw the
     * widget's content to the backing bitmap.
     */
    private void reDrawContent() {
        // If we haven't been set up yet, do nothing.
        if (backingCanvas == null || altitudeTable == null)
            return;
        
        synchronized (backingBitmap) {
            final Canvas canvas = backingCanvas;
            canvas.drawColor(0xff000000);

            // Draw in the daylight hours.
            drawDaylight(canvas);

            // Draw in the hours grid and hour labels.
            drawGrid(canvas);

            // Draw in the altitudes of all the bodies.
            drawAltitudes(canvas);

            // Draw in the data labels for all the bodies.
            drawDataPoints(canvas);

            // Widget needs a redraw now.
            postInvalidate();
        }
    }

    
    /**
     * Draw in the daylight hours.
     */
    private void drawDaylight(Canvas canvas) {
        final int ordinal = Body.Name.SUN.ordinal();
        
        graphPaint.setStyle(Paint.Style.FILL);
        graphPaint.setColor(DAY_COL);

        float prevAlt = altitudeTable[ordinal][0];

        float riseX = -1 * (float) hourWidth;
        for (int hour = 0; hour < CALC_HOURS; ++hour) {
            float alt = altitudeTable[ordinal][hour];
            float baseX = (float) (hour - 1) * (float) hourWidth;

            // Rise / set -- do a naïve interpolation.
            if (prevAlt < 0f && alt >= 0f || prevAlt >= 0f && alt < 0f) {
                float frac = -prevAlt / (alt - prevAlt) - 1f;
                float dx = frac * (float) hourWidth;
                
                if (alt >= 0f)
                    riseX = baseX + dx;
                else {
                    float setX = baseX + dx;
                    float bodyY = 0;
                    for (int i = 0; i < NUM_DISP_BODIES; ++i) {
                        canvas.drawRect(riseX, bodyY,
                                        setX, bodyY + bodyBarHeight,
                                        graphPaint);
                        bodyY += bodyHeight;
                    }
                }
            }

            prevAlt = alt;
        }

        // If the sun is still up, draw the last day.
        if (prevAlt >= 0f) {
            float bodyY = 0;
            for (int i = 0; i < NUM_DISP_BODIES; ++i) {
                canvas.drawRect(riseX, bodyY,
                                dispWidth + hourWidth, bodyY + bodyBarHeight,
                                graphPaint);
                bodyY += bodyHeight;
            }
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
        Calendar time = (Calendar) leftTime.clone();
        for (int hour = 0; hour < CALC_HOURS; ++hour) {
            float x = (float) (hour - 1) * (float) hourWidth;
            int h = time.get(Calendar.HOUR_OF_DAY);

            // Verticals, with a heavy line every 4.
            drawVertical(canvas, GRID_COL, x, h % 4 == 0 ? 2 : 1);
            
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
        graphPaint.setStrokeWidth(2);
        float bodyY = 0;
        for (int i = 0; i < NUM_DISP_BODIES; ++i) {
            canvas.drawLine(0, bodyY + bodyHeight, dispWidth, bodyY + bodyHeight, graphPaint);
            bodyY += bodyHeight;
        }

        // Draw in the altitude 30° lines.
        graphPaint.setStrokeWidth(1);
        bodyY = 0;
        for (int i = 0; i < NUM_DISP_BODIES; ++i) {
            float y = bodyY + bodyBarHeight * 1f / 3f;
            canvas.drawLine(0, y, dispWidth, y, graphPaint);
            y = bodyY + bodyBarHeight * 2f / 3f;
            canvas.drawLine(0, y, dispWidth, y, graphPaint);
            y = bodyY + bodyBarHeight;
            canvas.drawLine(0, y, dispWidth, y, graphPaint);
            bodyY += bodyHeight;
        }
    }
    
    
    /**
     * Draw in a vertical grid line.
     */
    private void drawVertical(Canvas canvas, int col, float x, int width) {
        graphPaint.setStrokeWidth(width);
        graphPaint.setColor(col);

        float bodyY = 0;
        for (int i = 0; i < NUM_DISP_BODIES; ++i) {
            canvas.drawLine(x, bodyY, x, bodyY + bodyBarHeight, graphPaint);
            bodyY += bodyHeight;
        }
    }
    
    
    /**
     * Draw in the altitude curves.
     */
    private void drawAltitudes(Canvas canvas) {
        graphPaint.setStyle(Paint.Style.STROKE);
        graphPaint.setColor(ALTITUDE_COL);
        graphPaint.setStrokeWidth(2);
        float bodyY = 0;
        for (Body.Name n : DISP_BODIES) {
            canvas.save();
            canvas.clipRect(0, bodyY, dispWidth, bodyY + bodyBarHeight);

            float px = -hourWidth;
            float py = bodyY + bodyHeight;
            for (int hour = 0; hour < CALC_HOURS; ++hour) {
                // Altitude, 0-1.
                float alt = altitudeTable[n.ordinal()][hour] / (float) Math.PI * 2f;

                // Hour 0 is off-screen to the left.
                float x = (float) (hour - 1) * (float) hourWidth;
                float y = bodyY + bodyBarHeight - alt * bodyBarHeight;
                canvas.drawLine(px, py, x, y, graphPaint);
                px = x;
                py = y;
            }
            
            canvas.restore();

            bodyY += bodyHeight;
        }
    }
    
    
    /**
     * Draw in the data point labels.
     */
    private void drawDataPoints(Canvas canvas) {
        graphPaint.setStyle(Paint.Style.STROKE);
        graphPaint.setColor(DATA_LABEL_COL);
        graphPaint.setStrokeWidth(0);
        graphPaint.setTextSize(labelSmSize);
        float bodyY = 0f;
        for (Body.Name n : DISP_BODIES) {
            float prevAlt = altitudeTable[n.ordinal()][0];
            float prevAz = azimuthTable[n.ordinal()][0];
            float labY1 = bodyY + bodyHeight - 6;
            float labY2 = bodyY + bodyBarHeight - 6;
            
            Calendar time = (Calendar) leftTime.clone();
            for (int hour = 0; hour < CALC_HOURS; ++hour) {
                int h = time.get(Calendar.HOUR_OF_DAY);
                float alt = altitudeTable[n.ordinal()][hour];
                float az = azimuthTable[n.ordinal()][hour];
                float labX = (float) (hour - 1) * (float) hourWidth;
        
                // Rise / set -- do a naïve interpolation.
                if (prevAlt < 0f && alt >= 0f || prevAlt >= 0f && alt < 0f) {
                    float frac = -prevAlt / (alt - prevAlt);
                    int count = formatTime((h + 24 - 1) % 24, (int) (60f * frac));
                    float dx = (frac - 1f) * (float) hourWidth - count * 3;
                    canvas.drawText(charBuf, 0, count, labX + dx, labY1, graphPaint);
                }
                
                // Check for a local maximum altitude.  Draw in the magnitude
                // at that point.
                if (hour < CALC_HOURS - 1) {
                    float nextAlt = altitudeTable[n.ordinal()][hour + 1];
                    if (alt > prevAlt && alt > nextAlt) {
                        int count = formatMag(magnitudeTable[n.ordinal()][hour]);
                        float dx = -count * 3;
                        canvas.drawText(charBuf, 0, count, labX + dx, labY1, graphPaint);
                    }
                }

                // If we're above the horizon, or if we're not north of East
                // or West, see if we passed a main azimuth.  This doesn't do
                // north, but who cares.
                for (int aName = 0; aName < 8; ++aName) {
                    float aAz = (float) Math.toRadians(aName * 45f);
                    if (prevAlt > 0f || alt > 0f || (aName >= 2 && aName <= 6)) {
                        if (prevAz < aAz && az >= aAz) {
                            float frac = (aAz - prevAz) / (az - prevAz) - 1f;
                            String lab = AZIMUTH_NAMES[aName];
                            float tw = graphPaint.measureText(lab);
                            float dx = frac * (float) hourWidth - tw / 2;
                            canvas.drawText(lab, labX + dx, labY2, graphPaint);
                        }
                    }
                }

                prevAlt = alt;
                prevAz = az;
                time.add(Calendar.HOUR_OF_DAY, 1);
            }

            bodyY += bodyHeight;
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
    

    private int formatMag(float mag) {
        int count = 0;
        
        if (mag < 0f) {
            charBuf[count++] = '-';
            mag = -mag;
        }
        if (mag > 10)
            charBuf[count++] = (char) ('0' + (int) mag / 10);
        charBuf[count++] = (char) ('0' + (int) mag % 10);
        charBuf[count++] = '.';
        charBuf[count++] = (char) ('0' + (int) (mag * 10) % 10);
        
        return count;
    }
    
   
	// ******************************************************************** //
	// Class Data.
	// ******************************************************************** //

    // Debugging tag.
	@SuppressWarnings("unused")
	private static final String TAG = "onwatch";

    // The names of all the celestial bodies we calculate for.  This includes
	// the Sun, which is displayed specially.
    private static final Body.Name[] CALC_BODIES = {
        Body.Name.SUN,
        Body.Name.MOON,
        Body.Name.MERCURY,
        Body.Name.VENUS,
        Body.Name.MARS,
        Body.Name.JUPITER,
        Body.Name.SATURN,
    };

    // The names of all the celestial bodies we display.
	private static final Body.Name[] DISP_BODIES = {
        Body.Name.SUN,
	    Body.Name.MOON,
	    Body.Name.MERCURY,
	    Body.Name.VENUS,
	    Body.Name.MARS,
	    Body.Name.JUPITER,
	    Body.Name.SATURN,
    };

    // The number of celestial bodies we display.
    private static final int NUM_DISP_BODIES = DISP_BODIES.length;

	// Minimum display width, in pixels.
	private static final int MIN_WIDTH = 640;
	
    // The number of hours to calculate and store.  We don't display the
	// first and last hours; their data is only used for the off-screen
	// endpoints of altitude curves, interpolating rise/set, etc.
    private static final int CALC_HOURS = 100;

    // Colour to draw the grid.
    private static final int GRID_COL = 0xff407040;

    // Colour to draw the daylight hours.
	private static final int DAY_COL = 0x50ffff00;

    // Colour to draw the object bars.
	private static final int ALTITUDE_COL = 0xff9060d0;

    // Colour to draw the labels.
	private static final int MAIN_LABEL_COL = 0xffffffff;
    private static final int DATA_LABEL_COL = 0xffe00080;

    // Names of the compass directions.  Order is from North clockwise in
    // 45° increments.
    private final static String[] AZIMUTH_NAMES = {
        "N", "NE", "E", "SE", "S", "SW", "W", "NW"
//        "N", "E", "S", "W",
    };


	// ******************************************************************** //
	// Private Data.
	// ******************************************************************** //

	// The scrolling window we're in.
	private View parentScroller;
	
    // The time and location models.
    private TimeModel timeModel;
    private LocationModel locationModel;

	// Size of the display.
	private int dispWidth = 0;
	private int dispHeight = 0;

    // Label text size, for regular and small labels.
    private int labelSize = 16;
    private int labelSmSize = 12;

    // Size of the margin below the planet bars, where we show rise
    // and set times.
    private int barMargin = labelSize + 5;

    // Size of the overall bottom margin, where the time labels go.
    private int marginBot = (labelSize + 5) * 2;

    // Width of one hour on the display, 1/24 of the total width.
	private float hourWidth;
	
	// Height of one body's display on the display.
	private float bodyHeight;
    
    // Height of one body's altitude bar on the display.
    private float bodyBarHeight;

    // Bitmap we draw the widget into, and the Canvas we draw with.
    private Bitmap backingBitmap;
    private Canvas backingCanvas;

    // Tables of altitudes, azimuths and magnitudes of all bodies by hour.  We
    // add one hour at the beginning and end so we know where the lines go.
    private float[][] altitudeTable = null;
    private float[][] azimuthTable = null;
    private float[][] magnitudeTable = null;

	// The time shown at the left edge of the display, in ms and
	// hour of day; also the weekday.
	private Calendar leftTime = null;
	
	// Base position the calculations are based on.
	private Position basePosition = null;
	
	// Observation for astronomical calculations.
	private Observation astroObservation = null;

	// Paint used for graphics.
	private Paint graphPaint;
	
	// Char buffer used for formatting.
	private char[] charBuf;
	
	// Thread which is calculating the astro data; null if not running.
	private Thread calcThread = null;

}

