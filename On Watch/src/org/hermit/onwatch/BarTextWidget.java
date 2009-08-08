
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
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.widget.TextView;


/**
 * This class displays a text label over a bargraph display.
 */
public class BarTextWidget
	extends TextView
{

	// ******************************************************************** //
    // Constructors.
    // ******************************************************************** //
	
	/**
	 * Create a watch clock.
	 * 
	 * @param	context			Parent application.
	 */
	public BarTextWidget(Context context) {
		super(context);
		init(context);
	}


	/**
	 * Create a crew watch schedule display.
	 * 
	 * @param	context			Parent application.
	 * @param	attrs			Layout attributes.
	 */
	public BarTextWidget(Context context, AttributeSet attrs) {
		super(context, attrs);
		init(context);
	}

	
	private void init(Context context) {
		barPaint = new Paint();
	}


    // ******************************************************************** //
	// Geometry.
	// ******************************************************************** //
	 
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
    	winWidth = width;
    	winHeight = height;
    	
    	// Re-draw.
		invalidate();
    }


	// ******************************************************************** //
	// Configuration.
	// ******************************************************************** //

	/**
	 * Set the bar position.
	 * 
	 * @param	frac			Fraction of the widget width to draw the
	 * 							bar at.
	 */
	public void setBar(float frac) {
		barFraction = frac;
		invalidate();
	}

	
	// ******************************************************************** //
	// Drawing.
	// ******************************************************************** //
	
	/**
	 * This method is called to ask the widget to draw itself.
	 * 
	 * @param	canvas		Canvas to draw into.
	 */
	@Override
	protected void onDraw(Canvas canvas) {
		// Draw in the progress bar.
		float bl = 0f;
		float br = bl + winWidth * barFraction;
		barPaint.setColor(BAR_COLOR);
		barPaint.setStyle(Paint.Style.FILL);
		canvas.drawRect(bl, 0f, br, winHeight, barPaint);

		// Draw in the text label.
		super.onDraw(canvas);
	}


	// ******************************************************************** //
	// Class Data.
	// ******************************************************************** //

    // Debugging tag.
	@SuppressWarnings("unused")
	private static final String TAG = "onwatch";

	// Colour to draw the bar.
	private static final int BAR_COLOR = 0xff006000;


	// ******************************************************************** //
	// Private Data.
	// ******************************************************************** //
	
	// Our window width and height.
	private int winWidth = 0;
	private int winHeight = 0;

	// Paint used for graphics.
	private Paint barPaint;
	
	// The fraction of width to show the bar at.
	private float barFraction = 0;

}

