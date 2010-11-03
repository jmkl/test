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

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;
import android.widget.Toast;

public class MobileDataSettings extends AbsCommonTelephonySettings {

	private MobileDataSettings() { }
	
	private static boolean getMobileDataState(final Context context) {
		final int dataState = ((TelephonyManager)
				context.getSystemService(Context.TELEPHONY_SERVICE))
					.getDataState();
		return TelephonyManager.DATA_CONNECTED == dataState
				|| TelephonyManager.DATA_SUSPENDED == dataState;
	}
	
    /**
     * Toggle the current state.
     * 
     * @param   context     The context we're running in.
     */
    static void toggle(final Context context) {
        Log.i(DazzleProvider.TAG, "toggle mobile data");
        
        if (isMobileDataEnabledInSettings(context)) {
        	if (isMobileDataTogglePossible(context)) {
        		setMobileDataEnabled(context, !getMobileDataState(context));
        	} else {
        		Log.d(DazzleProvider.TAG, "Mobile Data connectivity not possible or radio is off");
        	}
        } else {
        	Log.d(DazzleProvider.TAG, "Mobile Data disabled in system settings");
        	Toast.makeText(context.getApplicationContext(),
        			R.string.system_mobile_data_disabled, Toast.LENGTH_LONG)
        				.show();
        }
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
        final boolean enabled = isMobileDataTogglePossible(context);
        // calling "setEnabled" prevents view from being clickable and we
        // cannot show a Toast with explanation
        views.setInt(R.id.dazzle_mobile_data, "setBackgroundResource",
                enabled ? R.drawable.appwidget_button : android.R.color.transparent);
        views.setImageViewResource(iconWidget,
                    enabled ? R.drawable.radio : R.drawable.radio_off);
        final int indicator;
        if (enabled) {
        	views.setViewVisibility(indicatorWidget, View.VISIBLE);
            if (mobileDataTransition.inProgress()) {
            	if (getMobileDataState(context) == mobileDataTransition.to) {
                	indicator = getIndicatorState(context);
            	} else {
            		indicator = R.drawable.orange;
            	}
            } else {
            	indicator = getIndicatorState(context);
            }
        } else {
        	views.setViewVisibility(indicatorWidget, View.GONE);
        	indicator = R.drawable.grey;
        }
        views.setImageViewResource(indicatorWidget, indicator);
    }
    
    private static int getIndicatorState(final Context context) {
    	final int indicator;
        final NetworkInfo.State mobileState = ((ConnectivityManager)
                context.getSystemService(Context.CONNECTIVITY_SERVICE))
                    .getNetworkInfo(ConnectivityManager.TYPE_MOBILE).getState();
        if( NetworkInfo.State.CONNECTED == mobileState ) {
            indicator = R.drawable.green;
        } else if( NetworkInfo.State.DISCONNECTED == mobileState ) {
            indicator = R.drawable.grey;
        } else {
            indicator = R.drawable.orange;
        }
        return indicator;
    }
    
}
// EOF