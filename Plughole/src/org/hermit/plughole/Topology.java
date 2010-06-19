
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

import java.util.ArrayList;
import java.util.Iterator;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.util.Log;


/**
 * Class representing the physical topology of the playing board.  This
 * contains all of the physical objects that clutter up the board, and
 * has code to calculate bounces off those objects.
 */
class Topology {
	
	// ******************************************************************** //
    // Public Classes.
    // ******************************************************************** //
	
	/**
	 * This class contains the result of an intersect calculation.
	 * This is a mutable class -- for efficiency, we compute multiple
	 * passes of calculation in the same object.
	 */
	public static final class Intersect {

		/**
		 * The Line we hit.
		 */
		public Action action = null;

		/**
		 * Fraction (0 .. 1) along the motion vector where
		 * the intersect occurs.
		 */
		public double fraction = 0;
	
		/**
		 * Angle between the motion vector and base, 0 .. 90 degrees
		 */
		public double angle = 0;
		
		/**
		 * The reflection point X.
		 */
		public double interX = 0;
		
		/**
		 * The reflection point Y.
		 */
		public double interY = 0;
		
		/**
		 * The reflected motion end point X.
		 */
		public double endX = 0;
		
		/**
		 * The reflected motion end point X.
		 */
		public double endY = 0;
		
		/**
		 * The reflected motion direction unit vector X.
		 */
		public double directionX = 0;
		
		/**
		 * The reflected motion direction unit vector Y.
		 */
		public double directionY = 0;
		
		/**
		 * Make this Intersect a copy of the given Intersect.
		 * 
		 * @param	i			Intersect data to copy.
		 */
		public void copy(Intersect i) {
			action = i.action;
			fraction = i.fraction;
			angle = i.angle;
			interX = i.interX;
			interY = i.interY;
			endX = i.endX;
			endY = i.endY;
			directionX = i.directionX;
			directionY = i.directionY;
		}

	}

	
	// ******************************************************************** //
    // Constructor.
    // ******************************************************************** //
	
	/**
	 * Create a Table topology.
	 * 
	 * @param	app				The application context we're running in.
	 * @param	table			The game table.
	 */
	Topology(Plughole app, TableView table) {
		appContext = app;

		// Make some working variables.
		currReflect = new Intersect();
	}

	
    // ******************************************************************** //
    // Configuration Control.
    // ******************************************************************** //
	
	/**
	 * Set the surface size we're playing in.  Scale all topology data
	 * accordingly.
	 * 
	 * @param	width		New width of the playing area.
	 * @param	height		New height of the playing area.
	 * @throws LevelException Problem reloading the level.
	 */
	public void setTableSize(int width, int height)
		throws LevelReader.LevelException
	{
		// Make sure we've really got a size.
		if (width < 1 || height < 1)
			return;
		
		// If no change, forget it.
		if (width == currentWidth && height == currentHeight)
			return;
		
		Log.i(TAG, "topo set size " + width + "," + height);
		currentWidth = width;
		currentHeight = height;
	}


	/**
	 * Set the current level layout according to the given level data.
	 * 
	 * @param	level			The level to load.
	 * @throws LevelException Problem loading the level.
	 */
	public void setLevel(LevelData level)
		throws LevelReader.LevelException
	{
		// If we don't have a table size, can't load the level data.
		if (currentWidth <= 0 || currentHeight <= 0)
			throw new LevelReader.LevelException("can't load level yet");
		
		currentLevel = level;
		Matrix xform = getTransform();

		// Load the ball image.
		int size = (int) Math.round(LevelData.BALL * xform.getScale());
		ballImage = appContext.getScaledBitmap(R.drawable.ball, size, size);

		// Cache the values.
		fixedItems = currentLevel.getBackground();
		animItems = currentLevel.getAnims();
		zones = currentLevel.getZones();

		// Now create the lines that the ball actually bounces off.
		// For every visible polygon, grow it to get the real polygon that
		// we bounce the centre of the ball off.
		lines = new ArrayList<Line>();
		Iterator<Poly> walls = currentLevel.getWalls();
		while (walls.hasNext()) {
			Poly p = walls.next();
			for (Line l : p.getLines())
				lines.add(l);
		}
	}
	

	/**
	 * Get the transform which will convert a loaded level to the current
	 * screen's size and orientation.
	 */
	Matrix getTransform() {
		int lw = LevelData.LW;
		int lh = LevelData.LH;
		
		// If we have a portrait level and a landscape screen, then
		// rotate the level so we can fill as much of the screen as possible.
		boolean screenPort = currentHeight > currentWidth;
		boolean levelPort = lh > lw;
		
		// Rotation angle in degrees clockwise, and offsets in X and Y.
		Matrix xform = new Matrix();
		
		// To keep things consistent when switching screen rotation, always
		// go clockwise from landscape to portrait, and anti the other way.
		if (!levelPort && screenPort) {
			xform.translate(currentWidth, 0);
			xform.rotate(Matrix.ORotate.RIGHT);
			lw = LevelData.LH;
			lh = LevelData.LW;
		} else if (levelPort && !screenPort) {
			xform.translate(0, currentHeight);
			xform.rotate(Matrix.ORotate.LEFT);
			lw = LevelData.LH;
			lh = LevelData.LW;
		}

		// Now figure out the scale factor.  We want to scale the level as
		// big as possible while still fitting it all in.
		final double wscale = (float) currentWidth / (float) lw;
		final double hscale = (float) currentHeight / (float) lh;
		xform.scale(wscale < hscale ? wscale : hscale);
		Log.i(TAG, "topo import: rot " + xform.getRotation() +
									" scale " + xform.getScale());
		
		return xform;
	}
	
	
    // ******************************************************************** //
    // Accessors.
    // ******************************************************************** //

	LevelData getLevel() {
		return currentLevel;
	}

	
	/**
	 * Get the image for the current level's ball.
	 * 
	 * @return				The current level's ball image.
	 */
	public Bitmap getBallImage() {
		return ballImage;
	}
	
	
    // ******************************************************************** //
    // Zone Entries.
    // ******************************************************************** //
	
	/**
	 * Given a position, check that position against all the special
	 * zones (holes etc.) in this topology to see if it is inside any.
	 * If so, return the action defined by the zone.
	 * 
	 * @param	x			X of the position.
	 * @param	y			Y of the position.
	 * @param	res			Action to be set to the required action, if
	 * 						the position is in a zone.
	 * @return				Iff we entered a zone, the action to take.
	 * 						Otherwise null.
	 */
	final Action zone(double x, double y) {
		// Search every hole looking for hits.
		Action act;
		for (Hole zone : zones)
			if ((act = zone.entered(x, y)) != null)
				return act;

		return null;
	}


    // ******************************************************************** //
    // Object Collisions.
    // ******************************************************************** //

	/**
	 * Determine the intersection -- if any -- between the given line segment
	 * representing a motion, and each of the line segments in this topology.
	 * 
	 * @param	sx			Start X of the motion.
	 * @param	sy			Start Y of the motion.
	 * @param	ex			End X of the motion.
	 * @param	ey			End Y of the motion.
	 * @param	reflectOut	User-supplied object where we place the
	 * 						calculated reflection data.
	 * @return				True iff there is a clear intersection.
	 */
	final boolean reflect(double sx, double sy, double ex, double ey,
						  Intersect reflectOut)
	{
		// We always search for the closest bounce to the ball's initial
		// position.  The returned distance from reflect is the key.
		double best = -1;

		// Compute the extreme ranges of the input line.
		final double minX = sx < ex ? sx : ex;
		final double minY = sy < ey ? sy : ey;
		final double maxX = sx > ex ? sx : ex;
		final double maxY = sy > ey ? sy : ey;
		
		// Search every line looking for reflections.
		for (Line wall : lines) {
			// First, if the ranges of the wall and motion line don't
			// overlap, forget it.
			if (wall.maxX < minX || wall.maxY < minY ||
				wall.minX > maxX || wall.minY > maxY)
				continue;
			
			boolean r = reflect(wall, sx, sy, ex, ey, currReflect);
			if (r && (best < 0 || currReflect.fraction < best)) {
				reflectOut.copy(currReflect);
				best = reflectOut.fraction;
			}
		}

		return best >= 0;
	}
	
	
	/**
	 * Determine the intersection -- if any -- between two line segments,
	 * one representing a static base (e.g. a wall), and one representing
	 * a motion.  If there is an intersection, calculate the reflection.
	 * 
	 * We return a Point representing where the reflected motion ends, and
	 * a Vector -- which will be a unit vector -- representing the direction
	 * of motion after reflection.  We return this data in user-supplied
	 * structures for efficiency.
	 * 
	 * @param	base		Base line (i.e. the wall).
	 * @param	sx			Start X of the incident line representing motion.
	 * @param	sy			Start Y of the incident line representing motion.
	 * @param	ex			End X of the incident line representing motion.
	 * @param	ey			End Y of the incident line representing motion.
	 * @param	reflectOut	User-supplied object where we place the
	 * 						calculated reflection data.
	 * @return				True iff there is a clear intersection.  Otherwise
	 * 						false, which could mean that the segments are
	 * 						parallel, coincident, or disjoint.
	 */
	static final boolean reflect(Line base,
							     double sx, double sy, double ex, double ey,
							     Intersect reflectOut)
	{
		/*
		 * See: http://local.wasp.uwa.edu.au/~pbourke/geometry/lineline2d/
		 * Notes:
		 *  + The denominators for the equations for ua and ub are the same. 
		 *  + If the denominator for the equations for ua and ub is 0 then the
		 *    two lines are parallel. 
		 *  + If the denominator and numerator for the equations for ua and ub
		 *    are 0 then the two lines are coincident. 
		 */
		final double mDx = ex - sx;
		final double bDx = base.dx;
		final double mbX = sx - base.sx;
		final double mDy = ey - sy;
		final double bDy = base.dy;
		final double mbY = sy - base.sy;

		final double denom =  bDy * mDx - bDx * mDy;
		if (denom == 0)
			return false;
		final double numB = bDx * mbY - bDy * mbX;
		final double numM = mDx * mbY - mDy * mbX;

		final double fracB = numM / denom;
		final double fracM = numB / denom;

		/*
		 * Test if ua and ub lie between 0 and 1.  Whichever one lies
		 * within that range then the corresponding line segment contains
		 * the intersection point.  If both lie within the range of 0 to
		 * 1 then the intersection point is within both line segments.
		 * Otherwise the segments are disjoint.
		 */
		if (fracB < 0 || fracB > 1 || fracM < 0 || fracM > 1)
			return false;
		
		// Calculate the intersect position.  The base.u[xy] * MICRO_INCR
		// displaces it very slightly away from the wall, to try to avoid
		// fall-throughs.
		final double interX = base.sx + fracB * bDx + base.uy * MICRO_INCR;
		final double interY = base.sy + fracB * bDy - base.ux * MICRO_INCR;

		/*
		 * Calculate the reflection vector R as
		 *     R = M - 2 * B * ( M [dot] B )
		 * where
		 *     R   reflection vector
		 *     B   base (wall) vector scaled to a unit vector
		 *     M   incident (motion) vector
		 */
		
		// Calculate the vector representing the part of the motion
		// beyond the intersect.
		final double ix = mDx * (1 - fracM);
		final double iy = mDy * (1 - fracM);
		
		// Calculate dot(i, n) where n is the base unit normal.
		final double dot = ix * base.ux + iy * base.uy;
		
		// Calculate the reflection r = i - (2 * n * dot(i, n)),
		// and get its magnitude.
		double rx = 2 * base.ux * dot - ix;
		double ry = 2 * base.uy * dot - iy;
		double rMag = Math.sqrt(rx * rx + ry * ry);
		
		// If the reflection is zero-length, we have a problem, because
		// we are stopping in the wall; our next move is unpredictable.
		// So, instead make the reflection a small fraction of the
		// wall's left-hand normal vector.
		if (rMag == 0) {
			rx = base.uy * MICRO_INCR;
			ry = -base.ux * MICRO_INCR;
			rMag = Math.sqrt(rx * rx + ry * ry);
		}
		
		// Reflection unit vector.
		final double rux = rMag == 0 ? 0 : rx / rMag;
		final double ruy = rMag == 0 ? 0 : ry / rMag;
		
		// Calculate dot(ru, bu) where ru is the reflection unit vector and
		// bu is the base unit vector.  Then use this to get the angle
		// between the vectors.
		// We only want the hardness of the intersect, so ignore the
		// direction of the base and reduce it to the range 0 .. 90.
		final double rdot = rux * base.ux + ruy * base.uy;
		double angle = Math.toDegrees(Math.acos(rdot));
		if (angle > 90)
			angle = 180 - angle;

		// Set up the results.
		reflectOut.action = base.getAction();
		reflectOut.fraction = fracM;
		reflectOut.angle = angle;
		reflectOut.interX = interX;
		reflectOut.interY = interY;
		reflectOut.endX = interX + rx;
		reflectOut.endY = interY + ry;
		reflectOut.directionX = rux;
		reflectOut.directionY = ruy;

		return true;
	}


	// ******************************************************************** //
	// Drawing.
	// ******************************************************************** //

	/**
	 * Draw the static view of this topology onto the given canvas.
	 * 
	 * @param	canvas			Canvas to draw on.
	 */
	public void drawFixed(Canvas canvas) {
		// Draw the background colour.
		canvas.drawColor(TABLE_COLOUR);
		
        // Draw all the fixed and animated items in the topology.
		for (Element elem : fixedItems)
			elem.draw(canvas, 0, 0);
		for (Element elem : animItems)
			elem.draw(canvas, 0, 0);
	     
    	// Draw the lines for debug.
//    	android.graphics.Paint lpaint = new android.graphics.Paint();
//    	lpaint.setAntiAlias(true);
//    	lpaint.setColor(0xffff0000);
//    	for (Line l : lines)
//        	canvas.drawLine((float) l.sx, (float) l.sy,
//        					(float) l.ex, (float) l.ey, lpaint);
	}
	

	/**
	 * Draw the animated elements of this topology onto the given canvas.
	 * 
	 * @param	canvas			Canvas to draw on.
	 * @param	time			Total level time in ms.
	 * @param	clock			Level time remaining in ms.
	 */
	public void drawAnim(Canvas canvas, long time, long clock) {
        // Draw all the animated items in the topology.
		for (Element elem : animItems)
			elem.draw(canvas, time, clock);
	}
	

    // ******************************************************************** //
    // Class Data.
    // ******************************************************************** //

    // Debugging tag.
	private static final String TAG = "plughole";

	// A miniscule increment to an object's motion.  We use this to correct
	// some zeros that appear.
	private static final double MICRO_INCR = 0.0000001f;
	
	// The colour of the table background.
	private static final int TABLE_COLOUR = 0xff393c39;

	
    // ******************************************************************** //
    // Private Data.
    // ******************************************************************** //

	// Application we're running in.
	private Plughole appContext;

	// Current size of the playing area in pixels.
    private int currentWidth = 0;
	private int currentHeight = 0;

	// Current level data.  null if we haven't loaded a level.
	private LevelData currentLevel = null;

	// The fixed items that make up the scene.
	private ArrayList<Element> fixedItems = null;

	// The animated items in the scene.
	private ArrayList<Element> animItems = null;

	// The zones in the scene.
	private ArrayList<Hole> zones = null;

	// The lines that we use for collisions in the scene, by bouncing
	// the centre of the ball off them.  These are derived by growing
	// the wallItems.
	private ArrayList<Line> lines = null;

	// The ball image.
	private Bitmap ballImage = null;

	// Working variables used during bounce calculation.  Allocating them
	// once is significantly faster.
	private Intersect currReflect = null;

}

