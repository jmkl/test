
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
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;


/**
 * A view which displays a scalar value as dial.
 * 
 * This could be used, for example, to show an elevation angle.
 */
class DialAtom
	extends Gauge
{

	// ******************************************************************** //
	// Public Types.
	// ******************************************************************** //

	/**
	 * Which way the gauge is drawn relative to the body it's attached
	 * to.  Hence LEFT means a vertical gauge with the pointer pointing
	 * to the right.
	 */
	enum Orientation {
		LEFT,
		TOP,
		RIGHT,
		BOTTOM;
	}
	
	
	// ******************************************************************** //
	// Constructor.
	// ******************************************************************** //

	/**
	 * Set up this view.
	 * 
     * @param   parent          Parent surface.
	 * @param	gridCol			Colour for the graph grid.
	 * @param	plotCol			Colour for the graph plot.
	 * @param	orient		The orientation for this gauge.
	 */
	public DialAtom(SurfaceRunner parent, int gridCol, int plotCol,
					Orientation orient)
	{
		super(parent, gridCol, plotCol);
		
		orientation = orient;
		barThickness = getSidebarWidth();
	}


	/**
	 * Set up the paint for this element.  This is called during
	 * initialization.  Subclasses can override this to do class-specific
	 * one-time initialization.
	 * 
	 * @param paint			The paint to initialize.
	 */
	@Override
	protected void initializePaint(Paint paint) {
		paint.setAntiAlias(true);
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
		
		int w = bounds.right - bounds.left;
		int h = bounds.bottom - bounds.top;
		int radius = 0;

		switch (orientation) {
		case LEFT:
			radius = h / 2;
			barRect = new Rect(bounds.left, bounds.top,
					   		   bounds.left + barThickness, bounds.bottom);
			centreX = bounds.left + barThickness * 3 / 2;
			centreY = bounds.top + h / 2;
			midX = centreX + radius;
			midY = centreY;
			arcCentre = 0;
			break;
		case TOP:
			radius = w / 2;
			barRect = new Rect(bounds.left, bounds.top,
			   		   		   bounds.right, bounds.bottom + barThickness);
			centreX = bounds.left + w / 2;
			centreY = bounds.top + barThickness * 3 / 2;
			midX = centreX;
			midY = centreY + radius;
			arcCentre = 90;
			break;
		case RIGHT:
			radius = h / 2;
			barRect = new Rect(bounds.right - barThickness, bounds.top,
			   		   		   bounds.right, bounds.bottom);
			centreX = bounds.right - barThickness * 3 / 2;
			centreY = bounds.top + h / 2;
			midX = centreX - radius;
			midY = centreY;
			arcCentre = 180;
			break;
		case BOTTOM:
			radius = w / 2;
			barRect = new Rect(bounds.left, bounds.bottom - barThickness,
			   		   		   bounds.right, bounds.bottom);
			centreX = bounds.left + w / 2;
			centreY = bounds.bottom - barThickness * 3 / 2;
			midX = centreX;
			midY = centreY - radius;
			arcCentre = 270;
			break;
		}
		
		int p = barThickness * 2;
		int s = barThickness * 2 / 3;
		
		int arcrad = radius - p;
		arcRect = new RectF(centreX - arcrad, centreY - arcrad,
							centreX + arcrad, centreY + arcrad);

		// Create a path that defines the pointer.
		pointerPath = new Path();
		pointerPath.moveTo(radius, 0);
		pointerPath.lineTo(radius - p, -s);
		pointerPath.lineTo(radius - p, s);
		pointerPath.close();
		
		rotateMatrix = new Matrix();
	}


	/**
	 * Get the width of the dial at a specified height (assuming vertical).
	 * 
	 * @param	h		Requested height.
	 * @return			The minimum width needed for a vertical dial of that
	 * 					height.
	 */
	int getWidthForHeight(int h) {
		return h / 2 + (int) Math.ceil(barThickness * 2);
	}
	

	// ******************************************************************** //
	// Data Management.
	// ******************************************************************** //

	/**
	 * Set the given value as the new value for the displayed data.
	 * Update the display accordingly.
	 * 
	 * @param	value				The new value.  Should be an angle
	 * 								in degrees.
	 */
	public void setValue(float value) {
		currentValue = value;
		haveValue = true;
	}


	/**
	 * Clear the current value; i.e. go back to a "no data" state.
	 */
	public void clearValue() {
		haveValue = false;
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
		// Get the value and scale it.  We're synced at the View level.
		final float sval = currentValue;
		final boolean valid = haveValue;

		// Background color.
		int bgColour = getBackgroundColor();

		// Draw the base bar and arc.
		paint.setColor(getGridColor());
		paint.setStyle(Paint.Style.FILL);
		canvas.drawRect(barRect, paint);
		paint.setStyle(Paint.Style.STROKE);
		paint.setStrokeWidth(barThickness);
		canvas.drawArc(arcRect, arcCentre - 90, 180, false, paint);
		
		// Draw a mid-point datum line.
		paint.setStyle(Paint.Style.STROKE);
		paint.setStrokeWidth(SCALE_WIDTH);
		canvas.drawLine(centreX, centreY, midX, midY, paint);

		// If we have valid data, draw the data value as a pointer.
		// REMEMBER THE SCREEN Y-AXIS is NEGATIVE UP.
		if (valid) {
			rotateMatrix.setRotate(arcCentre + sval);
			pointerPath.transform(rotateMatrix);
			pointerPath.offset(centreX, centreY);

			// Draw the pointer, with a contrasting outline.
			paint.setColor(bgColour);
			paint.setStyle(Paint.Style.STROKE);
			paint.setStrokeWidth(1.5f);
			canvas.drawPath(pointerPath, paint);
			paint.setColor(getPlotColor());
			paint.setStyle(Paint.Style.FILL);
			canvas.drawPath(pointerPath, paint);
			
			pointerPath.offset(-centreX, -centreY);
			rotateMatrix.setRotate(-arcCentre - sval);
			pointerPath.transform(rotateMatrix);
		}
	}
	

    // ******************************************************************** //
    // Class Data.
    // ******************************************************************** //

    // Debugging tag.
	@SuppressWarnings("unused")
	private static final String TAG = "tricorder";

	// Widths of the scale lines.
	private static final int SCALE_WIDTH = 1;

	
	// ******************************************************************** //
	// Private Data.
	// ******************************************************************** //

	// The orientation of this gauge.
	private Orientation orientation;
	
	// Standard bar thickness for the current screen.
	private int barThickness;

	// A Rect that defines the bounds of the arc.
	private RectF arcRect;

	// A Rect that defines the bar drawn as the base of the arc.
	private Rect barRect;
	
	// Co-ordinates of the centre of the arc.
	private float centreX;
	private float centreY;
	
	// Co-ordinates of the centre point within the arc itself.
	private float midX;
	private float midY;
	
	// Angle which is at the centre of the arc's range.
	private float arcCentre;

	// A path that defines the pointer.
	private Path pointerPath;

	// Matrix used for rotating the pointer.
	private Matrix rotateMatrix;

	// The current value; flag to say whether we actually have data.
	private float currentValue;
	private boolean haveValue = false;
	
}

