
/**
 * Substrate: a collection of eye candies for Android.  Various screen
 * hacks from the xscreensaver collection can be viewed standalone, or
 * set as live wallpapers.
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


package org.hermit.substrate;


import java.lang.reflect.Constructor;

import org.hermit.android.core.MainActivity;
import org.hermit.android.core.OneTimeDialog;
import org.hermit.substrate.hacks.Guts;
import org.hermit.substrate.hacks.GutsPreferences;
import org.hermit.substrate.hacks.HappyPlace;
import org.hermit.substrate.hacks.HappyPlacePreferences;
import org.hermit.substrate.hacks.InterAggregate;
import org.hermit.substrate.hacks.InteraggregatePreferences;
import org.hermit.substrate.hacks.NodeGarden;
import org.hermit.substrate.hacks.NodeGardenPreferences;
import org.hermit.substrate.hacks.SandDollar;
import org.hermit.substrate.hacks.SandDollarPreferences;
import org.hermit.substrate.hacks.SandTraveller;
import org.hermit.substrate.hacks.SandTravellerPreferences;
import org.hermit.substrate.hacks.Substrate;
import org.hermit.substrate.hacks.SubstratePreferences;

import android.app.WallpaperInfo;
import android.app.WallpaperManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.Window;
import android.widget.Toast;


/**
 * A standalone activity which displays fullscreen eye candy.  Use
 * this to browse the eye candies, play with different options, hypnotize
 * your children, or something.  Mainly it gives the user
 * something to do when they don't know how to set up live wallpapers yet.
 * Hence, we use a one-time dialog to tell them how to do this.
 */
public class EyeCandyApp
    extends MainActivity
{

    // ******************************************************************** //
    // Activity Lifecycle.
    // ******************************************************************** //

    /**
     * Called when the activity is starting.  This is where most
     * initialisation should go: calling setContentView(int) to inflate
     * the activity's UI, etc.
     * 
     * You can call finish() from within this function, in which case
     * onDestroy() will be immediately called without any of the rest of
     * the activity lifecycle executing.
     * 
     * Derived classes must call through to the super class's implementation
     * of this method.  If they do not, an exception will be thrown.
     * 
     * @param   icicle          If the activity is being re-initialised
     *                          after previously being shut down then this
     *                          Bundle contains the data it most recently
     *                          supplied in onSaveInstanceState(Bundle).
     *                          Note: Otherwise it is null.
     */
    @Override
    public void onCreate(Bundle icicle) {
        Log.i(TAG, "onCreate(" + (icicle == null ? "null" : "icicle") + ")");

        super.onCreate(icicle);
        
        // Create our startup notice box.
        introNotice = new OneTimeDialog(this, "intro", R.string.intro_title,
                                        R.string.intro_text, R.string.button_close);

        // Set up the standard dialogs.
        setAboutInfo(R.string.about_text);
        setHomeInfo(R.string.url_homepage);
        setLicenseInfo(R.string.url_license);

        // We don't want a title bar.
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        // Create the application GUI.
        eyeCandyView = new EyeCandyView(this);
        setContentView(eyeCandyView);
        
        // Default to showing the first hack.  This usually gets overridden
        // in onRestoreInstanceState() and/or onResume().
        restoredHack = 0;
    }
    

    /**
     * Called after {@link #onCreate} or {@link #onStop} when the current
     * activity is now being displayed to the user.  It will
     * be followed by {@link #onRestart}.
     */
    @Override
    protected void onStart() {
        Log.i(TAG, "onStart()");
        
        super.onStart();
        eyeCandyView.onStart();
    }

    
    /**
     * This method is called after onStart() when the activity is being
     * re-initialized from a previously saved state, given here in state.
     * 
     * @param   icicle      The data most recently supplied in
     *                      onSaveInstanceState(Bundle).
     */
    @Override
    protected void onRestoreInstanceState(Bundle icicle) {
        Log.i(TAG, "onRestoreInstanceState()");

        // Set the value of restoredHack.  setHack() will be called in
        // onResume().
        restoredHack = icicle.getInt("currentHack", 0);
    }


    /**
     * Called after onRestoreInstanceState(Bundle), onRestart(), or onPause(),
     * for your activity to start interacting with the user.  This is a good
     * place to begin animations, open exclusive-access devices (such as the
     * camera), etc.
     * 
     * Derived classes must call through to the super class's implementation
     * of this method.  If they do not, an exception will be thrown.
     */
    @Override
    protected void onResume() {
        Log.i(TAG, "onResume()");

        super.onResume();

        // First time round, show the intro notice.
        introNotice.showFirst();
        
        // If one of our hacks is currently running, set that as the
        // hack to display initially.  Otherwise use whatever restoredHack
        // has.
        WallpaperManager wm = WallpaperManager.getInstance(this);
        WallpaperInfo winfo = wm.getWallpaperInfo();
        if (winfo != null) {
            String ws = winfo.getSettingsActivity();
            for (int i = 0; i < prefsList.length; ++i) {
                String hn = prefsList[i].getName();
                if (hn != null && hn.equals(ws)) {
                    restoredHack = i;
                    break;
                }
            }
        }
        
        // If restoredHack has been set, select the indicated hack.
        if (restoredHack >= 0) {
            setHack(restoredHack);
            restoredHack = -1;
        }

        // Resume the view.
        eyeCandyView.onResume();
        
        // Just start straight away.
        eyeCandyView.surfaceStart();
    }


    /**
     * Called to retrieve per-instance state from an activity before being
     * killed so that the state can be restored in onCreate(Bundle) or
     * onRestoreInstanceState(Bundle) (the Bundle populated by this method
     * will be passed to both).
     * 
     * @param   outState        A Bundle in which to place any state
     *                          information you wish to save.
     */
    @Override
    public void onSaveInstanceState(Bundle outState) {
        Log.i(TAG, "onSaveInstanceState()");
        super.onSaveInstanceState(outState);
        
        // Save the selected hack index.
        outState.putInt("currentHack", currentHack);
    }

    
    /**
     * Called as part of the activity lifecycle when an activity is going
     * into the background, but has not (yet) been killed.  The counterpart
     * to onResume(). 
     */
    @Override
    protected void onPause() {
        Log.i(TAG, "onPause()");
        
        super.onPause();
        
        eyeCandyView.onPause();
    }


    /**
     * Called when you are no longer visible to the user.  You will next
     * receive either {@link #onStart}, {@link #onDestroy}, or nothing,
     * depending on later user activity.
     */
    @Override
    protected void onStop() {
        Log.i(TAG, "onStop()");
        super.onStop();
        
        eyeCandyView.onStop();
    }

  
    // ******************************************************************** //
    // Menu Handling.
    // ******************************************************************** //

    /**
     * Initialize the contents of the app's options menu by adding items
     * to the given menu.
     * 
     * This is only called once, the first time the options menu is displayed.
     * To update the menu every time it is displayed, see
     * onPrepareOptionsMenu(Menu).
     * 
     * We build this menu programmatically, so we can create the "Select
     * hack" menu based on data.
     * 
     * @param   menu            The options menu in which we should
     *                          place our items.  We can safely hold on this
     *                          (and any items created from it), making
     *                          modifications to it as desired, until the next
     *                          time onCreateOptionsMenu() is called.
     * @return                  true for the menu to be displayed; false
     *                          to suppress showing it.
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Add the "Select hack" sub-menu.
        SubMenu selectMenu = menu.addSubMenu(R.string.menu_select);
        selectMenu.setIcon(R.drawable.ic_menu_show_list);
        
        // Add entries for all the hacks.
        for (int i = 0; i < hackNamesList.length; ++i)
            selectMenu.add(0, MENU_HACK_BASE + i, 0, hackNamesList[i]);
        
        // Make the remaining menu items -- these are all simple ones.
        MenuItem it;
        it = menu.add(0, MENU_PREFS, 0, R.string.menu_prefs);
        it.setIcon(R.drawable.ic_menu_preferences);
        it = menu.add(0, MENU_HELP, 0, R.string.menu_help);
        it.setIcon(R.drawable.ic_menu_help);
        it = menu.add(0, MENU_ABOUT, 0, R.string.menu_about);
        it.setIcon(R.drawable.ic_menu_info);
        it = menu.add(0, MENU_EXIT, 0, R.string.menu_exit);
        it.setIcon(R.drawable.ic_menu_close);

        return true;
    }
    
    
    /**
     * This hook is called whenever an item in your options menu is selected.
     * Derived classes should call through to the base class for it to
     * perform the default menu handling.  (True?)
     *
     * @param   item            The menu item that was selected.
     * @return                  false to have the normal processing happen.
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        final int id = item.getItemId();
        
        if (id >= MENU_HACK_BASE) {
            int index = id - MENU_HACK_BASE;
            if (index < hacksList.length)
                setHack(index);
        } else {
            switch (item.getItemId()) {
            case MENU_PREFS:
                // Launch the preferences activity as a subactivity, so we
                // know when it returns.
                Intent pIntent = new Intent();
                pIntent.setClass(this, prefsList[currentHack]);
                startActivity(pIntent);
                break;
            case MENU_HELP:
                // Launch the help activity as a subactivity.
                Intent hIntent = new Intent();
                hIntent.setClass(this, Help.class);
                startActivity(hIntent);
                break;
            case MENU_ABOUT:
                showAbout();
                break;
            case MENU_EXIT:
                finish();
                break;
            default:
                return super.onOptionsItemSelected(item);
            }
        }
        
        return true;
    }
    

    // ******************************************************************** //
    // Control.
    // ******************************************************************** //

    /**
     * Set the currently displayed hack.
     * 
     * @param   index       Index (in hacksList[] etc.) of the hack
     *                      to display.
     */
    private void setHack(int index) {
        // Do nothing if it's already set.
        if (index == currentHack)
            return;
        
        try {
            // Create an instance of the screen hack.
            Class<?> hclass = hacksList[index];
            Constructor<?> cons = hclass.getConstructor(Context.class);
            EyeCandy eyeCandy = (EyeCandy) cons.newInstance(this);
            
            // Display the hack.
            eyeCandyView.setHack(eyeCandy);
            currentHack = index;
        } catch (Exception e) {
            String text = "Can't set selected hack " +
                            hackNamesList[index] + ": " + e.getMessage();
            Toast toast = Toast.makeText(this, text, Toast.LENGTH_LONG);
            toast.show();
        }
    }


    // ******************************************************************** //
    // Class Data.
    // ******************************************************************** //

    // Debugging tag.
    @SuppressWarnings("unused")
    private static final String TAG = "Substrate";
    
    // Menu item constants.
    private static final int MENU_PREFS = 2;
    private static final int MENU_HELP = 3;
    private static final int MENU_ABOUT = 4;
    private static final int MENU_EXIT = 9;
    private static final int MENU_HACK_BASE = 100;

    // List of hacks.
    private static final Class<?>[] hacksList = {
        Substrate.class,
        InterAggregate.class,
        SandTraveller.class,
        Guts.class,
        HappyPlace.class,
        NodeGarden.class,
        SandDollar.class,
    };
    

    // List of the names of the hacks.
    private static final int[] hackNamesList = {
        R.string.substrate_title,
        R.string.interaggregate_title,
        R.string.sandtrav_title,
        R.string.guts_title,
        R.string.happy_title,
        R.string.nodes_title,
        R.string.dollar_title,
    };
    

    // List of the preferences activities for the above hacks.
    private static final Class<?>[] prefsList = {
        SubstratePreferences.class,
        InteraggregatePreferences.class,
        SandTravellerPreferences.class,
        GutsPreferences.class,
        HappyPlacePreferences.class,
        NodeGardenPreferences.class,
        SandDollarPreferences.class,
    };
    
    
    // ******************************************************************** //
    // Private Data.
    // ******************************************************************** //
    
    // Notice used to display an introduction on first start.
    private OneTimeDialog introNotice;

    // The surface manager for the view.
    private EyeCandyView eyeCandyView = null;
    
    // Restored hack index -- i.e. the one that should be set when we get
    // fully up and running.  Negative if not set.
    private int restoredHack = -1;

    // Currently set hack index.  Negative if not set.
    private int currentHack = -1;

}

