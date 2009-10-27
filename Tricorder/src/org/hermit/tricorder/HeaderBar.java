
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

import org.hermit.tricorder.TricorderView.ViewDefinition;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.view.View;


/**
 * The tricorder header bar, consisting of the corner swoop and the top
 * coloured bar.
 */
class HeaderBar
	extends View
{

	// ******************************************************************** //
	// Public Types.
	// ******************************************************************** //
	
	/**
	 * Set up this view.
	 * 
	 * @param	parent			Parent application context.
	 * @param	nWidth			Width of the navigation bar.  The swoopy corner
	 * 							we draw here has to match this.
	 * @param	tHeight			Height of the top title bar.  The swoopy corner
	 * 							we draw here has to match this.
	 */
	HeaderBar(Tricorder parent, int nWidth, int tHeight) {
		super(parent);
		
		navWidth = nWidth;
		titleHeight = tHeight;
		
    	setBackgroundColor(Tricorder.COL_BG);
		barPaint = new Paint();
		barPaint.setAntiAlias(true);
	}

			   
    // ******************************************************************** //
	// Geometry Management.
	// ******************************************************************** //

    /**
     * This is called during layout when the size of this view has
     * changed.  This is where we first discover our window size, so set
     * our geometry to match.
     * 
     * @param	w				Current width of this view.
     * @param	h				Current height of this view.
     * @param	oldw			Old width of this view.  0 if we were
     * 							just added.
     * @param	oldh			Old height of this view.   0 if we were
     * 							just added.
     */
	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		int subHeight = h - titleHeight;
		int innerWidth = navWidth / 2;
		outerCurve = new RectF(0, 0, navWidth * 2, h * 2);
		innerCurve = new RectF(navWidth, titleHeight,
							   navWidth + innerWidth * 2, h + subHeight);

		barPath = new Path();
		barPath.moveTo(0, h);
		barPath.arcTo(outerCurve, 180, 90);
		barPath.lineTo(w, 0);
		barPath.lineTo(w, titleHeight);
		barPath.lineTo(navWidth + innerWidth, titleHeight);
		barPath.arcTo(innerCurve, 270, -90);
		barPath.close();
	}

	
	// ******************************************************************** //
	// Data Management.
	// ******************************************************************** //

    /**
     * Select and display the given view.
     * 
     * @param	viewDef			View definition of the view to show.
     */
    void selectDataView(ViewDefinition viewDef) {
    	barColor = viewDef.bgColor;
		invalidate();
    }


	// ******************************************************************** //
	// View Drawing.
	// ******************************************************************** //
	
	/**
	 * This method is called to ask the view to draw itself.
	 * 
	 * @param	canvas		Canvas to draw into.
	 */
	@Override
	protected void onDraw(Canvas canvas) {
		// Drawing the bar is easy -- just draw the path.
		barPaint.setColor(barColor);
		barPaint.setStyle(Paint.Style.FILL);
		canvas.drawPath(barPath, barPaint);
	}
	
 
    // ******************************************************************** //
    // Class Data.
    // ******************************************************************** //

    // Debugging tag.
	@SuppressWarnings("unused")
	private static final String TAG = "tricorder";

	
	// ******************************************************************** //
	// Private Data.
	// ******************************************************************** //
	
	// Width of the navigation bar, and the height of the top title bar.
	// The swoopy corner we draw here has to blend into both of these.
	private int navWidth;
	private int titleHeight;
	 
	// Rectangles defining the bounds of the outer and inner curves
	// of the swoop.
	private RectF outerCurve;
	private RectF innerCurve;

	// Path defining the shape of the bar.
	private Path barPath;

	// Paint for drawing the bar.
	private Paint barPaint;

	// Current color for the bar.
	private int barColor;

}

