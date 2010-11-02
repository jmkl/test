/**
 * Dazzle: system/secure settings observer with custom logging
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

import android.content.ContentResolver;
import android.database.ContentObserver;
import android.net.Uri;
import android.util.Log;

public class SettingsObserver extends ContentObserver {

	private final String logMessage;
		
	public SettingsObserver(final ContentResolver resolver,
			final Uri uri, final String logMessage) {
		super(null);
		this.logMessage = logMessage;
		Log.d(DazzleProvider.TAG, "Subscribing to " +
				logMessage + " changes");
		resolver.registerContentObserver(uri, true, this);
	}

	@Override
	public void onChange(boolean selfChange) {
		Log.d(DazzleProvider.TAG, logMessage + " changed");
		DazzleProvider.requestUpdate();
	}

	void unsubscribe(final ContentResolver resolver) {
		resolver.unregisterContentObserver(this);
		Log.d(DazzleProvider.TAG, "Unsubscribed from "
				+ logMessage + " changes");
	}
	
}
// EOF
