
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


/**
 * Class representing an action triggered by some event in the game.
 */
final class Action
{

	// ******************************************************************** //
	// Public Types.
	// ******************************************************************** //
	
	/**
	 * The types of hole that exist.
	 */
	public static enum Type {
		ACCEL(),
		TELEPORT(),
		WIN(),
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
	 * @param	type			The type of this action.
	 */
	public Action(Type type) {
		this(type, 0);
	}


	/**
	 * Create an action that does something with a message.
	 * 
	 * @param	type			The type of this action.
	 * @param	msgid			The resource ID of the message it displays.
	 * 							Use zero for no message.
	 */
	public Action(Type type, int msgid) {
		this.type = type;
		this.messageId = msgid;
	}


	// ******************************************************************** //
	// Configuration.
	// ******************************************************************** //

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
	void setTarget(Point target) {
		this.target = target;
	}
	

	// ******************************************************************** //
	// Accessors.
	// ******************************************************************** //

	/**
	 * Get the type of this action.
	 * 
	 * @return				The type of this action.
	 */
	public Type getType() {
		return type;
	}


	/**
	 * Get the resource ID of the message for this action.
	 * 
	 * @return				The resource ID of the message for this action;
	 * 						0 if no message.
	 */
	public int getMessageId() {
		return messageId;
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
	public Point getTarget() {
		return target;
	}

	
	// ******************************************************************** //
	// Private Data.
	// ******************************************************************** //

	// The type of this Action.
	private final Type type;

	// The resource ID of the message for this action; 0 if no message.
	private final int messageId;

	// If this is a gravity action, the current acceleration.
	private double accelX = 0;
	private double accelY = 0;
	private double accelMag = 0;

	// If this is a TELEPORT action, the target point.
	private Point target = null;

}

