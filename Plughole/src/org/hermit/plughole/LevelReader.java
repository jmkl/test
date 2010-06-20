
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
import java.util.ArrayList;
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
		common = new LevelData();
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

			// Read the body of the level definition.
			readBlock(parser, xform, common);
		} catch (LevelException e) {
			throw e;
		} catch (XmlPullParserException e) {
			throw new LevelException(parser, "Parser error: " + e.getMessage());
		} catch (IOException e) {
			throw new LevelException(parser, "I/O error: " + e.getMessage());
		} catch (Throwable e) {
			throw new LevelException(parser, "Error " + e.getClass().getName() +
											 ": " + e.getMessage());
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
			LevelData.Header header = readHeader(parser);
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
			
			// Read the level header.
			LevelData theLevel = new LevelData();
			LevelData.Header header = readHeader(parser);
			theLevel.setHeader(header);

			// Read the body of the level definition.
			readBlock(parser, xform, theLevel);
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
		} catch (Throwable e) {
			throw new LevelException(parser, "Error " + e.getClass().getName() +
											 ": " + e.getMessage());
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
	private LevelData.Header readHeader(XmlPullParser parser)
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
	 * Read a level definition from the given parser.  Transform all the
	 * constructs to fit the playing screen, and place the resulting
	 * level data into a new LevelData object.
	 * 
	 * The parser is currently at the opening <Level> tag.  We will
	 * leave it after the closing tag.
	 * 
	 * @param	p				The parser to read from.
	 * @param	xform			The transformation that needs to be applied
	 * 							to the level to make it fit the screen.
	 * @param	ldata			The level data we read in.
	 * @throws XmlPullParserException
	 * @throws IOException
	 * @throws LevelException
	 */
	private void readBlock(XmlPullParser p, Matrix xform, LevelData ldata)
		throws XmlPullParserException, IOException, LevelException
	{
		int eventType;
	reading:
		while ((eventType = p.next()) != XmlPullParser.END_DOCUMENT) {
			switch (eventType) {
			case XmlPullParser.START_TAG:
				// Get the object's name and attributes.
				Bundle oattr = readAttributes(p);
				String tag = p.getName();
				
				// See if this tag has an ID.
				String id = oattr.getString("id");

				if (tag.equals("Start")) {
					Point point = readPoint(p, xform, oattr);
					ldata.setStart(point);
				} else if (tag.equals("Target")) {
					Point point = readPoint(p, xform, oattr);
					ldata.addPoint(point, id);
				} else if (tag.equals("Poly")) {
					Poly poly = readPoly(p, xform, oattr);
					ldata.addBackground(poly, id);
					ldata.addBarrier(poly, null);
				} else if (tag.equals("Rect")) {
					Poly poly = readRect(p, xform, oattr, ldata);
					ldata.addBackground(poly, id);
					ldata.addBarrier(poly, id);
                } else if (tag.equals("Zone")) {
                    Poly poly = readRect(p, xform, oattr, ldata);
                    ldata.addTrigger(poly, id);
				} else if (tag.equals("Hole")) {
					Hole hole = readHole(p, xform, oattr, ldata);
					ldata.addAnim(hole, id);
					ldata.addZone(hole, id);
					Poly trap = hole.getCentreTrap();
					if (trap != null)
						ldata.addWall(trap, id);
                } else if (tag.equals("ForceField")) {
                    ForceField field = readForceField(p, xform, oattr);
                    ldata.addAnim(field, id);
                    Poly wall = field.getBarrier();
                    if (wall != null) {
                        wall = ldata.addBarrier(wall, id);
                        field.setRealBarrier(wall);
                    }
				} else if (tag.equals("Display")) {
					Display dec = readDisplay(p, xform, oattr);
					ldata.addBackground(dec, id);
				} else if (tag.equals("Graphic")) {
					Element dec = readGraphic(p, xform, oattr);
					ldata.addBackground(dec, id);
				} else if (tag.equals("Anim")) {
					Element dec = readGraphic(p, xform, oattr);
					ldata.addAnim(dec, id);
				}
				break;
			case XmlPullParser.END_TAG:
				break reading;
			}
		}
	}

	
	/**
	 * Read a polygon definition from the level data, and transform it by
	 * the given Matrix.
	 * 
	 * Leave the parser after the end of the tag.
	 * 
	 * @param	p				The parser to read from.
	 * @param	xform			The transformation that needs to be applied
	 * 							to the level to make it fit the screen.
	 * @param	attrs			The tag's attributes.
	 * @return					The transformed polygon.
	 * @throws XmlPullParserException
	 * @throws IOException
	 * @throws LevelException
	 */
	private Poly readPoly(XmlPullParser p, Matrix xform, Bundle attrs)
		throws XmlPullParserException, IOException, LevelException
	{
		// See if this is a reference to a common poly.
		String ref = attrs.getString("ref");
		if (ref != null) {
			Object obj = common.getById(ref);
			if (obj == null || !(obj instanceof Poly))
				throw new LevelException(p, "no defined Poly with id " + ref);
			
			// Skip past the tag.
			int eventType;
			while ((eventType = p.next()) != XmlPullParser.END_DOCUMENT &&
										eventType != XmlPullParser.END_TAG)
				;
			return (Poly) obj;
		}

		// Read the points that make up the polygon.
		ArrayList<Point> points = new ArrayList<Point>();
		int eventType;
	reading:
		while ((eventType = p.next()) != XmlPullParser.END_DOCUMENT) {
			switch (eventType) {
			case XmlPullParser.START_TAG:
				if (p.getName().equals("Point")) {
					Bundle oattr = readAttributes(p);
					points.add(readPoint(p, xform, oattr));
				}
				break;
			case XmlPullParser.END_TAG:
				break reading;
			}
		}
		
		// The points are already transformed, so all we need to do is
		// make a polygon.
		return new Poly(appContext, points);
	}

	
	/**
	 * Read a rectangle from the given parser and make it into a new Poly.
	 * Transform it by the given Matrix.
	 * 
	 * Leave the parser after the end of the tag.
	 * 
	 * @param	p				The parser to read from.
	 * @param	xform			The transformation that needs to be applied
	 * 							to the level to make it fit the screen.
	 * @param	attrs			The tag's attributes.
     * @param   ldata           The level we're building.
	 * @return					The transformed polygon.
	 * @throws XmlPullParserException
	 * @throws IOException
	 * @throws LevelException
	 */
	private Poly readRect(XmlPullParser p, Matrix xform, Bundle attrs, LevelData ldata)
		throws XmlPullParserException, IOException, LevelException
	{
		float sx = attrs.getFloat("sx", 0);
		float sy = attrs.getFloat("sy", 0);
		float ex = attrs.getFloat("ex", 0);
		float ey = attrs.getFloat("ey", 0);

		// See if we have an action.
		Action action = null;
		String actName = attrs.getString("action");
		if (actName != null) {
		    Action.Type type;
	        try {
	            type = Action.Type.valueOf(actName);
	        } catch (IllegalArgumentException e) {
	            throw new LevelException(p, "invalid action type " + actName);
	        }
	        action = new Action(type);

		    String targId = attrs.getString("target");
		    if (targId != null) {
		        Object target = ldata.getById(targId);
		        if (target == null)
		            throw new LevelException(p, "no defined object with id \"" + targId + "\"");
		        action.setTarget(target);
		    }
		}

		// Skip past the tag.
		int eventType = p.getEventType();
		while (eventType != XmlPullParser.END_DOCUMENT &&
									eventType != XmlPullParser.END_TAG)
			eventType = p.next();

		// Create the transformed points, and make a Poly from them.
		ArrayList<Point> points = new ArrayList<Point>();
		points.add(xform.transform(new Point(sx, sy)));
		points.add(xform.transform(new Point(ex, sy)));
		points.add(xform.transform(new Point(ex, ey)));
		points.add(xform.transform(new Point(sx, ey)));

		return new Poly(appContext, points, action);
	}

	
	/**
	 * Read a hole from the given parser.  Transform it by the given Matrix.
	 * 
	 * Leave the parser after the end of the tag.
	 * 
	 * @param	p				The parser to read from.
	 * @param	xform			The transformation that needs to be applied
	 * 							to the level to make it fit the screen.
	 * @param	attrs			The tag's attributes.
     * @param   ldata           The level we're building.
	 * @return					The transformed Hole.
	 * @throws XmlPullParserException
	 * @throws IOException
	 * @throws LevelException
	 */
	private Hole readHole(XmlPullParser p, Matrix xform, Bundle attrs, LevelData ldata)
		throws XmlPullParserException, IOException, LevelException
	{
		String t = attrs.getString("type");
		float x = attrs.getFloat("x", 0);
		float y = attrs.getFloat("y", 0);
		String targId = attrs.getString("target");

		Hole.Type type;
		try {
			type = Hole.Type.valueOf(t);
		}
		catch (IllegalArgumentException e) {
			throw new LevelException(p, "invalid hole type " + t);
		}
		catch (NullPointerException e) {
			throw new LevelException(p, "missing hole type");
		}
	     
		Point target = null;
		if (targId != null) {
			Object tobj = ldata.getById(targId);
			if (tobj == null || !(tobj instanceof Point))
				throw new LevelException(p, "no defined Point with id " + targId);
			target = (Point) tobj;
		}
		
		// Skip past the tag.
		int eventType = p.getEventType();
		while (eventType != XmlPullParser.END_DOCUMENT &&
									eventType != XmlPullParser.END_TAG)
			eventType = p.next();
		
		return new Hole(appContext, type, x, y, target, xform);
	}

    
    /**
     * Read a force field from the given parser and make it into a new
     * ForceField.  Transform it by the given Matrix.
     * 
     * Leave the parser after the end of the tag.
     * 
     * @param   p               The parser to read from.
     * @param   xform           The transformation that needs to be applied
     *                          to the level to make it fit the screen.
     * @param   attrs           The tag's attributes.
     * @return                  The transformed polygon.
     * @throws XmlPullParserException
     * @throws IOException
     * @throws LevelException
     */
    private ForceField readForceField(XmlPullParser p, Matrix xform, Bundle attrs)
        throws XmlPullParserException, IOException, LevelException
    {
        float sx = attrs.getFloat("sx", 0);
        float sy = attrs.getFloat("sy", 0);
        float ex = attrs.getFloat("ex", 0);
        float ey = attrs.getFloat("ey", 0);
        RectF box = new RectF(sx, sy, ex, ey);

        // Skip past the tag.
        int eventType = p.getEventType();
        while (eventType != XmlPullParser.END_DOCUMENT &&
                                    eventType != XmlPullParser.END_TAG)
            eventType = p.next();

        return new ForceField(appContext, box, xform);
    }


	/**
	 * Read a display from the given parser.  Transform it by the given Matrix.
	 * 
	 * Leave the parser after the end of the tag.
	 * 
	 * @param	p				The parser to read from.
	 * @param	xform			The transformation that needs to be applied
	 * 							to the level to make it fit the screen.
	 * @param	attrs			The tag's attributes.
	 * @return					The transformed Display.
	 * @throws XmlPullParserException
	 * @throws IOException
	 * @throws LevelException
	 */
	private Display readDisplay(XmlPullParser p, Matrix xform, Bundle attrs)
		throws XmlPullParserException, IOException, LevelException
	{
		float size = attrs.getFloat("size", 0);
		float sx = attrs.getFloat("sx", 0);
		float sy = attrs.getFloat("sy", 0);
		float ex = attrs.getFloat("ex", 0);
		float ey = attrs.getFloat("ey", 0);
		RectF box = new RectF(sx, sy, ex, ey);
		String text = attrs.getString("text");

		// Skip past the tag.
		int eventType = p.getEventType();
		while (eventType != XmlPullParser.END_DOCUMENT &&
									eventType != XmlPullParser.END_TAG)
			eventType = p.next();

		if (text != null)
			return new Display(appContext, text, size, box, xform);
		else
			return new Display(appContext, size, box, xform);
	}


	/**
	 * Read a graphic from the given parser.  Transform it by the given Matrix.
	 * 
	 * Leave the parser after the end of the tag.
	 * 
	 * @param	p				The parser to read from.
	 * @param	xform			The transformation that needs to be applied
	 * 							to the level to make it fit the screen.
	 * @param	attrs			The tag's attributes.
	 * @return					The transformed Display.
	 * @throws XmlPullParserException
	 * @throws IOException
	 * @throws LevelException
	 */
	private Element readGraphic(XmlPullParser p, Matrix xform, Bundle attrs)
		throws XmlPullParserException, IOException, LevelException
	{
		String type = attrs.getString("type");
		int imgId = attrs.getInt("img", 0);
		float sx = attrs.getFloat("sx", 0);
		float sy = attrs.getFloat("sy", 0);
		float ex = attrs.getFloat("ex", 0);
		float ey = attrs.getFloat("ey", 0);
		RectF box = new RectF(sx, sy, ex, ey);
		boolean norot = attrs.getBoolean("norotate", false);
		boolean vertical = attrs.getBoolean("vertical", false);
		
		// Skip past the tag.
		int eventType = p.getEventType();
		while (eventType != XmlPullParser.END_DOCUMENT &&
									eventType != XmlPullParser.END_TAG)
			eventType = p.next();

		if (type != null) {
			if (type.equals("clock"))
				return new Clock(appContext, box, xform, vertical);
			if (type.equals("green_arrow"))
				return new Graphic(appContext, Graphic.GREEN_ARROW, box, xform, norot);
			throw new LevelException(p, "animation type \"" +
										type + "\"is not valid");
		}
		if (imgId == 0)
			throw new LevelException(p, "graphics require either" +
										" \"type\" or \"img\"");
		return new Graphic(appContext, imgId, box, xform, norot);
	}


	/**
	 * Read a point (any tag with "x" and "y" attributes) from the
	 * given parser.  Transform it by the given Matrix and return it as
	 * a Point.
	 * 
	 * Leave the parser after the end of the tag.
	 * 
	 * @param	p				The parser to read from.
	 * @param	xform			The transformation that needs to be applied
	 * 							to the level to make it fit the screen.
	 * @param	attrs			The tag's attributes.
	 * @return					The transformed Point.
	 * @throws XmlPullParserException
	 * @throws IOException
	 * @throws LevelException
	 */
	private Point readPoint(XmlPullParser p, Matrix xform, Bundle attrs)
		throws XmlPullParserException, IOException, LevelException
	{
		float x = attrs.getFloat("x", 0);
		float y = attrs.getFloat("y", 0);
		
		// Skip past the tag.
		int eventType = p.getEventType();
		while (eventType != XmlPullParser.END_DOCUMENT &&
									eventType != XmlPullParser.END_TAG)
			eventType = p.next();
		
		return xform.transform(new Point(x, y));
	}

	
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
		typeMap.put("norotate", 'B');
		typeMap.put("vertical", 'B');
	}

	
    // ******************************************************************** //
    // Public Data.
    // ******************************************************************** //

	// Application we're running in.
	private final Plughole appContext;
	
	// Application's resources.
	private final Resources resources;
	
	// Shared level elements.
	private final LevelData common;
	
}

