
/**
 * org.hermit.android.instrument: graphical instruments for Android.
 * <br>Copyright 2009 Ian Cameron Smith
 * 
 * <p>These classes provide input and display functions for creating on-screen
 * instruments of various kinds in Android apps.
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
     * @param   parent          Parent surface.
	 */
    public AudioAnalyser(SurfaceRunner parent) {
        super(parent);
        
        audioReader = new AudioReader(SAMPLE_RATE);
        
        fourierTransformer = new FFTTransformer(FFT_BLOCK);
        
        // Allocate the spectrum data.
        spectrumData = new float[FFT_BLOCK / 2];
        spectrumHist = new float[FFT_BLOCK / 2][4];
        spectrumIndex = 0;

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
        audioProcessed = audioSequence = 0;
        
        audioReader.startReader(FFT_BLOCK * DECIMATE, new AudioReader.Listener() {
            @Override
            public final void onReadComplete(short[] buffer) {
                receiveAudio(buffer);
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
    private final void receiveAudio(short[] buffer) {
        // Lock to protect updates to these local variables.  See run().
        synchronized (this) {
            audioData = buffer;
            ++audioSequence;
        }
    }
    

    // ******************************************************************** //
    // Main Loop.
    // ******************************************************************** //

    /**
     * Update the state of the instrument for the current frame.
     * This method must be invoked from the doUpdate() method of the
     * application's {@link SurfaceRunner}.
     * 
     * <p>Since this is called frequently, we first check whether new
     * audio data has actually arrived.
     * 
     * @param   now         Nominal time of the current frame in ms.
     */
    @Override
    public final void doUpdate(long now) {
        short[] buffer = null;
        synchronized (this) {
            if (audioData != null && audioSequence > audioProcessed) {
//                parentSurface.statsCount(4, (int) (audioSequence - audioProcessed - 1));
                audioProcessed = audioSequence;
                buffer = audioData;
            }
        }

        // If we got data, process it without the lock.
        if (buffer != null)
            processAudio(buffer);
    }


    /**
     * Handle audio input.  This is called on the thread of the
     * parent surface.
     * 
     * @param   buffer      Audio data that was just read.
     */
    private final void processAudio(short[] buffer) {
        // Process the buffer.  While reading it, it needs to be locked.
        synchronized (buffer) {
            // Calculate the power now, while we have the input
            // buffer; this is pretty cheap.
            final int len = buffer.length;

            // Draw the waveform now, while we have the raw data.
            if (waveformGauge != null) {
                SignalPower.biasAndRange(buffer, len - FFT_BLOCK, FFT_BLOCK, biasRange);
                final float bias = biasRange[0];
                float range = biasRange[1];
                if (range < 1f)
                    range = 1f;
                
//                long wavStart = System.currentTimeMillis();
                waveformGauge.update(buffer, len - FFT_BLOCK, FFT_BLOCK, bias, range);
//                long wavEnd = System.currentTimeMillis();
//                parentSurface.statsTime(1, (wavEnd - wavStart) * 1000);
            }
            
            // If we have a power gauge, calculate the signal power.
            if (powerGauge != null)
                currentPower = SignalPower.calculatePowerDb(buffer, 0, len);

            // If we have a spectrum analyser, set up the FFT input data.
            if (spectrumGauge != null)
                fourierTransformer.setInput(buffer, len - FFT_BLOCK, FFT_BLOCK);

            // Tell the reader we're done with the buffer.
            buffer.notify();
        }

        // If we have a spectrum analyser, perform the FFT.
        if (spectrumGauge != null) {
            // Do the (expensive) transformation.
            // The transformer has its own state, no need to lock here.
            fourierTransformer.transform();

            // Get the FFT output and draw the spectrum.
//            fourierTransformer.getResults(spectrumData);
            spectrumIndex = fourierTransformer.getResults(spectrumData, spectrumHist, spectrumIndex);
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
    
    // Buffered audio data, and sequence number of the latest block.
    private short[] audioData;
    private long audioSequence = 0;
    
    // Sequence number of the last block we processed.
    private long audioProcessed = 0;

    // Analysed audio spectrum data; history data for each frequency
    // in the spectrum; and index into the history data.
    private final float[] spectrumData;
    private final float[][] spectrumHist;
    private int spectrumIndex;
    
    // Current signal power level.
    private float currentPower = 0f;

    // Temp. buffer for calculated bias and range.
    private float[] biasRange = null;

}

