/**
 * Dazzle: a phone radio control widget for Android.
 * <br>Copyright 2010 Dmitry DELTA Malykhanov
 * 		for Ian Cameron Smith Dazzle widget
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

package org.hermit.dazzle;

import java.lang.reflect.Method;

import android.content.Context;
import android.util.Log;
import android.widget.RemoteViews;

public class PhoneRadioSettings extends AbsCommonTelephonySettings {

	private PhoneRadioSettings() { }

	private static void toggleRadioOnOff(final Context context) {
		final boolean enable = !isRadioOn(context);
        if (transition.inProgress()) {
        	Log.w(DazzleProvider.TAG, "Radio is already in transition "
        			+ transition + " when requesting "
        			+ (enable ? "ON" : "OFF"));
        }
		try {
			final Object iTelephony = getITelephony(context);
			final Method toggleRadioOnOff
				= iTelephony.getClass().getDeclaredMethod("toggleRadioOnOff");
			toggleRadioOnOff.invoke(iTelephony);
	        transition.start(enable);
	        Log.d(DazzleProvider.TAG, "Starting radio transition " + transition);
		} catch(Exception e) {
			transition.stop(!enable);
			Log.e(DazzleProvider.TAG, "toggleRadioOnOff()", e);
		}
	}

	/**
     * Toggle the current state.
     * 
     * @param   context     The context we're running in.
     */
    static void toggle(final Context context) {
        Log.i(DazzleProvider.TAG, "toggle radio data");
        
        toggleRadioOnOff(context);
    }

    /**
     * Set the indicator widget to represent our current state.
     * 
     * @param   context     The context we're running in.
     * @param   views       The widget view to modify.
     * @param   widget      The ID of the indicator widget.
     */
    static void setWidget(Context context, RemoteViews views, int widget) {
		final boolean radioState = isRadioOn(context);
		final int indicator;
		if (transition.inProgress()) {
			if( radioState == transition.to) {
				transition.stop(radioState);
				indicator = radioState ? R.drawable.green : R.drawable.grey;
				// TODO: check mobile data persistent setting and apply if required
				// TODO: watch the system mobile data setting and adjust the internal settings accordingly
			} else {
				indicator = R.drawable.orange;
			}
		} else {
			indicator = radioState ? R.drawable.green : R.drawable.grey;
		}
        views.setImageViewResource(widget, indicator);
    }
    
    /*
     * There are no useful callbacks/broadcasts about the phone radio state.
     * Hence we try to track the progress manually.
     */
    private static class Transition {
    	private boolean from;
    	private boolean to;
    	
    	public void start(final boolean target) {
    		from = !target;
    		to = target;
    	}
    	
    	public void stop(final boolean state) {
    		from = to = state;
    	}

    	public boolean inProgress() {
    		return from != to;
    	}
    	
    	public String toString() {
    		return (from ? "ON" : "OFF") + " -> " + (to ? "ON" : "OFF"); 
    	}
    }
    
    private static Transition transition = new Transition();
    
}
// EOF
