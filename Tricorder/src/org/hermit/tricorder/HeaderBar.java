
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

import java.util.Calendar;

import org.hermit.tricorder.TricorderView.ViewDefinition;
import org.hermit.utils.CharFormatter;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.os.Handler;
import android.view.View;


/**
 * The tricorder header bar, consisting of the corner swoop and the top
 * coloured bar.
 */
class HeaderBar
	extends View
{

	// ******************************************************************** //
	// Public Types.
	// ******************************************************************** //
	
	/**
	 * Set up this view.
	 * 
	 * @param	parent			Parent application context.
	 * @param	nWidth			Width of the navigation bar.  The swoopy corner
	 * 							we draw here has to match this.
	 * @param	tHeight			Height of the top title bar.  The swoopy corner
	 * 							we draw here has to match this.
	 */
	HeaderBar(Tricorder parent, int nWidth, int tHeight) {
		super(parent);
		
		navWidth = nWidth;
		titleHeight = tHeight;
		
		calendar = Calendar.getInstance();
        
        dateBuf = new char[8];
        timeBuf = new char[5];
		
    	setBackgroundColor(Tricorder.COL_BG);
		barPaint = new Paint();
	}

			   
    // ******************************************************************** //
	// Geometry Management.
	// ******************************************************************** //

    /**
     * This is called during layout when the size of this view has
     * changed.  This is where we first discover our window size, so set
     * our geometry to match.
     * 
     * @param	w				Current width of this view.
     * @param	h				Current height of this view.
     * @param	oldw			Old width of this view.  0 if we were
     * 							just added.
     * @param	oldh			Old height of this view.   0 if we were
     * 							just added.
     */
	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		barPath = new Path();
		barPath.moveTo(0, h);
		barPath.lineTo(navWidth, h);
		barPath.lineTo(navWidth, titleHeight);
		barPath.lineTo(w, titleHeight);
		barPath.lineTo(w, 0);
		barPath.lineTo(0, 0);
		barPath.close();
		
		// Position the text, and set up the painter for it.
		textSize = (float) titleHeight * 0.55f;
		barPaint.setTextSize(textSize);
		barPaint.setTypeface(Typeface.DEFAULT_BOLD);
        barPaint.setAntiAlias(true);
		text1X = w - barPaint.measureText("8888.888") - 3;
		text1Y = (float) titleHeight * 0.8f;
		text2X = navWidth - barPaint.measureText("88.88") - 3;
		text2Y = (float) titleHeight * 1.4f;
	
		// Get the stardate.
		updateStardate();
	}

	
	// ******************************************************************** //
	// Data Management.
	// ******************************************************************** //

    /**
     * Select and display the given view.
     * 
     * @param	viewDef			View definition of the view to show.
     */
    void selectDataView(ViewDefinition viewDef) {
    	barColor = viewDef.bgColor;
		invalidate();
    }


	// ******************************************************************** //
	// Stardate Handling.
	// ******************************************************************** //

    /**
     * Update the stored stardate in dateBuf[].  Schedule the update to
     * repeat as long as this view is visible.
     * 
     * We use the stardate notation in ST XI (there being no other consistent
     * convention).  This is YYYY.DDD, DDD being the Julian day number.
     * See http://en.wikipedia.org/wiki/Stardate.
     */
    private void updateStardate() {
        calendar.setTimeInMillis(System.currentTimeMillis());
        CharFormatter.formatInt(dateBuf, 0,
                                calendar.get(Calendar.YEAR),
                                4, false, true);
        dateBuf[4] = '.';
        CharFormatter.formatIntLeft(dateBuf, 5,
                                    calendar.get(Calendar.DAY_OF_YEAR),
                                    3, false);

        CharFormatter.formatInt(timeBuf, 0,
                                calendar.get(Calendar.HOUR_OF_DAY),
                                2, false, true);
        timeBuf[2] = '.';
        CharFormatter.formatInt(timeBuf, 3,
                                calendar.get(Calendar.MINUTE),
                                2, false, true);

        // Re-draw the widget.
        postInvalidate();
        
        // Schedule this to run again, if we're still visible.
        postHandler.removeCallbacks(stardateRunner);
        if (this.isShown()) {
            long minMs = calendar.get(Calendar.SECOND) * 1000 +
                                        calendar.get(Calendar.MILLISECOND);
            postHandler.postDelayed(stardateRunner, 60000 - minMs);
        }
    }
    

    private final Handler postHandler = new Handler();
    
    private final Runnable stardateRunner = new Runnable() {
        public void run() {
            updateStardate();
        }
    };
    
    
    // ******************************************************************** //
    // View Drawing.
    // ******************************************************************** //
    
	/**
	 * This method is called to ask the view to draw itself.
	 * 
	 * @param	canvas		Canvas to draw into.
	 */
	@Override
	protected void onDraw(Canvas canvas) {
	    if (barPath == null)
	        return;
	    
		// Drawing the bar is easy -- just draw the path.
		barPaint.setColor(barColor);
		barPaint.setStyle(Paint.Style.FILL);
		canvas.drawPath(barPath, barPaint);

        // And draw the stardate in.
        barPaint.setColor(0xff000000);
		canvas.drawText(dateBuf, 0, 8, text1X, text1Y, barPaint);
        canvas.drawText(timeBuf, 0, 5, text2X, text2Y, barPaint);
	}

		
	/**
	 * Called when the window containing has change its visibility
	 * (between GONE, INVISIBLE, and VISIBLE).
	 * 
	 * @param  vis         The new visibility of the window. 
	 */
	@Override
    protected void onWindowVisibilityChanged(int vis) {
	    if (vis == View.VISIBLE)
	        updateStardate();
	    else
            postHandler.removeCallbacks(stardateRunner);
	}
	
	
    // ******************************************************************** //
    // Class Data.
    // ******************************************************************** //

    // Debugging tag.
	@SuppressWarnings("unused")
	private static final String TAG = "tricorder";

	
	// ******************************************************************** //
	// Private Data.
	// ******************************************************************** //
	
	// Calendar used for stardates.
	private final Calendar calendar;
	
	// Width of the navigation bar, and the height of the top title bar.
	// The swoopy corner we draw here has to blend into both of these.
	private int navWidth;
	private int titleHeight;
	 
	// Rectangles defining the bounds of the outer and inner curves
	// of the swoop.
	private RectF outerCurve;
	private RectF innerCurve;

	// Path defining the shape of the bar.
	private Path barPath = null;

	// Paint for drawing the bar.
	private Paint barPaint;

	// Current color for the bar.
	private int barColor;
	
	// Text position and size.
	private float text1X = 0;
	private float text1Y = 0;
    private float text2X = 0;
    private float text2Y = 0;
	private float textSize = 0f;
	
	// Char buffers in which we build the stardate and time.
	private final char[] dateBuf;
    private final char[] timeBuf;

}

