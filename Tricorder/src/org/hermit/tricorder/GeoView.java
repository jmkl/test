
/**
 * Tricorder: turn your phone into a tricorder.
 * 
 * This is an Android implementation of a Star Trek tricorder, based on
 * the phone's own sensors.  It's also a demo project for sensor access.
 *
 *   This program is free software; you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License version 2
 *   as published by the Free Software Foundation (see COPYING).
 * 
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 */


package org.hermit.tricorder;


import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceHolder;


/**
 * A view which displays geographical data.
 */
class GeoView
	extends DataView
	implements LocationListener
{

	// ******************************************************************** //
	// Constructor.
	// ******************************************************************** //

	/**
	 * Set up this view.
	 * 
	 * @param	context			Parent application context.
     * @param	sh				SurfaceHolder we're drawing in.
	 */
	public GeoView(Tricorder context, SurfaceHolder sh) {
		super(context, sh);
		
		appContext = context;
		surfaceHolder = sh;

		// Get the information providers we need.
        locationManager =
        	(LocationManager) context.getSystemService(Context.LOCATION_SERVICE);

		netElement = new GeoElement(context, sh,
									HEAD_BG_COL, HEAD_TEXT_COL, false);
       	String[][] netStr = {
        		{ getRes(R.string.title_network), "", getRes(R.string.msgNoData) }
        	};
       	netElement.setText(netStr);
		
		gpsElement = new GeoElement(context, sh,
									HEAD_BG_COL, HEAD_TEXT_COL, true);
       	String[][] gpsStr = {
        		{ getRes(R.string.title_gps), "", getRes(R.string.msgNoData) }
        	};
       	gpsElement.setText(gpsStr);
	}

	   
    // ******************************************************************** //
	// Geometry Management.
	// ******************************************************************** //

    /**
     * This is called during layout when the size of this element has
     * changed.  This is where we first discover our size, so set
     * our geometry to match.
     * 
	 * @param	bounds		The bounding rect of this element within
	 * 						its parent View.
     */
	@Override
	protected void setGeometry(Rect bounds) {
		super.setGeometry(bounds);
		
		// Lay out the displays.
		int sx = bounds.left + appContext.getInterPadding();
		int ex = bounds.right;
		int y = bounds.top;
		
		int netHeight = netElement.getPreferredHeight();
		netElement.setGeometry(new Rect(sx, y, ex, y + netHeight));
        y += netHeight + appContext.getInterPadding();
		
		int gpsHeight = gpsElement.getPreferredHeight();
		gpsElement.setGeometry(new Rect(sx, y, ex, y + gpsHeight));
        y += gpsHeight;
	}

	
	// ******************************************************************** //
	// Data Management.
	// ******************************************************************** //
	
	/**
	 * Start this view.  This notifies the view that it should start
	 * receiving and displaying data.  The view will also get tick events
	 * starting here.
	 */
	@Override
	public void start() {
        // Register for location updates.
        for (String name : locationProviders) {
        	LocationProvider prov = locationManager.getProvider(name);
        	if (prov != null) {
        		locationManager.requestLocationUpdates(name, 60000, 0f, this);

        		// Prime the pump with the last known location.
        		Location prime = locationManager.getLastKnownLocation(name);
        		if (prime != null)
        			onLocationChanged(prime);
        	}
        }
	}
	
	
	/**
	 * A 1-second tick event.  Can be used for housekeeping and
	 * async updates.
	 * 
	 * @param	time				The current time in millis.
	 */
	@Override
	public void tick(long time) {
		netElement.tick(time);
		gpsElement.tick(time);
	}
	
	
	/**
	 * This view's aux button has been clicked.
	 */
	@Override
	public void auxButtonClick() {
		// Here is where we used to toggle the GPS power -- we can't
	    // any more.
	}
	

	/**
	 * Stop this view.  This notifies the view that it should stop
	 * receiving and displaying data, and generally stop using
	 * resources.
	 */
	@Override
	public void stop() {
		for (String name : locationProviders) {
			LocationProvider prov = locationManager.getProvider(name);
			if (prov != null)
				locationManager.removeUpdates(this);
		}
	}
	
	
	/**
	 * Called when the location has changed.  There are no restrictions
	 * on the use of the supplied Location object.
	 * 
	 * @param	loc			The new location, as a Location object.
	 */
	public void onLocationChanged(Location loc) {
		synchronized (surfaceHolder) {
			if (loc.getProvider().equals(LocationManager.NETWORK_PROVIDER))
				netElement.setValue(loc);
			else if (loc.getProvider().equals(LocationManager.GPS_PROVIDER))
				gpsElement.setValue(loc);
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
	public void onProviderDisabled(String provider) {
		Log.i(TAG, "Provider disabled: " + provider);
		synchronized (surfaceHolder) {
			if (provider.equals(LocationManager.NETWORK_PROVIDER))
				netElement.setStatus(getRes(R.string.msgDisabled));
			else if (provider.equals(LocationManager.GPS_PROVIDER)) {
				gpsElement.setStatus(getRes(R.string.msgDisabled));
			}
		}
	}
	 
	 
	/**
	 * Called when the provider is enabled by the user.
	 * 
	 * @param	provider			The name of the location provider
	 * 								associated with this update.
	 */
	public void onProviderEnabled(String provider) {
		Log.i(TAG, "Provider enabled: " + provider);
		synchronized (surfaceHolder) {
			if (provider.equals(LocationManager.NETWORK_PROVIDER))
				netElement.setStatus(null);
			else if (provider.equals(LocationManager.GPS_PROVIDER)) {
				gpsElement.setStatus(null);
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
	public void onStatusChanged(String provider, int status, Bundle extras) {
		Log.i(TAG, "Provider status: " + provider + "=" + status);
		synchronized (surfaceHolder) {
			String msg = null;
			if (status == LocationProvider.OUT_OF_SERVICE)
				msg = getRes(R.string.msgOffline);
			if (provider.equals(LocationManager.NETWORK_PROVIDER))
				netElement.setStatus(msg);
			else if (provider.equals(LocationManager.GPS_PROVIDER))
				gpsElement.setStatus(msg);
		}
	}


	// ******************************************************************** //
	// View Drawing.
	// ******************************************************************** //
	
	/**
	 * This method is called to ask the view to draw itself.
	 * 
	 * @param	canvas			Canvas to draw into.
	 * @param	now				Current system time in ms.
	 */
	@Override
	protected void draw(Canvas canvas, long now) {
		super.draw(canvas, now);
		
		// Draw the data views.
		netElement.draw(canvas, now);
		gpsElement.draw(canvas, now);
	}

	
    // ******************************************************************** //
    // Class Data.
    // ******************************************************************** //

    // Debugging tag.
	@SuppressWarnings("unused")
	private static final String TAG = "tricorder";
	
	// Location providers we monitor.
    private static final String[] locationProviders = {
    	LocationManager.NETWORK_PROVIDER,
		LocationManager.GPS_PROVIDER
	};

	// Heading bar background and text colours.
	private static final int HEAD_BG_COL = 0xffc0a000;
	private static final int HEAD_TEXT_COL = 0xff000000;
	
	
	// ******************************************************************** //
	// Private Data.
	// ******************************************************************** //

	// Our application context.
	private Tricorder appContext;
	
	// The surface we're drawing on.
	private SurfaceHolder surfaceHolder;

    // The location manager, from which we get location updates.
    private LocationManager locationManager;

	// Display panes for the network and GPS locations, with heading,
	// position, and course data for each.
	private GeoElement netElement;
	private GeoElement gpsElement;
	
}

