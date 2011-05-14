/**
 * Dazzle: system/secure settings observer service
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
import java.util.Map;


import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.net.Uri;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

/**
 * Service class to hold the content observers and let the platform know
 * that we really need them to stay around.
 * 
 */
public class ObserverService extends Service {

	// Do we need a separate thread to run observers?
	
	/*package*/ final static String TAG = "Dazzle/OSvc";
	
	private final static String PACKAGE
			= ObserverService.class.getPackage().getName();
	
	public final static String EXTRA_LOG_MESSAGE
			= PACKAGE + ".logMessage";

	public final static String EXTRA_PROVIDER_CLASS
			= PACKAGE + ".providerClass";
	
	public final static String EXTRA_WIDGET_ID
			= PACKAGE + ".widgetId";

	public final static String EXTRA_ACTION
			= PACKAGE + ".action";

	public final static String ACTION_REMOVE = PACKAGE + ".action.remove";
	
	public class LocalBinder extends Binder {
		ObserverService getService() {
			return ObserverService.this;
		}
	}
	private final IBinder binder = new LocalBinder();
	
	@Override
	public IBinder onBind(Intent intent) {
		return binder;
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
        // intent can be null if restared
        if( null == intent ) {
        	// sticky service restart, just leave it running, observers
        	// will be re-registered soon
            return START_STICKY;
        }
        // TODO: keep observers map (Uri + Provider class) in persistent storage
        // and load it in onCreate (and start the server if there are any)
		final Uri uri = intent.getData();
		SettingsObserver observer;
		String action = intent.getStringExtra(EXTRA_ACTION);
		if( ACTION_REMOVE.equals(action) ) {
			if( null != uri ) {
				observer = observers.remove(uri);
				if( null != observer ) {
					observer.unsubscribe();
				}
				if( 0 == observers.size() ) {
					stopSelf();
				}
			} else {
				unregisterClient(intent);
			}
		} else if( null == (observer = observers.get(uri)) ) {
			if( null != uri ) {
				Log.d(TAG, "Registering new observer for " + uri);
				observer = new SettingsObserver(
						getApplicationContext(), uri,
						intent.getStringExtra(EXTRA_LOG_MESSAGE));
				registerClient(intent, observer);
				observers.put(uri, observer);
			} else {
				Log.e(TAG, "Null URI passed in for action " + action);
			}
		} else { // observer is not null here, register new widget id
			registerClient(intent, observer);
		}
		return START_STICKY;
	}
	
	private void registerClient(final Intent intent, final SettingsObserver observer) {
		final String providerClass = intent.getStringExtra(EXTRA_PROVIDER_CLASS);
		observer.registerProviderClass(providerClass);
		final int widgetId = intent.getIntExtra(EXTRA_WIDGET_ID,
				AppWidgetManager.INVALID_APPWIDGET_ID);
		if( AppWidgetManager.INVALID_APPWIDGET_ID != widgetId ) {
			observer.registerWidget(providerClass, widgetId);
		}
	}
	
	private void unregisterClient(final Intent intent) {
		final String providerClass = intent.getStringExtra(EXTRA_PROVIDER_CLASS);
		final int widgetId = intent.getIntExtra(EXTRA_WIDGET_ID,
				AppWidgetManager.INVALID_APPWIDGET_ID);
		if( AppWidgetManager.INVALID_APPWIDGET_ID != widgetId ) {
			for( final Map.Entry<Uri, SettingsObserver> entry : observers.entrySet() ) {
				final SettingsObserver observer = entry.getValue();
				Log.d(TAG, "Removing widget " + widgetId + " from " + providerClass);
				observer.unregisterWidget(providerClass, widgetId);
				if( 0 == observer.getProvidersCount() ) {
					observer.unsubscribe();
					Log.d(TAG, "Removing unused observer for " + entry.getKey());
					observers.remove(entry.getKey());
				}
			}
		}
	}

	@Override
	public void onDestroy() {
        // cleanup remaining observers
        for( final SettingsObserver so : observers.values() ) {
        	so.unsubscribe();
        }
        observers.clear();
	}

    private final static Map<Uri, SettingsObserver> observers
			= new HashMap<Uri, SettingsObserver>();
    
}
// EOF
