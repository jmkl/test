
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


import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;


/**
 * A JPanel with a sensible grid layout.  Why the f**k doesn't
 * swing have this already?
 */
public class GridPanel
    extends JPanel
{

    // ******************************************************************** //
    // Constructors.
    // ******************************************************************** //

    /**
     * Initialize the panel.
     */
    public GridPanel() {
        // Make a GridBagLayout.
        gridbag = new GridBagLayout();
        setLayout(gridbag);

        // Set up the constraints.
        constr = new GridBagConstraints();
        constr.insets = new Insets(2, 2, 2, 2);
        constr.weightx = 1; constr.weighty = 1;
        constr.anchor = GridBagConstraints.NORTHWEST;
        constr.gridx = 0; constr.gridy = 0;
    }


    // ******************************************************************** //
    // GUI Construction.
    // ******************************************************************** //

    /**
     * Set the column space distribution weights to the given values.
     *
     * @param	weights		Weights for all the columns.  If there
     *				are more columns added than accounted for
     *				here, the extras will be given a weight of 1.
     */
    public void colWeights(int[] weights) {
        colWeights = weights;
    }


    /**
     * Set the row space distribution weight for future components
     * to the given value.  The default is 0.
     *
     * @param	weight		Weight for components added in future.
     */
    public void rowWeight(int weight) {
        rowWeight = weight;
    }


    /**
     * Set the component width multiplier for future components
     * to the given value.  The default is 1.
     *
     * @param	mult		Width multiplier for components
     *				added in future.
     */
    public void widthMult(int mult) {
        widthMult = mult;
    }


    /**
     * Start a new row in the layout.
     */
    public void newRow() {
        if (constr.gridx > 0) {
            constr.gridx = 0;
            ++constr.gridy;
        }
    }


    /**
     * Start a new row in the layout by adding the given label to it.
     *
     * @param	label		Text of the label to add.
     */
    public void newRow(String label) {
        if (constr.gridx > 0) {
            constr.gridx = 0;
            ++constr.gridy;
        }
        realAdd(new JLabel(label), 1, false, false);
    }


    /**
     * Start a new row in the layout by adding the given widget
     * as a label to it.
     *
     * @param	label		The label widget to add.
     */
    public void newRow(JComponent label) {
        if (constr.gridx > 0) {
            constr.gridx = 0;
            ++constr.gridy;
        }
        realAdd(label, 1, false, false);
    }


    /**
     * Add the given component to the panel at the next location in
     * the current row of the grid.
     *
     * @param	comp		The component to add.
     */
    public void add(JComponent comp) {
        realAdd(comp, 1 * widthMult, false, false);
    }


    /**
     * Add the given component to the panel at the next location in
     * the current row of the grid.
     *
     * @param	comp		The component to add.
     * @param	fillh		Iff true, stretch the component
     *				horizontally to fill its space, if the
     *				space is larger than the component.
     */
    public void add(JComponent comp, boolean fillh) {
        realAdd(comp, 1 * widthMult, fillh, false);
    }


    /**
     * Add the given component to the panel spanning multiple columns.
     *
     * @param	comp		The component to add.
     * @param	width		Number of columns to span.
     */
    public void add(JComponent comp, int width) {
        realAdd(comp, width * widthMult, false, false);
    }


    /**
     * Add the given component to the panel spanning multiple columns.
     *
     * @param	comp		The component to add.
     * @param	width		Number of columns to span.
     * @param	fillh		Iff true, stretch the component
     *				horizontally to fill its space, if the
     *				space is larger than the component.
     * @param	fillv		Iff true, stretch the component
     *				vertically to fill its space, if the
     *				space is larger than the component.
     */
    public void add(JComponent comp, int width, boolean fillh, boolean fillv) {
        realAdd(comp, width * widthMult, fillh, fillv);
    }


    /**
     * Add the given component to the panel spanning multiple columns.
     *
     * @param	comp		The component to add.
     * @param	width		Number of columns to span.
     * @param	fillh		Iff true, stretch the component
     *				horizontally to fill its space, if the
     *				space is larger than the component.
     * @param	fillv		Iff true, stretch the component
     *				vertically to fill its space, if the
     *				space is larger than the component.
     */
    private void realAdd(JComponent comp, int width,
            boolean fillh, boolean fillv)
    {
        // Set up the constraints.
        constr.gridwidth = width;
        if (fillv)
            constr.fill = fillh ? GridBagConstraints.BOTH : GridBagConstraints.VERTICAL;
        else
            constr.fill = fillh ? GridBagConstraints.HORIZONTAL : GridBagConstraints.NONE;

        if (colWeights != null && constr.gridx < colWeights.length)
            constr.weightx = colWeights[constr.gridx];
        else
            constr.weightx = 1;
        constr.weighty = rowWeight;

        // Add the component.
        gridbag.setConstraints(comp, constr);
        super.add(comp);

        constr.gridx += constr.gridwidth;
    }


    // ******************************************************************** //
    // Class Data.
    // ******************************************************************** //

    /**
     * UUID.
     */
    private static final long serialVersionUID = 8992981678142420463L;


    // ******************************************************************** //
    // Member Data.
    // ******************************************************************** //

    /**
     * The layout manager.
     */
    private GridBagLayout gridbag = null;

    /**
     * The layout constraints.
     */
    private GridBagConstraints constr = null;

    /**
     * Weights for all the columns.  If this is null, or there
     * are more columns added than accounted for here, the extras
     * will be given a weight of 1.
     */
    private int[] colWeights = null;

    /**
     * Row weight for new components.
     */
    private int rowWeight = 0;

    /**
     * Width multiplier for new components.
     */
    private int widthMult = 1;

}

