
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


import android.content.Context;
import android.media.AudioManager;
import android.media.SoundPool;


/**
 * Main sound effects player class.
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
        soundPool = new SoundPool(streams, AudioManager.STREAM_MUSIC, 0);
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
     * @param   sound       Resource ID of the sound sample for this effect.
     * @param   vol         Base volume for this effect.
     * @return              An Effect object representing the new effect.
     *                      Use this object to actually play the sound.
     */
    public Effect addEffect(int sound, float vol) {
        int id = soundPool.load(appContext, sound, 1);
        return new Effect(this, id, vol);
    }


    // ******************************************************************** //
    // Sound Playing.
    // ******************************************************************** //

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
     * @return              The ID of the stream which is playing the sound.
     *                      Zero if it's not playing.
     */
    public int play(Effect effect) {
        return play(effect, 1f, false);
    }
    

    /**
     * Play the given sound effect.  The sound won't be played if the volume
     * would be zero or less.
     * 
     * @param   effect      Sound effect to play.
     * @param   rvol        Relative volume for this sound, 0 - 1.
     * @param   loop        If true, loop the sound forever.
     * @return              The ID of the stream which is playing the sound.
     *                      Zero if it's not playing; this includes if
     *                      sounds are disabled, or if the volume is zero.
     */
    public int play(Effect effect, float rvol, boolean loop) {
        // Calculate the play volume.
        float vol = soundGain * rvol;
        if (vol <= 0f)
            return 0;
        if (vol > 1f)
            vol = 1f;
        
        // Set it playing.
        int stream = soundPool.play(effect.getSoundId(), vol, vol, 1, 0, 1f);
        if (stream != 0)
            soundPool.setLoop(stream, loop ? -1 : 0);
        return stream;
    }
	

    /**
     * Stop the given stream.
     * 
     * @param   id          Stream ID to stop.
     */
    public void stop(int id) {
        if (id != 0)
            soundPool.stop(id);
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
    
    // Sound pool used for sound effects.
    private final SoundPool soundPool;
    
	// Current overall sound gain.  If zero, sounds are suppressed.
	private float soundGain = 1f;

}

