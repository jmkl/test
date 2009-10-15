
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

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;


/**
 * Class containing all of the data which defines a game level.
 */
class LevelData {

    // ******************************************************************** //
    // Public Classes.
    // ******************************************************************** //

	/**
	 * Meta-data about a level.
	 */
	public static final class Header {
		
		public Header(String n, int g, int d, long t) {
			name = n;
			group = g;
			difficulty = d;
			levelTime = t;
			
			resourceId = 0;
			fileName = null;
		}

		/**
		 * Get the displayed name for this level, including level number.
		 * 
		 * @return				The displayed name for this level.
		 */
		public String getDisplayName() {
			return "" + group + "." + difficulty + ": " + name;
		}
		

		// The name of this level.
		public final String name;
		
		// Difficulty group and number within the group.
		public final int group;
		public final int difficulty;
		
		// The time given to complete this level, in ms.
		public final long levelTime;
		
		// The resource ID of the XML definition; 0 if it didn't come from
		// an XML resource.
		public int resourceId;

		// The name of file the XML definition was read from; null if it
		// didn't come from a file.
		public File fileName;
	}
	
	
    // ******************************************************************** //
    // Public Data.
    // ******************************************************************** //

	/**
	 * LW is the assumed width of the board for the purpose of level layout.
	 * Scale the board data appropriately for the actual screen size.
	 */
	public static final int LW = 24;

	/**
	 * LH is the assumed height of the board for the purpose of level layout.
	 * Scale the board data appropriately for the actual screen size.
	 */
	public static final int LH = 36;

	/**
	 * The assumed diameter of the overall bitmap of a hole.
	 */
	public static final int HOLE = 3;

	/**
	 * The assumed diameter of the ball for the purpose of level layout.
	 * Scale the ball appropriately for the actual screen size.
	 */
	public static final int BALL = 2;

	
    // ******************************************************************** //
    // Configuration.
    // ******************************************************************** //

	/**
	 * Set the ball radius.
	 * 
	 * @param	rad				The ball radius, used to map visible barriers
	 * 							to the ball centre position.
	 */
	static void setBallRadius(double rad) {
		ballRadius = rad;
	}
	
	
    // ******************************************************************** //
    // Constructor.
    // ******************************************************************** //
	
	/**
	 * Create blank level data.
	 */
	LevelData() {
		this.pointItems = new ArrayList<Point>();
		this.fixedItems = new ArrayList<Element>();
		this.animItems = new ArrayList<Element>();
		this.zoneItems = new ArrayList<Hole>();
		this.wallItems = new ArrayList<Poly>();
		this.idMap = new HashMap<String, Object>();
	}


    // ******************************************************************** //
    // Level Building.
    // ******************************************************************** //
	
	/**
	 * Set the level header data.
	 * 
	 * @param	header		Level header data.
	 */
	public void setHeader(Header header) {
		this.header = header;
	}
	
	
	/**
	 * Set the level start position.
	 * 
	 * @param	start		Level start position.
	 */
	public void setStart(Point start) {
		startPos = start;
	}
	

	/**
	 * Add a non-animated background item to this level.
	 * 
	 * @param	item		The element to add.
	 * @param	id			The ID of the element, or null.
	 */
	public void addBackground(Element item, String id) {
		fixedItems.add(item);
		if (id != null)
			idMap.put(id, item);
	}
	

	/**
	 * Add a Point -- e.g. a teleport target -- to this level.
	 * 
	 * @param	item		The element to add.
	 * @param	id			The ID of the element, or null.
	 */
	public void addPoint(Point item, String id) {
		pointItems.add(item);
		if (id != null)
			idMap.put(id, item);
	}
	

	/**
	 * Add an animated background item to this level.
	 * 
	 * @param	item		The element to add.
	 * @param	id			The ID of the element, or null.
	 */
	public void addAnim(Element item, String id) {
		animItems.add(item);
		if (id != null)
			idMap.put(id, item);
	}
	

	/**
	 * Add an active zone item to this level.
	 * 
	 * @param	item		The element to add.
	 * @param	id			The ID of the element, or null.
	 */
	public void addZone(Hole item, String id) {
		zoneItems.add(item);
		if (id != null)
			idMap.put(id, item);
	}
	

	/**
	 * Add a physical barrier defined by an abstract shape to this level.
	 * 
	 * @param	item		The element to add; this item is assumed to
	 * 						be the actual shape which the centre of the
	 * 						ball is constrained by, so it doesn't need
	 * 						to be grown.
	 * @param	id			The ID of the element, or null.
	 */
	public void addWall(Poly item, String id) {
		wallItems.add(item);
		if (id != null)
			idMap.put(id, item);
	}
	

	/**
	 * Add a physical barrier defined by a visible shape to this level.
	 * 
	 * @param	item		The element to add.  Since this item represents
	 * 						the visible appearance, and since we model the
	 * 						ball by its centre position, we need to grow
	 * 						this item by the ball's radius.
	 * @param	id			The ID of the element, or null.
	 */
	public void addBarrier(Poly item, String id) {
		wallItems.add(item.createLarger(ballRadius));
		if (id != null)
			idMap.put(id, item);
	}
	

    // ******************************************************************** //
    // Accessors.
    // ******************************************************************** //

	/**
	 * Get the displayed name for this level, including level number.
	 * 
	 * @return				The displayed name for this level.
	 */
	public String getDisplayName() {
		return header.getDisplayName();
	}
	

	/**
	 * Get the level time.
	 * 
	 * @return				The time to complete in ms.
	 */
	public long getTime() {
		return header.levelTime;
	}
	

	/**
	 * Get the start position of this level.
	 * 
	 * @return				The start position of this level.  null if not set.
	 */
	public Point getStart() {
		return startPos;
	}
	
	
	/**
	 * Get a level element by ID.
	 * 
	 * @param	id			The ID of the desired item.
	 * @return				The item identified by id, or null if not found.
	 */
	public Object getById(String id) {
		return (Object) idMap.get(id);
	}
	

	/**
	 * Get the walls in this level.
	 * 
	 * @return				An iterator over all the walls in this level.
	 */
	public Iterator<Poly> getWalls() {
		return wallItems.iterator();
	}
	

	/**
	 * Get the background items in this level.
	 * 
	 * @return				List of all background items in this level.
	 */
	public ArrayList<Element> getBackground() {
		return fixedItems;
	}
	

	/**
	 * Get the animated items in this level.
	 * 
	 * @return				List of all animated items in this level.
	 */
	public ArrayList<Element> getAnims() {
		return animItems;
	}
	

	/**
	 * Get the special zone items in this level.
	 * 
	 * @return				List of all special zones in this level.
	 */
	public ArrayList<Hole> getZones() {
		return zoneItems;
	}
	

    // ******************************************************************** //
    // Class Data.
    // ******************************************************************** //

	// The ball radius, used to map visible barriers to the ball
	// centre position.
	private static double ballRadius = 0;

	
    // ******************************************************************** //
    // Private Data.
    // ******************************************************************** //

	// Level meta-data.
	private Header header;

	// Start position of this level.
	private Point startPos = null;
	
	// The points (teleport targets etc.) in this level.
	private final ArrayList<Point> pointItems;
	
	// The fixed items in this level.
	private final ArrayList<Element> fixedItems;

	// The animated items in this level.
	private final ArrayList<Element> animItems;

	// The zones in this level.
	private final ArrayList<Hole> zoneItems;
	
	// The items in this level that the ball bounces off.
	private final ArrayList<Poly> wallItems;

	// A Map of all the items in this level which have IDs set.  This is
	// used so they can be referred to by name.
	private HashMap<String, Object> idMap;

}

