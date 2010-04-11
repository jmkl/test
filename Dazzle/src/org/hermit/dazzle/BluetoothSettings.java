
/**
 * Dazzle: a screen brightness control widget for Android.
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


package org.hermit.dazzle;


import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.util.Log;
import android.widget.RemoteViews;


/**
 * This static class provides utilities to manage the Bluetooth state.
 */
public class BluetoothSettings
{

    // ******************************************************************** //
    // Constructor.
    // ******************************************************************** //

    /**
     * Constructor -- hidden, as this class is non-instantiable.
     */
    private BluetoothSettings() {
    }
    

    // ******************************************************************** //
    // Status Handling.
    // ******************************************************************** //

    /**
     * Called after onRestoreInstanceState(Bundle), onRestart(), or onPause(),
     * for your activity to start interacting with the user.  This is a good
     * place to begin animations, open exclusive-access devices (such as the
     * camera), etc.
     * 
     * Derived classes must call through to the super class's implementation
     * of this method.  If they do not, an exception will be thrown.
     */
    static void toggle(Context context) {
        Log.i(TAG, "toggle Bluetooth");
        
        // Just toggle Bluetooth power, as long as we're not already in
        // an intermediate state.
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        int state = adapter.getState();
        if (state == BluetoothAdapter.STATE_OFF)
            adapter.enable();
        else if (state == BluetoothAdapter.STATE_ON)
            adapter.disable();
    }


    static void setWidget(Context context, RemoteViews views, int widget) {
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        int state = adapter.getState();
        
        int image = R.drawable.grey;
        if (state == BluetoothAdapter.STATE_OFF)
            image = R.drawable.grey;
        else if (state == BluetoothAdapter.STATE_ON)
            image = R.drawable.blue;
        else
            image = R.drawable.orange;

        views.setImageViewResource(widget, image);
    }


    // ******************************************************************** //
    // Class Data.
    // ******************************************************************** //

    // Debugging tag.
    @SuppressWarnings("unused")
    private static final String TAG = DazzleProvider.TAG;

}

