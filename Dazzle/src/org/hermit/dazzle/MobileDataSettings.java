/**
 * Dazzle: a mobile data control widget for Android.
 * <br>Copyright 2010 Dmitry DELTA Malykhanov
 * 		for Ian Cameron Smith Dazzle widget
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

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.RemoteViews;

public class MobileDataSettings extends AbsCommonTelephonySettings {

	private MobileDataSettings() { }

	private static boolean getMobileDataState(final Context context) {
		final int dataState = ((TelephonyManager)
				context.getSystemService(Context.TELEPHONY_SERVICE))
					.getDataState();
		return TelephonyManager.DATA_CONNECTED == dataState
				|| TelephonyManager.DATA_SUSPENDED == dataState;
	}
	
    private static boolean isEnabled(final Context context) {
    	if (isRadioOn(context)) {
			final NetworkInfo.State wifiState = ((ConnectivityManager)
					context.getSystemService(Context.CONNECTIVITY_SERVICE))
						.getNetworkInfo(ConnectivityManager.TYPE_WIFI).getState();
			return NetworkInfo.State.CONNECTING != wifiState
					&& NetworkInfo.State.CONNECTED != wifiState;
    	} else {
    		return false;
    	}
    }

	private static void toggleMobileDataState(final Context context) {
		if (isEnabled(context)) {
			final boolean enabled = getMobileDataState(context);
			try {
				Object iTelephony = getITelephony(context);
				Method action = iTelephony.getClass().getMethod(enabled
						? "disableDataConnectivity" : "enableDataConnectivity");
				action.invoke(iTelephony);
				Log.d(DazzleProvider.TAG, "Mobile data "
						+ (!enabled ? "enabled" : "disabled"));
			} catch (Exception e) {
				Log.e(DazzleProvider.TAG, "Cannot toggle mobile data state", e);
			}
		} else {
			Log.d(DazzleProvider.TAG, "Data connectivity not possible or radio is off");
		}
	}
	
    /**
     * Toggle the current state.
     * 
     * @param   context     The context we're running in.
     */
    static void toggle(final Context context) {
        Log.i(DazzleProvider.TAG, "toggle mobile data");
        
        toggleMobileDataState(context);
    }

    /**
     * Set the indicator widget to represent our current state.
     * 
     * @param   context     The context we're running in.
     * @param   views       The widget view to modify.
     * @param   iconWidget      The ID of the indicator widget.
     * @param   indicatorWidget The ID of the widget icon.
     */
    static void setWidgetState(final Context context, final RemoteViews views,
    		final int iconWidget, final int indicatorWidget)
    {
    	views.setImageViewResource(iconWidget,
    			isEnabled(context)
    				? R.drawable.radio : R.drawable.radio_off);
		final NetworkInfo.State mobileState = ((ConnectivityManager)
				context.getSystemService(Context.CONNECTIVITY_SERVICE))
					.getNetworkInfo(ConnectivityManager.TYPE_MOBILE).getState();
    	final int indicator;
    	if (NetworkInfo.State.CONNECTED == mobileState) {
    		indicator = R.drawable.green;
    	} else if (NetworkInfo.State.DISCONNECTED == mobileState) {
    		indicator = R.drawable.grey;
    	} else {
    		indicator = R.drawable.orange;
    	}
        views.setImageViewResource(indicatorWidget, indicator);
    }
    
}
// EOF