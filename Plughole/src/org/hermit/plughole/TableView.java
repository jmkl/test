
/**
 * Plughole: a rolling-ball accelerometer game.
 * <br>Copyright 2008-2010 Ian Cameron Smith
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


package org.hermit.plughole;

import org.hermit.android.core.SurfaceRunner;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.View;
import android.widget.TextView;


/**
 * This class embodies the logic of the playing table.  It handles all
 * the physics.
 */
class TableView
	extends SurfaceRunner
	implements SurfaceHolder.Callback, SensorEventListener
{

    // ******************************************************************** //
    // Public Constants.
    // ******************************************************************** //

	/*
	 * State-tracking constants
	 */
	public static enum State {
		COLD,			// Brand new start-up.
		READY,			// Ready to play.
		RUNNING,		// Actually running.
		PAUSE,			// Game paused.
		LOSE,			// Lost --transient, goes to READY.
		WIN;			// Won --transient, goes to READY.
	}
	

    // ******************************************************************** //
    // Constructor.
    // ******************************************************************** //

	/**
	 * Create a Table instance.
	 * 
	 * @param	app				The application context we're running in.
	 * @param	lman			The level manager.
	 */
	public TableView(Plughole app, LevelManager lman)
	{
        super(app, LOOPED_TICKER);

        sensorManager = (SensorManager) app.getSystemService(Context.SENSOR_SERVICE);

		levelManager = lman;
        
        // Create a Handler for messages to set the text overlay.
		overlayHandler = new Handler() {
            @Override
            public void handleMessage(Message m) {
            	Bundle data = m.getData();
            	int vis = data.getInt("viz");
            	String msg = data.getString("text");
                Log.i(TAG, "Overlay: set vis " + vis +
             		   				" (" + (msg == null ? "" : msg) + ")");
                textOverlay.setVisibility(vis);
                textOverlay.setText(msg);
            }
        };

		appContext = app;
		appResources = app.getResources();
		gameState = State.COLD;
		
		// Make a Topology to represent the table layout.
		tableTopo = new Topology(app, this);

        // Initialise the paint we use for drawing.
        drawPaint = new Paint();
        drawPaint.setAntiAlias(true);
        
        // Create working variables.
    	bounceResults = new Topology.Reflect();
    	triggerResults = new Topology.Intersect();
    	
    	// Start at the first level.
    	currentLevelIndex = 0;
    	
    	// We want focus.
    	setFocusable(true);
    	setFocusableInTouchMode(true);
    	
    	Log.i(TAG, "Table: created");
	}


    /**
     * Installs a pointer to the text view used for messages.
     * 
     * TODO: just move the handler up.
     * 
     * @param   textView        The TextView for messages.
     */
    public void setTextView(TextView textView) {
        textOverlay = textView;
    }
    
    
    // ******************************************************************** //
    // Client Methods.
    // ******************************************************************** //

    /**
     * The application is starting.  Perform any initial set-up prior to
     * starting the application.  We may not have a screen size yet,
     * so this is not a good place to allocate resources which depend on
     * that.
     */
    @Override
    protected void appStart() {
		startTable();
    }
    

    /**
     * Set the screen size.  This is guaranteed to be called before
     * animStart(), but perhaps not before appStart().
     * 
     * @param   width       The new width of the surface.
     * @param   height      The new height of the surface.
     * @param   config      The pixel format of the surface.
     */
    @Override
    protected void appSize(int width, int height, Bitmap.Config config) {
    	canvasWidth = width;
    	canvasHeight = height;

    	// Configure the topology to the new size.
    	try {
    		tableTopo.setTableSize(width, height);
    	} catch (LevelReader.LevelException e) {
    		appContext.reportException(e);
    		return;
    	}

    	// Create the pixmap for the background image.
    	backgroundImage = getBitmap();
    }
    

    /**
     * We are starting the animation loop.  The screen size is known.
     * 
     * <p>doUpdate() and doDraw() may be called from this point on.
     */
    @Override
    protected void animStart() {
        // Register for sensor updates.
        Sensor sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        if (sensor != null)
            sensorManager.registerListener(this, sensor,
                                           SensorManager.SENSOR_DELAY_GAME);
    }
    

    /**
     * We are stopping the animation loop, for example to pause the app.
     * 
     * <p>doUpdate() and doDraw() will not be called from this point on.
     */
    @Override
    protected void animStop() {
		stopTable();

        // Stop sensor updates.
        sensorManager.unregisterListener(this);
    }
    

    /**
     * The application is closing down.  Clean up any resources.
     */
    @Override
    protected void appStop() {
    }
    

	// ******************************************************************** //
    // Table State Control.
    // ******************************************************************** //
	
	/**
	 * Set the current recline angle.  This re-calibrates our idea of
	 * horizontal, so the player can hold the device tilted back at the
	 * set angle.
	 * 
	 * @param	angle			The new recline angle in degrees.
	 */
	void setReclineAngle(int angle) {
		reclineRadians = (float) Math.toRadians(angle);
	}


	/**
	 * Enable or disable the display of performance stats on-screen.
	 * 
	 * @param	show			Show stats iff true.
	 */
	void setShowPerf(boolean show) {
        setDebugPerf(show);
	}


	/**
	 * Start the game table running.  All the conditions we need to
	 * run are present (surface, size, resumed).
	 */
	private void startTable() {
		State state = State.READY;

		synchronized (this) {
			if (gameState == State.COLD) {
				if (restoringState != null)
					state = loadIcicle(restoringState);
			} else
				state = gameState;

			// Set the state, which may be the saved state, but don't run yet.
			// If there was a game running or paused, go to pause, else ready.
			if (state == State.RUNNING || state == State.PAUSE)
				setState(State.PAUSE, 0);
			else
				setState(State.READY, 0);
		}
	}


	/**
	 * Restore the game state from the given Bundle.
	 * 
	 * @param	icicle		The Bundle containing the saved state.
	 * @return				The saved game state.
	 */
	private State loadIcicle(Bundle icicle) {
		// See if we can get the game state.
		State state = null;
		try {
			state = State.valueOf(icicle.getString("gameState"));
		}
		catch (IllegalArgumentException e) { }
		catch (NullPointerException e) { }
		int level = icicle.getInt("currentLevelIndex");

		// If no valid basic state, give up.
		if (state == null)
			return State.READY;

		// Load the level.
		try {
			levelManager.gotoLevel(level);
			loadLevel();

			tiltXAccel = 0;
			tiltYAccel = 0;

			ballX = icicle.getDouble("ballX");
			ballY = icicle.getDouble("ballY");
			ballVelX = icicle.getDouble("ballVelX");
			ballVelY = icicle.getDouble("ballVelY");

			clockTotalTime = icicle.getLong("clockTotalTime");
			clockLastStart = icicle.getLong("clockLastStart");
			clockLastTime = icicle.getLong("clockLastTime");
		} catch (LevelReader.LevelException e) {
			Log.e(TAG, "Table: " + e.getMessage());
			appContext.reportException(e);
			return State.READY;
		}

		return state;
	}


	/**
	 * Stop the game table running.  Our surface may have been destroyed, so
	 * stop all accesses to it.
	 */
	private void stopTable() {
        surfaceStop();
	}
	
	
	/**
	 * Pause the game (if we're playing).
	 */
	void pause() {
		synchronized (this) {
			if (gameState == State.RUNNING)
				setState(State.PAUSE, 0);
		}
	}


	/**
	 * The "pause/play button" has been pushed.  Take action appropriately.
	 * 
	 * <p>Note that this is typically called on the UI thread.
	 */
	private void pausePlay() {
		synchronized (this) {
			if (gameState == State.READY || gameState == State.LOSE || gameState == State.WIN)
				newGame();
			else if (gameState == State.PAUSE)
				setState(State.RUNNING, 0);
			else if (gameState == State.RUNNING)
				setState(State.PAUSE, 0);
		}
	}
	

	/**
	 * Start a new game.
	 */
	void newGame() {
		synchronized (this) {
			if (!isEnabled())
				return;
			Log.i(TAG, "Table: new game");

			try {
				loadLevel();
			} catch (LevelReader.LevelException e) {
				Log.e(TAG, "Table: " + e.getMessage());
				appContext.reportException(e);
				return;
			}

			// Draw the topology into the background.
			drawTopology(backgroundImage);

			clockTotalTime = currentLevelData.getTime();
			clockLastTime = clockTotalTime;
			setState(State.RUNNING, 0);
		}
	}


	private void loadLevel() throws LevelReader.LevelException {
		final LevelData level = levelManager.loadLevel(tableTopo.getTransform());
		tableTopo.setLevel(level);
		currentLevelData = level;
		
		// Load the ball image.
		ballImage = tableTopo.getBallImage();
		ballCX = ballImage.getWidth() / 2;
		ballCY = ballImage.getHeight() / 2;

		// Set the initial ball position.
		Point sp = level.getStart();
		ballX = sp.x;
		ballY = sp.y;
		ballVelX = 0;
		ballVelY = 0;
		Log.i(TAG, "Table: start " + ballX + "," + ballY);
		
		// Get the level name display, if any, and set its text up.
		Object levobj = level.getById("level");
		if (levobj == null) {
			levelDisplay = null;
		} else {
			if (!(levobj instanceof Display))
				throw new LevelReader.LevelException("Object \"level\" must" +
														" be a <Display>");
			levelDisplay = (Display) levobj;
			levelDisplay.setText(level.getDisplayName());
		}
	}
	

    /**
     * Restart the level.
     */
	void restart() {
		synchronized (this) {
			if (gameState == State.RUNNING || gameState == State.PAUSE)
				setState(State.LOSE, 0);
			setState(State.READY, 0);
		}
	}


	/**
	 * Sets a deferred game state.  This is safe to call from the
	 * drawing loop.
	 * 
	 * @param	state				New state to set.
	 * @param	prefixid			Resource ID of a message to display; 0
	 * 								for just the basic message for the state.
	private void postState(State state, int prefixid) {
		synchronized (this) {
			nextState = state;
			nextPrefix = prefixid;
		}
	}
	 */


	/**
	 * Sets the game mode.  That is, whether we are running, paused, in the
	 * failure state, in the victory state, etc.
	 * 
	 * @param	state				New state to set.
	 * @param	prefixid			Resource ID of a message to display; 0
	 * 								for just the basic message for the state.
	 */
	private void setState(State state, int prefixid) {
		synchronized (this) {
			Log.i(TAG, "Table: set state " + state);

			/*
			 * This method optionally can cause a text message to be displayed
			 * to the user when the mode changes. Since the View that actually
			 * renders that text is part of the main View hierarchy and not
			 * owned by this thread, we can't touch the state of that View.
			 * Instead we use a Message + Handler to relay commands to the main
			 * thread, which updates the user-text View.
			 */
			gameState = state;

			if (gameState == State.RUNNING) {
				hideMessage();

		        surfaceStart();

				// Make sure the game board is on screen.
				// FIXME: refreshScreen();

				// Start the game clock from where it left off.
				long now = System.currentTimeMillis();
				clockLastStart = now;

				// Track frame time for animation and physics.
				lastFrameTime = now;
			} else {
		        surfaceStop();

				// Make sure the game board is on screen.
		        // FIXME: refreshScreen();

				// Save the current state of the game clock.
				long now = System.currentTimeMillis();
				clockLastTime -= now - clockLastStart;
				clockLastStart = now;

				int msgid = 0;
				if (gameState == State.READY)
					msgid = R.string.mode_ready;
				else if (gameState == State.PAUSE)
					msgid = R.string.mode_pause;
				else if (gameState == State.LOSE) {
					msgid = R.string.mode_lose;
					gameState = State.READY;
				} else if (gameState == State.WIN) {
					msgid = R.string.mode_win;
					gameState = State.READY;
					levelManager.nextLevel();
				}

				showMessage(msgid, prefixid);
			}
		}
	}
	
	
	private void showMessage(int msgid, int prefixid) {
		String text = appResources.getString(msgid);
		if (prefixid != 0)
			text = appResources.getString(prefixid) + "\n" + text;
		
    	Bundle b = new Bundle();
    	b.putString("text", text);
    	b.putInt("viz", View.VISIBLE);
    	
    	Message msg = overlayHandler.obtainMessage();
    	msg.setData(b);
    	overlayHandler.sendMessage(msg);
	}
	

	private void hideMessage() {
		Bundle b = new Bundle();
		b.putString("text", "");
		b.putInt("viz", View.INVISIBLE);
		
		Message msg = overlayHandler.obtainMessage();
		msg.setData(b);
		overlayHandler.sendMessage(msg);
	}


	LevelData getLevel() {
		return currentLevelData;
	}

	
    // ******************************************************************** //
    // User Input.
    // ******************************************************************** //

    /**
	 * Handle key input.
	 * 
     * @param	keyCode		The key code.
     * @param	event		The KeyEvent object that defines the
     * 						button action.
     * @return				True if the event was handled, false otherwise.
	 */
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
    	return false;
    }
    
    
    /**
	 * Handle touchscreen input.
	 * 
     * @param	event		The MotionEvent object that defines the action.
     * @return				True if the event was handled, false otherwise.
	 */
    @Override
    public boolean onTouchEvent(MotionEvent event) {
		Log.i(TAG, "Table: touch");

    	int action = event.getAction();
    	switch (action) {
    	case MotionEvent.ACTION_DOWN:
    		pausePlay();
    		return true;
    	case MotionEvent.ACTION_UP:
    	case MotionEvent.ACTION_MOVE:
    	case MotionEvent.ACTION_CANCEL:
    	default:
    		break;
    	}
    	
		return false;
    }

    
    // ******************************************************************** //
    // Sensor Handling.
    // ******************************************************************** //

    /**
     * Called when sensor values have changed.  The length and contents
     * of the values array vary depending on which sensor is being monitored.
     * 
     * <p>This is called on the sensor service's thread.
     *
	 * @param	event			The sensor event.
     */
	public void onSensorChanged(SensorEvent event) {
		int sensor = event.sensor.getType();
		if (sensor != Sensor.TYPE_ACCELEROMETER)
			return;
		
		final float[] values = event.values;
		float x = values[0];
		float y = values[1];
		float z = values[2];
		
		// Rotate the Y value in the Y-Z plane, according to the user's
		// recline prefs.
		final float cos = (float) Math.cos(reclineRadians);
		final float sin = (float) Math.sin(reclineRadians);
		y = y * cos - z * sin;

		// Save the X and Y accelerations.
		// We invert both axes to get the direction of down, then
		// invert the Y axis again.  The accelerometer uses
		// +Y = up (towards earpiece), whereas the graphics system
		// uses +Y = down.
		synchronized (this) {
			tiltXAccel = -x;
			tiltYAccel = y;
		}
	}


	/**
	 * Called when the accuracy of a sensor has changed.
	 * 
	 * @param	sensor			The sensor being monitored.
	 * @param	accuracy		The new accuracy of this sensor.
	 */
	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		// Don't need this.
	}

	
    // ******************************************************************** //
    // Physics.
    // ******************************************************************** //

	/**
     * Update the state of the application for the current frame.
     * 
     * <p>Applications must override this, and can use it to update
     * for example the physics of a game.  This may be a no-op in some cases.
     * 
     * <p>doDraw() will always be called after this method is called;
     * however, the converse is not true, as we sometimes need to draw
     * just to update the screen.  Hence this method is useful for
     * updates which are dependent on time rather than frames.
     * 
     * @param   now         Current time in ms.
	 */
    @Override
    protected void doUpdate(long now) {
		double elapsed = (now - lastFrameTime) / 1000.0;
		lastFrameTime = now;

		// Now reduce the base accelerations by a fixed value to
		// introduce friction -- small accelerations will become zero.
		double accelX, accelY;
		synchronized (this) {
			accelX = tiltXAccel * PHYS_PIXELS_PER_METRE;
			accelY = tiltYAccel * PHYS_PIXELS_PER_METRE;
		}
		if (accelX > PHYS_ACCEL_FRICTION)
			accelX -= PHYS_ACCEL_FRICTION;
		else if (accelX < -PHYS_ACCEL_FRICTION)
			accelX += PHYS_ACCEL_FRICTION;
		else
			accelX = 0;
		if (accelY > PHYS_ACCEL_FRICTION)
			accelY -= PHYS_ACCEL_FRICTION;
		else if (accelY < -PHYS_ACCEL_FRICTION)
			accelY += PHYS_ACCEL_FRICTION;
		else
			accelY = 0;

		// Subtract friction from our current velocity.
		final double fric = PHYS_VEL_FRICTION * elapsed;
		if (ballVelX > fric)
			ballVelX -= fric;
		else if (ballVelX < -fric)
			ballVelX += fric;
		else
			ballVelX = 0;
		if (ballVelY > fric)
			ballVelY -= fric;
		else if (ballVelY < -fric)
			ballVelY += fric;
		else
			ballVelY = 0;

		// The fraction of the move we are currently processing.  This
		// gets split if we bounce.
		double fraction = 1.0;
		int bounces = 0;
		double startX = ballX;
		double startY = ballY;

		do {
			double doFraction = fraction;
			
			// Calculate a rough idea of the distance we'll travel this segment.
			final double cx = elapsed * ballVelX * doFraction;
			final double cy = elapsed * ballVelY * doFraction;
			final double cdist = Math.sqrt(cx * cx + cy * cy);
			
			// Change the fraction we're doing this time round to make the move
			// no more than about 1.5 pixel.
			if (cdist > 1)
				doFraction *= 0.8 / cdist;
			
			// Take into account any zone we may be in, as this may
			// affect acceleration.
			Action action;
			if ((action = tableTopo.zone(ballX, ballY)) != null) {
				if (doAction(action, true, elapsed * doFraction))
					return;
			}
			
			// Change in velocity this turn is accel times elapsed time times
			// the fraction of the move we're currently looking at.
			double ddx = accelX * elapsed * doFraction;
			double ddy = accelY * elapsed * doFraction;

			double dxOld = ballVelX;
			double dyOld = ballVelY;
			ballVelX += ddx;
			ballVelY += ddy;

			// OK, now we can calculate our next position.
			final double ballOx = ballX;
			final double ballOy = ballY;
			final double ballDx = elapsed * (ballVelX + dxOld) / 2 * doFraction;
			final double ballDy = elapsed * (ballVelY + dyOld) / 2 * doFraction;
			ballX += ballDx;
			ballY += ballDy;

			// See if our proposed motion would hit anything.
			if (tableTopo.reflect(ballOx, ballOy, ballX, ballY, bounceResults)) {
				// If we've bounced too many times, there's no point in going
				// on.  Leave the ball at the start position and kill its speed.
				if (++bounces > MAX_BOUNCES) {
					ballX = startX;
					ballY = startY;
					ballVelX = 0;
					ballVelY = 0;
					break;
				}
				
				// We bounced.  Move the ball to the point of reflection,
				// and change the motion vector to the reflected direction.
				ballX = bounceResults.interX;
				ballY = bounceResults.interY;

				// Calculate the post-bounce velocity vector based on speed
				// reduced by the bounce, which in turn depends on the
				// impact angle.
				double speed = Math.sqrt(ballVelX * ballVelX + ballVelY * ballVelY);
				final double impact = Math.sin(Math.toRadians(bounceResults.angle));
				final double ispeed = speed * impact;
				speed -= ispeed * PHYS_BOUNCE_DECEL;
				ballVelX = bounceResults.directionX * speed;
				ballVelY = bounceResults.directionY * speed;
				
				// Kick the vibrator.
				if (ispeed >= MIN_VIBE_SPEED)
					appContext.kickVibe(Math.min(ispeed / FULL_VIBE_SPEED, 1));
				
				// Calculate the fraction of this timeslice which we actually
				// did before the impact.
				doFraction *= bounceResults.fraction;
				
				// Does this trigger an action?  If so, does that action stop
				// play?
				if (bounceResults.action != null)
					if (doAction(bounceResults.action, true, 0))
						return;
			}

			fraction -= doFraction;
		} while (fraction > 0);
        
        // Does this move trigger an action?  If so, does that action stop
        // play?
        if (tableTopo.intersect(startX, startY, ballX, ballY, triggerResults))
            if (doAction(triggerResults.action, triggerResults.inward, 0))
                return;

		// See if we fell off the table.
		if (ballX < 0 || ballX >= canvasWidth ||
										ballY < 0 || ballY >= canvasHeight)
			setState(State.LOSE, R.string.message_fell_off);
	}


	/**
	 * Carry out the given action, and determine whether it stops play.
	 * 
	 * @param	act			The Action to perform.
     * @param   inward      True iff this is the "on"-direction trigger
     *                      for this action.
	 * @param	time		The time in seconds we're acting over.
	 * @return				true iff play is stopped; false to continue.
	 */
	private boolean doAction(Action act, boolean inward, double time) {
	    if (!inward)
	        return false;
	    
		if (!onSurfaceThread())
			throw new IllegalStateException("doAction() called off surface thread");
		
		final int msgid = act.getMessageId();
		switch (act.getType()) {
        case SPEED:
            ballVelX = ballVelX * act.getAccelMag();
            ballVelY = ballVelY * act.getAccelMag();
            return false;
		case ACCEL:
			double speed = Math.sqrt(ballVelX * ballVelX + ballVelY * ballVelY);
			ballVelX = ballVelX * 0.85 + speed * act.getAccelX() * 0.15;
			ballVelY = ballVelY * 0.85 + speed * act.getAccelY() * 0.15;
			return false;
		case TELEPORT:
			Object dest = act.getTarget();
			if (dest != null && dest instanceof Point) {
				ballX = ((Point) dest).x;
				ballY = ((Point) dest).y;
			}
			return false;
        case OFF:
        case ON:
        case ONOFF:
            Object obj = act.getTarget();
            if (obj != null && obj instanceof ForceField)
                ((ForceField) obj).activate(act.getType());
            return false;
		case WIN:
			setState(State.WIN, msgid);
			return true;
		case LOSE:
			setState(State.LOSE, msgid);
			return true;
		}
		
		return false;
	}


    // ******************************************************************** //
    // Drawing.
    // ******************************************************************** //

	/**
	 * Draw the base game board into the given bitmap.
	 */
	private void drawTopology(Bitmap img) {
		if (currentLevelData == null)
			return;
			
		Canvas canvas = new Canvas(img);
		tableTopo.drawFixed(canvas);
	}
	

	/**
     * Draw the current frame of the application.
     * 
     * <p>Applications must override this, and are expected to draw the
     * entire screen into the provided canvas.
     * 
     * <p>This method will always be called after a call to doUpdate(),
     * and also when the screen needs to be re-drawn.
     * 
     * @param   canvas      The Canvas to draw into.
     * @param   now         Current time in ms.  Will be the same as that
     *                      passed to doUpdate(), if there was a preceeding
     *                      call to doUpdate().
	 */
	@Override
    protected void doDraw(Canvas canvas, long now) {
		if (currentLevelData == null)
			return;
			
		// Draw the background image.
		canvas.drawBitmap(backgroundImage, 0, 0, null);
		
		// Draw all the animations into the board.
		long clock = clockLastTime - (now - clockLastStart);
		tableTopo.drawAnim(canvas, clockTotalTime, clock);

		// Draw the ball.
		int xLeft = (int) ballX - ballCX;
		int yTop = (int) ballY - ballCY;
		canvas.drawBitmap(ballImage, xLeft, yTop, null);
	}


    // ******************************************************************** //
    // Save and Restore.
    // ******************************************************************** //

	/**
	 * Save the state of the game in the provided Bundle.
	 * 
     * @param   icicle      The Bundle to save our state in.
	 * @return				The Bundle with this view's state saved to it.
	 */
	public Bundle saveState(Bundle icicle) {
		synchronized (this) {
			if (icicle != null) {
				icicle.putString("gameState", gameState.toString());
				icicle.putInt("currentLevelIndex", currentLevelIndex);
				
				icicle.putDouble("ballX", ballX);
				icicle.putDouble("ballY", ballY);
				icicle.putDouble("ballVelX", ballVelX);
				icicle.putDouble("ballVelY", ballVelY);
				
				icicle.putLong("clockTotalTime", clockTotalTime);
				icicle.putLong("clockLastStart", clockLastStart);
				icicle.putLong("clockLastTime", clockLastTime);
			}
		}
		return icicle;
	}


	/**
	 * Restore the game state from the given Bundle.
	 * 
	 * @param	icicle		The Bundle containing the saved state.
	 */
	public void restoreState(Bundle icicle) {
		pause();
		synchronized (this) {
			// Save it for when we get started.
			restoringState = icicle;
		}
	}

	
    // ******************************************************************** //
    // Class Data.
    // ******************************************************************** //

    // Debugging tag.
	private static final String TAG = "plughole";

	// Acceleration scaling value.  We scale a real-world acceleration
	// caused by the handset's tilt in to a pixels/s² acceleration
	// by multiplying by this value.
	private static final int PHYS_PIXELS_PER_METRE = 200;

	// Acceleration friction in pixels/s² -- this amount is subtracted
	// from the absolute base acceleration, yielding a value of zero
	// for small accelerations.
	private static final double PHYS_ACCEL_FRICTION = 0.0;
	
	// Velocity friction, as a deceleration in pixels/s² -- this much
	// is subtracted from the velocity each second.
	private static final double PHYS_VEL_FRICTION = 1.5;
	
	// The fraction of the velocity we lose each time we bounce at
	// right angles to a surface.  Energy loss is less at shallower angles.
	private static final double PHYS_BOUNCE_DECEL = 0.4;

	// The maximum number of times we will bounce during a single bounce
	// check.  Used to make sure we don't loop forever, e.g. if two walls
	// are zero distance apart.
	private static final int MAX_BOUNCES = 5;
	
	// Minimum speed at which we will activate the vibrator.  Mustn't
	// set this too low, or we will be kicking it for every pixel we
	// roll along a wall.
	private static final long MIN_VIBE_SPEED = 120;

	// Ball speed which is considered flat-out for the purpose of hitting
	// the vibrator.
	private static final long FULL_VIBE_SPEED = 400;

	
    // ******************************************************************** //
    // Private Data.
    // ******************************************************************** //

    // The sensor manager, which we use to interface to all sensors.
    private SensorManager sensorManager;

	// Application we're running in.
	private Plughole appContext;

	// Application resources for this app.
	private Resources appResources;
    
    // Level manager.
    private LevelManager levelManager;

    // The text view we use to display the game state when paused etc.
    private TextView textOverlay;

	// If we're restoring, the saved state we're restoring.
	private Bundle restoringState = null;

	/** Message handler used by thread to interact with TextView */
	private Handler overlayHandler;

	// The size in pixels of the total playing area.
	private int canvasWidth = 1;
	private int canvasHeight = 1;
	
	// Topology representing the table layout.
	private Topology tableTopo;

	// The background image of the playing board.
	private Bitmap backgroundImage = null;

	// The ball image.
	private Bitmap ballImage;

    // Painter for drawing the current board state on screen.
    private Paint drawPaint;
    
    // Current recline angle in RADIANS.
	private float reclineRadians = 0;

	// The level we're currently playing.  Null if no level loaded yet.
	private int currentLevelIndex = -1;
	private LevelData currentLevelData = null;
	
	// The current state of the game.
	private State gameState;

	/** Used to figure out elapsed time between frames */
	private long lastFrameTime;

	// X and Y position of the centre of the ball within the ball image.
	private int ballCX;
	private int ballCY;

	// Pixel position of the centre of the ball.
	private double ballX;
	private double ballY;

	// The ball's current velocity in X and Y, in pixels per second.
	private double ballVelX;
	private double ballVelY;

	// The current handset tilt, as the acceleration in X and Y, in m/s².
	private float tiltXAccel;
	private float tiltYAccel;
	
	// Display in which the level name is drawn.  Null means no level name.
	private Display levelDisplay = null;

	// The game clock.  For accuracy, we don't try to decrement the clock on
	// each tick -- granularity would be an issue.  Instead we track time
	// since the clock was last restarted.  clockLastStart is the time
	// it was last started or stopped, and clockLastTime is the time in ms
	// that was left then.
	private long clockTotalTime = 0;
	private long clockLastStart = 0;
	private long clockLastTime = 0;

	// Working variables used during bounce calculation.  Allocating them
	// once is significantly faster.
	private Topology.Reflect bounceResults;

    // Working variables used during intersection calculation.  Allocating them
    // once is significantly faster.
    private Topology.Intersect triggerResults;

}

