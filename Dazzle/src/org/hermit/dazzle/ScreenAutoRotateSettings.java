/**
 * Dazzle: a screen auto rotation control widget for Android.
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
import android.provider.Settings;
import android.widget.RemoteViews;

public class ScreenAutoRotateSettings {
	
	private ScreenAutoRotateSettings() { }

	private static boolean isEnabled(final ContentResolver resolver) {
		return 1 == Settings.System.getInt(resolver,
				Settings.System.ACCELEROMETER_ROTATION, 1);
	}
	
	private static void setEnabled(final ContentResolver resolver,
			final boolean enabled)
	{
		Settings.System.putInt(resolver,
				Settings.System.ACCELEROMETER_ROTATION, enabled ? 1 : 0);
	}

	private static HashMap<Class<?>, Boolean> observer = new HashMap<Class<?>, Boolean>();
	
	static void subscribe(final Context context, final int widgetId, final Class<?> providerClass) {
		if( null == observer.get(providerClass) ) {
			DazzleProvider.registerSettingsObserver(context,
					widgetId,
					Settings.System.getUriFor(Settings.System.ACCELEROMETER_ROTATION),
					"Settings.System.ACCELEROMETER_ROTATION",
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
    	final ContentResolver resolver = context.getContentResolver();
    	setEnabled(resolver, !isEnabled(resolver));
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
