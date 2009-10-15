
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
 * A list of names (e.g. parameters) in a formula.
 */
public class NameList
	extends FormulaNode
{

	// ******************************************************************** //
	// Constructor.
	// ******************************************************************** //

	/**
	 * Construct a node.
	 */
	public NameList() {
		super(OPAREN);
	}

	
	// ******************************************************************** //
	// Accessors.
	// ******************************************************************** //

	/**
	 * Add a name to the list.
	 * 
	 * @param	n			The name to add.
	 */
	public void add(String n) {
		list.add(n);
	}


	/**
	 * Get the number of names in the list.
	 * 
	 * @return				The length of the list.
	 */
	public int size() {
		return list.size();
	}


	/**
	 * Get an iterator over the names in the list.
	 * 
	 * @return				The iterator.
	 */
	public Iterator<String> iterator() {
		return list.iterator();
	}


	/**
	 * Dump this node with a given prefix.
	 * 
	 * @param	prefix		Prefix for each line of output.
	 */
	@Override
	public void dumpThis(String prefix) {
		String type = toString();
		System.out.println(prefix + type);
	}


	// ******************************************************************** //
	// Private Data.
	// ******************************************************************** //

	// The actual list.
	private ArrayList<String> list = new ArrayList<String>();
	
}

