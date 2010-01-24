
package org.hermit.substrate;


import android.os.Bundle;
import android.preference.PreferenceActivity;


/**
 * Preferences activity for the Substrate live wallpaper.
 */
public class SubstratePreferences
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
        
        // Set up the key we're using for the wallpaper prefs.
        getPreferenceManager().setSharedPreferencesName(
                                    SubstrateWallpaper.SHARED_PREFS_NAME);
        
        // Load the preferences from an XML resource.
        addPreferencesFromResource(R.xml.substrate_settings);
    }

}

