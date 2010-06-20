
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

import java.util.Random;

import net.goui.util.MTRandom;

import android.graphics.Canvas;


/**
 * Class representing a visible element of the game board.  This abstract class
 * defines drawing operations.  It also provides convenience methods for
 * use in derived classes, such as random numbers.
 */
abstract class Element
{
	
	// ******************************************************************** //
	// Constructor.
	// ******************************************************************** //

	/**
	 * Create an element.
	 * 
	 * @param	app			Application context.
	 */
	Element(Plughole app) {
		appContext = app;
		elemAction = null;
	}


	/**
	 * Create an element.
	 * 
	 * @param	app			Application context.
	 * @param	action		Action to trigger if the ball interacts with
	 * 						this element.
	 */
	Element(Plughole app, Action action) {
		appContext = app;
		elemAction = action;
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
	 * Get the action triggered by this element.
	 * 
	 * @return				Action to trigger if the ball interacts with
	 * 						this element.
	 */
	protected final Action getAction() {
		return elemAction;
	}
	

    /**
     * Set the action triggered by this element.
     * 
     * @param   action      Action to trigger if the ball interacts with
     *                      this element.
     */
    protected final void setAction(Action action) {
        elemAction = action;
    }
    

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
	protected abstract void draw(Canvas canvas, long time, long clock);

	
	// ******************************************************************** //
	// Class Data.
	// ******************************************************************** //

	// Random number generator.
	private static final Random rnd = new MTRandom();

	
	// ******************************************************************** //
	// Private Data.
	// ******************************************************************** //
	
	// Our application.
	private Plughole appContext;

	// The action to take if the ball interacts with this element.
	private Action elemAction;

}

