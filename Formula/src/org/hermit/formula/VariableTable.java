
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


package org.hermit.formula;


import java.util.ArrayList;
import java.util.HashMap;


/**
 * A view which displays and runs a formula.
 */
public class VariableTable
{

	// ******************************************************************** //
	// Constructor.
	// ******************************************************************** //

	/**
	 * Construct a variable table.
	 */
	public VariableTable() {
		// Create the table of variables.
		contextStack = new ArrayList<HashMap<String, Double>>();

		// Push an initial, global context.
		push();
		
		reInit();
	}


	/**
	 * Re-initialize this variable table.
	 */
	public void reInit() {
		if (contextStack.size() != 1)
			throw new RuntimeException("Re-initialized var table with " +
									   contextStack.size() + " active contexts");
		
		currentContext.clear();
		
		// Pre-set some useful variables.
		set("pi", Math.PI);
	}


	// ******************************************************************** //
	// Context Handling.
	// ******************************************************************** //

	/**
	 * Push a level of context in the variable table.  Variables set
	 * subsequently will be set in the new context, and will vanish when
	 * popContext() is called.
	 */
	void push() {
		currentContext = new HashMap<String, Double>();
		contextStack.add(currentContext);
	}


	/**
	 * Pop a level of context off the variable table.
	 */
	void pop() {
		int size = contextStack.size() - 1;
		contextStack.remove(size);
		if (size > 0)
			currentContext = contextStack.get(size - 1);
		else
			currentContext = null;
	}


	/**
	 * Make a named variable local to the current context.  A definition of the
	 * variable will be added to the current top context, shadowing any
	 * definitions, if any, in lower contexts.  The variable will not have
	 * a value.
	 * 
	 * One effect is that if the formula later sets a variable
	 * of this name, it sets this one rather than creating a local copy
	 * in the then-current context (unless of course it declares a local
	 * with that name); but if the formula doesn't set it, it still won't
	 * have a value.
	 * 
	 * @param	name			Name of the variable to declare.
	 */
	void declare(String name) {
		currentContext.put(name, null);
	}
	

	/**
	 * Make a named variable local to the current context.  A definition of the
	 * variable will be added to the current top context, shadowing any
	 * definitions, if any, in lower contexts, and it will be set to the
	 * given value.
	 * 
	 * @param	name			Name of the variable to make local.
	 * @param	value			The initial value to set.
	 */
	void local(String name, double value) {
		currentContext.put(name, value);
	}
	

	/**
	 * Delete a named variable from the current context.  This causes
	 * the variable not to exist in this context; it may reveal a variable
	 * with the same name in a lower context.
	 * 
	 * <p>If the variable doesn't exist in the current context, nothing
	 * happens.
	 * 
	 * @param	name			Name of the variable to delete.
	 */
	void delete(String name) {
		currentContext.remove(name);
	}
	

	// ******************************************************************** //
	// Variable Handling.
	// ******************************************************************** //

	/**
	 * Determine whether a named variable exists.  Note that this doesn't
	 * check whether it has a value.
	 * 
	 * <p>Note that it's quite inefficient to call this method and then to
	 * call get(); just call get() and see if the return is null.
	 * 
	 * @param	name			Name of the variable to check.
	 * @return					true if it exists in some active context,
	 *							regardless of whether it has a value; false
	 *							if it does not exist.
	 */
	boolean exists(String name) {
		HashMap<String, Double> context = find(name);
		return context != null;
	}
	

	/**
	 * Determine whether a named variable exists, and if so whether the
	 * nearest instance of that variable has a value.
	 * 
	 * <p>Note that it's quite inefficient to call this method and then to
	 * call get(); just call get() and see if the return is null.
	 * 
	 * @param	name			Name of the variable to check.
	 * @return					true if it exists and has a value in some
	 * 							active context; false if it does not exist,
	 * 							or if the instance of the variable in the
	 * 							nearest enclosing context doesn't have a value.
	 */
	boolean isSet(String name) {
		return get(name) != null;
	}
	

	/**
	 * Get the value of a named variable.
	 * 
	 * @param	name			Name of the variable to get.
	 * @return					If the variable exists in some active context,
	 * 							and the instance of the variable in the
	 * 							nearest enclosing context has a value, then
	 * 							that value is returned; otherwise null.
	 */
	Double get(String name) {
		HashMap<String, Double> context = find(name);
		return context != null ? context.get(name) : null;
	}
	

	/**
	 * Set the state of a named variable.  If it already exists in some
	 * active context, that copy of the variable will be set; otherwise,
	 * it will be created in the current context and set.
	 * 
	 * @param	name			Name of the variable to set.
	 * @param	value			The value to set.
	 */
	void set(String name, double value) {
		HashMap<String, Double> context = find(name);
		if (context != null)
			context.put(name, value);
		else
			currentContext.put(name, value);
	}
	

	// ******************************************************************** //
	// Special Variables.
	// ******************************************************************** //

	/**
	 * Determine whether a return value exists in the current context.
	 * 
	 * @return					true if it exists, false if not.
	 */
	boolean hasReturn() {
		// Unlike regular variables, this only looks in the top context.
		return currentContext.containsKey("#return");
	}
	

	/**
	 * Get the return value in the current context.
	 * 
	 * @return					The value, null if it is not set.
	 */
	Double getReturn() {
		// Unlike regular variables, this only looks in the top context.
		return currentContext.get("#return");
	}
	

	/**
	 * Set the return value in the current context.
	 * 
	 * @param	name			Name of the variable to set.
	 * @param	value			The value to set.
	 */
	void setReturn(double value) {
		// Unlike regular variables, this always sets it in the top context.
		currentContext.put("#return", value);
	}
	

	// ******************************************************************** //
	// Utilities.
	// ******************************************************************** //

	/**
	 * Find a named variable in the context stack.
	 * 
	 * @param	name			Name of the variable to find.
	 * @return					If it exists, the context it is in; else null.
	 */
	private HashMap<String, Double> find(String name) {
		for (int i = contextStack.size() - 1; i >= 0; --i) {
			HashMap<String, Double> context = contextStack.get(i);
			if (context.containsKey(name))
				return context;
		}
		
		return null;
	}
	

	// ******************************************************************** //
	// Class Data.
	// ******************************************************************** //

	// Debugging tag.
	@SuppressWarnings("unused")
	private static final String TAG = "formula";


	// ******************************************************************** //
	// Private Data.
	// ******************************************************************** //

	// The table of variables in the current formula.
	private ArrayList<HashMap<String, Double>> contextStack;

	// The current topmost active context.
	private HashMap<String, Double> currentContext;

}

