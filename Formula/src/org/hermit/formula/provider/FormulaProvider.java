
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


package org.hermit.formula.provider;


import org.hermit.android.provider.TableProvider;


/**
 * Provides access to a database of formulae.
 */
public class FormulaProvider
	extends TableProvider
{
    
    // ******************************************************************** //
    // Constructor.
    // ******************************************************************** //
 
    /**
     * Create an instance of this content provider.
     */
    public FormulaProvider() {
        super(new FormulaSchema());
    }

}

