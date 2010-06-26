
/**
 * org.hermit.android.test: simple test app for Hermit Android.
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


package org.hermit.android.test;


import org.hermit.android.core.MainActivity;

import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;


/**
 * A simple test app.
 *
 * @author Ian Cameron Smith
 */
public class AndroidTest
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
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setContentView(R.layout.main);
        
        Button chirp = (Button) findViewById(R.id.button_chirp);
        chirp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                makeSound(soundChirp);
            }
        });
        Button bleep = (Button) findViewById(R.id.button_bleep);
        bleep.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                makeSound(soundBleep);
            }
        });
     
        Log.i(TAG, "Create sound pool");
        soundPool = createSoundPool();
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
    }


    /**
     * This method is called after onStart() when the activity is being
     * re-initialized from a previously saved state, given here in state.
     * Most implementations will simply use onCreate(Bundle) to restore
     * their state, but it is sometimes convenient to do it here after
     * all of the initialization has been done or to allow subclasses
     * to decide whether to use your default implementation.  The default
     * implementation of this method performs a restore of any view
     * state that had previously been frozen by onSaveInstanceState(Bundle).
     * 
     * This method is called between onStart() and onPostCreate(Bundle).
     * 
     * @param   inState         The data most recently supplied in
     *                          onSaveInstanceState(Bundle).
     */
    @Override
    protected void onRestoreInstanceState(Bundle inState) {
        Log.i(TAG, "onRestoreInstanceState()");
        
        super.onRestoreInstanceState(inState);
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
    }


    /**
     * Called to retrieve per-instance state from an activity before being
     * killed so that the state can be restored in onCreate(Bundle) or
     * onRestoreInstanceState(Bundle) (the Bundle populated by this method
     * will be passed to both).
     * 
     * If called, this method will occur before onStop().  There are no
     * guarantees about whether it will occur before or after onPause().
     * 
     * @param   outState        A Bundle in which to place any state
     *                          information you wish to save.
     */
    @Override
    public void onSaveInstanceState(Bundle outState) {
        Log.i(TAG, "onSaveInstanceState()");
        
        super.onSaveInstanceState(outState);
    }

    
    /**
     * Called as part of the activity lifecycle when an activity is going
     * into the background, but has not (yet) been killed.  The counterpart
     * to onResume(). 
     * 
     * After receiving this call you will usually receive a following call
     * to onStop() (after the next activity has been resumed and displayed),
     * however in some cases there will be a direct call back to onResume()
     * without going through the stopped state. 
     * 
     * Derived classes must call through to the super class's implementation
     * of this method.  If they do not, an exception will be thrown.
     */
    @Override
    protected void onPause() {
        Log.i(TAG, "onPause()");
        
        super.onPause();
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
    }


    /**
     * Perform any final cleanup before an activity is destroyed.  This can
     * happen either because the activity is finishing (someone called
     * finish() on it, or because the system is temporarily destroying this
     * instance of the activity to save space.  You can distinguish between
     * these two scenarios with the isFinishing() method.
     * 
     * Derived classes must call through to the super class's implementation
     * of this method.  If they do not, an exception will be thrown.
     */
    @Override
    protected void onDestroy() {
        Log.i(TAG, "onDestroy()");
        
        super.onDestroy();
    }
    

    // ******************************************************************** //
    // Sound.
    // ******************************************************************** //
    
    /**
     * Create a SoundPool containing the app's sound effects.
     */
    private SoundPool createSoundPool() {
        SoundPool pool = new SoundPool(3, AudioManager.STREAM_MUSIC, 0);
        soundChirp = pool.load(this, R.raw.chirp_low, 1);
        soundBleep = pool.load(this, R.raw.hmlu, 1);

        Log.i(TAG, "Loaded sounds: chirp=" + soundChirp + ", bleep=" + soundBleep);
        return pool;
    }


    /**
     * Make a sound.
     * 
     * @param   soundId         ID of the sound to play.
     */
    void makeSound(int soundId) {
        Log.i(TAG, "Play sound " + soundId);
        soundPool.play(soundId, 1f, 1f, 1, 0, 1f);
    }


    // ******************************************************************** //
    // Class Data.
    // ******************************************************************** //

    // Debugging tag.
    private static final String TAG = "android test";


    // ******************************************************************** //
    // Private Data.
    // ******************************************************************** //

    // Sound pool used for sound effects.
    private SoundPool soundPool;
    
    // Sound IDs.
    private int soundChirp = 0;
    private int soundBleep = 0;

}

