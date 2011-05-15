
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
import org.hermit.android.sound.Effect;

import android.graphics.Canvas;
import android.graphics.Rect;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.view.MotionEvent;
import android.view.Surface;


/**
 * A view which displays 3-axis sensor data in a variety of ways.
 * 
 * This could be used, for example, to show the accelerometer or
 * compass values.
 */
class TridataView
	extends DataView
	implements SensorEventListener
{

	// ******************************************************************** //
	// Constructor.
	// ******************************************************************** //

	/**
	 * Set up this view.
	 * 
	 * @param	context			Parent application context.
     * @param   parent          Parent surface.
	 * @param	sman			The SensorManager to get data from.
	 * @param	sensor			The ID of the sensor to read:
	 * 							Sensor.TYPE_XXX.
	 * @param	unit			The size of a unit of measure (for example,
	 * 							1g of acceleration).
	 * @param	range			How many units big to make the graph.
	 * @param	gridCol1		Colour for the graph grid in abs mode.
	 * @param	plotCol1		Colour for the graph plot in abs mode.
	 * @param	gridCol2		Colour for the graph grid in rel mode.
	 * @param	plotCol2		Colour for the graph plot in rel mode.
     * @param   sound           Sound to play while scanning.
	 */
	public TridataView(Tricorder context, SurfaceRunner parent,
					   SensorManager sman, int sensor,
					   float unit, float range,
					   int gridCol1, int plotCol1,
					   int gridCol2, int plotCol2, Effect sound)
	{
		super(context, parent);
		
		// Get the UI strings.
	    title_vect_abs = parent.getRes(R.string.title_vect_abs);
	    title_mag_abs = parent.getRes(R.string.title_mag_abs);
	    title_num_abs = parent.getRes(R.string.title_num_abs);
	    title_xyz_abs = parent.getRes(R.string.title_xyz_abs);
	    title_vect_rel = parent.getRes(R.string.title_vect_rel);
	    title_mag_rel = parent.getRes(R.string.title_mag_rel);
	    title_num_rel = parent.getRes(R.string.title_num_rel);
	    title_xyz_rel = parent.getRes(R.string.title_xyz_rel);

		appContext = context;
		sensorManager = sman;
		sensorId = sensor;
		dataUnit = unit;
		dataRange = range;
		gridColour1 = gridCol1;
	    plotColour1 = plotCol1;
		gridColour2 = gridCol2;
	    plotColour2 = plotCol2;
	    scanSound = sound;

		processedValues = new float[3];

        // Add the gravity 3-axis plot.
        plotView = new AxisElement(parent, unit, range,
				 				   gridCol1, plotCol1,
				 				   new String[] { "XXXXXXXXXXXXXXXXXXXX" });

        // Add the gravity magnitude chart.
        chartView = new MagnitudeElement(parent, unit, range,
        								 gridCol1, plotCol1,
        								 new String[] { "XXXXXXXXXXXXXXXXXXXX" });
        chartView.setScrolling(false);

        // Add the numeric display.
        numView = new Num3DElement(parent, gridCol1, plotCol1,
                                   new String[] { "XXXXXXXXXXXXXXXXXXXX" });
        
        xyzView = new MagnitudeElement(parent, 3, unit, range,
				 					   gridCol1, XYZ_PLOT_COLS,
				 					   new String[] { "XXXXXXXXXXXXXXXXXXXX" }, true);
        xyzView.setScrolling(false);

        viewEnabled = false;
        scanEnabled = false;
        setRelativeMode(false);
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
		
		plotBounds = plotView.getBounds();
		numBounds = numView.getBounds();
	}


    /**
     * Set up the layout of this view in portrait mode.
     * 
	 * @param	bounds		The bounding rect of this element within
	 * 						its parent View.
     */
	private void layoutPortrait(Rect bounds) {
		final int pad = getInterPadding();
		final int h = bounds.bottom - bounds.top;
		
		final int plotHeight = h / 3;
		int res = h - plotHeight - pad * 2;
		final int numHeight = res / 2;
		final int chartHeight = res / 2;

		int sx = bounds.left + pad;
		int ex = bounds.right;
		int y = bounds.top;
		
		plotView.setGeometry(new Rect(sx, y, ex, y + plotHeight));
		y += plotHeight + pad;
		
		chartView.setGeometry(new Rect(sx, y, ex, y + chartHeight));
		y += chartHeight + pad;
		
		// The numeric and XYZ views are alternatives and go in the
		// same place.
		numView.setGeometry(new Rect(sx, y, ex, y + numHeight));
		xyzView.setGeometry(new Rect(sx, y, ex, y + numHeight));
	}


    /**
     * Set up the layout of this view in landscape mode.
     * 
	 * @param	bounds		The bounding rect of this element within
	 * 						its parent View.
     */
	private void layoutLandscape(Rect bounds) {
		final int pad = getInterPadding();
		final int w = bounds.right - bounds.left;
		final int h = bounds.bottom - bounds.top;

		final int sx = bounds.left + pad;
		final int ex = bounds.right;
		int x = sx;
		int y = bounds.top;
		
		int plotWidth = (w - pad) / 3;
		plotView.setGeometry(new Rect(x, y, x + plotWidth, y + h));
		x += plotWidth + pad;
		
		int chartHeight = (h - pad) / 2;
		chartView.setGeometry(new Rect(x, y, ex, y + chartHeight));
		y += chartHeight + pad;
		
		// The numeric and XYZ views are alternatives and go in the
		// same place.
		numView.setGeometry(new Rect(x, y, ex, y + chartHeight));
		xyzView.setGeometry(new Rect(x, y, ex, y + chartHeight));
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
     * Set the general scanning mode.  This affects whichever views support
     * it.
     * 
     * @param   continuous      If true, scan all the time.  Otherwise,
     *                          scan only under user control.
     */
    @Override
    void setScanMode(boolean continuous) {
        scanContinuously = continuous;
        if (scanContinuously)
            appContext.setAuxButton(R.string.lab_blank);
        else
            appContext.setAuxButton(R.string.lab_scan_start);
    }


    /**
     * Set the general scanning mode.  This affects whichever views support
     * it.
     * 
     * @param   enable          If true, play a sound while scanning
     *                          under user control.  Else don't.
     */
    @Override
    void setScanSound(boolean enable) {
        scanPlaySound = enable;
    }

	
	/**
	 * Set or reset relative mode.  In relative mode, we report all
	 * values relative to the values that were in force when we entered
	 * it.  In absolute mode, we always report the absolute values.
	 * 
	 * We set the header fields in each part of the display to reflect
	 * the mode.
	 * 
	 * @param rel
	 */
	void setRelativeMode(boolean rel) {
		relativeMode = rel;
		relativeValues = null;
		
		if (!relativeMode) {
			plotView.setText(0, 0, title_vect_abs);
			plotView.setDataColors(gridColour1, plotColour1);
			chartView.setText(0, 0, title_mag_abs);
			chartView.setDataColors(gridColour1, plotColour1);
			numView.setText(0, 0, title_num_abs);
			numView.setDataColors(gridColour1, plotColour1);
			xyzView.setText(0, 0, title_xyz_abs);
			xyzView.setDataColors(gridColour1, XYZ_PLOT_COLS);
		} else {
			plotView.setText(0, 0, title_vect_rel);
			plotView.setDataColors(gridColour2, plotColour2);
			chartView.setText(0, 0, title_mag_rel);
			chartView.setDataColors(gridColour2, plotColour2);
			numView.setText(0, 0, title_num_rel);
			numView.setDataColors(gridColour2, plotColour2);
			xyzView.setText(0, 0, title_xyz_rel);
			xyzView.setDataColors(gridColour2, XYZ_PLOT_COLS);
		}
	}
	

	// ******************************************************************** //
	// State Management.
	// ******************************************************************** //
	
	/**
	 * Start this view.  This notifies the view that it should start
	 * receiving and displaying data.  The view will also get tick events
	 * starting here.
	 */
	@Override
	void start() {
        viewEnabled = true;
        
        if (scanContinuously)
            scanStart();
        else
            appContext.setAuxButton(R.string.lab_scan_start);
	}
	
    
    /**
     * This view's aux button has been clicked.  Toggle the scan mode.
     * Does nothing in continuous scan mode.
     */
    @Override
    void auxButtonClick() {
        if (!viewEnabled || scanContinuously)
            return;
        if (scanEnabled)
            scanStop();
        else
            scanStart();
    }
    

	/**
	 * Stop this view.  This notifies the view that it should stop
	 * receiving and displaying data, and generally stop using
	 * resources.
	 */
	@Override
	void stop() {
	    scanStop();
        viewEnabled = false;
	}
	
	
	private void scanStart() {
        Sensor sensor = sensorManager.getDefaultSensor(sensorId);
        if (sensor != null)
            sensorManager.registerListener(this, sensor,
                                           SensorManager.SENSOR_DELAY_GAME);
        chartView.setScrolling(true);
        xyzView.setScrolling(true);
        appContext.setAuxButton(scanContinuously ?
                                R.string.lab_blank: R.string.lab_scan_stop);
        if (scanSound != null && scanPlaySound && !scanContinuously)
            scanSound.loop();
        scanEnabled = true;
	}
	
    
    private void scanStop() {
        sensorManager.unregisterListener(this);
        chartView.setScrolling(false);
        xyzView.setScrolling(false);
        appContext.setAuxButton(scanContinuously ?
                                R.string.lab_blank: R.string.lab_scan_start);
        if (scanSound != null)
            scanSound.stop();
        scanEnabled = false;
    }
    

	// ******************************************************************** //
	// Data Management.
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
	 * @param	event			The sensor event.
	 */
    public void onSensorChanged(SensorEvent event) {
        try {
            onSensorData(event.sensor.getType(), event.values);
        } catch (Exception e) {
            appContext.reportException(e);
        }
    }


    /**
     * Called when sensor values have changed.  The length and contents
     * of the values array vary depending on which sensor is being monitored.
     *
     * @param   sensorId        The ID of the sensor being monitored.
     * @param   values          The new values for the sensor.
     */
    @Override
    public void onSensorData(int sensorId, float[] values) {
        if (values.length < 3)
            return;
        
        synchronized (this) {
            // If we're in relative mode, subtract the baseline values.
            if (relativeMode) {
                // First time through, set the baseline values.
                if (relativeValues == null) {
                    relativeValues = new float[3];
                    relativeValues[0] = values[0];
                    relativeValues[1] = values[1];
                    relativeValues[2] = values[2];
                }
                values[0] -= relativeValues[0];
                values[1] -= relativeValues[1];
                values[2] -= relativeValues[2];
            }
            
            // Transform for the device orientation.
            multiply(values, deviceTransformation, processedValues);

            final float x = processedValues[0];
            final float y = processedValues[1];
            final float z = processedValues[2];
            float m = 0.0f;
            float az = 0.0f;
            float alt = 0.0f;

            // Calculate the magnitude.
            m = (float) Math.sqrt(x*x + y*y + z*z);

            // Calculate the azimuth and altitude.
            az = (float) Math.toDegrees(Math.atan2(y, x));
            az = 90 - az;
            if (az < 0)
                az += 360;

            // sin alt = z / mag
            alt = m == 0 ? 0  : (float) Math.toDegrees(Math.asin(z / m));

            plotView.setValues(processedValues, m, az, alt);
            chartView.setValue(m);
            numView.setValues(processedValues, m, az, alt);
            xyzView.setValue(processedValues);
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
     * @param	event			The motion event.
     * @return					True if the event was handled, false otherwise.
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
	                if (plotBounds != null && plotBounds.contains(x, y)) {
	                    setRelativeMode(!relativeMode);
	                    appContext.soundSecondary();
	                    done = true;
	                } else if (numBounds != null && numBounds.contains(x, y)) {
	                    // Toggle the X/Y/Z display.
	                    showXyz = !showXyz;
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
    		
		// Draw the elements.
		plotView.draw(canvas, now, bg);
		chartView.draw(canvas, now, bg);
		if (showXyz)
			xyzView.draw(canvas, now, bg);
		else
			numView.draw(canvas, now, bg);
	}


    // ******************************************************************** //
    // Class Data.
    // ******************************************************************** //

    // Debugging tag.
	@SuppressWarnings("unused")
	private static final String TAG = "tricorder";
	
	// Colours for an XYZ plot.
	private static final int[] XYZ_PLOT_COLS =
						new int[] { 0xffff0000, 0xff00ff00, 0xff0000ff };
	
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
	private Tricorder appContext;
	
	// The sensor manager, which we use to interface to all sensors.
    private SensorManager sensorManager;
    
	// The ID of the sensor to read: Sensor.TYPE_XXX.
    private int sensorId;
    
    // The unit and range of the data.  baseDataRange is the specified range
    // for this display; dataRange is the current range under zooming.
	public final float dataUnit;
	public float dataRange;
    
    // Current device orientation, as a matrix which can be used
    // to correct sensor input.
    private int[][] deviceTransformation = TRANSFORM_0;
    
    // Flags for enabling the view -- when this view is displayed -- and
    // scanning, when the user presses the scan button.
    private boolean viewEnabled = false;
    private boolean scanEnabled = false;

	// Processed data values.
	private float[] processedValues = null;

	// If relativeMode is true, we're in relative mode.  Display values
	// relative to the values stored in relativeValues -- i.e. subtract
	// relativeValues from future inputs.
	private boolean relativeMode = false;
	private float[] relativeValues = null;

	// Colour of the graph grid and plot, in primary (absolute) and
    // secondary (relative) modes.
    private int gridColour1 = 0xff00ff00;
    private int plotColour1 = 0xffff0000;
    private int gridColour2 = 0xff00ff00;
    private int plotColour2 = 0xffff0000;

    // Sound to play while scanning.  null if none.
    private Effect scanSound = null;
    
    // If true, scan continuously; else only when the user says.
    private boolean scanContinuously = false;
    
    // If true, play a sound while scanning under user control.
    private boolean scanPlaySound = true;

    // 3-axis plot, magnitude chart and numeric display for the data.
    // numView and xyzView are two alternative modes for the bottom plot.
    // We also keep the current bounds for each element.
    private AxisElement plotView;
    private Rect plotBounds;
    private MagnitudeElement chartView;
	private Num3DElement numView;
    private Rect numBounds;
    private MagnitudeElement xyzView;

    // Do we show the XYZ display?  If not, it's numView.
    private boolean showXyz = false;

	// Some useful strings.
    private String title_vect_abs;
    private String title_mag_abs;
    private String title_num_abs;
    private String title_xyz_abs;
    private String title_vect_rel;
    private String title_mag_rel;
    private String title_num_rel;
    private String title_xyz_rel;

}

