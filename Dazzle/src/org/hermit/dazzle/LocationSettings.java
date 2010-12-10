/**
 * Dazzle: a GPS control widget for Android.
 * <br>Copyright 2010 Dmitry DELTA Malykhanov
 *             for Ian Cameron Smith Dazzle widget
 *             special thanks to Sergej Shafarenka for inspiration
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

import android.content.Context;
import android.content.Intent;
import android.location.LocationManager;
import android.net.Uri;
import android.provider.Settings;
import android.util.Log;
import android.widget.RemoteViews;

public class LocationSettings {

	private LocationSettings() { }
	
	private static HashMap<Class<?>, Boolean> observer = new HashMap<Class<?>, Boolean>();
	
	static void subscribe(final Context context, final int widgetId, final Class<?> providerClass) {
		if( null == observer.get(providerClass) ) {
			DazzleProvider.registerSettingsObserver(context,
					widgetId,
					Settings.Secure.getUriFor(Settings.Secure.LOCATION_PROVIDERS_ALLOWED),
					"Settings.Secure.LOCATION_PROVIDERS_ALLOWED",
					providerClass);
			observer.put(providerClass, Boolean.TRUE);
		}
		
	}
	
	private static boolean getGpsState(final Context context) {
		final LocationManager manager = (LocationManager)
			context.getSystemService(Context.LOCATION_SERVICE);
		return manager.isProviderEnabled(LocationManager.GPS_PROVIDER);
	}

	private static void toggleGps(final Context context) {
		final Intent intent = new Intent();
		intent.setClassName("com.android.settings",
				"com.android.settings.widget.SettingsAppWidgetProvider");
		intent.addCategory(Intent.CATEGORY_ALTERNATIVE);
		intent.setData(Uri.parse("custom:3"));
		context.sendBroadcast(intent);
	}

    /**
     * Toggle the current state.
     * 
     * @param   context     The context we're running in.
     */
    static void toggle(final Context context) {
        Log.i(DazzleProvider.TAG, "toggle gps location");
        
        try {
        	toggleGps(context);
        } catch(Exception e) {
        	Log.e(DazzleProvider.TAG, "toggleGps", e);
            Intent gpsIntent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
            gpsIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(gpsIntent);
        }
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
        		getGpsState(context) ? R.drawable.blue : R.drawable.grey);
    }
    
}
// EOF
