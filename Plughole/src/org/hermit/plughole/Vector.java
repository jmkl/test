
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
 * Class representing a vector.  This is an immutable class.
 * 
 * Bear in mind this is not a line, just a 2-D delta.  We don't store any
 * absolute position.
 */
final class Vector {

	// ******************************************************************** //
	// Constructors.
	// ******************************************************************** //
	
	/*
	 * Create a Vector with the given X and Y magnitudes.
	 * 
	 * @param	x			X magnitude.
	 * @param	y			Y magnitude.
	 */
	public Vector(double x, double y) {
		this.x = x;
		this.y = y;
	}

	
	/*
	 * Create a Vector which is the difference between two points; i.e.
	 * whose X and Y magnitudes are the deltas between the points.  We do
	 * not store the absolute position.
	 * 
	 * @param	a			First point.
	 * @param	b			Second point.  The created Vector represents the
	 * 						vector from a to b.
	 */
	public Vector(Point a, Point b) {
		this.x = b.x - a.x;
		this.y = b.y - a.y;
	}

	
	// ******************************************************************** //
	// Public Methods.
	// ******************************************************************** //

	/**
	 * Get the angle of this vector.
	 * 
	 * @return				The angle in which this Vector points, in degrees.
	 * 						Zero is the positive X direction; angles on the
	 * 						positive Y side are positive; angles on the
	 * 						negative Y side are negative.
	 */
	public double angle() {
		return Math.toDegrees(Math.atan2(y, x));
	}
	

	/**
	 * Get the angle of this vector relative to another vector.
	 * 
	 * @param	v			The Vector to measure against.
	 * @return				this.angle() - v.angle(), normalized to the
	 * 						range -180 .. 180.
	 */
	public double angle(Vector v) {
		double turn = this.angle() - v.angle();
		if (turn > 180)
			turn -= 360;
		else if (turn <- 180)
			turn += 360;
		return turn;
	}
	
	
	/**
	 * Create a new Vector which is the rotation of this Vector by
	 * the given angle.
	 * 
	 * @param	a			Angle to rotate by, positive clockwise,
	 * 						in degrees.
	 */
	public Vector rotate(double a) {
		a = Math.toRadians(a);
		final double cos = Math.cos(a);
		final double sin = Math.sin(a);
		return new Vector(x * cos - y * sin, y * cos + x * sin);
	}

	
	// ******************************************************************** //
	// Public Data.
	// ******************************************************************** //
	
	public final double x;
	public final double y;

}

