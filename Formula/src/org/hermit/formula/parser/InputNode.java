
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
 * An input field definition in a formula.
 */
public class InputNode
	extends FormulaNode
{

	// ******************************************************************** //
	// Constructor.
	// ******************************************************************** //

	/**
	 * Construct a node.
	 * 
	 * @param	prompt		The prompt string for this input field.
	 * @param	type		The keyword token for the type of this field.
	 * @param	name		The variable name set by this input field.
	 */
	public InputNode(String prompt, int type, String name) {
		super(INPUT);
		inputPrompt = prompt;
		inputType = type;
		inputVar = name;
	}

	
	// ******************************************************************** //
	// Accessors.
	// ******************************************************************** //

	/**
	 * Get the prompt string for this input.
	 * 
	 * @return				The prompt string for this input.
	 */
	public String getPrompt() {
		return inputPrompt;
	}


	/**
	 * Get the type keyword for this input.
	 * 
	 * @return				The type keyword for this input.
	 */
	public int getType() {
		return inputType;
	}


	/**
	 * Get the variable name for this input.
	 * 
	 * @return				The variable name for this input.
	 */
	public String getVariable() {
		return inputVar;
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
		System.out.println(prefix + type + " \"" + inputPrompt + "\" -> " + inputVar);
	}

	
	// ******************************************************************** //
	// Private Data.
	// ******************************************************************** //

	// The prompt string for this input field.
	private String inputPrompt = null;

	// The type keyword for this input field.
	private int inputType = -1;

	// The variable name for this input field.
	private String inputVar = null;
	
}

