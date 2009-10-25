
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


import org.hermit.tricorder.Tricorder.Sound;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.location.GpsSatellite;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.location.GpsStatus;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;


/**
 * A view which displays geographical data.
 */
class GeoView
	extends DataView
	implements LocationListener, GpsStatus.Listener
{

    // ******************************************************************** //
    // Local Constants and Classes.
    // ******************************************************************** //

    /**
     * Number of GPS satellites we can handle.
     */
    static final int NUM_SATS = 32;
    
    /**
     * Cached info on a satellite's status.
     */
    static final class GpsInfo {
        // Time at which this status was retrieved.  If 0, not valid.
        long time = 0;
        
        float azimuth;
        float elev;
        float snr;
        boolean hasAl;
        boolean hasEph;
        boolean used;
        
        // Time at which this satellite was last used in a fix.
        long usedTime = 0;
        
        // Colour to plot it with, based on status.
        int colour;
    }
    

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
		
		// Set up the satellite data cache.
		satCache = new GpsInfo[NUM_SATS];
		for (int i = 0; i < NUM_SATS; ++i) {
		    satCache[i] = new GpsInfo();
		}

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
       	
       	satElement = new SatelliteElement(context, sh,
       	                                  HEAD_BG_COL, HEAD_TEXT_COL);
        String[][] satStr = {
                { getRes(R.string.title_sats), "", getRes(R.string.msgNoData) }
            };
        satElement.setText(satStr);
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

        if (bounds.right - bounds.left < bounds.bottom - bounds.top)
            layoutPortrait(bounds);
        else
            layoutLandscape(bounds);
	}


    /**
     * Lay out in portrait mode.
     * 
     * @param   bounds      The bounding rect of this element within
     *                      its parent View.
     */
    private void layoutPortrait(Rect bounds) {
        // Lay out the displays.
        int pad = appContext.getInterPadding();
        int sx = bounds.left + pad;
        int ex = bounds.right;
        int y = bounds.top;
        
        int netHeight = netElement.getPreferredHeight();
        netElement.setGeometry(new Rect(sx, y, ex, y + netHeight));
        y += netHeight + pad;
        
        int gpsHeight = gpsElement.getPreferredHeight();
        gpsElement.setGeometry(new Rect(sx, y, ex, y + gpsHeight));
        y += gpsHeight + pad;
        
        satBounds = new Rect(sx, y, ex, bounds.bottom);
        satElement.setGeometry(satBounds);
    }


    /**
     * Lay out in landscape mode.
     * 
     * @param   bounds      The bounding rect of this element within
     *                      its parent View.
     */
    private void layoutLandscape(Rect bounds) {
        // Lay out the displays.
        int pad = appContext.getInterPadding();
        int sx = bounds.left + pad;
        int ex = bounds.right;
        
        int cw = (ex - sx - pad) / 2;
        if (cw < gpsElement.getPreferredWidth())
            cw = gpsElement.getPreferredWidth();
        int csx = sx;
        int cex = csx + cw;
        int y = bounds.top;
        
        int netHeight = netElement.getPreferredHeight();
        netElement.setGeometry(new Rect(csx, y, cex, y + netHeight));
        y += netHeight + pad;
        
        int gpsHeight = gpsElement.getPreferredHeight();
        gpsElement.setGeometry(new Rect(csx, y, cex, y + gpsHeight));

        csx = cex + pad;
        cex = ex;
        y = bounds.top;
        
        satElement.setGeometry(new Rect(csx, y, cex, bounds.bottom));
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
        		
        		// For the GPS, also add a GPS status listener to get
        		// additional satellite and fix info.
        		if (name.equals(LocationManager.GPS_PROVIDER))
        		    locationManager.addGpsStatusListener(this);
        		
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
		if (satCache != null)
		    satElement.setValues(satCache);
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
	 * @param	loc			   The new location, as a Location object.
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
	 * @param	provider		The name of the location provider
	 * 							associated with this update.
	 */
	public void onProviderDisabled(String provider) {
		Log.i(TAG, "Provider disabled: " + provider);
		synchronized (surfaceHolder) {
			if (provider.equals(LocationManager.NETWORK_PROVIDER))
				netElement.setStatus(getRes(R.string.msgDisabled));
			else if (provider.equals(LocationManager.GPS_PROVIDER)) {
				gpsElement.setStatus(getRes(R.string.msgDisabled));
                satElement.clearValues();
			}
		}
	}
	 
	 
	/**
	 * Called when the provider is enabled by the user.
	 * 
	 * @param	provider		The name of the location provider
	 * 							associated with this update.
	 */
	public void onProviderEnabled(String provider) {
		Log.i(TAG, "Provider enabled: " + provider);
		synchronized (surfaceHolder) {
			if (provider.equals(LocationManager.NETWORK_PROVIDER))
				netElement.setStatus(null);
			else if (provider.equals(LocationManager.GPS_PROVIDER)) {
				gpsElement.setStatus(null);
                satElement.clearValues();
			}
		}
	}
	  

	/**
	 * Called when the provider status changes.  This method is called
	 * when a provider is unable to fetch a location or if the provider
	 * has recently become available after a period of unavailability.
	 * 
	 * @param	provider		The name of the location provider
	 * 							associated with this update.
	 * @param	status			OUT_OF_SERVICE if the provider is out of
	 * 							service, and this is not expected to
	 * 							change in the near future;
	 * 							TEMPORARILY_UNAVAILABLE if the provider
	 * 							is temporarily unavailable but is expected
	 * 							to be available shortly; and AVAILABLE if
	 * 							the provider is currently available.
	 * @param	extras			An optional Bundle which will contain
	 * 							provider specific status variables.
	 * 							Common key/value pairs:
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
			else if (provider.equals(LocationManager.GPS_PROVIDER)) {
				gpsElement.setStatus(msg);
			}
		}
	}

	
    /**
     * Called to report changes in the GPS status.
     * 
     * @param   event           Event number describing what has changed.
     */
    @Override
    public void onGpsStatusChanged(int event) {
        switch (event) {
        case GpsStatus.GPS_EVENT_SATELLITE_STATUS:
            gpsStatus = locationManager.getGpsStatus(gpsStatus);
            Iterable<GpsSatellite> sats = gpsStatus.getSatellites();
            long time = System.currentTimeMillis();
            for (GpsSatellite sat : sats) {
                int prn = sat.getPrn();
                if (prn >= NUM_SATS)
                    continue;
                
                GpsInfo ginfo = satCache[prn];
                ginfo.time = time;
                ginfo.azimuth = sat.getAzimuth();
                ginfo.elev = sat.getElevation();
                ginfo.snr = sat.getSnr();
                ginfo.hasAl = sat.hasAlmanac();
                ginfo.hasEph = sat.hasEphemeris();
                ginfo.used = sat.usedInFix();
            }
            
//            GpsInfo g7 = satCache[7];
//            g7.time = time;
//            g7.azimuth = 340;
//            g7.elev = 30;
//            g7.snr = 40;
//            g7.hasAl = true;
//            g7.hasEph = true;
//            g7.used = true;
//            GpsInfo g14 = satCache[14];
//            g14.time = time;
//            g14.azimuth = 75;
//            g14.elev = 45;
//            g14.snr = 30;
//            g14.hasAl = true;
//            g14.hasEph = true;
//            g14.used = false;
//            GpsInfo g21 = satCache[21];
//            g21.time = time;
//            g21.azimuth = 205;
//            g21.elev = 60;
//            g21.snr = 30;
//            g21.hasAl = true;
//            g21.hasEph = false;
//            g21.used = false;

            for (int prn = 0; prn < NUM_SATS; ++prn) {
                GpsInfo ginfo = satCache[prn];
                if (time - ginfo.time > DATA_CACHE_TIME) {
                    ginfo.time = 0;
                    ginfo.usedTime = 0;
                } else {
                    if (ginfo.used)
                        ginfo.usedTime = time;
                    else if (time - ginfo.usedTime <= DATA_CACHE_TIME)
                        ginfo.used = true;
                    else
                        ginfo.usedTime = 0;
                    int colour = ginfo.used ? 0 : ginfo.hasEph ? 1 : ginfo.hasAl ? 2 : 3;
                    ginfo.colour = COLOUR_PLOT[colour];
                }
            }
           
            satElement.setValues(satCache);
            break;
        case GpsStatus.GPS_EVENT_STARTED:
        case GpsStatus.GPS_EVENT_STOPPED:
        case GpsStatus.GPS_EVENT_FIRST_FIX:
            break;
        }
    }


    // ******************************************************************** //
    // Input.
    // ******************************************************************** //

    /**
     * Handle touch screen motion events.
     * 
     * @param   event           The motion event.
     * @return                  True if the event was handled, false otherwise.
     */
    @Override
    public boolean handleTouchEvent(MotionEvent event) {
        final int x = (int) event.getX();
        final int y = (int) event.getY();
        final int action = event.getAction();
        boolean done = false;

        synchronized (surfaceHolder) {
            if (action == MotionEvent.ACTION_DOWN) {
                if (satBounds != null && satBounds.contains(x, y)) {
                    satElement.toggleMode();
                    appContext.postSound(Sound.CHIRP_LOW);
                    done = true;
                }
            }
        }

        event.recycle();
        return done;
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
		satElement.draw(canvas, now);
	}

	
    // ******************************************************************** //
    // Class Data.
    // ******************************************************************** //

    // Debugging tag.
	private static final String TAG = "tricorder";
	
	// Location providers we monitor.
    private static final String[] locationProviders = {
    	LocationManager.NETWORK_PROVIDER,
		LocationManager.GPS_PROVIDER
	};

	// Heading bar background and text colours.
	private static final int HEAD_BG_COL = 0xffc0a000;
	private static final int HEAD_TEXT_COL = 0xff000000;
	
	// Colours to represent sat status by.
    private static final int[] COLOUR_PLOT = {
        0xff00ffff, 0xff00ff00, 0xffffff00, 0xffff9000,
    };

    // Time in ms for which cached satellite data is valid.
    private static final int DATA_CACHE_TIME = 10 * 1000;
    
	
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
	
	// Display pane for satellite status.  It's current bounds.
    private SatelliteElement satElement;
    private Rect satBounds;
	
	// Latest GPS status.  If null, we haven't got one yet.
	private GpsStatus gpsStatus = null;
	
	// Cached satellite info.
	private GpsInfo[] satCache;

}

