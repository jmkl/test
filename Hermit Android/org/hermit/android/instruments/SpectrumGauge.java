
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

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Paint.Style;


/**
 * A graphical display which displays the audio spectrum from an
 * {@link AudioAnalyser} instrument.  This class cannot be instantiated
 * directly; get an instance by calling
 * {@link AudioAnalyser#getSpectrumGauge(SurfaceRunner)}.
 */
public class SpectrumGauge
    extends Gauge
{

	// ******************************************************************** //
	// Constructor.
	// ******************************************************************** //
	
	/**
	 * Create a SpectrumGauge.  This constructor is package-local, as
	 * public users get these from an {@link AudioAnalyser} instrument.
	 * 
	 * @param	parent			Parent surface.
	 */
	SpectrumGauge(SurfaceRunner parent) {
	    super(parent);
	}


	// ******************************************************************** //
	// Geometry.
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
	    
	    dispX = bounds.left;
	    dispY = bounds.top;
	    dispWidth = bounds.width();
	    dispHeight = bounds.height();

        // Create the bitmap for the spectrum display,
        // and the Canvas for drawing into it.
        specBitmap = getSurface().getBitmap(dispWidth, dispHeight);
        specCanvas = new Canvas(specBitmap);
	}

	
    // ******************************************************************** //
    // Data Updates.
    // ******************************************************************** //
    
	/**
	 * New data from the instrument has arrived.  This method is called
	 * on the thread of the instrument.
	 * 
     * @param   data    An array of floats defining the signal power
     *                      at each frequency in the spectrum.
     * @param   nyquist     The Nyquist frequency -- the highest frequency
     *                      represented in the spectrum data.
	 */
	final void update(float[] data, int nyquist) {
        final Canvas canvas = specCanvas;
        final Paint paint = getPaint();
        
        // Calculate a scaling factor.  We want a degree of AGC, but not
        // so much that the spectrum is always the same height.
        final int len = data.length;
        float max = 0f;
        for (int i = 1; i < len; ++i)
            if (data[i] > max)
                max = data[i];
        final float scale = (float) Math.pow(1f / (max / 0.2f), 0.8) * 5;
        
        // Now actually do the drawing.
        synchronized (this) {
            canvas.drawColor(0xff000000);

            //        paint.setColor(0xffff0000);
            //        paint.setStyle(Style.STROKE);
            //        canvas.drawLine(x, y - 256f, x + 256f, y - 256f, paint);

            paint.setStyle(Style.FILL);
            paintColor[1] = 1f;
            paintColor[2] = 1f;
            final float bw = (float) dispWidth / (float) len;
            for (int i = 1; i < len; ++i) {
                // Cycle the hue angle from 0° to 300°; i.e. red to purple.
                paintColor[0] = (float) i / (float) len * 300f;
                paint.setColor(Color.HSVToColor(paintColor));

                // Draw the bar.
                final float x = i * bw;
                final float y = dispHeight - data[i] * scale * dispHeight;
                canvas.drawRect(x, y, x + bw, dispHeight, paint);
            }
        }
    }


	// ******************************************************************** //
	// View Drawing.
	// ******************************************************************** //
	
	/**
	 * Do the subclass-specific parts of drawing for this element.
	 * This method is called on the thread of the containing SuraceView.
	 * 
	 * <p>Subclasses should override this to do their drawing.
	 * 
	 * @param	canvas		Canvas to draw into.
	 * @param	paint		The Paint which was set up in initializePaint().
     * @param   now         Nominal system time in ms. of this update.
	 */
	@Override
    protected final void drawBody(Canvas canvas, Paint paint, long now) {
	    // Since drawBody may be called more often than we get audio
	    // data, it makes sense to just draw the buffered image here.
	    synchronized (this) {
	        canvas.drawBitmap(specBitmap, dispX, dispY, null);
	    }
	}
	

	// ******************************************************************** //
	// Class Data.
	// ******************************************************************** //

    // Debugging tag.
	@SuppressWarnings("unused")
	private static final String TAG = "instrument";


	// ******************************************************************** //
	// Private Data.
	// ******************************************************************** //
    
	// Display position and size within the parent view.
    private int dispX = 0;
    private int dispY = 0;
	private int dispWidth = 0;
	private int dispHeight = 0;
	
    // Bitmap in which we draw the audio waveform display,
    // and the Canvas and Paint for drawing into it.
    private Bitmap specBitmap = null;
    private Canvas specCanvas = null;

    // Buffer for calculating the draw colour from H,S,V values.
    private float[] paintColor = { 0, 1, 1 };

}

