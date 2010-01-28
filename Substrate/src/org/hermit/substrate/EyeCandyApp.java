
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
import org.hermit.substrate.hacks.InterAggregate;
import org.hermit.substrate.hacks.InteraggregatePreferences;
import org.hermit.substrate.hacks.SandTraveller;
import org.hermit.substrate.hacks.SandTravellerPreferences;
import org.hermit.substrate.hacks.Substrate;
import org.hermit.substrate.hacks.SubstratePreferences;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.Window;


/**
 * A standalone activity which displays fullscreen eye candy.  Use
 * this to browse the eye candies, play with different options, hypnotize
 * your children, or something.  Mainly it gives the user
 * something to do when they don't know how to set up live wallpapers yet.
 * Hence, we use a "eula" to tell them how to do this.
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
        super.onCreate(icicle);
        
        // Create our EULA box.
        createEulaBox(R.string.eula_title, R.string.eula_text, R.string.button_close);       

        // Set up the standard dialogs.
        createMessageBox(R.string.button_close);
        setAboutInfo(R.string.about_text);
        setHomeInfo(R.string.button_homepage, R.string.url_homepage);
        setLicenseInfo(R.string.button_license, R.string.url_license);

        // We don't want a title bar.
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        // Create the application GUI.
        eyeCandyView = new EyeCandyView(this);
        setContentView(eyeCandyView);
        
        currentHack = 0;
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

        // Set the value of currentHack.  setHack() will be called in
        // onResume().
        currentHack = icicle.getInt("currentHack", 0);
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
        
        // First time round, show the EULA.
        showFirstEula();
        
        // By now currentHack has either been initialised or restored.
        // Select the indicated hack.
        setHack(currentHack);
        
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
     * Initialize the contents of the game's options menu by adding items
     * to the given menu.
     * 
     * This is only called once, the first time the options menu is displayed.
     * To update the menu every time it is displayed, see
     * onPrepareOptionsMenu(Menu).
     * 
     * When we add items to the menu, we can either supply a Runnable to
     * receive notification of selection, or we can implement the Activity's
     * onOptionsItemSelected(Menu.Item) method to handle them there.
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
        // We must call through to the base implementation.
        super.onCreateOptionsMenu(menu);
        
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);

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
        switch (item.getItemId()) {
        case R.id.menu_select_0:
            setHack(0);
            break;
        case R.id.menu_select_1:
            setHack(1);
            break;
        case R.id.menu_select_2:
            setHack(2);
            break;
        case R.id.menu_prefs:
            // Launch the preferences activity as a subactivity, so we
            // know when it returns.
            Intent pIntent = new Intent();
            pIntent.setClass(this, prefsList[currentHack]);
            startActivity(pIntent);
            break;
        case R.id.menu_help:
            // Launch the help activity as a subactivity.
            Intent hIntent = new Intent();
            hIntent.setClass(this, Help.class);
            startActivity(hIntent);
            break;
        case R.id.menu_about:
            showAbout();
            break;
        case R.id.menu_exit:
            finish();
            break;
        default:
            return super.onOptionsItemSelected(item);
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
        Class<?> hclass = hacksList[index];
        
        try {
            // Create an instance of the screen hack.
            Constructor<?> cons = hclass.getConstructor(Context.class);
            EyeCandy eyeCandy = (EyeCandy) cons.newInstance(this);
            
            // Display the hack.
            eyeCandyView.setHack(eyeCandy);
            currentHack = index;
        } catch (Exception e) {
            // throw new InstantiationException(e.getMessage());
        }
    }
    
    
    // ******************************************************************** //
    // Class Data.
    // ******************************************************************** //

    // Debugging tag.
    @SuppressWarnings("unused")
    private static final String TAG = "Substrate";
    
    // List of hacks.
    private static final Class<?>[] hacksList = {
        Substrate.class,
        InterAggregate.class,
        SandTraveller.class,
    };
    

    // List of the preferences activities for the above hacks.
    private static final Class<?>[] prefsList = {
        SubstratePreferences.class,
        InteraggregatePreferences.class,
        SandTravellerPreferences.class,
    };
    
    
    // ******************************************************************** //
    // Private Data.
    // ******************************************************************** //
    
    // The surface manager for the view.
    private EyeCandyView eyeCandyView = null;
    
    // Currently selected hack index.
    private int currentHack = 0;

}

