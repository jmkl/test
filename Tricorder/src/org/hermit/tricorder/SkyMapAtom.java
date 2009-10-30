
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

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.util.FloatMath;
import android.view.SurfaceHolder;


/**
 * A view which displays satellite status information as a sky diagram.
 */
class SkyMapAtom
	extends Element
{

	// ******************************************************************** //
	// Constructor.
	// ******************************************************************** //

	/**
	 * Set up this view.
	 * 
	 * @param	context			Parent application context.
     * @param	sh				SurfaceHolder we're drawing in.
	 * @param	gridCol			Colour for the graph grid.
	 * @param	plotCol			Colour for the graph plot.
	 */
	public SkyMapAtom(Tricorder context, SurfaceHolder sh,
	   				  int gridCol, int plotCol)
	{
		super(context, sh, gridCol, plotCol);
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
	protected void setGeometry(Rect bounds) {
		super.setGeometry(bounds);

		final int width = bounds.right - bounds.left;
        final int height = bounds.bottom - bounds.top;
        final int mindim = width < height ? width : height;
		
		// Figure out the centre position and radius.
		crossX = bounds.left + width / 2;
		crossY = bounds.top + height / 2;
		mapRadius = mindim / 2 - MARGIN;
		
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
	}


	// ******************************************************************** //
	// Data Management.
	// ******************************************************************** //

    /**
     * Set the azimuth of the device.
     * 
     * @param   azimuth            The new azimuth.
     */
    public void setAzimuth(float azimuth) {
        currentAzimuth = azimuth;
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
	 */
	@Override
	protected void drawBody(Canvas canvas, Paint paint) {
	    // Rotate to the azimuth to draw the grid.
	    canvas.save();
	    canvas.rotate(-currentAzimuth, crossX, crossY);
	    
		// Draw our axes.
        paint.setColor(gridColour);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(GRID_WIDTH);
        canvas.drawPath(gridPath, paint);
       
        paint.setStrokeWidth(0);
        paint.setTextSize(appContext.getTinyTextSize());
        float lw = paint.measureText("N");
        canvas.drawText("N", crossX - lw / 2, crossY - mapRadius - 2, paint);
        canvas.drawText("S", crossX - lw / 2, crossY + mapRadius + 15, paint);
        canvas.drawText("E", crossX + mapRadius + 2, crossY + 6, paint);
        canvas.drawText("W", crossX - mapRadius - 2 - lw, crossY + 6, paint);

        // Done with rotating.  We'll manually rotate the positions of
        // the satellites, but their text labels will be upright.
        canvas.restore();

		// Draw the data values if we have them.
		// REMEMBER THE SCREEN Y-AXIS is NEGATIVE UP.
		if (currentValues == null)
		    return;

		paint.setStyle(Paint.Style.FILL_AND_STROKE);
		paint.setStrokeWidth(0);
		paint.setTextSize(appContext.getTinyTextSize());

		for (int prn = 0; prn < currentValues.length; ++prn) {
            GeoView.GpsInfo ginfo = currentValues[prn];
            if (ginfo.time == 0)
                continue;

            // Convert the sat's polar co-ordinates to cartesian.
		    float azimuth = (float) Math.toRadians(ginfo.azimuth - currentAzimuth);
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
		    
		    canvas.drawText("" + prn, crossX + y + 3, crossY - x, paint);
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

	// Margin around the diagram.
	private static final int MARGIN = 16;

    // Thickness of the grid lines.
    private static final float GRID_WIDTH = 1.5f;

    // Radius of circle representing a satellite.
    private static final float SAT_RADIUS = 2f;

	
	// ******************************************************************** //
	// Private Data.
	// ******************************************************************** //

    // Path used to draw the grid.
    private Path gridPath = null;

    // The current device azimuth.  Used to rotate the display.
    private float currentAzimuth = 0f;

	// The current X, Y and Z values, and their absolute magnitude.
	private GeoView.GpsInfo[] currentValues = null;

	// X,Y position of the centre of the display.
	private int crossX;
	private int crossY;
	
	// Radius of the sky map.
	private int mapRadius;
	
}

