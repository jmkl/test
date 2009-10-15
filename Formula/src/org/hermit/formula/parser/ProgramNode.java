
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
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;


/**
 * A list of names (e.g. parameters) in a formula.
 */
public class ProgramNode
	extends FormulaNode
{

	// ******************************************************************** //
	// Constructor.
	// ******************************************************************** //

	/**
	 * Construct a program node.
	 */
	public ProgramNode() {
		super(-1);
	}

	
	// ******************************************************************** //
	// Tree Building.
	// ******************************************************************** //
	
	/**
	 * Add an input definition to the program.
	 * 
	 * @param	n			The definition to add.
	 */
	void addInput(InputNode n) {
		inputList.add(n);
	}

	
	/**
	 * Add an output definition to the program.
	 * 
	 * @param	n			The definition to add.
	 */
	void addOutput(OutputNode n) {
		outputList.add(n);
	}

	
	/**
	 * Add a function definition to the program.
	 * 
	 * @param	n			The definition to add.
	 */
	void addFunc(FuncNode n) {
		funcList.put(n.getName(), n);
	}


	// ******************************************************************** //
	// Accessors.
	// ******************************************************************** //
	
	/**
	 * Get the input definitions of the program.
	 * 
	 * @return				An iterator over the definitions.
	 */
	public Iterator<InputNode> getInputs() {
		return inputList.iterator();
	}

	
	/**
	 * Get the output definitions of the program.
	 * 
	 * @return				An iterator over the definitions.
	 */
	public Iterator<OutputNode> getOutputs() {
		return outputList.iterator();
	}


	/**
	 * Get the function definitions of the program.
	 * 
	 * @return				An iterator over the definitions.
	 */
	public Collection<FuncNode> getFuncs() {
		return funcList.values();
	}

	
	/**
	 * Get the function definition with the given name.
	 * 
	 * @param	name		The name of the function we want.
	 * @return				The definition, if it exists, or null.
	 */
	public FuncNode getFunc(String name) {
		return funcList.get(name);
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
	
		for (InputNode n : inputList)
			n.dump(prefix + "  ");
		for (OutputNode n : outputList)
			n.dump(prefix + "  ");
		Set<String> funcs = funcList.keySet();
		for (String name : funcs) {
			FuncNode n = funcList.get(name);
			System.out.println(prefix + "  " + "Function " + name);
			n.dump(prefix + "    ");
		}
	}


	/**
	 * Dump this node with a given prefix.
	 * 
	 * @param	prefix		Prefix for each line of output.
	 */
	@Override
	public void dumpThis(String prefix) {
		System.out.println(prefix + "Program");
	}


	// ******************************************************************** //
	// Private Data.
	// ******************************************************************** //

	// The lists of inputs and outputs in this program.
	private ArrayList<InputNode> inputList = new ArrayList<InputNode>();
	private ArrayList<OutputNode> outputList = new ArrayList<OutputNode>();

	// The functions in this program, organized by name.
	private HashMap<String, FuncNode> funcList = new HashMap<String, FuncNode>();
	
}

