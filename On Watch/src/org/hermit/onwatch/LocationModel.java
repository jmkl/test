
/**
 * On Watch: sailor's watchkeeping assistant.
 * <br>Copyright 2009 Ian Cameron Smith
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


package org.hermit.onwatch;


import java.util.ArrayList;

import org.hermit.geo.PointOfInterest;
import org.hermit.geo.Position;

import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;


/**
 * This class implements a GPS view.  It displays the current location info
 * and GPS state.
 */
public class LocationModel
	implements LocationListener
{

    // ******************************************************************** //
    // Public Types and Constants.
    // ******************************************************************** //
	
	/**
	 * Timezone value representing an unknown timezone.
	 */
	public static final int TZ_UNKNOWN = 999;
	
	/**
	 * Enumeration of the possible states of the GPS.
	 */
	public enum GpsState {
	    /** GPS state: unknown. */
		UNKNOWN(R.string.gps_unknown),
		
        /** GPS state: enabled. */
		ENABLED(R.string.gps_enabled),
        
        /** GPS state: disabled. */
		DISABLED(R.string.gps_disabled),
        
        /** GPS state: out of service. */
		OUT_OF_SERVICE(R.string.gps_oos),
        
        /** GPS state: temporarily out of service. */
		TEMP_OOS(R.string.gps_temp_oos);
		
		GpsState(int id) {
			messageId = id;
		}
		
		/** Message ID of the message associated with this state. */
		public final int messageId;
	}
	
	
	/**
	 * This listener class is used to receive notifications about location
	 * changes.
	 */
	public static class Listener {
	    /**
	     * Invoked when a location is acquired after not having one,
	     * either because we're starting or because the GPS has been off.
	     * 
	     * @param  state           GPS state.
	     * @param  stateMsg        GPS state message.
	     * @param  loc             Current location.
	     * @param  locMsg          Message describing the location.
	     */
        public void newLoc(GpsState state, String stateMsg,
                           Location loc, String locMsg) { }
        
        /**
         * Invoked when a new location is acquired.
         * 
         * @param  state           GPS state.
         * @param  stateMsg        GPS state message.
         * @param  loc             Current location.
         * @param  locMsg          Message describing the location.
         */
		public void locChange(GpsState state, String stateMsg,
						      Location loc, String locMsg) { }
		
        /**
         * Invoked when a position is acquired after not having one,
         * either because we're starting or because the GPS has been off.
         * 
         * @param  state           GPS state.
         * @param  stateMsg        GPS state message.
         * @param  pos             Current position.
         * @param  locMsg          Message describing the position.
         */
        public void newPos(GpsState state, String stateMsg,
                           Position pos, String locMsg) { }
        
        /**
         * Invoked when a new position is acquired.
         * 
         * @param  state           GPS state.
         * @param  stateMsg        GPS state message.
         * @param  pos             Current position.
         * @param  locMsg          Message describing the position.
         */
		public void posChange(GpsState state, String stateMsg,
			      			  Position pos, String locMsg) { }
	}
	

	/**
	 * This listener class is used to receive notifications about timezone
	 * changes.
	 */
	public static interface ZoneListener {
	    
		/**
		 * The timezone has changed.
		 * 
		 * @param newZone         The new timezone, as hours east or
		 *                        west of Greenwich, or TZ_UNKNOWN.
		 */
		public void tzChange(int newZone);
		
	}
	

	// ******************************************************************** //
    // Constructors.
    // ******************************************************************** //
	
	/**
	 * Create a location model.  Private since there is only one instance.
	 * 
	 * @param	context			Parent application.
	 */
	private LocationModel(Context context) {
		appContext = context;
		
		// Get the messages.
		lastFixMsg = appContext.getString(R.string.gps_last_fix);
		
		// Create the Handler we use for posting updates.
        updateHandler = new Handler() {
			@Override
			public void handleMessage(Message m) {
				update();
			}
		};
			
		// Set up the client lists.
		locationListeners = new ArrayList<Listener>();
		timezoneListeners = new ArrayList<ZoneListener>();

		// Get the location provider.
        locationManager =
        	(LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
	}

	
	/**
	 * Get the location model, creating it if it doesn't exist.
	 * 
	 * @param	context		   Parent application.
	 * @return                 The global location model instance.
	 */
	public static LocationModel getInstance(Context context) {
		if (modelInstance == null)
			modelInstance = new LocationModel(context);
		
		return modelInstance;
	}
	

	// ******************************************************************** //
    // Registered Listeners.
    // ******************************************************************** //
	
	/**
	 * Register a Handler to be called when the location or location state
	 * changes.
	 */
	public void listen(Listener handler) {
		locationListeners.add(handler);
	}
	
	
	/**
	 * Register a listener to be called when the nautical timezone
	 * changes.
	 */
	void listenTimezone(ZoneListener listener) {
		timezoneListeners.add(listener);
	}
	
	 
	// ******************************************************************** //
	// Run Control.
	// ******************************************************************** //
	
	/**
	 * Start the location monitor running.
	 */
	void resume() {
		// Register for location updates.
		final String name = LocationManager.GPS_PROVIDER;
		LocationProvider prov = locationManager.getProvider(name);
		if (prov != null) {
			locationManager.requestLocationUpdates(name, 60000, 0f, this);

			// Prime the pump with the last known location.
			Location prime = locationManager.getLastKnownLocation(name);
			if (prime != null)
				onLocationChanged(prime);
		}
	}
	

	/**
	 * Stop the location monitor.
	 */
	void pause() {
		// Stop GPS updates.
		locationManager.removeUpdates(this);
	}


	/**
	 * Regular tick event, for housekeeping.  Occurs every second, as
	 * close as possible to the 1-second boundary.
	 * 
	 * @param	time		Current system time in millis.
	 */
	void tick(long time) {
		// If more than 5 seconds since the last update, update now,
		// to refresh the "fix age" display.
		if (time - lastUpdate > 5000)
			update();
	}


	// ******************************************************************** //
    // Accessors.
    // ******************************************************************** //

	
	/**
	 * Get the current gps state.
	 * 
	 * @return				The current gps state.
	 */
	public synchronized final GpsState getGpsState() {
		return gpsState;
	}


	/**
	 * Get the current GPS status message.
	 * 
	 * @return				The current GPS status message.
	 */
	public synchronized final String getGpsStatusMsg() {
		return gpsStatusMsg;
	}


	/**
	 * Get the current GPS location.
	 * 
	 * @return				The current GPS location, null if not known.
	 */
	public synchronized final Location getCurrentLoc() {
		return currentLoc;
	}


	/**
	 * Get the current GPS location as a Position.
	 * 
	 * @return				The current GPS location as a Position,
	 * 						null if not known.
	 */
	public synchronized final Position getCurrentPos() {
		return currentPos;
	}


	/**
	 * Get a textual description of our location.
	 * 
	 * @return				The message.
	 */
	public synchronized final String getCurrentLocMsg() {
		return currentLocMsg;
	}


	/**
	 * Get the number of GPS satellites currently in view.
	 * 
	 * @return				The number of satellites.
	 */
	public synchronized final int getGpsSats() {
		return gpsSats;
	}


	// ******************************************************************** //
	// Location Updates.
	// ******************************************************************** //

	/**
	 * Called when the location has changed.  There are no restrictions
	 * on the use of the supplied Location object.
	 * 
	 * @param	loc			The new location, as a Location object.
	 */
	@Override
	public void onLocationChanged(Location loc) {
		if (loc.getProvider().equals(LocationManager.GPS_PROVIDER)) {
			synchronized (this) {
				currentLoc = loc;

				// Debug support.
				if (offsetLat != 0 || offsetLon != 0) {
					currentLoc.setLatitude(currentLoc.getLatitude() + offsetLat);
					currentLoc.setLongitude(currentLoc.getLongitude() + offsetLon);
				}
				
				currentPos = Position.fromDegrees(currentLoc.getLatitude(),
						                          currentLoc.getLongitude());
				
				positionValid = true;
				postUpdate();
			}
		}
	}


	/**
	 * Called when the provider is disabled by the user.
	 * If requestLocationUpdates is called on an already disabled provider,
	 * this method is called immediately.
	 * 
	 * @param	provider			The name of the location provider
	 * 								associated with this update.
	 */
	@Override
	public void onProviderDisabled(String provider) {
		Log.i(TAG, "Provider disabled: " + provider);
		if (provider.equals(LocationManager.GPS_PROVIDER)) {
            positionValid = false;
			synchronized (this) {
				gpsState = GpsState.DISABLED;
				postUpdate();
			}
		}
	}
	 
	 
	/**
	 * Called when the provider is enabled by the user.
	 * 
	 * @param	provider			The name of the location provider
	 * 								associated with this update.
	 */
	@Override
	public void onProviderEnabled(String provider) {
		Log.i(TAG, "Provider enabled: " + provider);
		if (provider.equals(LocationManager.GPS_PROVIDER)) {
            positionValid = false;
			synchronized (this) {
				gpsState = GpsState.ENABLED;
				postUpdate();
			}
		}
	}
	  

	/**
	 * Called when the provider status changes.  This method is called
	 * when a provider is unable to fetch a location or if the provider
	 * has recently become available after a period of unavailability.
	 * 
	 * @param	provider			The name of the location provider
	 * 								associated with this update.
	 * @param	status				OUT_OF_SERVICE if the provider is out of
	 * 								service, and this is not expected to
	 * 								change in the near future;
	 * 								TEMPORARILY_UNAVAILABLE if the provider
	 * 								is temporarily unavailable but is expected
	 * 								to be available shortly; and AVAILABLE if
	 * 								the provider is currently available.
	 * @param	extras				An optional Bundle which will contain
	 * 								provider specific status variables.
	 * 								Common key/value pairs:
	 * 								  satellites - the number of satellites
	 * 											   used to derive the fix.
	 */
	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {
		Log.i(TAG, "Provider status: " + provider + "=" + status);
		if (provider.equals(LocationManager.GPS_PROVIDER)) {
			synchronized (this) {
				switch (status) {
				case LocationProvider.OUT_OF_SERVICE:
					gpsState = GpsState.OUT_OF_SERVICE;
		            positionValid = false;
					break;
				case LocationProvider.TEMPORARILY_UNAVAILABLE:
					gpsState = GpsState.TEMP_OOS;
		            positionValid = false;
					break;
				case LocationProvider.AVAILABLE:
					gpsState = GpsState.ENABLED;
					break;
				default:
					gpsState = GpsState.UNKNOWN;
			        positionValid = false;
					break;
				}
				Integer sats = extras.getInt("satellites");
				gpsSats = sats == null ? -1 : sats;
				postUpdate();
			}
		}
	}


    /**
     * Post an update.  Use a Handler to get back on the main thread.
     */
	private void postUpdate() {
		updateHandler.sendEmptyMessage(1);
	}
	

	// ******************************************************************** //
	// Location and Status Updates.
	// ******************************************************************** //
	
    /**
     * Display the current date and time.
     */
	private void update() {
//      Log.v(TAG, "Location: update()");
	    
		int oldTz = nautTimezone;
		
		synchronized (this) {
			long time = System.currentTimeMillis();
			
			if (currentLoc != null) {
				if (currentPos != null)
					currentLocMsg = PointOfInterest.describePosition(currentPos);
				else
					currentLocMsg = "";
				
				// Calculate the age of the fix and make a status
				// message based on that.
				long age = time - currentLoc.getTime();
				gpsStatusMsg = getGpsStatus(age);
				
				// Calculate the nautical timezone based on the longitude.
				nautTimezone = (int) Math.round(currentLoc.getLongitude() / 15.0);
			} else {
				currentLocMsg = "";
				gpsStatusMsg = getGpsStatus(-1);
				nautTimezone = TZ_UNKNOWN;
			}

			lastUpdate = time;
		}
		
		// If we have a new position, tell folks.
		if (!prevPositionValid && positionValid) {
	        for (Listener listener : locationListeners) {
	            listener.newLoc(gpsState, gpsStatusMsg, currentLoc, currentLocMsg);
	            listener.newPos(gpsState, gpsStatusMsg, currentPos, currentLocMsg);
	        }
		}
		prevPositionValid = positionValid;
		
		// Notify all the registered clients that we have an update.
		for (Listener listener : locationListeners) {
			listener.locChange(gpsState, gpsStatusMsg, currentLoc, currentLocMsg);
			listener.posChange(gpsState, gpsStatusMsg, currentPos, currentLocMsg);
		}
		
		// Notify all the registered timezone clients if the timezone
		// has changed.
		if (nautTimezone != oldTz)
			for (ZoneListener listener : timezoneListeners)
				listener.tzChange(nautTimezone);
	}

	
	/**
	 * Get the current GPS status message.
	 * 
	 * @param	age			Age of the latest fix, in ms; -1 if none.
	 * @return				Current GPS status message.
	 */
	private String getGpsStatus(long age) {
		// If GPS isn't running, return the status.
		if (gpsState != GpsState.ENABLED && gpsState != GpsState.TEMP_OOS)
			return appContext.getString(gpsState.messageId);
		if (age < 0)
			return appContext.getString(GpsState.TEMP_OOS.messageId);

		Location l = currentLoc;

		// If the fix is old (more than 1 minute), warn the user.
		if (age / 1000 > 120)
			return String.format(lastFixMsg, age / 60000);

		// Otherwise just show the accuracy.
		String msg = "";
		if (l.hasAccuracy())
			msg = "Accuracy: " + Math.round(l.getAccuracy()) + " m";

		// Add the number of sats if we know.
		if (gpsSats >= 0)
			msg += (msg == "" ? "" : "; ") + gpsSats + " satellites";

		return msg;
	}
	
	
	// ******************************************************************** //
	// Debug Control.
	// ******************************************************************** //

	/**
	 * Fast-forward the location by bumping up the lat and lon offsets.
	 * Used for debugging.
	 * 
	 * @param	lat				Amount in degrees to add to the latitude offset.
	 * @param	lon				Amount in degrees to add to the longitude offset.
	 */
	void adjust(double lat, double lon) {
		offsetLat += lat;
		offsetLon += lon;

		postUpdate();
	}


	/**
	 * Reset the location offset.  Used to cancel debugging.
	 */
	void adjustReset() {
		offsetLat = 0.0;
		offsetLon = 0.0;

		postUpdate();
	}


	// ******************************************************************** //
	// Class Data.
	// ******************************************************************** //

    // Debugging tag.
	private static final String TAG = "onwatch";

	// The instance of the location model; null if not created yet.
	private static LocationModel modelInstance = null;

	
	// ******************************************************************** //
	// Private Data.
	// ******************************************************************** //

	// Parent app we're running in.
	private Context appContext;
	
	// Handler used to post updates from the GPS thread back to our
	// main thread.
	private Handler updateHandler;

	// List of listeners registered to be called when the location or
	// location state changes.
	private ArrayList<Listener> locationListeners = null;
	
	// List of listeners registered to be called when the nautical
	// timezone changes.
	private ArrayList<ZoneListener> timezoneListeners = null;

    // The location manager, from which we get location updates.
    private LocationManager locationManager;
	
	// The current state of the GPS receiver.
	private GpsState gpsState = GpsState.UNKNOWN;
	
	// The current GPS status message.
	private String gpsStatusMsg = "";

	// "Time since last fix" message.
	private String lastFixMsg;
	
	// The current location, null if not known.
	private Location currentLoc = null;
	
	// Current location as a Position.
	private Position currentPos = null;
	
	// Whether the current position is valid.  This will be false if
	// we have no position, or if it is a "stale" position; e.g. if the GPS
	// has no signal.
	private boolean positionValid = false;
    private boolean prevPositionValid = false;
	
	// Current nautical timezone, as hours offset from UTC.  Only valid when
	// currentPos != null.  Otherwise set to TZ_UNKNOWN.
	private int nautTimezone = TZ_UNKNOWN;

	// Description of our current location.
	private String currentLocMsg = "";
	
	// The number of GPS satellites locked on.
	private int gpsSats;
	
	// Time at which we last updated the model.
	private long lastUpdate = 0;

	// Debug: offset in degrees to add to the correct latitude and
	// longitude.  Used for "fast-forwarding" our location.
	private double offsetLat = 0.0;
	private double offsetLon = 0.0;

}

