
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

import java.util.List;

import org.hermit.android.instruments.Element;
import org.hermit.android.instruments.TextAtom;
import org.hermit.tricorder.Tricorder.Sound;
import org.hermit.utils.CharFormatter;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.net.wifi.ScanResult;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.telephony.CellLocation;
import android.telephony.NeighboringCellInfo;
import android.telephony.PhoneStateListener;
import android.telephony.ServiceState;
import android.telephony.TelephonyManager;
import android.telephony.gsm.GsmCellLocation;


/**
 * A view which displays several scalar parameters as graphs.
 */
class CommView
	extends DataView
{


    // ******************************************************************** //
	// Constructor.
	// ******************************************************************** //

	/**
	 * Set up this view.
	 * 
	 * @param	context			Parent application context.
	 */
	public CommView(Tricorder context) {
		super(context);
		
		appContext = context;
		
		// Get the information providers we need.
        telephonyManager =
        	(TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
		
        // Create the section header bars.
		String[] cfields = { getRes(R.string.lab_cell) , "", "999 days 23h" };
		cellHead = new HeaderBarElement(context, cfields);
		cellHead.setBarColor(0xffdfb682);
		cellHead.setText(0, 0, getRes(R.string.lab_cell));
		String[] wfields = {
			getRes(R.string.lab_wifi), "xx", "999 days 23h"

		};
		wifiHead = new HeaderBarElement(context, wfields);
		wifiHead.setBarColor(0xffdfb682);
		wifiHead.setText(0, 0, getRes(R.string.lab_wifi));
		
		// Create the label.
		String[] wsfields = { getRes(R.string.msgPoweringOff) };
    	wifiStatus = new TextAtom(context, wsfields, 1);
    	wifiStatus.setTextSize(getMiniTextSize());
    	wifiStatus.setTextColor(COLOUR_PLOT);
    	wifiStatusBuffer = wifiStatus.getBuffer();
    	CharFormatter.formatString(wifiStatusBuffer[0][0], 0, "?", -1);

        // Add the cellular bar graph, displaying ASU.  This snippet from
		// PhoneStateIntentReceiver.java: 
		//     For GSM, dBm = -113 + 2*asu
		//     - ASU=0 means "-113 dBm or less"
		//     - ASU=31 means "-51 dBm or greater"
		//     Current signal strength in dBm ranges from -113 - -51dBm
		// We'll assume an ASU range from 0 to 31.
		String[] ctext = { "000000000", "#", "T - Mobile X", "00" };
		cellBar = new BargraphElement(context, 5f, 6.2f,
									  COLOUR_GRID, COLOUR_PLOT, ctext, 1);
        cellBar.clearCid();
        cellBar.setLabel(getRes(R.string.msgNoData));
        cellBar.clearAsu();
		cellBars = new BargraphElement[MAX_CELL];
		for (int c = 0; c < MAX_CELL; ++c) {
			cellBars[c] = new BargraphElement(context, 5f, 6.2f,
					                          COLOUR_GRID, COLOUR_GRID, ctext, 1);
	        cellBars[c].clearCid();
	        cellBars[c].setLabel(getRes(R.string.msgNoData));
	        cellBars[c].clearAsu();
		}
		
		// Create the list of WiFi bargraphs, displaying ASU.  We'll assume
		// a WiFi ASU range from 0 to 41.
		wifiBars = new BargraphElement[MAX_WIFI];
		String[] wtext = { "2.456", "#", "aw19.alamedawireless.o", "00" };
		for (int w = 0; w < MAX_WIFI; ++w) {
			wifiBars[w] = new BargraphElement(context, 5f, 8.2f,
											  COLOUR_GRID, COLOUR_PLOT,
					  						  wtext, 1);
		}
    	
    	// Create the right-side bars.
    	cRightBar = new Element(context);
    	cRightBar.setBackgroundColor(COLOUR_GRID);
    	wLeftBar = new Element(context);
    	wLeftBar.setBackgroundColor(COLOUR_GRID);
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
	      
        if (bounds.right - bounds.left < bounds.bottom - bounds.top)
            layoutPortrait(bounds);
        else
            layoutLandscape(bounds);
    }


    /**
     * Set up the layout of this view in portrait mode.
     * 
     * @param   bounds      The bounding rect of this element within
     *                      its parent View.
     */
    private void layoutPortrait(Rect bounds) {
        int bar = getSidebarWidth();
		int pad = getInterPadding();
		
		int sx = bounds.left + pad;
		int y = bounds.top;
		
		y = layoutCells(sx, y, bounds.right, bounds.bottom, bar, pad);
		y += pad;
		y = layoutWifi(sx, y, bounds.right, bounds.bottom, bar, pad);
    }


    /**
     * Set up the layout of this view in portrait mode.
     * 
     * @param   bounds      The bounding rect of this element within
     *                      its parent View.
     */
    private void layoutLandscape(Rect bounds) {
        int bar = getSidebarWidth();
        int pad = getInterPadding();
        
        int sx = bounds.left + pad;
        int ex = bounds.right;
        int cw = (ex - sx) / 2 - pad;
        int x = sx;
        
        layoutCells(x, bounds.top, x + cw, bounds.bottom, bar, pad);
        x += cw + pad;
        layoutWifi(x, bounds.top, bounds.right, bounds.bottom, bar, pad);
    }


    /**
     * Lay out the WiFi bars.
     */
    private int layoutCells(int sx, int sy, int ex, int ey, int bar, int pad) {
        int cheadHeight = cellHead.getPreferredHeight();
        int cbarHeight = cellBar.getPreferredHeight();
        int gap = getInnerGap();
        int y = sy;
        
        // Lay out the cellular heading.
        cellHead.setGeometry(new Rect(sx, y, ex, y + cheadHeight));
        y += cheadHeight + gap;

        // Position the right side bar.
        int bars = cbarHeight * (MAX_CELL + 1) + gap * MAX_CELL;
        cRightBar.setGeometry(new Rect(ex - bar, y, ex, y + bars));
        ex -= bar + pad;
        
        // Lay out the connected cell signal bar.
        cellBar.setGeometry(new Rect(sx, y, ex, y + cbarHeight));
        y += cbarHeight + gap;
        
        // Place all the neighboring bars.
        for (int i = 0; i < MAX_CELL; ++i) {
            cellBars[i].setGeometry(new Rect(sx, y, ex, y + cbarHeight));
            y += cbarHeight + gap;
        }
        
        return y - gap;
    }


    /**
     * Lay out the WiFi bars.
     */
    private int layoutWifi(int sx, int sy, int ex, int ey, int bar, int pad) {
        int wheadHeight = wifiHead.getPreferredHeight();
        int wbarHeight = wifiStatus.getPreferredHeight();
        int gap = getInnerGap();
        int y = sy;
        
        wifiHead.setGeometry(new Rect(sx, y, ex, y + wheadHeight));
        y += wheadHeight + gap;
        
        // Now set up the right bar.
        wLeftBar.setGeometry(new Rect(ex - bar, y, ex, ey));
        ex -= bar + pad;

        // Place the WiFi status field.
        wifiStatus.setGeometry(new Rect(sx, y, ex, y + wbarHeight));
        y += wbarHeight + gap;

        // Place all the WiFi bars.
        for (int i = 0; i < MAX_WIFI; ++i) {
            int bh = wifiBars[i].getPreferredHeight();
            wifiBars[i].setGeometry(new Rect(sx, y, ex, y + bh));
            y += bh + gap;
        }
        
        return y - gap;
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
	public void appStart() {
        // Register for the intents that tell us about WiFi state changes.
        // Thanks to Kent for the tip.
        IntentFilter wifiFilter = new IntentFilter();
        wifiFilter.addAction(WifiManager.NETWORK_IDS_CHANGED_ACTION);
        wifiFilter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        wifiFilter.addAction(WifiManager.RSSI_CHANGED_ACTION);
        wifiFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        wifiFilter.addAction(WifiManager.SUPPLICANT_CONNECTION_CHANGE_ACTION);
        wifiFilter.addAction(WifiManager.SUPPLICANT_STATE_CHANGED_ACTION);
        wifiFilter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
        appContext.registerReceiver(wifiListener, wifiFilter);

        // Get the initial status, or any status that may have changed
		// while we were paused.
		getWifiState();
		getWifiConnection();
	}
	

	/**
	 * Start this view.  This notifies the view that it should start
	 * receiving and displaying data.  The view will also get tick events
	 * starting here.
	 */
	@Override
	public void start() {
		// Register for telephony events.
        telephonyManager.listen(phoneListener,
						        PhoneStateListener.LISTEN_CALL_STATE |
						        PhoneStateListener.LISTEN_CELL_LOCATION |
						        PhoneStateListener.LISTEN_DATA_ACTIVITY |
						        PhoneStateListener.LISTEN_DATA_CONNECTION_STATE |
						        PhoneStateListener.LISTEN_SERVICE_STATE |
						        PhoneStateListener.LISTEN_SIGNAL_STRENGTH);
        
        // We already set up WiFi monitoring in appStart().
        
        viewRunning = true;
    }
	
	
	/**
	 * A 1-second tick event.  Can be used for housekeeping and
	 * async updates.
	 * 
	 * @param	time				The current time in millis.
	 */
	@Override
	public void tick(long time) {
		if (wifiPowerState == WifiManager.WIFI_STATE_ENABLED) {
			long age = (time - wifiSignalsTime) / 1000;
			if (wifiSignalsTime == 0)
				wifiHead.setText(0, 2, getRes(R.string.msgNoData));
			else
				wifiHead.setText(0, 2, elapsed(age));

			age = (time - cellSignalsTime) / 1000;
			if (cellSignalsTime == 0)
				cellHead.setText(0, 2, getRes(R.string.msgNoData));
			else
				cellHead.setText(0, 2, elapsed(age));
			
			// Scan for WiFi networks every WIFI_SCAN_INTERVAL secs at most.
			long scan = (time - wifiScanTime) / 1000;
			if (age > WIFI_SCAN_INTERVAL && scan > WIFI_SCAN_INTERVAL) {
				wifiManager.startScan();
				wifiScanTime = time;
			}
		}
	}
	
	
	/**
	 * This view's aux button has been clicked.  Toggle the WiFi power.
	 */
	@Override
	public void auxButtonClick() {
		boolean enable = !wifiManager.isWifiEnabled();
		appContext.postSound(enable ? Sound.BOOP_BEEP : Sound.BEEP_BOOP);
		wifiManager.setWifiEnabled(enable);
		
		// The system starts a scan here, so there's no point in us
		// doing it.
		wifiScanTime = System.currentTimeMillis();
	}
	

	/**
	 * Stop this view.  This notifies the view that it should stop
	 * receiving and displaying data, and generally stop using
	 * resources.
	 */
	@Override
	public void stop() {
		viewRunning = false;
		 
        telephonyManager.listen(phoneListener,
		        			    PhoneStateListener.LISTEN_NONE);
        
        // Don't unregister for WiFi results.  These come in so slowly
        // we'll take them when we get them -- as long as the app is running.
	}

	
	/**
	 * Notification that the overall application is stopping (possibly
	 * to pause).  Views can use this to stop any long-term activity.
	 */
	@Override
	public void appStop() {
		// Unregister our Wifi listener.
		appContext.unregisterReceiver(wifiListener);
	}
	

	/**
	 * The PhoneStateListener is notified of any changes in the phone
	 * strate.
	 */
	private PhoneStateListener phoneListener = new PhoneStateListener() {
		
		@Override
		public void onCallStateChanged(int state, String incomingNumber) {

		}
		
		@Override
		public void onCellLocationChanged(CellLocation location) {
			synchronized (this) {
				if (location instanceof GsmCellLocation) {
					GsmCellLocation gloc = (GsmCellLocation) location;
					cellId = gloc.getCid();
					updateHead();
				}
			}
		}
		
		@Override
		public void onDataActivity(int direction) {
			synchronized (this) {
				switch (direction) {
				case TelephonyManager.DATA_ACTIVITY_NONE:
					cellHead.setTopIndicator(false, 0xffff0000);
					cellHead.setBotIndicator(false, 0xff00a000);
					break;
				case TelephonyManager.DATA_ACTIVITY_IN:
					cellHead.setTopIndicator(false, 0xffff0000);
					cellHead.setBotIndicator(true, 0xff00a000);
					break;
				case TelephonyManager.DATA_ACTIVITY_OUT:
					cellHead.setTopIndicator(true, 0xffff0000);
					cellHead.setBotIndicator(false, 0xff00a000);
					break;
				case TelephonyManager.DATA_ACTIVITY_INOUT:
					cellHead.setTopIndicator(true, 0xffff0000);
					cellHead.setBotIndicator(true, 0xff00a000);
					break;
				}
			}
		}
		
		@Override
		public void onDataConnectionStateChanged(int state) {

		}
		
		@Override
		public void onServiceStateChanged(ServiceState serviceState) {
			synchronized (this) {
				cellState = serviceState.getState();
				cellOp = serviceState.getOperatorAlphaLong();
				updateHead();
				if (cellState == ServiceState.STATE_OUT_OF_SERVICE ||
								cellState == ServiceState.STATE_POWER_OFF)
					cellBar.clearValue();
			}
		}
		
		@Override
		public void onSignalStrengthChanged(int asu) {
			synchronized (this) {
				cellAsu = asu;
				updateHead();
				if (cellState == ServiceState.STATE_IN_SERVICE ||
						cellState == ServiceState.STATE_EMERGENCY_ONLY)
					cellBar.setValue(cellAsu);
			}
		}
		
		private void updateHead() {
			// Update the header widget.
			switch (cellState) {
			case ServiceState.STATE_IN_SERVICE:
			case ServiceState.STATE_EMERGENCY_ONLY:
			    String label = cellOp;
			    if (cellState == ServiceState.STATE_EMERGENCY_ONLY)
			        label += "!";
				
				cellBar.setCid(cellId);
				cellBar.setLabel(label);
				cellBar.setAsu(cellAsu);
				break;
			case ServiceState.STATE_OUT_OF_SERVICE:
                cellBar.clearCid();
                cellBar.setLabel(getRes(R.string.msgNoSignal));
                cellBar.clearAsu();
                break;
			case ServiceState.STATE_POWER_OFF:
                cellBar.clearCid();
                cellBar.setLabel(getRes(R.string.msgNoPower));
                cellBar.clearAsu();
                break;
			default:
                cellBar.clearCid();
                cellBar.setLabel(getRes(R.string.msgNoData));
                cellBar.clearAsu();
                break;
			}

        	getNeighbor();
		}
		
	};


	/**
	 * The BroadcastReceiver receives the intent
	 * WifiManager.SCAN_RESULTS_AVAILABLE_ACTION, as set up in our
	 * constructor.  This tells us (and anyone else who cares) that a
	 * WiFi scan has completed.  We can get the results from
	 * wifiManager.getScanResults().
	 */
	private BroadcastReceiver wifiListener = new BroadcastReceiver() {
		@Override
		public void onReceive(Context c, Intent i) {
			final String action = i.getAction();
	        if (action.equals(WifiManager.NETWORK_IDS_CHANGED_ACTION))
	        	;
	        else if (action.equals(WifiManager.NETWORK_STATE_CHANGED_ACTION))
	        	getWifiConnection();
	        else if (action.equals(WifiManager.RSSI_CHANGED_ACTION))
	        	getWifiConnection();
	        else if (action.equals(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION))
	        	getScanResults();
	        else if (action.equals(WifiManager.SUPPLICANT_CONNECTION_CHANGE_ACTION))
	        	getWifiConnection();
	        else if (action.equals(WifiManager.SUPPLICANT_STATE_CHANGED_ACTION))
	        	getWifiConnection();
	        else if (action.equals(WifiManager.WIFI_STATE_CHANGED_ACTION))
	        	getWifiState();
		}
	};


	/**
	 * Get the current WiFi power state.  Update the display.
	 */
	private void getWifiState() {
		synchronized (this) {
			wifiPowerState = wifiManager.getWifiState();
			wifiPowerName = "?";
			wifiRunning = false;
			switch (wifiPowerState) {
			case WifiManager.WIFI_STATE_DISABLED:
				wifiPowerName = getRes(R.string.msgNoPower);
				break;
			case WifiManager.WIFI_STATE_DISABLING:
				wifiPowerName = getRes(R.string.msgPoweringOff);
				break;
			case WifiManager.WIFI_STATE_ENABLED:
				wifiRunning = true;
				wifiPowerName = getRes(R.string.msgEnabled);
				break;
			case WifiManager.WIFI_STATE_ENABLING:
				wifiPowerName = getRes(R.string.msgPoweringOn);
				break;
			case WifiManager.WIFI_STATE_UNKNOWN:
				wifiPowerName = getRes(R.string.msgNoData);
				break;
			}

			// If the WiFi is running, kick off a WiFi scan.  We do this here,
			// since the results come in so slowly we may as well get them now.
			// If we're not running, then any signal and connection data
			// is stale.
			if (wifiPowerState == WifiManager.WIFI_STATE_ENABLED) {
				long time = System.currentTimeMillis();
				long age = (time - wifiSignalsTime) / 1000;
				long scan = (time - wifiScanTime) / 1000;
				if (age > WIFI_SCAN_INTERVAL && scan > WIFI_SCAN_INTERVAL) {
					wifiManager.startScan();
					wifiScanTime = time;
				}
			} else {
				wifiSignals = null;
				wifiSignalsTime = 0;
				wifiConnection = null;
				wifiHead.setText(0, 2, "");
			}

			updateStatus();
		}
	}


	/**
	 * Get the latest WiFi connection status.  Update the display.
	 */
	private void getWifiConnection() {
		synchronized (this) {
			// If WiFi is off, don't bother.
			if (wifiPowerState != WifiManager.WIFI_STATE_ENABLED)
				return;

			wifiConnection = wifiManager.getConnectionInfo();

			SupplicantState sstate = wifiConnection.getSupplicantState();
			int format = 0;
			wifiConnState = "?";
			switch (sstate) {
			case ASSOCIATED:
				// Association completed.
				format = R.string.msgAssociated;
				break;
			case ASSOCIATING:
				// Trying to associate with a BSS/SSID. 
				format = R.string.msgAssociating;
				break;
			case COMPLETED:
				// All authentication completed. 
				// Trying to associate with a BSS/SSID. 
				format = R.string.msgConnected;
				break;
			case DISCONNECTED:
				// This state indicates that client is not associated, but is
				// likely to start looking for an access point. 
				wifiConnState = getRes(R.string.msgOffline);
				break;
			case DORMANT:
				// An Android-added state that is reported when a client issues an explicit DISCONNECT command. 
				wifiConnState = getRes(R.string.msgDormant);
				break;
			case FOUR_WAY_HANDSHAKE:
			case GROUP_HANDSHAKE:
				// WPA 4-Way Key Handshake in progress. 
				// WPA Group Key Handshake in progress. 
				format = R.string.msgAuthenticating;
				break;
			case INACTIVE:
				// Inactive state (wpa_supplicant disabled). 
				wifiConnState = getRes(R.string.msgNoSignal);
				break;
			case INVALID:
				// A pseudo-state that should normally never be seen. 
				wifiConnState = getRes(R.string.msgInvalid);
				break;
			case SCANNING:
				// Scanning for a network.
				wifiConnState = getRes(R.string.msgScanning);
				break;
			case UNINITIALIZED:
				// No connection to wpa_supplicant. 
				wifiConnState = getRes(R.string.msgNoData);
				break;
			}

			// If we have a formatted status, format it now.
			if (format != 0) {
				wifiConnState = String.format(getRes(format),
						wifiConnection.getSSID(),
						wifiConnection.getRssi());
			}

			updateStatus();
		}
	}

	
	/**
	 * Update the WiFi status field.
	 */
	private void updateStatus() {
		if (!wifiRunning)
		    CharFormatter.formatString(wifiStatusBuffer[0][0], 0, wifiPowerName, -1);
		else
	        CharFormatter.formatString(wifiStatusBuffer[0][0], 0, wifiConnState, -1);
		getScanResults();
	}

	
	private void getNeighbor() {
	    final List<NeighboringCellInfo> csigs = telephonyManager.getNeighboringCellInfo();
	    final long ctime = System.currentTimeMillis();
		
		synchronized (this) {
			cellSignals = csigs;
			cellSignalsTime = ctime;
			
			if (cellSignals == null) return;
			
			int w = 0;
			
			for (NeighboringCellInfo scan : cellSignals) {
				if (w >= MAX_CELL)
					break;

				BargraphElement bar = cellBars[w++];

				final int asu = scan.getRssi()/2;

                bar.setCid(scan.getCid());
                bar.setLabel("");
                bar.setAsu(asu);

				bar.setValue(asu);
			}
		}
	}
	
	/**
	 * Get the latest WiFi network scan results.  Update the display.
	 */
	private void getScanResults() {
		int strongest = 0;
		
		final List<ScanResult> wsigs = wifiManager.getScanResults();
		final long wtime = System.currentTimeMillis();
		
		synchronized (this) {
			wifiSignals = wsigs;
			wifiSignalsTime = wtime;
			
			if (wifiSignals == null) return;
			
			int w = 0;
			for (ScanResult scan : wifiSignals) {
				if (w >= MAX_WIFI)
					break;

				BargraphElement bar = wifiBars[w++];

				// Convert the value to ASU and display it.  See note
				// in the constructor.
				//     dBm = -113 + 2*asu
				final int asu = Math.round((scan.level + 113f) / 2f);
				if (asu > strongest)
					strongest = asu;

				// Decode the encryption mode, and represent it as a char.
				String crypt = scan.capabilities;
				char mode = ' ';
				if (crypt == null || crypt.length() == 0)
				    ;
                else if (crypt.startsWith("[IBSS"))
                    mode = '!';
				else if (crypt.startsWith("[WEP"))
				    mode = '*';
				else if (crypt.startsWith("[WPA"))
				    mode = '#';
				else
				    mode = '?';

                bar.setFlag(mode);
				bar.setFreq(scan.frequency / 1000f);
                bar.setLabel(scan.SSID);
                bar.setAsu(asu);
				bar.setDataColors(COLOUR_GRID,wifiConnection != null && 
						scan.BSSID.equals(wifiConnection.getBSSID())?
						COLOUR_PLOT:COLOUR_GRID);

				bar.setValue(asu);
			}
		}

		// If we're the visible view, make a sonar ping based on the
		// strongest signal.
		if (viewRunning) {
		    int strength = (int) ((float) strongest / 41f * 100f);
		    appContext.postPing(strength > 100 ? 100 : strength);
		}
	}
	

	// ******************************************************************** //
	// View Drawing.
	// ******************************************************************** //
	
	/**
	 * This method is called to ask the view to draw itself.
	 * 
	 * @param	canvas			Canvas to draw into.
	 * @param	now				Current system time in ms.
	 */
	@Override
	public void draw(Canvas canvas, long now) {
		super.draw(canvas, now);
		
		// Draw the graph views.
		cellHead.draw(canvas, now);
		cRightBar.draw(canvas, now);
		cellBar.draw(canvas, now);
		wifiHead.draw(canvas, now);
		wLeftBar.draw(canvas, now);
		wifiStatus.draw(canvas, now);

		// Draw the WiFi bars, as many as we have.
		synchronized (this) {
		    if (wifiSignals != null) {
		        for (int w = 0; w < wifiSignals.size() && w < MAX_WIFI; ++w)
		            wifiBars[w].draw(canvas, now);
		    }
		}

		synchronized (this) {
		    if (cellSignals != null) {
		        for (int w = 0; w < cellSignals.size() && w < MAX_CELL; ++w)
		            cellBars[w].draw(canvas, now);
		    }
		}
	}


    // ******************************************************************** //
    // Utilities.
    // ******************************************************************** //

	/**
	 * Convert an elapsed time into a user-friendly textual description.
	 * 
	 * @param millis			Elapsed time in seconds.
	 * @return					A human-friendly description of that time.
	 */
	private static final String elapsed(long secs) {
		long mins = secs / 60;
		long hours = mins / 60;
		long days = hours / 24;
		
		if (mins < 1)
			return "" + secs + "s";
		
		if (mins < 5)
			return "" + mins + "m " + secs % 60 + "s";
		if (hours < 1)
			return "" + mins + "m";
		
		if (hours < 5)
			return "" + hours + "h " + mins % 60 + "m";
		if (days < 1)
			return "" + hours + "h";
		
		if (days < 5)
			return "" + days + " days " + hours % 24 + "h";
		return "" + days + " days";
	}
	

    // ******************************************************************** //
    // Class Data.
    // ******************************************************************** //

    // Debugging tag.
	@SuppressWarnings("unused")
	private static final String TAG = "tricorder";
		
	// The maximum number of WiFi signals we will show.
	private static final int MAX_WIFI = 10;
	private static final int MAX_CELL = 2;
	
	// Minimum interval in seconds between WiFi scans.
	private static final int WIFI_SCAN_INTERVAL = 3;
	
	// Grid and plot colours.
	private static final int COLOUR_GRID = 0xffdfb682;
	private static final int COLOUR_PLOT = 0xffd09cd0;

	
	// ******************************************************************** //
	// Private Data.
	// ******************************************************************** //
	
	// The application context we're running in.
	private Tricorder appContext;
	
    // The telephony manager, for cellular state updates.
	private TelephonyManager telephonyManager;

	// The WiFi manager, for WiFi state updates.
	private WifiManager wifiManager;
	
	// The header bars for the cellular and WiFi sections.
	private HeaderBarElement cellHead;
	private HeaderBarElement wifiHead;

	// The bargraph for cellular.
	private BargraphElement cellBar;
	
	// Text field for displaying the WiFi status.
	private TextAtom wifiStatus;

    // Buffer where the WiFi status field contents are stored.
    private char[][][] wifiStatusBuffer;

	// The bargraphs for WiFi.  The number of these changes as we
	// scan for WiFi signals.
	private BargraphElement[] wifiBars;
	private BargraphElement[] cellBars;
	
	// The left-side bars for cell and wifi (just solid colour bars).
	private Element cRightBar;
	private Element wLeftBar;

	// Current cell state, operator, cell ID and signal strength.
	private int cellState = ServiceState.STATE_OUT_OF_SERVICE;
	private String cellOp = "";
	private int cellId = 0;
	private int cellAsu = 0;
	
	// Most recent WiFi scan results.  Null if no data.
	private List<ScanResult> wifiSignals = null;
	private List<NeighboringCellInfo> cellSignals = null;
	
	// Time at which we last started a WiFi scan, and the time we last
	// got scan data.
	private long wifiScanTime = 0;
	private long wifiSignalsTime = 0;
	private long cellSignalsTime = 0;
	
	// Current EiFi connection state.  Null if no data.
	private WifiInfo wifiConnection = null;
	
	// Flag whether WiFi is currently powered up.
	private boolean wifiRunning = false;
	
	// Current WiFi power state and connection state.
	private int wifiPowerState = WifiManager.WIFI_STATE_UNKNOWN;
	private String wifiPowerName = null;
	private String wifiConnState = null;
	
	// Flag whether this view is the up-front view.
	private boolean viewRunning = false;

}

