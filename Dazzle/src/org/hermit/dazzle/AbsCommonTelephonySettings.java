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
import android.telephony.TelephonyManager;
import android.util.Log;

abstract class AbsCommonTelephonySettings {

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
			final Method isRadioOn
					= iTelephony.getClass().getDeclaredMethod("isRadioOn");
			radioOn = ((Boolean) isRadioOn.invoke(iTelephony)).booleanValue();
			//Log.d(DazzleProvider.TAG, "Radio state: " + radioOn);
		} catch(Exception e) {
			Log.e(DazzleProvider.TAG, "isRadioOn()", e);
		}
		return radioOn;
	}

}
// EOF