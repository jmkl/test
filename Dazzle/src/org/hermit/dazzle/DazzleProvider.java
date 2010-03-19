
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
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.RemoteViews;


/**
 * The Dazzle widget provider.
 */
public class DazzleProvider
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
        AppWidgetManager manager = AppWidgetManager.getInstance(context);
        ComponentName comp = new ComponentName(context, DazzleProvider.class);
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
         RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.dazzle_1_1);

         Intent intent = new Intent(context, DazzleControl.class);
         PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, 0);
         views.setOnClickPendingIntent(R.id.dazzle_button, pendingIntent);
         
         String label = BrightnessSettings.getModeString(context);
         views.setTextViewText(R.id.dazzle_button, label);
         
         return views;
    }


    // ******************************************************************** //
    // Class Data.
    // ******************************************************************** //

    // Debugging tag.
    @SuppressWarnings("unused")
    private static final String TAG = "DazzleProvider";

}

