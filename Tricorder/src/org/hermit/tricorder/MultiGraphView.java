
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

import android.graphics.Canvas;
import android.graphics.Rect;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.view.SurfaceHolder;


/**
 * A view which displays several scalar parameters as graphs.
 */
class MultiGraphView
	extends DataView
	implements SensorEventListener
{

	// ******************************************************************** //
	// Public Types.
	// ******************************************************************** //

	/**
	 * Define the displayed graphs.
	 */
	private static enum GraphDefinition {
		LIGHT(R.string.lab_light, Sensor.TYPE_LIGHT,
						SensorManager.LIGHT_SUNLIGHT, 1.25f,
						0xffdfb682, 0xffd09cd0),
		PROX(R.string.lab_prox, Sensor.TYPE_PROXIMITY,
						1f, 5.2f,
						0xffccccff, 0xffff9e63),
		TEMP(R.string.lab_temp, Sensor.TYPE_TEMPERATURE,
						10f, 4.2f,
						0xffffcc66, 0xffd09cd0);

		GraphDefinition(int lab, int s, float u, float r, int gcol, int pcol) {
			labelId = lab;
			sensorId = s;
			dataUnit = u;
			dataRange = r;
			gridColour = gcol;
			plotColour = pcol;
		}
		
		private final int labelId;
		private final int sensorId;
		private final float dataUnit;
		private final float dataRange;
		private final int gridColour;
		private final int plotColour;
		
		private Sensor sensor = null;
		private MagnitudeElement view = null;
		private DataGenerator generator = null;
	}


	// ******************************************************************** //
	// Constructor.
	// ******************************************************************** //

	/**
	 * Set up this view.
	 * 
	 * @param	context			Parent application context.
     * @param	sh				SurfaceHolder we're drawing in.
	 * @param	sman			The SensorManager to get data from.
	 */
	public MultiGraphView(Tricorder context, SurfaceHolder sh, SensorManager sman) {
		super(context, sh);
		
		surfaceHolder = sh;
		sensorManager = sman;
		
        // Add the magnitude charts and labels for each value.
		String flab = getRes(GraphDefinition.LIGHT.labelId);
		String[] tfields = { flab, "00000.000" };
		
        for (GraphDefinition def : GraphDefinition.values()) {
        	def.view = new MagnitudeElement(context, sh,
        								    def.dataUnit, def.dataRange,
        								    def.gridColour, def.plotColour,
        								    tfields);
        	
        	def.view.setText(0, 0, getRes(def.labelId));
            def.view.setText(0, 1, getRes(R.string.msgNoData));
            
            // Get the sensor, if we have it.
        	def.sensor = sensorManager.getDefaultSensor(def.sensorId);
        }
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
		
		int pad = getContext().getInterPadding();
		int h = bounds.bottom - bounds.top;
		int graphHeight = (h - pad * 2) / 3;

		int sx = bounds.left + pad;
		int ex = bounds.right;
		int y = bounds.top;

		// Lay out the graphs.
        for (GraphDefinition def : GraphDefinition.values()) {
        	def.view.setGeometry(new Rect(sx, y, ex, y + graphHeight));
        	y += graphHeight + pad;
        }
	}


	// ******************************************************************** //
	// Configuration.
	// ******************************************************************** //

	/**
	 * Set whether we should simulate data for missing sensors.
	 * 
	 * @param	fakeIt			If true, sensors that aren't equipped will
	 * 							have simulated data displayed.  If false,
	 * 							they will show "No Data".
	 */
	@Override
	void setSimulateMode(boolean fakeIt) {
		String labStr = getRes(R.string.msgNoData);
		synchronized (surfaceHolder) {
			// For each graph, put it in simulation if requested and
			// if its sensor is not present.
			for (GraphDefinition def : GraphDefinition.values()) {
	        	if (fakeIt && def.sensor == null) {
	        		def.generator = new DataGenerator(this, def.sensorId,
	        									      1, def.dataUnit,
	        									      def.dataRange);
	        		def.view.setIndicator(true, 0xff0000ff);
	        	} else {
	        		def.generator = null;
	        		def.view.setIndicator(false, 0);
					def.view.setText(0, 1, labStr);
					def.view.clearValue();
	        	}
			}
		}
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
	    for (GraphDefinition def : GraphDefinition.values()) {
	        if (def.sensor != null)
	            sensorManager.registerListener(this, def.sensor,
	                                           SensorManager.SENSOR_DELAY_GAME);
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
		;
	}


	/**
	 * Stop this view.  This notifies the view that it should stop
	 * receiving and displaying data, and generally stop using
	 * resources.
	 */
	@Override
	public void stop() {
		// Unregister this listener for all sensors.
		sensorManager.unregisterListener(this);
    }


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
        onSensorData(event.sensor.getType(), event.values);
    }


    /**
     * Called when sensor values have changed.  The length and contents
     * of the values array vary depending on which sensor is being monitored.
     *
     * @param   sensor          The ID of the sensor being monitored.
     * @param   values          The new values for the sensor.
     */
    @Override
    public void onSensorData(int sensor, float[] values) {
		final float value = values[0];

		// Which graph is this for?
		GraphDefinition valId = null;
		if (sensor == Sensor.TYPE_LIGHT)
			valId = GraphDefinition.LIGHT;
		else if (sensor == Sensor.TYPE_PROXIMITY)
			valId = GraphDefinition.PROX;
		else if (sensor == Sensor.TYPE_TEMPERATURE)
			valId = GraphDefinition.TEMP;
		else
			return;

		synchronized (surfaceHolder) {
        	valId.view.setText(0, 1, format(value));
			valId.view.setValue(value);
		}
	}


	/**
	 * Format a float to a field width of 7, including sign, with 3
	 * decimals.  MUCH faster than String.format.
	 */
	private static final String format(float val) {
		int s = val < 0 ? -1 : 1;
		val *= s;
		int before = (int) val;
		int after = (int) ((val - before) * 1000);
		
		String b = (s < 0 ? "-" : " ") + before;
		String a = "" + after;
		StringBuilder res = new StringBuilder("   .000");
		int bs = 3 - b.length();
		res.replace((bs < 0 ? 0 : bs), 3, b);
		res.replace(7 - a.length(), 7, a);
		return res.toString();
	}


	// ******************************************************************** //
	// View Drawing.
	// ******************************************************************** //
	
	/**
	 * This method is called to ask the view to draw itself.
	 * 
	 * @param	canvas		Canvas to draw into.
	 * @param	now				Current system time in ms.
	 */
	@Override
	protected void draw(Canvas canvas, long now) {
		super.draw(canvas, now);
		
		// Draw the graph views.
        for (GraphDefinition def : GraphDefinition.values()) {
        	// If the sensor is not equipped, fake the data if requested to.
        	if (def.generator != null)
        		def.generator.generateValues();
        		
        	def.view.draw(canvas, now);
        }
	}


    // ******************************************************************** //
    // Class Data.
    // ******************************************************************** //

    // Debugging tag.
	@SuppressWarnings("unused")
	private static final String TAG = "tricorder";

	
	// ******************************************************************** //
	// Private Data.
	// ******************************************************************** //

	// The surface we're drawing on.
	private SurfaceHolder surfaceHolder;

	// The sensor manager, which we use to interface to all sensors.
    private SensorManager sensorManager;

}

