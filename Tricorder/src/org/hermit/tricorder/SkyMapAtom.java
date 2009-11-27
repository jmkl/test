
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

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
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
		mapRadius = mindim / 2f - MARGIN;
		
		// Create the path that draws the grid.
        gridPath = new Path();
        gridPath.moveTo(crossX - mapRadius, crossY);
        gridPath.lineTo(crossX + mapRadius, crossY);
        gridPath.moveTo(crossX, crossY - mapRadius);
        gridPath.lineTo(crossX, crossY + mapRadius);
        gridPath.addCircle(crossX, crossY, mapRadius * 1f / 3f, Path.Direction.CW);
        gridPath.addCircle(crossX, crossY, mapRadius * 2f / 3f, Path.Direction.CW);
        gridPath.addCircle(crossX, crossY, mapRadius * 3f / 3f, Path.Direction.CW);
        gridPath.moveTo(crossX, crossY - mapRadius);
        gridPath.lineTo(crossX - 5, crossY - mapRadius + 16);
        gridPath.lineTo(crossX + 5, crossY - mapRadius + 16);
        gridPath.close();
        
        // Create the path that draws the compass needle.  Since we need
        // to draw the two halves different colours, this path draws
        // one half, pointing up.
        final int nw = mindim / 30;
        final int nl = mindim / 5;
        needlePath = new Path();
        needlePath.moveTo(crossX - nw, crossY);
        needlePath.lineTo(crossX, crossY - nl);
        needlePath.lineTo(crossX + nw, crossY);
        needlePath.close();
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
//        Log.i(TAG, "T:" + trueAz + ", S:" + azimuthSmoothed);
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
	    
		// Draw our axes.
        paint.setColor(getGridColor());
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(GRID_WIDTH);
        paint.setAntiAlias(true);
        canvas.drawPath(gridPath, paint);
       
        paint.setStrokeWidth(0);
        paint.setTextSize(getTinyTextSize());
        float lw = paint.measureText("N");
        canvas.drawText("N", crossX - lw / 2, crossY - mapRadius - 2, paint);
        canvas.drawText("S", crossX - lw / 2, crossY + mapRadius + 15, paint);
        canvas.drawText("E", crossX + mapRadius + 2, crossY + 6, paint);
        canvas.drawText("W", crossX - mapRadius - 2 - lw, crossY + 6, paint);

        // Now draw in the compass needle.
        canvas.rotate(currentDeclination, crossX, crossY);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(COLOUR_NORTH);
        canvas.drawPath(needlePath, paint);
        canvas.rotate(180f, crossX, crossY);
        paint.setColor(COLOUR_SOUTH);
        canvas.drawPath(needlePath, paint);

        // Done with rotating.  We'll manually rotate the positions of
        // the satellites, but their text labels will be upright.
        canvas.restore();

		// Draw the data values if we have them.
		// REMEMBER THE SCREEN Y-AXIS is NEGATIVE UP.
		if (currentValues == null)
		    return;

		paint.setStyle(Paint.Style.FILL_AND_STROKE);
		paint.setStrokeWidth(0);
		paint.setTextSize(getTinyTextSize());
		
		for (int prn = 1; prn <= GeoView.NUM_SATS; ++prn) {
            GeoView.GpsInfo ginfo = currentValues[prn];
            if (ginfo.time == 0)
                continue;

            // Convert the sat's polar co-ordinates to cartesian.
		    float azimuth = (float) Math.toRadians(ginfo.azimuth - azimuthSmoothed);
		    if (azimuth > Math.PI)
		        azimuth  = azimuth - TWOPI;
		    final float elev = (float) Math.toRadians(ginfo.elev);
		    final float dist = (HALFPI - elev) / HALFPI * mapRadius;
		    final float x = FloatMath.cos(azimuth) * dist;
            final float y = FloatMath.sin(azimuth) * dist;

            // Plot the sat, simultaneously correcting for the azimuth
            // being north-relative and clockwise, and the inverted screen Y.
            paint.setColor(ginfo.colour);
		    canvas.drawCircle(crossX + y, crossY - x, SAT_RADIUS, paint);
		    
		    canvas.drawText("" + prn, crossX + y + 3, crossY - x + 4, paint);
		}
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
    
	// Margin around the diagram.
	private static final int MARGIN = 16;

    // Thickness of the grid lines.
    private static final float GRID_WIDTH = 1.5f;

    // Radius of circle representing a satellite.
    private static final float SAT_RADIUS = 2f;

    // Colours for the compass needle.
    private static final int COLOUR_NORTH = 0xffff0000;
    private static final int COLOUR_SOUTH = 0xffffffff;

	
	// ******************************************************************** //
	// Private Data.
	// ******************************************************************** //

    // Path used to draw the grid.
    private Path gridPath = null;

    // Path used to draw one half of the magnetic compass needle.
    private Path needlePath = null;

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
	
}

