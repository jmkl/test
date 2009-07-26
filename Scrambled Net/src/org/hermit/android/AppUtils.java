
/**
 * org.hermit.utils: useful Android utility classes.
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


package org.hermit.android;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;

import android.app.Activity;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;


/**
 * This class provides some simple application-related utilities.
 */
public class AppUtils
{

	// ******************************************************************** //
	// Public Classes.
	// ******************************************************************** //

	/**
	 * Version info detail level.
	 */
	public enum Detail {
		NONE,				// Do not display.
		SIMPLE,				// Show basic name and version.
		DEBUG;				// Show debug-level detail.
	}
	
	
	/**
	 * Information on an application version.
	 */
	public class Version {
		// Application's pretty name.  null if unknown.
		public CharSequence appName = null;
		
		// Version code of the app.  -1 if unknown.
		public int versionCode = -1;
		
		// Version name of the app.  null if unknown.
		public CharSequence versionName = null;
		
		// Description either of the app or the version.  null if unknown.
		public CharSequence appDesc = null;
	}
	
	
	// ******************************************************************** //
	// Constructor.
	// ******************************************************************** //

	/**
	 * Set up an app utils instance for the given activity.
	 * 
	 * @param	parent			Activity for which we want information.
	 */
	public AppUtils(Activity parent) {
		parentApp = parent;
		resources = parent.getResources();
	}


	// ******************************************************************** //
	// Current App Info.
	// ******************************************************************** //
	   
    /**
     * Get the version info for the current app.
     * 
     * @return				App version info.  null if the info could
     * 						not be found.
     */
    public Version getAppVersion() {
    	// If we have the info, just return it.
    	if (appVersion != null)
    		return appVersion;
    	
    	// Get the package manager.
    	PackageManager pm = parentApp.getPackageManager();
    	
    	// Get our package name and use it to get our package info.  We
    	// don't need the optional info.
    	String pname = parentApp.getPackageName();
    	try {
        	appVersion = new Version();

        	PackageInfo pinfo = pm.getPackageInfo(pname, 0);
			appVersion.versionCode = pinfo.versionCode;
			appVersion.versionName = pinfo.versionName;
		 	
		 	// Get the pretty name and description of the app.
		 	ApplicationInfo ainfo = pinfo.applicationInfo;
		 	if (ainfo != null) {
		 		int alabel = ainfo.labelRes;
		 		if (alabel != 0)
		 			appVersion.appName = resources.getText(alabel);
			 	
		 		int dlabel = ainfo.descriptionRes;
		 		if (dlabel != 0)
		 			appVersion.appDesc = resources.getText(dlabel);
		 	}
		} catch (NameNotFoundException e) {
			appVersion = null;
		}
		
		return appVersion;
    }
    
    
    /**
     * Get a string containing the name and version info for the current
     * app's package, in a simple format.
     * 
     * @return				Descriptive name / version string.
     */
    public String getVersionString() {
    	return getVersionString(Detail.SIMPLE);
    }
    
   
    /**
     * Get a string containing the name and version info for the current
     * app's package.
     * 
     * @param detail		How much detail we want.
     * @return				Descriptive name / version string.
     */
    public String getVersionString(Detail detail) {
    	String pname = parentApp.getPackageName();
    	Version ver = getAppVersion();
    	if (ver == null)
    		return String.format("%s (no info)", pname);

    	CharSequence aname = ver.appName;
    	if (aname == null)
    		aname = "?";
    	int vcode = ver.versionCode;
    	CharSequence vname = ver.versionName;
    	if (vname == null)
    		vname = "?.?";

    	String res = null;
    	if (detail == Detail.DEBUG)
    		res = String.format("%s (%s) %s (%d)", aname, pname, vname, vcode);
    	else {
    		// TODO: the "version" should really come from a resource,
    		// but I don't want a separate resources file in this package.
    		res = String.format("%s version %s", aname, vname);
    	}

    	return res;
    }
    

	// ******************************************************************** //
	// Update Status.
	// ******************************************************************** //
	   
    /**
     * Determine whether an update exists for this app.  We do this by looking
     * at a given text file on the app's web site, which contains data on the
     * latest available version, and comparing it with the current version.
     * 
     * The file is simple newline-separated text.  The format consists of
     * four fields, each on one line:
     * 		versionCode
     * 		versionName
     * 		Summary
     * 		Description
     * where:
     * 		versionCode		latest version's version code
     * 		versionName		latest version's version name
     * 		Summary			1-line summary of what's new
     * 		Description		longer description of what's new
     * All fields must be a single line.  A maximum length is enforced
     * on each line.
     * 
     * Example usage:
     *     checkUpdateStatus(new URL("http://my.site.com/latest.xml"));
     *
     * @param	statUrl			URL of the latest version status file.
     * @return					The version info for the available update,
     * 							if there is one; else null.
     */
    public Version checkUpdateStatus(URL statUrl) {
    	// Get our own version, and the available update version.
    	// If we can't, give up.
    	Version ver = getAppVersion();
    	Version uv = getUpdateVersion(statUrl);
    	if (ver == null || uv == null)
    		return null;
    	
    	if (uv.versionCode > ver.versionCode)
    		return uv;
    	
		return null;
    }
    
    
    /**
     * Get the version info of the latest available update, if possible.
     * 
     * We will not even attempt to read the update status unless at least
     * UPDATE_CHECK_INTERVAL has elapsed since the last time we tried
     * (successfully or not) to read it.  To manage this, we use the
     * "updateCheckTime" user preference.
     * 
     * @param	statUrl		URL of the update status file.
     * @return				Update version, if we could read it; else
     * 						null.
     */
    private Version getUpdateVersion(URL statUrl) {
    	// If we already got the data, just return that.
    	if (updateVersion != null)
    		return updateVersion;
    	
    	// See if it's long enough since the last check.
    	if (!updateCheckTimeOK(UPDATE_CHECK_INTERVAL))
    		return null;
    	
    	// Attempt to connect to the URL.  If we fail, just bomb out.
    	URLConnection conn;
		try {
			conn = statUrl.openConnection();
		} catch (IOException e) {
			return null;
		}

		InputStream stream = null;
		InputStreamReader reads = null;
		BufferedReader readc = null;
		try {
    		// Open a buffered reader on the connection.  What a chore...
    		stream = conn.getInputStream();
    		reads = new InputStreamReader(stream);
    		readc = new BufferedReader(reads);
    		 
    		Version uversion = new Version();
    		StringBuilder buf = new StringBuilder(MAX_DESC_LEN);

    		// Read the app name.
    		readLine(readc, buf, MAX_NAME_LEN);
    		uversion.appName = buf.toString();

    		// Read the version code.
    		readLine(readc, buf, MAX_VERSION_LEN);
    		uversion.versionCode = new Integer(buf.toString());

    		// Read the version name.
    		readLine(readc, buf, MAX_VERSION_LEN);
    		uversion.versionName = buf.toString();

    		// Read the update description.
    		readLine(readc, buf, MAX_DESC_LEN);
    		uversion.appDesc = buf.toString();

    		updateVersion = uversion;
		} catch (IOException e) {
			return null;
		} catch (NumberFormatException e) {
			return null;
		} finally {
    		// Clean up the streams etc.  Other than that, we just bomb.
    		try {
    			if (readc != null)
    				readc.close();
    		} catch (IOException e1) { }
    		try {
    			if (reads != null)
    				reads.close();
    		} catch (IOException e1) { }
    		try {
    			if (stream != null)
    				stream.close();
    		} catch (IOException e1) { }
    	}
		
		return updateVersion;
    }


    /**
     * Determine whether enough time has elapsed since the last update
     * check, by looking at the "updateCheckTime" user preference.
     * 
     * @param	interval	Minimum time in ms that must have elapsed since
     * 						the last check.
     * @return				True iff it is OK to check for an update.
     */
    private boolean updateCheckTimeOK(long interval) {
    	// Get the current time.
    	long now = System.currentTimeMillis();
    	
    	// Get the last check time from our preferences.
    	SharedPreferences prefs = parentApp.getPreferences(0);
    	long last = prefs.getLong("updateCheckTime", 0);
    	
    	// If the next allowed check is in the future, no dice.
    	if (last + interval > now)
    		return false;

    	// Update the update check time to now.
    	SharedPreferences.Editor editor = prefs.edit();
    	editor.putLong("updateCheckTime", now);
    	editor.commit();
	
    	// OK to check.
		return true;
    }

    
    /**
     * Read a line from the given reader into the given buffer, up to a
     * given maximum number of characters.  If the line we find is too
     * long, or if anything else goes wrong -- such as EOF before a
     * newline -- consider it to be a fatal error.  Basically anything
     * unexpected is fatal.
     * 
     * @param	readc				Reader to read from.
     * @param	buf					Buffer to place the result into.  It
     * 								will be emptied before reading.
     * @param	max					Maximum number of chars to read.
     * @throws	IOException			Something went wrong.
     */
    private void readLine(BufferedReader readc, StringBuilder buf, int max)
    	throws IOException
    {
    	buf.setLength(0);
    	int n = 0;
    	int c;
		while ((c = readc.read()) != '\n') {
			// Consider an EOF or overlong line to be a fatal error.
			if (c == -1)
				throw new IOException("Unexpected EOF in status file.");
			if (n >= max)
				throw new IOException("Line too long in status file.");
			
			buf.append((char) c);
			++n;
		}
    }
    

	// ******************************************************************** //
	// Class Data.
	// ******************************************************************** //

    // Minimum interval between update checks, in ms.
    private static final long UPDATE_CHECK_INTERVAL = 1000 * 3600 * 24;
    
    // Maximum allowed length of an app name in an update status file.
    private static final int MAX_NAME_LEN = 32;

    // Maximum allowed length of a version code or name in an update
    // status file.  We allow for up to "99.99.99".
    private static final int MAX_VERSION_LEN = 8;

    // Maximum allowed length of a version description in an update
    // status file.
    private static final int MAX_DESC_LEN = 250;

    
	// ******************************************************************** //
	// Private Data.
	// ******************************************************************** //
 
	// Parent application context.
	private Activity parentApp;
	
	// App's resources.
	private Resources resources;
	
	// Version info for this application instance.  null if we don't
	// have it yet.
	private Version appVersion = null;
	
	// Version info for the latest available update.  null if we don't
	// have it yet.
	private Version updateVersion = null;
	
}

