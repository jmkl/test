
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

import android.graphics.RectF;


/**
 * Class representing a hole in the game board, into which the ball can
 * fall.  This is an immutable class.
 */
final class Hole
	extends Graphic
{

	// ******************************************************************** //
	// Public Types.
	// ******************************************************************** //
	
	/**
	 * The types of hole that exist.
	 */
	public static enum Type {
		BLANK(Graphic.BLANK_HOLE, null, 0),
		LAVA(Graphic.LAVA_ANIM, Action.Type.LOSE, R.string.message_lava),
		PORT(Graphic.PORT_ANIM, Action.Type.TELEPORT, 0),
		EXIT(Graphic.EXIT_ANIM, Action.Type.WIN, 0);
		
		Type(int[] img, Action.Type act, int msgid) {
			imageIds = img;
			actionType = act;
			messageId = msgid;
		}
		
		// Resource ID of the hole's image.
		final int[] imageIds;
		
		// The type of action to take when the ball falls in.
		final Action.Type actionType;
		
		// The resource ID of the message to display when the ball falls in.
		// Zero for no message.
		final int messageId;
	}

	
	// ******************************************************************** //
	// Constructor.
	// ******************************************************************** //

	/**
	 * Create a hole.
	 * 
	 * @param	app				Application context.  This provides
	 * 							access to resources and image loading.
	 * @param	type			The type of this hole.
	 * @param	x				The X position.
	 * @param	y				The Y position.
	 * @param	ref				Referenced item, if any.
	 * @param	xform			Transform to apply to the raw data.
	 */
	public Hole(Plughole app, Type type, double x, double y, Point ref, Matrix xform)
	{
		super(app, type.imageIds,
			  new RectF((float) x - LevelData.HOLE / 2f,
					    (float) y - LevelData.HOLE / 2f,
					    (float) x + LevelData.HOLE / 2f,
					    (float) y + LevelData.HOLE / 2f),
			  xform);

		this.type = type;
		this.target = ref;
		double scale = xform.getScale();
		
		// Calculate the actual geometry of the hole.
		centre = xform.transform(new Point(x, y));
		slopeRadius = SLOPE_RADIUS * scale;
		fallRadius = FALL_RADIUS * scale;
		
		// Create the action for when the ball is partly in the hole.
		this.slopeAction = new Action(Action.Type.ACCEL);
	}


	// ******************************************************************** //
	// Accessors.
	// ******************************************************************** //

	/**
	 * Get the auxilliary level element used to detect the ball falling fully
	 * in to this hole.
	 * 
	 * @return				Trap object which triggers the appropriate action
	 * 						when the ball falls fully in.  Null if there is
	 * 						no action.
	 */
	Poly getCentreTrap() {
		if (type.actionType == null)
			return null;
		
		// Create a trap at the centre to detect when the ball is fully in.
		Action fall = new Action(type.actionType, type.messageId);
		if (type == Type.PORT)
			fall.setTarget(target);
		Poly sink = new Poly(getApp(), centre, fallRadius, fall);
		
		return sink;
	}
	
	
	/**
	 * Get the type of this hole.
	 * 
	 * @return				The type of this hole.
	 */
	public Type getType() {
		return type;
	}


	// ******************************************************************** //
	// Interaction.
	// ******************************************************************** //

	/**
	 * Given a position, check that position against
	 * this hole to see if it is inside.
	 * If so, return the action to take.
	 * 
	 * @param	x			X of the position.
	 * @param	y			Y of the position.
	 * @return				Iff we entered this zone, the required action.
	 * 						Otherwise null.
	 */
	final Action entered(double x, double y) {
		final double dist = centre.distance(x, y);
		if (dist < slopeRadius) {
			double accel = 0;
			
			// Don't accelerate in the centre -- we'll just get flung out.
			if (dist >= fallRadius) {
				double frac = (dist - fallRadius) / (slopeRadius - fallRadius);
				frac = 1 - frac;
				accel = 1 - Math.sqrt(1 - frac * frac);
				
				// Determine the vector of the acceleration.
				double dx = centre.x - x;
				double dy = centre.y - y;
				double dm = Math.sqrt(dx * dx + dy * dy);
				slopeAction.setAccel(dx / dm, dy / dm, accel);
			} else
				slopeAction.setAccel(0, 0, 0);
			
			return slopeAction;
		}
		
		return null;
	}
	

	/**
	 * Get the destination of this ball if it enters this zone.
	 * 
	 * @return				The position the ball moves to after entering.
	 * 						null if there is no effect on ball position.
	 */
	public Point getDest() {
		return centre;
	}

	
    // ******************************************************************** //
    // Class Data.
    // ******************************************************************** //
	
	// The radius of the hole, in unscaled level co-ordinates.
	private static final double SLOPE_RADIUS = 1.3;

	// The radius of the zone within which the ball is fully in, in unscaled
	// level co-ordinates.  This is the distance from the centre of
	// the hole that the centre of the ball must be for the ball to be
	// fully inside the hole.
	private static final double FALL_RADIUS = SLOPE_RADIUS - LevelData.BALL / 2;


	// ******************************************************************** //
	// Private Data.
	// ******************************************************************** //

	// The type of this Hole.
	private final Type type;

	// If this is a PORT hole, the target point.
	private Point target = null;

	// The action to trigger when the ball is partly in this hole.
	private final Action slopeAction;

	// Actual position of this hole in the scaled playing board.
	private final Point centre;
	
	// Radius of the slope zone of the hole.
	private final double slopeRadius;
	
	// Radius of the fall zone of the hole.
	private final double fallRadius;

}

