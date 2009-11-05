
/**
 * Wind Blink: a wind meter for Android.
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


package org.hermit.windblink;


import org.hermit.android.core.SurfaceRunner;
import org.hermit.dsp.FFTTransformer;
import org.hermit.dsp.SignalPower;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.MotionEvent;


/**
 * The main wind meter view.  This class relies on the parent SurfaceRunner
 * class to do the bulk of the animation control.
 */
public class WindMeter
	extends SurfaceRunner
{

    // ******************************************************************** //
    // Constructor.
    // ******************************************************************** //

	/**
	 * Create a WindMeter instance.
	 * 
	 * @param	app			The application context we're running in.
	 */
    public WindMeter(Context app) {
        super(app);
        
        audioReader = new AudioReader();
        
        fourierTransformer = new FFTTransformer(FFT_BLOCK);
        
        spectrumData = new float[FFT_BLOCK / 2];
        
        meterPeaks = new float[METER_PEAKS];
        meterPeakTimes = new long[METER_PEAKS];
        meterPrevious = new float[METER_AVERAGE_COUNT];
        
        // On-screen debug stats display.
        statsCreate(new String[] { "µs FFT", "µs dWav", "µs dSpe", "µs dMet", "skip/s" });
        setDebugPerf(false);
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
    protected void appStart() {
    }


    /**
     * Set the screen size.  This is guaranteed to be called before
     * animStart(), but perhaps not before appStart().
     * 
     * @param   width       The new width of the surface.
     * @param   height      The new height of the surface.
     * @param   config      The pixel format of the surface.
     */
    @Override
    protected void appSize(int width, int height, Bitmap.Config config) {
        // Create the bitmap for the audio waveform display,
        // and the Canvas for drawing into it.
        waveBitmap = getBitmap(WAVE_WIDTH, WAVE_HEIGHT);
        waveCanvas = new Canvas(waveBitmap);
        
        // Create the bitmap for the audio spectrum display,
        // and the Canvas for drawing into it.
        spectrumBitmap = getBitmap(SPECTRUM_WIDTH, SPECTRUM_HEIGHT);
        spectrumCanvas = new Canvas(spectrumBitmap);

        // Create the bitmap for the VU meter display,
        // and the Canvas for drawing into it.
        meterBitmap = getBitmap(METER_WIDTH, METER_HEIGHT);
        meterCanvas = new Canvas(meterBitmap);

        // Make a Paint for drawing the display.
        screenPaint = new Paint();
    }
    

    /**
     * We are starting the animation loop.  The screen size is known.
     * 
     * <p>doUpdate() and doDraw() may be called from this point on.
     */
    @Override
    protected void animStart() {
        audioReader.startReader(FFT_BLOCK * DECIMATE, new AudioReader.Listener() {
            @Override
            public void onReadComplete(short[] buffer) {
                processAudio(buffer);
            }
        });
    }
    

    /**
     * We are stopping the animation loop, for example to pause the app.
     * 
     * <p>doUpdate() and doDraw() will not be called from this point on.
     */
    @Override
    protected void animStop() {
        audioReader.stopReader();
    }
    

    /**
     * The application is closing down.  Clean up any resources.
     */
    @Override
    protected void appStop() {
    }
    

    // ******************************************************************** //
    // Audio Processing.
    // ******************************************************************** //

    /**
     * Handle audio input.
     */
    private void processAudio(short[] buffer) {
        // Lock on audioReader to protect updates to these local variables.
        // See doUpdate().
        synchronized (audioReader) {
            audioData = buffer;
            ++audioSequence;
        }
    }
    
    
    // ******************************************************************** //
    // Animation Rendering.
    // ******************************************************************** //
    
    /**
     * Update the state of the application for the current frame.
     * 
     * <p>Applications must override this, and can use it to update
     * for example the physics of a game.  This may be a no-op in some cases.
     * 
     * <p>doDraw() will always be called after this method is called;
     * however, the converse is not true, as we sometimes need to draw
     * just to update the screen.  Hence this method is useful for
     * updates which are dependent on time rather than frames.
     * 
     * @param   now         Nominal time of the current frame in ms.
     */
    @Override
    protected void doUpdate(long now) {
        // We need to lock on audioReader to get the current audioData
        // and audioSequence correctly; see processAudio().
        short[] buffer = null;
        synchronized (audioReader) {
            if (audioData != null && audioSequence > audioProcessed) {
                // Count skipped blocks, if any.
                statsCount(4, (int) (audioSequence - audioProcessed - 1));
                audioProcessed = audioSequence;
                buffer = audioData;
            }
        }

        // If we got a buffer, process it.  While reading it, it needs
        // to be locked.
        if (buffer != null) {
            synchronized (buffer) {
                // Calculate the power now, while we have the input
                // buffer; this is pretty cheap.
                final int len = buffer.length;
                currentPower = SignalPower.calculatePowerDb(buffer, 0, len);

                // Set up the FFT input data.
                fourierTransformer.setInput(buffer, len - FFT_BLOCK, FFT_BLOCK);

                // Draw the waveform now, while we have the raw data.
                long wavStart = System.currentTimeMillis();
                drawWaveform(now, buffer, len - FFT_BLOCK, FFT_BLOCK,
                             waveCanvas, 0, 0, WAVE_WIDTH, WAVE_HEIGHT);
                long wavEnd = System.currentTimeMillis();
                statsTime(1, (wavEnd - wavStart) * 1000);

                // Tell the reader we're done with the buffer.
                buffer.notify();
            }
            
            // Do the (expensive) transformation.
            // The transformer has its own state, no need to lock here.
            long fftStart = System.currentTimeMillis();
            fourierTransformer.transform();
            long fftEnd = System.currentTimeMillis();
            statsTime(0, (fftEnd - fftStart) * 1000);

            // Get the FFT output and draw the spectrum.
            fourierTransformer.getResults(spectrumData);
            long speStart = System.currentTimeMillis();
            drawSpectrum(now, spectrumData,
                         spectrumCanvas, 0, 0, SPECTRUM_WIDTH, SPECTRUM_HEIGHT);
            long speEnd = System.currentTimeMillis();
            statsTime(2, (speEnd - speStart) * 1000);
        }
    }


    /**
     * Draw the current frame of the application.
     * 
     * <p>Applications must override this, and are expected to draw the
     * entire screen into the provided canvas.
     * 
     * <p>This method will always be called after a call to doUpdate(),
     * and also when the screen needs to be re-drawn.
     * 
     * @param   canvas      The Canvas to draw into.
     * @param   now         Nominal time of the current frame in ms.
     */
    @Override
    protected void doDraw(Canvas canvas, long now) {
        // Draw the VU meter.  We do this every frame, even though the
        // power level may not have changed (if a new audio read hasn't
        // arrived), so that the peak bars can animate smoothly.
        long vuStart = System.currentTimeMillis();
        drawVuMeter(now, currentPower, meterCanvas, 0, 0, METER_WIDTH, METER_HEIGHT);
        long vuEnd = System.currentTimeMillis();
        statsTime(3, (vuEnd - vuStart) * 1000);
  
        // Draw the components on to the screen.
        canvas.drawBitmap(waveBitmap, 32, 32, null);
        canvas.drawBitmap(spectrumBitmap, 32, 128, null);
        canvas.drawBitmap(meterBitmap, 32, 416, null);
    }

    
    /**
     * Draw the waveform of the current audio sample into the given canvas.
     * 
     * @param   now         Nominal time of the current frame in ms.
     * @param   buffer      Buffer containing the raw audio data to draw.
     * @param   off         Offset in data of the input data.
     * @param   len         Length of the input data.
     * @param   canvas      The Canvas to draw into.
     * @param   cx          X position to draw at.
     * @param   cy          Y position to draw at.
     * @param   cw          Width to draw in.
     * @param   ch          Height to draw in.
     */
    private void drawWaveform(long now, short[] buffer, int off, int len,
                              Canvas canvas, int cx, int cy, int cw, int ch)
    {
        canvas.drawColor(0xff000000);
        
        // Calculate a scaling factor.  We want a degree of AGC, but not
        // so much that the waveform is always the same height.  Also
        // calculate the signal bias -- if we scale without removing the
        // bias, a large scale moves the signal off-screen.
        float max = 0f;
        float bias = 0;
        for (int i = off; i < off + len; ++i) {
            bias += buffer[i];
            if (buffer[i] > max)
                max = buffer[i];
        }
        bias /= len;
        final float scale = (float) Math.pow(1f / (max / 6500f), 0.7) / 16384 * ch;
        
        final float baseY = cy + ch / 2f;
        final float uw = (float) cw / (float) len;

        // Draw the axes.
        screenPaint.setColor(0xffffff00);
        screenPaint.setStyle(Style.STROKE);
        canvas.drawLine(cx, cy, cx, cy + ch - 1, screenPaint);

        // Draw the waveform.  Drawing vertical lines up/down to the
        // waveform creates a "filled" effect, and is *much* faster
        // than drawing the waveform itself with diagonal lines.
        screenPaint.setColor(0xffffff00);
        screenPaint.setStyle(Style.STROKE);
        for (int i = 0; i < len; ++i) {
            final float x = cx + i * uw;
            final float y = baseY - (buffer[off + i] - bias) * scale;
            canvas.drawLine(x, baseY, x, y, screenPaint);
        }
    }

    
    /**
     * Draw the current frame of the application into the given canvas.
     * 
     * @param   now         Nominal time of the current frame in ms.
     * @param   data        Buffer containing the spectrum data to draw.
     * @param   canvas      The Canvas to draw into.
     * @param   cx          X position to draw at.
     * @param   cy          Y position to draw at.
     * @param   cw          Width to draw in.
     * @param   ch          Height to draw in.
     */
    private void drawSpectrum(long now, float[] data, Canvas canvas,
                              int cx, int cy, int cw, int ch)
    {
        canvas.drawColor(0xff000000);
        
        // Calculate a scaling factor.  We want a degree of AGC, but not
        // so much that the spectrum is always the same height.
        final int len = data.length;
        float max = 0f;
        for (int i = 1; i < len; ++i)
            if (data[i] > max)
                max = data[i];
        final float scale = (float) Math.pow(1f / (max / 0.2f), 0.8) * 5;
       
//        screenPaint.setColor(0xffff0000);
//        screenPaint.setStyle(Style.STROKE);
//        canvas.drawLine(x, y - 256f, x + 256f, y - 256f, screenPaint);
       
        screenPaint.setStyle(Style.FILL);
        paintColor[1] = 1f;
        paintColor[2] = 1f;
        final float bw = (float) cw / (float) len;
        for (int i = 1; i < len; ++i) {
            // Cycle the hue angle from 0° to 300°; i.e. red to purple.
            paintColor[0] = (float) i / (float) len * 300f;
            screenPaint.setColor(Color.HSVToColor(paintColor));
            
            // Draw the bar.
            final float x = cx + i * bw;
            final float y = cy + ch - data[i] * scale * ch;
            canvas.drawRect(x, y, x + bw, cy + ch, screenPaint);
        }
    }
    

    /**
     * Draw the VU meter into the given canvas.
     * 
     * @param   now         Nominal time of the current frame in ms.
     * @param   power       The power level to draw, range 0-1.
     * @param   canvas      The Canvas to draw into.
     * @param   cx          X position to draw at.
     * @param   cy          Y position to draw at.
     * @param   cw          Width to draw in.
     * @param   ch          Height to draw in.
     */
    private void drawVuMeter(long now, float power, Canvas canvas,
                             int cx, int cy, int cw, int ch)
    {
        canvas.drawColor(0xff000000);
        
        // Get the previous power value, and add the new value into the
        // history buffer.  Re-calcluate the rolling average power value.
        if (++meterIndex >= meterPrevious.length)
            meterIndex = 0;
        float prev = meterPrevious[meterIndex];
        meterPrevious[meterIndex] = power;
        meterAverage -= prev / METER_AVERAGE_COUNT;
        meterAverage += power / METER_AVERAGE_COUNT;

        screenPaint.setColor(0xffffff00);
        screenPaint.setStyle(Style.STROKE);
        canvas.drawRect(cx, cy, cx + cw - 1, cy + ch - 1, screenPaint);
        
        // Draw the grid.
        final float gap = 5f;
        final float bw = cw - 1f;
        final float gw = bw / 10f;
        for (float i = 0f; i < bw; i += gw)
            canvas.drawLine(cx + i + 1, cy, cx + i + 1, cy + ch - 1, screenPaint);

        // Draw the average bar.
        final float pa = meterAverage * bw;
        screenPaint.setStyle(Style.FILL);
        screenPaint.setColor(0xff00ffff);
        canvas.drawRect(cx, cy + gap, cx + pa + 1, cy + ch - gap, screenPaint);
        
        // Draw the power bar.
        final float p = power * bw;
        screenPaint.setStyle(Style.FILL);
        screenPaint.setColor(0xffffff00);
        canvas.drawRect(cx, cy + gap * 2, cx + p + 1, cy + ch - gap * 2, screenPaint);

        // Now, handle the peaks.  First, delete any that have been passed
        // or have timed out.
        for (int i = 0; i < METER_PEAKS; ++i) {
            if (meterPeakTimes[i] != 0 &&
                    (meterPeaks[i] < power ||
                     now - meterPeakTimes[i] > METER_PEAK_TIME))
                meterPeakTimes[i] = 0;
        }
        
        // If the meter has gone up, set a new peak, if there's an empty
        // slot.  If there isn't, don't bother, because we would be kicking
        // out a higher peak, which we don't want.
        if (power > prev) {
            boolean done = false;
            
            // First, check for a slightly-higher existing peak.  If there
            // is one, just bump its time.
            for (int i = 0; i < METER_PEAKS; ++i) {
                if (meterPeakTimes[i] != 0 && meterPeaks[i] - power < 0.025) {
                    meterPeakTimes[i] = now;
                    done = true;
                    break;
                }
            }
            
            if (!done) {
                // Now scan for an empty slot.
                for (int i = 0; i < METER_PEAKS; ++i) {
                    if (meterPeakTimes[i] == 0) {
                        meterPeaks[i] = power;
                        meterPeakTimes[i] = now;
                        break;
                    }
                }
            }
        }
        
        // Now, draw in the peaks.
        screenPaint.setStyle(Style.FILL);
        for (int i = 0; i < METER_PEAKS; ++i) {
            if (meterPeakTimes[i] != 0) {
                // Fade the peak according to its age.
                long age = now - meterPeakTimes[i];
                float fac = 1f - ((float) age / (float) METER_PEAK_TIME);
                int alpha = (int) (fac * 255f);
                screenPaint.setColor(Color.argb(alpha, 255, 0, 0));
                
                // Draw it in.
                final float pp = meterPeaks[i] * bw;
                canvas.drawRect(cx + pp - 1, cy + gap * 2,
                                cx + pp + 3, cy + ch - gap * 2, screenPaint);
            }
        }
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
	private static final String TAG = "WindMeter";

    // Audio buffer size, in samples.
    private static final int FFT_BLOCK = 256;

    // Amount by which we decimate the input for each FFT.  We read this
    // many multiples of FFT_BLOCK, but then FFT only the last FFT_BLOCK
    // samples.
    private static final int DECIMATE = 2;

    // Size for the waveform display.
    private static final int WAVE_WIDTH = 256;
    private static final int WAVE_HEIGHT = 64;

    // Size for the audio spectrum display.
    private static final int SPECTRUM_WIDTH = 256;
    private static final int SPECTRUM_HEIGHT = 256;

    // Size for the VU meter.
    private static final int METER_WIDTH = 256;
    private static final int METER_HEIGHT = 32;
    
    // Number of peaks we will track in the VU meter.
    private static final int METER_PEAKS = 4;

    // Time in ms over which peaks in the VU meter fade out.
    private static final int METER_PEAK_TIME = 4000;

    // Number of updates over which we average the VU meter to get
    // a rolling average.
    private static final int METER_AVERAGE_COUNT = 200;

	
	// ******************************************************************** //
	// Private Data.
	// ******************************************************************** //
    
    // Our audio input device.
    private final AudioReader audioReader;
    
    // Buffered audio data, and sequence number of the latest block.
    private short[] audioData;
    private long audioSequence = 0;
    
    // Sequence number of the last block we processed.
    private long audioProcessed = 0;

    // Fourier Transform calculator we use for calculating the spectrum.
    private final FFTTransformer fourierTransformer;
    
    // Analysed audio spectrum data.
    private final float[] spectrumData;

    // Current signal power level.
    private float currentPower = 0f;

    // Bitmap in which we draw the audio waveform display,
    // and the Canvas for drawing into it.
    private Bitmap waveBitmap = null;
    private Canvas waveCanvas = null;

    // Bitmap in which we draw the audio spectrum display,
    // and the Canvas for drawing into it.
    private Bitmap spectrumBitmap = null;
    private Canvas spectrumCanvas = null;

    // Bitmap in which we draw the VU meter display,
    // and the Canvas for drawing into it.
    private Bitmap meterBitmap = null;
    private Canvas meterCanvas = null;
    
    // Peak markers in the VU meter, and the times for each one.  A zero
    // time indicates a peak not set.
    private float[] meterPeaks = null;
    private long[] meterPeakTimes = null;

    // Buffered old meter levels, used to calculate the rolling average.
    // Index of the most recent value.
    private float[] meterPrevious = null;
    private int meterIndex = 0;
    
    // Rolling average power value.
    private float meterAverage = 0f;

    // Paint used for drawing the display.
    private Paint screenPaint = null;
    private float[] paintColor = { 0, 1, 1 };
    
}

