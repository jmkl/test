
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

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;


/**
 * Class representing a polygon.  This is designed to represent objects in
 * the level that the ball interacts with.
 */
class Poly
	extends Element
{

	// ******************************************************************** //
	// Constructor.
	// ******************************************************************** //
	
	/**
	 * Create a polygon from the given points array.
	 * 
	 * @param	app				Application context.
	 * @param	points			Array of points representing the polygon,
	 * 							assumed to be in clockwise order.
	 */
	public Poly(Plughole app, ArrayList<Point> points) {
		this(app, points, false, null);
	}
	
	
	/**
	 * Create a polygon from the given points array.
	 * 
	 * @param	app				Application context.
	 * @param	points			Array of points representing the polygon,
	 * 							assumed to be in clockwise order.
	 * @param	action			Action to trigger if the ball hits this polygon.
	 */
	public Poly(Plughole app, ArrayList<Point> points, Action action) {
		this(app, points, false, action);
	}
	
	
	/**
	 * Create a polygon from the given points array.
	 * 
	 * @param	app				Application context.
	 * @param	points			Array of points representing the polygon,
	 * 							assumed to be in clockwise order.
	 * @param	inverted		If true, the polygon is inverted -- the
	 * 							outside becomes the inside and vice versa.
	 * @param	action			Action to trigger if the ball hits this polygon.
	 */
	public Poly(Plughole app, ArrayList<Point> points,
				boolean inverted, Action action)
	{
		super(app, action);
		
		init(points, inverted);
	}

	
	/**
	 * Create a new Poly representing a circle as a set of line segments.
	 * 
	 * @param	app				Application context.
	 * @param	centre			Centre point of the circle.
	 * @param	r				Radius of the circle.
	 */
	public Poly(Plughole app, Point centre, double r) {
		this(app, centre, r, null);
	}

	
	/**
	 * Create a new Poly representing a circle as a set of line segments.
	 * 
	 * @param	app				Application context.
	 * @param	centre			Centre point of the circle.
	 * @param	r				Radius of the circle.
	 * @param	action			Action to trigger if the ball hits this polygon.
	 */
	public Poly(Plughole app, Point centre, double r, Action action) {
		super(app, action);

		ArrayList<Point> points = new ArrayList<Point>(360 / CORNER_SEG);

		// Compute a vector representing three o'clock.
		Vector v1 = new Vector(r, 0);

		// Create points around the circle.
		for (int i = 0; i < 360 / CORNER_SEG; ++i) {
			points.add(centre.offset(v1));
			v1 = v1.rotate(CORNER_SEG);
		}

		// And make the points into a new polygon.
		init(points, inverted);
	}

	
	/**
	 * Set up this polygon from the given points array.
	 * 
	 * @param	app				Application context.
	 * @param	points			Array of points representing the polygon,
	 * 							assumed to be in clockwise order.
	 * @param	inverted		If true, the polygon is inverted -- the
	 * 							outside becomes the inside and vice versa.
	 */
	private void init(ArrayList<Point> points, boolean inverted) {
		this.inverted = inverted;
		
		// Convert the points list into a lines list.  This is how
		// we represent the poly internally.
		final int npoints = points.size();
		this.lines = new Line[npoints];
		if (inverted) {
			// TODO: ?
		} else {
			Action act = getAction();
			for (int i = 0; i < npoints; ++i) {
				int j = i + 1;
				if (j >= npoints)
					j = 0;
				lines[i] = new Line(points.get(i), points.get(j), act);
			}
		}
	}
	

    // ******************************************************************** //
    // Transformations.
    // ******************************************************************** //

	/**
	 * Create a new Poly which is a larger version of this one.
	 * 
	 * Each line which makes up the poly is moved outwards -- i.e. to the
	 * left -- by a specified distance, in a direction perpendicular
	 * to itself.  A new polygon is constructed from these lines.  Convex
	 * corners are rounded off using multiple line segments.
	 * 
	 * This object is not altered.
	 * 
	 * @param	dist			Distance to grow by.
	 * @return					The new, grown polygon.
	 */
	Poly createLarger(double dist) {
		// Create the outward-displaced versions of all our lines.
		Line[] grown = new Line[lines.length];
		for (int i = 0; i < lines.length; ++i)
			grown[i] = lines[i].moveLeft(dist);

		// Create the corners between all pairs of the new, moved lines.
		// These corners become the boundary of the new polygon.
		ArrayList<Point> npoints = new ArrayList<Point>(grown.length * 4);
		for (int i = 0; i < grown.length; ++i) {
			int j = i + 1;
			if (j >= grown.length)
				j = 0;
			makeCorner(lines[i].getEnd(), grown[i], grown[j], npoints);
		}

		// And make the intersects into a new polygon.
		return new Poly(getApp(), npoints, getAction());
	}

	
	/**
	 * Make a "rounded" corner between the two given line segments.  If
	 * the corner is concave, these segments intersect; if it's convex,
	 * then we need to make a rounding.  Do so by adding new points to 
	 * the given points list.
	 * 
	 * @param	centre			The centre point of the corner.
	 * @param	l1				First line segment.
	 * @param	l2				Second line segment.
	 * @param	points			Points list -- we will add new points to
	 * 							this as required to make the corner.
	 */
	private void makeCorner(Point centre, Line l1, Line l2,
						    ArrayList<Point> points)
	{
		// Compute the vectors from the centre point to the two line ends.
		Vector v1 = new Vector(centre, l1.getEnd());
		Vector v2 = new Vector(centre, l2.getStart());
		
		// Calculate the angle between the two vectors.
		// Bear in mind the angles are zero towards positive X, positive
		// towards positive Y -- WHICH IS DOWN THE SCREEN -- and negative
		// towards negative Y.
		double turn = v2.angle(v1);
		
		// If the angle is zero, I'm not turning and don't need another
		// point.
		if (turn == 0)
			return;
		
		// If the angle is less than 0, then I'm turning left -- i.e.
		// this is a concave corner.  The corner point is just the
		// intersection of the two lines.
		if (turn < 0) {
			Point np = Line.intersect(l1, l2);
			points.add(np);
			return;
		}
		
		// Otherwise, I'm turning right.   We need to fill in the corner.
		// Use a line segment.
		for (;;) {
			points.add(centre.offset(v1));
			v1 = v1.rotate(CORNER_SEG);
			if (v2.angle(v1) < 0)
				break;
		}
	}
	
	
	/**
	 * Get the lines which make up this polygon.
	 * 
	 * @return				The lines which make up this polygon.
	 */
	Line[] getLines() {
		return lines;
	}


	// ******************************************************************** //
	// Drawing.
	// ******************************************************************** //
	
	/**
	 * Create a graphics Path representing this polygon.  This is used
	 * for drawing it.
	 */
	private void makepath() {
		gfxPath = new Path();
		if (inverted)
			gfxPath.setFillType(Path.FillType.INVERSE_EVEN_ODD);
		
		boolean first = true;
		for (Line l : lines) {
			if (first) {
				gfxPath.moveTo((float) l.sx, (float) l.sy);
				first = false;
			} else
				gfxPath.lineTo((float) l.sx, (float) l.sy);
		}
		gfxPath.close();
	}
	

	/**
	 * Draw this graphic onto the given canvas.
	 * 
	 * @param	canvas			Canvas to draw on.
	 * @param	time			Total level time in ms.  A time of zero
	 * 							indicates that we're drawing statically,
	 * 							not in the game loop.
	 * @param	clock			Level time remaining in ms.
	 */
	@Override
	protected void draw(Canvas canvas, long time, long clock) {
		// Create the graphics path if we haven't yet.
		if (gfxPath == null)
			makepath();
		
		// Clip to the polygon, so when we draw the bevel using thick lines,
		// we don't spread out.
		canvas.save();
		canvas.clipPath(gfxPath);
		
		// Fill the poly.
		polyPaint.setStyle(Paint.Style.FILL);
		polyPaint.setColor(0xffd0d0d0);
		canvas.drawPath(gfxPath, polyPaint);
	
		// Draw a bevelled edge on the outside of the poly.
		polyPaint.setStyle(Paint.Style.STROKE);
		for (int width = 6, color = 0xa0; width > 0; color -= 0x45, width -= 2) {
			polyPaint.setStrokeWidth(width);
			polyPaint.setARGB(0xff, color, color, color);
			canvas.drawPath(gfxPath, polyPaint);
		}
		
		canvas.restore();
	}
	
	
	// ******************************************************************** //
	// Class Data.
	// ******************************************************************** //

    // Debugging tag.
	@SuppressWarnings("unused")
	private static final String TAG = "plughole";
	
	// The granularity of rounded "corners" we create when growing
	// a polygon.  A new point is added every CORNER_SEG degrees.
	private static final int CORNER_SEG = 30;

	// Paint we use for drawing all polygons.
	private static final Paint polyPaint = new Paint();
	static {
		polyPaint.setAntiAlias(true);
	}
	
	
	// ******************************************************************** //
	// Private Data.
	// ******************************************************************** //

	// The points defining the polygon boundary.  These are assumed to
	// go clockwise around the border, so moving from points[0] to
	// points[1], the left side is outside the polygon, right side inside.
	// If inverted is true, however, they go anticlockwise.  In either case,
	// the right side of each line faces the inside -- the solid part.
	// Of course the last point joins to the first.
	private Line[] lines;
	
	// True iff this polygon is inverted -- ie. it defines a hole in
	// the plane, not a solid.
	private boolean inverted;

	// A graphics Path representing this polygon.  This is used for
	// drawing it.  Null if not set up yet.
	private Path gfxPath = null;
	
}

