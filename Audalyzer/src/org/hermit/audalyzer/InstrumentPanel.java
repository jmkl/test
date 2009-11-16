
/**
 * Audalyzer: an audio analyzer for Android.
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


package org.hermit.audalyzer;


import org.hermit.android.instruments.AudioAnalyser;
import org.hermit.android.instruments.InstrumentSurface;
import org.hermit.android.instruments.PowerGauge;
import org.hermit.android.instruments.SpectrumGauge;
import org.hermit.android.instruments.WaveformGauge;

import android.app.Activity;
import android.graphics.Rect;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.MotionEvent;


/**
 * The main audio analyser view.  This class relies on the parent SurfaceRunner
 * class to do the bulk of the animation control.
 */
public class InstrumentPanel
	extends InstrumentSurface
{

    // ******************************************************************** //
    // Constructor.
    // ******************************************************************** //

	/**
	 * Create a WindMeter instance.
	 * 
	 * @param	app			The application context we're running in.
	 */
    public InstrumentPanel(Activity app) {
        super(app, OPTION_DYNAMIC);
        
        audioAnalyser = new AudioAnalyser(this);
        waveformGauge = audioAnalyser.getWaveformGauge(this);
        spectrumGauge = audioAnalyser.getSpectrumGauge(this);
        powerGauge = audioAnalyser.getPowerGauge(this);
        
        addInstrument(audioAnalyser);
        addGauge(waveformGauge);
        addGauge(spectrumGauge);
        addGauge(powerGauge);

        // On-screen debug stats display.
        setDebugPerf(false);
    }
    

    // ******************************************************************** //
    // Layout Processing.
    // ******************************************************************** //

    /**
     * Lay out the display for a given screen size.
     * 
     * @param   width       The new width of the surface.
     * @param   height      The new height of the surface.
     */
    @Override
    protected void layout(int width, int height) {
        // Make up some layout parameters.
        int min = Math.min(width, height);
        int gutter = min / (min > 400 ? 15 : 20);

        // Calculate the layout based on the screen configuration.
        if (width > height)
            layoutLandscape(width, height, gutter);
        else
            layoutPortrait(width, height, gutter);
        
        // Set the gauge geometries.
        waveformGauge.setGeometry(waveRect);
        spectrumGauge.setGeometry(specRect);
        powerGauge.setGeometry(meterRect);
    }
   

    /**
     * Lay out the display for a given screen size.
     * 
     * @param   width       The new width of the surface.
     * @param   height      The new height of the surface.
     * @param   gutter      Spacing to leave between items.
     */
    private void layoutLandscape(int width, int height, int gutter) {
        // Divide the display into two columns.
        int col = (width - gutter * 3) / 2;
        
        // Divide the left pane in two.
        int row = (height - gutter * 3) / 2;
        
        int x = gutter;
        int y = gutter;
        waveRect = new Rect(x, y, x + col, y + row);
        y += row + gutter;
        meterRect = new Rect(x, y, x + col, height - gutter);
        
        x += col + gutter;
        y = gutter;
        specRect = new Rect(x, y, x + col, height - gutter);
    }
    

    /**
     * Lay out the display for a given screen size.
     * 
     * @param   width       The new width of the surface.
     * @param   height      The new height of the surface.
     * @param   gutter      Spacing to leave between items.
     */
    private void layoutPortrait(int width, int height, int gutter) {
        // Divide the display into three vertical elements, the
        // spectrum display being double-height.
        int unit = (height - gutter * 4) / 4;
        int col = width - gutter * 2;

        int x = gutter;
        int y = gutter;
        waveRect = new Rect(x, y, x + col, y + unit);
        y += unit + gutter;
        
        specRect = new Rect(x, y, x + col, y + unit * 2);
        y += unit * 2 + gutter;
        
        meterRect = new Rect(x, y, x + col, y + unit);
    }
    
    
	// ******************************************************************** //
	// Input Handling.
	// ******************************************************************** //

    /**
	 * Handle key input.
	 * 
     * @param	keyCode		The key code.
     * @param	event		The KeyEvent object that defines the
     * 						button action.
     * @return				True if the event was handled, false otherwise.
	 */
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
    	return false;
    }
    
    
    /**
	 * Handle touchscreen input.
	 * 
     * @param	event		The MotionEvent object that defines the action.
     * @return				True if the event was handled, false otherwise.
	 */
    @Override
    public boolean onTouchEvent(MotionEvent event) {
    	int action = event.getAction();
//    	final float x = event.getX();
//        final float y = event.getY();
    	switch (action) {
    	case MotionEvent.ACTION_DOWN:
            break;
        case MotionEvent.ACTION_MOVE:
            break;
    	case MotionEvent.ACTION_UP:
            break;
    	case MotionEvent.ACTION_CANCEL:
            break;
        default:
            break;
    	}

		return true;
    }


    // ******************************************************************** //
    // Save and Restore.
    // ******************************************************************** //

    /**
     * Save the state of the game in the provided Bundle.
     * 
     * @param   icicle      The Bundle in which we should save our state.
     */
    protected void saveState(Bundle icicle) {
//      gameTable.saveState(icicle);
    }


    /**
     * Restore the game state from the given Bundle.
     * 
     * @param   icicle      The Bundle containing the saved state.
     */
    protected void restoreState(Bundle icicle) {
//      gameTable.pause();
//      gameTable.restoreState(icicle);
    }
    

    // ******************************************************************** //
    // Class Data.
    // ******************************************************************** //

    // Debugging tag.
	@SuppressWarnings("unused")
	private static final String TAG = "Audalyzer";

	
	// ******************************************************************** //
	// Private Data.
	// ******************************************************************** //
    
    // Our audio input device.
    private final AudioAnalyser audioAnalyser;
    
    // The gauges associated with this instrument.
    private WaveformGauge waveformGauge = null;
    private SpectrumGauge spectrumGauge = null;
    private PowerGauge powerGauge = null;

    // Bounding rectangles for the waveform, spectrum, and VU meter displays.
    private Rect waveRect = null;
    private Rect specRect = null;
    private Rect meterRect = null;

}

