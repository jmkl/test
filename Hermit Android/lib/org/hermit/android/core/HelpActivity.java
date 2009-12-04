
/**
 * org.hermit.android.core: useful Android foundation classes.
 * 
 * These classes are designed to help build various types of application.
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


package org.hermit.android.core;


import android.app.Activity;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;


/**
 * An activity which displays an application's help, in a structured
 * format.  Help is supplied via resources arrays; one array contains
 * the titles of the help sections, one contains the texts for each
 * section.  Sub-sections can be added.
 * 
 * <p>To use: subclass this activity, and have the subclass call
 * {@link #addHelpFromArrays(int, int)}.  Then start this activity when
 * you need to display help.
 */
public class HelpActivity
    extends Activity
{
    
    // ******************************************************************** //
    // Activity Lifecycle.
    // ******************************************************************** //

    /**
     * Called when the activity is starting.  This is where most
     * initialization should go: calling setContentView(int) to inflate
     * the activity's UI, etc.
     * 
     * @param   icicle          Saved application state, if any.
     */
    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        // Create a scrolling panel as the main view.
        mainView = new ScrollView(this);
        setContentView(mainView);
    }


    // ******************************************************************** //
    // Configuration.
    // ******************************************************************** //
    
    /**
     * Add help to this help activity.
     * 
     * <p>The parameters are the resource IDs of two arrays; the first
     * contains the titles of the help sections, the second contains the
     * bodies of the sections.  The arrays must be the same length.
     * 
     * <p>A title and the corresponding content may both be resource
     * IDs of further arrays.  These arrays will be added as sub-sections
     * in the most recently-added section.  There must be an enclosing
     * outer section.
     * 
     * <p>If this method is called more than once, help will be added to the
     * top level.  (This isn't really recommended.)
     * 
     * @param   titlesId        Resource ID of the titles array.
     * @param   textsId         Resource ID of the contents array.
     */
    protected void addHelpFromArrays(int titlesId, int textsId) {
        // Create a new level-0 help view, and add it to the main view.
        HelpView hv = new HelpView(0, titlesId, textsId);
        mainView.addView(hv);
    }
    

    // ******************************************************************** //
    // Private Classes.
    // ******************************************************************** //

    /**
     * This class implements a help view, containing structured
     * help text.
     */
    private final class HelpView
        extends LinearLayout
    {
        
        /**
         * Construct a help view.
         * 
         * @param   level           Nesting level for this view.
         * @param   titlesId        Resource ID of the titles array.
         * @param   textsId         Resource ID of the contents array.
         */
        private HelpView(int level, int titlesId, int textsId) {
            super(HelpActivity.this);
            setOrientation(VERTICAL);
            
            Resources res = getResources();
            
            // Get the section titles and texts from resources.
            TypedArray titles = res.obtainTypedArray(titlesId);
            final int nTitles = titles.length();
            TypedArray texts = res.obtainTypedArray(textsId);
            final int nTexts = texts.length();
            if (nTitles != nTexts)
                throw new IllegalArgumentException("HelpActivity:" +
                                        " titles and contents arrays" +
                                        " must be the same length");

            // Now process all the titles and their corresponding contents.
            TypedValue title = new TypedValue();
            TypedValue text = new TypedValue();
            for (int t = 0; t < nTitles; ++t) {
                titles.getValue(t, title);
                texts.getValue(t, text);
                if (title.type != text.type)
                    throw new IllegalArgumentException("HelpActivity:" +
                            " titles and contents values" +
                            " must be the same type (item " + t + ")");
                switch (title.type) {
                case TypedValue.TYPE_STRING:
                    addSection(level, title, text);
                    break;
                case TypedValue.TYPE_REFERENCE:
                    addSubs(level, title, text);
                    break;
                default:
                    throw new IllegalArgumentException("HelpActivity:" +
                                        " invalid item type (item " + t + ")");
                }
            }
            
        }
        
        private void addSection(int level, TypedValue title, TypedValue text) {
            LinearLayout.LayoutParams lp;
            
            ViewGroup body = new BodyView(HelpActivity.this, level, text.string);
            ViewGroup head = new TitleView(HelpActivity.this, level, title.string, body);

            lp = new LinearLayout.LayoutParams(FPAR, WCON);
            lp.topMargin = 2;
            lp.leftMargin = level * 32;
            addView(head, lp);
            lp = new LinearLayout.LayoutParams(FPAR, WCON);
            addView(body, lp);
            
            prevBody = body;
        }
        
        private void addSubs(int level, TypedValue title, TypedValue text) {
            if (prevBody == null)
                throw new IllegalArgumentException("HelpActivity:" +
                                        " sub-sections must be attached" +
                                        " to an enclosing section");

            ViewGroup body = new HelpView(level + 1, title.resourceId, text.resourceId);
            
            prevBody.addView(body, new LinearLayout.LayoutParams(FPAR, WCON));
            prevBody = body;
        }

        // The most recently added section body.
        private ViewGroup prevBody = null;
        
    }


    /**
     * This class implements a title view; it controls the visibility
     * of its associated content.
     */
    private static final class TitleView
        extends LinearLayout
        implements View.OnClickListener
    {
        
        /**
         * Construct a title view.
         * 
         * @param   parent          Parent activity.
         * @param   level           Nesting level for this view.
         * @param   title           Title string
         * @param   body            The view displaying the content for this
         *                          section.  Its visibility will
         *                          be controlled by this TitleView.
         */
        private TitleView(Activity parent, int level, CharSequence title, View body) {
            super(parent);
            setOrientation(HORIZONTAL);
            
            // Store the reference to the body for later.
            bodyView = body;

            // Set our overall background colour depending on the
            // section level.
            setBackgroundColor(LEVEL_COLS[level % LEVEL_COLS.length]);

            LayoutParams lp;
            
            // Add an expand control.  Because Android libraries can't
            // have their own resources, we use a text view showing "+"
            // and "-".
            expandView = new TextView(parent);
            expandView.setTextColor(0xff0000ff);
            expandView.setTextSize(38 - level * 4);
            expandView.setGravity(Gravity.CENTER);
            lp = new LayoutParams(64, FPAR);
            addView(expandView, lp);
   
            // Create the title text view.
            textView = new TextView(parent);
            textView.setTextColor(0xff000000);
            textView.setTextSize(28 - level * 4);
            textView.setGravity(Gravity.CENTER_VERTICAL);
            textView.setText(title);
            lp = new LayoutParams(FPAR, FPAR);
            addView(textView, lp);
            
            setExpanded(false);
            setClickable(true);
            setOnClickListener(this);
        }
        
        @Override
        public void onClick(View v) {
            setExpanded(!bodyVis);
        }
        
        /**
         * Set this section to be expanded or not.
         */
        private void setExpanded(boolean expanded) {
            bodyVis = expanded;
            expandView.setText(bodyVis ? "–" : "+");
            bodyView.setVisibility(bodyVis ? VISIBLE : GONE);
        }
    
        private TextView expandView = null;
        private TextView textView = null;
        private View bodyView = null;
        private boolean bodyVis = false;
    }
    

    /**
     * This class implements a section body view.  It's basically
     * just text, but sub-sections may be added to it, so we use
     * LinearLayout.
     */
    private static final class BodyView extends LinearLayout {
        
        /**
         * Construct a body view.
         * 
         * @param   parent          Parent activity.
         * @param   level           Nesting level for this view.
         * @param   text            Content text string
         */
        private BodyView(Activity parent, int level, CharSequence text) {
            super(parent);
            setOrientation(VERTICAL);
            
            TextView bt = new TextView(parent);
            bt.setPadding((level + 1) * 32, 0, 0, 0);
            bt.setText(text);
            LayoutParams lp = new LayoutParams(FPAR, WCON);
            addView(bt, lp);
        }
        
    }
    
    
    // ******************************************************************** //
    // Private Constants.
    // ******************************************************************** //

    // Colours for the headings at various levels.
    private static final int[] LEVEL_COLS = {
        0xff008c8c, 0xff8c8c00, 0xff8c008c
    };
    
    // Convenience shorthands.
    private static final int FPAR = LinearLayout.LayoutParams.FILL_PARENT;
    private static final int WCON = LinearLayout.LayoutParams.WRAP_CONTENT;
    

    // ******************************************************************** //
    // Private Data.
    // ******************************************************************** //
    
    // The main top-level view; the scroll view that contains the
    // top-level help widget.
    private ScrollView mainView = null;
    
}

