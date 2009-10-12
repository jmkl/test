
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


import java.util.Random;


/**
 * The DataGenerator is basically a fake sensor that can be used to
 * simulate values for sensors that aren't there.
 */
public class DataGenerator
{

	// ******************************************************************** //
	// Constructor.
	// ******************************************************************** //

    /**
     * Instantiate a data generator.
     * 
     * @param   client          Client which wants the faked data.
     * @param   sensor          Sensor ID to simulate.
     * @param   dim             Number of axes (dimensions) the sensor has.
     * @param   unit            Data unit size.
     * @param   range           Range of data to generate, in multiples of
     *                          unit.
     */
	public DataGenerator(DataView client,
						 int sensor, int dim, float unit, float range)
	{
		clientListener = client;
		sensorId = sensor;
		sensorDim = dim;
		dataMax = unit * range;
		
		currentValues = new float[dim];
		currentRate = new float[dim];
		for (int i = 0; i < dim; ++i) {
			currentValues[i] = dataMax / 2f;
			currentRate[i] = 0f; // dataMax / 50f;
		}
	}
	

	// ******************************************************************** //
	// Common Utilities.
	// ******************************************************************** //

	/**
	 * Generate a set of data from this simulated sensor.
	 */
	public final void generateValues() {
		for (int i = 0; i < sensorDim; ++i) {
			// Calculate where the current value is in the range 0 .. max, as a
			// factor in range -1 .. 1.
			final float off = currentValues[i] / dataMax * 2f - 1f;

			// Calculate where the current rate is in the range
			// -max/50 .. max/50, as a factor in range -1 .. 1.
			final float roff = currentRate[i] / (dataMax / 50f);

			// Adjust the rate by a random Gaussian biased by our current
			// position in the range.
			currentRate[i] += (random.nextGaussian() - off / 2 - roff / 2) * dataMax / 50;

			// Finally use a random fraction of the current rate to adjust
			// the actual value.
			currentValues[i] += random.nextFloat() * currentRate[i];
		}
		
		clientListener.onSensorData(sensorId, currentValues);
	}


	// ******************************************************************** //
	// Class Data.
	// ******************************************************************** //
	
	// Random number generator used for faking data.
	private static final Random random = new Random();

	
	// ******************************************************************** //
	// Private Data.
	// ******************************************************************** //
	
	// Listener to pass the data to.
	private DataView clientListener;

	// The ID of the sensor we're faking.
	private final int sensorId;
	
	// The number of axes in the sensor.
	private final int sensorDim;
	
	// Max desirable values.
	private final float dataMax;
	
	// Current simulated value.
	private float[] currentValues;
	
	// Current rate of change of the value.
	private float[] currentRate;
	
}

