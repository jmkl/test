
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

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.view.SurfaceHolder;


/**
 * This class displays an element of the UI.  An element is a region
 * within a view, and can display text, etc.
 */
class TextAtom
	extends Element
{

	// ******************************************************************** //
	// Constructor.
	// ******************************************************************** //
	
	/**
	 * Set up this view.
	 * 
	 * @param	context			Parent application context.
     * @param	sh				SurfaceHolder we're drawing in.
	 */
	TextAtom(Tricorder context, SurfaceHolder sh) {
		super(context, sh);
		textSize = context.getBaseTextSize();
	}

	
	/**
	 * Set up this view, and configure the text fields to be displayed in
	 * this element.  This is equivalent to calling setTextFields()
	 * after the basic constructor.
	 * 
	 * We support display of a single field, or a rectangular table
	 * of fields.  The fields are specified by passing in sample text
	 * values to be measured; we then allocate the space automatically.
	 * 
	 * @param	context		Parent application context.
     * @param	sh			SurfaceHolder we're drawing in.
	 * @param	fields		Strings representing the columns to display.
	 * 						Each one should be a sample piece of text
	 * 						which will be measured to determine the
	 * 						required space for each column.
	 * @param	rows		The number of rows to display.
	 */
	TextAtom(Tricorder context, SurfaceHolder sh, String[] fields, int rows) {
		super(context, sh);
		textSize = context.getBaseTextSize();
		setTextFields(fields, rows);
	}


	/**
	 * Set up the paint for this element.  This is called during
	 * initialization.  Subclasses can override this to do class-specific
	 * one-time initialization.
	 * 
	 * @param paint			The paint to initialize.
	 */
	@Override
	protected void initializePaint(Paint paint) {
		paint.setTextScaleX(FONT_SCALEX);
		paint.setTypeface(FONT_FACE);
		paint.setAntiAlias(true);
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
	protected void setGeometry(Rect bounds) {
		super.setGeometry(bounds);

		// Position our text based on our actual geometry.  If setTextFields()
		// hasn't been called this does nothing.
		positionText();
	}


    /**
     * Set the margins around the displayed text.  This the total space
     * between the edges of the element and the outside bounds of the text.
     * 
	 * @param	left		The left margin.
	 * @param	top			The top margin.
	 * @param	right		The right margin.
	 * @param	bottom		The bottom margin.
     */
	void setMargins(int left, int top, int right, int bottom) {
		marginLeft = left;
		marginTop = top;
		marginRight = right;
		marginBottom = bottom;
		
		// Position our text based on these new margins.  If setTextFields()
		// hasn't been called this does nothing.
		positionText();
	}


	/**
	 * Set up the text fields to be displayed in this element.
	 * If this is never called, there will be no text.
	 * 
	 * We support display of a single field, or a rectangular table
	 * of fields.  The fields are specified by passing in sample text
	 * values to be measured; we then allocate the space automatically.
	 * 
	 * This must be called before setText() can be called.
	 * 
	 * @param	fields			Strings representing the columns to display.
	 * 							Each one should be a sample piece of text
	 * 							which will be measured to determine the
	 * 							required space for each column.
	 * @param	rows			The number of rows to display.
	 */
	protected void setTextFields(String[] fields, int rows) {
		fieldTemplate = fields;
		numRows = rows;
		
		// Make the values array.
		textValues = new String[rows][fields.length];
		
		// Position our text based on the template.  If setGeometry()
		// hasn't been called yet, then the positions will not be final,
		// but getTextWidth() and getTextHeight() will return sensible
		// values.
		positionText();
	}
	
	
	/**
	 * Get the minimum width needed to fit all the text.
	 * 
	 * @return			The minimum width needed to fit all the text.
	 * 					Returns zero if setTextFields() hasn't been called.
	 */
	@Override
	int getPreferredWidth() {
		return textWidth;
	}
	

	/**
	 * Get the minimum height needed to fit all the text.
	 * 
	 * @return			The minimum height needed to fit all the text.
	 * 					Returns zero if setTextFields() hasn't been called.
	 */
	@Override
	int getPreferredHeight() {
		return textHeight;
	}


	/**
	 * Position the text based on the current template and geometry.
	 * If If setTextFields() hasn't been called this does nothing.
	 * If setGeometry() hasn't been called yet, then the positions will
	 * not be final, but getTextWidth() and getTextHeight() will return
	 * sensible values.
	 */
	private void positionText() {
		if (fieldTemplate == null)
			return;
		
		final int nf = fieldTemplate.length;
		colsX = new int[nf];
		rowsY = new int[numRows];
		
		Rect bounds = getBounds();
		Paint paint = getPaint();
		paint.setTextSize(textSize);

		// Assign all the column positions based on minimum width.
		int x = bounds.left;
		for (int i = 0; i < nf; ++i) {
			int len = (int) Math.ceil(paint.measureText(fieldTemplate[i]));
			int lp = i > 0 ? textPadLeft : marginLeft;
			int rp = i < nf - 1 ? textPadRight : marginRight;
			colsX[i] = x + lp;
			x += len + lp + rp;
		}
		textWidth = x - bounds.left;
		
		// If we have excess width, distribute it into the inter-column gaps.
		// Don't adjust textWidth because it is the minimum.
		if (nf > 1) {
			int excess = (bounds.right - x) / (nf - 1);
			if (excess > 0) {
				for (int i = 1; i < nf; ++i)
					colsX[i] += excess * i;
			}
		}
		
		// Assign all the row positions based on minimum height.
 	   	Paint.FontMetricsInt fm = paint.getFontMetricsInt();
		int y = bounds.top;
		for (int i = 0; i < numRows; ++i) {
			int tp = i > 0 ? textPadTop : marginTop;
			int bp = i < numRows - 1 ? textPadBottom : marginBottom;
			rowsY[i] = y + tp - fm.ascent - 2;
			y += -fm.ascent - 2 + fm.descent + tp + bp;
		}
		textHeight = y - bounds.top;
	}
	
	
    // ******************************************************************** //
	// Appearance.
	// ******************************************************************** //

	/**
	 * Set the text colour of this element.
	 * 
	 * @param	col			The new text colour, in ARGB format.
	 */
	void setTextColor(int col) {
		plotColour = col;
	}
	

	/**
	 * Get the text colour of this element.
	 * 
	 * @return				The text colour, in ARGB format.
	 */
	int getTextColor() {
		return plotColour;
	}
	

	/**
	 * Set the text size of this element.
	 * 
	 * @param	size		The new text size.
	 */
	void setTextSize(float size) {
		textSize = size;
		
		// Position our text based on the new size.  If setTextFields()
		// hasn't been called this does nothing.
		positionText();
	}
	

	/**
	 * Get the text size of this element.
	 * 
	 * @return				The text size.
	 */
	float getTextSize() {
		return textSize;
	}
	

	// ******************************************************************** //
	// Data Management.
	// ******************************************************************** //

	/**
	 * Set the text values displayed in this view.
	 * 
	 * @param	text			The new text field values.
	 */
	protected void setText(String[][] text) {
		textValues = text;
	}


	/**
	 * Set a specific text value displayed in this view.
	 * 
	 * @param	row				Row of the field to change.
	 * @param	col				Column of the field to change.
	 * @param	text			The new text field value.
	 */
	protected void setText(int row, int col, String text) {
		textValues[row][col] = text;
	}


	// ******************************************************************** //
	// View Drawing.
	// ******************************************************************** //
	
	/**
	 * This method is called to ask the element to draw itself.
	 * 
	 * @param	canvas		Canvas to draw into.
	 * @param	paint		The Paint which was set up in initializePaint().
	 */
	@Override
	protected void drawBody(Canvas canvas, Paint paint) {
		// Set up the display style.
		paint.setColor(plotColour);
		paint.setTextSize(textSize);

		final String[][] tv = textValues;
		
		// If we have any text to show, draw it.
		if (tv != null) {
			for (int row = 0; row < rowsY.length && row < tv.length; ++row) {
				String[] fields = tv[row];
				int y = rowsY[row];
				for (int col = 0; col < colsX.length && col < fields.length; ++col) {
					String field = fields[col];
					if (field == null)
						field = "";
					int x = colsX[col];
					canvas.drawText(field, 0, field.length(), x, y, paint);
				}
			}
		}
	}

	
    // ******************************************************************** //
    // Class Data.
    // ******************************************************************** //

    // Debugging tag.
	@SuppressWarnings("unused")
	private static final String TAG = "tricorder";

	// Horizontal scaling of the font; used to produce a tall, thin font.
	private static final Typeface FONT_FACE = Typeface.MONOSPACE;

	// Horizontal scaling of the font; used to produce a tall, thin font.
	private static final float FONT_SCALEX = 0.6f;
	
	
	// ******************************************************************** //
	// Private Data.
	// ******************************************************************** //
	
	// Template for the text fields we're displaying.
	private String[] fieldTemplate = null;
	private int numRows = 0;
	
	// Horizontal positions of the text columns, and vertical positions
	// of the rows.  These are the actual text base positions.  These
	// will be null if we have no defined text fields.
	private int[] colsX = null;
	private int[] rowsY = null;
	
	// The width and height we would need to display all the text at minimum,
	// including padding and margins.  Set after a call to setTextFields().
	private int textWidth = 0;
	private int textHeight = 0;
	
	// Current values of the displayed text fields.
	private String[][] textValues = null;

	// Current text size.
	private float textSize;

	// Margins.  These are applied around the outside of the text.
	private int marginLeft = 0;
	private int marginRight = 0;
	private int marginTop = 0;
	private int marginBottom = 0;

	// Text padding.  This is applied between all pairs of text fields.
	private int textPadLeft = 2;
	private int textPadRight = 2;
	private int textPadTop = 0;
	private int textPadBottom = 0;
	
}

