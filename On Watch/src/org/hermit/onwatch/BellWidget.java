
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

import org.hermit.onwatch.TimeModel.Field;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.View;


/**
 * This class displays the time within a watch in graphical form, using bells.
 */
public class BellWidget
	extends View
{

	// ******************************************************************** //
    // Constructors.
    // ******************************************************************** //
	
	/**
	 * Create a watch clock.
	 * 
	 * @param	context			Parent application.
	 */
	public BellWidget(Context context) {
		super(context);
		init(context);
	}


	/**
	 * Create a crew watch schedule display.
	 * 
	 * @param	context			Parent application.
	 * @param	attrs			Layout attributes.
	 */
	public BellWidget(Context context, AttributeSet attrs) {
		super(context, attrs);
		init(context);
	}

	
	private void init(Context context) {
		appContext = context;

		// Get the time model.  Get a callback every minute to update
		// the display.
		timeModel = TimeModel.getInstance(context);
		timeModel.listen(TimeModel.Field.CHIMING, new TimeModel.Listener() {
			@Override
			public void change(Field field, int value, long time) {
				invalidate();
			}
		});
	}


    // ******************************************************************** //
	// Geometry.
	// ******************************************************************** //

	/**
	 * Measure the view and its content to determine the measured width
	 * and the measured height.  This method is invoked by measure(int, int)
	 * and should be overriden by subclasses to provide accurate and efficient
	 * measurement of their contents.
	 *
	 * CONTRACT: When overriding this method, you must call
	 * setMeasuredDimension(int, int) to store the measured width and height
	 * of this view.
	 * 
	 * @param	wspec		Horizontal space requirements as imposed by the
	 * 						parent, encoded with View.MeasureSpec.
	 * @param	hspec		Vertical space requirements as imposed by the
	 * 						parent, encoded with View.MeasureSpec.
	 */
	@Override
	protected void onMeasure(int wspec, int hspec) {
		int baseWidth = BELL_SIZE * 8 + BELL_PAD_X * 2;
		int baseHeight = BELL_SIZE + BELL_PAD_Y * 2;
		
        int widthMode = MeasureSpec.getMode(wspec);
        int widthSize =  MeasureSpec.getSize(wspec);
        int heightMode = MeasureSpec.getMode(hspec);
        int heightSize =  MeasureSpec.getSize(hspec);

        float hScale = 1.0f;
        float vScale = 1.0f;

        if (widthMode != MeasureSpec.UNSPECIFIED)
            hScale = (float) widthSize / (float) baseWidth;

        if (heightMode != MeasureSpec.UNSPECIFIED)
            vScale = (float )heightSize / (float) baseHeight;

        float scale = Math.min(hScale, vScale);
        if (scale > 1.1f)
        	scale = 1.1f;

        int hSize = resolveSize((int) (baseWidth * scale), wspec);
        int vSize = resolveSize((int) (baseHeight * scale), hspec);
        setMeasuredDimension(hSize, vSize);
	}
	
	 
    /**
     * This is called during layout when the size of this view has
     * changed.  This is where we first discover our window size, so set
     * our geometry to match.
     * 
     * @param	width			Current width of this view.
     * @param	height			Current height of this view.
     * @param	oldw			Old width of this view.  0 if we were
     * 							just added.
     * @param	oldh			Old height of this view.   0 if we were
     * 							just added.
     */
	@Override
	protected void onSizeChanged(int width, int height, int oldw, int oldh) {
    	super.onSizeChanged(width, height, oldw, oldh);

    	if (width <= 0 || height <= 0)
    		return;

    	bellBoxWidth = width / 8 - BELL_PAD_X * 2;
    	bellBoxHeight = height;
    	
    	// Get the bell images.
    	Resources res = appContext.getResources();
    	Bitmap l = BitmapFactory.decodeResource(res, R.drawable.bell);
    	Bitmap g = BitmapFactory.decodeResource(res, R.drawable.bell_gray);
    	Bitmap n = BitmapFactory.decodeResource(res, R.drawable.bell_ghost);
    	int iw = l.getWidth();
    	int ih = l.getHeight();
    	bellImageWidth = Math.min(bellBoxWidth, iw);
    	bellImageHeight = Math.min(bellBoxHeight, ih);

    	// Create bell images scaled to the screen.
    	if (bellImageWidth == iw && bellImageHeight == ih) {
    		bellLight = l;
    		bellGray = g;
    		bellGhost = n;
    	} else {
    		bellLight = Bitmap.createScaledBitmap(l, bellImageWidth, bellImageHeight, true);
    		bellGray = Bitmap.createScaledBitmap(g, bellImageWidth, bellImageHeight, true);
    		bellGhost = Bitmap.createScaledBitmap(n, bellImageWidth, bellImageHeight, true);
    		l.recycle();
    		g.recycle();
    		n.recycle();
    	}

    	// Need to re-draw.
    	invalidate();
    }


	// ******************************************************************** //
	// Drawing.
	// ******************************************************************** //

	/**
	 * This method is called to ask the widget to draw itself.
	 * 
	 * @param	canvas		Canvas to draw into.
	 */
	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		
		// What watch are we in?
		TimeModel.Watch watchNum = timeModel.getWatch();

		// Calculate the bells to display.  This complicated by the fact
		// that we show the last watch's 8 bells for a minute into the new
		// watch, and of course the dog watches.
		int numBells = timeModel.get(TimeModel.Field.CHIMING);
		int totalBells = watchNum == TimeModel.Watch.DOG1 ? 4 : 8;
		int ghostBells = 0;
    	if (watchNum == TimeModel.Watch.DOG2) {
    		if (numBells == 8)
    			totalBells -= 4;
    		else
    			ghostBells = 4;
    		numBells -= 4;
    	}
    	int grayBells = totalBells - ghostBells - numBells;

		float x = BELL_PAD_X;
		float y = BELL_PAD_Y;
		float px = (bellBoxWidth - bellImageWidth) / 2.0f;
		float py = (bellBoxHeight - bellImageHeight) / 2.0f;
		
		// Draw the left-side ghost bells, if any.  These are bells from
		// the first dog watch, if we're in the last dog watch.
		for (int i = 0; i < ghostBells; ++i) {
			canvas.drawBitmap(bellGhost, x + px, y + py, null);
			x += bellBoxWidth;
		}

		// Draw in all the full bells.
		for (int i = 0; i < numBells; ++i) {
			canvas.drawBitmap(bellLight, x + px, y + py, null);
			x += bellBoxWidth;
		}

		// Draw in the remaining bells as gray.
		for (int i = 0; i < grayBells; ++i) {
			canvas.drawBitmap(bellGray, x + px, y + py, null);
			x += bellBoxWidth;
		}
		
		// Draw the right-side ghost bells, if any.  These are bells for
		// the last dog watch, if we're in the first dog watch.
		for (int i = totalBells; i < 8; ++i) {
			canvas.drawBitmap(bellGhost, x + px, y + py, null);
			x += bellBoxWidth;
		}
	}


	// ******************************************************************** //
	// Class Data.
	// ******************************************************************** //

    // Debugging tag.
	@SuppressWarnings("unused")
	private static final String TAG = "onwatch";

	// Maximum size of the bell images.
	private static final int BELL_SIZE = 32;

	// Padding to leave on either side of the bell images.
	private static final int BELL_PAD_X = 0;
	private static final int BELL_PAD_Y = 2;


	// ******************************************************************** //
	// Private Data.
	// ******************************************************************** //

	// Parent app we're running in.
	private Context appContext;
	
	// Watch calendar, which does all our date/time calculations.
	private TimeModel timeModel;

	// Size of the bell image assets.
	private int bellImageWidth = 0;
	private int bellImageHeight = 0;

	// Size of the bell image on screen.
	private int bellBoxWidth = 0;
	private int bellBoxHeight = 0;
	
	// The bitmaps for the bell states.
	private Bitmap bellGray;
	private Bitmap bellLight;
	private Bitmap bellGhost;

}

