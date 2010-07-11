
/**
 * org.hermit.android.sound: sound effects for Android.
 * 
 * These classes provide functions to help apps manage their sound effects.
 *
 * <br>Copyright 2009-2010 Ian Cameron Smith
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


package org.hermit.android.sound;


import java.util.ArrayList;

import android.content.Context;
import android.media.AudioManager;
import android.media.SoundPool;


/**
 * Main sound effects player class.  This is a pretty thin wrapper around
 * {@link android.media.SoundPool}, but adds some mild usefulness such as
 * per-effect volume.
 */
public class Player
{

    // ******************************************************************** //
    // Constructor.
    // ******************************************************************** //

    /**
     * Create a sound effect player that can handle 3 streams at once.
     * 
     * @param   context     Application context we're running in.
     */
    public Player(Context context) {
        this(context, 3);
    }
    

    /**
     * Create a sound effect player.
     * 
     * @param   context     Application context we're running in.
     * @param   streams     Maximum number of sound streams to play
     *                      simultaneously.
     */
    public Player(Context context, int streams) {
        appContext = context;
        numStreams = streams;
        soundEffects = new ArrayList<Effect>();
        soundPool = null;
    }


	// ******************************************************************** //
	// Sound Setup.
	// ******************************************************************** //

    /**
     * Add a sound effect to this player.
     * 
     * @param   sound       Resource ID of the sound sample for this effect.
     * @return              An Effect object representing the new effect.
     *                      Use this object to actually play the sound.
     */
    public Effect addEffect(int sound) {
        return addEffect(sound, 1f);
    }


    /**
     * Add a sound effect to this player.
     * 
     * @param   resId       Resource ID of the sound sample for this effect.
     * @param   vol         Base volume for this effect.
     * @return              An Effect object representing the new effect.
     *                      Use this object to actually play the sound.
     */
    public Effect addEffect(int resId, float vol) {
        Effect effect = new Effect(this, resId, vol);
        
        synchronized (this) {
            soundEffects.add(effect);

            // If we are running, we can load the sound now.
            if (soundPool != null) {
                int id = soundPool.load(appContext, effect.getResourceId(), 1);
                effect.setSoundId(id);
            }
        }

        return effect;
    }


    // ******************************************************************** //
    // Sound Playing.
    // ******************************************************************** //

    /**
     * Get the overall gain for sounds.
     * 
     * @return		        Current gain.  1 = normal; 0 means don't play
     *                      sounds.
     */
    public float getGain() {
        return soundGain;
    }
    

    /**
     * Set the overall gain for sounds.
     * 
     * @param   gain        Desired gain.  1 = normal; 0 means don't play
     *                      sounds.
     */
    public void setGain(float gain) {
        soundGain = gain;
    }
    

    /**
     * Play the given sound effect.
     * 
     * @param   effect      Sound effect to play.
     */
    public void play(Effect effect) {
        play(effect, 1f, false);
    }
    

    /**
     * Play the given sound effect.  The sound won't be played if the volume
     * would be zero or less.
     * 
     * @param   effect      Sound effect to play.
     * @param   rvol        Relative volume for this sound, 0 - 1.
     * @param   loop        If true, loop the sound forever.
     * @throws  IllegalStateException   Attempting to play before
     *                      {@link #resume()} or after {@link #resume()}.
     */
    public void play(Effect effect, float rvol, boolean loop) {
        synchronized (this) {
            if (soundPool == null)
                throw new IllegalStateException("can't play while suspended");

            // Calculate the play volume.
            float vol = soundGain * rvol;
            if (vol <= 0f)
                return;
            if (vol > 1f)
                vol = 1f;

            // Set it playing.
            int stream = soundPool.play(effect.getSoundId(), vol, vol, 1,
                    loop ? -1 : 0, 1f);
            effect.setStreamId(stream);
        }
    }
	

    /**
     * Stop the given effect, if it's playing.
     * 
     * @param   effect      Sound effect to stop.
     * @throws  IllegalStateException   Attempting to stop before
     *                      {@link #resume()} or after {@link #resume()}.
     */
    public void stop(Effect effect) {
        synchronized (this) {
            if (soundPool == null)
                throw new IllegalStateException("can't stop while suspended");

            int id = effect.getStreamId();
            if (id != 0) {
                soundPool.stop(id);
                effect.setStreamId(0);
            }
        }
    }
    

    /**
     * Stop all streams.
     */
    public void stopAll() {
        synchronized (this) {
            for (Effect e : soundEffects)
                stop(e);
        }
    }
    

	// ******************************************************************** //
	// Suspend / Resume.
	// ******************************************************************** //

    /**
     * Resume this player.  This allocates the media resources for all our
     * registered sound effects.  This method must be called before the
     * player can be used, and must be called again after calling
     * {@link #suspend()}
     * 
     * <p>It's a good idea for apps to do this in Activity.onResume().
     */
    public void resume() {
        synchronized (this) {
            if (soundPool == null) {
                soundPool = new SoundPool(numStreams, AudioManager.STREAM_MUSIC, 0);

                for (Effect e : soundEffects) {
                    int id = soundPool.load(appContext, e.getResourceId(), 1);
                    e.setSoundId(id);
                }
            }
        }
    }
    

    /**
     * Suspend this player.  This closes down the media resources the
     * player is using.  It's a good idea for apps to do this in
     * Activity.onPause().
     * 
     * <p>Following suspend, {@link #resume()} must be called before this
     * player can be used again.
     */
    public void suspend() {
        synchronized (this) {
            if (soundPool != null) {
                for (Effect e : soundEffects) {
                    stop(e);
                    e.setSoundId(-1);
                }
                soundPool.release();
                soundPool = null;
            }
        }
    }


    // ******************************************************************** //
    // Class Data.
    // ******************************************************************** //

    // Debugging tag.
	@SuppressWarnings("unused")
    private static final String TAG = "sound";


	// ******************************************************************** //
	// Private Data.
	// ******************************************************************** //

    // Our parent application context.
    private final Context appContext;
    
    // Maximum number of sound streams to play simultaneously.
    private final int numStreams;
 
    // Sound pool used for sound effects.  Null if not allocated; e.g.
    // if we are suspended.
    private SoundPool soundPool;
    
    // All sound effects.
    private ArrayList<Effect> soundEffects;
    
	// Current overall sound gain.  If zero, sounds are suppressed.
	private float soundGain = 1f;

}

