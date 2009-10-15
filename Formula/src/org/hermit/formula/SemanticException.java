
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


/**
 * An exception representing a runtime semantic error in the program.
 */
public class SemanticException
	extends Exception
{

	/**
	 * Construct an exception with an error message.
	 * 
	 * @param	msg			The error message.
	 */
	public SemanticException(String msg) {
		super(msg);
	}

	private static final long serialVersionUID = 7892966480988858315L;

}

