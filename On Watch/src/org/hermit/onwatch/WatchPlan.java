
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


/**
 * This enum defines a watch plan.
 */
public enum WatchPlan
{

	THREE_HOUR(R.string.wplan_three, 1, new float[] {
			0.0f, 3.0f, 6.0f, 9.0f, 12.0f, 15.0f, 18.0f, 21.0f
	}),
	THREE_HOUR_D(R.string.wplan_three_d, 1, new float[] {
			0.0f, 3.0f, 6.0f, 9.0f, 12.0f, 15.0f, 16.5f, 18.0f, 21.0f
	}),
	THREE_HOUR_SPLIT(R.string.wplan_three_s, 2, new float[] {
			0.0f, 3.0f, 6.0f, 9.0f, 12.0f, 15.0f, 18.0f, 21.0f
	}),
	FOUR_HOUR(R.string.wplan_four, 1, new float[] {
			0.0f, 4.0f, 8.0f, 12.0f, 16.0f, 20.0f
	}),
	FOUR_HOUR_D(R.string.wplan_four_d, 1, new float[] {
			0.0f, 4.0f, 8.0f, 12.0f, 16.0f, 18.0f, 20.0f
	}),
	FOUR_HOUR_DS(R.string.wplan_four_ds, 2, new float[] {
			0.0f, 4.0f, 8.0f, 12.0f, 16.0f, 18.0f, 20.0f
	}),
	SIX_HOUR(R.string.wplan_six, 1, new float[] {
			0.0f, 6.0f, 12.0f, 18.0f
	}),
	SIX_HOUR_D(R.string.wplan_six_d, 1, new float[] {
			0.0f, 6.0f, 12.0f, 15.0f, 18.0f
	});


	/**
	 * Create a watch plan.  Private since there is only one instance.
	 * 
	 * @param	nameId			Resource ID of the name of this plan.
	 * @param	ply				Number of crew on watch at once.
	 * @param	times			Times of the watches, in decimal hours.
	 */
	WatchPlan(int nameId, int ply, float[] times) {
		planNameId = nameId;
		planPly = ply;
		planTimes = times;
	}

	/**
	 * Get the basic length of a watch (not counting dog watches).
	 * 
	 * @return					Basic watch length in decimal hours.
	 */
	public float getBaseLength() {
		return planTimes[1] - planTimes[0];
	}
	
    // The resource ID of the name of this plan.
    public final int planNameId;

    // The times of the watches in this plan, in decimal hours.
    public final float[] planTimes;

    // The number of crew on watch at once.
    public final int planPly;
      
}

