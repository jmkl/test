
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
 * A function definition in a formula.
 */
public class FuncNode
	extends FormulaNode
{

	// ******************************************************************** //
	// Constructor.
	// ******************************************************************** //

	/**
	 * Construct a function node.
	 * 
	 * @param	name 		The name of this function.
	 * @param	params 		The parameters of this function.
	 * @param	body 		A node whose children are the statements
	 * 						making up the body.
	 */
	public FuncNode(String name, NameList params, FormulaNode body) {
		super(OPAREN);
		funcName = name;
		funcParams = params;
		funcBody = body;
	}

	
	// ******************************************************************** //
	// Accessors.
	// ******************************************************************** //

	/**
	 * Get the name of this function.
	 * 
	 * @return				The name of this function.
	 */
	public String getName() {
		return funcName;
	}


	/**
	 * Get the parameter list of this function.
	 * 
	 * @return				The parameter list of this function.
	 */
	public NameList getParams() {
		return funcParams;
	}


	/**
	 * Get the body of this function.
	 * 
	 * @return				The body of this function, as a FormulaNode
	 * 						whose children are the top-level statements.
	 */
	public FormulaNode getBody() {
		return funcBody;
	}


	// ******************************************************************** //
	// Debug.
	// ******************************************************************** //

	/**
	 * Dump this node tree with a given prefix.
	 * 
	 * @param	prefix		Prefix for each line of output.
	 */
	@Override
	public void dump(String prefix) {
		dumpThis(prefix);

		// Dump the body.
		if (funcBody != null)
			funcBody.dump(prefix + "  ");
	}


	/**
	 * Dump this node with a given prefix.
	 * 
	 * @param	prefix		Prefix for each line of output.
	 */
	@Override
	public void dumpThis(String prefix) {
		String type = toString();
		
		System.out.println(prefix + type + " " + funcName);
	}

	
	// ******************************************************************** //
	// Private Data.
	// ******************************************************************** //

	// The name for this function.
	private String funcName = null;

	// The parameters.
	private NameList funcParams = null;

	// The statements that make up the body of this function.
	private FormulaNode funcBody = null;
	
}

