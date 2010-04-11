
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


import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.appwidget.AppWidgetProviderInfo;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
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
    implements LocationListener
{
    
    // ******************************************************************** //
    // Public Constants.
    // ******************************************************************** //

    // Debugging tag.
    static final String TAG = "Dazzle";

    // The controls we support.
    enum Control {
        WIFI(R.id.dazzle_wifi, "enableWifi"),
        BLUETOOTH(R.id.dazzle_bluetooth, "enableBluetooth"),
        GPS(R.id.dazzle_gps, "enableGps"),
        AIRPLANE(R.id.dazzle_airplane, "enableAirplane"),
        BRIGHTNESS(R.id.dazzle_brightness, "enableBrightness");
        
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
        
        myContext = context;
        
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
        
        myContext = context;

        // Update the specified widgets.
        final int num = ids.length;
        for (int i = 0; i < num; ++i) {
            final int id = ids[i];
            updateWidget(context, id);
        }

        // We need to listen for GPS updates to be notified of status changes.
        // Doing this in onEnabled() doesn't seem to work.
        if (!locationListening) {
            LocationManager locationManager =
                (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
                                                   1000000, 0f, this);
            locationListening = true;
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
        
        // Delete all the prefs for each widget instance.
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor edit = prefs.edit();
        for (int id : ids)
            for (DazzleProvider.Control c : DazzleProvider.Control.CONTROLS)
                edit.remove(c.pref + "-" + id);
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
        
        LocationManager locationManager =
            (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        locationManager.removeUpdates(this);
        
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
        
        if (intent.hasCategory(Intent.CATEGORY_ALTERNATIVE)) {
            Uri data = intent.getData();
            Log.d(TAG, "Handle receive " + data);
            int id = Integer.parseInt(data.getSchemeSpecificPart());
            if (id >= 0 && id < Control.CONTROLS.length) {
                Control control = Control.CONTROLS[id];
                handleClick(context, control);
            }
        }
        
        // State changes fall through.
        updateAllWidgets(context);
    }


    private void handleClick(Context context, Control control) {
        Log.d(TAG, "Handle control " + control.toString());

        switch (control) {
        case WIFI:
            WiFiSettings.toggle(context);
            break;
        case BLUETOOTH:
            BluetoothSettings.toggle(context);
            break;
        case GPS:
            Intent gpsIntent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
            gpsIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(gpsIntent);
            break;
        case AIRPLANE:
            AirplaneSettings.toggle(context);
            break;
        case BRIGHTNESS:
            Intent screenIntent = new Intent(context, BrightnessControl.class);
            screenIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(screenIntent);
            break;
        }
    }


    /**
     * Handle GPS status changes to update the widget.
     * 
     * @param   provider        Provider which has changed.
     */
    @Override
    public void onProviderEnabled(String provider) {
        updateAllWidgets(myContext);
    }

    
    /**
     * Handle GPS status changes to update the widget.
     * 
     * @param   provider        Provider which has changed.
     */
    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        updateAllWidgets(myContext);
    }
    
    
    /**
     * Ignore location changes.
     * 
     * @param   location        Location to ignore.
     */
    @Override
    public void onLocationChanged(Location location) {
    }
    
    
    /**
     * Handle GPS status changes to update the widget.
     * 
     * @param   provider        Provider which has changed.
     */
    @Override
    public void onProviderDisabled(String provider) {
        updateAllWidgets(myContext);
    }


    // ******************************************************************** //
    // Update Management.
    // ******************************************************************** //

    /**
     * Immediately update the specified widget's state.
     * 
     * @param   context     The context in which this update is running.
     */
    static void updateWidget(Context context, int id) {
        AppWidgetManager manager = AppWidgetManager.getInstance(context);
        AppWidgetProviderInfo info = manager.getAppWidgetInfo(id);
        updateWidget(context, manager, info.provider, id);
    }


    /**
     * Immediately update the states of all our widgets.
     * 
     * @param   context     The context in which this update is running.
     */
    static void updateAllWidgets(Context context) {
        Log.d(TAG, "updateAllWidgets()");

        AppWidgetManager manager = AppWidgetManager.getInstance(context);
        for (ComponentName provider : DAZZLE_PROVIDERS) {
            int[] ids = manager.getAppWidgetIds(provider);
            for (int id : ids)
                updateWidget(context, manager, provider, id);
        }
        
        Log.d(TAG, "updateAllWidgets() DONE");
    }
    
    
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

        Log.d(TAG, "updateWidget(" + id + ") = " + clazz.getName());
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
             boolean enable = prefs.getBoolean(c.pref + "-" + id, true);
             if (!enable) {
                 views.setViewVisibility(c.id, View.GONE);
             } else {
                 views.setViewVisibility(c.id, View.VISIBLE);
                 views.setOnClickPendingIntent(c.id, c.createIntent(context, clazz));
             }
         }

         WiFiSettings.setWidget(context, views, R.id.wifi_ind);

         BluetoothSettings.setWidget(context, views, R.id.bluetooth_ind);

         GpsSettings.setWidget(context, views, R.id.gps_ind);

         AirplaneSettings.setWidget(context, views, R.id.airplane_ind);

         BrightnessSettings.setWidget(context, views, R.id.brightness_ind);

         return views;
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
    
    // Our context.
    private Context myContext = null;
    
    // Flag if we have installed the location listener.
    private boolean locationListening = false;

}

