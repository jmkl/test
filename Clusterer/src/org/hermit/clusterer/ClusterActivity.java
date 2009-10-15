
/**
 * clusterer: a testbed application for cluster analysis.
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


package org.hermit.clusterer;


import org.hermit.geometry.Region;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.Button;
import android.widget.Toast;


/**
 * Clustering test viewer.  This class implments a simple view which
 * displays the results of a clustering test as it runs.
 */
public class ClusterActivity
    extends Activity
{
    
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.cluster_view);
        
        messageHandler = new Handler();
        
        // Get the control panel.
        controlPanel = findViewById(R.id.control_panel);
        
        // Get the clustering widget and set up its handler.  When the main
        // window is tapped, show the controls.
        clusterWidget = (ClusterWidget) findViewById(R.id.cluster_view);
        clusterWidget.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                seeControls();
            }
        });
        
        // Get the control buttons.
        Button generateButton = (Button) findViewById(R.id.but_generate);
        generateButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                messageHandler.post(testSetup);
                seeControls();
            }
        });
        Button stepButton = (Button) findViewById(R.id.but_step);
        stepButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (clusterTest == null) {
                    Toast.makeText(ClusterActivity.this,
                                   R.string.no_data_msg,
                                   Toast.LENGTH_SHORT).show();
                    return;
                }

                messageHandler.post(testStep);
                seeControls();
            }
        });
        Button solveButton = (Button) findViewById(R.id.but_solve);
        solveButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (clusterTest == null) {
                    Toast.makeText(ClusterActivity.this,
                                   R.string.no_data_msg,
                                   Toast.LENGTH_SHORT).show();
                    return;
                }

                messageHandler.post(testSolve);
                seeControls();
            }
        });
        Button resetButton = (Button) findViewById(R.id.but_reset);
        resetButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (clusterTest == null) {
                    Toast.makeText(ClusterActivity.this,
                                   R.string.no_data_msg,
                                   Toast.LENGTH_SHORT).show();
                    return;
                }

                messageHandler.post(testReset);
                seeControls();
            }
        });
    }

    
    /**
     * Called after onRestoreInstanceState(Bundle), onRestart(), or onPause(),
     * for your activity to start interacting with the user.  This is a good
     * place to begin animations, open exclusive-access devices (such as the
     * camera), etc.
     */
    @Override
    protected void onResume() {
        Log.i(TAG, "onResume()");
        super.onResume();
    }

    
    /**
     * Called as part of the activity lifecycle when an activity is going
     * into the background, but has not (yet) been killed.  The counterpart
     * to onResume(). 
     */
    @Override
    protected void onPause() {
        Log.i(TAG, "onPause()");
        super.onPause();
    }

    
    // ******************************************************************** //
    // UI Management.
    // ******************************************************************** //
    
    /**
     * Make the control panel visible, if it isn't already.
     */
    private void seeControls() {
        // Remove any existing fade-out timer.
        messageHandler.removeCallbacks(hideControls);

        // If not visible, then fade them in.
        if (controlPanel.getVisibility() != View.VISIBLE) {
            Animation a = showControlsAnimation;
            a.setDuration(500);
            a.startNow();
            controlPanel.setAnimation(a);
            controlPanel.setVisibility(View.VISIBLE);
        }
        
        // In any case, re-set the timer to fade them out again.
        messageHandler.postDelayed(hideControls, 3000);
    }
    
    
    /**
     * This Runnable can be fired as a delayed run to hide the controls.
     */
    Runnable hideControls = new Runnable() {
        @Override
        public void run() {
            // If visible, fade them out.
            if (controlPanel.getVisibility() == View.VISIBLE) {
                Animation a = hideControlsAnimation;
                a.setDuration(1000);
                a.startNow();
                controlPanel.setAnimation(a);
                controlPanel.setVisibility(View.INVISIBLE);
            }
        }
    };
    
    
    // ******************************************************************** //
    // Cluster Analysis.
    // ******************************************************************** //

    /**
     * Set up a test case.
     */
    private Runnable testSetup = new Runnable() {
        public void run() {
            screenRegion = new Region(0, 0, clusterWidget.getWidth(),
                                            clusterWidget.getHeight());
            
            // Set the clustering algorithm based on the intent.
            clusterTest = new ClusterTest(ClusterActivity.this, screenRegion, getIntent());
            
            // Display the initial data.
            clusterWidget.set(clusterTest);
        }
    };


    /**
     * Step the current test case one iteration.
     */
    private Runnable testStep = new Runnable() {
        public void run() {
            clusterTest.step();
            clusterWidget.set(clusterTest);
        }
    };


    /**
     * Step the current test case one iteration.
     */
    private Runnable testSolve = new Runnable() {
        public void run() {
            boolean converged = clusterTest.step();
            clusterWidget.set(clusterTest);

            // If we converged, that's it.
            if (!converged && clusterTest.getIterations() < MAX_LOOP_COUNT)
                messageHandler.postDelayed(testSolve, 500);
        }
    };


    /**
     * Reset the test, keeping the same initial data.
     */
    private Runnable testReset = new Runnable() {
        public void run() {
            clusterTest.reset();
            clusterWidget.set(clusterTest);
        }
    };


    // ******************************************************************** //
    // Private Constants.
    // ******************************************************************** //

    // Debugging tag.
    private static final String TAG = "Cluster";
    
    private static final int MAX_LOOP_COUNT = 100;
    
    
    // ******************************************************************** //
    // Private Data.
    // ******************************************************************** //

    // Handler for messages, such as those used to hide the controls.
    private Handler messageHandler;
    
    // The widget used to display the results.
    private ClusterWidget clusterWidget = null;

    // The control panel, which fades in and out.
    private View controlPanel = null;
    
    // Animations used to show and hide the control panel.
    private Animation hideControlsAnimation = new AlphaAnimation(1f, 0f);
    private Animation showControlsAnimation = new AlphaAnimation(0f, 1f);

    private ClusterTest clusterTest = null;
    
    // The region of the 2D real plane which is mapped to the screen.
    private Region screenRegion = null;

}

