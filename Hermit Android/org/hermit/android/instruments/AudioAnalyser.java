
/**
 * org.hermit.android.instrument: graphical instruments for Android.
 * 
 * These classes provide input and display functions for creating on-screen
 * instruments of various kinds in Android apps.
 *
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


package org.hermit.android.instruments;


import org.hermit.android.core.SurfaceRunner;
import org.hermit.android.io.AudioReader;
import org.hermit.dsp.FFTTransformer;
import org.hermit.dsp.SignalPower;

import android.app.Activity;
import android.os.Bundle;


/**
 * An {@link Instrument} which analyses an audio stream in various ways.
 */
public class AudioAnalyser
	extends Instrument
{

    // ******************************************************************** //
    // Constructor.
    // ******************************************************************** //

	/**
	 * Create a WindMeter instance.
	 * 
	 * @param	app			The application context we're running in.
	 */
    public AudioAnalyser(Activity app) {
        super(app);
        
        audioReader = new AudioReader(SAMPLE_RATE);
        
        fourierTransformer = new FFTTransformer(FFT_BLOCK);
        
        spectrumData = new float[FFT_BLOCK / 2];
        
        biasRange = new float[2];
    }


    // ******************************************************************** //
    // Run Control.
    // ******************************************************************** //

    /**
     * The application is starting.  Perform any initial set-up prior to
     * starting the application.  We may not have a screen size yet,
     * so this is not a good place to allocate resources which depend on
     * that.
     */
    @Override
    public void appStart() {
    }


    /**
     * We are starting the main run; start measurements.
     */
    @Override
    public void measureStart() {
        audioReader.startReader(FFT_BLOCK * DECIMATE, new AudioReader.Listener() {
            @Override
            public void onReadComplete(short[] buffer) {
                processAudio(buffer);
            }
        });
    }
    

    /**
     * We are stopping / pausing the run; stop measurements.
     */
    @Override
    public void measureStop() {
        audioReader.stopReader();
    }
    

    /**
     * The application is closing down.  Clean up any resources.
     */
    @Override
    public void appStop() {
    }
    

    // ******************************************************************** //
    // Gauges.
    // ******************************************************************** //

    /**
     * Get a waveform gauge for this audio analyser.
     * 
     * @param   surface     The surface in which the gauge will be displayed.
     * @return              A gauge which will display the audio waveform.
     */
    public WaveformGauge getWaveformGauge(SurfaceRunner surface) {
        if (waveformGauge != null)
            throw new RuntimeException("Already have a WaveformGauge" +
                                       " for this AudioAnalyser");
        waveformGauge = new WaveformGauge(surface);
        return waveformGauge;
    }
    

    /**
     * Get a spectrum analyser gauge for this audio analyser.
     * 
     * @param   surface     The surface in which the gauge will be displayed.
     * @return              A gauge which will display the audio waveform.
     */
    public SpectrumGauge getSpectrumGauge(SurfaceRunner surface) {
        if (spectrumGauge != null)
            throw new RuntimeException("Already have a SpectrumGauge" +
                                       " for this AudioAnalyser");
        spectrumGauge = new SpectrumGauge(surface);
        return spectrumGauge;
    }
    

    /**
     * Get a signal power gauge for this audio analyser.
     * 
     * @param   surface     The surface in which the gauge will be displayed.
     * @return              A gauge which will display the signal power in
     *                      a dB meter.
     */
    public PowerGauge getPowerGauge(SurfaceRunner surface) {
        if (powerGauge != null)
            throw new RuntimeException("Already have a PowerGauge" +
                                       " for this AudioAnalyser");
        powerGauge = new PowerGauge(surface);
        return powerGauge;
    }
    

    // ******************************************************************** //
    // Audio Processing.
    // ******************************************************************** //

    /**
     * Handle audio input.  This is called on the thread of the audio
     * reader.
     * 
     * @param   buffer      Audio data that was just read.
     */
    private void processAudio(short[] buffer) {
        // Process the buffer.  While reading it, it needs to be locked.
        synchronized (buffer) {
            // Calculate the power now, while we have the input
            // buffer; this is pretty cheap.
            final int len = buffer.length;
            
            // If we have a power gauge, calculate the signal power.
            if (powerGauge != null)
                currentPower = SignalPower.calculatePowerDb(buffer, 0, len);

            // If we have a spectrum analyser, set up the FFT input data.
            if (spectrumGauge != null)
                fourierTransformer.setInput(buffer, len - FFT_BLOCK, FFT_BLOCK);

            // Draw the waveform now, while we have the raw data.
            //                drawWaveform(now, buffer, len - FFT_BLOCK, FFT_BLOCK,
            //                             waveCanvas, 0, 0, waveRect.width(), waveRect.height());
            if (waveformGauge != null) {
                SignalPower.biasAndRange(buffer, 0, len, biasRange);
                final float bias = biasRange[0];
                float range = biasRange[1];
                if (range < 1f)
                    range = 1f;
                
                waveformGauge.update(buffer, bias, range);
            }

            // Tell the reader we're done with the buffer.
            buffer.notify();
        }

        // If we have a spectrum analyser, perform the FFT.
        if (spectrumGauge != null) {
            // Do the (expensive) transformation.
            // The transformer has its own state, no need to lock here.
            fourierTransformer.transform();

            // Get the FFT output and draw the spectrum.
            fourierTransformer.getResults(spectrumData);
            spectrumGauge.update(spectrumData, NYQUIST_FREQ);
        }
        
        // If we have a power gauge, display the signal power.
        if (powerGauge != null)
            powerGauge.update(currentPower);
    }

    
    // ******************************************************************** //
    // Save and Restore.
    // ******************************************************************** //

    /**
     * Save the state of the game in the provided Bundle.
     * 
     * @param   icicle      The Bundle in which we should save our state.
     */
    @Override
    protected void saveState(Bundle icicle) {
//      gameTable.saveState(icicle);
    }


    /**
     * Restore the game state from the given Bundle.
     * 
     * @param   icicle      The Bundle containing the saved state.
     */
    @Override
    protected void restoreState(Bundle icicle) {
//      gameTable.pause();
//      gameTable.restoreState(icicle);
    }
    

    // ******************************************************************** //
    // Class Data.
    // ******************************************************************** //

    // Debugging tag.
	@SuppressWarnings("unused")
	private static final String TAG = "instrument";

    // Audio sample rate, in samples/sec.
    private static final int SAMPLE_RATE = 8000;

    // The Nyquist frequency -- the highest frequency we can sample.
    private static final int NYQUIST_FREQ = SAMPLE_RATE / 2;

    // Audio buffer size, in samples.
    private static final int FFT_BLOCK = 256;

    // Amount by which we decimate the input for each FFT.  We read this
    // many multiples of FFT_BLOCK, but then FFT only the last FFT_BLOCK
    // samples.
    private static final int DECIMATE = 2;

	
	// ******************************************************************** //
	// Private Data.
	// ******************************************************************** //
    
    // Our audio input device.
    private final AudioReader audioReader;

    // Fourier Transform calculator we use for calculating the spectrum.
    private final FFTTransformer fourierTransformer;
    
    // The gauges associated with this instrument.  Any may be null if not
    // in use.
    private WaveformGauge waveformGauge = null;
    private SpectrumGauge spectrumGauge = null;
    private PowerGauge powerGauge = null;
   
    // Analysed audio spectrum data.
    private final float[] spectrumData;

    // Current signal power level.
    private float currentPower = 0f;

    // Temp. buffer for calculated bias and range.
    private float[] biasRange = null;

}

