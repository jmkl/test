
/**
 * On Watch: sailor's watchkeeping assistant.
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


package org.hermit.onwatch;


import java.util.ArrayList;

import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ViewAnimator;


/**
 * This class is the controller for the main GUI for OnWatch.  It manages
 * the main ViewAnimator which flips between various child views, with buttons
 * to initiate switching.
 */
public class MainController
{

	// ******************************************************************** //
    // Constructor.
    // ******************************************************************** //
	
	/**
	 * Create a view instance.
	 * 
	 * @param	context			Parent application.
	 */
	public MainController(OnWatch context) {
		appContext = context;
    	
//		// Get the main screen elements.
//		viewFlipper = (ViewAnimator) context.findViewById(R.id.main_view_flipper);
//		
//		// Get the number of child views.  Create the controllers for
//		// each child.
//		numChildren = viewFlipper.getChildCount();
//		childViews = new ArrayList<OnWatchController>();
//		for (int i = 0; i < numChildren; ++i) {
//			View child = viewFlipper.getChildAt(i);
//			int id = child.getId();
//			OnWatchController controller = null;
//			switch (id) {
//			case R.id.home_view:
//				controller = new HomeController(context);
//				break;
//			case R.id.passage_view:
//				controller = new PassageController(context);
//				break;
//			case R.id.schedule_view:
//				controller = new ScheduleController(context);
//				break;
//			case R.id.astro_view:
//				controller = new AstroController(context);
//				break;
//			default:
//				throw new RuntimeException("MainController: unexpected child ID " + id);
//			}
//			childViews.add(controller);
//
//			// Set handlers on the control buttons.
//			ImageButton left = (ImageButton) child.findViewById(R.id.flip_left);
//			if (left != null) {
//				left.setOnClickListener(new OnClickListener() {
//					@Override
//					public void onClick(View arg0) {
//						flipLeft();
//					}
//				});
//			}
//			ImageButton right = (ImageButton) child.findViewById(R.id.flip_right);
//			if (right != null) {
//				right.setOnClickListener(new OnClickListener() {
//					@Override
//					public void onClick(View arg0) {
//						flipRight();
//					}
//				});
//			}
//		}

//    	// Handle keys on the main view for debugging.
//		mainView = (LinearLayout) context.findViewById(R.id.main_view);
//		mainView.setOnKeyListener(new OnKeyListener() {
//			@Override
//			public boolean onKey(View arg0, int keyCode, KeyEvent event) {
//				return debugKey(keyCode, event);
//			}
//		});
//		
//    	// Load the animations for the switchers.
//    	animSlideInLeft = AnimationUtils.loadAnimation(context,
//                									   R.anim.slide_in_left);
//    	animSlideOutLeft = AnimationUtils.loadAnimation(context,
//				   										R.anim.slide_out_left);
//    	animSlideInRight = AnimationUtils.loadAnimation(context,
//				   										R.anim.slide_in_right);
//    	animSlideOutRight = AnimationUtils.loadAnimation(context,
//				   										 R.anim.slide_out_right);
	}
	
	   
    // ******************************************************************** //
    // Save and Restore.
    // ******************************************************************** //

    /**
     * Save the application's state to the given Bundle.
     * 
     * @param   icicle          A Bundle in which the app's state should
     *                          be saved.
     */
    public void saveState(Bundle icicle) {
//        icicle.putInt("currentView", viewFlipper.getDisplayedChild());
    }


    /**
     * Restore the application's state from the given Bundle.
     * 
     * @param   icicle          The app's saved state.
     */
    public void restoreState(Bundle icicle) {
//        viewFlipper.setDisplayedChild(icicle.getInt("currentView"));
    }


	// ******************************************************************** //
	// State Control.
	// ******************************************************************** //

	/**
	 * Start the application.  Called at initial start-up.
	 */
	void start() {
//		for (OnWatchController v : childViews)
//        	v.start();
	}
	

	/**
	 * Resume the application.
	 */
	public void resume() {
//		for (OnWatchController v : childViews)
//        	v.resume();
	}


	/**
	 * Pause the application.
	 */
	public void pause() {
//		for (OnWatchController v : childViews)
//        	v.pause();
	}
	

	/**
	 * Stop the application.  Called (probably) when shutting down completely.
	 */
	void stop() {
//		for (OnWatchController v : childViews)
//        	v.stop();
	}
	

	/**
	 * Regular tick event, for housekeeping.  Occurs every second, as
	 * close as possible to the 1-second boundary.
	 * 
	 * @param	time			Current system time in millis.
	 */
	public void tick(long time) {
//		for (OnWatchController v : childViews)
//        	v.tick(time);
	}


	// ******************************************************************** //
	// Flipping.
	// ******************************************************************** //

	/**
	 * Move to the next child view on the left.
	 */
	private void flipLeft() {
//		int curr = viewFlipper.getDisplayedChild();
//
//		viewFlipper.setInAnimation(animSlideInRight);
//		viewFlipper.setOutAnimation(animSlideOutRight);
//		
//		if (curr > 0)
//			viewFlipper.showPrevious();
//		else
//			viewFlipper.setDisplayedChild(numChildren - 1);
	}


	/**
	 * Move to the next child view on the right.
	 */
	private void flipRight() {
//		int curr = viewFlipper.getDisplayedChild();
//
//		viewFlipper.setInAnimation(animSlideInLeft);
//		viewFlipper.setOutAnimation(animSlideOutLeft);
//		
//		if (curr < numChildren - 1)
//			viewFlipper.showNext();
//		else
//			viewFlipper.setDisplayedChild(0);
	}

	
	// ******************************************************************** //
	// Debug Control.
	// ******************************************************************** //

	void setDebug(boolean space, boolean time) {
//		debugSpace = space;
//		debugTime = time;
//		mainView.setFocusable(space || time);
//		
//		if (!space) {
//			LocationModel locModel = LocationModel.getInstance(appContext);
//			locModel.adjustReset();
//		}
//		if (!time) {
//			TimeModel timeModel = TimeModel.getInstance(appContext);
//			timeModel.adjustReset();
//		}
	}
	

//	/**
//	 * Handle a key event.
//	 * 
//	 * @param	keyCode			Key code that represents the button pressed.
//	 * @param	event			KeyEvent object that defines the button action.
//	 * @return					If you handled the event, return true.
//	 * 							If you want to allow the event to be handled
//	 *							by the next receiver, return false. 
//	 */
//	private boolean debugKey(int keyCode, KeyEvent event) {
//		if (debugSpace) {
//			LocationModel locModel = LocationModel.getInstance(appContext);
//			double amt = 11.33;
//			switch (keyCode) {
//			case KeyEvent.KEYCODE_DPAD_LEFT:
//				locModel.adjust(0, -amt);
//				return true;
//			case KeyEvent.KEYCODE_DPAD_UP:
//				locModel.adjust(amt, 0);
//				return true;
//			case KeyEvent.KEYCODE_DPAD_RIGHT:
//				locModel.adjust(0, amt);
//				return true;
//			case KeyEvent.KEYCODE_DPAD_DOWN:
//				locModel.adjust(-amt, 0);
//				return true;
//			}
//		}
//
//		if (debugTime) {
//			TimeModel timeModel = TimeModel.getInstance(appContext);
//			switch (keyCode) {
//			case KeyEvent.KEYCODE_DPAD_UP:
//				timeModel.adjust(1);
//				return true;
//			case KeyEvent.KEYCODE_DPAD_RIGHT:
//				timeModel.adjust(2);
//				return true;
//			case KeyEvent.KEYCODE_DPAD_DOWN:
//				timeModel.adjust(3);
//				return true;
//			}
//		}
//
//		return false;
//	}


	// ******************************************************************** //
	// Class Data.
	// ******************************************************************** //

    // Debugging tag.
	@SuppressWarnings("unused")
	private static final String TAG = "onwatch";

	
	// ******************************************************************** //
	// Private Data.
	// ******************************************************************** //

	// Parent app we're running in.
	private OnWatch appContext;

	// The main view layout.
    private LinearLayout mainView;
	
    // The view flipper widget, which is the main part of our GUI.
    private ViewAnimator viewFlipper;
  
	// Animations for the view flipper.
	private Animation animSlideInLeft;
	private Animation animSlideOutLeft;
	private Animation animSlideInRight;
	private Animation animSlideOutRight;
	
    // The number of child views we have.  Must be the same as childViews.size().
    private int numChildren = 0;

	// The sub-views of this app, and their custom button bars.
	private ArrayList<OnWatchController> childViews;

	// Debug enable flags.
	private boolean debugSpace = false;
	private boolean debugTime = false;

}

