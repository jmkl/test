
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


import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

import net.goui.util.MTRandom;

import org.hermit.plughole.LevelReader.LevelException;
import org.xmlpull.v1.XmlPullParser;

import android.graphics.Canvas;
import android.graphics.RectF;


/**
 * Class representing an element of the game board.  This abstract class
 * defines methods for building the hierarchy of elements, and run-time
 * features such as drawing operations.  It also provides convenience
 * methods for use in derived classes, such as random numbers.
 */
abstract class Element
{
	
	// ******************************************************************** //
	// Constructor.
	// ******************************************************************** //

	/**
	 * Create a non-rectangular element.
	 * 
	 * @param	app			Application context.
	 * @param	id			The id of this element.
	 * @param	xform		The transformation that needs to be applied
	 * 						to the element to map it to screen co-ordinates.
	 */
	Element(Plughole app, String id, Matrix xform) {
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
	Element(Plughole app, String id, RectF visRect, Matrix xform) {
		appContext = app;
		elemId = id;
		visualRect = visRect;
		screenTransform = xform;
	}

	
	// ******************************************************************** //
	// Construction.
	// ******************************************************************** //
	
	/**
	 * Add a child to this element.  This is used during level parsing.
	 * 
	 * <p>The default implementation just rejects children.  Subclasses
	 * must override this if they expect children.
	 * 
	 * @param	p			The parser the level is being read from.
	 * @param	tag 		The name of this item's XML tag.
	 * @param	child		The child to add to this element.
	 * @return				true iff this child has been accepted.  If
	 * 						false, the child is actually a sibling; it
	 * 						has not been added here, and needs to be
	 * 						added to the parent.
	 */
	boolean addChild(XmlPullParser p, String tag, Object child)
		throws LevelException
	{
		throw new LevelException(p, "<" + tag + "> does not take children");
	}
	
	
	/**
	 * Add an action to this element.  This is used during level parsing.
	 * 
	 * @param	act 		The action to add.
	 */
	void addAction(Action act)
		throws LevelException
	{
	    if (buildingActions == null)
	         buildingActions = new HashMap<Action.Trigger, ArrayList<Action>>();
	    
	    Action.Trigger trig = act.getTrigger();
	    if (!buildingActions.containsKey(trig))
	        buildingActions.put(trig, new ArrayList<Action>());
	    
	    ArrayList<Action> alist = buildingActions.get(trig);
	    alist.add(act);
	}
	

	/**
	 * We're finished adding children; do any required initialization.
	 * 
	 * <p>The default implementation does nothing.  Subclasses
	 * can override this if they wish. Subclass implementations <b>must</b>
	 * call through to this base implementation as their first action.
	 */
	void finished() {
	}
	
	
	// ******************************************************************** //
	// Accessors.
	// ******************************************************************** //

	/**
	 * Get the application context.
	 * 
	 * @return             Our application context.
	 */
	protected final Plughole getApp() {
		return appContext;
	}
	

	/**
	 * Get the ID of this element.
	 * 
	 * @return              The ID; null if none is set.
	 */
	protected final String getId() {
		return elemId;
	}
	

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
	

    /**
     * Get the actions associated with a particular trigger for this element.
     * 
     * @param   trig        The trigger we want the actions for.
     * @return              The actions attached to that trigger; null if none.
     */
    protected final Action[] getActions(Action.Trigger trig) {
        if (buildingActions == null)
            return null;
        ArrayList<Action> alist = buildingActions.get(trig);
        if (alist == null)
            return null;
        
        Action[] arr = new Action[alist.size()];
        alist.toArray(arr);
        return arr;
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
	// Utility Methods.
	// ******************************************************************** //

	/**
	 * Get a random integer between 0 (inclusive) and the specified value
	 * (exclusive).
	 * 
	 * @param	range		The bound on the random number to be returned.
	 * @return				An int value between 0 (inclusive) and range
	 * 						(exclusive).
	 */
	protected final int rndInt(int range) {
		return rnd.nextInt(range);
	}
	
	
	// ******************************************************************** //
	// Class Data.
	// ******************************************************************** //

	// Random number generator.
	private static final Random rnd = new MTRandom();

	
	// ******************************************************************** //
	// Private Data.
	// ******************************************************************** //
	
	// Our application.
	private final Plughole appContext;

	// The ID of this element.
	private final String elemId;
	
	// The transformation that needs to be applied to the element to
	// map it to screen co-ordinates.
	private final Matrix screenTransform;

    // The rectangle box of this element, in level co-ordinates, if it
    // is a rectangle; else null.
    private RectF visualRect;

    // List of OnCross actions this element has.  Null if none.
	// Used in element construction.
    private HashMap<Action.Trigger, ArrayList<Action>> buildingActions = null;

}

