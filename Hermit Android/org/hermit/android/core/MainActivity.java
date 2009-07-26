
/**
 * core: basic Android utilities.
 * <br>Copyright 2004-2009 Ian Cameron Smith
 *
 * <p>This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as
 * published by the Free Software Foundation (see COPYING).
 *
 * <p>This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * General Public License for more details.
 *
 * <p>You should have received a copy of the GNU General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA
 * 02111-1307, USA.
 */


package org.hermit.android.core;


import java.util.HashMap;

import android.app.Activity;
import android.content.Intent;
import android.util.Log;


/**
 * An enhanced Activity class, for use as the main activity of an application.
 * The main thing this class provides is a nice callback-based mechanism
 * for starting sub-activities.  This makes it easier for different parts
 * of an app to kick off sub-activities and get the results.
 * 
 * <p>Note: it is best that sub-classes do not implement
 * onActivityResult(int, int, Intent).  If they do, then for safety use
 * small request codes, and call super.onActivityResult(int, int, Intent)
 * when you get an unknown code.
 *
 * @author Ian Cameron Smith
 */
public class MainActivity
	extends Activity
{

	// ******************************************************************** //
	// Public Classes.
	// ******************************************************************** //

	/**
	 * This interface defines a listener for sub-activity results.
	 */
	public static abstract class ActivityListener {

	    /**
	     * Called when an activity you launched exits.
	     * 
	     * <p>Applications can override this to be informed when an activity
	     * finishes, either by an error, the user pressing "back", or
	     * normally, or whatever.  The default implementation calls either
	     * onActivityCanceled(), if resultCode == RESULT_CANCELED, or
	     * else onActivityResult().
	     * 
	     * @param	resultCode		The integer result code returned by the
	     * 							child activity through its setResult().
	     * @param	data			Additional data returned by the activity.
	     */
		public void onActivityFinished(int resultCode, Intent data) {
			if (resultCode == RESULT_CANCELED)
				onActivityCanceled(data);
			else
				onActivityResult(resultCode, data);
		}
	    

	    /**
	     * Called when an activity you launched exits with a result code
	     * of RESULT_CANCELED.  This will happen if the user presses "back",
	     * or if the activity returned that code explicitly, didn't return
	     * any result, or crashed during its operation.
	     * 
	     * <p>Aplications can override this if they want to be separately
	     * notified of a RESULT_CANCELED.  It doesn't make sense to override
	     * both onActivityFinished() and this method.
	     * 
	     * @param	data			Additional data returned by the activity.
	     */
	    public void onActivityCanceled(Intent data) { }
		
	    /**
	     * Called when an activity you launched exits with a result code
	     * other than RESULT_CANCELED, giving you the resultCode it
	     * returned, and any additional data from it.
	     * 
	     * <p>Aplications can override this if they want to be separately
	     * notified of a normal exit.  It doesn't make sense to override
	     * both onActivityFinished() and this method.
	     * 
	     * @param	resultCode		The integer result code returned by the
	     * 							child activity through its setResult().
	     * @param	data			Additional data returned by the activity.
	     */
	    public void onActivityResult(int resultCode, Intent data) { }

	    // This listener's request code.  This code is auto-assigned
	    // the first time the listener is used, and is used to find it
	    // from the response.
	    private int requestCode = 0;
	    
	}


	// ******************************************************************** //
	// Public API.
	// ******************************************************************** //

    /**
     * Launch an activity for which you would like a result when it
     * finished.  When this activity exits, the given ActivityListener
     * will be invoked.
     * 
     * <p>Note that this method should only be used with Intent protocols
     * that are defined to return a result.  In other protocols (such
     * as ACTION_MAIN or ACTION_VIEW), you may not get the result when
     * you expect.
     * 
     * As a special case, if you call startActivityForResult() during
     * the initial onCreate() / onResume() of your activity, then your
     * window will not be displayed until a result is returned back
     * from the started activity.
     * 
     * This method throws ActivityNotFoundException if there was no
     * Activity found to run the given Intent.
     * 
     * @param	intent			The intent to start.
     * @param	listener		Listener to invoke when the activity returns.
     */
    public void startActivityForResult(Intent intent, ActivityListener listener) {
    	// If this listener doesn't yet have a request code, give it one,
    	// and add it to the map so we can find it again.  On subsequent calls
    	// we re-use the same code.
    	if (listener.requestCode == 0) {
    		listener.requestCode = nextRequest++;
        	codeMap.put(listener.requestCode, listener);
    	}
    	
    	// Start the sub-activity.
    	startActivityForResult(intent, listener.requestCode);
    }


	// ******************************************************************** //
	// Activity Management.
	// ******************************************************************** //

    /**
     * Called when an activity you launched exits, giving you the requestCode
     * you started it with, the resultCode it returned, and any additional
     * data from it.  The resultCode will be RESULT_CANCELED if the activity
     * explicitly returned that, didn't return any result, or crashed during
     * its operation.
     * 
     * @param	requestCode		The integer request code originally supplied
     * 							to startActivityForResult(), allowing you to
     * 							identify who this result came from.
     * @param	resultCode		The integer result code returned by the child
     * 							activity through its setResult().
     * @param	data			Additional data to return to the caller.
     */
    @Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    	ActivityListener listener = codeMap.get(requestCode);
    	if (listener == null)
    		Log.e("MainActivity", "Unknown request code: " + requestCode);
    	else
			listener.onActivityFinished(resultCode, data);
    }


	// ******************************************************************** //
	// Private Data.
	// ******************************************************************** //

    // The next request code available to be used.  Our request codes
    // start at a large number, for no special reason.
    private int nextRequest = 0x60000000;
    
    // This map translates request codes to the listeners registered for
    // those requests.  It is used when a response is received to activate
    // the correct listener.
    private HashMap<Integer, ActivityListener> codeMap =
    							new HashMap<Integer, ActivityListener>();
    
}

