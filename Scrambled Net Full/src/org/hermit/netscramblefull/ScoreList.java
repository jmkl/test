
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


package org.hermit.netscramblefull;


import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.format.DateUtils;
import android.view.Window;
import android.view.WindowManager;
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

        // We don't want a title bar.
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                             WindowManager.LayoutParams.FLAG_FULLSCREEN);
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        setContentView(R.layout.score_layout);
        
        SharedPreferences scorePrefs = getSharedPreferences("scores", MODE_PRIVATE);
        BoardView.Skill[] skillVals = BoardView.Skill.values();
        
        // Populate the best clicks table.
        TableLayout clicksTable = (TableLayout) findViewById(R.id.clicksTable);
        for (BoardView.Skill skill : skillVals) {
            String sizeName = "size" + skill.toString();
            String clickName = "clicks" + skill.toString();

            // Get the best to date for this skill level.
            int size = scorePrefs.getInt(sizeName, -1);
            int clicks = scorePrefs.getInt(clickName, -1);
            long date = scorePrefs.getLong(clickName + "Date", 0);

            // Create a table row for this column.
            TableRow row = new TableRow(this);
            clicksTable.addView(row);

            // Add a label field to display the skill level.
            TextView skillLab = new TextView(this);
            skillLab.setTextSize(16);
            skillLab.setTextColor(0xff000000);
            String stext = getString(skill.label);
            if (size > 0)
                stext += " (" + size + ")";
            skillLab.setText(stext);
            row.addView(skillLab);

            // Add a field to display the clicks count.
            TextView clickLab = new TextView(this);
            clickLab.setTextSize(16);
            clickLab.setTextColor(0xff000000);
            clickLab.setText(clicks < 0 ? "--" : "" + clicks);
            row.addView(clickLab);

            // Add a field to display the date/time of this record.
            TextView dateLab = new TextView(this);
            dateLab.setTextSize(16);
            dateLab.setTextColor(0xff000000);
            dateLab.setText(dateString(date));
            row.addView(dateLab);
        }
        
        // Populate the best timess table.
        TableLayout timeTable = (TableLayout) findViewById(R.id.timeTable);
        for (BoardView.Skill skill : skillVals) {
            String sizeName = "size" + skill.toString();
            String timeName = "time" + skill.toString();

            // Get the best to date for this skill level.
            int size = scorePrefs.getInt(sizeName, -1);
            int time = scorePrefs.getInt(timeName, -1);
            long date = scorePrefs.getLong(timeName + "Date", 0);

            // Create a table row for this column.
            TableRow row = new TableRow(this);
            timeTable.addView(row);

            // Add a label field to display the skill level.
            TextView skillLab = new TextView(this);
            skillLab.setTextSize(16);
            skillLab.setTextColor(0xff000000);
            String stext = getString(skill.label);
            if (size > 0)
                stext += " (" + size + ")";
            skillLab.setText(stext);
            row.addView(skillLab);

            // Add a field to display the time taken.
            TextView timeLab = new TextView(this);
            timeLab.setTextSize(16);
            timeLab.setTextColor(0xff000000);
            timeLab.setText(time < 0 ? "--" :
                            String.format("%2d:%02d", time / 60, time % 60));
            row.addView(timeLab);

            // Add a field to display the date/time of this record.
            TextView dateLab = new TextView(this);
            dateLab.setTextSize(16);
            dateLab.setTextColor(0xff000000);
            dateLab.setText(dateString(date));
            row.addView(dateLab);
        }
    }
    
    
    private String dateString(long date) {
        if (date == 0)
            return "";
        int flags = DateUtils.isToday(date) ?
                        DateUtils.FORMAT_SHOW_TIME : DateUtils.FORMAT_SHOW_DATE;
        flags |= DateUtils.FORMAT_ABBREV_ALL;
        return DateUtils.formatDateTime(this, date, flags);
    }

}

