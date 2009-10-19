
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


package org.hermit.watchaids;


import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;


/**
 * A widget which displays a signal flag along with its letter, meaning
 * and representation in morse code.
 */
public class FlagWidget
	extends LinearLayout
{

	// ******************************************************************** //
    // Public Constants.
    // ******************************************************************** //

    /**
     * Definitions of all the flags.
     */
    public enum Flag {
        /** Letter flag A */
        FLAG_A(R.string.flag_a, R.drawable.leta, R.string.morse_a, R.string.code_a),
        /** Letter flag B */
        FLAG_B(R.string.flag_b, R.drawable.letb, R.string.morse_b, R.string.code_b),
        /** Letter flag C */
        FLAG_C(R.string.flag_c, R.drawable.letc, R.string.morse_c, R.string.code_c),
        /** Letter flag D */
        FLAG_D(R.string.flag_d, R.drawable.letd, R.string.morse_d, R.string.code_d),
        /** Letter flag E */
        FLAG_E(R.string.flag_e, R.drawable.lete, R.string.morse_e, R.string.code_e),
        /** Letter flag F */
        FLAG_F(R.string.flag_f, R.drawable.letf, R.string.morse_f, R.string.code_f),
        /** Letter flag G */
        FLAG_G(R.string.flag_g, R.drawable.letg, R.string.morse_g, R.string.code_g),
        /** Letter flag H */
        FLAG_H(R.string.flag_h, R.drawable.leth, R.string.morse_h, R.string.code_h),
        /** Letter flag I */
        FLAG_I(R.string.flag_i, R.drawable.leti, R.string.morse_i, R.string.code_i),
        /** Letter flag J */
        FLAG_J(R.string.flag_j, R.drawable.letj, R.string.morse_j, R.string.code_j),
        /** Letter flag K */
        FLAG_K(R.string.flag_k, R.drawable.letk, R.string.morse_k, R.string.code_k),
        /** Letter flag L */
        FLAG_L(R.string.flag_l, R.drawable.letl, R.string.morse_l, R.string.code_l),
        /** Letter flag M */
        FLAG_M(R.string.flag_m, R.drawable.letm, R.string.morse_m, R.string.code_m),
        /** Letter flag N */
        FLAG_N(R.string.flag_n, R.drawable.letn, R.string.morse_n, R.string.code_n),
        /** Letter flag O */
        FLAG_O(R.string.flag_o, R.drawable.leto, R.string.morse_o, R.string.code_o),
        /** Letter flag P */
        FLAG_P(R.string.flag_p, R.drawable.letp, R.string.morse_p, R.string.code_p),
        /** Letter flag Q */
        FLAG_Q(R.string.flag_q, R.drawable.letq, R.string.morse_q, R.string.code_q),
        /** Letter flag R */
        FLAG_R(R.string.flag_r, R.drawable.letr, R.string.morse_r, R.string.code_r),
        /** Letter flag S */
        FLAG_S(R.string.flag_s, R.drawable.lets, R.string.morse_s, R.string.code_s),
        /** Letter flag T */
        FLAG_T(R.string.flag_t, R.drawable.lett, R.string.morse_t, R.string.code_t),
        /** Letter flag U */
        FLAG_U(R.string.flag_u, R.drawable.letu, R.string.morse_u, R.string.code_u),
        /** Letter flag V */
        FLAG_V(R.string.flag_v, R.drawable.letv, R.string.morse_v, R.string.code_v),
        /** Letter flag W */
        FLAG_W(R.string.flag_w, R.drawable.letw, R.string.morse_w, R.string.code_w),
        /** Letter flag Z */
        FLAG_X(R.string.flag_x, R.drawable.letx, R.string.morse_x, R.string.code_x),
        /** Letter flag Y */
        FLAG_Y(R.string.flag_y, R.drawable.lety, R.string.morse_y, R.string.code_y),
        /** Letter flag Z */
        FLAG_Z(R.string.flag_z, R.drawable.letz, R.string.morse_z, R.string.code_z),
        
        /** Number flag 0 */
        FLAG_0(R.string.flag_0, R.drawable.num0, R.string.morse_0, 0),
        /** Number flag 1 */
        FLAG_1(R.string.flag_1, R.drawable.num1, R.string.morse_1, 0),
        /** Number flag 2 */
        FLAG_2(R.string.flag_2, R.drawable.num2, R.string.morse_2, 0),
        /** Number flag 3 */
        FLAG_3(R.string.flag_3, R.drawable.num3, R.string.morse_3, 0),
        /** Number flag 4 */
        FLAG_4(R.string.flag_4, R.drawable.num4, R.string.morse_4, 0),
        /** Number flag 5 */
        FLAG_5(R.string.flag_5, R.drawable.num5, R.string.morse_5, 0),
        /** Number flag 6 */
        FLAG_6(R.string.flag_6, R.drawable.num6, R.string.morse_6, 0),
        /** Number flag 7 */
        FLAG_7(R.string.flag_7, R.drawable.num7, R.string.morse_7, 0),
        /** Number flag 8 */
        FLAG_8(R.string.flag_8, R.drawable.num8, R.string.morse_8, 0),
        /** Number flag 9 */
        FLAG_9(R.string.flag_9, R.drawable.num9, R.string.morse_9, 0),
        
        /** First substitute */
        FLAG_SUB1(R.string.flag_s1, R.drawable.sub1, 0, 0),
        /** Second substitute */
        FLAG_SUB2(R.string.flag_s2, R.drawable.sub2, 0, 0),
        /** Third substitute */
        FLAG_SUB3(R.string.flag_s3, R.drawable.sub3, 0, 0),
 
        /** Code / answer pennant */
        FLAG_CODE(R.string.flag_code, R.drawable.code, 0, 0);
 
        Flag(int name, int img, int morse, int meaning) {
            nameId = name;
            imgId = img;
            morseId = morse;
            textId = meaning;
        }
        
        private int nameId;
        private int imgId;
        private int morseId;
        private int textId;
    }
    
    
    // ******************************************************************** //
    // Constructors.
    // ******************************************************************** //
    
	/**
	 * Create a flag display widget.
	 * 
	 * @param	context			Parent application.
     * @param   flag            Which flag to display.
	 */
	public FlagWidget(Context context, Flag flag) {
		super(context);
		init(context, flag);
	}


	/**
	 * Create a flag display widget from an XML spec.
	 * 
	 * @param	context			Parent application.
	 * @param	attrs			Layout attributes.
	 */
	public FlagWidget(Context context, AttributeSet attrs) {
		super(context, attrs);

        TypedArray a = context.obtainStyledAttributes(attrs,
                                                      R.styleable.FlagWidget);

        CharSequence name = a.getString(R.styleable.FlagWidget_name);
        if (name == null)
            throw new IllegalArgumentException("FlagWidget requires a \"name\" parameter");
        Flag flag = Flag.valueOf(name.toString());
        if (flag == null)
            throw new IllegalArgumentException("FlagWidget parameter \"" +
                                               name + "\" is not a valid flag name");
        init(context, flag);
	}

	
    /**
     * Set up this flag display widget.
     * 
     * @param   context         Parent application.
     * @param   flag            Which flag to display.
     */
	private void init(Context context, Flag flag) {
		setOrientation(HORIZONTAL);
		setPadding(4, 4, 4, 4);
		LinearLayout.LayoutParams lp;
		final int WC = LinearLayout.LayoutParams.WRAP_CONTENT;
        final int FP = LinearLayout.LayoutParams.FILL_PARENT;
		
		ImageView flagView = new ImageView(context);
		flagView.setImageResource(flag.imgId);
		lp = new LinearLayout.LayoutParams(WC, WC);
		addView(flagView, lp);
		
		View textView = createText(context, flag);
        lp = new LinearLayout.LayoutParams(FP, WC);
        addView(textView, lp);
	}


    /**
     * Create the text view.
     * 
     * @param   context         Parent application.
     * @param   flag            Which flag to display.
     */
    private View createText(Context context, Flag flag) {
        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(VERTICAL);
        LinearLayout.LayoutParams lp;
        final int WC = LinearLayout.LayoutParams.WRAP_CONTENT;
        final int FP = LinearLayout.LayoutParams.FILL_PARENT;
        
        View nameView = createName(context, flag);
        lp = new LinearLayout.LayoutParams(FP, WC);
        layout.addView(nameView, lp);
  
        if (flag.textId != 0) {
            TextView letterView = new TextView(context);
            letterView.setText(flag.textId);
            lp = new LinearLayout.LayoutParams(FP, WC);
            layout.addView(letterView, lp);
        }
        
        return layout;
    }


    /**
     * Create the name and morse text view.
     * 
     * @param   context         Parent application.
     * @param   flag            Which flag to display.
     */
    private View createName(Context context, Flag flag) {
        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(HORIZONTAL);
        LinearLayout.LayoutParams lp;
        final int WC = LinearLayout.LayoutParams.WRAP_CONTENT;
        final int FP = LinearLayout.LayoutParams.FILL_PARENT;
        
        TextView nameView = new TextView(context);
        nameView.setText(flag.nameId);
        lp = new LinearLayout.LayoutParams(FP, WC, 1);
        layout.addView(nameView, lp);
        
        TextView morseView = new TextView(context);
        morseView.setTypeface(Typeface.DEFAULT_BOLD);
        if (flag.morseId != 0)
            morseView.setText(flag.morseId);
        lp = new LinearLayout.LayoutParams(FP, WC, 1);
        layout.addView(morseView, lp);
        
        return layout;
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
    	
    	// Re-draw.
		invalidate();
    }


	// ******************************************************************** //
	// Configuration.
	// ******************************************************************** //

	/**
	 * Set the bar position.
	 * 
	 * @param	frac			Fraction of the widget width to draw the
	 * 							bar at.
	 */
	public void setBar(float frac) {
		barFraction = frac;
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
		// Draw in the progress bar.
		float bl = 0f;
		float br = bl + winWidth * barFraction;
		barPaint.setColor(BAR_COLOR);
		barPaint.setStyle(Paint.Style.FILL);
		canvas.drawRect(bl, 0f, br, winHeight, barPaint);

		// Draw in the text label.
		super.onDraw(canvas);
	}


	// ******************************************************************** //
	// Class Data.
	// ******************************************************************** //

    // Debugging tag.
	@SuppressWarnings("unused")
	private static final String TAG = "onwatch";

	// Colour to draw the bar.
	private static final int BAR_COLOR = 0xff006000;


	// ******************************************************************** //
	// Private Data.
	// ******************************************************************** //
	
	// Our window width and height.
	private int winWidth = 0;
	private int winHeight = 0;

	// Paint used for graphics.
	private Paint barPaint;
	
	// The fraction of width to show the bar at.
	private float barFraction = 0;

}

