
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
 * Class representing a hole in the game board, into which the ball can
 * fall.  This is an immutable class.
 */
final class Hole
	extends Poly
{
	
	// ******************************************************************** //
	// Constructor.
	// ******************************************************************** //

	/**
	 * Create a hole.
	 * 
     * @param   app         Application context.
     * @param   id          The ID of this element.
	 * @param	x			The X position.
	 * @param	y			The Y position.
	 * @param	xform		Transform to apply to the raw data.
	 */
	public Hole(Plughole app, String id, double x, double y, Matrix xform) {
        super(app, id, new Point(x, y), FALL_RADIUS,
                new RectF((float) x - LevelData.HOLE / 2f,
                          (float) y - LevelData.HOLE / 2f,
                          (float) x + LevelData.HOLE / 2f,
                          (float) y + LevelData.HOLE / 2f), xform);

		double scale = xform.getScale();
		
		// Calculate the actual geometry of the hole.
		centre = xform.transform(x, y);
		slopeRadius = SLOPE_RADIUS * scale;
		fallRadius = FALL_RADIUS * scale;
		
		// Create the action for when the ball is partly in the hole.
		this.slopeActions = new Action[] {
		    new Action(Action.Trigger.WHILEZONE, Action.Type.ACCEL)
		};
	}


	// ******************************************************************** //
	// Interaction.
	// ******************************************************************** //

	/**
	 * Given a position, check that position against this hole to see if
	 * it is inside.  If so, return the action to take.
	 * 
	 * @param	x			X of the position.
	 * @param	y			Y of the position.
	 * @return				Iff we entered this zone, the required action.
	 * 						Otherwise null.
	 */
	public final Action[] entered(double x, double y) {
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
				slopeActions[0].setAccel(dx / dm, dy / dm, accel);
			} else
				slopeActions[0].setAccel(0, 0, 0);
			
			return slopeActions;
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
    // Drawing.
    // ******************************************************************** //

    /**
     * Draw this hole onto the given canvas.
     * 
     * @param   canvas          Canvas to draw on.
     * @param   time            Total level time in ms.  A time of zero
     *                          indicates that we're drawing statically,
     *                          not in the game loop.
     * @param   clock           Level time remaining in ms.
     */
    @Override
    protected void draw(Canvas canvas, long time, long clock) {
        // Nothing to do... all drawing is by attached Graphic objects.
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

	// The actions to trigger when the ball is partly in this hole.
	private final Action[] slopeActions;

	// Actual position of this hole in the scaled playing board.
	private final Point centre;
	
	// Radius of the slope zone of the hole.
	private final double slopeRadius;
	
	// Radius of the fall zone of the hole.
	private final double fallRadius;

}

