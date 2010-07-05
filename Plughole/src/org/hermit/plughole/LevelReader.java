
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
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import android.graphics.RectF;
import android.os.Bundle;
import android.util.AttributeSet;
import android.util.Xml;


/**
 * Class containing all of the data which defines the game levels.
 */
class LevelReader {

    // ******************************************************************** //
    // Public Types.
    // ******************************************************************** //
	
	/**
	 * Level data exception.  Used to signal problems in the level data.
	 */
	public static class LevelException extends Exception {
		public LevelException(String s) {
			super("Error while reading level definition: " + s);
		}
		public LevelException(XmlPullParser p, String s) {
			super("Error while reading level definition: " +
				  s + " at line " + p.getLineNumber());
		}
		private static final long serialVersionUID = 7901796892882169753L;
	}

	
    // ******************************************************************** //
    // Constructor.
    // ******************************************************************** //
	
	/**
	 * Create a level reader.
	 * 
	 * @param	app				The application context we're running in.
	 */
	LevelReader(Plughole app) {
		appContext = app;
		resources = app.getResources();
	}


    // ******************************************************************** //
    // Level Read Control.
    // ******************************************************************** //
	
	/**
	 * Read common definitions from the given XML resource.  Transform all the
	 * constructs to fit the playing screen, and save them in this.common.
	 * 
	 * @param	resid			The resource ID of the level definition.
	 * @param	xform			The transformation that needs to be applied
	 * 							to the level to make it fit the screen.
	 * @throws LevelException	Error encountered while reading.
	 */
	void readCommon(int resid, Matrix xform)
		throws LevelException
	{
		XmlResourceParser parser = null;
		try {
			parser = resources.getXml(resid);
			
			// Find the <Common> tag.
			if (!findBlock(parser, "Common"))
				throw new LevelException("Level common data " +
										 " doesn't contain a <Common> tag!");
			
            // Read the level definition.
			common = (LevelData) readItem(parser, xform, null);
		} catch (LevelException e) {
			throw e;
		} catch (XmlPullParserException e) {
			throw new LevelException(parser, "Parser error: " + e.getMessage());
		} catch (IOException e) {
			throw new LevelException(parser, "I/O error: " + e.getMessage());
		} finally {
			parser.close();
		}
	}


	/**
	 * Read a level header from the given XML resource.
	 * 
	 * @param	resid			The resource ID of the level definition.
	 * @return					The level data.
	 * @throws LevelException	Error encountered while reading.
	 */
	LevelData.Header readLevelHead(int resid)
	throws LevelException
	{
		XmlResourceParser parser = null;
		try {
			parser = resources.getXml(resid);
			LevelData.Header header = readLevelHead(parser);
			header.resourceId = resid;
			return header;
		} finally {
			if (parser != null)
				parser.close();
		}
	}


	/**
	 * Read a level header from the given XML file.
	 * 
	 * @param	file			The file containing the level definition.
	 * @return					The level data.
	 * @throws LevelException	Error encountered while reading.
	 */
	LevelData.Header readLevelHead(File file)
		throws LevelException
	{
		FileInputStream stream = null;
		try {
	        XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
	        factory.setNamespaceAware(true);
	        XmlPullParser parser = factory.newPullParser();

	        stream = new FileInputStream(file);
	        parser.setInput(stream, null);

	        LevelData.Header header = readLevelHead(parser);
	        header.fileName = file;
	        return header;
		} catch (FileNotFoundException e) {
			throw new LevelException("File not found: " + e.getMessage());
		} catch (XmlPullParserException e) {
			throw new LevelException("Parser setup error: " + e.getMessage());
		} finally {
			if (stream != null) {
				try {
					stream.close();
				} catch (IOException e) { }
			}
		}
	}


	/**
	 * Read a level header from the given XML parser.
	 * 
	 * @param	resid			The parser to read the level definition from.
	 * @return					The level header data.
	 * @throws LevelException	Error encountered while reading.
	 */
	LevelData.Header readLevelHead(XmlPullParser parser)
		throws LevelException
	{
		try {
			// Find the <Level> tag.
			if (!findBlock(parser, "Level"))
				throw new LevelException("Level specification " +
										 " doesn't contain a <Level> tag!");
			
			// Read the level header.
			LevelData.Header header = buildLevelHeader(parser);
			return header;
		} catch (LevelException e) {
			throw e;
		} catch (XmlPullParserException e) {
			throw new LevelException(parser, "Parser error: " + e.getMessage());
		} catch (IOException e) {
			throw new LevelException(parser, "I/O error: " + e.getMessage());
		} catch (Throwable e) {
			throw new LevelException(parser, "Error " + e.getClass().getName() +
											 ": " + e.getMessage());
		}
	}


	/**
	 * Read the level definition corresponding to the given header, from
	 * the same source the header was read from.  Transform all the
	 * constructs to fit the playing screen, and place the resulting
	 * level data in this object.
	 * 
	 * @param	head			Header of the level definition to load.
	 * @param	xform			The transformation that needs to be applied
	 * 							to the level to make it fit the screen.
	 * @return					The level data.
	 * @throws LevelException	Error encountered while reading.
	 */
	LevelData readLevel(LevelData.Header head, Matrix xform)
		throws LevelException
	{
		if (head.resourceId != 0)
			return readLevel(head.resourceId, xform);
		else if (head.fileName != null)
			return readLevel(head.fileName, xform);
		return null;
	}


	/**
	 * Read a level definition from the given XML resource.  Transform all the
	 * constructs to fit the playing screen, and return the resulting
	 * level definition.
	 * 
	 * @param	resid			The resource ID of the level definition.
	 * @param	xform			The transformation that needs to be applied
	 * 							to the level to make it fit the screen.
	 * @return					The level data.
	 * @throws LevelException	Error encountered while reading.
	 */
	LevelData readLevel(int resid, Matrix xform)
		throws LevelException
	{
		XmlResourceParser parser = null;
		try {
			parser = resources.getXml(resid);
			LevelData data = readLevel(parser, xform);
			// TODO: data.header.resourceId = resid;
			return data;
		} finally {
			if (parser != null)
				parser.close();
		}
	}


	/**
	 * Read a level definition from the given XML file.  Transform all the
	 * constructs to fit the playing screen, and return the resulting
	 * level definition.
	 * 
	 * @param	file			The file containing the level definition.
	 * @param	xform			The transformation that needs to be applied
	 * 							to the level to make it fit the screen.
	 * @return					The level data.
	 * @throws LevelException	Error encountered while reading.
	 */
	LevelData readLevel(File file, Matrix xform)
		throws LevelException
	{
		FileInputStream stream = null;
		try {
	        XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
	        factory.setNamespaceAware(true);
	        XmlPullParser parser = factory.newPullParser();

	        stream = new FileInputStream(file);
	        parser.setInput(stream, null);

	        LevelData level = readLevel(parser, xform);
	        // TODO: header.fileName = file;
	        return level;
		} catch (FileNotFoundException e) {
			throw new LevelException("File not found: " + e.getMessage());
		} catch (XmlPullParserException e) {
			throw new LevelException("Parser setup error: " + e.getMessage());
		} finally {
			if (stream != null) {
				try {
					stream.close();
				} catch (IOException e) { }
			}
		}
	}


	/**
	 * Read a level definition from the given parser.  Transform all the
	 * constructs to fit the playing screen, and return the resulting
	 * level definition.
	 * 
	 * @param	parser			The parser to read the level definition from.
	 * @param	xform			The transformation that needs to be applied
	 * 							to the level to make it fit the screen.
	 * @return					The level data.
	 * @throws LevelException	Error encountered while reading.
	 */
	private LevelData readLevel(XmlPullParser parser, Matrix xform)
		throws LevelException
	{
		try {
			// Find the <Level> tag.
			if (!findBlock(parser, "Level"))
				throw new LevelException("Level specification " +
										 " doesn't contain a <Level> tag!");
			
			// Read the level definition.
			LevelData theLevel = (LevelData) readItem(parser, xform, null);

			// Check that we've got the elements we need.
			if (theLevel.getStart() == null)
				throw new LevelException("Level specification " +
										 " doesn't contain a <Start>!");

			return theLevel;
		} catch (LevelException e) {
			throw e;
		} catch (XmlPullParserException e) {
			throw new LevelException(parser, "Parser error: " + e.getMessage());
		} catch (IOException e) {
			throw new LevelException(parser, "I/O error: " + e.getMessage());
		}
	}


    // ******************************************************************** //
    // Level Parsing.
    // ******************************************************************** //
	
	/**
	 * Read the given XML parser up to the beginning of the named tag.
	 * 
	 * @param	parser			The parser to read the level definition from.
	 * @param	tag				Name of the tag to seek to.
	 * @return					true if the tag was found; else false.
	 * @throws XmlPullParserException
	 * @throws IOException
	 * @throws LevelException	Error encountered while reading.
	 */
	private boolean findBlock(XmlPullParser parser, String tag)
		throws XmlPullParserException, IOException, LevelException
	{
		int eventType = parser.getEventType();
		while (eventType != XmlPullParser.END_DOCUMENT) {
			if(eventType == XmlPullParser.START_DOCUMENT)
				;
			else if(eventType == XmlPullParser.END_DOCUMENT)
				;
			else if(eventType == XmlPullParser.START_TAG) {
				if (parser.getName().equals(tag))
					return true;
			}
			eventType = parser.next();
		}

		return false;
	}


	/**
	 * Read a level header from the given XML parser.  Assumes we are already
	 * at the <Level> tag.
	 * 
	 * @param	parser			The parser to read the level definition from.
	 * @return					The level header.
	 * @throws XmlPullParserException
	 * @throws IOException
	 * @throws LevelException	Error encountered while reading.
	 */
	private LevelData.Header buildLevelHeader(XmlPullParser parser)
		throws XmlPullParserException, IOException, LevelException
	{
		// Read the level attributes.
		Bundle attrs = readAttributes(parser);
		String name = attrs.getString("name");
		if (name == null)
			name = "???";
		int group = attrs.getInt("group", 0);
		int difficulty = attrs.getInt("difficulty", 0);
		long time = attrs.getInt("time", 120) * 1000;

		return new LevelData.Header(name, group, difficulty, time);
	}


	/**
	 * Read a level item from the given parser, transform it to fit the
	 * playing screen, and return the resulting object.
	 * 
	 * The parser is currently at the opening tag.  We will
	 * leave it on the tag after the closing tag.
	 * 
	 * @param	p			The parser to read from.
	 * @param	xform		The transformation that needs to be applied
	 * 						to the level to make it fit the screen.
	 * @param	parent		The parent element of this element; null if none.
	 * @throws XmlPullParserException
	 * @throws IOException
	 * @throws LevelException
	 */
	private Object readItem(XmlPullParser p, Matrix xform, Element parent)
		throws XmlPullParserException, IOException, LevelException
	{
		// We should be at an open tag right now.
		int eventType = p.getEventType();
		if (eventType != XmlPullParser.START_TAG)
			throw new LevelException(p, "expecting open tag, got \"" +
										p.getText() + "\"");

		// Read the item attributes from the open tag.
		String tag = p.getName();
		Bundle attrs = readAttributes(p);
		
		// Build the object for this node.
		Object item = buildItem(p, xform, tag, attrs);

		// Get to the next token.
		eventType = p.nextTag();
		
		// Now if this is an Element, read any nested tags.  Some nested
		// tags get pushed up and added to the parent.  The important thing
		// is that they are added before finished() is called.
		if (item instanceof Element) {
			Element elem = (Element) item;
			while (eventType != XmlPullParser.END_TAG) {
				Object child = readItem(p, xform, elem);
				if (!elem.addChild(p, tag, child) && parent != null)
					parent.addChild(p, tag, child);
		        eventType = p.nextTag();
			}
			elem.finished();
		}

		// Check the close tag.  If we're at an open tag, produce
		// an appropriate error
		eventType = p.getEventType();
		if (eventType == XmlPullParser.START_TAG)
			throw new LevelException(p, "unexpected <" + p.getName() +
										"> inside <" + tag + ">");
		else if (eventType != XmlPullParser.END_TAG || !p.getName().equals(tag))
			throw new LevelException(p, "expecting </" + tag +
										">, got \"" + p.getText() + "\"");
		
		return item;
	}


    // ******************************************************************** //
    // Level Element Parsing.
    // ******************************************************************** //

	/**
	 * Read a level item from the given parser, transform it
	 * to fit the playing screen, and place the resulting
	 * level data into the given LevelData object.
	 * 
	 * The parser is currently at the opening tag.  We will
	 * leave it after the closing tag.
	 * 
	 * @param	p			The parser to read from.
	 * @param	xform		The transformation that needs to be applied
	 * 						to the level to make it fit the screen.
	 * @param	tag 		The name of this item's XML tag.
	 * @param	attrs		The XML attributes for this item.
	 * @throws LevelException
	 */
	private Object buildItem(XmlPullParser p, Matrix xform,
							 String tag, Bundle attrs)
		throws LevelException
	{
		// See if this tag has an ID.
		String id = attrs.getString("id");

		if (tag.equals("Level") || tag.equals("Common")) {
			return buildLevel(p, xform, tag, id, attrs);
		} else if (tag.equals("Point")) {
			return buildPoint(p, xform, tag, id, attrs);
		} else if (tag.equals("Start")) {
			Point point = buildPoint(p, xform, tag, id, attrs);
			return new Location(appContext, id, Location.Type.START, point);
		} else if (tag.equals("Target")) {
			if (id == null)
				throw new LevelException(p, "<Target> must have an id");
			Point point = buildPoint(p, xform, tag, id, attrs);
			return new Location(appContext, id, Location.Type.TARGET, point);
		} else if (tag.equals("Rect")) {
			return buildRect(p, xform, tag, id, attrs);
		} else if (tag.equals("Poly")) {
			return buildPoly(p, xform, tag, id, attrs);
		} else if (tag.equals("Hole")) {
			return buildHole(p, xform, tag, id, attrs);
        } else if (tag.equals("Wall")) {
            return buildWall(p, xform, tag, id, attrs);
        } else if (tag.equals("Draw")) {
            return buildDraw(p, xform, tag, id, attrs);
		} else if (tag.equals("OnCross") || tag.equals("OnBounce") ||
													tag.equals("WhileZone")) {
			return buildAction(p, xform, tag, id, attrs);
		} else if (tag.equals("Graphic")) {
			return buildGraphic(p, xform, tag, id, attrs);
		} else if (tag.equals("Anim")) {
			return buildAnim(p, xform, tag, id, attrs);
		} else if (tag.equals("Text")) {
			return buildText(p, xform, tag, id, attrs);
		} else
			throw new LevelException(p, "unrecognised tag: <" + tag + ">");
	}


	/**
	 * Read a level header from the given XML parser.  Assumes we are already
	 * at the <Level> tag.
	 * 
	 * @param	p			The parser to read from.
	 * @param	xform		The transformation that needs to be applied
	 * 						to the level to make it fit the screen.
	 * @param	tag 		The name of this item's XML tag.
	 * @param	id			The ID of this element.
	 * @param	attrs		The XML attributes for this item.
	 * @return				The constructed element.
	 * @throws LevelException	Error encountered while reading.
	 */
	private LevelData buildLevel(XmlPullParser p, Matrix xform, String tag,
			 					 String id, Bundle attrs)
		throws LevelException
	{
		// Read the level attributes.
		String name = attrs.getString("name");
		if (name == null)
			name = "???";
		int group = attrs.getInt("group", 0);
		int difficulty = attrs.getInt("difficulty", 0);
		long time = attrs.getInt("time", 120) * 1000;

		// Read the level header.
		LevelData theLevel = new LevelData(appContext, name,
		                                   group, difficulty, time, xform);
		return theLevel;
	}


	/**
	 * Read a point (any tag with "x" and "y" attributes) from the
	 * given parser.  Transform it by the given Matrix and return it as
	 * a Point.
	 * 
	 * @param	p			The parser to read from.
	 * @param	xform		The transformation that needs to be applied
	 * 						to the level to make it fit the screen.
	 * @param	tag 		The name of this item's XML tag.
	 * @param	id			The ID of this element.
	 * @param	attrs		The XML attributes for this item.
	 * @return				The constructed element.
	 * @throws LevelException	Error encountered while reading.
	 */
	private Point buildPoint(XmlPullParser p, Matrix xform, String tag,
							 String id, Bundle attrs)
		throws LevelException
	{
		float x = attrs.getFloat("x", 0);
		float y = attrs.getFloat("y", 0);
		return xform.transform(new Point(x, y));
	}

	
	/**
	 * Read a rectangle from the given parser and make it into a new Poly.
	 * Transform it by the given Matrix.
	 * 
	 * @param	p			The parser to read from.
	 * @param	xform		The transformation that needs to be applied
	 * 						to the level to make it fit the screen.
	 * @param	tag 		The name of this item's XML tag.
	 * @param	id			The ID of this element.
	 * @param	attrs		The XML attributes for this item.
	 * @return				The constructed element.
	 * @throws LevelException	Error encountered while reading.
	 */
	private Poly buildRect(XmlPullParser p, Matrix xform, String tag,
			 			  String id, Bundle attrs)
		throws LevelException
	{
		float sx = attrs.getFloat("sx", 0);
		float sy = attrs.getFloat("sy", 0);
		float ex = attrs.getFloat("ex", 0);
		float ey = attrs.getFloat("ey", 0);

        RectF box = new RectF(sx, sy, ex, ey);
		return new Poly(appContext, id, box, xform);
	}


	/**
	 * Read a polygon definition from the level data, and transform it by
	 * the given Matrix.
	 * 
	 * @param	p			The parser to read from.
	 * @param	xform		The transformation that needs to be applied
	 * 						to the level to make it fit the screen.
	 * @param	tag 		The name of this item's XML tag.
	 * @param	id			The ID of this element.
	 * @param	attrs		The XML attributes for this item.
	 * @return				The constructed element.
	 * @throws LevelException	Error encountered while reading.
	 */
	private Poly buildPoly(XmlPullParser p, Matrix xform, String tag,
			  			  String id, Bundle attrs)
		throws LevelException
	{
		// See if this is a reference to a common poly.
		String ref = attrs.getString("ref");
		if (ref != null) {
			Object obj = common.getById(ref);
			if (obj == null || !(obj instanceof Poly))
				throw new LevelException(p, "no defined Poly with id " + ref);
			return (Poly) obj;
		}
		
		// Make an empty polygon.
		return new Poly(appContext, id, xform);
	}


	/**
	 * Read a hole from the given parser.  Transform it by the given Matrix.
	 * 
	 * @param	p			The parser to read from.
	 * @param	xform		The transformation that needs to be applied
	 * 						to the level to make it fit the screen.
	 * @param	tag 		The name of this item's XML tag.
	 * @param	id			The ID of this element.
	 * @param	attrs		The XML attributes for this item.
	 * @return				The constructed element.
	 * @throws LevelException	Error encountered while reading.
	 */
	private Hole buildHole(XmlPullParser p, Matrix xform, String tag,
			  			  String id, Bundle attrs)
		throws LevelException
	{
		float x = attrs.getFloat("x", 0);
		float y = attrs.getFloat("y", 0);
		
		return new Hole(appContext, id, x, y, xform);
	}


    /**
     * Build a Wall definition from the current tag.
     * 
     * @param   p           The parser to read from.
     * @param   xform       The transformation that needs to be applied
     *                      to the level to make it fit the screen.
     * @param   tag         The name of this item's XML tag.
     * @param   id          The ID of this element.
     * @param   attrs       The XML attributes for this item.
     * @return              The constructed element.
     * @throws LevelException   Error encountered while reading.
     */
    private Poly.Wall buildWall(XmlPullParser p, Matrix xform, String tag,
                             String id, Bundle attrs)
        throws LevelException
    {
        boolean init = attrs.getBoolean("initial", true);
        Poly.Wall wall = new Poly.Wall(init);
        return wall;
    }


    /**
     * Build a Draw definition from the current tag.
     * 
     * @param   p           The parser to read from.
     * @param   xform       The transformation that needs to be applied
     *                      to the level to make it fit the screen.
     * @param   tag         The name of this item's XML tag.
     * @param   id          The ID of this element.
     * @param   attrs       The XML attributes for this item.
     * @return              The constructed element.
     * @throws LevelException   Error encountered while reading.
     */
    private Poly.Draw buildDraw(XmlPullParser p, Matrix xform, String tag,
                             String id, Bundle attrs)
        throws LevelException
    {
        int col = LevelData.WALL_COLOR;
        if (attrs.containsKey("color"))
            col = attrs.getInt("color");
        return new Poly.Draw(col);
    }


	/**
	 * Build an Action from the current tag.
	 * 
	 * @param	p			The parser to read from.
	 * @param	xform		The transformation that needs to be applied
	 * 						to the level to make it fit the screen.
	 * @param	tag 		The name of this item's XML tag.
	 * @param	id			The ID of this element.
	 * @param	attrs		The XML attributes for this item.
	 * @return				The constructed element.
	 * @throws LevelException	Error encountered while reading.
	 */
	private Action buildAction(XmlPullParser p, Matrix xform, String tag,
							   String id, Bundle attrs)
		throws LevelException
	{
        // Read the trigger type, and translate it to the proper value.
		Action.Trigger trig = null;
		try {
			trig = Action.Trigger.valueOf(tag.toUpperCase());
		} catch (IllegalArgumentException e) {
			throw new LevelException(p, "strange action trigger \"" + tag + "\"");
		}

		// Read the action type, and translate it to the proper value.
		String tname = attrs.getString("type");
		if (tname == null)
			throw new LevelException(p, "<" + tag +
									    "> requires a \"type\" attribute");
		Action.Type type = null;
		try {
			type = Action.Type.valueOf(tname.toUpperCase());
		} catch (IllegalArgumentException e) {
			throw new LevelException(p, "action type \"" +
										tname + "\" is not valid");
		}
		
		String msg = attrs.getString("message");

		Action action = new Action(trig, type, msg);
	    
	    // Add type-specific action parameters.
		switch (type) {
		case SPEED:
		    if (!attrs.containsKey("value"))
		        throw new LevelException(p, "\"speed\" action requires" +
		                                    " a \"value\" attribute");
		    action.setSpeed(attrs.getFloat("value"));
		    break;
		case TELEPORT:
        case OFF:
        case ON:
        case ONOFF:
		    if (!attrs.containsKey("target"))
		        throw new LevelException(p, "\"teleport\" action requires" +
		                                    " a \"target\" attribute");
		    action.setTarget(attrs.getString("target"));
		    break;
		default:
		    break;
		}

		return action;
	}


	/**
	 * Read a graphic from the given parser.
	 * 
	 * @param	p			The parser to read from.
	 * @param	xform		The transformation that needs to be applied
	 * 						to the level to make it fit the screen.
	 * @param	tag 		The name of this item's XML tag.
	 * @param	id			The ID of this element.
	 * @param	attrs		The XML attributes for this item.
	 * @return				The constructed element.
	 * @throws LevelException	Error encountered while reading.
	 */
	private Element buildGraphic(XmlPullParser p, Matrix xform, String tag,
								 String id, Bundle attrs)
		throws LevelException
	{
		int imgId = attrs.getInt("img", 0);
		boolean norot = attrs.getBoolean("norotate", false);

		if (imgId == 0)
			throw new LevelException(p, "<" + tag +
									    "> requires an \"img\" attribute");
		
		return new Graphic(appContext, id, imgId, xform, norot);
	}


    /**
     * Read an animated graphic from the given parser.
     * 
     * @param   p           The parser to read from.
     * @param   xform       The transformation that needs to be applied
     *                      to the level to make it fit the screen.
     * @param   tag         The name of this item's XML tag.
     * @param   id          The ID of this element.
     * @param   attrs       The XML attributes for this item.
     * @return              The constructed element.
     * @throws LevelException   Error encountered while reading.
     */
    private Element buildAnim(XmlPullParser p, Matrix xform, String tag,
                              String id, Bundle attrs)
        throws LevelException
    {
        // Read the animation type, and translate it to the proper value.
        String tname = attrs.getString("type");
        if (tname == null)
            throw new LevelException(p, "<" + tag +
                                        "> requires a \"type\" attribute");
        Anim.Type type = null;
        try {
            type = Anim.Type.valueOf(tname.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new LevelException(p, "animation type \"" +
                                        tname + "\" is not valid");
        }
        
        boolean norot = attrs.getBoolean("norotate", false);
        
        return new Anim(appContext, id, type, xform, norot);
    }


	/**
	 * Read a display from the given parser.  Transform it by the given Matrix.
	 * 
	 * @param	p			The parser to read from.
	 * @param	xform		The transformation that needs to be applied
	 * 						to the level to make it fit the screen.
	 * @param	tag 		The name of this item's XML tag.
	 * @param	id			The ID of this element.
	 * @param	attrs		The XML attributes for this item.
	 * @return				The constructed element.
	 * @throws LevelException	Error encountered while reading.
	 */
	private Text buildText(XmlPullParser p, Matrix xform, String tag,
				 				String id, Bundle attrs)
		throws LevelException
	{
		float size = attrs.getFloat("size", 1.0f);
		String text = attrs.getString("text");
		
		// We must have text; unless this is a special field, whose
		// ID starts with "$".
		boolean isSpecial = id != null && id.charAt(0) == '$';
		if (text == null && !isSpecial)
			throw new LevelException(p, "<" + tag +
									    "> requires a \"text\" attribute");
		
		return new Text(appContext, id, text, size, xform);
	}


    // ******************************************************************** //
    // Parsing Utilities.
    // ******************************************************************** //

	/**
	 * Read the attributes for the current tag, process them according to
	 * the type defined in typeMap, and wrap them up in a bundle.
	 * 
	 * @param	p				The parser to read from.
	 * @return					Bundle containing the attributes.
	 * @throws XmlPullParserException
	 * @throws IOException
	 * @throws LevelException
	 */
	private Bundle readAttributes(XmlPullParser p)
		throws XmlPullParserException, IOException, LevelException
	{
		Bundle attrs = new Bundle();

		AttributeSet a = Xml.asAttributeSet(p);
		for (int i = 0; i < p.getAttributeCount(); ++i) {
			String n = p.getAttributeName(i);
			Character t = typeMap.get(n);
			if (t == null)
				throw new LevelException(p, "unknown attribute " + n);
			try {
				switch (t) {
				case 'B':
					try {
						attrs.putBoolean(n, a.getAttributeBooleanValue(i, false));
					} catch (RuntimeException e) {
						throw new LevelException(p, "bad bool " +
							    e.getMessage());
					}
					break;
				case 'I':
					attrs.putInt(n, a.getAttributeIntValue(i, 0));
					break;
				case 'F':
					try {
						attrs.putFloat(n, a.getAttributeFloatValue(i, 0));
					} catch (RuntimeException e) {
						// This is crap!  If it's not actually a float -- i.e.
						// has a fraction -- they throw RuntimeException.
						attrs.putFloat(n, a.getAttributeIntValue(i, 0));
					}
					break;
				case 'S':
					String s = a.getAttributeValue(i);
					if (s.charAt(0) == '@') {
						int sid = resources.getIdentifier(s.substring(1),
														  "string",
														  "org.hermit.plughole"); 
						if (sid != 0)
							s = (String) resources.getText(sid);
					}
					attrs.putString(n, s);
					break;
                case 'C':
                    attrs.putInt(n, a.getAttributeIntValue(i, 0));
                    break;
				case '#':
					String iname = a.getAttributeValue(i);
					if (iname.charAt(0) != '@')
						throw new LevelException(p, "invalid image resource ID " +
													iname);
					int rid = resources.getIdentifier(iname.substring(1),
							  						  "drawable",
							  						  "org.hermit.plughole"); 
					attrs.putInt(n, rid);
					break;
				default:
					throw new LevelException(p, "unknown attribute type!!! " +
											    n + "=" + t);
				}
			} catch (RuntimeException e) {
				// This is crap!  They throw RuntimeException for some errors.
				throw new LevelException(p, "attribute error: " +
											e.getMessage());
			}
		}
		
		return attrs;
	}
	

    // ******************************************************************** //
    // Class Data.
    // ******************************************************************** //

    // Debugging tag.
	@SuppressWarnings("unused")
	private static final String TAG = "plughole";
	
	// Map of the types of attributes we can have in the XML file.
	private static final HashMap<String, Character> typeMap =
									new HashMap<String, Character>();
	static {
		typeMap.put("x", 'F');
		typeMap.put("y", 'F');
		typeMap.put("sx", 'F');
		typeMap.put("sy", 'F');
		typeMap.put("ex", 'F');
		typeMap.put("ey", 'F');
        typeMap.put("value", 'F');
		typeMap.put("size", 'F');
		typeMap.put("name", 'S');
		typeMap.put("img", '#');
		typeMap.put("id", 'S');
		typeMap.put("ref", 'S');
		typeMap.put("target", 'S');
		typeMap.put("group", 'I');
		typeMap.put("difficulty", 'I');
		typeMap.put("time", 'I');
		typeMap.put("type", 'S');
        typeMap.put("action", 'S');
		typeMap.put("text", 'S');
        typeMap.put("message", 'S');
		typeMap.put("norotate", 'B');
		typeMap.put("vertical", 'B');
        typeMap.put("initial", 'B');
        typeMap.put("color", 'C');
	}

	
    // ******************************************************************** //
    // Public Data.
    // ******************************************************************** //

	// Application we're running in.
	private final Plughole appContext;
	
	// Application's resources.
	private final Resources resources;
	
	// Shared level elements,if we have any.
	private LevelData common = null;
	
}

