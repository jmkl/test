
/**
 * broadtest: system broadcast dumper.
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


package org.hermit.broadtest.provider;


import org.hermit.android.provider.TableProvider;


/**
 * Content provider for stored broadcast events.
 */
public class BroadcastProvider
	extends TableProvider
{

    // ******************************************************************** //
    // Constructor.
    // ******************************************************************** //

    /**
     * Create an instance of this content provider.
     */
    public BroadcastProvider() {
        super(new BroadcastSchema());
    }
    
}

