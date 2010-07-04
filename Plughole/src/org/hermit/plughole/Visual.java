
/**
 * Plughole: a rolling-ball accelerometer game.
 * <br>Copyright 2008-2010 Ian Cameron Smith
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


package org.hermit.plughole;


import android.graphics.Canvas;
import android.graphics.RectF;


/**
 * Class representing a visual element of the game board.  This abstract class
 * defines methods for building the hierarchy of elements, and run-time
 * features such as drawing operations.
 */
abstract class Visual
    extends Element
{
	
	// ******************************************************************** //
	// Constructor.
	// ******************************************************************** //

	/**
	 * Create a non-rectangular visual element.
	 * 
	 * @param	app			Application context.
	 * @param	id			The id of this element.
	 * @param	xform		The transformation that needs to be applied
	 * 						to the element to map it to screen co-ordinates.
	 */
	Visual(Plughole app, String id, Matrix xform) {
		this(app, id, null, xform);
	}


	/**
	 * Create an element.
	 * 
	 * @param	app		   Application context.
	 * @param	id		   The id of this element.
	 * @param	visRect    If not null, a rectangular box to which a graphic
	 *                     can be drawn for this element, in level
	 *                     co-ordinates.  Null if the element does not have
	 *                     a rectangular base shape.  However, holes use
	 *                     their bounding box as a visual rect, so they can
	 *                     have graphics.
	 * @param	xform	   The transformation that needs to be applied
	 * 					   to the element to map it to screen co-ordinates.
	 */
	Visual(Plughole app, String id, RectF visRect, Matrix xform) {
		super(app, id);
		visualRect = visRect;
		screenTransform = xform;
	}


    // ******************************************************************** //
    // Building.
    // ******************************************************************** //

    /**
     * Set the visual rectangular box of this element.  This is called when
     * we're added to our parent.
     * 
     * Subclasses that need a rect can override this.
     * 
     * @param   rect        The rectangle, in level co-ordinates, suitable for
     *                      attaching Graphics to.
     */
    void setRect(RectF rect) {
    }
    

	// ******************************************************************** //
	// Accessors.
	// ******************************************************************** //

    /**
     * Get the visual rectangular box of this element.
     * 
     * @return              Our rectangle, in level co-ordinates, suitable for
     *                      attaching Graphics to.  If this Element is not
     *                      a rectangle, null.
     */
    protected final RectF getVisualRect() {
        return visualRect;
    }
    

	/**
	 * Get the transform for this element.
	 * 
	 * @return              The transformation that needs to be applied
	 * 						to the element to map it to screen co-ordinates.
	 */
	protected final Matrix getTransform() {
		return screenTransform;
	}
	
    
    // ******************************************************************** //
    // Drawing.
    // ******************************************************************** //

    /**
     * Draw this element onto the given canvas.
     * 
     * @param   canvas          Canvas to draw on.
     * @param   time            Total level time in ms.  A time of zero
     *                          indicates that we're drawing statically,
     *                          not in the game loop.
     * @param   clock           Level time remaining in ms.
     */
    protected abstract void draw(Canvas canvas, long time, long clock);

	
	// ******************************************************************** //
	// Private Data.
	// ******************************************************************** //
	
	// The transformation that needs to be applied to the element to
	// map it to screen co-ordinates.
	private final Matrix screenTransform;

    // The rectangle box of this element, in level co-ordinates, if it
    // is a rectangle; else null.
    private RectF visualRect;

}

