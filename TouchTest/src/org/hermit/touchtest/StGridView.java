
/**
 * Touch Test: a multi-touch test app for Android.
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


package org.hermit.touchtest;


import android.content.Context;
import android.view.MotionEvent;


/**
 * Single-touch implementation of the touch test view.
 */
class StGridView
    extends GridView
{

    // ******************************************************************** //
    // Constructor.
    // ******************************************************************** //

	/**
	 * Create a GridView instance.
	 * 
	 * @param	context  The application context we're running in.
	 */
    public StGridView(Context context) {
        super(context);
    }

    
    // ******************************************************************** //
    // Input Handling.
    // ******************************************************************** //
    
    /**
	 * Handle touchscreen input.
	 * 
     * @param	event		The MotionEvent object that defines the action.
     * @return				True if the event was handled, false otherwise.
	 */
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        final int action = event.getAction();

        // Get the action, and the pointer.  The pointer ID is always
        // zero.
        final int pact = action & MotionEvent.ACTION_MASK;
        Pointer rec = getPointer(0);

        // Update the up/down state of this pointer.
        switch (pact) {
        case MotionEvent.ACTION_DOWN:
            {
                rec.seen = true;
                rec.down = true;
                rec.x = event.getX();
                rec.y = event.getY();
                rec.size = event.getSize();
                
                rec.trailStart = 0;
                rec.trailLen = 0;
            }
            break;
        case MotionEvent.ACTION_UP:
            rec.down = false;
            break;
        case MotionEvent.ACTION_MOVE:
            {
                rec.x = event.getX();
                rec.y = event.getY();
                rec.size = event.getSize();
                
                int nh = event.getHistorySize();
                for (int h = 0; h < nh; ++h) {
                    float hx = event.getHistoricalX(h);
                    float hy = event.getHistoricalY(h);
                    addPoint(rec, hx, hy);
                }
                addPoint(rec, rec.x, rec.y);
            }
            break;
        }

        postUpdate();
        
        return true;
    }

}

