
/**
 * NetScramble: unscramble a network and connect all the terminals.
 * The player is given a network diagram with the parts of the network
 * randomly rotated; he/she must rotate them to connect all the terminals
 * to the server.
 * 
 * This is an Android implementation of the KDE game "knetwalk" by
 * Andi Peredri, Thomas Nagy, and Reinhold Kainhofer.
 *
 * © 2007-2010 Ian Cameron Smith <johantheghost@yahoo.com>
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


package org.hermit.netscramble;


import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;


/**
 * An activity which displays the "high score list" (personal bests)
 * for NetScramble.
 */
public class ScoreList
	extends Activity
{

    /**
     * Called when the activity is starting.  This is where most
     * initialization should go: calling setContentView(int) to inflate
     * the activity's UI, etc.
     * 
     * You can call finish() from within this function, in which case
     * onDestroy() will be immediately called without any of the rest of
     * the activity lifecycle executing.
     * 
     * Derived classes must call through to the super class's implementation
     * of this method.  If they do not, an exception will be thrown.
     * 
     * @param   icicle          If the activity is being re-initialized
     *                          after previously being shut down then this
     *                          Bundle contains the data it most recently
     *                          supplied in onSaveInstanceState(Bundle).
     *                          Note: Otherwise it is null.
     */
    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        
        setContentView(R.layout.score_layout);
        
        // Populate the score table.
        SharedPreferences scorePrefs = getSharedPreferences("scores", MODE_PRIVATE);
        TableLayout tableView = (TableLayout) findViewById(R.id.scoreTable);
        BoardView.Skill[] values = BoardView.Skill.values();
        for (BoardView.Skill skill : values) {
            // Get the best to date for this skill level.
            int clicks = scorePrefs.getInt("clicks" + skill.toString(), -1);
            int time = scorePrefs.getInt("time" + skill.toString(), -1);

            // Create a table row for this column.
            TableRow row = new TableRow(this);
            tableView.addView(row);

            // Add a label field to display the skill level.
            TextView skillLab = new TextView(this);
            skillLab.setTextSize(16);
            skillLab.setText(skill.label);
            row.addView(skillLab);

            // Add a field to display the clicks count.
            TextView clickLab = new TextView(this);
            clickLab.setTextSize(16);
            clickLab.setText(clicks < 0 ? "--" : "" + clicks);
            row.addView(clickLab);

            // Add a field to display the time count.
            TextView timeLab = new TextView(this);
            timeLab.setTextSize(16);
            timeLab.setText(time < 0 ? "--" :
            		         String.format("%2d:%02d", time / 60, time % 60));
            row.addView(timeLab);
        }
    }

}

