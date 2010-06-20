
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
 * Class representing a point.  This is an immutable class.
 */
final class Point {
	
	// ******************************************************************** //
	// Constructor.
	// ******************************************************************** //
	
	/**
	 * Create a point at the given X and Y.
	 * 
	 * @param	x			X position.
	 * @param	y			Y position.
	 */
	public Point(double x, double y) {
		this.x = x;
		this.y = y;
	}


	/**
	 * Create a new point at the given offset from this one.
	 * 
	 * @param	p			Starting point.
	 * @param	off			Offset to displace by.
	 */
	public Point(Point p, Vector off) {
		this.x = p.x + off.x;
		this.y = p.y + off.y;
	}


	// ******************************************************************** //
	// Public Methods.
	// ******************************************************************** //
	
	/**
	 * Measure the distance between this point and another.
	 * 
	 * @param	x			X position of the other point.
	 * @param	y			Y position of the other point.
	 * @return				Distance between the points.
	 */
	public final double distance(double x, double y) {
		final double dx = x - this.x;
		final double dy = y - this.y;
		return Math.sqrt(dx * dx + dy * dy);
	}

	
	/**
	 * Measure the distance between this point and another.
	 * 
	 * @param	p			The other point.
	 * @return				Distance between the points.
	 */
	public final double distance(Point p) {
		final double dx = p.x - this.x;
		final double dy = p.y - this.y;
		return Math.sqrt(dx * dx + dy * dy);
	}


	/**
	 * Create a new point at the given offset from this one.
	 * 
	 * @param	x			X offset to displace by.
	 * @param	y			Y offset to displace by.
	 * @return				A new point offset from this one.
	 */
	public final Point offset(double x, double y) {
		return new Point(this.x + x, this.y + y);
	}


	/**
	 * Create a new point at the given offset from this one.
	 * 
	 * @param	off			Offset to displace by.
	 * @return				A new point offset from this one.
	 */
	public final Point offset(Vector off) {
		return new Point(x + off.x, y + off.y);
	}


	/**
	 * Create a scaled version of this point according to the given scale
	 * factor.  The scale is applied about the co-ordinate origin; so the
	 * returned point will in general be moved as well as scaled.
	 * 
	 * @param	sf			Scale factor.
	 * @return				A new point which is a scaled version of this one.
	 */
	public final Point scale(double sf) {
		return new Point(x * sf, y * sf);
	}
	

	// ******************************************************************** //
	// Utilities.
	// ******************************************************************** //

	@Override
	public String toString() {
		return new String("" + x + "," + y);
	}
	
	
	// ******************************************************************** //
	// Public Data.
	// ******************************************************************** //
	
	public final double x;
	public final double y;

}

