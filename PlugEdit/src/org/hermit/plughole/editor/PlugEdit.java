
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


import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.BoxLayout;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.KeyStroke;
import javax.swing.WindowConstants;
import javax.swing.event.ChangeListener;


/**
 * JJuke front-end.
 *
 * <p>This class implements the GUI front-end for JJuke.
 */
public final class PlugEdit
extends JFrame
{

    // ******************************************************************** //
    // Public Constructors.
    // ******************************************************************** //

    /**
     * Constructor.  Create a control panel.
     */
    public PlugEdit() {
        init();
    }


    /**
     * Constructor.  Create a control panel.
     *
     * @param	app		The application this controller belongs to.
     */
    private void init() {
        // Catch window close requests.
        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) { dismiss(); }
        });

        // Put the gui in.
        buildGui();

        // Pack it and pop it.
        setTitle("JJuke Master Control");
        pack();
        setVisible(true);
    }


    /**
     * Create the GUI.
     */
    private void buildGui() {
        // Make the menu bar.
        JMenuBar menuBar = buildMenuBar();
        setJMenuBar(menuBar);

        // Create a panel for the GUI.
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        // Make the status display.
        JPanel editPanel = new EditPanel(this);
        panel.add(editPanel);
        
        //    // Make the playlist.
        //    playlist = new PlayListView(appContext, appContext.getPlaylist(), this);
        //    panel.add(playlist);
        //
        //    // Add the playlist's menu bar items to our menu bar.
        //    playlist.buildMenuBarMenus(menuBar);
        //
        //    // Make the volume controls.
        //    JPanel mixer = new Mixer(appContext, false, this);
        //    panel.add(mixer);
        //
        //    // Make the music control buttons.
        //    JPanel mButtons = new ControlButtons(appContext, 1, null, this);
        //    panel.add(mButtons);

        // Add the GUI to the frame.
        getContentPane().add("Center", panel);
    }


    /**
     * Create the menu bar.
     */
    private JMenuBar buildMenuBar() {
        // Create the menu bar.
        JMenuBar menuBar = new JMenuBar();

        JMenu menu;
        JMenuItem menuItem;

        // Build the "Playlist" menu.
        menu = new JMenu("JJuke");
        menu.setMnemonic(KeyEvent.VK_P);
        menuBar.add(menu);

        menuItem = new JMenuItem("Graph", KeyEvent.VK_G);
        menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_G,
                ActionEvent.ALT_MASK));
        menuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                //	appContext.spectrumAnalyserView();
            }
        });
        menu.add(menuItem);

        menu.addSeparator();

        menuItem = new JMenuItem("Close", KeyEvent.VK_C);
        menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_C,
                ActionEvent.ALT_MASK));
        menuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) { dismiss(); }
        });
        menu.add(menuItem);

        menuItem = new JMenuItem("Quit", KeyEvent.VK_Q);
        menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Q,
                ActionEvent.ALT_MASK));
        menuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                //	appContext.requestShutdown(JFrame.this);
            }
        });
        menu.add(menuItem);

        return menuBar;
    }


    // ******************************************************************** //
    // Package Methods.
    // ******************************************************************** //

    /**
     * Pop up this view.
     */
    public void popup() {
        setVisible(true);
    }


    /**
     * Hide this view (if we're in a frame); or shut down the app,
     * if this is a controller window.
     */
    public void dismiss() {
        setVisible(false);
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
        return "Master()";
    }


    // ******************************************************************** //
    // Application Main.
    // ******************************************************************** //

    /**
     * Main routine.  Parse from a named file (if an argument is
     * supplied), or standard input, and start editing the defined
     * model.
     *
     * @param	args		User-specified parameters.
     */
    public static void main(String args[]) {
        new PlugEdit();
    }


    // ******************************************************************** //
    // Class Data.
    // ******************************************************************** //

    /**
     * 
     */
    private static final long serialVersionUID = -4357121313088968773L;


    // ******************************************************************** //
    // Private Data.
    // ******************************************************************** //

    /**
     * Iff true, we're the control window; quit when we're closed.
     */
    private boolean controller = false;

    /**
     * The "Auto-Edit" toggle switch in the menu.
     */
    private JCheckBoxMenuItem autoEdit = null;

    /**
     * The music "jump-to" slider.
     */
    private JSlider positionSlider = null;

    /**
     * The listener for slider events.
     */
    private ChangeListener sliderListener = null;

    /**
     * The number of seconds in the current track.
     */
    private int currentTrackSecs = -1;

}

