/**
 * Dazzle: a screen timeout control widget for Android.
 * <br>Copyright 2010 Dmitry DELTA Malykhanov
 *             for Ian Cameron Smith Dazzle widget
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

import java.util.HashMap;

import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.provider.Settings;
import android.widget.RemoteViews;

public class ScreenAlwaysOnSettings {

	private ScreenAlwaysOnSettings() { }
	
	private final static int SCREEN_ALWAYS_ON_TIMEOUT = -1;
	
	private final static int SCREEN_DEFAULT_TIMEOUT = 60000;
	
	private final static String SHADOW_SCREEN_OFF_TIMEOUT
			= "Settings.System.SCREEN_OFF_TIMEOUT";
	
	private static boolean isEnabled(final ContentResolver resolver) {
		return Settings.System.getInt(resolver,
				Settings.System.SCREEN_OFF_TIMEOUT, SCREEN_DEFAULT_TIMEOUT) < 0;
	}
	
	private static void setEnabled(final Context context,
			final boolean enabled)
	{
		final ContentResolver resolver = context.getContentResolver();
		final int timeout = Settings.System.getInt(resolver,
				Settings.System.SCREEN_OFF_TIMEOUT, SCREEN_DEFAULT_TIMEOUT);
		if (enabled) {
			if( SCREEN_ALWAYS_ON_TIMEOUT != timeout ) {
				setLastKnownTimeout(context, timeout);
				Settings.System.putInt(resolver,
						Settings.System.SCREEN_OFF_TIMEOUT, SCREEN_ALWAYS_ON_TIMEOUT);
			}
		} else {
			final int lastKnownTimeout = getLastKnownTimeout(context);
			Settings.System.putInt(resolver,
					Settings.System.SCREEN_OFF_TIMEOUT, lastKnownTimeout);
		}
	}

	private static int getLastKnownTimeout(final Context context) {
		return DazzleProvider.getShadowPreferences(context).getInt(
				SHADOW_SCREEN_OFF_TIMEOUT, SCREEN_DEFAULT_TIMEOUT);
	}
	
	private static void setLastKnownTimeout(final Context context, final int timeout) {
		SharedPreferences prefs = DazzleProvider.getShadowPreferences(context);
		final int value = prefs.getInt(SHADOW_SCREEN_OFF_TIMEOUT, SCREEN_DEFAULT_TIMEOUT);
		if( value != timeout ) {
			prefs.edit().putInt(SHADOW_SCREEN_OFF_TIMEOUT, timeout).commit();
		}
	}

	private static HashMap<Class<?>, Boolean> observer = new HashMap<Class<?>, Boolean>();
	
	static void subscribe(final Context context, final int widgetId, final Class<?> providerClass) {
		if( null == observer.get(providerClass) ) {
			DazzleProvider.registerSettingsObserver(context,
					widgetId,
					Settings.System.getUriFor(Settings.System.SCREEN_OFF_TIMEOUT),
					"Settings.System.SCREEN_OFF_TIMEOUT",
					providerClass);
			observer.put(providerClass, Boolean.TRUE);
		}
		
	}
	
	/**
     * Toggle the current state.
     * 
     * @param   context     The context we're running in.
     */
    static void toggle(Context context) {
    	setEnabled(context, !isEnabled(context.getContentResolver()));
    }

    /**
     * Set the indicator widget to represent our current state.
     * 
     * @param   context     The context we're running in.
     * @param   views       The widget view to modify.
     * @param   widget      The ID of the indicator widget.
     */
    static void setWidget(Context context, RemoteViews views, int widget) {
    	views.setImageViewResource(widget,
    			isEnabled(context.getContentResolver())
    					? R.drawable.green : R.drawable.grey);
    }
    
}
// EOF
