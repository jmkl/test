
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


import org.hermit.android.core.SurfaceRunner;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.RectF;
import android.hardware.GeomagneticField;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.GpsSatellite;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.Surface;


/**
 * A view which displays geographical data.
 */
class GeoView
	extends DataView
	implements LocationListener, GpsStatus.Listener, SensorEventListener
{

    // ******************************************************************** //
    // Local Constants and Classes.
    // ******************************************************************** //

    /**
     * Number of GPS satellites we can handle.  Satellites are numbered 1-32;
     * this is the "PRN number", i.e. a number which identifies the
     * satellite's PRN, which is a 1023-bit number.
     */
    static final int NUM_SATS = 32;
    
    /**
     * Cached info on a satellite's status.
     */
    static final class GpsInfo {
        GpsInfo(int prn) {
            this.prn = prn;
            this.name = "" + prn;
        }
        
        final int prn;
        final String name;
        
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
        
        // Bounding rectangle for the display in the sky view.
        RectF rect;
        
        // Label position for the display in the sky view.
        float textX;
        float textY;
        
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
     * @param   parent          Parent surface.
     * @param   sman            The SensorManager to get data from.
	 */
	public GeoView(Tricorder context, SurfaceRunner parent, SensorManager sman) {
		super(context, parent);
	      
        // Get some UI strings.
        msgDisabled = parent.getRes(R.string.msgDisabled);
        msgOffline = parent.getRes(R.string.msgOffline);

		appContext = context;
		sensorManager = sman;

		// Set up the satellite data cache.  For simplicity, we allocate
		// NUM_SATS + 1 so we can index by PRN number.
		satCache = new GpsInfo[NUM_SATS + 1];
		for (int i = 1; i <= NUM_SATS; ++i)
		    satCache[i] = new GpsInfo(i);
		numSats = 0;

		// Get the information providers we need.
        locationManager =
        	(LocationManager) context.getSystemService(Context.LOCATION_SERVICE);

		netElement = new GeoElement(parent,
									HEAD_BG_COL, HEAD_TEXT_COL, false);
       	netElement.setText(0, 0, parent.getRes(R.string.title_network));
		
		gpsElement = new GeoElement(parent,
									HEAD_BG_COL, HEAD_TEXT_COL, true);
       	gpsElement.setText(0, 0, parent.getRes(R.string.title_gps));
       	
       	satElement = new SatelliteElement(parent,
       	                                  HEAD_BG_COL, HEAD_TEXT_COL);
        satElement.setText(0, 0, parent.getRes(R.string.title_sats));
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
	public void setGeometry(Rect bounds) {
		super.setGeometry(bounds);

        if (bounds.right - bounds.left < bounds.bottom - bounds.top)
            layoutPortrait(bounds);
        else
            layoutLandscape(bounds);
        
        satElement.formatValues(satCache);
	}


    /**
     * Lay out in portrait mode.
     * 
     * @param   bounds      The bounding rect of this element within
     *                      its parent View.
     */
    private void layoutPortrait(Rect bounds) {
        // Lay out the displays.
        int pad = getInterPadding();
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
        int pad = getInterPadding();
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
        
        satBounds = new Rect(csx, y, cex, bounds.bottom);
        satElement.setGeometry(satBounds);
    }

    
    /**
     * Set the device rotation, so that we
     * can adjust the sensor axes to match the screen axes.
     * 
     * @param   rotation    Device rotation, as one of the
     *                      Surface.ROTATION_XXX flags.
     */
    public void setRotation(int rotation) {
        switch (rotation) {
        case Surface.ROTATION_0:
            deviceTransformation = TRANSFORM_0;
            break;
        case Surface.ROTATION_90:
            deviceTransformation = TRANSFORM_90;
            break;
        case Surface.ROTATION_180:
            deviceTransformation = TRANSFORM_180;
            break;
        case Surface.ROTATION_270:
            deviceTransformation = TRANSFORM_270;
            break;
        }
    }
    

    // ******************************************************************** //
    // Configuration.
    // ******************************************************************** //

    /**
     * Set the units in which to display numeric data.
     * 
     * @param   unit            Units to display.
     */
    @Override
    void setDataUnits(Tricorder.Unit unit) {
        netElement.setDataUnits(unit);
        gpsElement.setDataUnits(unit);
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
	void start() {
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
        
        // Get orientation updates.
        registerSensor(Sensor.TYPE_ACCELEROMETER);
        registerSensor(Sensor.TYPE_MAGNETIC_FIELD);
	}
	
	
	private final void registerSensor(int type) {
        Sensor sensor = sensorManager.getDefaultSensor(type);
        if (sensor != null)
            sensorManager.registerListener(this, sensor,
                                           SensorManager.SENSOR_DELAY_GAME);
	}
	
	
	/**
	 * A 1-second tick event.  Can be used for housekeeping and
	 * async updates.
	 * 
	 * @param	time				The current time in millis.
	 */
	@Override
	void tick(long time) {
		netElement.tick(time);
		gpsElement.tick(time);
		if (satCache != null)
		    satElement.setValues(satCache, numSats);
	}
	
	
	/**
	 * This view's aux button has been clicked.
	 */
	@Override
	void auxButtonClick() {
		// Here is where we used to toggle the GPS power -- we can't
	    // any more.
	}
	

	/**
	 * Stop this view.  This notifies the view that it should stop
	 * receiving and displaying data, and generally stop using
	 * resources.
	 */
	@Override
	void stop() {
		for (String name : locationProviders) {
			LocationProvider prov = locationManager.getProvider(name);
			if (prov != null)
				locationManager.removeUpdates(this);
		}
		
        sensorManager.unregisterListener(this);
	}
	

    // ******************************************************************** //
    // Location Management.
    // ******************************************************************** //

	/**
	 * Called when the location has changed.  There are no restrictions
	 * on the use of the supplied Location object.
	 * 
	 * @param	loc			   The new location, as a Location object.
	 */
	public void onLocationChanged(Location loc) {
	    try {
	        synchronized (this) {
	            if (loc.getProvider().equals(LocationManager.NETWORK_PROVIDER)) {
	                netLocation = loc;
	                netElement.setValue(loc);
	            } else if (loc.getProvider().equals(LocationManager.GPS_PROVIDER)) {
	                gpsLocation = loc;
	                gpsElement.setValue(loc);
	            }
	        }
	    } catch (Exception e) {
	        appContext.reportException(e);
	    }
	}


    /**
     * Called when the provider is enabled by the user.
     * 
     * @param   provider        The name of the location provider
     *                          associated with this update.
     */
    public void onProviderEnabled(String provider) {
        try {
            Log.i(TAG, "Provider enabled: " + provider);
            synchronized (this) {
                if (provider.equals(LocationManager.NETWORK_PROVIDER))
                    netElement.setStatus(null);
                else if (provider.equals(LocationManager.GPS_PROVIDER)) {
                    gpsElement.setStatus(null);
                    satElement.clearValues();
                }
            }
        } catch (Exception e) {
            appContext.reportException(e);
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
	    try {
	        Log.i(TAG, "Provider disabled: " + provider);
	        synchronized (this) {
	            if (provider.equals(LocationManager.NETWORK_PROVIDER))
	                netElement.setStatus(msgDisabled);
	            else if (provider.equals(LocationManager.GPS_PROVIDER)) {
	                gpsElement.setStatus(msgDisabled);
	                satElement.clearValues();
	            }
	        }
	    } catch (Exception e) {
	        appContext.reportException(e);
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
	    try {
	        Log.i(TAG, "Provider status: " + provider + "=" + status);
	        synchronized (this) {
	            String msg = null;
	            if (status == LocationProvider.OUT_OF_SERVICE)
	                msg = msgOffline;
	            if (provider.equals(LocationManager.NETWORK_PROVIDER))
	                netElement.setStatus(msg);
	            else if (provider.equals(LocationManager.GPS_PROVIDER)) {
	                gpsElement.setStatus(msg);
	            }
	        }
	    } catch (Exception e) {
	        appContext.reportException(e);
	    }
	}


    /**
     * Called to report changes in the GPS status.
     * 
     * @param   event           Event number describing what has changed.
     */
	@Override
	public void onGpsStatusChanged(int event) {
	    try {
	        switch (event) {
	        case GpsStatus.GPS_EVENT_SATELLITE_STATUS:
	            gpsStatus = locationManager.getGpsStatus(gpsStatus);
	            Iterable<GpsSatellite> sats = gpsStatus.getSatellites();
	            long time = System.currentTimeMillis();
	            for (GpsSatellite sat : sats) {
	                int prn = sat.getPrn();
	                if (prn < 1 || prn > NUM_SATS)
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

	            //            // Fake some satellites, for testing.
	            //            Random r = new Random();
	            //            r.setSeed(4232);
	            //            for (int i = 1; i <= NUM_SATS; ++i) {
	            //                GpsInfo ginfo = satCache[i];
	            //                if (i % 3 == 0) {
	            //                    ginfo.time = time - r.nextInt(5000);
	            //                    ginfo.azimuth = r.nextFloat() * 360.0f;
	            //                    ginfo.elev = r.nextFloat() * 90.0f;
	            //                    ginfo.snr = 12;
	            //                    ginfo.hasAl = r.nextInt(4) != 0;
	            //                    ginfo.hasEph = ginfo.hasAl && r.nextInt(3) != 0;
	            //                    ginfo.used = ginfo.hasEph && r.nextBoolean();
	            //                } else {
	            //                    ginfo.time = 0;
	            //                }
	            //            }

	            // Post-process the sats.
	            numSats = 0;
	            for (int prn = 1; prn <= NUM_SATS; ++prn) {
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
	                    ++numSats;
	                }
	            }

	            satElement.setValues(satCache, numSats);
	            break;
	        case GpsStatus.GPS_EVENT_STARTED:
	        case GpsStatus.GPS_EVENT_STOPPED:
	        case GpsStatus.GPS_EVENT_FIRST_FIX:
	            break;
	        }
	    } catch (Exception e) {
	        appContext.reportException(e);
	    }
	}


    // ******************************************************************** //
    // Geomagnetic Data Management.
    // ******************************************************************** //

    /**
     * Check the geomagnetic field, if the information we have isn't
     * up to date.
     */
    private void checkGeomag() {
        // See if we have valid data.
        long now = System.currentTimeMillis();
        if (geomagneticField != null && now - geomagneticTime < GEOMAG_CACHE_TIME)
            return;
        
        // Get our best location.  If we don't have one, can't do nothing.
        final Location loc = gpsLocation != null ? gpsLocation : netLocation;
        if (loc == null)
            return;

        // Get the geomag data. 
        geomagneticField = new GeomagneticField((float) loc.getLatitude(),
                                                (float) loc.getLongitude(),
                                                (float) loc.getAltitude(), now);    
        geomagneticTime = now;
    }
  

    // ******************************************************************** //
    // Sensor Management.
    // ******************************************************************** //

    /**
     * Called when the accuracy of a sensor has changed.
     * 
     * @param   sensor          The sensor being monitored.
     * @param   accuracy        The new accuracy of this sensor.
     */
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Don't need anything here.
    }


    /**
     * Called when sensor values have changed.
     *
     * @param   event           The sensor event.
     */
    public void onSensorChanged(SensorEvent event) {
        try {
            final float[] values = event.values;
            if (values.length < 3)
                return;

            int type = event.sensor.getType();
            if (type == Sensor.TYPE_ACCELEROMETER) {
                if (accelValues == null)
                    accelValues = new float[3];
                multiply(values, deviceTransformation, accelValues);
            } else if (type == Sensor.TYPE_MAGNETIC_FIELD) {
                if (magValues == null)
                    magValues = new float[3];
                multiply(values, deviceTransformation, magValues);
            }
            checkGeomag();
            if (accelValues == null || magValues == null || geomagneticField == null)
                return;

            // Get the device rotation matrix.
            float[] rotate = new float[9];
            boolean ok = SensorManager.getRotationMatrix(rotate, null, accelValues, magValues);
            if (!ok)
                return;

            // Compute the device's orientation based on the rotation matrix.
            final float[] orient = new float[3];
            SensorManager.getOrientation(rotate, orient);

            // Get the azimuth of device Y from magnetic north.  Compensate for
            // magnetic declination.
            final float azimuth = (float) Math.toDegrees(orient[0]);
            final float dec = geomagneticField.getDeclination();
            satElement.setAzimuth(azimuth + dec, dec);
        } catch (Exception e) {
            appContext.reportException(e);
        }
    }

    
    /*
     * Result[x] = vals[0]*tran[x][0] * vals[1]*tran[x][1] * vals[2]*tran[x][2].
     */
    private static final void multiply(float[] vals, int[][] tran, float[] result) {
        for (int x = 0; x < 3; ++x) {
            float r = 0;
            for (int y = 0; y < 3; ++y)
                r += vals[y] * tran[x][y];
            result[x] = r;
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
        boolean done = false;
        try {
            final int x = (int) event.getX();
            final int y = (int) event.getY();
            final int action = event.getAction();

            synchronized (this) {
                if (action == MotionEvent.ACTION_DOWN) {
                    if (satBounds != null && satBounds.contains(x, y)) {
                        satElement.toggleMode();
                        appContext.soundSecondary();
                        done = true;
                    }
                }
            }
        } catch (Exception e) {
            appContext.reportException(e);
        }

        return done;
    }


	// ******************************************************************** //
	// View Drawing.
	// ******************************************************************** //
	
	/**
	 * This method is called to ask the view to draw itself.
	 * 
	 * @param	canvas		Canvas to draw into.
	 * @param	now			Current system time in ms.
     * @param   bg          Iff true, tell the gauge to draw its background
     *                      first.
	 */
	@Override
	public void draw(Canvas canvas, long now, boolean bg) {
		super.draw(canvas, now, bg);
		
		// Draw the data views.
		netElement.draw(canvas, now, bg);
		gpsElement.draw(canvas, now, bg);
		satElement.draw(canvas, now, bg);
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

    // Time in ms for which cached geomagnetic data is valid.
    private static final int GEOMAG_CACHE_TIME = 2 * 3600 * 1000;
    
    // Co-ordinate transformations.
    private static final int[][] TRANSFORM_0 = {
        {  1,  0,  0 },
        {  0,  1,  0 },
        {  0,  0,  1 },
    };
    private static final int[][] TRANSFORM_90 = {
        {  0, -1,  0 },
        {  1,  0,  0 },
        {  0,  0,  1 },
    };
    private static final int[][] TRANSFORM_180 = {
        { -1,  0,  0 },
        {  0, -1,  0 },
        {  0,  0,  1 },
    };
    private static final int[][] TRANSFORM_270 = {
        {  0,  1,  0 },
        { -1,  0,  0 },
        {  0,  0,  1 },
    };

	
	// ******************************************************************** //
	// Private Data.
	// ******************************************************************** //

    // Application handle.
    private final Tricorder appContext;
    
    // The sensor manager, which we use to interface to all sensors.
    private SensorManager sensorManager;

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
	
	// Cached satellite info.  Indexed by the satellite's PRN number,
	// which is in the range 1-NUM_SATS.
	private GpsInfo[] satCache;
	
	// Number of satellites for which we have info.
	private int numSats;

    // The most recent network and GPS locations.
    private Location netLocation = null;
    private Location gpsLocation = null;
    
    // Current device orientation, as a matrix which can be used
    // to correct sensor input.
    private int[][] deviceTransformation = TRANSFORM_0;

    // Current geomagnetic data, and the time at which it was fetched.
    // null if it hasn't been got yet.
    private GeomagneticField geomagneticField = null;
    private long geomagneticTime = 0;
    
    // The most recent accelerometer and compass data.
    private float[] accelValues = null;
    private float[] magValues = null;
    
    // Some useful strings.
    private final String msgDisabled;
    private final String msgOffline;

}

