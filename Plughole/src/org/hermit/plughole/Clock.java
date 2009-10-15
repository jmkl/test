
/**
 * Plughole: a rolling-ball accelerometer game.
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


package org.hermit.plughole;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;


/**
 * Class representing a graphic clock drawn on the game board.
 */
class Clock
	extends Element
{
	
	// ******************************************************************** //
	// Constructor.
	// ******************************************************************** //

	/**
	 * Create a graphic which displays an animation.
	 * 
	 * @param	app				Application context.  This provides
	 * 							access to resources and image loading.
	 * @param	imgIds			Resource IDs of the graphics in the animation.
	 * @param	box				The bounding box for the graphic.
	 * @param	xform			Transform to apply to the raw data.
	 * @param	vertical		If false, the clock counts down from the
	 * 							end X to the start X.  If true, end Y to
	 * 							start Y.
	 */
	public Clock(Plughole app, RectF box, Matrix xform, boolean vertical) {
		super(app);
		
		// Calculate the actual geometry of the graphic.
		bounds = xform.transform(box);
		
		// Figure out which way we grow taking rotation into account.
		Matrix.ORotate rot = xform.getRotation();
		if (rot == Matrix.ORotate.LEFT || rot == Matrix.ORotate.RIGHT)
			vertical = !vertical;
		isVertical = vertical;
	}


	// ******************************************************************** //
	// Accessors.
	// ******************************************************************** //

	/**
	 * Get the bounding box of this graphic.
	 */
	RectF getBounds() {
		return bounds;
	}
	

	// ******************************************************************** //
	// Drawing.
	// ******************************************************************** //

	/**
	 * Draw this graphic onto the given canvas.
	 * 
	 * @param	canvas			Canvas to draw on.
	 * @param	time			Total level time in ms.  A time of zero
	 * 							indicates that we're drawing statically,
	 * 							not in the game loop.
	 * @param	clock			Level time remaining in ms.
	 */
	@Override
	protected void draw(Canvas canvas, long time, long clock) {
		clockPaint.setColor(0xff008000);
		clockPaint.setStyle(Paint.Style.FILL);
		canvas.drawRect(bounds, clockPaint);

		if (time > 0 && clock > 0) {
			final float frac = (float) clock / (float) time;
			final int color = frac < 0.2f ? 0xffff0000 :
								frac < 0.4f ? 0xffffff00 : 0xff00ff00;
			clockPaint.setColor(color);
			clockPaint.setStyle(Paint.Style.FILL);

			if (isVertical) {
				final int h = (int) (Math.round(bounds.bottom - bounds.top));
				final float end = bounds.bottom - h * frac;
				canvas.drawRect(bounds.left, end, bounds.right, bounds.bottom, clockPaint);
			} else {
				final int w = (int) (Math.round(bounds.right - bounds.left));
				final float end = bounds.left + w * frac;
				canvas.drawRect(bounds.left, bounds.top, end, bounds.bottom, clockPaint);
			}
		}

		clockPaint.setColor(0xff000000);
		clockPaint.setStyle(Paint.Style.STROKE);
		canvas.drawRect(bounds, clockPaint);
	}

	
	// ******************************************************************** //
	// Class Data.
	// ******************************************************************** //

    // Debugging tag.
	@SuppressWarnings("unused")
	private static final String TAG = "plughole";
	
	// Paint we use for drawing all text.
	private static final Paint clockPaint = new Paint();

	
	// ******************************************************************** //
	// Private Data.
	// ******************************************************************** //

	// Actual bounding box of this graphic in the scaled playing board.
	private RectF bounds;

	// True if the clock grows vertically; false for horizontal.
	private boolean isVertical;

}

