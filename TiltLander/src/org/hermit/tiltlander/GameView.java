
/**
 * Tilt Lander: an accelerometer-controlled moon landing game for Android.
 * <br>Copyright (C) 2007 Google Inc.
 * 
 * <p>Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * <p>http://www.apache.org/licenses/LICENSE-2.0
 * 
 * <p>Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */


package org.hermit.tiltlander;


import org.hermit.android.core.SurfaceRunner;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;


/**
 * View that draws, takes keystrokes, etc. for a simple LunarLander game.
 * 
 * Has a mode which RUNNING, PAUSED, etc. Has a x, y, dx, dy, ... capturing the
 * current ship physics. All x/y etc. are measured with (0,0) at the lower left.
 * updatePhysics() advances the physics based on realtime. draw() renders the
 * ship, and does an invalidate() to prompt another draw() as soon as possible
 * by the system.
 */
public class GameView
	extends SurfaceRunner
    implements SensorEventListener
{

	// ******************************************************************** //
	// Public Constants.
	// ******************************************************************** //
    
    /**
     * Game difficulty levels.
     */
    public enum Difficulty {
        /**
         * Easy difficulty -- bigger landing pad, more fuel, and less
         * sensitivity to landing speed and angle.
         */
        EASY(1.5f, 1.33f, 1.5f, 1.33f, 0.75f),
        
        /**
         * Medium difficulty -- medium parameters.
         */
        MEDIUM(1f, 1f, 1f, 1f, 1f),
        
        /**
         * Hard difficulty -- smaller landing pad, less fuel, and more
         * sensitivity to landing speed.
         */
        HARD(0.875f, 0.75f, 0.875f, 1f, 1.33f);
        
        Difficulty(float fuel, float pad, float spd, float ang, float ispd) {
            adjFuel = fuel;
            adjWidth = pad;
            adjSpeed = spd;
            adjAngle = ang;
            adjInit = ispd;
        }
        final float adjFuel;
        final float adjWidth;
        final float adjSpeed;
        final float adjAngle;
        final float adjInit;
    }
    
    
	// ******************************************************************** //
	// Constructor.
	// ******************************************************************** //

	/**
	 * Construct a game view.
	 * 
	 * @param	parent			The application context we're running in.
	 */
    public GameView(Context parent) {
        super(parent);
        init(parent);
    }
    

    /**
     * Construct a game view.
     * 
     * @param   parent          The application context we're running in.
     * @param   attrs           Layout attributes for this view.
     */
    public GameView(Context parent, AttributeSet attrs) {
        super(parent, attrs);
        init(parent);
    }
    

    /**
     * Initialize this game view.
     * 
     * @param   parent          The application context we're running in.
     */
    private void init(Context parent) {
        parentApp = parent;

        sensorManager = (SensorManager)
                            parent.getSystemService(Context.SENSOR_SERVICE);

        // Animation delay.
//        setDelay(30);
        
        // get handles to some important objects
        mHandler = new Handler() {
            @Override
            public void handleMessage(Message m) {
                mStatusText.setVisibility(m.getData().getInt("viz"));
                mStatusText.setText(m.getData().getString("text"));
            }
        };

        Resources res = parent.getResources();
        
        // Cache the strings.
        speedString = res.getString(R.string.warn_speed);
        angleString = res.getString(R.string.warn_angle);
        fuelString = res.getString(R.string.warn_fuel);

        // cache handles to our key sprites & other drawables
        mLanderImage = res.getDrawable(R.drawable.lander_plain);
        mFiringImage = res.getDrawable(R.drawable.lander_firing);
        mCrashedImage = res.getDrawable(R.drawable.lander_crashed);

        // load background image as a Bitmap instead of a Drawable b/c
        // we don't need to transform it and it's faster to draw this way
        mBackgroundImage = BitmapFactory.decodeResource(res,
                                                        R.drawable.earthrise);

        // Use the regular lander image as the model size for all sprites
        mLanderWidth = mLanderImage.getIntrinsicWidth();
        mLanderHeight = mLanderImage.getIntrinsicHeight();

        // Initialize paints for speedometer
        mLinePaint = new Paint();
        mLinePaint.setAntiAlias(true);
        mLinePaint.setARGB(255, 0, 255, 0);

        mWinsInARow = 0;
        mDifficulty = Difficulty.MEDIUM;

        tiltInverted = 1;

        scratchHsv = new float[3];
        
        // initial show-up of lander (not yet playing)
        mX = mLanderWidth;
        mY = mLanderHeight * 2;
        mTotalFuel = mRemFuel = PHYS_FUEL_INIT;
        mDX = 0;
        mDY = 0;
        mHeading = 0;
        mEngineFiring = true;
        
        setState(State.READY);
    }


    // ******************************************************************** //
    // Run Control.
    // ******************************************************************** //

    /**
     * Installs a pointer to the text view used for messages.
     * 
     * @param   textView        The text view.
     */
    public void setTextView(TextView textView) {
        mStatusText = textView;
    }

    
    /**
     * The application is starting.  Perform any initial set-up prior to
     * starting the application.  We may not have a screen size yet,
     * so this is not a good place to allocate resources which depend on
     * that.
     */
    @Override
    protected void appStart() {
        // We want the surface to be running the whole time.
        surfaceStart();
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
        // We usually get a zero-sized resize, which is useless;
        // ignore it.
        if (width < 1 || height < 1)
            return;

        // Get our orientation, so that we can adjust the sensor axes
        // to match the screen axes.
        Resources res = parentApp.getResources();
        Configuration conf = res.getConfiguration();
        deviceOrientation = conf.orientation;

        mCanvasWidth = width;
        mCanvasHeight = height;
        final int minDim = width < height ? width : height;
        
        // don't forget to resize the background image
        mBackgroundImage = Bitmap.createScaledBitmap(
                mBackgroundImage, width, height, true);
        
        // Set a good drawing thickness.
        lineThickness = minDim / 220;
        
        // Work out how big the gauges should be.
        gaugeWidth = (mCanvasWidth - UI_MARGIN * 4) / 3;
        gaugeHeight = gaugeWidth / 6;
        gaugeTop = UI_MARGIN;
        gaugeMid = gaugeTop + gaugeHeight / 2;
        gaugeBot = gaugeTop + gaugeHeight;
        gaugeSpdLeft = UI_MARGIN;
        gaugeSpdRight = gaugeSpdLeft + gaugeWidth;
        gaugeHorizMid = mCanvasWidth / 2;
        gaugeHorizLeft = gaugeHorizMid - gaugeWidth / 2;
        gaugeHorizRight = gaugeHorizMid + gaugeWidth / 2;
        gaugeFuelRight = width - UI_MARGIN;
        gaugeFuelLeft = gaugeFuelRight - gaugeWidth;
        
        gaugeTextSize = minDim / 480f * 30f;
        gaugeTextBase = gaugeBot + (int) gaugeTextSize;
        mLinePaint.setTypeface(Typeface.DEFAULT_BOLD);
        mLinePaint.setTextSize(gaugeTextSize);
        
        // Position the debug info.
        setDebugPos(0, gaugeTextBase + (int) gaugeTextSize);
    }
 

    /**
     * We are starting the animation loop.  The screen size is known.
     * 
     * <p>doUpdate() and doDraw() may be called from this point on.
     */
    @Override
    protected void animStart() {
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
        pause();
        sensorManager.unregisterListener(this);
    }
    

    /**
     * The application is closing down.  Clean up any resources.
     */
    @Override
    protected void appStop() {
        
    }
    

    // ******************************************************************** //
    // Game Control.
    // ******************************************************************** //

    /**
     * Starts the game, setting parameters for the current difficulty.
     */
    public void doStart() {
        synchronized (this) {
            // First set the game for Medium difficulty
            mTotalFuel = PHYS_FUEL_INIT * mDifficulty.adjFuel;
            mGoalWidth = (int) (mLanderWidth * TARGET_WIDTH * mDifficulty.adjWidth);
            mGoalSpeed = (int) (TARGET_SPEED * mDifficulty.adjSpeed);
            mGoalAngle = (int) (TARGET_ANGLE * mDifficulty.adjAngle);
            int speedInit = (int) (PHYS_SPEED_INIT * mDifficulty.adjInit);
            mRemFuel = mTotalFuel;
            mEngineFiring = false;

            // Adjust difficulty params for EASY/HARD

            // pick a convenient initial location for the lander sprite
            mX = mCanvasWidth / 2;
            mY = mCanvasHeight - mLanderHeight / 2;

            // Start with a little random motion.
            mDY = (float) Math.random() * -speedInit;
            mDX = (float) Math.random() * 2 * speedInit - speedInit;
            mHeading = 0;

            // Figure initial spot for landing, not too near center
            while (true) {
                mGoalX = (int) (Math.random() * (mCanvasWidth - mGoalWidth));
                if (Math.abs(mGoalX - (mX - mLanderWidth / 2)) > mCanvasWidth / 5)
                    break;
            }

            mLastTime = System.currentTimeMillis() + 100;
            setState(State.RUNNING);
        }
    }

    /**
     * Pauses the physics update & animation.
     */
    public void pause() {
        synchronized (this) {
            if (mMode == State.RUNNING)
                setState(State.PAUSE);
        }
    }


    /**
     * Resumes from a pause.
     */
    public void unpause() {
        // Move the real time clock up to now
        synchronized (this) {
            mLastTime = System.currentTimeMillis() + 100;
        }
        setState(State.RUNNING);
    }

    /**
     * Sets the current difficulty.
     * 
     * @param   difficulty      The desired difficulty level for the game.
     */
    public void setDifficulty(Difficulty difficulty) {
        synchronized (this) {
            mDifficulty = difficulty;
        }
    }

    
    /**
     * Set the tilt control direction for this game view.
     * 
     * @param   inverted        False for normal -- tilt right to turn
     *                          clockwise.
     *                          True for inverted -- tilt right to turn
     *                          anti-clockwise.
     */
    public void setTiltInverted(boolean inverted) {
        synchronized (this) {
            tiltInverted = inverted ? -1 : 1;
        }
    }

    /**
     * Sets if the engine is currently firing.
     */
    private void setFiring(boolean firing) {
        synchronized (this) {
            mEngineFiring = firing;
        }
    }

    
    /**
     * Sets the game mode. That is, whether we are running, paused, in the
     * failure state, in the victory state, etc.
     * 
     * @see #setState(int, CharSequence)
     * @param mode one of the State.* constants
     */
    private void setState(State mode) {
        synchronized (this) {
            setState(mode, null);
        }
    }

    /**
     * Sets the game mode. That is, whether we are running, paused, in the
     * failure state, in the victory state, etc.
     * 
     * @param mode one of the State.* constants
     * @param message string to add to screen or null
     */
    private void setState(State mode, CharSequence message) {
        /*
         * This method optionally can cause a text message to be displayed
         * to the user when the mode changes. Since the View that actually
         * renders that text is part of the main View hierarchy and not
         * owned by this thread, we can't touch the state of that View.
         * Instead we use a Message + Handler to relay commands to the main
         * thread, which updates the user-text View.
         */
        synchronized (this) {
            mMode = mode;

            if (mMode == State.RUNNING) {
                Message msg = mHandler.obtainMessage();
                Bundle b = new Bundle();
                b.putString("text", "");
                b.putInt("viz", View.GONE);
                msg.setData(b);
                mHandler.sendMessage(msg);
            } else {
                mRotating = 0;
                mTiltAngle = 0;
                mEngineFiring = false;
                Resources res = parentApp.getResources();
                CharSequence str = "";
                if (mMode == State.READY)
                    str = res.getText(R.string.mode_ready);
                else if (mMode == State.PAUSE)
                    str = res.getText(R.string.mode_pause);
                else if (mMode == State.LOSE)
                    str = res.getText(R.string.mode_lose);
                else if (mMode == State.WIN)
                    str = res.getString(R.string.mode_win_prefix)
                            + mWinsInARow + " "
                            + res.getString(R.string.mode_win_suffix);

                if (message != null)
                    str = message + "\n" + str;

                if (mMode == State.LOSE)
                    mWinsInARow = 0;

                Message msg = mHandler.obtainMessage();
                Bundle b = new Bundle();
                b.putString("text", str.toString());
                b.putInt("viz", View.VISIBLE);
                msg.setData(b);
                mHandler.sendMessage(msg);
            }
        }
    }


    // ******************************************************************** //
    // Client Methods.
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
        // Do nothing if mLastTime is in the future.
        // This allows the game-start to delay the start of the physics
        // by 100ms or whatever.
        if (mLastTime > now)
            return;
        
        // Do nothing if we're not running.
        if (mMode != State.RUNNING)
            return;

        double elapsed = (now - mLastTime) / 1000.0;

        // Handle the tilt.  First calculate how far we need to turn
        // to get there.
        double delta = mTiltAngle - mHeading;
        if (delta < -180)
            delta += 360;
        else if (delta > 180)
            delta -= 360;
        
        // Calculate the rotation rate as delta per 1/5 sec, capped to
        // PHYS_SLEW_SEC.
        delta *= 5;
        if (Math.abs(delta) > PHYS_SLEW_SEC)
            delta = delta < 0 ? -PHYS_SLEW_SEC : PHYS_SLEW_SEC;
        mHeading += delta * elapsed;

        // If the user is controlling manually, update heading
        if (mRotating != 0)
            mHeading += mRotating * (PHYS_SLEW_SEC * elapsed);

        // Bring the heading back into the range 0..360
        if (mHeading < 0)
            mHeading += 360;
        else if (mHeading >= 360)
            mHeading -= 360;

        // Base accelerations -- 0 for x, gravity for y
        double ddx = 0.0;
        double ddy = -PHYS_DOWN_ACCEL_SEC * elapsed;

        if (mEngineFiring) {
            // taking 0 as up, 90 as to the right
            // cos(deg) is ddy component, sin(deg) is ddx component
            double elapsedFiring = elapsed;
            double fuelUsed = elapsedFiring * PHYS_FUEL_SEC;

            // tricky case where we run out of fuel partway through the
            // elapsed
            if (fuelUsed > mRemFuel) {
                elapsedFiring = mRemFuel / fuelUsed * elapsed;
                fuelUsed = mRemFuel;

                // Oddball case where we adjust the "control" from here
                mEngineFiring = false;
            }

            mRemFuel -= fuelUsed;

            // have this much acceleration from the engine
            double accel = PHYS_FIRE_ACCEL_SEC * elapsedFiring;

            double radians = 2 * Math.PI * mHeading / 360;
            ddx = Math.sin(radians) * accel;
            ddy += Math.cos(radians) * accel;
        }

        double dxOld = mDX;
        double dyOld = mDY;

        // figure speeds for the end of the period
        mDX += ddx;
        mDY += ddy;
        mSpeed = (float) Math.sqrt(mDX * mDX + mDY * mDY);
        
        // figure position based on average speed during the period
        mX += elapsed * (mDX + dxOld) / 2;
        mY += elapsed * (mDY + dyOld) / 2;

        mLastTime = now;

        // Evaluate if we have landed ... stop the game
        float yLowerBound = TARGET_PAD_HEIGHT + mLanderHeight / 2f -
                                                    TARGET_BOTTOM_PADDING;
        if (mY <= yLowerBound) {
            mY = yLowerBound;

            State result = State.LOSE;
            CharSequence message = "";
            Resources res = parentApp.getResources();
            boolean onGoal = (mGoalX <= mX - mLanderWidth / 2 && mX
                    + mLanderWidth / 2 <= mGoalX + mGoalWidth);

            // "Hyperspace" win -- upside down, going fast,
            // puts you back at the top.
            if (onGoal && Math.abs(mHeading - 180) < mGoalAngle
                    && mSpeed > PHYS_SPEED_HYPERSPACE) {
                result = State.WIN;
                mWinsInARow++;
                doStart();

                return;
                // Oddball case: this case does a return, all other cases
                // fall through to setMode() below.
            } else if (!onGoal) {
                message = res.getText(R.string.message_off_pad);
            } else if (!(mHeading <= mGoalAngle || mHeading >= 360 - mGoalAngle)) {
                message = res.getText(R.string.message_bad_angle);
            } else if (mSpeed > mGoalSpeed) {
                message = res.getText(R.string.message_too_fast);
            } else {
                result = State.WIN;
                mWinsInARow++;
            }

            setState(result, message);
        }
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
     *                      passed to doUpdate(), if there was a preceding
     *                      call to doUpdate().
     */
    @Override
    protected void doDraw(Canvas canvas, long now) {
        // Draw the background image. Operations on the Canvas accumulate
        // so this is like clearing the screen.
        canvas.drawBitmap(mBackgroundImage, 0, 0, null);

        int yTop = mCanvasHeight - ((int) mY + mLanderHeight / 2);
        int xLeft = (int) mX - mLanderWidth / 2;

        // Draw the status gauges.
        drawFuel(canvas, now);
        drawHorizon(canvas, now);
        drawSpeed(canvas, now);
        
        // Draw the landing pad.
        mLinePaint.setStyle(Paint.Style.STROKE);
        mLinePaint.setStrokeWidth(lineThickness * 2);
        mLinePaint.setColor(UI_PAD_COL);
        canvas.drawLine(mGoalX, 1 + mCanvasHeight - TARGET_PAD_HEIGHT,
                mGoalX + mGoalWidth, 1 + mCanvasHeight - TARGET_PAD_HEIGHT,
                mLinePaint);

        // Draw the ship with its current rotation
        canvas.save();
        canvas.rotate((float) mHeading, (float) mX, mCanvasHeight - (float) mY);
        if (mMode == State.LOSE) {
            mCrashedImage.setBounds(xLeft, yTop, xLeft + mLanderWidth, yTop
                    + mLanderHeight);
            mCrashedImage.draw(canvas);
        } else if (mEngineFiring) {
            mFiringImage.setBounds(xLeft, yTop, xLeft + mLanderWidth, yTop
                    + mLanderHeight);
            mFiringImage.draw(canvas);
        } else {
            mLanderImage.setBounds(xLeft, yTop, xLeft + mLanderWidth, yTop
                    + mLanderHeight);
            mLanderImage.draw(canvas);
        }
        canvas.restore();
    }

    
    /**
     * Draw the fuel gauge.
     * 
     * @param   canvas      The Canvas to draw into.
     * @param   now         Current time in ms.  Will be the same as that
     *                      passed to doUpdate(), if there was a preceding
     *                      call to doUpdate().
     */
    private void drawFuel(Canvas canvas, long now) {
        // See what fraction of fuel we're on.
        float fuelFrac = (float) mRemFuel / (float) mTotalFuel;
        
        // Work out what colour to draw the fuel bar in.
        scratchHsv[0] = fuelFrac * 120f;
        scratchHsv[1] = 1f;
        scratchHsv[2] = 1f;
        int fuelCol = Color.HSVToColor(scratchHsv);
        
        // Draw the fuel bar.
        mLinePaint.setStyle(Paint.Style.FILL);
        mLinePaint.setColor(fuelCol);
        float w = (float) gaugeWidth * fuelFrac;
        canvas.drawRect(gaugeFuelLeft, gaugeTop, gaugeFuelLeft + w, gaugeBot, mLinePaint);
        
        // Draw the outline.
        mLinePaint.setStyle(Paint.Style.STROKE);
        mLinePaint.setStrokeWidth(lineThickness);
        mLinePaint.setColor(UI_GAUGE_COL);
        canvas.drawRect(gaugeFuelLeft, gaugeTop, gaugeFuelRight, gaugeBot, mLinePaint);
        
        // Draw the warning message if we're low.
        if (fuelFrac < 0.2f) {
            // Flash the warning based on time of the second and fuel level.
            long elap = now - lastFuelWarn;
            if (elap > 150) {
                mLinePaint.setStyle(Paint.Style.STROKE);
                mLinePaint.setStrokeWidth(0f);
                mLinePaint.setColor(fuelCol);
                canvas.drawText(fuelString, gaugeFuelLeft, gaugeTextBase, mLinePaint);
                if (elap > 300 + 400 * fuelFrac * 5f)
                    lastFuelWarn = now;
            }
        }
    }

    
    /**
     * Draw the artificial horizon.
     * 
     * @param   canvas      The Canvas to draw into.
     * @param   now         Current time in ms.  Will be the same as that
     *                      passed to doUpdate(), if there was a preceding
     *                      call to doUpdate().
     */
    private void drawHorizon(Canvas canvas, long now) {
        // Work out how far we are off level.
        float offAngle = mHeading < 180f ? mHeading : 360 - mHeading;
        
        // Work out what our warning fraction is.  It is zero at or below
        // the safe angle, and rises to 1 at 2 * safe.
        float warnFrac = offAngle / mGoalAngle - 1f;
        if (warnFrac < 0.0f)
            warnFrac = 0.0f;
        else if (warnFrac > 1.0f)
            warnFrac = 1.0f;
        
        // Work out what colour to draw the horizon in.  Since we have a
        // specific goal, below that is green, above shades from yellow to red.
        int horizCol;
        if (warnFrac <= 0f)
            horizCol = 0xff00ff00;
        else {
            scratchHsv[0] = (1 - warnFrac) * 60f;
            scratchHsv[1] = 1f;
            scratchHsv[2] = 1f;
            horizCol = Color.HSVToColor(scratchHsv);
        }
        
        // Draw the outline.
        mLinePaint.setStyle(Paint.Style.STROKE);
        mLinePaint.setStrokeWidth(lineThickness);
        mLinePaint.setColor(UI_GAUGE_COL);
        canvas.drawLine(gaugeHorizLeft, gaugeTop, gaugeHorizRight, gaugeTop, mLinePaint);
        canvas.drawLine(gaugeHorizLeft, gaugeBot, gaugeHorizRight, gaugeBot, mLinePaint);
        
        // Draw the horizon line.
        mLinePaint.setStyle(Paint.Style.STROKE);
        mLinePaint.setStrokeWidth(lineThickness);
        mLinePaint.setColor(horizCol);
        canvas.save();
        canvas.rotate(mHeading, gaugeHorizMid, gaugeMid);
        canvas.drawLine(gaugeHorizLeft, gaugeMid, gaugeHorizRight, gaugeMid, mLinePaint);
        canvas.drawCircle(gaugeHorizMid, gaugeMid, gaugeHeight / 4f, mLinePaint);
        canvas.restore();

        // Draw the warning message if we're squint.
        if (warnFrac > 0f) {
            // Flash the warning based on the warning level.
            long elap = now - lastAngleWarn;
            if (elap > 150) {
                mLinePaint.setStyle(Paint.Style.STROKE);
                mLinePaint.setStrokeWidth(0f);
                mLinePaint.setColor(horizCol);
                canvas.drawText(angleString, gaugeHorizLeft, gaugeTextBase, mLinePaint);
                if (elap > 300 + 400 * (1 - warnFrac) * 5f)
                    lastAngleWarn = now;
            }
        }
    }

    
    /**
     * Draw the speed gauge.
     * 
     * @param   canvas      The Canvas to draw into.
     * @param   now         Current time in ms.  Will be the same as that
     *                      passed to doUpdate(), if there was a preceding
     *                      call to doUpdate().
     */
    private void drawSpeed(Canvas canvas, long now) {
        // See what fraction of max. speed we're at.
        float speedFrac = (float) mSpeed / (float) PHYS_SPEED_MAX;
        if (speedFrac > 1f)
            speedFrac = 1f;
        
        // Calculate a safe speed for this altitude.  Below 1/3 of screen
        // height, it's the goal speed.  Otherwise it rises one multiple
        // of goal speed per 1.3 height.
        float safeSpeed = mY / mCanvasHeight * 3f * mGoalSpeed;
        if (safeSpeed < mGoalSpeed)
            safeSpeed = mGoalSpeed;

        // Work out what our warning fraction is.  It is zero at or below
        // the safe speed, and rises to 1 at 2 * safe.
        float warnFrac = mSpeed / safeSpeed - 1f;
        if (warnFrac < 0.0f)
            warnFrac = 0.0f;
        else if (warnFrac > 1.0f)
            warnFrac = 1.0f;
        
        // Work out what colour to draw the speed bar in.  Since we have a
        // specific goal, below that is green, above shades from yellow to red.
        int speedCol;
        if (mSpeed < mGoalSpeed)
            speedCol = 0xff00ff00;
        else {
            scratchHsv[0] = (1 - warnFrac) * 60f;
            scratchHsv[1] = 1f;
            scratchHsv[2] = 1f;
            speedCol = Color.HSVToColor(scratchHsv);
        }
        
        // Draw the speed bar.
        mLinePaint.setStyle(Paint.Style.FILL);
        mLinePaint.setColor(speedCol);
        float w = (float) gaugeWidth * speedFrac;
        canvas.drawRect(gaugeSpdLeft, gaugeTop, gaugeSpdLeft + w, gaugeBot, mLinePaint);
        
        // Draw the outline.
        mLinePaint.setStyle(Paint.Style.STROKE);
        mLinePaint.setStrokeWidth(lineThickness);
        mLinePaint.setColor(UI_GAUGE_COL);
        canvas.drawRect(gaugeSpdLeft, gaugeTop, gaugeSpdRight, gaugeBot, mLinePaint);
        
        // Draw the warning message if we're low.
        if (warnFrac > 0f) {
            // Flash the warning based on the warning level.
            long elap = now - lastSpeedWarn;
            if (elap > 150) {
                mLinePaint.setStyle(Paint.Style.STROKE);
                mLinePaint.setStrokeWidth(0f);
                mLinePaint.setColor(speedCol);
                canvas.drawText(speedString, gaugeSpdLeft, gaugeTextBase, mLinePaint);
                if (elap > 300 + 400 * (1 - warnFrac) * 5f)
                    lastSpeedWarn = now;
            }
        }
    }


	// ******************************************************************** //
	// Input Handling.
	// ******************************************************************** //

    /**
	 * Handle key input.
	 * 
     * @param	keyCode			The key code.
     * @param	event			The KeyEvent object that defines the
     * 							button action.
     * @return					True if the event was handled, false otherwise.
	 */
    @Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
        boolean okStart = false;
        if (keyCode == KeyEvent.KEYCODE_DPAD_UP) okStart = true;
        if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN) okStart = true;
        if (keyCode == KeyEvent.KEYCODE_S) okStart = true;

        if (okStart
                && (mMode == State.READY || mMode == State.LOSE || mMode == State.WIN)) {
            // ready-to-start -> start
            doStart();
            return true;
        } else if (mMode == State.PAUSE && okStart) {
            // paused -> running
            unpause();
            return true;
        } else if (mMode == State.RUNNING) {
            // center/space -> fire
            if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER
                    || keyCode == KeyEvent.KEYCODE_SPACE) {
                setFiring(true);
                return true;
                // left/q -> left
            } else if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT
                    || keyCode == KeyEvent.KEYCODE_Q) {
                mRotating = -1;
                return true;
                // right/w -> right
            } else if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT
                    || keyCode == KeyEvent.KEYCODE_W) {
                mRotating = 1;
                return true;
                // up -> pause
            } else if (keyCode == KeyEvent.KEYCODE_DPAD_UP) {
                pause();
                return true;
            }
        }

        return false;
	}
	
    
    /**
     * Handle key input.
     * 
     * @param   keyCode         The key code.
     * @param   event           The KeyEvent object that defines the
     *                          button action.
     * @return                  True if the event was handled, false otherwise.
     */
    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        boolean handled = false;

        synchronized (this) {
            if (mMode == State.RUNNING) {
                if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER
                        || keyCode == KeyEvent.KEYCODE_SPACE) {
                    setFiring(false);
                    handled = true;
                } else if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT
                        || keyCode == KeyEvent.KEYCODE_Q
                        || keyCode == KeyEvent.KEYCODE_DPAD_RIGHT
                        || keyCode == KeyEvent.KEYCODE_W) {
                    mRotating = 0;
                    handled = true;
                }
            }
        }

        return handled;
    }
    

    /**
     * Handle trackball motion events.
     * 
     * @param	event			The motion event.
     * @return					True if the event was handled, false otherwise.
     */
    @Override
    public boolean onTrackballEvent(MotionEvent event) {
    	// Actually, just let these come through as D-pad events.
    	return false;
    }
    

    /**
     * Handle MotionEvent events.
     * 
     * @param   event           The motion event.
     * @return                  True if the event was handled, false otherwise.
     */
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int action = event.getAction();
        
        // If we're stopped, touch screen to start.
        if (mMode != State.RUNNING) {
            if (action == MotionEvent.ACTION_DOWN) {
                if (mMode == State.PAUSE)
                    unpause();
                else
                    doStart();
                return true;
            }
        } else {
            switch (action) {
            case MotionEvent.ACTION_DOWN:
                setFiring(true);
                return true;
            case MotionEvent.ACTION_UP:
                setFiring(false);
                return true;
            case MotionEvent.ACTION_MOVE:
            case MotionEvent.ACTION_CANCEL:
            default:
                break;
            }
        }

        return false;
    }

    
    // ******************************************************************** //
    // Sensor Handling.
    // ******************************************************************** //

    /**
     * Called when the accuracy of a sensor has changed.
     * 
     * @param   sensor          The sensor being monitored.
     * @param   accuracy        The new accuracy of this sensor.
     */
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Don't need anything here.
    }


    /**
     * Called when sensor values have changed.
     *
     * @param   event           The sensor event.
     */
    public void onSensorChanged(SensorEvent event) {
        final float[] values = event.values;
        if (event.sensor.getType() != Sensor.TYPE_ACCELEROMETER || values.length < 3)
            return;
        if (mMode != State.RUNNING)
            return;

        // Calculate the angle of tilt in X; i.e. the elevation off the Y-Z
        // plane.  This is pretty easy; the X value is the opposite side,
        // and the absolute magnitude of the current value is the hypotenuse.
        // So sin a = x / m.
        float x, y, z;
        if (deviceOrientation == Configuration.ORIENTATION_LANDSCAPE) {
            x = -values[1];
            y = values[0];
            z = values[2];
        } else {
            x = values[0];
            y = values[1];
            z = values[2];
        }
        float m = (float) Math.sqrt(x*x + y*y + z*z);
        float tilt = m == 0 ? 0  : (float) Math.toDegrees(Math.asin(x / m));
        Log.v(TAG, "tilt: " + x + "," + y + "," + z + " -> " + tilt);

        // Amplify the user's movements.
        tilt *= (1.5 * -tiltInverted);

        synchronized (this) {
            mTiltAngle = tilt < 0 ? tilt + 360 : tilt;
        }
    }

    
    // ******************************************************************** //
    // State Save/Restore.
    // ******************************************************************** //

    /**
     * Save game state so that the user does not lose anything
     * if the game process is killed while we are in the 
     * background.
     * 
	 * @param	outState		A Bundle in which to place any state
	 * 							information we wish to save.
     */
    protected void saveState(Bundle outState) {
        outState.putString(KEY_DIFFICULTY, mDifficulty.toString());
        outState.putFloat(KEY_X, mX);
        outState.putFloat(KEY_Y, mY);
        outState.putFloat(KEY_DX, mDX);
        outState.putFloat(KEY_DY, mDY);
        outState.putFloat(KEY_HEADING, mHeading);
        outState.putInt(KEY_LANDER_WIDTH, Integer.valueOf(mLanderWidth));
        outState.putInt(KEY_LANDER_HEIGHT, Integer.valueOf(mLanderHeight));
        outState.putInt(KEY_GOAL_X, Integer.valueOf(mGoalX));
        outState.putInt(KEY_GOAL_SPEED, Integer.valueOf(mGoalSpeed));
        outState.putInt(KEY_GOAL_ANGLE, Integer.valueOf(mGoalAngle));
        outState.putInt(KEY_GOAL_WIDTH, Integer.valueOf(mGoalWidth));
        outState.putInt(KEY_WINS, Integer.valueOf(mWinsInARow));
        outState.putFloat(KEY_TOT_FUEL, mTotalFuel);
        outState.putFloat(KEY_REM_FUEL, mRemFuel);
    }

    
    /**
     * Restore our game state from the given Bundle.
     * 
     * @param	savedState	A Bundle containing the saved state.
     * @param	skill		Skill level of the saved game.
     * @return				true if the state was restored OK; false
     * 						if the saved state was incompatible with the
     * 						current configuration.
     */
    boolean restoreState(Bundle savedState) {
        setState(State.PAUSE);
        mRotating = 0;
        mTiltAngle = 0;
        mEngineFiring = false;

        mDifficulty = Difficulty.valueOf(savedState.getString(KEY_DIFFICULTY));
        mX = savedState.getFloat(KEY_X);
        mY = savedState.getFloat(KEY_Y);
        mDX = savedState.getFloat(KEY_DX);
        mDY = savedState.getFloat(KEY_DY);
        mHeading = savedState.getFloat(KEY_HEADING);

        mLanderWidth = savedState.getInt(KEY_LANDER_WIDTH);
        mLanderHeight = savedState.getInt(KEY_LANDER_HEIGHT);
        mGoalX = savedState.getInt(KEY_GOAL_X);
        mGoalSpeed = savedState.getInt(KEY_GOAL_SPEED);
        mGoalAngle = savedState.getInt(KEY_GOAL_ANGLE);
        mGoalWidth = savedState.getInt(KEY_GOAL_WIDTH);
        mWinsInARow = savedState.getInt(KEY_WINS);
        mTotalFuel = savedState.getFloat(KEY_TOT_FUEL);
        mRemFuel = savedState.getFloat(KEY_REM_FUEL);

        return true;
    }
    
    
    // ******************************************************************** //
    // Private Constants.
    // ******************************************************************** //
    
    /**
     * Game states.
     */
    private enum State {
        LOSE,
        PAUSE,
        READY,
        RUNNING,
        WIN;
    }


    /*
     * Physics constants
     */
    private static final int PHYS_DOWN_ACCEL_SEC = 35;
    private static final int PHYS_FIRE_ACCEL_SEC = 80;
    private static final int PHYS_FUEL_INIT = 60;
    private static final int PHYS_FUEL_SEC = 10;
    private static final int PHYS_SLEW_SEC = 100; // degrees/second max rotate
    private static final int PHYS_SPEED_HYPERSPACE = 180;
    private static final int PHYS_SPEED_INIT = 30;
    private static final int PHYS_SPEED_MAX = 120;
    /*
     * Goal condition constants
     */
    private static final int TARGET_ANGLE = 8; // > this angle means crash
    private static final int TARGET_BOTTOM_PADDING = 17; // px below gear
    private static final int TARGET_PAD_HEIGHT = 8; // how high above ground
    private static final int TARGET_SPEED = 32; // > this speed means crash
    private static final double TARGET_WIDTH = 1.6; // width of target
    
    /*
     * UI constants (i.e. the speed & fuel bars)
     */
    
    // Margins around the gauges.
    private static final int UI_MARGIN = 10;
    
    // Colour for the gauges' outlines etc.
    private static final int UI_GAUGE_COL = 0xffffff00;
   
    // Landing pad thickness and colour.
    private static final int UI_PAD_COL = 0xff00ffa0;

    // Saved state item keys.
    private static final String KEY_DIFFICULTY = "mDifficulty";
    private static final String KEY_X = "mX";
    private static final String KEY_Y = "mY";
    private static final String KEY_DX = "mDX";
    private static final String KEY_DY = "mDY";
    private static final String KEY_TOT_FUEL = "mTotFuel";
    private static final String KEY_REM_FUEL = "mRemFuel";
    private static final String KEY_GOAL_ANGLE = "mGoalAngle";
    private static final String KEY_GOAL_SPEED = "mGoalSpeed";
    private static final String KEY_GOAL_WIDTH = "mGoalWidth";
    private static final String KEY_GOAL_X = "mGoalX";
    private static final String KEY_HEADING = "mHeading";
    private static final String KEY_LANDER_HEIGHT = "mLanderHeight";
    private static final String KEY_LANDER_WIDTH = "mLanderWidth";
    private static final String KEY_WINS = "mWinsInARow";

    
    // ******************************************************************** //
    // Class Data.
    // ******************************************************************** //

    // Debugging tag.
	@SuppressWarnings("unused")
    private static final String TAG = "netscramble";
	
	
	// ******************************************************************** //
	// Private Data.
	// ******************************************************************** //
    
    // The parent application context.
    private Context parentApp;
    
    /** The sensor manager, which we use to interface to all sensors. */
    private SensorManager sensorManager;

    /** Pointer to the text view to display "Paused.." etc. */
    private TextView mStatusText;

    // The drawable to use as the background of the animation canvas.
    private Bitmap mBackgroundImage;
    
    // Current device orientation, as one of the
    // Configuration.ORIENTATION_XXX flags.
    private int deviceOrientation = 0;

    // Current size of the surface/canvas.
    private int mCanvasWidth = 1;
    private int mCanvasHeight = 1;
    
    // Thickness to draw lines with.
    private int lineThickness;
    
    // Locations and sizes of the gauges.
    private int gaugeWidth = 0;
    private int gaugeHeight = 0;
    private int gaugeTop = 0;
    private int gaugeMid = 0;
    private int gaugeBot = 0;
    private int gaugeSpdLeft = 0;
    private int gaugeSpdRight = 0;
    private int gaugeHorizLeft = 0;
    private int gaugeHorizMid = 0;
    private int gaugeHorizRight = 0;
    private int gaugeFuelLeft = 0;
    private int gaugeFuelRight = 0;
    
    // Size and position for text labels.
    private float gaugeTextSize = 0;
    private int gaugeTextBase = 0;
    
    // Warning strings for overspeed, bad angle and low fuel.  These are set up
    // from resources.
    private String speedString = "";
    private String angleString = "";
    private String fuelString = "";

    /** What to draw for the Lander when it has crashed */
    private Drawable mCrashedImage;

    /**
     * Current difficulty -- amount of fuel, allowed angle, etc. Default is
     * MEDIUM.
     */
    private Difficulty mDifficulty;

    // Velocity in X and Y, and overall speed.
    private float mDX;
    private float mDY;
    private float mSpeed;

    /** Is the engine burning? */
    private boolean mEngineFiring;

    /** What to draw for the Lander when the engine is firing */
    private Drawable mFiringImage;

    /** Total fuel for this level */
    private float mTotalFuel;

    /** Fuel remaining */
    private float mRemFuel;
    
    // Time of the last speed, angle and fuel warnings, if we're showing them.
    private long lastSpeedWarn;
    private long lastAngleWarn;
    private long lastFuelWarn;
    
    /** Allowed angle. */
    private int mGoalAngle;

    /** Allowed speed. */
    private int mGoalSpeed;

    /** Width of the landing pad. */
    private int mGoalWidth;

    /** X of the landing pad. */
    private int mGoalX;

    /** Message handler used by thread to interact with TextView */
    private Handler mHandler;

    /**
     * Lander heading in degrees, with 0 up, 90 right. Kept in the range
     * 0..360.
     */
    private float mHeading;

    /** Pixel height of lander image. */
    private int mLanderHeight;

    /** What to draw for the Lander in its normal state */
    private Drawable mLanderImage;

    /** Pixel width of lander image. */
    private int mLanderWidth;

    /** Used to figure out elapsed time between frames */
    private long mLastTime;

    /** Paint to draw the lines on screen. */
    private Paint mLinePaint;

    /** The state of the game. One of READY, RUNNING, PAUSE, LOSE, or WIN */
    private State mMode;

    /** Currently rotating, -1 left, 0 none, 1 right. */
    private int mRotating;
    
    /**
     * The desired lander heading currently set by the handset tilt,
     * in degrees, with 0 up, 90 right.  Kept in the range
     * 0..360.
     */
    private float mTiltAngle;

    /** Number of wins in a row. */
    private int mWinsInARow;

    /** X of lander center. */
    private float mX;

    /** Y of lander center. */
    private float mY;

    /** are the controls inverted? */
    private int tiltInverted = 1;

    // Scratch array for HSV conversions.
    private float[] scratchHsv;

}

