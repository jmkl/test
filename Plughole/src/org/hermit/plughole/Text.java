
/**
 * Plughole: a rolling-ball accelerometer game.
 * <br>Copyright 2008-2010 Ian Cameron Smith
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


package org.hermit.plughole;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;


/**
 * Class representing text drawn on the game board.  It doesn't
 * interact with the game in any way, it's just decoration.
 */
class Text
	extends Visual
{
	
	// ******************************************************************** //
	// Constructor.
	// ******************************************************************** //

	/**
	 * Create a decal by applying a transformation to a decal spec.
	 * 
	 * @param	app			Application context.  This provides
	 * 						access to resources and image loading.
	 * @param	id			The ID of this element.
	 * @param	text		The string to display.
	 * @param	size		Size to display the text at; must be
	 * 						transformed using xform.
	 * @param	xform		Transform to apply to the raw data.
	 */
	Text(Plughole app, String id, String text, float size, Matrix xform) {
		super(app, id, null, xform);
		
		textString = text;
		textLevelSize = size;
	}


    // ******************************************************************** //
    // Building.
    // ******************************************************************** //

    /**
     * Set the visual rectangular box of this element.  This is called when
     * we're added to our parent.
     * 
     * @param   rect        The rectangle, in level co-ordinates, suitable for
     *                      attaching Graphics to.
     */
    @Override
    void setRect(RectF rect) {
        Matrix xform = getTransform();
        RectF box = xform.transform(rect);

        // Calculate the actual geometry of the decal.
        bounds = xform.transform(box);
        textSize = textLevelSize * (float) xform.getScale();
        textAngle = xform.getRotation();
        centreX = (bounds.left + bounds.right) / 2;
        centreY = (bounds.top + bounds.bottom) / 2;
        
        // Calculate the vertical text layout parameters based on the
        // general font metrics.
        textPaint.setTextSize(textSize);
        Paint.FontMetricsInt fm = textPaint.getFontMetricsInt();
        textHeight = -fm.ascent + fm.descent;
        textBase = -fm.ascent - textHeight / 2;
    }
    

	// ******************************************************************** //
	// Accessors.
	// ******************************************************************** //

	/**
	 * Get the bounding box of this decal.
	 */
	RectF getBounds() {
		return bounds;
	}
	

	/**
	 * Get the centre point of the text in this decal.
	 */
	Point getCentre() {
		return new Point(centreX, centreY);
	}
	
	
	/**
	 * Set the text of this decal.
	 */
	void setText(String text) {
		textString = text;
	}
	

	// ******************************************************************** //
	// Drawing.
	// ******************************************************************** //

	/**
	 * Draw this graphic onto the given canvas.
	 * 
	 * @param	canvas			Canvas to draw on.
	 * @param	time			Total level time in ms.  A time of zero
	 * 							indicates that we're drawing statically,
	 * 							not in the game loop.
	 * @param	clock			Level time remaining in ms.
	 */
	@Override
	protected void draw(Canvas canvas, long time, long clock) {
		draw(canvas, textString);
	}


	/**
	 * Draw this decal onto the given canvas with the given text.
	 * 
	 * @param	canvas			Canvas to draw on.
	 */
	void draw(Canvas canvas, String text) {
		// Position the text centred within the bounding box.
		textPaint.setTextSize(textSize);
		int hlen = (int) Math.ceil(textPaint.measureText(text) / 2);
		
		float textX = 0, textY = 0;
		switch (textAngle) {
		case NONE:
			textX = centreX - hlen;
			textY = centreY + textBase;
			break;
		case LEFT:
			textX = centreX + textBase;
			textY = centreY + hlen;
			break;
		case RIGHT:
			textX = centreX - textBase;
			textY = centreY - hlen;
			break;
		case FULL:
			textX = centreX + hlen;
			textY = centreY - textBase;
			break;
		}

		canvas.save();
		canvas.rotate(textAngle.degrees, textX, textY);
		textPaint.setColor(0xffffff00);
		textPaint.setStyle(Paint.Style.FILL_AND_STROKE);
		textPaint.setStrokeWidth(2f);
		canvas.drawText(text, textX, textY, textPaint);
		textPaint.setColor(0xff000000);
		textPaint.setStrokeWidth(0.5f);
		canvas.drawText(text, textX, textY, textPaint);
		canvas.restore();
	}


	// ******************************************************************** //
	// Class Data.
	// ******************************************************************** //

    // Debugging tag.
	@SuppressWarnings("unused")
	private static final String TAG = "plughole";
	
	// Paint we use for drawing all text.
	private static final Paint textPaint = new Paint();
	static {
		textPaint.setTypeface(Typeface.SERIF);
		textPaint.setAntiAlias(true);
	}
	
	
	// ******************************************************************** //
	// Private Data.
	// ******************************************************************** //
	
    // Size to display the text at, in level co-ordinates.
    private final float textLevelSize;

	// Actual bounding box of this decal in screen co-ordinates.
	private RectF bounds;

	// The size text to display.
	private String textString;

	// The size of the text to display in screen co-ordinates.
	private float textSize;

	// The angle the text needs to be drawn at.
	private Matrix.ORotate textAngle;
	
	// The position where the centre of the text goes.
	private float centreX;
	private float centreY;
	
	// The overall height of the text according to the font metrics.
	private float textHeight;
	
	// How far down from the centre of the text is the baseline.
	private float textBase;

}

