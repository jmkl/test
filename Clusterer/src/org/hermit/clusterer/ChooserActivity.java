
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


import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;


/**
 * Activity which runs a k-means clustering visualizer, after letting the user
 * pick which algorithm to use.
 */
public class ChooserActivity extends Activity {
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.chooser);
        
        // Handle button presses.
        Button bDalvik = (Button) findViewById(R.id.button_kmeans);
        bDalvik.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				launch("kmeans");
			}
        });
        Button bFuzzy = (Button) findViewById(R.id.button_fuzzy);
        bFuzzy.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                launch("fuzzy");
            }
        });
    }
    
    
    private void launch(String which) {
    	Intent intent = new Intent(this, ClusterActivity.class);
    	intent.putExtra("algorithm", which);
    	startActivity(intent);
    }
   
}

