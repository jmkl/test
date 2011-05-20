
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


import org.hermit.onwatch.CrewModel.Crew;
import org.hermit.onwatch.TimeModel.Field;
import org.hermit.utils.TimeUtils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.GradientDrawable;
import android.util.AttributeSet;
import android.view.View;


/**
 * This custom widget displays the watch schedule in graphical form.
 */
public class ScheduleWidget
	extends View
{

	// ******************************************************************** //
    // Constructors.
    // ******************************************************************** //

	/**
	 * Create a crew watch schedule display.
	 * 
	 * @param	context			Parent application.
	 */
	public ScheduleWidget(Context context) {
		super(context);
		init(context);
	}


	/**
	 * Create a crew watch schedule display.
	 * 
	 * @param	context			Parent application.
	 * @param	attrs			Layout attributes.
	 */
	public ScheduleWidget(Context context, AttributeSet attrs) {
		super(context, attrs);
		init(context);
	}

	
	/**
	 * Set up this widget.
	 * 
	 * @param	context			Parent application.
	 */
	private void init(Context context) {
		appContext = context;
        
        setMinimumHeight(HOUR_HEIGHT * 24 * NUM_DAYS);

		// Get the time model.  Get a callback every minute to update
		// the display.
		timeModel = TimeModel.getInstance(context);
		timeModel.listen(TimeModel.Field.MINUTE, new TimeModel.Listener() {
			@Override
			public void change(Field field, int value, long time) {
				reDrawContent();
			}
		});

        // Get or create the crew model.  Monitor it for changes.
        crewModel = CrewModel.getInstance(context);
		
		// Register for watch crew changes.
		crewModel.listen(new CrewModel.Listener() {
			@Override
			public void watchPlanChanged() {
				reDrawContent();
			}
			@Override
			public void watchChange(int day, int watch, Crew[] crew) {
			}
			@Override
			public void watchAlert(Crew[] nextCrew) {
			}
		});

        // Create the paint and the gradients.
		graphPaint = new Paint();
		cursorGrad = new GradientDrawable(GradientDrawable.Orientation.BOTTOM_TOP,
										  new int[] { CURSOR_COL & 0x80ffffff, 0 });
		cursorGrad.setShape(GradientDrawable.RECTANGLE);

		// Set up our initial content.
		reDrawContent();
	}


    // ******************************************************************** //
	// Geometry.
	// ******************************************************************** //

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

    	dispWidth = width;
    	
		backingBitmap = Bitmap.createBitmap(width, height,
				  							Bitmap.Config.RGB_565);
		backingCanvas = new Canvas(backingBitmap);
		reDrawContent();
	}


	// ******************************************************************** //
	// Control.
	// ******************************************************************** //
	
	/**
	 * Update the displayed data.
	 */
	void update() {
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
		// Just re-draw from our cached bitmap.
		canvas.drawBitmap(backingBitmap, 0, 0, null);
	}
	
	
	/**
	 * This method is called when some data has changed.  Re-draw the
	 * widget to the backing bitmap.
	 */
	private void reDrawContent() {
		if (backingCanvas == null)
			return;
		
		// Get the watch data for NUM_DAYS + 1 days, so we have the partial
    	// day at the end.
		WatchPlan plan = crewModel.getWatchPlan();
		float[] watchTimes = plan.planTimes;
		CrewModel.Crew[][] watchNames = crewModel.getWatchSchedule(NUM_DAYS + 1);

		// Figure out what weekday and hour we're starting from.
		// Round back to the previous watch boundary.
		int firstDay = timeModel.get(TimeModel.Field.WDAY);
		int firstHour = timeModel.get(TimeModel.Field.HOUR);
		firstHour -= firstHour % plan.getBaseLength() + plan.getBaseLength();
		if (firstHour < 0) {
			firstHour += 24;
			if (--firstDay < 1)
				firstDay += 7;
		}
		
		// Draw the background.
		drawBackground(backingCanvas, plan, watchTimes, firstDay, firstHour);
		
		// If we have a watch plan and crew, draw the watch plan, else
		// just put up a message.
		if (watchNames == null) {
			graphPaint.setStyle(Paint.Style.FILL);
			graphPaint.setTextSize(LABEL_SIZE);
			graphPaint.setColor(LABEL_COL);
			String msg = appContext.getString(R.string.msg_no_crew);
			backingCanvas.drawText(msg, WATCH_START_X, WATCH_START_X, graphPaint);
		} else
			drawWatches(backingCanvas, plan, watchTimes, watchNames, firstDay, firstHour);
		
		// Widget needs a redraw now.
		invalidate();
	}

	
	/**
	 * Draw the background to this widget.
	 * 
	 * @param	canvas		Canvas to draw into.
	 */
	private void drawBackground(Canvas canvas, WatchPlan plan, float[] times,
								int firstDay, int firstHour)
	{
		canvas.drawColor(0xff000000);
		
		// Draw in the hours grid.
		graphPaint.setColor(GRID_COL);
		graphPaint.setStyle(Paint.Style.STROKE);
		for (int h = 0; h < 24 * NUM_DAYS; ++h) {
			// Thicker line for traditional watch boundaries.
			int hour = (h + firstHour) % 24;
			int thick = hour % 4 == 0 || hour == 18 ? 3 : 1;
			
			int y = h * HOUR_HEIGHT;
			graphPaint.setStrokeWidth(thick);
			canvas.drawLine(0, y, dispWidth, y, graphPaint);
		}

		// Draw the "now" cursor.
		float now = timeModel.get(TimeModel.Field.HOUR) +
						(float) timeModel.get(TimeModel.Field.MINUTE) / 60.0f;
		int nowY = (int) ((now - firstHour) * HOUR_HEIGHT);
		cursorGrad.setBounds(0, nowY - CURSOR_HEIGHT, dispWidth, nowY);
		cursorGrad.draw(canvas);
		graphPaint.setColor(CURSOR_COL);
		graphPaint.setStyle(Paint.Style.STROKE);
		graphPaint.setStrokeWidth(1);
		canvas.drawLine(0, nowY, dispWidth, nowY, graphPaint);
		
		// Draw in the hour labels.
		int wpd = times.length;
		int nw = (NUM_DAYS + 1) * wpd;
		graphPaint.setStyle(Paint.Style.FILL);
		graphPaint.setTextSize(LABEL_SIZE);
		graphPaint.setColor(LABEL_COL);
		for (int w = 0; w < nw; ++w) {
			int day = w / wpd;
			
			float h1 = times[w % wpd] + day * 24;
			if (h1 < firstHour)
				continue;
			float y1 = (h1 - firstHour) * HOUR_HEIGHT;

			// Draw the day label.
			int dayNum = (day + firstDay - 1) % 7 + 1;
			String dayName = TimeModel.weekdayShortName(dayNum);
			canvas.drawText(dayName, DAY_LABEL_X, y1 + DAY_LABEL_Y, graphPaint);

			// Draw the hour label.
			float lh = h1 % 24;
			String time = TimeUtils.timeMsToHm((long) (lh * 3600000));
			canvas.drawText(time, TIME_LABEL_X, y1 + TIME_LABEL_Y, graphPaint);
		}
	}

	
	/**
	 * Draw in the watch-standers.
	 * 
	 * @param	canvas		Canvas to draw into.
	 */
	private void drawWatches(Canvas canvas, WatchPlan plan,
						     float[] times, CrewModel.Crew[][] watchNames,
						     int firstDay, int firstHour)
	{
		// Get the watch data for NUM_DAYS + 1 days, so we have the partial
    	// day at the end.
		int wpd = times.length;
		int nw = (NUM_DAYS + 1) * wpd;
		
		int cols = watchNames.length;
		float colWidth = (dispWidth - WATCH_START_X) / cols;
	
		// Draw in the watches.  We do NUM_DAYS + 1, because we start
		// part-way through the first day.
		float nameCentre = LABEL_SIZE / 2f - 4f;
		for (int c = 0; c < cols; ++c) {
			float x1 = WATCH_START_X + c * colWidth;
			float x2 = x1 + colWidth - WATCH_MARGIN;
			
			for (int w1 = 0; w1 < nw; ++w1) {
				int w2 = w1 + 1;
				
				CrewModel.Crew crew = watchNames[c][w1 % nw];

				// Figure the day and hour of this and the next watch.
				int day1 = w1 / wpd;
				int day2 = w2 / wpd;
				float h1 = times[w1 % wpd] + day1 * 24;
				if (h1 < firstHour)
					continue;
				float h2 = times[w2 % wpd] + day2 * 24;
				
				// Calculate the position for this watch.
				float y1 = (h1 - firstHour) * HOUR_HEIGHT;
				float y2 = (h2 - firstHour) * HOUR_HEIGHT;
				
				// Colour the watch.  We use the crew colour with the alpha
				// reduced according to which watch column this is.
				int colour = crew.getColour();
				colour -= c * 0x30000000 + 0x80000000;
				graphPaint.setColor(colour);
				graphPaint.setStyle(Paint.Style.FILL);
				canvas.drawRect(x1, y1, x2, y2, graphPaint);

				// And draw the watch-stander's name, if there is one.
				graphPaint.setColor(LABEL_COL);
				graphPaint.setStyle(Paint.Style.FILL);
				graphPaint.setTextSize(LABEL_SIZE);
				float ny = y1 + (y2 - y1) / 2;
				canvas.drawText(crew.name, x1 + 20, ny + nameCentre, graphPaint);
			}
		}
	}


	// ******************************************************************** //
	// Class Data.
	// ******************************************************************** //

    // Debugging tag.
	@SuppressWarnings("unused")
	private static final String TAG = "onwatch";

    // The number of days of the schedule to display in the schedule view.
    private static final int NUM_DAYS = 3;

    // The height in pixels of one hour in the schedule view.
    private static final int HOUR_HEIGHT = 20;

    // Colour to draw the grid.
	private static final int GRID_COL = 0xa0d04000;

    // Colour to draw the cursor.
	private static final int CURSOR_COL = 0xffffff00;

    // The thickness of the cursor.
	private static final int CURSOR_HEIGHT = 20;

    // Colour to draw the labels.
	private static final int LABEL_COL = 0xffffffff;

    // Label text size.
    private static final int LABEL_SIZE = 20;

    // Day and hour label positions.
    private static final float DAY_LABEL_X = 6;
    private static final float DAY_LABEL_Y = LABEL_SIZE - 4f;
    private static final float TIME_LABEL_X = 51;
    private static final float TIME_LABEL_Y = LABEL_SIZE - 4f;

    // Watch display positions.
    private static final float WATCH_START_X = 110;
    private static final float WATCH_MARGIN = 6;


	// ******************************************************************** //
	// Private Data.
	// ******************************************************************** //

	// Parent app we're running in.
	private Context appContext;

	// Time model, which does all our date/time calculations.
	private TimeModel timeModel;

    // The crew data model.
    private CrewModel crewModel = null;

	// Size of the display.
	private int dispWidth = 0;
	
	// Bitmap we draw the widget into, and the Canvas we draw with.
	private Bitmap backingBitmap;
	private Canvas backingCanvas;

	// Paint used for graphics.
	private Paint graphPaint;
	
	// Gradient used for drawing the "now" cursor.
	private GradientDrawable cursorGrad;

}

