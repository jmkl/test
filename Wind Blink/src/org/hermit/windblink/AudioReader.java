
/**
 * Wind Blink: a wind meter for Android.
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


package org.hermit.windblink;


import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;


/**
 * The main wind meter view.  This class relies on the parent SurfaceRunner
 * class to do the bulk of the animation control.
 */
public class AudioReader
    implements Runnable
{

    // ******************************************************************** //
    // Public Classes.
    // ******************************************************************** //

    /**
     * Listener for audio reads.
     */
    public static abstract class Listener {
        /**
         * An audio read has completed.
         * @param   buffer      Buffer containing the data.
         */
        public abstract void onReadComplete(short[] buffer);
    }
    
    
    // ******************************************************************** //
    // Constructor.
    // ******************************************************************** //

	/**
	 * Create a WindMeter instance.
	 */
    public AudioReader() {
//        audioManager = (AudioManager) app.getSystemService(Context.AUDIO_SERVICE);

        audioBufferBytes = AudioRecord.getMinBufferSize(SAMPLE_RATE,
                                     AudioFormat.CHANNEL_CONFIGURATION_MONO,
                                     AudioFormat.ENCODING_PCM_16BIT) * 2;
        audioInput = new AudioRecord(MediaRecorder.AudioSource.MIC,
                                     SAMPLE_RATE,
                                     AudioFormat.CHANNEL_CONFIGURATION_MONO,
                                     AudioFormat.ENCODING_PCM_16BIT,
                                     audioBufferBytes);
    }


    // ******************************************************************** //
    // Run Control.
    // ******************************************************************** //

    /**
     * Start this reader.
     * 
     * @param   block       Number of bytes of input to read at a time.
     *                      This is different from the system audio
     *                      buffer size.
     * @param   listener    Listener to be notified on each completed read.
     */
    public void startReader(int block, Listener listener) {
        Log.i(TAG, "Reader: Start Thread");
        synchronized (this) {
            inputBlockSize = block;
            inputBuffer = new short[2][inputBlockSize];
            inputBufferWhich = 0;
            inputBufferIndex = 0;
            inputListener = listener;
            running = true;
            readerThread = new Thread(this, "Audio Reader");
            readerThread.start();
        }
    }
    

    /**
     * Start this reader.
     */
    public void stopReader() {
        Log.i(TAG, "Reader: Signal Stop");
        synchronized (this) {
            running = false;
        }
        try {
            if (readerThread != null)
                readerThread.join();
        } catch (InterruptedException e) {
            ;
        }
        readerThread = null;
        Log.i(TAG, "Reader: Thread Stopped");
    }


    // ******************************************************************** //
    // Main Loop.
    // ******************************************************************** //
    
    /**
     * Main loop of the audio reader.  This runs in our own thread.
     */
    @Override
    public void run() {
        short[] buffer;
        int index, readSize;

        try {
            int astate = audioInput.getState();
            audioInput.startRecording();
            Log.i(TAG, "Reader: Start Recording (" + astate +
                                " -> " + audioInput.getState() + ")");
            while (running) {
                synchronized (this) {
                    if (!running)
                        break;
                    readSize = inputBlockSize;
                    int space = inputBlockSize - inputBufferIndex;
                    if (readSize > space)
                        readSize = space;
                    buffer = inputBuffer[inputBufferWhich];
                    index = inputBufferIndex;
                }

                int nread = audioInput.read(buffer, index, readSize);

                boolean done = false;
                synchronized (this) {
                    if (!running)
                        break;
                    int end = inputBufferIndex + nread;
                    if (end >= inputBlockSize) {
                        inputBufferWhich = (inputBufferWhich + 1) % 2;
                        inputBufferIndex = 0;
                        done = true;
                    } else
                        inputBufferIndex = end;
                }

                if (done)
                    readDone(buffer);
            }
        } finally {
            Log.i(TAG, "Reader: Stop Recording");
            audioInput.stop();
        }
    }

    
    /**
     * Notify the client that a read has completed.
     * 
     * @param   buffer      Buffer containing the data.
     */
    private void readDone(short[] buffer) {
        inputListener.onReadComplete(buffer);
    }
    

    // ******************************************************************** //
    // Class Data.
    // ******************************************************************** //

    // Debugging tag.
	@SuppressWarnings("unused")
	private static final String TAG = "WindMeter";

    // Audio sample rate, in samples/sec.
    private static final int SAMPLE_RATE = 8000;

	
	// ******************************************************************** //
	// Private Data.
	// ******************************************************************** //
    
    // Our audio input device.
    private final AudioRecord audioInput;
    
    // Size of the system audio buffer.
    private final int audioBufferBytes;

    // Our audio input buffer, and the index of the next item to go in.
    private short[][] inputBuffer = null;
    private int inputBufferWhich = 0;
    private int inputBufferIndex = 0;

    // Size of the block to read each time.
    private int inputBlockSize = 0;
    
    // Listener for input.
    private Listener inputListener = null;
    
    // Flag whether the thread should be running.
    private boolean running = false;
    
    // The thread, if any, which is currently reading.  Null if not running.
    private Thread readerThread = null;
}

