
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


import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.border.Border;
import javax.swing.border.EtchedBorder;


/**
 * Dialog which allows the user to enter or edit level details.
 */
class LevelProps
	extends JDialog
	implements PropertyChangeListener
{


    // ******************************************************************** //
    // Public Constructors.
    // ******************************************************************** //

	/**
     * Create a level properties dialog.
     *
     * @param	app       The application this view belongs to.
     */
    public LevelProps(PlugEdit app) {
        super(app, true);
        appContext = app;

        // Create our GUI.
        optionPane = new JOptionPane(makeControls(),
                                     JOptionPane.PLAIN_MESSAGE,
                                     JOptionPane.OK_CANCEL_OPTION,
                                     null,
                                     DIALOG_BUTTONS,
                                     SAVE_BUTTON);
        setContentPane(optionPane);

        // Handle option pane state changes.
        optionPane.addPropertyChangeListener(this);

        // Handle the dialog closing.
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
        	@Override
			public void windowClosing(WindowEvent we) {
        		optionPane.setValue(new Integer(JOptionPane.CLOSED_OPTION));
        	}
        });
        
        pack();
        setTitle("Level Properties");
    }


    // ******************************************************************** //
    // GUI Construction.
    // ******************************************************************** //

    /**
     * Make a panel containing the level properties.
     *
     * @return            A new Panel containing the property controls.
     */
    private JComponent makeControls() {
        // Make the panel with a grid layout.  Set up the column weights.
        GridPanel panel = new GridPanel();
        panel.colWeights(new int[] { 0, 1, 1 });
        Border opad = BorderFactory.createEmptyBorder(3, 3, 3, 3);
        Border ipad = BorderFactory.createEmptyBorder(4, 4, 4, 4);
        Border etch = BorderFactory.createEtchedBorder(EtchedBorder.LOWERED);
        Border out = BorderFactory.createCompoundBorder(opad, etch);
        panel.setBorder(BorderFactory.createCompoundBorder(out, ipad));

        panel.widthMult(2);

        panel.newRow("Level name:");
        panel.add(levelNameField = new JTextField("", 10), true);

        panel.newRow("Group:");
        levelGroupSpin = new JSpinner(new SpinnerNumberModel(1, 1, 64, 1));
        panel.add(levelGroupSpin);

        panel.newRow("Level:");
        levelDiffSpin = new JSpinner(new SpinnerNumberModel(1, 1, 64, 1));
        panel.add(levelDiffSpin);

        panel.newRow("Grid width:");
        gridWidthSpin = new JSpinner(new SpinnerNumberModel(24, 1, 80, 1));
        panel.add(gridWidthSpin);

        panel.newRow("Grid height:");
        gridHeightSpin = new JSpinner(new SpinnerNumberModel(36, 1, 100, 1));
        panel.add(gridHeightSpin);

        panel.newRow("Time:");
        levelTimeSpin = new JSpinner(new SpinnerNumberModel(80, 1, 300, 1));
        panel.add(levelTimeSpin);

        return panel;
    }


    // ******************************************************************** //
    // Actions.
    // ******************************************************************** //

    /**
     * Handle state changes in the option pane.
     */
    public void propertyChange(PropertyChangeEvent e) {
        String prop = e.getPropertyName();

        if (!isVisible() || e.getSource() != optionPane)
        	return;
        
        if (prop.equals(JOptionPane.VALUE_PROPERTY) ||
        		prop.equals(JOptionPane.INPUT_VALUE_PROPERTY)) {
            Object value = optionPane.getValue();

            // Ignore the value being reset.
            if (value == JOptionPane.UNINITIALIZED_VALUE)
                return;

            // Reset the value, so if the user presses the same button
            // again, a property change event will be fired.
            optionPane.setValue(JOptionPane.UNINITIALIZED_VALUE);

            if (value.equals(SAVE_BUTTON)) {
                String lname = levelNameField.getText();
                clearAndHide();
            } else {
//                dd.setLabel("It's OK.  "
//                         + "We won't force you to type "
//                         + magicWord + ".");
//                typedText = null;
                clearAndHide();
            }
        }
    }

    
    /**
     * Clear the dialog and hide it.
     */
    public void clearAndHide() {
    	levelNameField.setText(null);
        setVisible(false);
    }

    
    // ******************************************************************** //
    // Class Data.
    // ******************************************************************** //

    // UUID.
	private static final long serialVersionUID = -7828181438749164700L;

	// The buttons on this dialog.
    private static final String SAVE_BUTTON = "Save";
    private static final String CANCEL_BUTTON = "Cancel";
    private static final String[] DIALOG_BUTTONS = { SAVE_BUTTON, CANCEL_BUTTON };

    
    // ******************************************************************** //
    // Member Data.
    // ******************************************************************** //

    // Our application context.
    private PlugEdit appContext;
    
    // Our main GUI panel.
    private JOptionPane optionPane = null;

    // GUI widgets.
    private JTextField levelNameField = null;
    private JSpinner levelGroupSpin = null;
    private JSpinner levelDiffSpin = null;
    private JSpinner gridWidthSpin = null;
    private JSpinner gridHeightSpin = null;
    private JSpinner levelTimeSpin = null;

}

