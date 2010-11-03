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
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;
import android.widget.RemoteViews;

public class PhoneRadioSettings extends AbsCommonTelephonySettings {

	private PhoneRadioSettings() { }

	public static void onBoot(final Context context, final Intent intent) {
        final SharedPreferences prefs = DazzleProvider.getShadowPreferences(context);
        if (prefs.getBoolean(SHADOW_PHONE_RADIO, true)) {
        	if (isMobileDataEnabledInSettings(context)) {
        		restoreMobileDataState(context, prefs);
        	}
        } else {
        	setRadioEnabled(context, false);
        }
	}
    
	private static void restoreMobileDataState(final Context context,
			final SharedPreferences prefs)
	{
    	final boolean mobileDataState
				= prefs.getBoolean(SHADOW_MOBILE_DATA, true);
    	Log.d(DazzleProvider.TAG, "Restoring last known mobile data state: "
    			+ (mobileDataState ? "enabled" : "disabled"));
    	setMobileDataEnabled(context, mobileDataState);
	}

	private static void setRadioEnabled(final Context context, final boolean enabled) {
        if (transition.inProgress()) {
        	Log.w(DazzleProvider.TAG, "Radio is already in transition "
        			+ transition + " when requesting "
        			+ (enabled ? "ON" : "OFF"));
        }
		try {
			final Object iTelephony = getITelephony(context);
			final Method toggleRadioOnOff
				= iTelephony.getClass().getDeclaredMethod("setRadio", boolean.class);
			toggleRadioOnOff.invoke(iTelephony, enabled);
	        transition.start(enabled);
	        final SharedPreferences prefs = DazzleProvider.getShadowPreferences(context);
	        if (enabled != prefs.getBoolean(SHADOW_PHONE_RADIO, true)) {
	        	prefs.edit().putBoolean(SHADOW_PHONE_RADIO, enabled).commit();
	        }
	        if (enabled && isMobileDataEnabledInSettings(context)) {
	        	restoreMobileDataState(context, prefs);
	        }
	        Log.d(DazzleProvider.TAG, "setRadioEnabled: Starting radio transition "
	        		+ transition);
		} catch(Exception e) {
			transition.stop(!enabled);
			Log.e(DazzleProvider.TAG, "setRadioEnabled(" + enabled + ")", e);
		}
	}

	/**
     * Toggle the current state.
     * 
     * @param   context     The context we're running in.
     */
    static void toggle(final Context context) {
        Log.i(DazzleProvider.TAG, "toggle radio data");
        
        setRadioEnabled(context, !isRadioOn(context));
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
				// TODO: watch the system mobile data setting and adjust the internal settings accordingly
			} else {
				indicator = R.drawable.orange;
			}
		} else {
			indicator = radioState ? R.drawable.green : R.drawable.grey;
		}
        views.setImageViewResource(widget, indicator);
    }
    
    private static Transition transition = new Transition();

}
// EOF
