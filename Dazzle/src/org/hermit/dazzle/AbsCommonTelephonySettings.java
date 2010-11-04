/**
 * Dazzle: common phone radio controls
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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.util.Log;

abstract class AbsCommonTelephonySettings {

	protected final static String SHADOW_MOBILE_DATA
			= "Settings.Secure.MOBILE_DATA";
	
	protected final static String SHADOW_PHONE_RADIO
			= "Settings.Shadow.PHONE_RADIO";

	protected static Object getITelephony(final Context context)
		throws SecurityException, NoSuchMethodException,
		IllegalArgumentException, InvocationTargetException,
		IllegalAccessException
	{
		final TelephonyManager tm = (TelephonyManager)
				context.getSystemService(Context.TELEPHONY_SERVICE);
		final Method getITelephony
				= tm.getClass().getDeclaredMethod("getITelephony");
		if (!getITelephony.isAccessible()) {
			getITelephony.setAccessible(true);
		}
		return getITelephony.invoke(tm);
	}

	protected static boolean isRadioOn(final Context context) {
		boolean radioOn = false;
		try {
			final Object iTelephony = getITelephony(context);
			if (null != iTelephony) {
				final Method isRadioOn
						= iTelephony.getClass().getDeclaredMethod("isRadioOn");
				radioOn = ((Boolean) isRadioOn.invoke(iTelephony)).booleanValue();
			}
			//Log.d(DazzleProvider.TAG, "Radio state: " + radioOn);
		} catch(Exception e) {
			Log.e(DazzleProvider.TAG, "isRadioOn()", e);
		}
		return radioOn;
	}

    protected static boolean isMobileDataEnabledInSettings(final Context context) {
        return 1 == Settings.Secure.getInt(context.getContentResolver(),
        		"mobile_data", 1);
    }

    protected static boolean isMobileDataTogglePossible(final Context context) {
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
    
    protected static void setShadowMobileDataEnabled(final Context context,
    		final boolean enabled)
    {
		final SharedPreferences prefs = DazzleProvider.getShadowPreferences(context);
		if (prefs.getBoolean(SHADOW_MOBILE_DATA, true) != enabled) {
			prefs.edit().putBoolean(SHADOW_MOBILE_DATA, enabled).commit();
		}
    }

	protected static void setMobileDataEnabled(final Context context,
			final boolean enabled)
	{
		if (mobileDataTransition.inProgress()) {
        	Log.w(DazzleProvider.TAG, "Mobile Data is already in transition "
        			+ mobileDataTransition + " when requesting "
        			+ (enabled ? "ON" : "OFF"));
		}
		try {
			Object iTelephony = getITelephony(context);
			Method action = iTelephony.getClass().getMethod(enabled
					? "enableDataConnectivity" : "disableDataConnectivity");
			action.invoke(iTelephony);
			mobileDataTransition.start(enabled);
			setShadowMobileDataEnabled(context, enabled);
			Log.d(DazzleProvider.TAG, "setMobileDataEnabled: Mobile data "
					+ (enabled ? "enabled" : "disabled"));
		} catch (Exception e) {
			mobileDataTransition.stop(!enabled);
			Log.e(DazzleProvider.TAG, "setMobileDataEnabled: "
					+ "Cannot toggle mobile data state", e);
		}
	}
	
	protected static Transition mobileDataTransition = new Transition();
	
    /*
     * There are no useful callbacks/broadcasts about the phone radio state.
     * Hence we try to track the progress manually.
     */
    protected static class Transition {
    	protected boolean from;
    	protected boolean to;
    	
    	public void start(final boolean target) {
    		from = !target;
    		to = target;
    	}
    	
    	public void stop(final boolean state) {
    		from = to = state;
    	}

    	public boolean inProgress() {
    		return from != to;
    	}
    	
    	public String toString() {
    		return (from ? "ON" : "OFF") + " -> " + (to ? "ON" : "OFF"); 
    	}
    }
    
}
// EOF