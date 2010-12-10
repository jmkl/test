
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


import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.util.Log;
import android.widget.RemoteViews;


/**
 * This static class provides utilities to manage the WiFi state.
 */
public class WiFiSettings extends AbsCommonWiFiSettings
{

    // ******************************************************************** //
    // Constructor.
    // ******************************************************************** //

    /**
     * Constructor -- hidden, as this class is non-instantiable.
     */
    private WiFiSettings() {
    }
    

    // ******************************************************************** //
    // Status Handling.
    // ******************************************************************** //

    /**
     * Toggle the current state.
     * 
     * @param   context     The context we're running in.
     */
    static void toggle(Context context) {
        Log.i(TAG, "toggle WiFi");
        
        // Just toggle WiFi power, as long as we're not already in
        // an intermediate state.
        WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        int state = wifiManager.getWifiState();
        if (state == WifiManager.WIFI_STATE_DISABLED) {
        	final int wifiApState = getWifiApState(wifiManager);
        	if (/*WifiManager.*/WIFI_AP_STATE_ENABLING == wifiApState
        			|| /*WifiManager.*/WIFI_AP_STATE_ENABLED == wifiApState) {
        		// disable tethering, if active
        		Log.d(TAG, "Disable tethering before enabling WiFi");
        		setWifiApEnabled(wifiManager, false);
        	}
            wifiManager.setWifiEnabled(true);
        } else if (state == WifiManager.WIFI_STATE_ENABLED) {
            wifiManager.setWifiEnabled(false);
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
        WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        int state = wifiManager.getWifiState();
        
        int image = R.drawable.grey;
        if (state == WifiManager.WIFI_STATE_DISABLED) {
            image = R.drawable.grey;
        } else if (state == WifiManager.WIFI_STATE_ENABLED) {
        	final NetworkInfo.State wifiState = ((ConnectivityManager)
					context.getSystemService(Context.CONNECTIVITY_SERVICE))
						.getNetworkInfo(ConnectivityManager.TYPE_WIFI)
							.getState();
            image = NetworkInfo.State.CONNECTED == wifiState
            	? R.drawable.green : R.drawable.blue;
        } else {
            image = R.drawable.orange;
        }
        // TODO: track off->on transition and restore mobile data state

        views.setImageViewResource(widget, image);
    }


    // ******************************************************************** //
    // Class Data.
    // ******************************************************************** //

    // Debugging tag.
    @SuppressWarnings("unused")
    private static final String TAG = DazzleProvider.TAG;

}

