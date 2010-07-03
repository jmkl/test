
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


/**
 * Class representing an action triggered by some event in the game.
 */
final class Action
{

	// ******************************************************************** //
	// Public Types.
	// ******************************************************************** //
	
	/**
	 * The events that can trigger an action.
	 */
	public static enum Trigger {
	    /**
	     * An action triggered by crossing a line.
	     */
        ONCROSS(),
	    /**
	     * An action triggered by bouncing off a wall.
	     */
        ONBOUNCE(),
	    /**
	     * An action triggered by being in a defined zone.
	     */
        WHILEZONE();
        
        public static final Trigger[] VALUES = values();
        public static final int NUM_VALUES = VALUES.length;
	}
	
	
	/**
	 * The types of hole that exist.
	 */
	public static enum Type {
	    /**
	     * An action that changes the ball's speed.
	     */
        SPEED(),
        /**
         * An action that accelerates the ball in a given direction.
         */
		ACCEL(),
        /**
         * An action that teleports the ball somewhere.
         */
		TELEPORT(),
        /**
         * An action that turns some element off.
         */
        OFF(),
        /**
         * An action that turns some element on.
         */
        ON(),
        /**
         * An action that toggles some element's on/off state.
         */
        ONOFF(),
        /**
         * The player wins.
         */
		WIN(),
        /**
         * The player loses.
         */
		LOSE();
		
		Type() {
		}
	}

	
	// ******************************************************************** //
	// Constructor.
	// ******************************************************************** //

	/**
	 * Create an action.
	 * 
	 * @param	trig		How this action is triggered.
	 * @param	type		The type of this action.
	 */
	public Action(Trigger trig, Type type) {
		this(trig, type, null);
	}


	/**
	 * Create an action that does something with a message.
	 * 
	 * @param	trig		How this action is triggered.
	 * @param	type		The type of this action.
	 * @param	msg			The message it displays.  Null for no message.
	 */
	public Action(Trigger trig, Type type, String msg) {
		this.trigger = trig;
		this.type = type;
		this.message = msg;
	}


	// ******************************************************************** //
	// Configuration.
	// ******************************************************************** //

    /**
     * Set the current speed change in this action.
     * 
     * @param   mag         Magnitude of acceleration, 0-1.
     */
    void setSpeed(double mag) {
        accelX = 0;
        accelY = 0;
        accelMag = mag;
    }
    

	/**
	 * Set the current acceleration in this action.
	 * 
	 * @param	x			X component of the unit vector.
	 * @param	y			Y component of the unit vector.
	 * @param	mag			Magnitude of acceleration, 0-1.
	 */
	void setAccel(double vx, double vy, double mag) {
		accelX = vx;
		accelY = vy;
		accelMag = mag;
	}
	

	/**
	 * Set the target item (e.g. place to teleport to) in this action.
	 * 
	 * @param	target		Target element for this action.
	 */
	void setTarget(String target) {
		// FIXME this.target = target;
	}
	

	/**
	 * Set the target item (e.g. place to teleport to) in this action.
	 * 
	 * @param	target		Target element for this action.
	 */
	void setTarget(Object target) {
		this.target = target;
	}
	

	// ******************************************************************** //
	// Accessors.
	// ******************************************************************** //

	/**
	 * Get the event that triggers this action.
	 * 
	 * @return				The trigger for this action.
	 */
	public Trigger getTrigger() {
		return trigger;
	}


	/**
	 * Get the type of this action.
	 * 
	 * @return				The type of this action.
	 */
	public Type getType() {
		return type;
	}


	/**
	 * Get the message for this action.
	 * 
	 * @return				The message for this action; null if no message.
	 */
	public String getMessage() {
		return message;
	}


	/**
	 * Get the Y component of the unit vector defining the direction of
	 * acceleration caused by this action.
	 * 
	 * @return				The X component of the unit vector.
	 */
	public double getAccelX() {
		return accelX;
	}


	/**
	 * Get the Y component of the unit vector defining the direction of
	 * acceleration caused by this action.
	 * 
	 * @return				The Y component of the unit vector.
	 */
	public double getAccelY() {
		return accelY;
	}


	/**
	 * Get the magnitude of acceleration caused by this action.
	 * 
	 * @return				The magnitude of acceleration caused by this action.
	 */
	public double getAccelMag() {
		return accelMag;
	}


	/**
	 * Get the element targeted by this action.
	 * 
	 * @return				The target element for this action.
	 */
	public Object getTarget() {
		return target;
	}

	
	// ******************************************************************** //
	// Utilities.
	// ******************************************************************** //

	/**
	 * Convert this instance to a String.
	 * 
	 * @return             String representation of this instance.
	 */
	@Override
    public String toString() {
	    return type.toString();
	}
	
    
    // ******************************************************************** //
    // Private Data.
    // ******************************************************************** //

	// The trigger for this Action.
	private final Trigger trigger;

	// The type of this Action.
	private final Type type;

	// The message for this action; null if no message.
	private final String message;

	// If this is a gravity action, the current acceleration.
	private double accelX = 0;
	private double accelY = 0;
	private double accelMag = 0;

	// If this is a TELEPORT action, the target point.
	private Object target = null;

}

