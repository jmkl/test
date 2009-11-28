
/**
 * org.hermit.android.instrument: graphical instruments for Android.
 * <br>Copyright 2009 Ian Cameron Smith
 * 
 * <p>These classes provide input and display functions for creating on-screen
 * instruments of various kinds in Android apps.
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


package org.hermit.android.instruments;


import org.hermit.android.core.SurfaceRunner;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;


/**
 * A graphical display which shows some data in a region
 * within a view.  The data may come from an {@link Instrument} or
 * some other source.
 */
public class Gauge
{

	// ******************************************************************** //
	// Constructor.
	// ******************************************************************** //
	
	/**
	 * Set up this view.
	 * 
     * @param   parent          Parent surface.
     */
	public Gauge(SurfaceRunner parent) {
		parentSurface = parent;

		// Set up our paint.
		drawPaint = new Paint();

		initializePaint(drawPaint);
	}

	
	/**
	 * Set up this view.
	 * 
     * @param   parent          Parent surface.
     * @param	grid			Colour for drawing a data scale / grid.
     * @param	plot			Colour for drawing data plots.
	 */
	public Gauge(SurfaceRunner parent, int grid, int plot) {
		parentSurface = parent;
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
    // Global Layout Parameters.
    // ******************************************************************** //
	
    /**
     * Set the default font for all text.
     * 
     * @param   face        The default font for all text.
     */
    public static void setTextTypeface(Typeface face) {
        baseTextFace = face;
    }


    /**
     * Get the default font for all text.
     * 
     * @return              The default font for all text.
     */
    public static Typeface getTextTypeface() {
        return baseTextFace;
    }


    /**
     * Set the base size for text based on this screen's size.
     * 
     * @param   size        Base text size for the app.
     */
	public static void setBaseTextSize(float size) {
        baseTextSize = size;
        effectiveBaseTextSize = size < MIN_TEXT ? MIN_TEXT : size;
    }


    /**
     * Get the base size for text based on this screen's size.
     * 
     * @return              Base text size for the app.
     */
	public static float getBaseTextSize() {
        return baseTextSize;
    }


    /**
     * Get the size for header text based on this screen's size.
     * 
     * @return              Header text size for the app.
     */
    public static float getHeadTextSize() {
        return baseTextSize * 1.3f;
    }


    /**
     * Get the size for mini text based on this screen's size.
     * 
     * @return              Mini text size for the app.
     */
	public static float getMiniTextSize() {
        return effectiveBaseTextSize * 0.97f;
    }


    /**
     * Get the size for tiny text based on this screen's size.
     * 
     * @return              Tiny text size for the app.
     */
	public static float getTinyTextSize() {
        return effectiveBaseTextSize * 0.88f;
    }


    /**
     * Set the horizontal scaling of the font; this can be used to
     * produce a tall, thin font.
     * 
     * @param   scale       Horizontal scaling of the font.
     */
    public static void setTextScaleX(float scale) {
        textScaleX = scale;
    }


    /**
     * Get the base size for text based on this screen's size.
     * 
     * @return              Horizontal scaling of the font.
     */
    public static float getTextScaleX() {
        return textScaleX;
    }


    /**
     * Set the sidebar width.
     * 
     * @param   width       The sidebar width.
     */
	public static void setSidebarWidth(int width) {
        viewSidebar = width;
    }

    
    /**
     * Get the sidebar width.
     * 
     * @return              The sidebar width.
     */
	public static int getSidebarWidth() {
        return viewSidebar;
    }


    /**
     * Set the amount of padding between major elements in a view.
     * 
     * @param   pad     The amount of padding between major elements in a view.
     */
	public static void setInterPadding(int pad) {
        interPadding = pad;
    }


    /**
     * Get the amount of padding between major elements in a view.
     * 
     * @return          The amount of padding between major elements in a view.
     */
	public static int getInterPadding() {
        return interPadding;
    }


    /**
     * Set the amount of padding within atoms within an element.  Specifically
     * the small gaps in side bars.
     * 
     * @param   gap     The amount of padding within atoms within an element
     */
	public static void setInnerGap(int gap) {
        innerGap = gap;
    }


    /**
     * Get the amount of padding within atoms within an element.  Specifically
     * the small gaps in side bars.
     * 
     * @return          The amount of padding within atoms within an element
     */
	public static int getInnerGap() {
        return innerGap;
    }


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
	public void setGeometry(Rect bounds) {
		elemBounds = bounds;
	}

	
	/**
	 * Get the minimum preferred width for this atom.
	 * 
	 * @return			The minimum preferred width for this atom.
	 * 					Returns zero if we don't know yet.
	 */
	public int getPreferredWidth() {
		return 0;
	}
	

	/**
	 * Get the minimum preferred height for this atom.
	 * 
	 * @return			The minimum preferred height for this atom.
	 * 					Returns zero if we don't know yet.
	 */
	public int getPreferredHeight() {
		return 0;
	}
	

    /**
     * Determine whether we have the bounding rect of this Element.
     * 
     * @return              True if our geometry has been set up.
     */
    public final boolean haveBounds() {
        return getWidth() > 0 && getHeight() > 0;
    }


	/**
	 * Get the bounding rect of this Element.
	 * 
	 * @return				The bounding rect of this element within
	 * 						its parent View.  This will be 0, 0, 0, 0 if
	 * 						setGeometry() has not been called yet.
	 */
	public final Rect getBounds() {
		return elemBounds;
	}

	
	/**
	 * Get the width of this element -- i.e. the current configured width.
	 * 
	 * @return				The width of this element within
	 * 						its parent View.  This will be 0 if
	 * 						setGeometry() has not been called yet.
	 */
	public final int getWidth() {
		return elemBounds.right - elemBounds.left;
	}
	
	
	/**
	 * Get the height of this element -- i.e. the current configured height.
	 * 
	 * @return				The height of this element within
	 * 						its parent View.  This will be 0 if
	 * 						setGeometry() has not been called yet.
	 */
	public final int getHeight() {
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
	public void setBackgroundColor(int col) {
		colBg = col;
	}
	

	/**
	 * Get the background colour of this element.
	 * 
	 * @return				The background colour, in ARGB format.
	 */
	public int getBackgroundColor() {
		return colBg;
	}
	

	/**
	 * Set the plot colours of this element.
	 * 
     * @param	grid      Colour for drawing a data scale / grid.
     * @param	plot      Colour for drawing data plots.
	 */
	public void setDataColors(int grid, int plot) {
		gridColour = grid;
		plotColour = plot;
	}
	

    /**
     * Set the data scale / grid colour of this element.
     * 
     * @param   grid      Colour for drawing a data scale / grid.
     */
    public void setGridColor(int grid) {
        gridColour = grid;
    }
    

    /**
     * Set the data plot colour of this element.
     * 
     * @param   plot      Colour for drawing a data plot.
     */
    public void setPlotColor(int plot) {
        plotColour = plot;
    }
    

    /**
     * Get the data scale / grid colour of this element.
     * 
     * @return            Colour for drawing a data scale / grid.
     */
    public int getGridColor() {
        return gridColour;
    }
    

    /**
     * Get the data plot colour of this element.
     * 
     * @return          Colour for drawing data plots.
     */
    public int getPlotColor() {
        return plotColour;
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
     * This method is called to ask the element to draw its static
     * content; i.e. the background / chrome.
     * 
     * @param   canvas      Canvas to draw into.
     */
    public void drawBackground(Canvas canvas) {
        // Clip to our part of the canvas.
        canvas.save();
        canvas.clipRect(getBounds());
        
        drawBackgroundBody(canvas, drawPaint);
        
        canvas.restore();
    }

    
    /**
     * Do the subclass-specific parts of drawing the background
     * for this element.  Subclasses should override
     * this if they have significant background content which they would
     * like to draw once only.  Whatever is drawn here will be saved in
     * a bitmap, which will be rendered to the screen before the
     * dynamic content is drawn.
     * 
     * <p>Obviously, if implementing this method, don't clear the screen when
     * drawing the dynamic part.
     * 
     * @param   canvas      Canvas to draw into.
     * @param   paint       The Paint which was set up in initializePaint().
     */
    protected void drawBackgroundBody(Canvas canvas, Paint paint) {
        // If not overridden, just fill with BG colour.
        canvas.drawColor(colBg);
    }
    

	/**
	 * This method is called to ask the element to draw its dynamic content.
	 * 
	 * @param	canvas		Canvas to draw into.
	 * @param	now			Nominal system time in ms. of this update.
	 */
	public void draw(Canvas canvas, long now) {
		drawStart(canvas, drawPaint, now);
		drawBody(canvas, drawPaint, now);
		drawFinish(canvas, drawPaint, now);
	}


	/**
	 * Do initial parts of drawing for this element.
	 * 
	 * @param	canvas		Canvas to draw into.
	 * @param	paint		The Paint which was set up in initializePaint().
     * @param   now         Nominal system time in ms. of this update.
	 */
	protected void drawStart(Canvas canvas, Paint paint, long now) {
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
     * @param   now         Nominal system time in ms. of this update.
	 */
	protected void drawBody(Canvas canvas, Paint paint, long now) {
		// If not overridden, just fill with BG colour.
		canvas.drawColor(colBg);
	}
	

	/**
	 * Wrap up drawing of this element.
	 * 
	 * @param	canvas		Canvas to draw into.
	 * @param	paint		The Paint which was set up in initializePaint().
     * @param   now         Nominal system time in ms. of this update.
	 */
	protected void drawFinish(Canvas canvas, Paint paint, long now) {
		canvas.restore();
	}

	
	// ******************************************************************** //
	// Utilities.
	// ******************************************************************** //

	/**
	 * Get the app context of this Element.
	 * 
	 * @return             The app context we're running in.
	 */
	protected SurfaceRunner getSurface() {
		return parentSurface;
	}
	

	// ******************************************************************** //
	// Private Constants.
	// ******************************************************************** //

	// The minimum happy text size.
	private static final float MIN_TEXT = 20f;
	
	
    // ******************************************************************** //
    // Class Data.
    // ******************************************************************** //

    // Debugging tag.
	@SuppressWarnings("unused")
	private static final String TAG = "instrument";

    // The default font for all text.
    private static Typeface baseTextFace = Typeface.MONOSPACE;

    // The base size for all text, based on screen size.
    private static float baseTextSize = MIN_TEXT;
    
    // Base text size used to calculate smaller sizes.
    private static float effectiveBaseTextSize = MIN_TEXT;
    
    // The horizontal scaling of the font; this can be used to
    // produce a tall, thin font.
    private static float textScaleX = 1f;

    // The thickness of a side bar in a view element.
    private static int viewSidebar;
    
    // The amount of padding between major elements in a view.
    private static int interPadding;
    
    // The amount of padding within atoms within an element.  Specifically
    // the small gaps in side bars.
    private static int innerGap;


	// ******************************************************************** //
	// Private Data.
	// ******************************************************************** //

	// Application handle.
	private final SurfaceRunner parentSurface;
	
	// The paint we use for drawing.
	private Paint drawPaint = null;

	// The bounding rect of this element within its parent View.
	private Rect elemBounds = new Rect(0, 0, 0, 0);

	// Background colour.
	private int colBg = 0xff000000;

	// Colour of the graph grid and plot.
	private int gridColour = 0xff00ff00;
	private int plotColour = 0xffff0000;

}

