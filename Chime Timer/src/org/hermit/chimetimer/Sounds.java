
/**
 * Chime Timer: a simple and elegant timer.
 * <br>Copyright 2011 Ian Cameron Smith
 * 
 * <p>This app is a configurable, but simple and nice countdown timer.
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


package org.hermit.chimetimer;


import org.hermit.android.sound.Effect;
import org.hermit.android.sound.Player;

import android.content.Context;
import android.os.Handler;
import android.os.Message;


/**
 * The sounds for ChimeTimer.
 */
class Sounds
{

	// ******************************************************************** //
    // Shared Types.
    // ******************************************************************** //

    /**
     * The sounds that we make.
     */
	static enum SoundEffect {
    	BELL_A(R.raw.analogue_a),
    	BELL_Bb(R.raw.analogue_bflat),
    	BELL_C(R.raw.analogue_c),
    	BELL_Cs(R.raw.analogue_csharp),
    	BELL_D(R.raw.analogue_d),
    	BELL_Eb(R.raw.analogue_eb),
    	BELL_F(R.raw.analogue_f),
    	BELL_G(R.raw.analogue_g),
    	CHIME_1(R.raw.tibetan_01),
    	CHIME_2(R.raw.tibetan_02),
    	CHIME_3(R.raw.tibetan_03),
    	CHIME_4(R.raw.tibetan_04),
    	CHIME_5(R.raw.tibetan_05),
    	CHIME_6(R.raw.tibetan_06),
    	SIMPLE_1(R.raw.shriek_2011);
	
    	private SoundEffect(int res) {
    		soundRes = res;
    	}
    	
    	void play() {
    		if (playerEffect == null)
    			throw new IllegalStateException("tried to play before player" +
    											" was initialised");
    		playerEffect.play();
    	}
    	
    	static final SoundEffect valueOf(int ordinal) {
    		if (ordinal < 0 || ordinal >= VALUES.length)
    			return null;
    		return VALUES[ordinal];
    	}
        
    	private static final SoundEffect[] VALUES = values();

    	// Resource ID for the sound file.
    	private final int soundRes;
    	
    	// Effect object representing this sound.
        private Effect playerEffect = null;
    }

	
	// ******************************************************************** //
    // Constructor.
    // ******************************************************************** //

	/**
	 * Called by the system when the service is first created.
	 */
    public Sounds(Context context) {
    	appContext = context;
    	
        // Load the sounds.
        soundPlayer = createSoundPlayer();
    }
    

	// ******************************************************************** //
	// Run Control.
	// ******************************************************************** //

    /**
     * Resume the sound manager from a pause.
     */
    public void resume() {
        soundPlayer.resume();
    }
    

    /**
     * Pause the sound manager (e.g. for maintenance).
     */
    public void pause() {
        soundPlayer.suspend();
    }
    

    /**
     * Shut this sound manager down completely.  This frees all resources
     * associated with this sound manager.
     * 
     * <p>Following shutdown, this instance can not be used again.
     */
    public void shutdown() {
        soundPlayer.shutdown();
    }


	// ******************************************************************** //
	// Sound Playing.
	// ******************************************************************** //
    
    /**
     * Create a sound player containing the app's sound effects.
     */
    private Player createSoundPlayer() {
    	Player player = new Player(appContext, 3);
        for (SoundEffect sound : SoundEffect.VALUES)
            sound.playerEffect = player.addEffect(sound.soundRes, 1);
        return player;
    }

    
    /**
     * Make a sound.  Play it immediately.  Don't touch the queue.
     * 
     * @param	which			ID of the sound to play.
     */
    void makeSound(SoundEffect which) {
    	soundHandler.sendEmptyMessage(which.ordinal());
	}


    /**
     * Handler used to schedule sound playing.  Send an empty message
     * with what == the ordinal of the required sound.
     */
    private Handler soundHandler = new Handler() {
        @Override
		public void handleMessage(Message msg) {
        	SoundEffect which = SoundEffect.valueOf(msg.what);
        	if (which != null)
                which.play();
        }
    };
    

	// ******************************************************************** //
	// Class Data.
	// ******************************************************************** //

    // Debugging tag.
	@SuppressWarnings("unused")
	private static final String TAG = "ChimeTimer";

	
	// ******************************************************************** //
	// Private Data.
	// ******************************************************************** //
    
    // Our app context.
    private final Context appContext;
    
    // Sound player used for sound effects.
    private Player soundPlayer = null;

}

