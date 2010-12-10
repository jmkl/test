
/**
 * Dazzle: a screen brightness control widget for Android.
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


package org.hermit.dazzle;


import org.hermit.android.core.Errors;
import org.hermit.dazzle.observers.ObserverService;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.appwidget.AppWidgetProviderInfo;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;


/**
 * The Dazzle widget provider.
 */
public abstract class DazzleProvider
    extends AppWidgetProvider
{
    
    // ******************************************************************** //
    // Public Constants.
    // ******************************************************************** //

    // Debugging tag.
    static final String TAG = "Dazzle";
    
    // system-wide persistent settings
    private final static String SHADOW_SETTINGS = "settings.system";

    // The controls we support.
    enum Control {
        RINGER(R.id.dazzle_ringer, "enableRinger"),
        OTRINGER(R.id.dazzle_otringer, "enableOtRinger"),
        RADIO_SETTINGS(R.id.dazzle_radio_settings, "enableRadioSettings"),
        MOBILE_DATA(R.id.dazzle_mobile_data, "enableRadio"),
        PHONE_RADIO(R.id.dazzle_phone_radio, "enablePhoneRadio"),
        WIFI(R.id.dazzle_wifi, "enableWifi"),
        WIFI_AP(R.id.dazzle_wifi_ap, "enableWifiAp"),
        BLUETOOTH(R.id.dazzle_bluetooth, "enableBluetooth"),
        GPS(R.id.dazzle_gps, "enableGps"),
        AIRPLANE(R.id.dazzle_airplane, "enableAirplane"),
        SYNC(R.id.dazzle_sync, "enableSync"),
        BRIGHTNESS(R.id.dazzle_brightness, "enableBrightness"),
        OTBRIGHTNESS(R.id.dazzle_otbrightness, "enableOtBrightness"),
        BRIGHTAUTO(R.id.dazzle_brightauto, "enableBrightauto"),
        OTBRIGHTAUTO(R.id.dazzle_otbrightauto, "enableOtBrightauto"),
        AUTORORATE(R.id.dazzle_autorotate, "enableAutoRotate"),
        SCREEN_ALWAYS_ON(R.id.dazzle_screen_always_on, "enableScreenAlwaysOn");
        
        Control(int id, String pref) {
            this.id = id;
            this.pref = pref;
        }

        PendingIntent createIntent(Context context, Class<?> clazz) {
            Intent i = new Intent(context, clazz);
            i.addCategory(Intent.CATEGORY_ALTERNATIVE);
            i.setData(Uri.parse("custom:" + ordinal()));

            return PendingIntent.getBroadcast(context, 0, i, 0);
        }

        static Control[] CONTROLS = values();
        int id;
        String pref;
    }

    private final static String PACKAGE_NAME
    		= DazzleProvider.class.getPackage().getName();
    
    public final static String CATEGORY_UPDATE_WIDGET
    		= PACKAGE_NAME + ".CATEGORY_UPDATE_WIDGET";
    
    static void registerSettingsObserver(final Context context, final int widgetId,
    		final Uri uri, final String message, final Class<?> providerClass)
    {
    	final Intent intent = new Intent(context, ObserverService.class);
    	intent.setData(uri);
    	intent.putExtra(ObserverService.EXTRA_LOG_MESSAGE, message);
    	intent.putExtra(ObserverService.EXTRA_PROVIDER_CLASS, providerClass.getName());
    	intent.putExtra(ObserverService.EXTRA_WIDGET_ID, widgetId);
    	context.startService(intent);
    }
    
    private static void unregisterSettingsObserver(final Context context,
    		final int widgetId, final Class<?> providerClass)
    {
    	final Intent intent = new Intent(context, ObserverService.class);
    	intent.putExtra(ObserverService.EXTRA_PROVIDER_CLASS, providerClass.getName());
    	intent.putExtra(ObserverService.EXTRA_WIDGET_ID, widgetId);
    	context.startService(intent);
    }
    
    static SharedPreferences getShadowPreferences(final Context context) {
		return context.getSharedPreferences(SHADOW_SETTINGS, Context.MODE_PRIVATE);
    }

    // ******************************************************************** //
    // Widget Lifecycle.
    // ******************************************************************** //

    /**
     * This is called when an instance the App Widget is created for the first
     * time.  For example, if the user adds two instances of your App Widget,
     * this is only called the first time.  If you need to open a new database
     * or perform other setup that only needs to occur once for all App
     * Widget instances, then this is a good place to do it.
     * 
     * <p>When the last AppWidget for this provider is deleted,
     * {@link AppWidgetManager#ACTION_APPWIDGET_DISABLED} is sent by the
     * AppWidget manager, and {@link #onDisabled} is called.  If after that,
     * an AppWidget for this provider is created again, onEnabled() will
     * be called again.
     *
     * @param   context     The context in which this receiver is running.
     * @see     AppWidgetManager#ACTION_APPWIDGET_ENABLED
     */
    @Override
    public void onEnabled(Context context) {
        Log.d(TAG, "onEnabled()");
        
        // When the first widget is created, register our system broadcast
        // receiver.  We don't want to be listening for these if nobody has
        // our widget active.
        // This setting is sticky across reboots, but that doesn't matter,
        // because this will be called after boot if there is a widget
        // instance for this provider.
        PackageManager pm = context.getPackageManager();
        pm.setComponentEnabledSetting(SystemBroadcastReceiver.COMP_NAME,
                                      PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                                      PackageManager.DONT_KILL_APP);
    }


    /**
     * This is called to update the App Widget at intervals defined by the
     * updatePeriodMillis attribute in the AppWidgetProviderInfo.
     * It is also called when the user adds the App Widget, so it should
     * perform the essential setup, such as define event handlers for Views
     * and start a temporary Service, if necessary.  However, if you have
     * declared a configuration Activity, this method is not called when
     * the user adds the App Widget, but is called for the subsequent updates.
     * It is the responsibility of the configuration Activity to perform the
     * first update when configuration is done.
     * 
     * @param   context     The context in which this receiver is running.
     * @param   manager     An {@link AppWidgetManager} object you can call
     *                      {@link AppWidgetManager#updateAppWidget} on.
     * @param   ids         The appWidgetIds for which an update is needed.
     *                      Note that this may be all of the AppWidget
     *                      instances for this provider, or just a subset
     *                      of them.
     * @see     AppWidgetManager#ACTION_APPWIDGET_UPDATE
     */
    @Override
    public void onUpdate(Context context, AppWidgetManager manager, int[] ids) {
        Log.d(TAG, "onUpdate()");
        
        // Update the specified widgets.  Be aware that some of these IDs
        // could be stale.
        final int num = ids.length;
        for (int i = 0; i < num; ++i) {
            final int id = ids[i];
            try {
                updateWidget(context, id);
            } catch (Exception e) {
                Errors.reportException(context, e);
            }
        }

        Log.d(TAG, "onUpdate() DONE");
    }

    /**
     * Called when one or more AppWidget instances have been deleted.
     * Override this method to implement your own AppWidget functionality.
     * 
     * @param   context     The context in which this receiver is running.
     * @param   ids         The appWidgetIds that have been deleted from
     *                      their host.
     * @see     AppWidgetManager#ACTION_APPWIDGET_DELETED
     */
    @Override
    public void onDeleted(Context context, int[] ids) {
        Log.d(TAG, "onDeleted()");

        // Delete all the prefs for each widget instance.  Be aware that
        // some of these IDs could be stale.
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor edit = prefs.edit();
        for (int id : ids) {
            for (DazzleProvider.Control c : DazzleProvider.Control.CONTROLS) {
                edit.remove(c.pref + "-" + id);
            }
            unregisterSettingsObserver(context, id, getClass());
        }
        edit.commit();
    }


    /**
     * This is called when the last instance of your App Widget is deleted
     * from the App Widget host.  This is where you should clean up any work
     * done in onEnabled(Context), such as delete a temporary database.
     * 
     * @param   context     The context in which this receiver is running.
     * @see     AppWidgetManager#ACTION_APPWIDGET_DISABLED
     */
    @Override
    public void onDisabled(Context context) {
        Log.d(TAG, "onDisabled()");

        context.stopService(new Intent(context, ObserverService.class));
        // When the last widget is deleted, stop listening for system
        // broadcasts.
        PackageManager pm = context.getPackageManager();
        pm.setComponentEnabledSetting(SystemBroadcastReceiver.COMP_NAME,
                                      PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                                      PackageManager.DONT_KILL_APP);
    }


    // ******************************************************************** //
    // Input Handling.
    // ******************************************************************** //

    /**
     * Receives and processes a button pressed intent or state change.
     *
     * @param   context     Our context.
     * @param   intent      Indicates the pressed button.
     */
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "onReceive()");

        super.onReceive(context, intent);

        try {
            if (intent.hasCategory(Intent.CATEGORY_ALTERNATIVE)) {
                Uri data = intent.getData();
                Log.d(TAG, "Handle receive " + data);
                int id = Integer.parseInt(data.getSchemeSpecificPart());
                if (id >= 0 && id < Control.CONTROLS.length) {
                    Control control = Control.CONTROLS[id];
                    handleClick(context, control);
                    return; // the widget will update itself in response to a system event
                }
            } else if (intent.hasCategory(DazzleProvider.CATEGORY_UPDATE_WIDGET)) {
            	Log.d(TAG, "Widget update requested via " + CATEGORY_UPDATE_WIDGET);
            	if( MOBILE_DATA_SETTINGS_URI.equals(intent.getData()) ) {
            		MobileDataSettings.updateShadowSettings(context);
            	} else {
	            	final int[] widgetIds = intent.getIntArrayExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS);
	            	if( null != widgetIds && widgetIds.length > 0 ) {
		            	for( final int id : widgetIds ) {
		            		updateWidget(context, id);
		            	}
		            	return;
	            	} // else {
	            		// fall back to update all widgets
	            	// }
            	}
            }
        } catch (Exception e) {
            Errors.reportException(context, e);
        }

        // State changes fall through, but only if it wasn't an update request
        // which has already done by the superclass in super.onReceive(...)
        if( !AppWidgetManager.ACTION_APPWIDGET_UPDATE.equals(intent.getAction()) ) {
        	updateAllWidgets(context);
        }
    }

    private final static Uri MOBILE_DATA_SETTINGS_URI
    		= Settings.Secure.getUriFor("mobile_data");
    
    //com.android.settings/.SecuritySettings
    private void launchSettingsActivity(final Context context,
    		final String packageName, final String className)
    {
    	final Intent intent = new Intent(Intent.ACTION_MAIN);
    	intent.setClassName(packageName, className);
    	intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    	context.startActivity(intent);
    }
    
    private void handleClick(Context context, Control control) {
        Log.d(TAG, "Handle control " + control.toString());

        switch (control) {
        case RINGER:
            Intent ringerIntent = new Intent(context, RingerControl.class);
            ringerIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(ringerIntent);
            break;
        case OTRINGER:
            RingerSettings.toggle(context);
            break;
        case RADIO_SETTINGS:
    		launchSettingsActivity(context,
    				"com.android.phone", "com.android.phone.Settings");
        	break;
        case MOBILE_DATA:
        	if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
        		launchSettingsActivity(context,
        				"com.android.phone", "com.android.phone.Settings");
        	} else {
        		MobileDataSettings.toggle(context);
        	}
            break;
        case PHONE_RADIO:
        	if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
        		launchSettingsActivity(context,
        				"com.android.settings", "com.android.settings.RadioInfo");
        	} else {
        		PhoneRadioSettings.toggle(context);
        	}
        	break;
        case WIFI:
            WiFiSettings.toggle(context);
            break;
        case WIFI_AP:
        	if( Build.VERSION.SDK_INT >= Build.VERSION_CODES.FROYO ) {
        		WiFiApSettings.toggle(context);
        	}
        	break;
        case BLUETOOTH:
            // Can only do Bluetooth from Eclair on.
            if (android.os.Build.VERSION.SDK_INT >=
                                        android.os.Build.VERSION_CODES.ECLAIR)
                BluetoothSettings.toggle(context);
            break;
        case GPS:
        	if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
        		launchSettingsActivity(context,
        				"com.android.settings", "com.android.settings.SecuritySettings");
        	} else {
        		LocationSettings.toggle(context);
        	}
            break;
        case AIRPLANE:
            AirplaneSettings.toggle(context);
            break;
        case SYNC:
            // Can only do SYnc from Eclair on.
            if (android.os.Build.VERSION.SDK_INT >=
                                        android.os.Build.VERSION_CODES.ECLAIR)
                SyncSettings.toggle(context);
            break;
        case AUTORORATE:
        	ScreenAutoRotateSettings.toggle(context);
        	break;
        case SCREEN_ALWAYS_ON:
        	ScreenAlwaysOnSettings.toggle(context);
        	break;
        case BRIGHTNESS:
        case BRIGHTAUTO:
        case OTBRIGHTNESS:
        case OTBRIGHTAUTO:
            Intent screenIntent = new Intent(context, BrightnessControl.class);
            screenIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            screenIntent.putExtra("auto", control == Control.BRIGHTAUTO ||
                                          control == Control.OTBRIGHTAUTO);
            screenIntent.putExtra("onetouch", control == Control.OTBRIGHTNESS ||
                                              control == Control.OTBRIGHTAUTO);
            context.startActivity(screenIntent);
            break;
        }
    }


    // ******************************************************************** //
    // Update Management.
    // ******************************************************************** //

    /**
     * Immediately update the specified widget's state.
     * 
     * @param   context     The context in which this update is running.
     * @param   id          The ID of the widget to update.  Note that this
     *                      could be a stale ID.
     */
    static void updateWidget(Context context, int id) {
        try {
            AppWidgetManager manager = AppWidgetManager.getInstance(context);
            AppWidgetProviderInfo info = manager.getAppWidgetInfo(id);
            if (info != null)
                updateWidget(context, manager, info.provider, id);
        } catch (Exception e) {
            Errors.reportException(context, e);
        }
    }


    /**
     * Immediately update the states of all our widgets.
     * 
     * @param   context     The context in which this update is running.
     */
    static void updateAllWidgets(Context context) {
        try {
            Log.d(TAG, "updateAllWidgets()");

            AppWidgetManager manager = AppWidgetManager.getInstance(context);
            for (ComponentName provider : DAZZLE_PROVIDERS) {
                int[] ids = manager.getAppWidgetIds(provider);
                for (int id : ids)
                    updateWidget(context, manager, provider, id);
            }

            Log.d(TAG, "updateAllWidgets() DONE");
        } catch (Exception e) {
            Errors.reportException(context, e);
        }
    }
    
    
    /**
     * Immediately update the specified widget's state.
     * 
     * @param   context     The context in which this update is running.
     * @param   manager     The widget manager.
     * @param   provider    Name of the provider that this widget belongs to.
     * @param   id          The ID of the widget to update.
     */
    private static void updateWidget(Context context, AppWidgetManager manager,
                                     ComponentName provider, int id)
    {
        String provName = provider.getClassName();
        Class<?> clazz;
        try {
            clazz = Class.forName(provName);
        } catch (ClassNotFoundException e) {
            Log.e(TAG, "updateWidget(" + id + "): CLASS " +
                       provider + " NOT FOUND!!!");
            return;
        }
        Log.d(TAG, "updateWidget(" + id);
        RemoteViews views = buildViews(context, clazz, id);
        manager.updateAppWidget(id, views);
    }


    /**
     * Create a RemoteViews object representing the current state of the
     * widget.
     * 
     * @param   context     The context in which this update is running.
     * @param   clazz       The provider class for this widget instance.
     * @param   id          The widget ID for which we're building a view.
     * @return              The new RemoteViews.
     */
    private static RemoteViews buildViews(Context context, Class<?> clazz, int id) {
         RemoteViews views = new RemoteViews(context.getPackageName(),
                                             R.layout.dazzle_widget);
         SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

         for (Control c : Control.CONTROLS) {
             boolean enable = prefs.getBoolean(c.pref + "-" + id, false);
             if (!enable) {
                 views.setViewVisibility(c.id, View.GONE);
             } else {
                 views.setViewVisibility(c.id, View.VISIBLE);
                 views.setOnClickPendingIntent(c.id, c.createIntent(context, clazz));
                 setControl(context, id, views, c, clazz);
             }
         }

         return views;
    }
    
    
    private static void setControl(Context context, int widgetId, RemoteViews views, Control control, Class<?> clazz) {
        switch (control) {
        case RINGER:
            RingerSettings.setWidget(context, views, R.id.ringer_ind);
            break;
        case OTRINGER:
            RingerSettings.setWidget(context, views, R.id.otringer_ind);
            break;
        case RADIO_SETTINGS:
        	// no op
        	break;
        case MOBILE_DATA:
        	MobileDataSettings.subscribe(context, widgetId, clazz);
            MobileDataSettings.setWidgetState(context, views,
                       R.id.mobile_data_icon, R.id.mobile_data_ind);
            break;
        case PHONE_RADIO:
        	PhoneRadioSettings.setWidget(context, views, R.id.phone_radio_ind);
        	break;
        case WIFI:
            WiFiSettings.setWidget(context, views, R.id.wifi_ind);
            break;
        case WIFI_AP:
        	if( Build.VERSION.SDK_INT >= Build.VERSION_CODES.FROYO ) {
        		WiFiApSettings.subscribe(context);
        		WiFiApSettings.setWidget(context, views, R.id.wifi_ap_ind);
        	}
        	break;
        case BLUETOOTH:
            // Can only do Bluetooth from Eclair on.
            if (android.os.Build.VERSION.SDK_INT >=
                                        android.os.Build.VERSION_CODES.ECLAIR)
                BluetoothSettings.setWidget(context, views, R.id.bluetooth_ind);
            break;
        case GPS:
        	LocationSettings.subscribe(context, widgetId, clazz);
        	LocationSettings.setWidget(context, views, R.id.gps_ind);
            break;
        case AIRPLANE:
            AirplaneSettings.setWidget(context, views, R.id.airplane_ind);
            break;
        case SYNC:
            // Can only do Sync from Eclair on.
            if (android.os.Build.VERSION.SDK_INT >=
                                        android.os.Build.VERSION_CODES.ECLAIR)
                SyncSettings.setWidget(context, views, R.id.sync_ind);
            break;
        case AUTORORATE:
        	ScreenAutoRotateSettings.subscribe(context, widgetId, clazz);
        	ScreenAutoRotateSettings.setWidget(context, views,
                    R.id.autorotate_ind);
        	break;
        case SCREEN_ALWAYS_ON:
        	ScreenAlwaysOnSettings.subscribe(context, widgetId, clazz);
        	ScreenAlwaysOnSettings.setWidget(context, views,
        			R.id.screen_always_on_ind);
        	break;
        case BRIGHTNESS:
            BrightnessSettings.setWidget(context, views, R.id.brightness_ind);
            break;
        case OTBRIGHTNESS:
            BrightnessSettings.setWidget(context, views, R.id.otbrightness_ind);
            break;
        case BRIGHTAUTO:
            BrightnessSettings.setWidget(context, views, R.id.brightauto_ind);
            break;
        case OTBRIGHTAUTO:
            BrightnessSettings.setWidget(context, views, R.id.otbrightauto_ind);
            break;
        }
    }
    
    
    // ******************************************************************** //
    // Class Data.
    // ******************************************************************** //
    
    // Names of all the providers which this class services; i.e. our
    // subclasses.
    private static ComponentName[] DAZZLE_PROVIDERS = {
        new ComponentName("org.hermit.dazzle", "org.hermit.dazzle.Dazzle11Provider"),
        new ComponentName("org.hermit.dazzle", "org.hermit.dazzle.Dazzle21Provider"),
        new ComponentName("org.hermit.dazzle", "org.hermit.dazzle.Dazzle31Provider"),
        new ComponentName("org.hermit.dazzle", "org.hermit.dazzle.Dazzle41Provider"),
    };
    
    
    // ******************************************************************** //
    // Private Data.
    // ******************************************************************** //
    
}

