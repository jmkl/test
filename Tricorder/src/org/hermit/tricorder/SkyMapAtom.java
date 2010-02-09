
/**
 * Tricorder: turn your phone into a tricorder.
 * 
 * This is an Android implementation of a Star Trek tricorder, based on
 * the phone's own sensors.  It's also a demo project for sensor access.
 *
 *   This program is free software; you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License version 2
 *   as published by the Free Software Foundation (see COPYING).
 * 
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 */


package org.hermit.tricorder;

import org.hermit.android.core.SurfaceRunner;
import org.hermit.android.instruments.Gauge;
import org.hermit.utils.CharFormatter;

import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.util.FloatMath;


/**
 * A view which displays satellite status information as a sky diagram.
 */
class SkyMapAtom
	extends Gauge
{

	// ******************************************************************** //
	// Constructor.
	// ******************************************************************** //

	/**
	 * Set up this view.
	 * 
     * @param   parent          Parent surface.
	 * @param	gridCol			Colour for the graph grid.
	 * @param	plotCol			Colour for the graph plot.
	 */
	public SkyMapAtom(SurfaceRunner parent, int gridCol, int plotCol) {
		super(parent, gridCol, plotCol);
		
	    azimuthHistory = new float[AZIMUTH_HISTORY][2];
	    azimuthAverage = new float[2];
	    azimuthIndex = 0;

	    // Set up the compass direction labels.
	    Resources res = parent.getResources();
        labelStrings = new char[8][];
        labelStrings[0] = res.getString(R.string.lab_n).toCharArray();
        labelStrings[1] = res.getString(R.string.lab_e).toCharArray();
        labelStrings[2] = res.getString(R.string.lab_s).toCharArray();
        labelStrings[3] = res.getString(R.string.lab_w).toCharArray();
        labelStrings[4] = "388".toCharArray();
        labelStrings[5] = res.getString(R.string.lab_dt).toCharArray();
        labelStrings[6] = "388".toCharArray();
        labelStrings[7] = res.getString(R.string.lab_dm).toCharArray();
	}

	   
    // ******************************************************************** //
	// Geometry Management.
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

		final int width = bounds.right - bounds.left;
        final int height = bounds.bottom - bounds.top;
        final int mindim = width < height ? width : height;
		
		// Figure out the centre position and radius.
		crossX = bounds.left + width / 2f;
		crossY = bounds.top + height / 2f;
		mapRadius = mindim / 2f;
		
        // Create the path that draws the elevation rings.
		ringsPath = new Path();
		ringsPath.addCircle(crossX, crossY, mapRadius * 1f / 3f, Path.Direction.CW);
		ringsPath.addCircle(crossX, crossY, mapRadius * 2f / 3f, Path.Direction.CW);
		ringsPath.addCircle(crossX, crossY, mapRadius * 3f / 3f, Path.Direction.CW);

		// Create the path that draws the horizontal and vertical bars.
		final float barWidth = mapRadius / 7f;
        final float barBlock = mapRadius / 7.2f;
        barsPath = new Path();
        barsPath.addRect(crossX - mapRadius, crossY - barWidth,
                         crossX + mapRadius, crossY + barWidth, Path.Direction.CW);
        barsPath.addRect(crossX - barWidth, crossY - mapRadius,
                         crossX + barWidth, crossY + mapRadius, Path.Direction.CW);
        
        // Create the path to draw the true north arrow.
        northPath = new Path();
        for (int i = 0; i < 3; ++i) {
            final float y1 = crossY - barWidth - barBlock * i;
            final float y2 = y1 - barBlock * 0.8f;
            northPath.addRect(crossX - barWidth + GRID_WIDTH / 2f, y1 - GRID_WIDTH / 2f,
                              crossX + barWidth - GRID_WIDTH / 2f, y2 + GRID_WIDTH / 2f, Path.Direction.CW);
        }
        final float ny1 = crossY - barWidth - barBlock * 3f;
        final float ny2 = crossY - barWidth - barBlock * 4.2f;
        northPath.moveTo(crossX - barWidth + GRID_WIDTH / 2f, ny1 - GRID_WIDTH / 2f);
        northPath.lineTo(crossX, ny2 + GRID_WIDTH / 2f);
        northPath.lineTo(crossX + barWidth - GRID_WIDTH / 2f, ny1 - GRID_WIDTH / 2f);
        northPath.close();

        // Create the path to draw the background for the true north bar.
        northBlock = new Path();
        northBlock.addRect(crossX - barWidth, crossY - barWidth,
                           crossX + barWidth, ny2 - barBlock * 0.2f, Path.Direction.CW);

        // Create the path to draw the background for the magnetic north bar.
        final float needWidth = mindim / 32f;
        final float needBlock = mapRadius / 8f;
        final float needLen = mindim / 15f;
        needleBlock = new Path();
        needleBlock.addRect(crossX - needWidth, crossY - mapRadius,
                            crossX + needWidth, crossY + mapRadius, Path.Direction.CW);
        
        // Create the path that draws the compass needle south part.
        magSouthPath = new Path();
        final float sy1 = crossY + mapRadius - needBlock * 0.1f;
        final float sy2 = sy1 - needBlock * 7.8f;
        magSouthPath.addRect(crossX - needWidth + GRID_WIDTH / 2f, sy1 - GRID_WIDTH / 2f,
                             crossX + needWidth - GRID_WIDTH / 2f, sy2 + GRID_WIDTH / 2f, Path.Direction.CW);
        
        // Create the path that draws the compass needle north part.
        magNorthPath = new Path();
        for (int i = 8; i < 15; ++i) {
            final float y1 = crossY + mapRadius - needBlock * i - needBlock * 0.1f;
            final float y2 = y1 - needBlock * 0.8f;
            magNorthPath.addRect(crossX - needWidth + GRID_WIDTH / 2f, y1 - GRID_WIDTH / 2f,
                               crossX + needWidth - GRID_WIDTH / 2f, y2 + GRID_WIDTH / 2f, Path.Direction.CW);
        }
        magNorthPath.moveTo(crossX - needWidth + GRID_WIDTH / 2f, crossY - mapRadius + needLen - GRID_WIDTH / 2f);
        magNorthPath.lineTo(crossX, crossY - mapRadius + GRID_WIDTH / 2f);
        magNorthPath.lineTo(crossX + needWidth - GRID_WIDTH / 2f, crossY - mapRadius + needLen - GRID_WIDTH / 2f);
        magNorthPath.close();
        
        // Set up the compass label positions.
        labelX = new float[8];
        labelY = new float[8];
        labelSize = getMiniTextSize();

        Paint paint = new Paint();
        float labelHeight = labelSize - paint.descent();
        paint.setTypeface(Typeface.DEFAULT_BOLD);
        paint.setTextSize(labelSize);
        float off = mapRadius * 1f / 8f;
        labelX[0] = crossX - mt(paint, labelStrings[0]) / 2f;
        labelY[0] = crossY - mapRadius + labelSize;
        labelX[1] = crossX + mapRadius - off - mt(paint, labelStrings[1]) / 2f;
        labelY[1] = crossY + labelHeight / 2f - paint.descent() / 2f;
        labelX[2] = crossX - mt(paint, labelStrings[2]) / 2f;
        labelY[2] = crossY + mapRadius - labelSize * 0.2f;
        labelX[3] = crossX - mapRadius + off - mt(paint, labelStrings[3]) / 2f;
        labelY[3] = crossY + labelHeight / 2f - paint.descent() / 2f;
        labelX[4] = crossX - mapRadius;
        labelY[4] = crossY - mapRadius + labelSize;
        labelX[5] = crossX - mapRadius;
        labelY[5] = crossY - mapRadius + labelSize * 2f;
        labelX[6] = crossX + mapRadius - mt(paint, labelStrings[6]);
        labelY[6] = crossY - mapRadius + labelSize;
        labelX[7] = crossX + mapRadius - mt(paint, labelStrings[7]);
        labelY[7] = crossY - mapRadius + labelSize * 2f;
	}

	
	final float mt(Paint paint, char[] text) {
	    return paint.measureText(text, 0, text.length);
	}
	

    /**
     * This is called after we have our geometry to do any required layout
     * on the given satellite status data.
     * 
     * @param   sats            The satellite data.
     */
    void formatValues(GeoView.GpsInfo[] sats) {
        float tsize = getTinyTextSize();
        Paint paint = new Paint();
        paint.setTypeface(Typeface.DEFAULT_BOLD);
        paint.setTextSize(tsize);

        for (int prn = 1; prn <= GeoView.NUM_SATS; ++prn) {
            GeoView.GpsInfo ginfo = sats[prn];
            
            final float m = GRID_WIDTH / 2f;
            final float wd = paint.measureText(ginfo.name) + 3;
            final float td = paint.descent();
            final float ht = (tsize - td) / 2f;

            ginfo.rect = new RectF(-SAT_RADIUS - m, -ht - m, wd + m, ht + m);
            ginfo.textX = 3;
            ginfo.textY = ht - td / 2f;
        }
    }


	// ******************************************************************** //
	// Data Management.
	// ******************************************************************** //

    /**
     * Set the azimuth of the device.
     * 
     * @param   trueAz          The new true (not magnetic) azimuth in degrees.
     * @param   dec             The new magnetic declination in degrees.
     */
    public void setAzimuth(float trueAz, float dec) {
        // Calculate the rolling average azimuth.  To do this, we need
        // to convert to vector format, average, and convert back.
        if (++azimuthIndex >= AZIMUTH_HISTORY)
            azimuthIndex = 0;
        
        float r = (float) Math.toRadians(trueAz);
        for (int i = 0; i < 2; ++i) {
            float prev = azimuthHistory[azimuthIndex][i];
            float curr = i == 0 ? (float) Math.sin(r) : (float) Math.cos(r);
            azimuthHistory[azimuthIndex][i] = curr;
            azimuthAverage[i] -= prev / AZIMUTH_HISTORY;
            azimuthAverage[i] += curr / AZIMUTH_HISTORY;
        }
        azimuthSmoothed = (float) Math.toDegrees(Math.atan2(azimuthAverage[0],
                                                            azimuthAverage[1]));
        if (azimuthSmoothed < 0)
            azimuthSmoothed += 360f;
        
        // Save the declination.  It changes very slowly, so this is fine.
        currentDeclination = dec;
    }


    /**
     * Set the given satellite status data.
     * 
     * @param   sats            The new satellite data.
     */
    public void setValues(GeoView.GpsInfo[] sats) {
        currentValues = sats;
	}


	/**
     * Clear the satellite status data; i.e. go back to a "no data" state.
	 */
	public void clearValues() {
	    currentValues = null;
	}


	// ******************************************************************** //
	// View Drawing.
	// ******************************************************************** //
	
	/**
	 * Do the subclass-specific parts of drawing for this element.
	 * 
	 * Subclasses should override this to do their drawing.
	 * 
	 * @param	canvas		Canvas to draw into.
	 * @param	paint		The Paint which was set up in initializePaint().
     * @param   now         Nominal system time in ms. of this update.
	 */
	@Override
	protected void drawBody(Canvas canvas, Paint paint, long now) {
	    // Rotate to the azimuth to draw the grid.
	    canvas.save();
	    canvas.rotate(-azimuthSmoothed, crossX, crossY);
	    
        paint.setAntiAlias(true);
	    
	    // Draw colour circles for elevations.
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(COLOUR_RING_3);
	    canvas.drawCircle(crossX, crossY, mapRadius * 3f / 3f, paint);
        paint.setColor(COLOUR_RING_2);
        canvas.drawCircle(crossX, crossY, mapRadius * 2f / 3f, paint);
        paint.setColor(COLOUR_RING_1);
        canvas.drawCircle(crossX, crossY, mapRadius * 1f / 3f, paint);

		// Draw our elevation rings over the circles, and the grid bars.
        paint.setColor(getBackgroundColor());
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(GRID_WIDTH);
        canvas.drawPath(ringsPath, paint);
        canvas.drawPath(barsPath, paint);

        // Now black out the north arrow background and draw it in.
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(getBackgroundColor());
        canvas.drawPath(northBlock, paint);
        paint.setColor(COLOUR_TRUE_NORTH);
        canvas.drawPath(northPath, paint);

        // Draw in the direction labels.
        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(getBackgroundColor());
        paint.setStrokeWidth(0);
        paint.setTypeface(Typeface.DEFAULT_BOLD);
        paint.setTextSize(labelSize);
        for (int i = 0; i < 4; ++i)
            canvas.drawText(labelStrings[i], 0, labelStrings[i].length,
                            labelX[i], labelY[i], paint);

        // Rotate to magnetic north.
        canvas.rotate(currentDeclination, crossX, crossY);
        
        // Now draw in the compass needle bar.
        paint.setColor(getBackgroundColor());
        paint.setStyle(Paint.Style.FILL_AND_STROKE);
        paint.setStrokeWidth(GRID_WIDTH);
        canvas.drawPath(needleBlock, paint);

        // Now draw in the compass needle.
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(COLOUR_MAG_SOUTH);
        canvas.drawPath(magSouthPath, paint);
        paint.setColor(COLOUR_MAG_NORTH);
        canvas.drawPath(magNorthPath, paint);
        
        // Done with rotating.  We'll manually rotate the positions of
        // the satellites, so that their text labels will be upright.
        canvas.restore();
        
        // Draw the heading.
        int trueAz = Math.round(azimuthSmoothed) % 360;
        CharFormatter.formatInt(labelStrings[4], 0, trueAz, 3, false);
        int magAz = Math.round(azimuthSmoothed - currentDeclination + 360) % 360;
        CharFormatter.formatInt(labelStrings[6], 0, magAz, 3, false);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(0);
        paint.setTypeface(Typeface.DEFAULT_BOLD);
        paint.setTextSize(labelSize);
        paint.setColor(COLOUR_TRUE_NORTH);
        for (int i = 4; i < 6; ++i)
            canvas.drawText(labelStrings[i], 0, labelStrings[i].length,
                            labelX[i], labelY[i], paint);
        paint.setColor(COLOUR_MAG_NORTH);
        for (int i = 6; i < 8; ++i)
            canvas.drawText(labelStrings[i], 0, labelStrings[i].length,
                            labelX[i], labelY[i], paint);

		// Draw the data values if we have them.
		// REMEMBER THE SCREEN Y-AXIS is NEGATIVE UP.
		if (currentValues == null)
		    return;

		paint.setStyle(Paint.Style.FILL_AND_STROKE);
		paint.setStrokeWidth(0);
		float tsize = getTinyTextSize();
        paint.setTypeface(Typeface.DEFAULT_BOLD);
		paint.setTextSize(tsize);
		
		for (int prn = 1; prn <= GeoView.NUM_SATS; ++prn) {
            GeoView.GpsInfo ginfo = currentValues[prn];
            if (ginfo.time == 0)
                continue;

            // Convert the sat's polar co-ordinates to cartesian.
		    float azimuth = (float) Math.toRadians(ginfo.azimuth - azimuthSmoothed);
		    if (azimuth > Math.PI)
		        azimuth = azimuth - TWOPI;
		    final float elev = (float) Math.toRadians(ginfo.elev);
		    final float dist = (HALFPI - elev) / HALFPI * mapRadius;
		    final float x = FloatMath.cos(azimuth) * dist;
            final float y = FloatMath.sin(azimuth) * dist;

            // Plot the sat, simultaneously correcting for the azimuth
            // being north-relative and clockwise, and the inverted screen Y.
            plotSat(canvas, paint, crossX + y, crossY - x, ginfo);
		}
	}
	
	
	private void plotSat(Canvas canvas, Paint paint, float x, float y, GeoView.GpsInfo ginfo) {
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(getBackgroundColor());
        paint.setAlpha(90);
        RectF r = ginfo.rect;
        canvas.drawRect(x + r.left, y + r.top, x + r.right, y + r.bottom, paint);
        
        paint.setStyle(Paint.Style.FILL_AND_STROKE);
        paint.setColor(ginfo.colour);
        canvas.drawCircle(x, y, SAT_RADIUS, paint);
        canvas.drawText(ginfo.name, x + ginfo.textX, y + ginfo.textY, paint);
	}

	
    // ******************************************************************** //
    // Class Data.
    // ******************************************************************** //

    // Debugging tag.
	@SuppressWarnings("unused")
	private static final String TAG = "tricorder";

    // Half of pi; 90 degrees in radians.  Two pi, 360 degrees.
    private static final float HALFPI = (float) (Math.PI / 2f);
    private static final float TWOPI = (float) (Math.PI * 2f);

    // The number of samples of the azimuth we average to get the
    // rolling average value.  This determines the damping time for
    // the compass display.
    private static final int AZIMUTH_HISTORY = 20;
    
    // Thickness of the grid lines.
    private static final float GRID_WIDTH = 4f;

    // Radius of circle representing a satellite.
    private static final float SAT_RADIUS = 2f;
    
    // Colours for the main display.
    private static final int COLOUR_RING_1 = 0xffc4d489;
    private static final int COLOUR_RING_2 = 0xffb1deb7;
    private static final int COLOUR_RING_3 = 0xffc4d489;
    private static final int COLOUR_TRUE_NORTH = 0xff81e0f0;

    // Colours for the compass needle.
    private static final int COLOUR_MAG_NORTH = 0xfffc8958;
    private static final int COLOUR_MAG_SOUTH = 0xffb8bfc7;

	
	// ******************************************************************** //
	// Private Data.
	// ******************************************************************** //

    // Path used to draw the elevation rings.
    private Path ringsPath = null;

    // Path used to draw the horizontal and vertical bars.
    private Path barsPath = null;

    // Path used to draw the true north bar.
    private Path northBlock = null;
    private Path northPath = null;

    // Path used to draw the magnetic compass needle.  This gets rotated
    // according to declination before drawing.
    private Path needleBlock = null;
    private Path magSouthPath = null;
    private Path magNorthPath = null;

    // The rolling average true (not magnetic) device azimuth in degrees,
    // and a history buffer used to compute this average.  In order to
    // average an azimuth, we need to convert it to vector form; hence
    // the history holds the sines and cosines of the instantaneous
    // azimuth values.
    private float[][] azimuthHistory = null;
    private int azimuthIndex = 0;
    private float[] azimuthAverage = null;
    private float azimuthSmoothed = 0f;

    // The current magnetic declination in degrees.
    private float currentDeclination = 0f;
    
    // Current satellite info.  Indexed by the satellite's PRN number,
    // which is in the range 1-NUM_SATS.
	private GeoView.GpsInfo[] currentValues = null;

	// X,Y position of the centre of the display.
	private float crossX;
	private float crossY;
	
	// Radius of the sky map.
	private float mapRadius;
	
	// Text labels to draw on the display.
	private char[][] labelStrings = null;
	private float[] labelX = null;
	private float[] labelY = null;
	private float labelSize = 0f;
	
}

