Adding a Hack
=============

Here's how to add a new hack to Substrate.

 1. Choose an existing hack to be a model.  (Substrate is good.)
	-- We'll refer to this hack as "Model" from now on.
 2. Choose a name for your new hack.
	-- We'll call it "NewHack" from now on.
 3. In the "hacks" Java package, copy:
		Model.java				to NewHack.java
		ModelPreferences.java	to NewHackPreferences.java
		ModelWallpaper.java		to NewHackWallpaper.java
 4. In res/xml, copy:
		model.xml				to newhack.xml
		model_settings.xml		to newhack_settings.xml
 5. In res/drawable, copy:
		model_preview.png		to newhack_preview.png
 6. Edit NewHack.java to contain your code, as follows:
	-- Edit the class comment
	-- Change SHARED_PREFS_NAME to "newhack_settings"
	-- In onConfigurationSet(), add any necessary screen-specific init
	   (e.g. select a good palette).
	-- Edit readPreferences() to read and set your hack's preferences.
	-- Place initialization code in reset().  This function should
	   reset the hack back to blank and restart it.
	-- Implement iterate() to update the hack one small step, not taking
	   too much time.  The model in Substrate.java shows how you can split
	   a complete computation cycle into many small steps.
	-- The remainder of the file is the meat of your implementation.
	-- Make sure your code handles whatever screen size is passed in to
	   onConfigurationSet()
	   -- Bear in mind you can access these parameters from the protected
	      fields in EyeCandy.java, don't bother saving local copies
 7. Edit NewHackPreferences.java as follows:
	-- Change a couple of references of Model to NewHack.
 8. Edit NewHackWallpaper.java as follows:
	-- Make onCreateHack() create and return an instance of NewHack.
 9. Edit res/values/strings.xml as follows:
	-- Create a section for your new hack, with all required strings,
	   including settings support.
10. Edit res/values/arrays.xml as follows:
	-- Create a preferences section for your new hack, with all required
	   settings names and values.
	-- Add a section in the "Hacks" section describing your hack's settings.
11. Edit res/xml/newhack.xml as follows:
	-- Change references of Model to NewHack.
12. Edit res/xml/newhack_settings.xml as follows:
	-- Create the definitions of yout hack's settings.  Change all
	   references of Model to NewHack.
13. Edit EyeCandyApp.java as follows:
	-- Add your hack to the three arrays at the end.
14. Edit AndroidManifest.xml as follows:
    -- Based on the sections for Model, add a wallpaper service section
       and a settings activity section for the new hack.
15. Run the app, and test your hack and its settings in the app.  Check
    the help, menu titles, etc.
16. Set it as a live wallpaper and check that it works.
17. Grab a nice screenshot, pick a 128x128 section, and put it in 
	res/drawable/newhack_preview.png.
