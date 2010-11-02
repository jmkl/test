/**
 * Dazzle: common WiFi tethering controls
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

package org.hermit.dazzle;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WifiManager;
import android.util.Log;
import android.widget.RemoteViews;

public class WiFiApSettings extends AbsCommonWiFiSettings {

	private WiFiApSettings() { }
	
	// see com.android.settings.wifi.WifiApEnabler
	
	private static BroadcastReceiver apStateReceiver = null;
	
	static void subscribe(final Context context) {
		// Subscribe to WifiManager.WIFI_AP_STATE_CHANGED_ACTION.
		// We can declare it in a filter for SystemBroadcastReceiver, but
		// we only need it for Froyo when we use hotspot widget toggle.
		if( null == apStateReceiver ) {
			apStateReceiver = new BroadcastReceiver() {
				@Override
				public void onReceive(Context context, Intent intent) {
					DazzleProvider.updateAllWidgets(context);
				}
			};
			context.getApplicationContext().registerReceiver(apStateReceiver,
					new IntentFilter(/*WifiManager.*/WIFI_AP_STATE_CHANGED_ACTION));
		}
	}
	
	private static void setEnabled(final WifiManager wfm, final boolean enabled) {
		// from WifiApEnabler.java
		final int wifiState = wfm.getWifiState();
        if (enabled && ((wifiState == WifiManager.WIFI_STATE_ENABLING) ||
                    (wifiState == WifiManager.WIFI_STATE_ENABLED))) {
        	Log.d(DazzleProvider.TAG, "Disabling WiFi Client to enable AP");
            wfm.setWifiEnabled(false);
            //Settings.Secure.putInt(cr, Settings.Secure.WIFI_SAVED_STATE, 1);
            // TODO: when persistent settings implemented, restore the WiFi
            // connection when turning off the hotspot
        }
        setWifiApEnabled(wfm, enabled);
	}

	/**
     * Toggle the current state.
     * 
     * @param   context     The context we're running in.
     */
    static void toggle(Context context) {
		final WifiManager wfm = (WifiManager)
				context.getSystemService(Context.WIFI_SERVICE);
    	setEnabled(wfm, !isEnabled(wfm));
    }

    /**
     * Set the indicator widget to represent our current state.
     * 
     * @param   context     The context we're running in.
     * @param   views       The widget view to modify.
     * @param   widget      The ID of the indicator widget.
     */
    static void setWidget(Context context, RemoteViews views, int widget) {
    	final int indicator;
		final WifiManager wfm = (WifiManager)
				context.getSystemService(Context.WIFI_SERVICE);
    	switch( getWifiApState(wfm) ) {
    	case WIFI_AP_STATE_DISABLED:
    		indicator = R.drawable.grey;
    		break;
    	case WIFI_AP_STATE_DISABLING:
    	case WIFI_AP_STATE_ENABLING:
    		indicator = R.drawable.orange;
    		break;
    	case WIFI_AP_STATE_ENABLED:
    		indicator = R.drawable.green;
    		break;
    	case WIFI_AP_STATE_FAILED:
    	default:
    		indicator = R.drawable.red;
    		break;
    	}
    	views.setImageViewResource(widget, indicator);
    }
    
}
// EOF
