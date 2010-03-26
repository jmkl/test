
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
import android.view.Display;
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
        // Get the physical display size to show to the user.
	    Display display = context.getWindowManager().getDefaultDisplay();
	    dispWidth = display.getWidth();
	    dispHeight = display.getHeight();
	    
	    // This info appears to be junk.
//	    dispFormat = pixelFormatName(display.getPixelFormat());

        // Get the font scale based on the display's resolution.
        DisplayMetrics metrics = new DisplayMetrics();
        display.getMetrics(metrics);
        float fontScale = metrics.scaledDensity;

        textSize = 16f * fontScale;
        blockSize = (int) textSize;
	    
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
        labelWidth = (int) textPaint.measureText("MMMM");
		
		testPatterns = new ArrayList<Pattern>();
	}

	
//	private String pixelFormatName(int fmt) {
//	    switch (fmt) {
//	    case PixelFormat.L_8:
//	        return "L:8";
//	    case PixelFormat.RGB_332:
//	        return "RGB:332";
//	    case PixelFormat.RGB_565:
//	        return "RGB:565";
//	    case PixelFormat.RGB_888:
//	        return "RGB:888";
//	    default:
//	        return "Unk:" + fmt;
//	    }
//	}

	
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
    	
    	// Create the display info label.  Pixel format would be great, but
    	// the value from the system looks like junk.
    	String dispInfo = "" + dispWidth + "x" + dispHeight;
        Label info = new Label(dispInfo, PAD, PAD);
        testPatterns.add(info);
        y += textSize + PAD;

        for (int i = 0; i < testColours.length; ++i) {
            int x = PAD;
            
            Label l = new Label(testNames[i], x, y);
            testPatterns.add(l);
            x += labelWidth + PAD;
            
    	    Pattern p = new PointPattern(x, y, testColours[i]);
    	    testPatterns.add(p);
    	    x += p.getWidth() + PAD;
    	    
            p = new LinePattern(x, y, testColours[i]);
            testPatterns.add(p);
    
    	    y += p.getHeight();
    	}
        
    	y += PAD;
    	
        for (int i = 0; i < testCombos.length; ++i) {
            int x = PAD;
            
            Label l = new Label(comboNames[i], x, y);
            testPatterns.add(l);
            x += labelWidth + PAD;
            
            Pattern p = new BarPattern(x, y, testCombos[i]);
            testPatterns.add(p);
            x += p.getWidth() + PAD;
        
            p = new GridPattern(x, y, testCombos[i]);
            testPatterns.add(p);
            
            y += p.getHeight();
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
        Pattern(int x, int y, int col) {
            this.patternX = x;
            this.patternY = y;
            this.paint = new Paint();
            this.paint.setAntiAlias(false);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(1f);
            this.paint.setColor(col);
        }
        int getWidth() {
            float perA = NUM_BLOCKS / 2f + 4;
            return Math.round(perA * (NUM_BLOCKS - 1) + blockSize * NUM_BLOCKS);
        }
        int getHeight() {
            return blockSize;
        }
        void draw(Canvas canvas) {
            int x = patternX;
            int y = patternY;
            for (int i = 0; i < NUM_BLOCKS; ++i) {
                drawBlock(canvas, x, y, i);
                x += i + 4 + blockSize;
            }
        }
        void drawBlock(Canvas canvas, int sx, int sy, int index) { }

        final int patternX;
        final int patternY;
        final Paint paint;
    }
    

    private final class Label extends Pattern {
        Label(String lab, int x, int y) {
            super(x, y, 0xffffffff);
            this.label = lab;
        }
        @Override
        int getWidth() {
            return labelWidth;
        }
        @Override
        int getHeight() {
            return blockSize;
        }
        @Override
        final void draw(Canvas canvas) {
            canvas.drawText(label, patternX, patternY - textBase, textPaint);
        }
        
        final String label;
    }
    

    private abstract class MultiPattern extends Pattern {
        MultiPattern(int x, int y, int[] cols) {
            super(x, y, cols[0]);
            gridCols = cols;
            numCols = gridCols.length;
        }
        
        final int[] gridCols;
        final int numCols;
    }
    

    private final class PointPattern extends Pattern {
        PointPattern(int x, int y, int col) {
            super(x, y, col);
        }
        
        @Override
        void drawBlock(Canvas canvas, int sx, int sy, int index) {
            for (int y = index % 3; y < blockSize; y = y * 2 + 2) {
                for (int x = 0; x < blockSize; x = x * 2 + 2) {
                    canvas.drawPoint(sx + x, sy + y, paint);
                }
            }
        }
    }
    

    private final class LinePattern extends Pattern {
        LinePattern(int x, int y, int col) {
            super(x, y, col);
        }
        
        @Override
        void drawBlock(Canvas canvas, int sx, int sy, int index) {
            int mid = (int) (blockSize / 2);
            int off = mid + index - NUM_BLOCKS / 2;
            canvas.drawLine(sx, sy + off, sx + blockSize, sy + off, paint);
            canvas.drawLine(sx + off, sy, sx + off, sy + blockSize, paint);
        }
    }
    

    private final class BarPattern extends MultiPattern {
        BarPattern(int x, int y, int[] cols) {
            super(x, y, cols);
        }
        
        @Override
        void drawBlock(Canvas canvas, int sx, int sy, int index) {
            // Draw vertical bars in the top half.
            int half = blockSize / 2;
            for (int x = 0; x < blockSize; ++x) {
                paint.setColor(gridCols[x % numCols]);
                canvas.drawLine(sx + x, sy, sx + x, sy + half, paint);
            }
            
            // Draw horizontal bars in the bottom half.
            for (int y = half; y < blockSize; ++y) {
                paint.setColor(gridCols[(y + index) % numCols]);
                canvas.drawLine(sx, sy + y, sx + blockSize, sy + y, paint);
            }
        }
    }


    private final class GridPattern extends MultiPattern {
        GridPattern(int x, int y, int[] cols) {
            super(x, y, cols);
        }
        
        @Override
        void drawBlock(Canvas canvas, int sx, int sy, int index) {
            for (int y = 0; y < blockSize; ++y) {
                for (int x = 0; x < blockSize; ++x) {
                    paint.setColor(gridCols[(y + x) % numCols]);
                    canvas.drawPoint(sx + x, sy + y, paint);
                }
            }
        }
    }
    

    // ******************************************************************** //
    // Class Data.
    // ******************************************************************** //

    // Debugging tag.
	@SuppressWarnings("unused")
	private static final String TAG = "onwatch";

	// Padding to leave around items.
	private static final int PAD = 10;

    // Number of pattern blocks to draw in each test.
    private static final int NUM_BLOCKS = 3;

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

    // Physical display width and height and pixel format.
    private int dispWidth = 0;
    private int dispHeight = 0;

	// Our window width and height.
	private int winWidth = 0;
	private int winHeight = 0;

	// Paints used for text and graphics.
    private Paint textPaint;
	private Paint cardPaint;
	
	// Size for text labels.
	private float textSize;
    
    // Width allowance for text labels.
    private int labelWidth;
	   
    // Size for pattern blocks.
    private int blockSize;

    // Baseline position for text, relative to the top of its area.
    private float textBase = 0;
    
    // List of all the patterns we have on screen.
    private ArrayList<Pattern> testPatterns;

}

