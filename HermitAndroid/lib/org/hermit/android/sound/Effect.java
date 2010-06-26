
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


/**
 * Class representing a specific sound effect.
 */
public class Effect
{

    // ******************************************************************** //
    // Constructor.
    // ******************************************************************** //

    /**
     * Create a sound effect description.  This constructor is not public;
     * outside users get an instance by calling {@link Player#addEffect(int)},
     * or {@link Player#addEffect(int, float)}.
     * 
     * @param   player     The Player this effect belongs to.
     * @param   id         Player's ID of this effect.
     * @param   vol        Base volume for this effect (used to modify the
     *                     sound file's inherent volume, if needed).  1 is
     *                     normal.
     */
    Effect(Player player, int id, float vol) {
        soundPlayer = player;
        playerSoundId = id;
        playVol = vol;
    }
    

	// ******************************************************************** //
	// Accessors.
	// ******************************************************************** //

    /**
     * Get the player's ID for this sound.
     * 
     * @return              Player's ID of this effect.
     */
    final int getSoundId() {
        return playerSoundId;
    }


    /**
     * Get the effect's volume.
     * 
     * @return              Base volume for this effect.
     */
    final float getPlayVol() {
        return playVol;
    }

    
    /**
     * Set the effect's volume.
     * 
     * @param   playVol     New base volume.
     */
    final void setPlayVol(float vol) {
        playVol = vol;
    }


    // ******************************************************************** //
    // Playing.
    // ******************************************************************** //

    /**
     * Play this sound effect.
     */
    public void play() {
        play(1f, false);
    }


    /**
     * Play this sound effect.
     * 
     * @param   rvol            Relative volume for this sound, 0 - 1.
     */
    public void play(float rvol) {
        play(rvol, false);
    }


    /**
     * Start playing this sound effect in a continuous loop.
     */
    public void loop() {
        play(1f, true);
    }


    /**
     * Play this sound effect.
     * 
     * @param   rvol            Relative volume for this sound, 0 - 1.
     * @param   loop            If true, loop the sound forever.
     */
    void play(float rvol, boolean loop) {
        streamId = soundPlayer.play(this, rvol * playVol, loop);
    }

    
    /**
     * Stop this sound effect immediately.
     */
    public void stop() {
        if (streamId != 0) {
            soundPlayer.stop(streamId);
            streamId = 0;
        }
    }


    /**
     * Determine whether this effect is playing.
     * 
     * @return              True if this sound effect is playing.
     */
    public final boolean isPlaying() {
        return streamId != 0;
    }


	// ******************************************************************** //
	// Private Data.
	// ******************************************************************** //
	
	// The player this effect is attached to.
	private final Player soundPlayer;

    // Sound ID of this effect in the sound player.
    private final int playerSoundId;

    // Base volume for this effect.
    private float playVol;

    // Stream ID of the stream which is playing this effect; 0
    // if it's not playing.
    private int streamId = 0;

}

