
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

import java.io.File;
import java.io.FilenameFilter;

import org.hermit.plughole.LevelReader.LevelException;

import android.util.Log;
import android.util.SparseArray;


/**
 * This class manages all the available game levels.  Essentially
 * it keeps an ordered list of the built-in levels plus user-defined
 * levels (if any).
 */
class LevelManager {

    // ******************************************************************** //
    // Public Classes.
    // ******************************************************************** //

	/**
	 * Meta-information about a level.
	 */
	public static final class Info {
		
	}
	
	
    // ******************************************************************** //
    // Constructor.
    // ******************************************************************** //
	
	/**
	 * Create a level reader.
	 * 
	 * @param	app				The application context we're running in.
	 * @param	t				The game table.
	 */
	LevelManager(Plughole app) {
		levelReader = new LevelReader(app);
		
		currentIndex = 0;
	}


    // ******************************************************************** //
    // Level Read Control.
    // ******************************************************************** //

	/**
	 * Set whether to include user-defined levels.
	 * 
	 * @param	enable			If true, load user-defined levels.
	 * @throws LevelException 
	 */
	void setLoadUserLevels(boolean enable) throws LevelException {
		buildIndex(enable);
	}
	
	
    // ******************************************************************** //
    // Level Loading.
    // ******************************************************************** //
	
	/**
	 * Load the currently-selected level.
	 * 
	 * @param	xform		The transformation that needs to be applied
	 * 						to the level to make it fit the screen.
	 * @return				The level data.
	 * @throws LevelException	Error encountered while reading.
	 */
	LevelData loadLevel(Matrix xform) throws LevelReader.LevelException {
		// Load the level.
		levelReader.readCommon(R.xml.level_common, xform);
		return levelReader.readLevel(levelIndex.valueAt(currentIndex), xform);
	}
	
	
	/**
	 * Set the selected level to the next level.
	 */
	void nextLevel() {
		if (++currentIndex >= levelIndex.size())
			currentIndex = 0;
	}


	/**
	 * Set the selected level to the given level.
	 * 
	 * @param	index			Index of the level to select.
	 * @throws LevelException	Invalid level index.
	 */
	void gotoLevel(int index) throws LevelReader.LevelException {
		// If the index is invalid, throw.
		if (index < 0 || index >= levelIndex.size())
			throw new LevelException("invalid level index in saved state");
		
		currentIndex = index;
	}


    // ******************************************************************** //
    // Level Index.
    // ******************************************************************** //

	/**
	 * Set whether to include user-defined levels.
	 * 
	 * @param	user			If true, load user-defined levels.
	 * @throws LevelException 
	 */
	private void buildIndex(boolean user) throws LevelException {
		levelIndex = new SparseArray<LevelData.Header>();

		// Load all the built-in levels.
		for (int i : gameLevels) {
			Log.i(TAG, "Import BI level " + i);
			LevelData.Header head = levelReader.readLevelHead(i);
			addLevel(head);
		}
		
		// Load all the user-defined levels, if required.
		if (user) {
			File[] files = USER_DIR.listFiles(new FilenameFilter() {
				@Override
				public boolean accept(File dir, String filename) {
					return filename.endsWith(".xml");
				}
			});

			if (files != null) {
				for (File f : files) {
					Log.i(TAG, "Import user level " + f.getPath());
					LevelData.Header head = levelReader.readLevelHead(f);
					addLevel(head);
				}
			}
		}
		
		for (int i = 0; i < levelIndex.size(); ++i) {
			LevelData.Header head = levelIndex.valueAt(i);
			Log.i(TAG, "Level " + i + ": " + head.getDisplayName());
		}
	}
	
	
	private void addLevel(LevelData.Header head) {
		// Save the level header with a unique key.
		int serial = 0;
		int key;
		do {
			key = (head.group << 24) | (head.difficulty << 8) | ++serial;
		} while (levelIndex.indexOfKey(key) >= 0);
		Log.i(TAG, "Add level " + key + "->" + head.getDisplayName());
		levelIndex.append(key, head);
	}
	
	
    // ******************************************************************** //
    // Class Data.
    // ******************************************************************** //

    // Debugging tag.
	private static final String TAG = "plughole";
	
	// Pathname of the directory where user levels are kept.
	private static final File USER_DIR = new File("/sdcard/plughole");
	
	// The built-in levels we have.
	private static final int[] gameLevels = {
		R.xml.level_1_1,
		R.xml.level_1_2,
		R.xml.level_1_3,
	};

	
    // ******************************************************************** //
    // Public Data.
    // ******************************************************************** //

	// Level reader we use for reading levels.
	private LevelReader levelReader;
	
	// The index of all levels we know about.
	private SparseArray<LevelData.Header> levelIndex;

	// Current level index.
	private int currentIndex;
	
}

