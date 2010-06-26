
/**
 * PlugEdit: a level editor for Plughole.
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


package org.hermit.plughole.editor;


import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

import javax.swing.JPanel;


/**
 * The main level editor panel.  This class displays the current level
 * and allows it to be edited.
 */
public final class EditPanel
    extends JPanel
{

    // ******************************************************************** //
    // Public Constructors.
    // ******************************************************************** //

    /**
     * Create a level editor panel.
     *
     * @param	app       The application this view belongs to.
     */
    public EditPanel(PlugEdit app) {
        appContext = app;
        buildGui();
    }


    /**
     * Create the edit panel GUI.
     */
    private void buildGui() {
        setPreferredSize(new Dimension(XMARGIN * 6, YMARGIN * 6));
        setPreferredSize(new Dimension(180, 340));
        setBackground(Color.black);

        // Listen for resize events.
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                handleResize();
            }
        });
    }


    // ******************************************************************** //
    // Package Methods.
    // ******************************************************************** //

    /**
     * Handle a resize.
     */
    private void handleResize() {
        // Get the current size and recalculate the graph parameters.
        getSize(currentSize);
        // calcGraphParams();

        // Schedule a repaint.
        repaint();
    }


    /**
     * Player has spectrum analyser information.
     * 
     * @param	bands		Array of signal strength by frequency band.
  public void spectrum(int bands[]) {
    boolean changed = false;

    // Check the number of frequency bands.
    if (bands.length > numBands) {
      numBands = bands.length;
      changed = true;
    }

    // Allocate the values and peaks arrays, if not already there.
    if (currentValues == null || numBands > currentValues.length)
      currentValues = new int[numBands];
    if (peakValues == null || numBands > peakValues.length)
      peakValues = new int[numBands];

    // Save the data, and figure out the vertical scale.
    maxCurrentValue = 0;
    for (int i = 0; i < bands.length; ++i) {
      currentValues[i] = bands[i];
      if (bands[i] > maxCurrentValue)
	maxCurrentValue = bands[i];
    }

    // Adjust the peak values.  Slide them down 0.5%, and push them up
    // to at least the current values.
    maxCurrentPeak = 0;
    for (int i = 0; i < numBands; ++i) {
      peakValues[i] -= graphMax * 1 / 200;
      if (i < bands.length && bands[i] > peakValues[i])
	peakValues[i] = bands[i];
      if (peakValues[i] > maxCurrentPeak)
	maxCurrentPeak = peakValues[i];
    }

    // See if the scale is out of range.
    if (maxCurrentPeak > graphMax)
      changed = true;
    if (graphMax > maxCurrentPeak + GRID && graphMax > GRID * MIN_GRID) {
      if (++highScaleCount > HIGH_SCALE_LAG)
	changed = true;
    } else
      highScaleCount = 0;

    // If we changed any basic parameters, we need to recalculate the
    // graph params.
    if (changed)
      calcGraphParams();

    // Schedule a repaint.
    repaint();
  }
     */


    /**
     * Repaint the graph.
     * 
     * @param	g		Graphics context for painting with.
     */
    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);

        // If nothing to paint, that's it.
        if (currentValues == null)
            return;

        // Starting position.
        int xPos;
        int yPos = YMARGIN + graphHeight;

        // Calculate the colors to draw the bands in.
        if (bandColors == null || bandColors.length != numBands) {
            bandColors = new Color[numBands];
            for (int i = 0; i < numBands; ++i)
                bandColors[i] = calcColor(i, numBands);
        }

        // Draw the grid.  Encroach by a pixel into the top margin.
        for (int i = 0; i <= graphMax / GRID; ++i) {
            int height = i * GRID * graphHeight / graphMax;
            if (i % BIG_GRID == 0)
                g.setColor(GRID_MAJOR);
            else if (graphMax / GRID <= MAX_GRID)
                g.setColor(GRID_MINOR);
            else
                g.setColor(GRID_DARK);
            g.drawLine(XMARGIN, yPos - height, XMARGIN + graphWidth, yPos - height);
        }

        // Draw the peak markers.
        xPos = XMARGIN;
        g.setColor(Color.yellow);
        for (int i = 0; i < numBands; ++i) {
            int height = peakValues[i] * graphHeight / graphMax;
            g.fillRect(xPos + peakMargin, yPos - height - 2, peakWidth, 2);
            xPos += colWidth;
        }

        // Draw the bars.
        xPos = XMARGIN;
        for (int i = 0; i < numBands; ++i) {
            int height = currentValues[i] * graphHeight / graphMax;
            g.setColor(bandColors[i]);
            g.fillRect(xPos + barMargin, yPos - height, barWidth, height);
            xPos += colWidth;
        }
    }


    /**
     * Calculate the color to draw the given position in the range
     * 0 - max.
     * 
     * @param	index		Position to draw.
     * @param	max		Range of the given index.
     * @return			The calculated color.
     */
    private Color calcColor(int index, int max) {
        int width = max / 3;
        int band = index * 3 / max;
        int offset = index - band * width;
        int r = 0, g = 255, b = 0;

        switch (band) {
        case 0:
            r = 255;
            g = 0;
            b = offset * 255 / width;
            if (b > 255) b = 255;
            break;
        case 1:
            r = (width - offset) * 255 / width;
            if (r < 0) r = 0;
            g = 0;
            b = 255;
            break;
        case 2:
            r = 0;
            g = offset * 255 / width;
            if (g > 255) b = 255;
            b = 255;
            break;
        }
        return new Color(r, g, b);
    }


    // ******************************************************************** //
    // Support Functions.
    // ******************************************************************** //

    /**
     * Convert this instance to a string.  This allows it to be printed.
     * 
     * @return			A String representation of this object.
     */
    @Override
    public String toString() {
        return "SpectrumDisplay()";
    }


    // ******************************************************************** //
    // Class Data.
    // ******************************************************************** //

    /**
     * 
     */
    private static final long serialVersionUID = -8180953789572225102L;

    /**
     * Horizontal margins.
     */
    private static final int XMARGIN = 0;

    /**
     * Vertical margins.
     */
    private static final int YMARGIN = 1;

    /**
     * Grid spacing, in meter units.
     */
    private static final int GRID = 250;

    /**
     * How often to do a "large grid" line..
     */
    private static final int BIG_GRID = 5;

    /**
     * Minimum number of visible grid lines.
     */
    private static final int MIN_GRID = 5;

    /**
     * Maximum number of visible "non-large" grid lines.
     */
    private static final int MAX_GRID = 20;

    /**
     * Number of samples we allow the scale to be too high for
     * before scaling it down.
     */
    private static final int HIGH_SCALE_LAG = 512;

    /**
     * Amount to adjust the display up or down by at a time.
     */
    private static final float GRID_ADJUST = 1.025f;

    /**
     * Colors for the grid lines.
     */
    private static final Color GRID_MAJOR = new Color(0, 192, 0);
    private static final Color GRID_MINOR = new Color(0, 0, 255);
    private static final Color GRID_DARK = new Color(0, 0, 192);


    // ******************************************************************** //
    // Private Data.
    // ******************************************************************** //

    /**
     * The application this controller belongs to.
     */
    private PlugEdit appContext = null;

    /**
     * The data update rate; number of frames between updates.
     */
    private int updateRate = 2;

    /**
     * The current graph window size.
     */
    private Dimension currentSize = new Dimension(0, 0);

    /**
     * The number of frequency bands to display.
     */
    private int numBands = 0;

    /**
     * The current frequency power values.
     */
    private int[] currentValues = null;

    /**
     * The peak frequency power values.
     */
    private int[] peakValues = null;

    /**
     * The current maximum power value in the display.
     */
    private int maxCurrentValue = 0;

    /**
     * The current maximum of the peak power values.
     */
    private int maxCurrentPeak = 0;

    /**
     * Graph drawing parameters.
     */
    private int graphWidth = 0;
    private int graphHeight = 0;
    private int colWidth = 0;
    private int barWidth = 0;
    private int barMargin = 0;
    private int peakWidth = 0;
    private int peakMargin = 0;

    /**
     * Graph scale.
     */
    private int graphMax = GRID * MIN_GRID;

    /**
     * Colors used to draw the various bands.
     */
    private Color[] bandColors = null;

    /**
     * Number of samples for which the graph scale has been too high.
     */
    private int highScaleCount = 0;

}

