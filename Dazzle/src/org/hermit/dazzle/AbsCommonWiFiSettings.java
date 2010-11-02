/**
 * Dazzle: common WiFi controls, tethering support
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

import java.lang.reflect.Method;

import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.util.Log;

public abstract class AbsCommonWiFiSettings {

	// from frameworks/base/wifi/java/android/net/wifi/WifiManager.java
    public static final String WIFI_AP_STATE_CHANGED_ACTION =
        "android.net.wifi.WIFI_AP_STATE_CHANGED";

	public static final int WIFI_AP_STATE_DISABLING = 0;
	public static final int WIFI_AP_STATE_DISABLED = 1;
	public static final int WIFI_AP_STATE_ENABLING = 2;
	public static final int WIFI_AP_STATE_ENABLED = 3;
	public static final int WIFI_AP_STATE_FAILED = 4;	

	protected static int getWifiApState(final WifiManager wfm) {
		int state = /*WifiManager.*/WIFI_AP_STATE_DISABLED;
		try {
			final Method getWifiApState = wfm.getClass().getMethod(
					"getWifiApState");
			state = ((Integer) getWifiApState.invoke(wfm));
		} catch(Exception e) {
			Log.e(DazzleProvider.TAG, "Cannot get WiFi AP state", e);
		}
		return state;
	}
	
	protected static boolean isEnabled(final WifiManager wfm) {
		final int state = getWifiApState(wfm);
		return /*WifiManager.*/WIFI_AP_STATE_ENABLING == state
				|| /*WifiManager.*/WIFI_AP_STATE_ENABLED == state;
	}
	
	protected static boolean setWifiApEnabled(final WifiManager wfm, final boolean enabled) {
		boolean success = false;
		try {
			final Method setWifiApEnabled = wfm.getClass().getMethod(
					"setWifiApEnabled", WifiConfiguration.class, Boolean.TYPE);
			setWifiApEnabled.invoke(wfm, null, enabled);
			success = true;
		} catch(Exception e) {
			Log.e(DazzleProvider.TAG, "Cannot set WiFi AP state", e);
		}
		return success;
	}
	
}
// EOF
