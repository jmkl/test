
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


import android.os.Bundle;
import android.preference.PreferenceActivity;


/**
 * Simple preferences activity for Tilt Lander.
 */
public class Preferences
	extends PreferenceActivity
{

    /**
     * Called when the activity is starting.  This is where most
     * initialization should go: calling setContentView(int) to inflate
     * the activity's UI, etc.
     * 
     * @param   icicle          Activity's saved state, if any.
     */
    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        
        // Load the preferences from an XML resource.
        addPreferencesFromResource(R.xml.preferences);
    }

}

