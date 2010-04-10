
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


import android.app.Activity;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.Toast;


/**
 * The Dazzle widget provider.
 */
public abstract class DazzleProvider
    extends AppWidgetProvider
{

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
        
        checkClass(context, "onEnabled()");
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
        
        checkClass(context, "onUpdate()");
        final int num = ids.length;
        for (int i = 0; i < num; ++i) {
            int id = ids[i];
            RemoteViews views = buildViews(context);
            manager.updateAppWidget(id, views);
        }
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
        
        providerClazz = null;
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
        
        providerClazz = null;
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
        
        // Since we get WiFi notifications etc., all our classes including
        // those not in use will come here.  So, reject all but the one
        // which is displayed.
        Class<? extends DazzleProvider> clazz = getClass();
        if (!clazz.equals(providerClazz))
            return;
        
        if (intent.hasCategory(Intent.CATEGORY_ALTERNATIVE)) {
            Uri data = intent.getData();
            Log.d(TAG, "Handle receive " + data);
            int id = Integer.parseInt(data.getSchemeSpecificPart());
            if (id >= 0 && id < Control.CONTROLS.length) {
                Control control = Control.CONTROLS[id];
                handleClick(context, control);
            }
        }
        
        // State changes fall through
        updateWidgets(context);
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
        case BRIGHTNESS:
            // Should never get here.
            break;
        }
    }


    // ******************************************************************** //
    // Update Management.
    // ******************************************************************** //
    
    /**
     * Immediately update the widget state.
     * 
     * @param   context     The context in which this update is running.
     */
    static void updateWidgets(Context context) {
        Log.d(TAG, "updateWidgets() for " + providerClazz.getName());

        AppWidgetManager manager = AppWidgetManager.getInstance(context);
        ComponentName comp = new ComponentName(context, providerClazz);
        RemoteViews views = buildViews(context);
        manager.updateAppWidget(comp, views);
    }


    /**
     * Create a RemoteViews object representing the current state of the
     * widget.
     * 
     * @param   context     The context in which this update is running.
     * @return              The new RemoteViews.
     */
    private static RemoteViews buildViews(Context context) {
         RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.dazzle_widget);

         for (Control c : Control.CONTROLS)
             views.setOnClickPendingIntent(c.id, c.createIntent(context));

         WiFiSettings.setWidget(context, views, R.id.wifi_ind);

         BluetoothSettings.setWidget(context, views, R.id.bluetooth_ind);

         BrightnessSettings.setWidget(context, views, R.id.brightness_ind);

         return views;
    }
    

    // ******************************************************************** //
    // Utils.
    // ******************************************************************** //

    private void checkClass(Context context, String where) {
        Class<? extends DazzleProvider> clazz = getClass();

        if (providerClazz == null) {
            providerClazz = clazz;
            Log.d(TAG, "Set class " + clazz.getName());
        } else if (!providerClazz.equals(clazz)) {
            Toast toast = Toast.makeText(context,
                                         R.string.error_only_one,
                                         Toast.LENGTH_SHORT);
            toast.show();
        }
    }


    // ******************************************************************** //
    // Class Data.
    // ******************************************************************** //

    // Debugging tag.
    @SuppressWarnings("unused")
    private static final String TAG = "DazzleProvider";
    
    // The actual class of this provider.  It sucks, but we need this,
    // and it means we can only have one subclass active at a time
    // (because the individual controls can't know what widget they belong to).
    private static Class<? extends DazzleProvider> providerClazz = null;

    // The controls we support.
    private enum Control {
        WIFI(R.id.dazzle_wifi, null),
        BLUETOOTH(R.id.dazzle_bluetooth, null),
        BRIGHTNESS(R.id.dazzle_brightness, BrightnessControl.class);
        
        Control(int id, Class<? extends Activity> clazz) {
            this.id = id;
            this.clazz = clazz;
        }

        PendingIntent createIntent(Context context) {
            if (clazz != null) {
                Intent intent = new Intent(context, clazz);
                return PendingIntent.getActivity(context, 0, intent, 0);
            } else {
                Intent i = new Intent(context, providerClazz);
                i.addCategory(Intent.CATEGORY_ALTERNATIVE);
                i.setData(Uri.parse("custom:" + ordinal()));

                return PendingIntent.getBroadcast(context, 0, i, 0);
            }
        }

        static Control[] CONTROLS = values();
        int id;
        Class<? extends Activity> clazz;
    }
    
}

