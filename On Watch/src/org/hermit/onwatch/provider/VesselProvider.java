
/**
 * On Watch: sailor's watchkeeping assistant.
 * <br>Copyright 2009-2010 Ian Cameron Smith
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


package org.hermit.onwatch.provider;


import org.hermit.android.provider.TableProvider;


/**
 * Content provider for stored vessel (boat and crew) data.
 */
public class VesselProvider
    extends TableProvider
{
    
    // ******************************************************************** //
    // Constructor.
    // ******************************************************************** //
 
    /**
     * Create an instance of this content provider.
     */
    public VesselProvider() {
        super(new VesselSchema());
    }
    
}

