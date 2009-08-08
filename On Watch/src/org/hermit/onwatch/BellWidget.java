
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
import android.graphics.Paint;
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
	 * @param	bar				True to draw a progress bar behind the bells.
	 */
	public BellWidget(Context context, boolean bar) {
		super(context);
		drawBar = bar;
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
		timeModel.listen(TimeModel.Field.MINUTE, new TimeModel.Listener() {
			@Override
			public void change(Field field, long time, int value) {
				invalidate();
			}
		});

		bellPaint = new Paint();
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
		// Our height is half the specified width.
	    int wd = getDefaultSize(getSuggestedMinimumWidth(), wspec);
		int bell = wd / 8 - BELL_PAD_X * 2;
		setMeasuredDimension(wd, bell + BELL_PAD_Y * 2);
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
    	winWidth = width;
    	winHeight = height;

    	bellWidth = width / 8 - BELL_PAD_X * 2;
    	bellHeight = bellWidth;

    	// Get the bell images, scaled to the screen.
    	Resources res = appContext.getResources();
    	Bitmap l = BitmapFactory.decodeResource(res, R.drawable.bell);
    	bellLight = Bitmap.createScaledBitmap(l, bellWidth, bellHeight, true);
    	Bitmap g = BitmapFactory.decodeResource(res, R.drawable.bell_gray);
    	bellGray = Bitmap.createScaledBitmap(g, bellWidth, bellHeight, true);
    	
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
		
		// Draw in the progress bar for the minutes of the watch.
		if (drawBar) {
			float bl = watchNum == TimeModel.Watch.DOG2 ? winWidth / 2f : 0f;
			float minutes = timeModel.get(TimeModel.Field.WATCHMIN);
			float br = bl + winWidth * minutes / 240f;
			bellPaint.setColor(BAR_COLOR);
			bellPaint.setStyle(Paint.Style.FILL);
			canvas.drawRect(bl, 0f, br, winHeight, bellPaint);
		}

		// Calculate the bells to display.  This complicated by the fact
		// that we show the last watch's 8 bells for a minute into the new
		// watch, and of course the dog watches.
		int numBells = timeModel.get(TimeModel.Field.CHIMING);
		int totalBells = watchNum == TimeModel.Watch.DOG1 ? 4 : 8;
		boolean ghostBells = false;
    	if (watchNum == TimeModel.Watch.DOG2) {
    		if (numBells == 8)
    			totalBells -= 4;
    		else
    			ghostBells = true;
    		numBells -= 4;
    	}
    	int grayBells = totalBells - (ghostBells ? 4 : 0) - numBells;

		float x = 0;
		float y = 0;

		// Skip the ghost bells.  These are bells from the first
		// dog watch, if we're in the last dog watch.
		if (ghostBells)
			x += winWidth / 2f;

		// Draw in all the full bells.
		for (int i = 0; i < numBells; ++i) {
			canvas.drawBitmap(bellLight, x + BELL_PAD_X, y + BELL_PAD_Y, null);
			x += winWidth / 8f;
		}

		// Draw in the remaining bells as gray.
		for (int i = 0; i < grayBells; ++i) {
			canvas.drawBitmap(bellGray, x + BELL_PAD_X, y + BELL_PAD_Y, null);
			x += winWidth / 8f;
		}
	}


	// ******************************************************************** //
	// Class Data.
	// ******************************************************************** //

    // Debugging tag.
	@SuppressWarnings("unused")
	private static final String TAG = "onwatch";

	// Colour to draw the watch progress bar.
	private static final int BAR_COLOR = 0xff008000;

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
	
	// Do we draw a progress bar?
	private boolean drawBar = false;
	
	// Our window width and height.
	private int winWidth = 0;
	private int winHeight = 0;

	// Size of the bell image on screen.
	private int bellWidth = 0;
	private int bellHeight = 0;
	
	// The bitmaps for the bell states.
	private Bitmap bellGray;
	private Bitmap bellLight;

	// Paint used for graphics.
	private Paint bellPaint;

}

