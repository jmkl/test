
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
 * User-initiated exception.
 * 
 * <p>This exception class represents an error based on what the
 * user has asked us to do; such as loading a non-existant file.
 * This error should be reported as a simple information message.
 */
public class UserException
    extends Exception
{

    /**
     * Create an exception with a String message.
     * 
     * @param   s           Error message.
     */
    public UserException(String s) { super(s); }

    /**
     * UUID.
     */
    private static final long serialVersionUID = -3290427156469123866L;

}

