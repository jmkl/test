
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


import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.hermit.plughole.LevelReader.LevelException;
import org.xmlpull.v1.XmlPullParser;


/**
 * Class containing all of the data which defines a game level.
 */
class LevelData
    extends Element
{

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

    /**
     * Default wall colour.
     */
    public static final int WALL_COLOR = 0xffa0a0a0;

	
    // ******************************************************************** //
    // Constructor.
    // ******************************************************************** //
	
	/**
	 * Create blank level data.
     * 
     * @param   app         Application context.
     * @param   name        The name of this level.
     * @param   group       Difficulty group.
     * @param   diff        Level number within the difficulty group.
     * @param   time        Time allowed for the level, in seconds.
     * @param   xform       Transform to apply to the raw data.
	 */
	LevelData(Plughole app, String name,
	          int group, int diff, long time, Matrix xform)
	{
        super(app, null);
        
        this.header = new Header(name, group, diff, time);

		this.locationItems = new ArrayList<Location>();
		this.fixedItems = new LinkedList<Visual>();
		this.animItems = new ArrayList<Visual>();
		this.zoneItems = new ArrayList<Hole>();
		this.wallItems = new ArrayList<Poly>();
        this.triggerItems = new ArrayList<Poly>();
		this.idMap = new HashMap<String, Element>();
	}


    // ******************************************************************** //
    // Level Building.
    // ******************************************************************** //
    
    /**
     * Add a child to this element.  This is used during level parsing.
     * 
     * @param   p           The parser the level is being read from.
     * @param   tag         The name of this item's XML tag.
     * @param   child       The child to add to this element.
     * @return              true iff this child has been accepted.  If
     *                      false, the child is actually a sibling; it
     *                      has not been added here, and needs to be
     *                      added to the parent.
     */
    @Override
    boolean addChild(XmlPullParser p, String tag, Object child)
        throws LevelException
    {
        if (child instanceof Element) {
            Element elem = ((Element) child);
            String id = elem.getId();
            if (id != null && !idMap.containsKey(id))
                idMap.put(id, elem);
            
            elem.resolveRefs(p, idMap);
        }
        
        if (child instanceof Location) {
            Location loc = (Location) child;
            switch (loc.type) {
            case START:
                startPos = loc;
                break;
            case TARGET:
                locationItems.add(loc);
                break;
            }
            return true;
        } else if (child instanceof Poly) {
            Poly poly = (Poly) child;
            if (poly instanceof Hole)
                zoneItems.add((Hole) poly);
            // If a polygon is drawn, add it to the start of the list so
            // children draw after it.
            if (poly.isDrawn())
                fixedItems.addFirst(poly);
            if (poly.isWall())
                wallItems.add(poly);    // Handles ONBOUNCE as well.
            if (poly.getActions(Action.Trigger.ONCROSS) != null)
                triggerItems.add(poly);
            return true;
        } else if (child instanceof Graphic) {
            fixedItems.add((Graphic) child);
            return true;
        } else if (child instanceof Anim) {
            animItems.add((Anim) child);
            return true;
        } else if (child instanceof Text) {
            fixedItems.add((Text) child);
            return true;
        }

        throw new LevelException(p, "element <" + p.getName() +
                                    "> not permitted in <" + tag + ">");
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
	public Location getStart() {
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
     * Get the trigger elements in this level.
     * 
     * @return              An iterator over all the trigger elements
     *                      in this level.
     */
    public Iterator<Poly> getTriggers() {
        return triggerItems.iterator();
    }
    

	/**
	 * Get the background items in this level.
	 * 
	 * @return				List of all background items in this level.
	 */
	public List<Visual> getBackground() {
		return fixedItems;
	}
	

	/**
	 * Get the animated items in this level.
	 * 
	 * @return				List of all animated items in this level.
	 */
	public List<Visual> getAnims() {
		return animItems;
	}
	

	/**
	 * Get the special zone items in this level.
	 * 
	 * @return				List of all special zones in this level.
	 */
	public List<Hole> getZones() {
		return zoneItems;
	}
	

    // ******************************************************************** //
    // Private Data.
    // ******************************************************************** //

	// Level meta-data.
	private Header header;

	// Start position of this level.
	private Location startPos = null;
	
	// The locations (teleport targets etc.) in this level.
	private final ArrayList<Location> locationItems;
	
	// The fixed items in this level.
	private final LinkedList<Visual> fixedItems;

	// The animated items in this level.
	private final ArrayList<Visual> animItems;

	// The zones in this level.
	private final ArrayList<Hole> zoneItems;
	
	// The items in this level that the ball bounces off.
	private final ArrayList<Poly> wallItems;
    
    // The items in this level that just trigger actions.
    private final ArrayList<Poly> triggerItems;

	// A Map of all the items in this level which have IDs set.  This is
	// used so they can be referred to by name.
	private HashMap<String, Element> idMap;

}

