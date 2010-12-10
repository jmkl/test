/**
 * Dazzle: system/secure settings observer with custom logging
 * <br>Copyright 2010 Dmitry DELTA Malykhanov
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

package org.hermit.dazzle.observers;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.hermit.dazzle.DazzleProvider;

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;
import android.net.Uri;
import android.util.Log;

/**
 * Wrapper around content observer, sends update request for widgets listening
 * for settings changes.
 * 
 */
public class SettingsObserver extends ContentObserver {

	private final Context context;
	
	private final Uri uri;
	
	protected final String logMessage;
	
	// notifications map, provider -> widget ids
	private final HashMap<String, WidgetInfo> providers
			= new HashMap<String, WidgetInfo>();
	
	private class WidgetInfo {
		HashSet<Integer> ids = new HashSet<Integer>();
		int[] array;
		
		// trying to keep most frequently used (send notification) operation
		// less expensive...
		protected void sync() {
			array = new int[ids.size()];
			int i = 0;
			for( int id : ids ) {
				array[i++] = id;
			}
		}
	}

	SettingsObserver(final Context context,
			final Uri uri, final String logMessage) {
		super(null);
		this.context = context;
		this.uri = uri;
		this.logMessage = logMessage;
		Log.d(ObserverService.TAG, "Subscribing to " +
				logMessage + " changes");
		context.getContentResolver().registerContentObserver(uri, true, this);
	}

	void registerProviderClass(final String providerClass) {
		Log.d(ObserverService.TAG, "Tracking updates for " + providerClass
				+ " " + uri);
		final WidgetInfo clients = providers.put(providerClass, new WidgetInfo());
		// normally provider should register only once, hence almost no overhead
		if( null != clients && clients.ids.size() > 0 ) {
			// re-registered provider class, keep clients
			providers.put(providerClass, clients);
			// no need to update widgets here
		}
	}
	
	// we trust the caller to always invoke registerProviderClass() first
	void registerWidget(final String providerClass, final int... widgetId) {
		if( widgetId.length > 0 ) {
			final WidgetInfo widgets = providers.get(providerClass);
			for( final int id : widgetId ) {
				widgets.ids.add(id);
			}
			widgets.sync();
		}
	}
	
	void unregisterWidget(final String providerClass, final int widgetId) {
		final WidgetInfo widgets = providers.get(providerClass);
		widgets.ids.remove(widgetId);
		if( 0 == widgets.ids.size() ) {
			providers.remove(providerClass);
		} else {
			widgets.sync();
		}
	}
	
	int getProvidersCount() {
		return providers.size();
	}
	
	@Override
	public void onChange(boolean selfChange) {
		Log.d(ObserverService.TAG, logMessage + " changed");
		for( Map.Entry<String, WidgetInfo> provider : providers.entrySet() ) {
	    	final Intent intent = new Intent();
	    	intent.addCategory(DazzleProvider.CATEGORY_UPDATE_WIDGET);
	    	intent.setClassName(context, provider.getKey());
	    	intent.setData(uri);
	    	intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS,
	    			provider.getValue().array);
	    	context.sendBroadcast(intent);
		}
	}

	void unsubscribe() {
		context.getContentResolver().unregisterContentObserver(this);
		Log.d(ObserverService.TAG, "Unsubscribed from "
				+ logMessage + " changes");
	}
	
}
// EOF
