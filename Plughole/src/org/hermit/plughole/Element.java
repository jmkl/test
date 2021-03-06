
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


import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

import net.goui.util.MTRandom;

import org.hermit.plughole.LevelReader.LevelException;
import org.xmlpull.v1.XmlPullParser;


/**
 * Class representing an element of the game board.  This abstract class
 * defines methods for building the hierarchy of elements, and convenience
 * methods for use in derived classes, such as random numbers.
 */
abstract class Element
{
	
	// ******************************************************************** //
	// Constructor.
	// ******************************************************************** //

	/**
	 * Create a non-rectangular element.
	 * 
	 * @param	app			Application context.
	 * @param	id			The id of this element.
	 * @param	xform		The transformation that needs to be applied
	 * 						to the element to map it to screen co-ordinates.
	 */
	Element(Plughole app, String id) {
        appContext = app;
        elemId = id;
	}

	
	// ******************************************************************** //
	// Construction.
	// ******************************************************************** //
	
	/**
	 * Add a child to this element.  This is used during level parsing.
	 * 
	 * <p>The default implementation just rejects children.  Subclasses
	 * must override this if they expect children.
	 * 
	 * @param	p			The parser the level is being read from.
	 * @param	tag 		The name of this child's XML tag.
	 * @param	child		The child to add to this element.
	 * @return				true iff this child has been accepted.  If
	 * 						false, the child is actually a sibling; it
	 * 						has not been added here, and needs to be
	 * 						added to the parent.
     * @throws LevelException 
	 */
	boolean addChild(XmlPullParser p, String tag, Object child)
		throws LevelException
	{
		throw new LevelException(p, "<" + tag + "> does not take children");
	}
	
	
	/**
	 * Add an action to this element.  This is used during level parsing.
	 * 
	 * @param	act 		The action to add.
     * @throws LevelException 
	 */
	void addAction(Action act)
		throws LevelException
	{
	    if (buildingActions == null)
	         buildingActions = new HashMap<Action.Trigger, ArrayList<Action>>();
	    
	    Action.Trigger trig = act.getTrigger();
	    if (!buildingActions.containsKey(trig))
	        buildingActions.put(trig, new ArrayList<Action>());
	    
	    ArrayList<Action> alist = buildingActions.get(trig);
	    alist.add(act);
	}
	
    
    /**
     * Finalize all object references within this object.
     * 
     * @param   p           The parser the level is being read from.
     * @param   map         Mapping of object IDs to objects.
     * @throws LevelException 
     */
    void resolveRefs(XmlPullParser p, HashMap<String, Element> map)
        throws LevelException
    {
        if (buildingActions != null) {
            for (Action.Trigger trig : Action.Trigger.VALUES) {
                ArrayList<Action> alist = buildingActions.get(trig);
                if (alist != null)
                    for (Action act : alist)
                        act.resolveRefs(p, map);
            }
        }
    }


	/**
	 * We're finished adding children; do any required initialization.
	 * 
	 * <p>The default implementation does nothing.  Subclasses
	 * can override this if they wish. Subclass implementations <b>must</b>
	 * call through to this base implementation as their first action.
     * 
     * @param   p           The parser the level is being read from.
     * @throws LevelException 
	 */
	void finished(XmlPullParser p) throws LevelException {
	}
	
	
	// ******************************************************************** //
	// Accessors.
	// ******************************************************************** //

	/**
	 * Get the application context.
	 * 
	 * @return             Our application context.
	 */
	protected final Plughole getApp() {
		return appContext;
	}
	

	/**
	 * Get the ID of this element.
	 * 
	 * @return              The ID; null if none is set.
	 */
	protected final String getId() {
		return elemId;
	}
	

    /**
     * Get the actions associated with a particular trigger for this element.
     * 
     * @param   trig        The trigger we want the actions for.
     * @return              The actions attached to that trigger; null if none.
     */
    protected final Action[] getActions(Action.Trigger trig) {
        if (buildingActions == null)
            return null;
        ArrayList<Action> alist = buildingActions.get(trig);
        if (alist == null)
            return null;
        
        Action[] arr = new Action[alist.size()];
        alist.toArray(arr);
        return arr;
    }
    

    // ******************************************************************** //
    // State Control.
    // ******************************************************************** //

    /**
     * Get the enable state of this item.
     * 
     * @return            Current enablement state.
     */
    boolean getEnable() {
        return isEnabled;
    }
    

    /**
     * Enable or disable this item.  The effects of this depend on the
     * type of element.
     * 
     * <p>Subclasses may override this, but must call through to this method.
     * 
     * @param  enable      New enablement state.
     */
    void setEnable(boolean enable) {
        isEnabled = enable;
    }
    

	// ******************************************************************** //
	// Utility Methods.
	// ******************************************************************** //

	/**
	 * Get a random integer between 0 (inclusive) and the specified value
	 * (exclusive).
	 * 
	 * @param	range		The bound on the random number to be returned.
	 * @return				An int value between 0 (inclusive) and range
	 * 						(exclusive).
	 */
	protected final int rndInt(int range) {
		return rnd.nextInt(range);
	}
	
	
	// ******************************************************************** //
	// Class Data.
	// ******************************************************************** //

	// Random number generator.
	private static final Random rnd = new MTRandom();

	
	// ******************************************************************** //
	// Private Data.
	// ******************************************************************** //
	
	// Our application.
	private final Plughole appContext;

	// The ID of this element.
	private final String elemId;

    // List of OnCross actions this element has.  Null if none.
	// Used in element construction.
    private HashMap<Action.Trigger, ArrayList<Action>> buildingActions = null;
    
    // Current enable flag.
    private boolean isEnabled = true;

}

