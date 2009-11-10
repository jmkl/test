
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


import org.hermit.android.core.SurfaceRunner;
import org.hermit.android.io.AudioReader;
import org.hermit.dsp.FFTTransformer;
import org.hermit.dsp.SignalPower;
import org.hermit.utils.CharFormatter;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.Paint.Style;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.MotionEvent;


/**
 * The main audio analyser view.  This class relies on the parent SurfaceRunner
 * class to do the bulk of the animation control.
 */
public class AudioMeter
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
    public AudioMeter(Context app) {
        super(app, OPTION_DYNAMIC);
        
        audioReader = new AudioReader();
        
        fourierTransformer = new FFTTransformer(FFT_BLOCK);
        
        spectrumData = new float[FFT_BLOCK / 2];
        
        meterPeaks = new float[METER_PEAKS];
        meterPeakTimes = new long[METER_PEAKS];
        meterPrevious = new float[METER_AVERAGE_COUNT];
        
        biasRange = new float[2];
        dbBuffer = "-100.0 dB".toCharArray();
        
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
        layout(width, height);
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
    // Layout Processing.
    // ******************************************************************** //

    /**
     * Lay out the display for a given screen size.
     * 
     * @param   width       The new width of the surface.
     * @param   height      The new height of the surface.
     */
    private void layout(int width, int height) {
        // Make up some layout parameters.
        int gutter = width / 20;
        if (Math.min(width, height) > 400)
            gutter = width / 15;

        if (width > height)
            layoutLandscape(width, height, gutter);
        else
            layoutPortrait(width, height, gutter);
        
        // Create the bitmap for the audio waveform display,
        // and the Canvas for drawing into it.
        waveBitmap = getBitmap(waveRect.width(), waveRect.height());
        waveCanvas = new Canvas(waveBitmap);
        
        // Create the bitmap for the audio spectrum display,
        // and the Canvas for drawing into it.
        spectrumBitmap = getBitmap(specRect.width(), specRect.height());
        spectrumCanvas = new Canvas(spectrumBitmap);

        // Create the bitmap for the VU meter display,
        // and the Canvas for drawing into it.
        meterBitmap = getBitmap(meterRect.width(), meterRect.height());
        meterCanvas = new Canvas(meterBitmap);

        // Make a Paint for drawing the display.
        screenPaint = new Paint();
        screenPaint.setTypeface(Typeface.MONOSPACE);
        
        // Do some layout within the meter.
        int mh = meterRect.height();
        meterBarY = 0;
        meterBarHeight = 32;
        meterLabSize = 14;
        meterLabY = meterBarY + meterBarHeight + meterLabSize + 2;
        meterTextSize = 42;
        meterTextY = meterLabY + meterTextSize + 4;
        meterBarMargin = meterLabSize * 1;
        
        // Draw in the meter background.
        drawVuMeterBg(meterCanvas,
                      0, 0, meterRect.width(), meterRect.height());
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
    // Audio Processing.
    // ******************************************************************** //

    /**
     * Handle audio input.
     */
    private void processAudio(short[] buffer) {
        // Lock to protect updates to these local variables.  See doUpdate().
        synchronized (this) {
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
        // We need to lock to get the current audioData
        // and audioSequence correctly; see processAudio().
        short[] buffer = null;
        synchronized (this) {
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
                             waveCanvas, 0, 0, waveRect.width(), waveRect.height());
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
            drawSpectrum(now, spectrumData, spectrumCanvas,
                         0, 0, specRect.width(), specRect.height());
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
        // Draw the components on to the screen.
        canvas.drawColor(0xff000000);
        canvas.drawBitmap(waveBitmap, waveRect.left, waveRect.top, null);
        canvas.drawBitmap(spectrumBitmap, specRect.left, specRect.top, null);
        
        // Draw the VU meter.  We do this every frame, even though the
        // power level may not have changed (if a new audio read hasn't
        // arrived), so that the peak bars can animate smoothly.
        canvas.drawBitmap(meterBitmap, meterRect.left, meterRect.top, null);
        drawVuMeter(now, currentPower, canvas,
                    meterRect.left, meterRect.top,
                    meterRect.width(), meterRect.height());
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
        
        // Get the signal bias and range.
        SignalPower.biasAndRange(buffer, off, len, biasRange);
        final float bias = biasRange[0];
        float range = biasRange[1];
        if (range < 1f)
            range = 1f;

        // Calculate a scaling factor.  We want a degree of AGC, but not
        // so much that the waveform is always the same height.  Note we have
        // to take bias into account, otherwise we could scale the signal
        // off the screen.
        float scale = (float) Math.pow(1f / (range / 6500f), 0.7) / 16384 * ch;
        if (Float.isInfinite(scale))
            scale = 0f;
        if (scale < 0.001f)
            scale = 0.001f;
        else if (scale > 1000f)
            scale = 1000f;
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
     * Draw the VU meter background into the given canvas.
     * 
     * @param   canvas      The Canvas to draw into.
     * @param   cx          X position to draw at.
     * @param   cy          Y position to draw at.
     * @param   w           Width to draw in.
     * @param   h           Height to draw in.
     */
    private void drawVuMeterBg(Canvas canvas,
                               int cx, int cy, int w, int h)
    {
        canvas.drawColor(0xff000000);
        
        screenPaint.setColor(0xffffff00);
        screenPaint.setStyle(Style.STROKE);
        
        // Draw the grid.
        final int mx = cx + meterBarMargin;
        final int mw = w - meterBarMargin * 2;
        final int by = meterBarY;
        final int bh = meterBarHeight;
        final float bw = mw - 1f;
        final float gw = bw / 10f;
        canvas.drawRect(mx, by, mx + bw - 1, by + bh - 1, screenPaint);
        for (float i = 0f; i < bw; i += gw) {
            canvas.drawLine(mx + i + 1, by, mx + i + 1, by + bh - 1, screenPaint);
        }

        // Draw the labels below the grid.
        final int ly = meterLabY;
        final int ls = meterLabSize;
        screenPaint.setTextSize(ls);
        for (int i = 0; i <= 10; ++i) {
            String text = "" + (i * 10 - 100);
            float tw = screenPaint.measureText(text);
            float lx = mx + i * gw + 1 - (tw / 2);
            canvas.drawText(text, lx, ly, screenPaint);
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
     * @param   w           Width to draw in.
     * @param   h           Height to draw in.
     */
    private void drawVuMeter(long now, float power, Canvas canvas,
                             int cx, int cy, int w, int h)
    {
        // Get the previous power value, and add the new value into the
        // history buffer.  Re-calculate the rolling average power value.
        if (++meterIndex >= meterPrevious.length)
            meterIndex = 0;
        float prev = meterPrevious[meterIndex];
        meterPrevious[meterIndex] = power;
        meterAverage -= prev / METER_AVERAGE_COUNT;
        meterAverage += power / METER_AVERAGE_COUNT;
        
        // Re-calculate the peak markers.
        calculatePeaks(now, power, prev);

        screenPaint.setColor(0xffffff00);
        screenPaint.setStyle(Style.STROKE);
        
        // Position parameters.
        final int mx = cx + meterBarMargin;
        final int mw = w - meterBarMargin * 2;
        final int by = cy + meterBarY;
        final int bh = meterBarHeight;
        final float gap = 6f;
        final float bw = mw - 1f;

        // Draw the average bar.
        final float pa = meterAverage * bw;
        screenPaint.setStyle(Style.FILL);
        screenPaint.setColor(METER_AVERAGE_COL);
        canvas.drawRect(mx, by + gap, mx + pa + 1, by + bh - gap, screenPaint);
        
        // Draw the power bar.
        final float p = power * bw;
        screenPaint.setStyle(Style.FILL);
        screenPaint.setColor(METER_POWER_COL);
        canvas.drawRect(mx, by + gap * 2, mx + p + 1, by + bh - gap * 2, screenPaint);

        // Now, draw in the peaks.
        screenPaint.setStyle(Style.FILL);
        for (int i = 0; i < METER_PEAKS; ++i) {
            if (meterPeakTimes[i] != 0) {
                // Fade the peak according to its age.
                long age = now - meterPeakTimes[i];
                float fac = 1f - ((float) age / (float) METER_PEAK_TIME);
                int alpha = (int) (fac * 255f);
                screenPaint.setColor(METER_PEAK_COL | (alpha << 24));
                // Draw it in.
                final float pp = meterPeaks[i] * bw;
                canvas.drawRect(mx + pp - 1, by + gap * 2,
                                mx + pp + 3, by + bh - gap * 2, screenPaint);
            }
        }

        // Draw the text below the meter.
        final int ty = cy + meterTextY;
        final int ts = meterTextSize;
        final float dB = (meterAverage - 1f) * 100f;
        CharFormatter.formatFloat(dbBuffer, 0, dB, 6, 1);
        screenPaint.setStyle(Style.STROKE);
        screenPaint.setColor(0xff00ffff);
        screenPaint.setTextSize(ts);
//      private int meterTextY = 0;
//      private int meterTextSize = 0;

        canvas.drawText(dbBuffer, 0, dbBuffer.length, cx, ty, screenPaint);
    }


    /**
     * Re-calculate the positions of the peak markers in the VU meter.
     */
    private void calculatePeaks(long now, float power, float prev) {
        // First, delete any that have been passed or have timed out.
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

    // Number of peaks we will track in the VU meter.
    private static final int METER_PEAKS = 4;

    // Time in ms over which peaks in the VU meter fade out.
    private static final int METER_PEAK_TIME = 4000;

    // Number of updates over which we average the VU meter to get
    // a rolling average.
    private static final int METER_AVERAGE_COUNT = 200;

    // Colours for the meter pwoer bar and average bar and peak marks.
    // In METER_PEAK_COL, alpha is set dynamically in the code.
    private static final int METER_POWER_COL = 0xffffff00;
    private static final int METER_AVERAGE_COL = 0x8080d000;
    private static final int METER_PEAK_COL = 0x00ff0000;

	
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
    
    // Bounding rectangles for the waveform, spectrum, and VU meter displays.
    private Rect waveRect = null;
    private Rect specRect = null;
    private Rect meterRect = null;
    
    // Layout parameters for the VU meter.  Position and size for the
    // bar itself; position and size for the bar labels; position
    // and size for the main readout text.
    private int meterBarY = 0;
    private int meterBarHeight = 0;
    private int meterLabY = 0;
    private int meterLabSize = 0;
    private int meterTextY = 0;
    private int meterTextSize = 0;
    private int meterBarMargin = 0;
    
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
    
    // Temp. buffer for calculated bias and range.
    private float[] biasRange = null;
    
    // Buffer for displayed dB value text.
    private char[] dbBuffer = null;

}

