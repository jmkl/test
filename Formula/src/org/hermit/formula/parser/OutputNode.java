
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


/**
 * An output field definition in a formula.
 */
public class OutputNode
	extends FormulaNode
{

	// ******************************************************************** //
	// Constructor.
	// ******************************************************************** //

	/**
	 * Construct a node.
	 * 
	 * @param	label		The label string for this output field.
	 * @param	format		The printf-style format string for this field.
	 * @param	name		The variable name displayed by this output field.
	 */
	public OutputNode(String label, String format, String name) {
		super(OUTPUT);
		outputLabel = label;
		outputFormat = format;
		outputName = name;
	}

	
	// ******************************************************************** //
	// Accessors.
	// ******************************************************************** //

	/**
	 * Get the label string for this output.
	 * 
	 * @return				The label string for this output.
	 */
	public String getLabel() {
		return outputLabel;
	}


	/**
	 * Get the format string for this output.
	 * 
	 * @return				The format string for this output.
	 */
	public String getFormat() {
		return outputFormat;
	}


	/**
	 * Get the variable name for this output.
	 * 
	 * @return				The variable name for this output.
	 */
	public String getVariable() {
		return outputName;
	}


	// ******************************************************************** //
	// Debug.
	// ******************************************************************** //

	/**
	 * Dump this node with a given prefix.
	 * 
	 * @param	prefix		Prefix for each line of output.
	 */
	@Override
	public void dumpThis(String prefix) {
		String type = toString();
		System.out.println(prefix + type + " \"" + outputLabel + "\" -> " + outputName);
	}

	
	// ******************************************************************** //
	// Private Data.
	// ******************************************************************** //

	// The prompt string for this output field.
	private String outputLabel = null;

	// The printf-style format for this output field.
	private String outputFormat = null;

	// The variable name for this output field.
	private String outputName = null;
	
}

