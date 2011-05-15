
/**
 * Tricorder: turn your phone into a tricorder.
 * 
 * This is an Android implementation of a Star Trek tricorder, based on
 * the phone's own sensors.  It's also a demo project for sensor access.
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


package org.hermit.tricorder;

import java.net.MalformedURLException;
import java.net.URL;
import java.text.DateFormatSymbols;
import java.util.Calendar;
import java.util.Observable;
import java.util.Observer;
import java.util.TimeZone;

import org.hermit.android.core.SurfaceRunner;
import org.hermit.android.instruments.TextGauge;
import org.hermit.android.net.CachedFile;
import org.hermit.android.net.WebBasedData;
import org.hermit.android.net.WebFetcher;
import org.hermit.utils.CharFormatter;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.view.MotionEvent;


/**
 * A view which displays several scalar parameters as graphs.
 */
class SolarView
	extends DataView
	implements Observer
{

	// ******************************************************************** //
	// Constructor.
	// ******************************************************************** //

	/**
	 * Set up this view.
	 * 
	 * @param	context			Parent application context.
     * @param   parent          Parent surface.
	 */
	public SolarView(Tricorder context, SurfaceRunner parent) {
		super(context, parent);
		appContext = context;
		
		// Set up the database helper.
		databaseHelper = new DbHelper(appContext);
        TimeZone utc = TimeZone.getTimeZone("UTC");
        imageCal = Calendar.getInstance(utc);

		String[] tfields1 = { parent.getRes(R.string.lab_solar_elec),
								"99 days to J 16 16:00" };
		String[] tfields2 = { parent.getRes(R.string.lab_solar_spots),
							  "99 days to Jan 16" };
	
		// The solar image display, and its caption.
		sunImage = new ImageAtom(parent, FILES_SOHO, SUN_URLS);
		final String[] fields = { "Jan 99 12:02xx", };
		sunCaption = new TextGauge(parent, fields, 3);
		sunCaption.setTextSize(getBaseTextSize() - 5);
		sunCaption.setTextColor(0xffffff00);
		sunCaptionBuf = sunCaption.getBuffer();
		
		sunData = new TextGauge(parent, fields, 3);
		sunData.setTextSize(getBaseTextSize() - 5);
		sunData.setTextColor(COLOUR_PLOT);
		sunDataBuf = sunData.getBuffer();
		
		// Big solar image display, for alternate mode.
		sunBigImage = new ImageAtom(parent, FILES_SOHO, SUN_URLS);

		// Graph for solar wind data.
		swindGraph = new MagnitudeElement(parent,
		                                  EPAM1_PLOT_FIELDS.length, 400, 5,
										  COLOUR_GRID, EPAM1_PLOT_COLS,
										  tfields1);       	
		swindGraph.setDataSource(SRC_EPAM, EPAM1_PLOT_FIELDS);
		swindGraph.setText(0, 0, parent.getRes(R.string.lab_solar_prot));
		swindGraph.setText(0, 1, parent.getRes(R.string.msgNoData));

		// Graph for magnetic data.
		epamGraph = new MagnitudeElement(parent,
		                                 EPAM2_PLOT_FIELDS.length, 400, 5,
										 COLOUR_GRID, EPAM2_PLOT_COLS,
										 tfields1);       	
		epamGraph.setDataSource(SRC_EPAM, EPAM2_PLOT_FIELDS);
		epamGraph.setText(0, 0, parent.getRes(R.string.lab_solar_elec));
		epamGraph.setText(0, 1, parent.getRes(R.string.msgNoData));

		// Graph for solar data (sunspots / flares).
		solGraph = new MagnitudeElement(parent,
		                                DSD_PLOT_FIELDS.length, 10, 2,
										COLOUR_GRID, DSD_PLOT_COLS,
										tfields2);
		solGraph.setTimeScale(DSD_PLOT_TIMESCALE);
		solGraph.setDataSource(SRC_DSD, DSD_PLOT_FIELDS);
		solGraph.setText(0, 0, parent.getRes(R.string.lab_solar_spots));
		solGraph.setText(0, 1, parent.getRes(R.string.msgNoData));

		// Listen for updates from the, for the captions.
//		SRC_SWEPAM.addObserver(this);
//		SRC_EPAM.addObserver(this);
		SRC_DSD.addObserver(this);

		// Listen for updates from the image cache, to set the dates in
		// the image captions.
		FILES_SOHO.addObserver(this);
		
		setDisplayedSunImage(0);
	}
	

    // ******************************************************************** //
	// Geometry Management.
	// ******************************************************************** //

    /**
     * This is called during layout when the size of this element has
     * changed.  This is where we first discover our size, so set
     * our geometry to match.
     * 
	 * @param	bounds		The bounding rect of this element within
	 * 						its parent View.
     */
	@Override
	public void setGeometry(Rect bounds) {
		super.setGeometry(bounds);
		
		final int pad = getInterPadding();
		final int w = bounds.right - bounds.left;
		final int h = bounds.bottom - bounds.top;
		if (w < h) {
			layoutMainPort(bounds, pad);
			layoutAltPort(bounds, pad);
		} else {
			layoutMainLand(bounds, pad);
			layoutAltLand(bounds, pad);
		}
		
		sunImage.setGeometry(imageBounds);
		sunCaption.setGeometry(sunCaptionStd);
		sunData.setGeometry(sunDataStd);
		swindGraph.setGeometry(swindBounds);
		epamGraph.setGeometry(epamBounds);
		
		solGraph.setGeometry(solBounds);
		sunBigImage.setGeometry(imageBigBounds);
	}


    /**
     * Layout the main view components for portrait mode.
     * 
	 * @param	bounds		The bounding rect of this element within
	 * 						its parent View.
     */
	private void layoutMainPort(Rect bounds, int pad) {
		final int h = bounds.bottom - bounds.top;
		final int graphHeight = (h - pad * 2) / 3;

		int sx = bounds.left + pad;
		int ex = bounds.right;
		int size = graphHeight;
		int x = sx;
		int y = bounds.top;

		// Place the image display.  We assume the image has its own padding.
		imageBounds = new Rect(x, y, sx + size, y + size);
		x += size + pad;
		
		// Put the caption on the right, and the data display below it.
		int capHeight = sunCaption.getPreferredHeight();
		sunCaptionStd = new Rect(x, y, ex, y + capHeight);
		sunDataStd = new Rect(x, y + capHeight, ex, y + graphHeight);
		y += graphHeight + pad;
	
		// Lay out the graphs.
		swindBounds = new Rect(sx, y, ex, y + graphHeight);
		y += graphHeight + pad;
		
		epamBounds = new Rect(sx, y, ex, y + graphHeight);
	}


    /**
     * Layout the alternate view components for portrait mode.
     * 
	 * @param	bounds		The bounding rect of this element within
	 * 						its parent View.
     */
	private void layoutAltPort(Rect bounds, int pad) {
		int sx = bounds.left + pad;
		int ex = bounds.right;
		int size = ex - sx;

		// Place the big sun for alternate mode.
		int y = bounds.top;
		imageBigBounds = new Rect(sx, y, ex, y + size);
		y += size + pad;
		
		// Set the positions of the captions in alternate mode.
		int capHeight = sunCaption.getPreferredHeight();
		int dataHeight = sunData.getPreferredHeight();
		int width = size / 2 - pad;
		int x = sx;
		sunCaptionAlt = new Rect(x, y, x + width, y + capHeight);
		x += width + pad;
		sunDataAlt = new Rect(x, y, ex, y + dataHeight);
		y += (capHeight > dataHeight ? capHeight : dataHeight) + pad;
		
		// And add the solar graph for alt mode.
		solBounds = new Rect(sx, y, ex, bounds.bottom);
	}


    /**
     * Layout the main view components for landscape mode.
     * 
	 * @param	bounds		The bounding rect of this element within
	 * 						its parent View.
     */
	private void layoutMainLand(Rect bounds, int pad) {
		final int h = bounds.bottom - bounds.top;
		final int graphHeight = (h - pad) / 2;
		final int capHeight = sunCaption.getPreferredHeight();
		final int dataHeight = sunData.getPreferredHeight();

		int sx = bounds.left + pad;
		int ex = bounds.right;
		int size = h - capHeight - dataHeight;
		int x = sx;
		int y = bounds.top;

		// Place the image display.  We assume the image has its own padding.
		imageBounds = new Rect(x, y, x + size, y + size);
		y += size;
		
		// Put the captions below.
		sunCaptionStd = new Rect(x, y, x + size, y + capHeight);
		y += capHeight;
		sunDataStd = new Rect(x, y, x + size, y + dataHeight);
	
		x += size + pad;
		y = bounds.top;
		
		// Lay out the graphs.
		swindBounds = new Rect(x, y, ex, y + graphHeight);
		y += graphHeight + pad;
		
		epamBounds = new Rect(x, y, ex, y + graphHeight);
	}


    /**
     * Layout the alternate view components for landscape mode.
     * 
	 * @param	bounds		The bounding rect of this element within
	 * 						its parent View.
     */
	private void layoutAltLand(Rect bounds, int pad) {
		int sx = bounds.left + pad;
		int ex = bounds.right;
		int capWidth = sunCaption.getPreferredWidth();
		int capHeight = sunCaption.getPreferredHeight();
		int dataWidth = sunData.getPreferredWidth();
		int dataHeight = sunData.getPreferredHeight();
		int size = ex - sx - capWidth - dataWidth - pad;

		// Place the big sun for alternate mode.
		int x = sx;
		int y = bounds.top;
		imageBigBounds = new Rect(x, y, x + size, y + size);
		x += size + pad;
		
		// Set the positions of the captions in alternate mode.
		int width = (ex - x) / 2 - pad;
		sunCaptionAlt = new Rect(x, y, x + width, y + capHeight);
		x += width + pad;
		sunDataAlt = new Rect(x, y, ex, y + dataHeight);
		y += (capHeight > dataHeight ? capHeight : dataHeight) + pad;
		
		// And add the solar graph for alt mode.
		solBounds = new Rect(sx + size + pad, y, ex, bounds.bottom);
	}


	// ******************************************************************** //
	// Data Management.
	// ******************************************************************** //

	/**
	 * Notification that the overall application is starting (possibly
	 * resuming from a pause).  This does not mean that this view is visible.
	 * Views can use this to kick off long-term data gathering, but they
	 * should not use this to begin any CPU-intensive work; instead,
	 * wait for start().
	 */
	@Override
	void appStart() {
		// Calling getWritableDatabase() gets the helper to open (and maybe
		// create / update) the DB, and it also tells all our sources
		// about it.
	    if (database == null)
	        database = databaseHelper.getWritableDatabase();
	}
	

	/**
	 * Start this view.  This notifies the view that it should start
	 * receiving and displaying data.  The view will also get tick events
	 * starting here.
	 */
	@Override
	void start() {
	}
	

	/**
	 * A 1-second tick event.  Can be used for housekeeping and
	 * async updates.
	 * 
	 * @param	time				The current time in millis.
	 */
	@Override
	void tick(long time) {
		// Every minute, give our data sources a chance to check for
		// currency.
		if (time - lastDataCheck >= 1000 * 60) {
			lastDataCheck = time;
			for (WebBasedData s : ALL_SOURCES)
				s.update(time);
			FILES_SOHO.update(appContext, time);
		}
	}

	
	/**
	 * This view's aux button has been clicked.  Toggle the display mode.
	 */
	@Override
	void auxButtonClick() {
		appContext.soundActivate();
		altMode = !altMode;
		if (!altMode) {
			sunCaption.setGeometry(sunCaptionStd);
			sunData.setGeometry(sunDataStd);
		} else {
			sunCaption.setGeometry(sunCaptionAlt);
			sunData.setGeometry(sunDataAlt);
		}
	}
	

	/**
	 * Stop this view.  This notifies the view that it should stop
	 * receiving and displaying data, and generally stop using
	 * resources.
	 */
	@Override
	void stop() {
	}
	

	/**
	 * Notification that the overall application is stopping (possibly
	 * to pause).  Views can use this to stop any long-term activity.
	 */
	@Override
	void appStop() {
		// Don't want any more web data coming in...
		WebFetcher.killAll();

		// ... with the database closed.
		databaseHelper.close();
		database = null;
	}


    /**
     * Unbind any statically-bound resources which could leak memory.
     * This is typically used when the app is being destroyed, possibly
     * as a result of a device orientation change.  If we have static data
     * which links to the activity, the activity will be leaked (i.e.
     * prevented from being garbage collected).  Hence unbind it here.
     */
	@Override
    protected void unbindResources() {
		databaseHelper = null;
		FILES_SOHO.deleteObservers();
		SRC_EPAM.deleteObservers();
		SRC_DSD.deleteObservers();
    }
    

	// ******************************************************************** //
	// Web Data Handling.
	// ******************************************************************** //

	/**
	 * Observer method, called when an update happens on an object
	 * we're observing.
	 * 
	 * @param	o				The Observable.
	 * @param	arg				The update argument.
	 */
	public void update(Observable o, Object arg) {
	    try {
	        if (o instanceof WebBasedData && arg instanceof Long)
	            updateData((WebBasedData) o, (Long) arg);
	        else if (o instanceof CachedFile && arg instanceof URL)
	            updateImage((CachedFile) o, (URL) arg);
	    } catch (Exception e) {
	        appContext.reportException(e);
	    }
	}
	
	
	/**
	 * This method is invoked when a data source has finished loading
	 * a set of data.
	 * 
	 * @param	source			The data source.
	 * @param	latest			The date/time in ms UTC of the newest
	 * 							record that was loaded.
	 */
	private void updateData(WebBasedData s, long latest) {
//		if (s == SRC_SWEPAM) {
//			int days = Math.round(swindGraph.getDataLength() / 24f);
//			swindGraph.setText(0, 1, "" + days + " days to " + formatTime(latest));
//		} else if (s == SRC_EPAM) {
//			int days = Math.round(epamGraph.getDataLength() / 24f);
//			epamGraph.setText(0, 1, "" + days + " days to " + formatTime(latest));
//		} else
	    if (s == SRC_DSD) {
			// For the DSD data, use the last record for the sun data fields.
			ContentValues data = s.lastRecord();
			if (data != null) {
				synchronized (this) {
				    String spots = "Spots: " + data.getAsInteger("nsunspot") +
                                    "(" + data.getAsInteger("asunspot") + ")";
				    String flares = "Flares: " + data.getAsInteger("flares");
				    String rf = "RF Flux: " + data.getAsInteger("rflux");
				    CharFormatter.formatString(sunDataBuf[0][0], 0, spots, -1);
                    CharFormatter.formatString(sunDataBuf[1][0], 0, flares, -1);
                    CharFormatter.formatString(sunDataBuf[2][0], 0, rf, -1);
					
					int ndays = solGraph.getDataLength();
					String now = formatDate(latest);
					solGraph.setText(0, 1, "" + ndays + " days to " + now);
					swindGraph.setText(0, 1, "" + ndays + " days to " + now);
					epamGraph.setText(0, 1, "" + ndays + " days to " + now);
				}
			}
		}
	}


	/**
	 * This method is invoked when a file is loaded by the image cache.
	 * 
	 * @param	cache			The image cache.
	 * @param	url				The URL of the file that was loaded.
	 */
	private void updateImage(CachedFile cache, URL url) {
		for (int i = 0; i < SUN_URLS.length; ++i) {
			if (SUN_URLS[i].equals(url)) {
				synchronized (this) {
					CachedFile.Entry entry = cache.getFile(url);
					String dstr = formatDate(entry.date);
					SUN_CAPTIONS[i][2] = dstr;
					if (i == currentSunImage)
					    CharFormatter.formatString(sunCaptionBuf[2][0], 0, dstr, -1);
					break;
				}
			}
		}
	}

	
	private String formatDate(long date) {
		imageCal.setTimeInMillis(date);
		return SHORT_MONTHS[imageCal.get(Calendar.MONTH)] + " " +
						      imageCal.get(Calendar.DAY_OF_MONTH);
	}

	
	// ******************************************************************** //
	// View Control.
	// ******************************************************************** //
	
	/**
	 * Select which image is displayed in the Sun view.
	 * 
	 * @param	index			Index of the image to display.
	 */
	private void setDisplayedSunImage(int index) {
		sunImage.setDisplayedImage(index);
		sunBigImage.setDisplayedImage(index);
		String[] text = SUN_CAPTIONS[index];
        CharFormatter.formatString(sunCaptionBuf[0][0], 0, text[0], -1);
        CharFormatter.formatString(sunCaptionBuf[1][0], 0, text[1], -1);
        CharFormatter.formatString(sunCaptionBuf[2][0], 0, text[2], -1);
		
		currentSunImage = index;
	}
	
	
	// ******************************************************************** //
	// Input.
	// ******************************************************************** //

    /**
     * Handle touch screen motion events.
     * 
     * @param	event			The motion event.
     * @return					True if the event was handled, false otherwise.
     */
	@Override
	public boolean handleTouchEvent(MotionEvent event) {
        boolean done = false;
	    try {
	        final int x = (int) event.getX();
	        final int y = (int) event.getY();
	        final int action = event.getAction();

	        synchronized (this) {
	            if (action == MotionEvent.ACTION_DOWN) {
	                if ((!altMode && imageBounds != null && imageBounds.contains(x, y)) ||
	                        (altMode && imageBigBounds != null && imageBigBounds.contains(x, y))) {
	                    int i = (currentSunImage + 1) % SUN_URLS.length;
	                    setDisplayedSunImage(i);
	                    appContext.soundSecondary();
	                }
	            }
	        }
	    } catch (Exception e) {
	        appContext.reportException(e);
	    }
        return done;
	}


	// ******************************************************************** //
	// View Drawing.
	// ******************************************************************** //
	
	/**
	 * This method is called to ask the view to draw itself.
	 * 
	 * @param	canvas		Canvas to draw into.
	 * @param	now			Current system time in ms.
     * @param   bg          Iff true, tell the gauge to draw its background
     *                      first.
	 */
	@Override
	public void draw(Canvas canvas, long now, boolean bg) {
		super.draw(canvas, now, bg);
		
		if (!altMode) {
			sunImage.draw(canvas, now, bg);
			sunCaption.draw(canvas, now, bg);
			sunData.draw(canvas, now, bg);
			swindGraph.draw(canvas, now, bg);
			epamGraph.draw(canvas, now, bg);
		} else {
			sunBigImage.draw(canvas, now, bg);
			sunCaption.draw(canvas, now, bg);
			sunData.draw(canvas, now, bg);
			solGraph.draw(canvas, now, bg);
		}
	}


    // ******************************************************************** //
    // Private Classes.
    // ******************************************************************** //

	/**
     * This class helps open, create, and upgrade the solar database.
     */
    private final class DbHelper extends SQLiteOpenHelper {
    	DbHelper(Context context) {
            super(context, DB_NAME, null, DB_VER);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
    		for (WebBasedData s : ALL_SOURCES)
    			s.createTable(db);
    		FILES_SOHO.createTable(db);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldV, int newV) {
    		for (WebBasedData s : ALL_SOURCES)
    			s.upgradeTable(db, oldV, newV);
    		FILES_SOHO.upgradeTable(db, oldV, newV);
         }

        @Override
		public void onOpen(SQLiteDatabase db) {
    		for (WebBasedData s : ALL_SOURCES)
    			s.setDatabase(db);
    		FILES_SOHO.setDatabase(db);
        }

        @Override
        public void close() {
            // Make sure we don't try to use the closed database.
            for (WebBasedData s : ALL_SOURCES)
                s.setDatabase(null);
            FILES_SOHO.setDatabase(null);
            
            super.close();
        }
    }
    

    // ******************************************************************** //
    // Class Data.
    // ******************************************************************** //

    // Debugging tag.
	@SuppressWarnings("unused")
	private static final String TAG = "tricorder";
	
	// Short month names for date formatting.
	private static final String[] SHORT_MONTHS =
									new DateFormatSymbols().getShortMonths();
	 
	// Database for solar data.  Bump the version number to nuke the data
	// and start over.
	private static final String DB_NAME = "SolarData";
	private static final int DB_VER = 38;

	// Source URLs for Sun images.
	private static URL[] SUN_URLS;
	static {
		try {
			SUN_URLS = new URL[] {
				new URL("http://sohowww.nascom.nasa.gov/data/realtime/eit_304/512/latest.jpg"),
				new URL("http://sohowww.nascom.nasa.gov/data/realtime/eit_284/512/latest.jpg"),
				new URL("http://sohowww.nascom.nasa.gov/data/realtime/eit_195/512/latest.jpg"),
				new URL("http://sohowww.nascom.nasa.gov/data/realtime/eit_171/512/latest.jpg"),
				new URL("http://sohowww.nascom.nasa.gov/data/realtime/hmi_igr/512/latest.jpg"),
				new URL("http://sohowww.nascom.nasa.gov/data/realtime/hmi_mag/512/latest.jpg"),
				new URL("http://sohowww.nascom.nasa.gov/data/realtime/c2/512/latest.jpg"),
				new URL("http://sohowww.nascom.nasa.gov/data/realtime/c3/512/latest.jpg"),
			};
		} catch (MalformedURLException e) { }
	}
	private static final String[][] SUN_CAPTIONS = new String[][] {
		{ "UV 304 Å", "60-80k °K", "" },
		{ "UV 284 Å", "2M °K", "" },
		{ "UV 195 Å", "1.5M °K", "" },
		{ "UV 171 Å", "1M °K", "" },
		{ "6768 Å", "Visible", "" },
		{ "Magnetogram", "", "" },
		{ "Corona", "r=8.4M km", "" },
		{ "Corona", "r=22M km", "" },
	};
	private CachedFile FILES_SOHO =
								new CachedFile("images_soho", SUN_URLS);

	
	// Source URLs for ACE data.  See http://www.swpc.noaa.gov/ace/ and
	// http://www.swpc.noaa.gov/ftpdir/lists/ace2/README.
	private static final String URL_ACE_BASE =
						"http://www.swpc.noaa.gov/ftpdir/lists/ace2/";

//	// Solar Wind Electron Proton Alpha Monitor; hourly Averaged
//	// Real-time Bulk Parameters of the Solar Wind Plasma.
//	private static final String[] FIELDS_SWEPAM = {
//		 "status",
//		 "protons",
//         "speed",
//         "temp",
//	};
//	private static final WebBasedData SRC_SWEPAM =
//		new WebBasedData("hourly_swepam", URL_ACE_BASE, "_ace_swepam_1h.txt",
//						 3600000, true, FIELDS_SWEPAM);
//	private static final String[] SWEPAM_PLOT_FIELDS = {
//		"protons", "speed", "temp"
//	};
//	private static final float[] SWEPAM_PLOT_SCALES = {
//		100, 1, 0.003f
//	};
//	private static final int[] SWEPAM_PLOT_COLS = {
//		0xffff0000, 0xff00ff00, 0xff0000ff
//	};


	// Solar Isotope Spectrometer data; hourly Averaged Real-time Integral
	// Flux of High-energy Solar Protons.  Fields after date:
	//     ---- Integral Proton Flux ----
	//     S    > 10 MeV    S    > 30 MeV
	//     0    2.18e+00    0    1.53e+00
//	private static final String[] FIELDS_SIS = {
//		"status10", "protons10", "status30", "protons30",
//	};
//	private static final WebBasedData SRC_SIS =
//	new WebBasedData("hourly_sis", URL_ACE_BASE, "_ace_sis_1h.txt",
//					 3600000, true, FIELDS_SIS);

	// Magnetometer; hourly Averaged Real-time Interplanetary Magnetic
	// Field Values.  Fields after date:
	//     ----------------  GSM Coordinates ---------------
	//     S     Bx      By      Bz      Bt     Lat.   Long.
	//     0    -4.1     0.6    -1.1     4.3   -15.0   171.8
//	private static final String[] FIELDS_MAG = {
//		"status", "magx", "magy", "magz", "magt", "lat", "lon",
//	};
//	private static final WebBasedData SRC_MAG =
//	new WebBasedData("hourly_mag", URL_ACE_BASE, "_ace_mag_1h.txt",
//					 3600000, true, FIELDS_MAG);
//	private static final float[] SCALE_MAG = {
//	1, 1, 1, 0.5f
//};

	// Electron, Proton, and Alpha Monitor; hourly Averaged Real-time
	// Differential Electron and Proton Flux.
	//     electrons38     ~600
    //     electrons175    ~31
    //     protons47       ~8000
    //     protons115      ~400
    //     protons310      ~20
    //     protons761      ~1
    //     protons1060     ~0.4
    //     protonsL        ~1000
    //     protonsH        ~1000
	private static final String[] FIELDS_EPAM = {
		"statuse", "electrons38", "electrons175",
		"statusp", "protons47", "protons115", "protons310", "protons761",
		"protons1060", "anisotropy",
        // Synthetic fields at the end so they don't screw up the parser.
        "protonsL", "protonsM", "protonsH",
        "electronsL", "electronsH",
	};
	private WebBasedData SRC_EPAM =
		new WebBasedData("hourly_epam", URL_ACE_BASE, "_ace_epam_1h.txt",
						 3600000, true, FIELDS_EPAM) {
        @Override
        protected void process(ContentValues rec) {
            // Make up protons summaries.  Scale to ~1000.
            float pro1 = 0f;
            try {
                pro1 = rec.getAsFloat("protons47") / 8f;
                pro1 /= 1f;
            } catch (NullPointerException e) { }
            float pro2 = 0f;
            try {
                pro2 = rec.getAsFloat("protons115") * 2.5f +
                       rec.getAsFloat("protons310") * 50f;
                pro2 /= 2f;
            } catch (NullPointerException e) { }
            float pro3 = 0f;
            try {
                pro3 = rec.getAsFloat("protons761") * 1000f +
                       rec.getAsFloat("protons1060") * 2500f;
                pro3 /= 2f;
            } catch (NullPointerException e) { }
            rec.put("protonsL", pro1);
            rec.put("protonsM", pro2);
            rec.put("protonsH", pro3);

            // Make up electrons summaries.  Scale to ~1000.
            float elec1 = 0f;
            try {
                elec1 = rec.getAsFloat("electrons38") * 1.66f;
            } catch (NullPointerException e) { }
            float elec2 = 0f;
            try {
                elec2 = rec.getAsFloat("electrons175") * 33.33f;
            } catch (NullPointerException e) { }
            rec.put("electronsL", elec1);
            rec.put("electronsH", elec2);
        }
    };
	private static final String[] EPAM1_PLOT_FIELDS = {
		"protonsL", "protonsM", "protonsH"
	};
    private static final int[] EPAM1_PLOT_COLS = {
        0xff00d0ff, 0xffffff00, 0xfff00000
    };
    private static final String[] EPAM2_PLOT_FIELDS = {
        "electronsL", "electronsH"
    };
    private static final int[] EPAM2_PLOT_COLS = {
        0xff00d0ff, 0xfff00000
    };

	// Daily solar data such as sunspot numbers.
	private static final String URL_DSD =
						"http://www.swpc.noaa.gov/ftpdir/latest/DSD.txt";
	private static final String[] FIELDS_DSD = {
		"rflux", "nsunspot", "asunspot", "sunspotnew", "field", "xraybg",
		"flaresxc", "flaresxm", "flaresxx", "flaresxs",
		"flareso1", "flareso2", "flareso3",
		// Synthetic fields at the end so they don't screw up the parser.
		"xflares", "oflares", "flares"
	};
	private WebBasedData SRC_DSD =
		new WebBasedData("daily_solar", URL_DSD, null,
						 24 * 3600000, false, FIELDS_DSD) {
		@Override
		protected void process(ContentValues rec) {
		    // Make up flares totals.  Use try to protect against missing
		    // fields.
		    int xflares = 0;
		    try {
		        xflares = rec.getAsInteger("flaresxc") +
		                  rec.getAsInteger("flaresxm") +
		                  rec.getAsInteger("flaresxx") +
		                  rec.getAsInteger("flaresxs");
		    } catch (NullPointerException e) { }
		    int oflares = 0;
		    try {
		        oflares += rec.getAsInteger("flareso1");
		    } catch (NullPointerException e) { }
            try {
                oflares += rec.getAsInteger("flareso2");
            } catch (NullPointerException e) { }
            try {
                oflares += rec.getAsInteger("flareso3");
            } catch (NullPointerException e) { }
		    rec.put("xflares", xflares);
		    rec.put("oflares", oflares);
		    rec.put("flares", xflares + oflares);
		}
	};
	private static final String[] DSD_PLOT_FIELDS = {
		"nsunspot", "oflares", "xflares"
	};
	private static final float DSD_PLOT_TIMESCALE = 6;
	private static final int[] DSD_PLOT_COLS = { 
		0xffff0000, 0xff00ffff, 0xfff000ff,
	};


	// Our data sources.
	private WebBasedData[] ALL_SOURCES = {
		/* SRC_SWEPAM, */ SRC_EPAM, SRC_DSD
	};
	
	// Grid and plot colours.
	private static final int COLOUR_GRID = 0xfff0a082;
	private static final int COLOUR_PLOT = 0xffd09cd0;

	
	// ******************************************************************** //
	// Private Data.
	// ******************************************************************** //

    // Application handle.
    private final Tricorder appContext;
    
	// Database open/close helper.
	private DbHelper databaseHelper = null;
	
	// The actual database, if open.
	private SQLiteDatabase database = null;

	// Image atom where we display the Sun's image, and its caption and
	// data display.
	private ImageAtom sunImage;
    private Rect imageBounds;
	private TextGauge sunCaption;
	private TextGauge sunData;
	
	// Text field buffers for the Sun captions and the Sun data display.
    private char[][][] sunCaptionBuf;
    private char[][][] sunDataBuf;

	// Positions of the caption and data in both modes.
    private Rect sunCaptionStd;
    private Rect sunCaptionAlt;
    private Rect sunDataStd;
    private Rect sunDataAlt;

	// Big sun display for alternate mode.
	private ImageAtom sunBigImage;
    private Rect imageBigBounds;

	// Which sun index we're displaying.
	private int currentSunImage = 0;

	// Graphs for solar wind data and magnetic field strengths.
	private MagnitudeElement swindGraph;
    private Rect swindBounds;
	private MagnitudeElement epamGraph;
    private Rect epamBounds;
	private MagnitudeElement solGraph;
    private Rect solBounds;
   
    // Alternate display mode flag.  In alt mode we show the large sun.
    private boolean altMode = false;

    // Calendar used for formatting image dates.
    private Calendar imageCal;
    
    // Time in ms at which we last checked our data for currency.
    private long lastDataCheck = 0;
    
}

