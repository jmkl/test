
/**
 * PlugEdit: a level editor for Plughole.
 * <br>Copyright 2010 Ian Cameron Smith
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


package org.hermit.plughole.editor;


/**
 * Application-initiated exception.
 * 
 * <p>This exception class represents an error caused by a problem
 * within the application; ie. something that shouldn't happen.
 * This error should be reported as an internal error, with appropriate
 * debugging.
 *
 * <p>As a subclass of RuntimeException, this exception is not
 * required to be declared in "throws" clauses.  It should only be
 * used for cases that "should never happen".
 */
public class AppException
    extends RuntimeException
{

    /**
     * Create an exception with a String message.
     * 
     * @param   s           Error message.
     */
    public AppException(String s) { super(s); }

    /**
     * UUID.
     */
    private static final long serialVersionUID = -3788770780856162659L;

}

