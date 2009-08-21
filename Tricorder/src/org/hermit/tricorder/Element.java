
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
import android.graphics.Rect;
import android.view.SurfaceHolder;


/**
 * This class displays an element of the UI.  An element is a region
 * within a view, and can display text, etc.
 */
class Element
{

	// ******************************************************************** //
	// Constructor.
	// ******************************************************************** //
	
	/**
	 * Set up this view.
	 * 
	 * @param	context			Parent application context.
     * @param	sh				SurfaceHolder we're drawing in.
	 */
	public Element(Tricorder context, SurfaceHolder sh) {
		appContext = context;

		// Set up our paint.
		drawPaint = new Paint();

		initializePaint(drawPaint);
	}

	
	/**
	 * Set up this view.
	 * 
	 * @param	context			Parent application context.
     * @param	sh				SurfaceHolder we're drawing in.
     * @param	grid			Colour for drawing a data scale / grid.
     * @param	plot			Colour for drawing data plots.
	 */
	public Element(Tricorder context, SurfaceHolder sh, int grid, int plot) {
		appContext = context;
		gridColour = grid;
		plotColour = plot;

		// Set up our paint.
		drawPaint = new Paint();

		initializePaint(drawPaint);
	}


	/**
	 * Set up the paint for this element.  This is called during
	 * initialization.  Subclasses can override this to do class-specific
	 * one-time initialization.
	 * 
	 * @param paint			The paint to initialize.
	 */
	protected void initializePaint(Paint paint) { }
	
	   
    // ******************************************************************** //
	// Geometry.
	// ******************************************************************** //

    /**
     * This is called during layout when the size of this element has
     * changed.  This is where we first discover our size, so set
     * our geometry to match.
     * 
	 * @param	bounds		The bounding rect of this element within
	 * 						its parent View.
     */
	protected void setGeometry(Rect bounds) {
		elemBounds = bounds;
	}

	
	/**
	 * Get the minimum preferred width for this atom.
	 * 
	 * @return			The minimum preferred width for this atom.
	 * 					Returns zero if we don't know yet.
	 */
	int getPreferredWidth() {
		return 0;
	}
	

	/**
	 * Get the minimum preferred height for this atom.
	 * 
	 * @return			The minimum preferred height for this atom.
	 * 					Returns zero if we don't know yet.
	 */
	int getPreferredHeight() {
		return 0;
	}
	

	/**
	 * Get the bounding rect of this Element.
	 * 
	 * @return				The bounding rect of this element within
	 * 						its parent View.  This will be 0, 0, 0, 0 if
	 * 						setGeometry() has not been called yet.
	 */
	protected Rect getBounds() {
		return elemBounds;
	}

	
	/**
	 * Get the width of this element -- i.e. the current configured width.
	 * 
	 * @return				The width of this element within
	 * 						its parent View.  This will be 0 if
	 * 						setGeometry() has not been called yet.
	 */
	protected final int getWidth() {
		return elemBounds.right - elemBounds.left;
	}
	
	
	/**
	 * Get the height of this element -- i.e. the current configured height.
	 * 
	 * @return				The height of this element within
	 * 						its parent View.  This will be 0 if
	 * 						setGeometry() has not been called yet.
	 */
	protected final int getHeight() {
		return elemBounds.bottom - elemBounds.top;
	}
	
	
    // ******************************************************************** //
	// Appearance.
	// ******************************************************************** //

	/**
	 * Set the background colour of this element.
	 * 
	 * @param	col			The new background colour, in ARGB format.
	 */
	void setBackgroundColor(int col) {
		colBg = col;
	}
	

	/**
	 * Get the background colour of this element.
	 * 
	 * @return				The background colour, in ARGB format.
	 */
	int getBackgroundColor() {
		return colBg;
	}
	

	/**
	 * Set the background colour of this element.
	 * 
     * @param	grid			Colour for drawing a data scale / grid.
     * @param	plot			Colour for drawing data plots.
	 */
	void setDataColors(int grid, int plot) {
		gridColour = grid;
		plotColour = plot;
	}
	

	// ******************************************************************** //
	// View Drawing.
	// ******************************************************************** //
	
	/**
	 * Get this element's Paint.
	 * 
	 * @return				The Paint which was set up in initializePaint().
	 */
	protected Paint getPaint() {
		return drawPaint;
	}
	
	
	/**
	 * This method is called to ask the element to draw itself.
	 * 
	 * @param	canvas		Canvas to draw into.
	 * @param	now				Current system time in ms.
	 */
	protected void draw(Canvas canvas, long now) {
		drawStart(canvas, drawPaint);
		drawBody(canvas, drawPaint);
		drawFinish(canvas, drawPaint);
	}


	/**
	 * Do initial parts of drawing for this element.
	 * 
	 * @param	canvas		Canvas to draw into.
	 * @param	paint		The Paint which was set up in initializePaint().
	 */
	protected void drawStart(Canvas canvas, Paint paint) {
		// Clip to our part of the canvas.
		canvas.save();
		canvas.clipRect(getBounds());
	}

	
	/**
	 * Do the subclass-specific parts of drawing for this element.
	 * 
	 * Subclasses should override this to do their drawing.
	 * 
	 * @param	canvas		Canvas to draw into.
	 * @param	paint		The Paint which was set up in initializePaint().
	 */
	protected void drawBody(Canvas canvas, Paint paint) {
		// If not overridden, just fill with BG colour.
		canvas.drawColor(colBg);
	}
	

	/**
	 * Wrap up drawing of this element.
	 * 
	 * @param	canvas		Canvas to draw into.
	 * @param	paint		The Paint which was set up in initializePaint().
	 */
	protected void drawFinish(Canvas canvas, Paint paint) {
		canvas.restore();
	}

	
	// ******************************************************************** //
	// Utilities.
	// ******************************************************************** //

	protected Tricorder getContext() {
		return appContext;
	}
	

	protected String getRes(int resid) {
		return appContext.getString(resid);
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

	// Application handle.
	protected final Tricorder appContext;
	
	// The paint we use for drawing.
	protected Paint drawPaint = null;

	// The bounding rect of this element within its parent View.
	protected Rect elemBounds = new Rect(0, 0, 0, 0);

	// Background colour.
	protected int colBg = 0xff000000;

	// Colour of the graph grid and plot.
	protected int gridColour = 0xff00ff00;
	protected int plotColour = 0xffff0000;

}

