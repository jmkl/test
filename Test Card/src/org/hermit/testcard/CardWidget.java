
/**
 * TestCard: a screen test card for Android.
 * <br>Copyright 2010 Ian Cameron Smith
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


package org.hermit.testcard;


import java.util.ArrayList;

import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.View;


/**
 * This widget displays the actual test card.
 */
public class CardWidget
	extends View
{

	// ******************************************************************** //
    // Constructors.
    // ******************************************************************** //
	
	/**
	 * Create a test card.
	 * 
	 * @param	context			Parent application.
	 */
	public CardWidget(Activity context) {
		super(context);
		init(context);
	}


	/**
	 * Create a crew watch schedule display.
	 * 
	 * @param	context			Parent application.
	 * @param	attrs			Layout attributes.
	 */
	public CardWidget(Context context, AttributeSet attrs) {
		super(context, attrs);
		init((Activity) context);
	}

	
	private void init(Activity context) {
        // Get the font scale based on the display's resolution.
        DisplayMetrics metrics = new DisplayMetrics();
        context.getWindowManager().getDefaultDisplay().getMetrics(metrics);
        float fontScale = metrics.scaledDensity;

        textSize = 16f * fontScale;
	    
        textPaint = new Paint();
        textPaint.setAntiAlias(true);
        textPaint.setTypeface(Typeface.DEFAULT);
        textPaint.setTextSize(textSize);
        textPaint.setColor(0xffffffff);
        
        // Set up the pattern painter.  We absolutely do NOT want anti-
        // alias here.
		cardPaint = new Paint();
		cardPaint.setAntiAlias(false);
		
		textBase = textPaint.ascent();
		
		testPatterns = new ArrayList<Pattern>();
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
    	winWidth = width;
    	winHeight = height;
    	
    	// Add all our patterns.
    	int y = PAD;
    	
        for (int i = 0; i < testColours.length; ++i) {
    	    Pattern p = new PointPattern(testNames[i], PAD, y, testColours[i]);
    	    testPatterns.add(p);
    	    y += p.getHeight();
    	}
    	y += PAD;
    	
        for (int i = 0; i < testColours.length; ++i) {
            Pattern p = new LinePattern(testNames[i], PAD, y, testColours[i]);
            testPatterns.add(p);
            y += p.getHeight();
        }
        y += PAD;
        
        for (int i = 0; i < testCombos.length; ++i) {
            Pattern p = new GridPattern(comboNames[i], PAD, y, testCombos[i]);
            testPatterns.add(p);
            y += p.getHeight();
        }
        y += PAD;
        
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

	    if (winWidth <= 0 || winHeight <= 0)
	        return;
        
        // Draw all our patterns.
        for (Pattern p : testPatterns)
            p.draw(canvas);
	}


	// ******************************************************************** //
	// Private Classes.
	// ******************************************************************** //

    private abstract class Pattern {
        Pattern(String lab, int x, int y, int col) {
            this.label = lab;
            this.baseX = x;
            this.baseY = y;
            this.patternX = x + 80;
            this.patternY = y;
            this.paint = new Paint();
            this.paint.setAntiAlias(false);
            this.paint.setColor(col);
        }
        abstract int getWidth();
        abstract int getHeight();
        void draw(Canvas canvas) {
            canvas.drawText(label, baseX, baseY - textBase, textPaint);
        }
        final String label;
        final int baseX;
        final int baseY;
        final int patternX;
        final int patternY;
        final Paint paint;
    }
    

    private final class PointPattern extends Pattern {
        PointPattern(String lab, int x, int y, int col) {
            super(lab, x, y, col);
        }
        @Override
        int getWidth() {
            return Math.round((NUM_POINTS / 2f + 4) * (NUM_POINTS - 1) + 1);
        }
        @Override
        int getHeight() {
            return Math.round(textSize);
        }
       
        @Override
        void draw(Canvas canvas) {
            super.draw(canvas);
            int x = patternX;
            int y = patternY + (int) (textSize / 2);
            for (int i = 0; i < NUM_POINTS; ++i) {
                canvas.drawPoint(x, y, paint);
                x += i + 4;
            }
        }
        
        private static final int NUM_POINTS = 11;
    }
    

    private final class LinePattern extends Pattern {
        LinePattern(String lab, int x, int y, int col) {
            super(lab, x, y, col);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(1f);
        }
        @Override
        int getWidth() {
            float perA = NUM_LINES / 2f + 4 + textSize;
            float perB = NUM_LINES / 2f + 5;
            return Math.round((perA + perB) * (NUM_LINES - 1) + 1);
        }
        @Override
        int getHeight() {
            return Math.round(textSize);
        }
       
        @Override
        void draw(Canvas canvas) {
            super.draw(canvas);
            int x = patternX;
            int y = patternY;
            for (int i = 0; i < NUM_LINES; ++i) {
                canvas.drawLine(x, y + i, x + textSize, y + i, paint);
                x += i + 4 + textSize;
                canvas.drawLine(x, y, x, y + (int) textSize, paint);
                x += i + 5;
            }
        }
        
        private static final int NUM_LINES = 3;
    }
    

    private final class GridPattern extends Pattern {
        GridPattern(String lab, int x, int y, int[] cols) {
            super(lab, x, y, cols[0]);
            gridCols = cols;
            numCols = gridCols.length;
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(1f);
        }
        @Override
        int getWidth() {
            float perA = NUM_GRIDS / 2f + 4 + GRID_DIM;
            return Math.round(perA * (NUM_GRIDS - 1) + 1);
        }
        @Override
        int getHeight() {
            return GRID_DIM;
        }
       
        @Override
        void draw(Canvas canvas) {
            super.draw(canvas);
            int x = patternX;
            int y = patternY;
            for (int i = 0; i < NUM_GRIDS; ++i) {
                drawGrid(canvas, x, y);
                x += i + 4 + GRID_DIM;
            }
        }
        private void drawGrid(Canvas canvas, int sx, int sy) {
            for (int y = 0; y < GRID_DIM; ++y) {
                for (int x = 0; x < GRID_DIM; ++x) {
                    paint.setColor(gridCols[(y * GRID_DIM + x) % numCols]);
                    canvas.drawPoint(sx + x, sy + y, paint);
                }
            }
        }
        
        private static final int GRID_DIM = 29;
        private static final int NUM_GRIDS = 3;
        private final int[] gridCols;
        private final int numCols;
    }
    

    // ******************************************************************** //
    // Class Data.
    // ******************************************************************** //

    // Debugging tag.
	@SuppressWarnings("unused")
	private static final String TAG = "onwatch";

	// Padding to leave around items.
	private static final int PAD = 10;

	// List of all colours we will draw for each pattern.
	private int[] testColours = {
	    Color.WHITE, Color.RED, Color.GREEN, Color.BLUE,
	    Color.YELLOW, Color.CYAN, Color.MAGENTA
	};
    private String[] testNames = {
            "W", "R", "G", "B",
            "Y", "C", "M",
        };

    // List of colour combos we will draw for multi-colour patterns.
    private int[][] testCombos = {
        { Color.WHITE, Color.BLACK, },
        { Color.RED, Color.GREEN, Color.BLUE, },
        { Color.RED, Color.GREEN, },
        { Color.RED, Color.BLUE, },
        { Color.GREEN, Color.BLUE, },
        { Color.YELLOW, Color.CYAN, Color.MAGENTA, },
        { Color.WHITE, Color.RED, Color.GREEN, Color.BLUE, },
        { Color.WHITE, Color.YELLOW, Color.CYAN, Color.MAGENTA, },
    };
    private String[] comboNames = {
        "WK", "RGB", "RG", "RB", "GB",
        "YCM", "WRGB", "WYCM",
    };


	// ******************************************************************** //
	// Private Data.
	// ******************************************************************** //

	// Our window width and height.
	private int winWidth = 0;
	private int winHeight = 0;

	// Paints used for text and graphics.
    private Paint textPaint;
	private Paint cardPaint;
	
	// Size for text labels.
	private float textSize;
	
    // Baseline position for text, relative to the top of its area.
    private float textBase = 0;
    
    // List of all the patterns we have on screen
    private ArrayList<Pattern> testPatterns;

}

