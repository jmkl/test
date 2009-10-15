
/**
 * Formula: programmable custom computations.
 * <br>Copyright 2009 Ian Cameron Smith
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


package org.hermit.formula.parser;

import java.util.ArrayList;
import java.util.Iterator;


/**
 * Top-level node in a formula.
 */
public class FormulaNode
	implements FormulaParserConstants
{

	// ******************************************************************** //
	// Constructor.
	// ******************************************************************** //

	/**
	 * Construct a node.  Children can be added later.
	 * 
	 * @param	key			The symbol for the keyword indicating the
	 * 						type of this node; one of the constants from
	 * 						FormulaParserConstants.
	 */
	public FormulaNode(int key) {
		keyword = key;
	}


	/**
	 * Construct a node with a literal double value.
	 * 
	 * @param	key			The symbol for the keyword indicating the
	 * 						type of this node; one of the constants from
	 * 						FormulaParserConstants.
	 * @param	val			The value this node.
	 */
	public FormulaNode(int key, double val) {
		this(key);
		doubleVal = val;
		haveDouble = true;
	}


	/**
	 * Construct a node with a literal string value.
	 * 
	 * @param	key			The symbol for the keyword indicating the
	 * 						type of this node; one of the constants from
	 * 						FormulaParserConstants.
	 * @param	val			The value this node.
	 */
	public FormulaNode(int key, String val) {
		this(key);
		stringVal = val;
	}


	/**
	 * Construct a node with one child.  Other children can be added later.
	 * 
	 * @param	key			The symbol for the keyword indicating the
	 * 						type of this node; one of the constants from
	 * 						FormulaParserConstants.
	 * @param	c1			First child of this node.
	 */
	public FormulaNode(int key, FormulaNode c1) {
		this(key);
		addChild(c1);
	}


	/**
	 * Construct a node with two children.  Other children can be added later.
	 * 
	 * @param	key			The symbol for the keyword indicating the
	 * 						type of this node; one of the constants from
	 * 						FormulaParserConstants.
	 * @param	c1			First child of this node.
	 * @param	c2			Second child of this node.
	 */
	public FormulaNode(int key, FormulaNode c1, FormulaNode c2) {
		this(key);
		addChild(c1);
		addChild(c2);
	}


	/**
	 * Construct a node with two children.  Other children can be added later.
	 * 
	 * @param	key			The symbol for the keyword indicating the
	 * 						type of this node; one of the constants from
	 * 						FormulaParserConstants.
	 * @param	c1			First child of this node.
	 * @param	c2			Second child of this node.
	 * @param	c3			Third child of this node.
	 */
	public FormulaNode(int key, FormulaNode c1, FormulaNode c2, FormulaNode c3) {
		this(key);
		if (c1 != null)
			addChild(c1);
		if (c2 != null)
			addChild(c2);
		if (c3 != null)
			addChild(c3);
	}

	
	// ******************************************************************** //
	// Tree Building.
	// ******************************************************************** //

	/**
	 * Add a node to the list of children.
	 * 
	 * @param	n			The node to add.
	 */
	public void addChild(FormulaNode n) {
		if (children == null)
			children = new ArrayList<FormulaNode>(2);
		children.add(n);
	}


	// ******************************************************************** //
	// Accessors.
	// ******************************************************************** //

	/**
	 * Get the keyword of this node.
	 * 
	 * @return				The keyword of this node.  -1 if none specified.
	 */
	public int getKeyword() {
		return keyword;
	}


	/**
	 * Get the number of children.
	 * 
	 * @return				The number of children in this node.
	 */
	public int getNumChildren() {
		return children == null ? 0 : children.size();
	}
	

	/**
	 * Get an iterator on the children.
	 * 
	 * @return				An iterator on the children.
	 */
	public Iterator<FormulaNode> getChildren() {
		return children == null ? null : children.iterator();
	}
	

	/**
	 * Get a specified child of this node.
	 * 
	 * @param	i			Index of the child we want.
	 * @return				The specified child; null if there is no
	 * 						child with that index.
	 */
	public FormulaNode getChild(int i) {
		if (children == null || i >= children.size())
			return null;
		return children.get(i);
	}


	/**
	 * Get the double value of this node, if any.
	 * 
	 * @return				The double value of this node; 0 if it has none.
	 */
	public double getDoubleValue() {
		return doubleVal;
	}


	/**
	 * Get the string value of this node, if any.
	 * 
	 * @return				The string value of this node; null if it has none.
	 */
	public String getStringValue() {
		return stringVal;
	}


	// ******************************************************************** //
	// Debug.
	// ******************************************************************** //
	
	/**
	 * Return a String representation of this node.
	 * 
	 * @return					This node as a String.
	 */
	@Override
	public String toString() {
		return keyword > 0 ? FormulaParserConstants.tokenImage[keyword] : "?";
	}

	
	/**
	 * Dump this node tree.
	 */
	public void dump() {
		dump("");
	}


	/**
	 * Dump this node tree with a given prefix.
	 * 
	 * @param	prefix		Prefix for each line of output.
	 */
	public void dump(String prefix) {
		dumpThis(prefix);
	
		if (children != null)
			for (FormulaNode n : children)
				n.dump(prefix + "  ");
	}


	/**
	 * Dump this node with a given prefix.
	 * 
	 * @param	prefix		Prefix for each line of output.
	 */
	public void dumpThis(String prefix) {
		String type = toString();
		String val = "";
		
		if (haveDouble)
			val += " (" + doubleVal + ")";
		if (stringVal != null)
			val += " (" + stringVal + ")";
		System.out.println(prefix + type + val);
	}


	// ******************************************************************** //
	// Private Data.
	// ******************************************************************** //

	// The keyword associated with this node, if any.  E.g. if this is a Type
	// node, the keyword specifies which type.  -1 if none specified.
	private int keyword = -1;
	
	// The double value of this node, and flag whether we have one.
	private double doubleVal = 0;
	private boolean haveDouble = false;
	
	// The String value of this node; null if none.
	private String stringVal = null;

	// The children of this node.  null if there aren't any.
	private ArrayList<FormulaNode> children = null;

}

