
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
 * Class representing a location on the board.  This could be the start,
 * or a teleport target, or some other special point.  This is an
 * immutable class.
 */
final class Location
{
	
	// ******************************************************************** //
	// Public Types.
	// ******************************************************************** //

	public enum Type {
		START, TARGET;
	}
	
	
	// ******************************************************************** //
	// Constructor.
	// ******************************************************************** //
	
	/**
	 * Create a point at the given X and Y.
	 * 
	 * @param	id 			The ID of this location.  May be null.
	 * @param	type		What kind of location this is.
	 * @param	where		It's position.
	 */
	public Location(String id, Type type, Point where) {
		this.id = id;
		this.type = type;
		this.where = where;
	}


	// ******************************************************************** //
	// Accessors.
	// ******************************************************************** //

	/**
	 * Get the X co-ordinate.
	 * 
	 * @return             This location's X co-ordinate.
	 */
	public double getX() {
	    return where.x;
	}


    /**
     * Get the Y co-ordinate.
     * 
     * @return             This location's Y co-ordinate.
     */
    public double getY() {
        return where.y;
    }

	
    // ******************************************************************** //
    // Utilities.
    // ******************************************************************** //

	@Override
	public String toString() {
		return new String("" + type + ":" + where);
	}
	
	
	// ******************************************************************** //
	// Public Data.
	// ******************************************************************** //
	
	public final String id;
	public final Type type;
	public final Point where;

}

