
/**
 * Plughole: a rolling-ball accelerometer game.
 *
 *   This program is free software; you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License version 2
 *   as published by the Free Software Foundation (see COPYING).
 * 
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 */


package org.hermit.plughole;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.View;


/**
 * This class embodies the logic of the playing table.  It handles all
 * the physics.
 */
class Table {

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
	 * @param	holder			The surface holder for our view.
	 * @param	handler			A message handler to which we can send
	 * 							text overlay requests.
	 */
	public Table(Plughole app, SurfaceHolder holder, LevelManager lman, Handler handler)
	{
		surfaceHolder = holder;
		levelManager = lman;
		overlayHandler = handler;

		appContext = app;
		appResources = app.getResources();
		enableFlags = 0;
		gameState = State.COLD;
		
		// Make a Topology to represent the table layout.
		tableTopo = new Topology(app, this);

        // Initialise the paint we use for drawing.
        drawPaint = new Paint();
        drawPaint.setAntiAlias(true);
        
        // Create working variables.
    	bounceResults = new Topology.Intersect();
    	
    	// Start at the first level.
    	currentLevelIndex = 0;
    	
    	Log.i(TAG, "Table: created");
	}


	// ******************************************************************** //
    // Accessors.
    // ******************************************************************** //
	
	/**
	 * Get the board width.
	 * 
	 * @return				The width of the board.  Zero if not known yet.
	 */
	public double getWidth() {
		return canvasWidth;
	}


	/**
	 * Get the board height.
	 * 
	 * @return				The height of the board.  Zero if not known yet.
	 */
	public double getHeight() {
		return canvasHeight;
	}


	// ******************************************************************** //
    // App State Control.
    // ******************************************************************** //

	/**
	 * Enable the game to play.  This is called after our surface is
	 * created.  Until this is called, it is not safe to proceed.
	 */
	void surfaceCreated() {
        Log.i(TAG, "Table: surfaceCreated");
        setEnable(ENABLE_SURFACE);
	}


	/**
	 * Shut down the game.  This is called when our surface is
	 * being destroyed.  WARNING: after this method returns, the
	 * Surface/Canvas must never be touched again!
	 */
	void surfaceDestroyed() {
        Log.i(TAG, "Table: surfaceDestroyed");
        clearEnable(ENABLE_SURFACE);
	}
	

	/**
	 * We're resuming the app.  Get ready to go.  We don't actually
	 * go to RUNNING because the user should do that when ready.
	 */
	void onResume() {
        Log.i(TAG, "Table: onResume");
        setEnable(ENABLE_RESUMED);
	}


	/**
	 * We're pausing the app.
	 */
	void onPause() {
        Log.i(TAG, "Table: onPause");
        clearEnable(ENABLE_RESUMED);
	}

	
    /**
     * This is called immediately after the surface size changes.
     * This method is always called at least once.
     * 
     * @param	format		The new PixelFormat of the surface.
     * @param	width		The new width of the surface.
     * @param	height		The new height of the surface.
     */
	public void setSurfaceSize(int format, int width, int height) {
        Log.i(TAG, "Table: set size " + width + "x" + height);
        
        setSize(format, width, height);
        setEnable(ENABLE_SIZE);
	}

	
	/**
	 * Set the given enable flag, and see if we're good to go.
	 * 
	 * @param	flag		The flag to set.
	 */
	private void setEnable(int flag) {
		boolean enabled1 = false;
		boolean enabled2 = false;
		synchronized (surfaceHolder) {
			enabled1 = (enableFlags & ENABLE_ALL) == ENABLE_ALL;
			enableFlags |= flag;
			enabled2 = (enableFlags & ENABLE_ALL) == ENABLE_ALL;
		}

		// Are we all set?
		if (!enabled1 && enabled2) {
			startTable();
		}
	}


	/**
	 * Clear the given enable flag, and see if we need to shut down.
	 * 
	 * @param	flag		The flag to clear.
	 */
	private void clearEnable(int flag) {
		boolean enabled1 = false;
		boolean enabled2 = false;
		synchronized (surfaceHolder) {
			enabled1 = (enableFlags & ENABLE_ALL) == ENABLE_ALL;
			enableFlags &= ~flag;
			enabled2 = (enableFlags & ENABLE_ALL) == ENABLE_ALL;
		}

		// Do we need to stop?
		if (enabled1 && !enabled2) {
			stopTable();
		}
	}


	/**
	 * Determine whether we are currently fully enabled.
	 * 
	 * @return				true iff all enable conditions are set.
	 */
	private boolean isEnabled() {
		synchronized (surfaceHolder) {
			return (enableFlags & ENABLE_ALL) == ENABLE_ALL;
		}
	}

	
	// ******************************************************************** //
    // Table State Control.
    // ******************************************************************** //
	
    /**
     * Set the size of the table.
     * 
     * @param	format		The new PixelFormat of the surface.
     * @param	width		The new width of the surface.
     * @param	height		The new height of the surface.
     */
	private void setSize(int format, int width, int height) {
		synchronized (surfaceHolder) {
			canvasWidth = width;
			canvasHeight = height;

			// Configure the topology to the new size.
			try {
				tableTopo.setTableSize(width, height);
			} catch (LevelReader.LevelException e) {
				appContext.reportError(e);
				return;
			}

			// Create the pixmap for the background image.
			Bitmap.Config config = null;
			switch (format) {
			case PixelFormat.A_8:
				config = Bitmap.Config.ALPHA_8;
				break;
			case PixelFormat.RGBA_4444:
				config = Bitmap.Config.ARGB_4444;
				break;
			case PixelFormat.RGBA_8888:
				config = Bitmap.Config.ARGB_8888;
				break;
			case PixelFormat.RGB_565:
				config = Bitmap.Config.RGB_565;
				break;
			default:
				config = Bitmap.Config.RGB_565;
			break;
			}
			backgroundImage = Bitmap.createBitmap(width, height, config);
		}
	}


	/**
	 * Enable or disable the display of performance stats on-screen.
	 * 
	 * @param	show			Show stats iff true.
	 */
	void setShowPerf(boolean show) {
		synchronized (surfaceHolder) {
			showPerf = show;
		}
	}


	/**
	 * Start the game table running.  All the conditions we need to
	 * run are present (surface, size, resumed).
	 */
	private void startTable() {
		State state = State.READY;

		synchronized (surfaceHolder) {
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
			appContext.reportError(e);
			return State.READY;
		}

		return state;
	}


	/**
	 * Stop the game table running.  Our surface may have been destroyed, so
	 * stop all accesses to it.
	 */
	private void stopTable() {
		// Kill the thread if it's running, and wait for it to die.
		// This is important when the surface is destroyed, as we can't
		// touch the surface after we return.
		Ticker ticker = null;
		synchronized (surfaceHolder) {
			ticker = animTicker;
		}
		if (ticker != null && ticker.isAlive())
			ticker.killAndWait();
	}
	
	
	/**
	 * Pause the game (if we're playing).
	 */
	void pause() {
		synchronized (surfaceHolder) {
			if (gameState == State.RUNNING)
				setState(State.PAUSE, 0);
		}
	}


	/**
	 * The "pause/play button" has been pushed.  Take action appropriately.
	 */
	void pausePlay() {
		synchronized (surfaceHolder) {
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
		synchronized (surfaceHolder) {
			if (!isEnabled())
				return;
			Log.i(TAG, "Table: new game");

			try {
				loadLevel();
			} catch (LevelReader.LevelException e) {
				Log.e(TAG, "Table: " + e.getMessage());
				appContext.reportError(e);
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
		
		// Get the performance info position.
		Object perfobj = level.getById("perf");
		if (perfobj == null) {
			perfPosX = 14;
			perfPosY = 20;
		} else {
			if (!(perfobj instanceof Display))
				throw new LevelReader.LevelException("Object \"perf\" must" +
														" be a <Display>");
			Display perfDisplay = (Display) perfobj;
			Point pos = perfDisplay.getCentre();
			perfPosX = (float) pos.x - 24;
			perfPosY = (float) pos.y;
		}
	}
	

    /**
     * Restart the level.
     */
	void restart() {
		synchronized (surfaceHolder) {
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
	 */
	private void postState(State state, int prefixid) {
		synchronized (surfaceHolder) {
			nextState = state;
			nextPrefix = prefixid;
		}
	}


	/**
	 * Cehck for and activate a deferred game state.
	 */
	private void checkState() {
		synchronized (surfaceHolder) {
			if (nextState != null) {
				setState(nextState, nextPrefix);
				nextState = null;
				nextPrefix = 0;
			}
		}
	}


	/**
	 * Sets the game mode.  That is, whether we are running, paused, in the
	 * failure state, in the victory state, etc.
	 * 
	 * @param	state				New state to set.
	 * @param	prefixid			Resource ID of a message to display; 0
	 * 								for just the basic message for the state.
	 */
	private void setState(State state, int prefixid) {
		synchronized (surfaceHolder) {
			if (!isEnabled())
				return;

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

				if (animTicker != null && animTicker.isAlive())
					animTicker.kill();
				Log.i(TAG, "Table: set running: start ticker");
				animTicker = new Ticker(this);

				// Make sure the game board is on screen.
				refreshScreen();

				// Start the game clock from where it left off.
				long now = System.currentTimeMillis();
				clockLastStart = now;

				// Track frame time for animation and physics.
				lastFrameTime = now;
			} else {
				if (animTicker != null && animTicker.isAlive())
					animTicker.kill();

				// Make sure the game board is on screen.
				refreshScreen();

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
	 * Process the tilt of the device, and use it to swivel the lander.
	 * 
	 * @param xtilt		Acceleration in the X direction.  -1g .. 1g.
	 * @param ytilt		Acceleration in the Y direction.  -1g .. 1g.
	 */
	void setTilt(float xtilt, float ytilt) {
		synchronized (surfaceHolder) {
			if (gameState == State.RUNNING) {
				// We invert the Y axis here.  The accelerometer uses
				// +Y = up (towards earpiece), whereas the graphics system
				// uses +Y = down.
				tiltXAccel = xtilt;
				tiltYAccel = -ytilt;
			}
		}
	}
	

    // ******************************************************************** //
    // Run Control.
    // ******************************************************************** //

	public void tick() {
		Canvas canvas = null;
		synchronized (surfaceHolder) {
			try {
				canvas = surfaceHolder.lockCanvas(null);
				long now = System.currentTimeMillis();
				if (gameState == State.RUNNING) {
					updatePhysics(now);

					// If we're tracking performance, update the metrics.
					// Count microsecs, so we can display a number more than
					// 1 per second.  The granularity sucks but hopefully
					// it averages out.
					if (showPerf) {
						physTime += (System.currentTimeMillis() - now) * 1000;
						++physCount;
					}
				}

				doDraw(canvas, now);
			} finally {
				// do this in a finally so that if an exception is thrown
				// during the above, we don't leave the Surface in an
				// inconsistent state
				if (canvas != null)
					surfaceHolder.unlockCanvasAndPost(canvas);
			}
		}

		// Activate any state changes we fired.
		synchronized (surfaceHolder) {
			checkState();
		}
	}

	
    // ******************************************************************** //
    // Physics.
    // ******************************************************************** //

	/**
	 * Figures the lander state (x, y, fuel, ...) based on the passage of
	 * realtime.  Does not invalidate().  Called at the start of draw().
	 * Detects the end-of-game and sets the UI to the next state.
	 * 
	 * @param	now				Current system time in ms.
	 */
	void updatePhysics(long now) {
		double elapsed = (now - lastFrameTime) / 1000.0;
		lastFrameTime = now;

		// Now reduce the base accelerations by a fixed value to
		// introduce friction -- small accelerations will become zero.
		double accelX = tiltXAccel * PHYS_PIXELS_PER_METRE;
		if (accelX > PHYS_ACCEL_FRICTION)
			accelX -= PHYS_ACCEL_FRICTION;
		else if (accelX < -PHYS_ACCEL_FRICTION)
			accelX += PHYS_ACCEL_FRICTION;
		else
			accelX = 0;
		double accelY = tiltYAccel * PHYS_PIXELS_PER_METRE;
		if (accelY > PHYS_ACCEL_FRICTION)
			accelY -= PHYS_ACCEL_FRICTION;
		else if (accelY < -PHYS_ACCEL_FRICTION)
			accelY += PHYS_ACCEL_FRICTION;
		else
			accelY = 0;

		// Subtract friction from ou current velocity.
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
				if (doAction(action, elapsed * doFraction))
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
					if (doAction(bounceResults.action, 0))
						return;
			}
			
			fraction -= doFraction;
		} while (fraction > 0);

		// See if we fell off the table.
		if (ballX < 0 || ballX >= canvasWidth ||
										ballY < 0 || ballY >= canvasHeight)
			postState(State.LOSE, R.string.message_fell_off);
	}


	/**
	 * Carry out the given action, and determine whether it stops play.
	 * 
	 * @param	act			The Action to perform.
	 * @param	time		The time in seconds we're acting over.
	 * @return				true iff play is stopped; false to continue.
	 */
	private boolean doAction(Action act, double time) {
		final int msgid = act.getMessageId();
		switch (act.getType()) {
		case ACCEL:
			double speed = Math.sqrt(ballVelX * ballVelX + ballVelY * ballVelY);
			ballVelX = ballVelX * 0.85 + speed * act.getAccelX() * 0.15;
			ballVelY = ballVelY * 0.85 + speed * act.getAccelY() * 0.15;
			return false;
		case TELEPORT:
			Point dest = act.getTarget();
			if (dest != null) {
				ballX = dest.x;
				ballY = dest.y;
			}
			return false;
		case WIN:
			postState(State.WIN, msgid);
			return true;
		case LOSE:
			postState(State.LOSE, msgid);
			return true;
		}
		
		return false;
	}


    // ******************************************************************** //
    // Drawing.
    // ******************************************************************** //
	
	/**
	 * Draw the game board to the screen in its current state, as a one-off.
	 * This can be used to refresh the screen.
	 */
	private void refreshScreen() {
        Log.i(TAG, "Table: refreshScreen");

		// Draw the topology into the background.
		synchronized (surfaceHolder) {
			drawTopology(backgroundImage);
		}

		// Draw the whole board to the screen.
		Canvas canvas = null;
		try {
			canvas = surfaceHolder.lockCanvas(null);
			synchronized (surfaceHolder) {
				long now = System.currentTimeMillis();
				doDraw(canvas, now);
			}
		} finally {
			// do this in a finally so that if an exception is thrown
			// during the above, we don't leave the Surface in an
			// inconsistent state
			if (canvas != null)
				surfaceHolder.unlockCanvasAndPost(canvas);
		}
	}
	

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
	 * Draws the current state of the board to the provided Canvas.
	 * 
	 * @param	canvas			Canvas to draw on.
	 * @param	now				Current system time in ms.
	 */
	private void doDraw(Canvas canvas, long now) {
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
	
		// Show performance data, if required.
		if (showPerf) {
			// Count frames per second.
			++fpsSinceLast;
			
			// If it's time to make a new displayed total, tot up the figures
			// and reset the running counts.
			if (now - perfLastTime > 1000) {
				fpsLastCount = fpsSinceLast;
				fpsSinceLast = 0;
				physLastAvg = physCount == 0 ? 0 : physTime / physCount;
				physTime = 0;
				physCount = 0;
				perfLastTime = now;
			}
			
			// Draw the FPS and average physics time on screen.
			drawPaint.setColor(0xffff0000);
			canvas.drawText("" + fpsLastCount + " fps",
							perfPosX, perfPosY - 6, drawPaint);
			canvas.drawText("" + physLastAvg + " µs",
							perfPosX, perfPosY + 6, drawPaint);
		}
	}


    // ******************************************************************** //
    // Save and Restore.
    // ******************************************************************** //

	/**
	 * Save the state of the game in the provided Bundle.
	 * 
	 * @return					The Bundle with this view's state saved to it.
	 */
	public Bundle saveState(Bundle icicle) {
		synchronized (surfaceHolder) {
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
		synchronized (surfaceHolder) {
			// Save it for when we get started.
			restoringState = icicle;
		}
	}

	
    // ******************************************************************** //
    // Class Data.
    // ******************************************************************** //

    // Debugging tag.
	@SuppressWarnings("unused")
	private static final String TAG = "plughole";

	// Enable flags.  In order to run, we need onSurfaceCreated() and
	// onResume(), which can come in either order.  So we track which ones
	// we have by these flags.  When all are set, we're good to go.  Note
	// that this is distinct from the game state machine, and its pause
	// and resume actions -- the whole game is enabled by the combination
	// of these flags set in enableFlags.
	private static final int ENABLE_SURFACE = 0x01;
	private static final int ENABLE_SIZE = 0x02;
	private static final int ENABLE_RESUMED = 0x04;
	private static final int ENABLE_ALL =
						ENABLE_SURFACE | ENABLE_SIZE | ENABLE_RESUMED;

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
	
	// Application we're running in.
	private Plughole appContext;

	// Application resources for this app.
	private Resources appResources;
    
    // Level manager.
    private LevelManager levelManager;

	// Enablement flags; see comment above.
	private int enableFlags = 0;

	// If we're restoring, the saved state we're restoring.
	private Bundle restoringState = null;
	
	// The surface manager for the view.
	private SurfaceHolder surfaceHolder;

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

	// The level we're currently playing.  Null if no level loaded yet.
	private int currentLevelIndex = -1;
	private LevelData currentLevelData = null;
	
	// The current state of the game.
	private State gameState;

	// Deferred state and message to be set when convenient.
	private State nextState;
	private int nextPrefix;

    // The ticker which runs each frame of animation.
    private Ticker animTicker = null;

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

	// Display performance data on-screen.
	private boolean showPerf = false;
	
	// Position on screen where the performance info is drawn.
	private float perfPosX = 0;
	private float perfPosY = 0;

	// Data for counting frames per second.  Value displayed at last
	// update, time in system ms of last update, frames since last update.
	private int fpsLastCount = 0;
	private int fpsSinceLast = 0;
	
	// Data for monitoring physics performance.  We count the total number
	// of ms spent doing physics since last update, and number of physics
	// passes since last update; and keep the last displayed average time.
	private long physTime = 0;
	private int physCount = 0;
	private long physLastAvg = 0;
	
	// Time of last performance display update.  Used for both FPS and physics.
	private long perfLastTime = 0;

	// Working variables used during bounce calculation.  Allocating them
	// once is significantly faster.
	private Topology.Intersect bounceResults;

}

