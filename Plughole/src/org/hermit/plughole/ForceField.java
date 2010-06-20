
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


import android.graphics.RectF;


/**
 * Class representing a force field in the game board.  This is an impervious
 * barrier while turned on, but can be turned off.  This is an immutable class.
 */
final class ForceField
	extends Graphic
{
	
	// ******************************************************************** //
	// Constructor.
	// ******************************************************************** //

	/**
	 * Create a hole.
	 * 
	 * @param	app        Application context.  This provides
	 *                     access to resources and image loading.
     * @param   box        The bounding box for the force field.
	 * @param	xform      Transform to apply to the raw data.
	 */
	public ForceField(Plughole app, RectF box, Matrix xform) {
		super(app, Graphic.FORCE_ANIM, box, xform);
        
        fieldUp = true;
	}


	// ******************************************************************** //
	// Accessors.
	// ******************************************************************** //

	/**
	 * Get the auxiliary level element which implements the force barrier.
	 * 
	 * @return             Barrier object which stops the ball passing
	 *                     through.  We add an action which zaps your speed
	 *                     up when you touch the field.
	 */
	Poly getBarrier() {
        // Create a barrier that gives the ball a sudden speed boost.
        Action zap = new Action(Action.Type.SPEED);
        zap.setSpeed(FORCE_SPEEDUP);
        return new Poly(getApp(), getBounds(), zap);
	}
	

    /**
     * Store the actual polygon which implements the wall.  This will be
     * a modified version of the one we returned above.
     * 
     * @param   wall        The real wall polygon.
     */
	void setRealBarrier(Poly wall) {
	    fieldPoly = wall;
	    
        fieldPoly.setReflectEnable(fieldUp);
        setVisible(fieldUp);
	}
	

    // ******************************************************************** //
    // Control.
    // ******************************************************************** //

    /**
     * Alter this force field's on/off state according to the given
     * action.
     * 
     * @param   action      One of Action.Type.OFF, Action.Type.ON, or
     *                      Action.Type.ONOFF (toggle).
     */
    void activate(Action.Type action) {
        switch (action) {
        case OFF:
            fieldUp = false;
            break;
        case ON:
            fieldUp = true;
            break;
        case ONOFF:
            fieldUp = !fieldUp;
            break;
        default:
            break;
        }
        fieldPoly.setReflectEnable(fieldUp);
        setVisible(fieldUp);
    }
    
    
    // ******************************************************************** //
    // Class Data.
    // ******************************************************************** //
    
    // The factor by which hitting the force field speeds up the ball.
    private static final double FORCE_SPEEDUP = 3.0f;


	// ******************************************************************** //
	// Private Data.
	// ******************************************************************** //

    // Flag whether this force field is on or not.
    private boolean fieldUp = true;
    
    // The polygon defining the outline of this force field.
    private Poly fieldPoly = null;
    
}

