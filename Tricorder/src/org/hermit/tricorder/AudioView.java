
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
import org.hermit.android.instruments.AudioAnalyser;
import org.hermit.android.instruments.PowerGauge;
import org.hermit.android.instruments.SpectrumGauge;
import org.hermit.android.instruments.WaveformGauge;

import android.graphics.Canvas;
import android.graphics.Rect;


/**
 * A view which displays several scalar parameters as graphs.
 */
class AudioView
	extends DataView
{

	// ******************************************************************** //
	// Constructor.
	// ******************************************************************** //

	/**
	 * Set up this view.
	 * 
	 * @param	context			Parent application context.
     * @param   parent          Parent surface.
	 */
	public AudioView(Tricorder context, SurfaceRunner parent) {
		super(context, parent);
        
        audioAnalyser = new AudioAnalyser(parent);
        
        waveformGauge = audioAnalyser.getWaveformGauge(parent);
        String[] wfields = { parent.getRes(R.string.lab_aco_wave) };
        waveformWindow = new ElementWrapper(parent, waveformGauge,
                                            COLOUR_GRID, COLOUR_PLOT, wfields);
        waveformWindow.setText(0, 0, wfields[0]);

        spectrumGauge = audioAnalyser.getSpectrumGauge(parent);
        String[] sfields = { parent.getRes(R.string.lab_aco_spec) };
        spectrumWindow = new ElementWrapper(parent, spectrumGauge,
                                            COLOUR_GRID, COLOUR_PLOT, sfields);
        spectrumWindow.setText(0, 0, sfields[0]);
        
        powerGauge = audioAnalyser.getPowerGauge(parent);
        String[] pfields = { parent.getRes(R.string.lab_aco_power) };
        powerWindow = new ElementWrapper(parent, powerGauge,
                                         COLOUR_GRID, COLOUR_PLOT, pfields);
        powerWindow.setText(0, 0, pfields[0]);
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
    }


    /**
     * Set up the layout of this view in portrait mode.
     * 
     * @param   bounds      The bounding rect of this element within
     *                      its parent View.
     */
    private void layoutPortrait(Rect bounds) {
		int pad = getInterPadding();
		int h = bounds.bottom - bounds.top;
		int unit = (h - pad * 2) / 10;

		int sx = bounds.left + pad;
		int ex = bounds.right;
		int y = bounds.top;

        waveformWindow.setGeometry(new Rect(sx, y, ex, y + 3 * unit));
        y += 3 * unit + pad;
        
        spectrumWindow.setGeometry(new Rect(sx, y, ex, y + 4 * unit));
        y += 4 * unit + pad;
        
        powerWindow.setGeometry(new Rect(sx, y, ex, y + 3 * unit));
    }


    /**
     * Set up the layout of this view in landscape mode.
     * 
     * @param   bounds      The bounding rect of this element within
     *                      its parent View.
     */
    private void layoutLandscape(Rect bounds) {
        int pad = getInterPadding();
        int h = bounds.bottom - bounds.top;
        int unit = (h - pad) / 2;

        int sx = bounds.left + pad;
        int ex = bounds.right;
        int cw = (ex - sx) / 2 - pad;
        int x = sx;
        int y = bounds.top;

        waveformWindow.setGeometry(new Rect(x, y, x + cw, y + unit));
        y += unit + pad;
        
        powerWindow.setGeometry(new Rect(x, y, x + cw, y + unit));
        y = bounds.top;
        x += cw + pad;
        
        spectrumWindow.setGeometry(new Rect(x, y, ex, bounds.bottom));
	}


    // ******************************************************************** //
    // Accessors.
    // ******************************************************************** //
    
    /**
     * Get the audio analyser.
     * 
     * @return          The audio analyser.
     */
	AudioAnalyser getAudioAnalyser() {
        return audioAnalyser;
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
	    audioAnalyser.measureStart();
	}
	

	/**
	 * A 1-second tick event.  Can be used for housekeeping and
	 * async updates.
	 * 
	 * @param	time				The current time in millis.
	 */
	@Override
	void tick(long time) {
		;
	}


	/**
	 * Stop this view.  This notifies the view that it should stop
	 * receiving and displaying data, and generally stop using
	 * resources.
	 */
	@Override
	void stop() {
	    audioAnalyser.measureStop();
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
		
		audioAnalyser.doUpdate(now);
		waveformWindow.draw(canvas, now, bg);
		spectrumWindow.draw(canvas, now, bg);
		powerWindow.draw(canvas, now, bg);
	}


    // ******************************************************************** //
    // Class Data.
    // ******************************************************************** //

    // Debugging tag.
	@SuppressWarnings("unused")
	private static final String TAG = "tricorder";
    
    // Grid and plot colours.
    private static final int COLOUR_GRID = 0xffdfb682;
    private static final int COLOUR_PLOT = 0xffd09cd0;

	
	// ******************************************************************** //
	// Private Data.
	// ******************************************************************** //
    
    // Our audio input device.
    private final AudioAnalyser audioAnalyser;
    
    // The gauges associated with this instrument.
    private WaveformGauge waveformGauge = null;
    private SpectrumGauge spectrumGauge = null;
    private PowerGauge powerGauge = null;
    
    // Wrapper windows for the gauges.
    private ElementWrapper waveformWindow = null;
    private ElementWrapper spectrumWindow = null;
    private ElementWrapper powerWindow = null;

}

