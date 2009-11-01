
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

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.util.Log;
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
        
        appContext = app;
        
        audioManager = (AudioManager) appContext.getSystemService(Context.AUDIO_SERVICE);

        audioBufferBytes = AudioRecord.getMinBufferSize(SAMPLE_RATE,
                                     AudioFormat.CHANNEL_CONFIGURATION_MONO,
                                     AudioFormat.ENCODING_PCM_16BIT) * 2;
        audioInput = new AudioRecord(MediaRecorder.AudioSource.MIC,
                                     SAMPLE_RATE,
                                     AudioFormat.CHANNEL_CONFIGURATION_MONO,
                                     AudioFormat.ENCODING_PCM_16BIT,
                                     audioBufferBytes);
        audioBufferSamples = audioBufferBytes / 2;
        
        fourierTransformer = new FourierTransformer(FFT_BLOCK);
        
        audioBuffer = new short[audioBufferSamples];
        audioIndex = 0;
        
        spectrumData = new float[FFT_BLOCK / 2];
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
        // Create the basic image of the garden.  Make it the right
        // size and pixel format for efficiency.
//        gardenImage = loadImage(R.drawable.sand, width, height);
//        gardenImageDark = loadImage(R.drawable.sand_dark, width, height);
        
        // Create the bitmap we actually render to.  Make the Canvas which
        // draws into it -- used for drawing in the finger path.
        renderBitmap = getBitmap();
        renderCanvas = new Canvas(renderBitmap);

        // Make a Paint for drawing in the garden.  This is used to render
        // the current stroke.
        fingerPaint = new Paint();
        fingerPaint.setAntiAlias(true);
    }
    

    /**
     * We are starting the animation loop.  The screen size is known.
     * 
     * <p>doUpdate() and doDraw() may be called from this point on.
     */
    @Override
    protected void animStart() {
        audioInput.startRecording();
    }
    

    /**
     * We are stopping the animation loop, for example to pause the app.
     * 
     * <p>doUpdate() and doDraw() will not be called from this point on.
     */
    @Override
    protected void animStop() {
        audioInput.stop();
    }
    

    /**
     * The application is closing down.  Clean up any resources.
     */
    @Override
    protected void appStop() {
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
     * @param   now         Current time in ms.
     */
    @Override
    protected void doUpdate(long now) {
        // If we have less than an FFT block in the buffer, we'll just add to
        // it.  Otherwise we'll start at the beginning.
        if (audioIndex >= FFT_BLOCK)
            audioIndex = 0;
        int space = audioBufferSamples - audioIndex;

        int nread = audioInput.read(audioBuffer, audioIndex, space);
        audioIndex += nread;
        lastReadCount = nread;

        // See if we've read a complete block.  If so, FFT it.
        if (audioIndex >= FFT_BLOCK) {
            int pos = audioIndex - FFT_BLOCK;
            long fftStart = System.currentTimeMillis();
            fourierTransformer.fftMag(audioBuffer, pos, FFT_BLOCK, spectrumData);
            lastFftTime = System.currentTimeMillis() - fftStart;
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
     * @param   now         Current time in ms.  Will be the same as that
     *                      passed to doUpdate(), if there was a preceeding
     *                      call to doUpdate().
     */
    @Override
    protected void doDraw(Canvas canvas, long now) {
//        canvas.drawBitmap(renderBitmap, 0, 0, null);
//        canvas.drawBitmap(gardenImage, 0, 0, null);
        
        canvas.drawColor(0xff000000);
        
        fingerPaint.setColor(0xffff0000);
        canvas.drawText("Read: " + lastReadCount, 10, 10, fingerPaint);
        canvas.drawText("FFT: " + lastFftTime, 10, 30, fingerPaint);
        
        int x = 32;
        int y = 400;
        for (int i = 0; i < FFT_BLOCK / 2; ++i) {
            float bar = y - spectrumData[i] * 256f;
            canvas.drawRect(x, bar, x + 1, y, fingerPaint);
            x += 2;
        }
    }
    

    // ******************************************************************** //
    // Digital Signal Processing.
    // ******************************************************************** //

    /**
     * Digital filter designed by mkfilter/mkshape/gencode, by A.J. Fisher.
     * Parameters:
     *     filtertype   =   Butterworth
     *     passtype     =   Bandpass
     *     order        =   2
     *     samplerate   =   11025
     *     corner1      =   800
     *     corner2      =   1200
     * Command line:
     *     /www/usr/fisher/helpers/mkfilter
     *                -Bu -Bp -o 2 -a 7.2562358277e-02 1.0884353741e-01 -l
     */

    private static final int NZEROS = 4;
    private static final int NPOLES = 4;
    private static final float GAIN = 8.965792418e+01f;

    private static float[] xv = new float[NZEROS + 1];
    private static float[] yv = new float[NPOLES + 1];

    private float filterloop(float next) {
        xv[0] = xv[1]; xv[1] = xv[2]; xv[2] = xv[3]; xv[3] = xv[4]; 
        xv[4] = next / GAIN;
        yv[0] = yv[1]; yv[1] = yv[2]; yv[2] = yv[3]; yv[3] = yv[4]; 
        yv[4] =   (xv[0] + xv[4]) - 2 * xv[2]
                     + ( -0.7244344274f * yv[0]) + (  2.6514151770f * yv[1])
                     + ( -4.1246731962f * yv[2]) + (  3.1184723705f * yv[3]);
        return yv[4];
    }

    
    
    


    private float calculatePowerDb(short[] sdata, int samples) {
        /**
        Calculate power of the signal depending on format.

        Since the signal may not have an average value of 0 precisely,
        we shouldn't simply calculate:

        sum_for_all_samples (pulse_value²) / number_of_samples

        but this formula assumes that the average is zero, which is not
        always true (for example, in 8 bits on a Sound Blaster 64,
        there is always a shift by one unit.

        We could calculate in two passes, first the average, then the
        power of the measure minus the average. But we can do this in
        one pass.

        Let measure = signal + bias,
        where measure is the pulse value,
        signal is what we want,
        bias is a constant, such that the average of signal is zero.
        
        What we want is the value of: power = sum_for_all_samples (signal²)

        Let's calculate in the same pass:
        a=sum_for_all_samples (measure²)
        and
        b=sum_for_all_samples (measure)
        
        Then a and b are equivalent to:
        a = sum_for_all_samples (measure²)
          = sum_for_all_samples ((signal + bias)²)
          = sum_for_all_samples (signal² + bias²)
          = sum_for_all_samples (signal²) + number_of_samples * bias²
      
        and 
        b = sum_for_all_samples (measure)
          = bias * number_of_samples
        that is, number_of_samples * bias² = b² / number_of_samples

        So a = power + b² / number_of_samples

        And power = a - b² / number_of_samples

        So we've got the correct power of the signal in one pass.
        
      */



        long b = 0;
        long a = 0;
        float floatPower = 0;
        for (int i = 0; i < samples; i++) {
            /* Since we calculate the square of something that can be
              as big as +-32767 we assume a width of at least 32 bits
              for a signed int. Moreover, we add a thousand of these
              to calculate power, so 32 bits aren't enough. I chose 64
              bits unsigned int for precision. We could have switched
              to float or double instead... */
            int thispulse = sdata[i];
            /* Note: we calculate max value anyway, to detect clipping */
            a += (thispulse * thispulse);
            b += thispulse;
        }

        /* Ok for raw power. Now normalize it. */
        long power = a - b * b / samples;

        int maxAmp = 32768;
        floatPower = ((float) power) / ((float) maxAmp) / ((float) maxAmp) / ((float) samples);

        /* we want leftmost to be 100dB
          (though signal-to-noise ratio can't be more than 96.33dB in power)
          and rightmost to be 0dB (maximum power) */
        float dBvalue = 1f + 0.1f * (float) Math.log10(floatPower);
        return dBvalue;
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

    // Audio sample rate, in samples/sec.
    private static final int SAMPLE_RATE = 8000;

    // Audio buffer size, in samples.
    private static final int FFT_BLOCK = 256;

	
	// ******************************************************************** //
	// Private Data.
	// ******************************************************************** //
    
	// Our application context.
	private final Context appContext;
    
    // Our audio manager.
    AudioManager audioManager;

    // Our audio input device.
    private final AudioRecord audioInput;
    
    // Fourier Transform calculator we use for calculating the spectrum.
    private final FourierTransformer fourierTransformer;
    
    // Our audio input buffer, and the index of the next item to go in.
    private final int audioBufferBytes;
    private final int audioBufferSamples;
    private final short[] audioBuffer;
    private int audioIndex = 0;
    
    // Last audio read size.
    private int lastReadCount = 0;
    
    // Analysed audio spectrum data.
    private final float[] spectrumData;
    
    // Time in ms that the last FFT took.
    private long lastFftTime = 0;

    // Bitmap in which we maintain the current image of the garden,
	// and the Canvas for drawing into it.
	private Bitmap renderBitmap = null;
	private Canvas renderCanvas = null;

    // Paint used for drawing the display.
    private Paint fingerPaint = null;

    // Last touch event position.
    float lastX = 0.0f;
    float lastY = 0.0f;
    
}

