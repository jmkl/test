
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

import java.io.File;
import java.net.URL;
import java.util.HashMap;
import java.util.Observable;
import java.util.Observer;

import org.hermit.android.core.SurfaceRunner;
import org.hermit.android.instruments.Gauge;
import org.hermit.android.net.CachedFile;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Handler;


/**
 * An atom which draws an image from the web.
 */
class ImageAtom
	extends Gauge
	implements Observer
{

	// ******************************************************************** //
	// Constructor.
	// ******************************************************************** //

	/**
	 * Set up this atom.
	 * 
     * @param   parent          Parent surface.
     * @param	cache			File cache which will hold the image files.
	 * @param	urls			URLs of the specific  images we will want
	 * 							to display.
	 */
	ImageAtom(SurfaceRunner parent, CachedFile cache, URL[] urls) {
		super(parent);

		fileCache = cache;
		imageUrls = urls;

		imageCache = new HashMap<URL, Bitmap>();
		currentImage = imageUrls[0];
		currentBitmap = null;
		statusString = parent.getRes(R.string.msgLoading);
		
		loadHandler = new Handler();
		
		fileCache.addObserver(this);
	}

   
    // ******************************************************************** //
	// Geometry.
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
		
		imageX = bounds.left;
		imageY = bounds.top;
		imageWidth = bounds.right - bounds.left;
		imageHeight = bounds.bottom - bounds.top;
		
		// Clear the image cache and the current bitmap.
		imageCache.clear();
		currentBitmap = null;
		
		// Add all the URLs to the image cache.  Since we may have the
		// cache database already, try to get the images.
		long delay = 500;
		for (URL url : imageUrls) {
		    imageCache.put(url, null);
		    loadHandler.postDelayed(new Loader(url), delay);
		    delay += 200;
		}
	}

	
	private final class Loader implements Runnable {
	    Loader(URL url) {
	        imgUrl = url;
	    }
        @Override
        public void run() {
            synchronized (imageCache) {
                CachedFile.Entry entry = fileCache.getFile(imgUrl);
                if (entry.path != null)
                    imageLoaded(imgUrl, entry.path);
            }
        }
        private final URL imgUrl;
	}
	

	// ******************************************************************** //
	// Image Loading.
	// ******************************************************************** //

	/**
	 * This method is invoked when a file is loaded by the file cache.
	 * 
	 * @param	o   			The cached file that was loaded.
	 * @param	arg				The URL of the file that was loaded.
	 */
	public void update(Observable o, Object arg) {
		if (!(o instanceof CachedFile) || !(arg instanceof URL))
			return;

		CachedFile cache = (CachedFile) o;
		URL url = (URL) arg;
		CachedFile.Entry entry = cache.getFile(url);

		synchronized (imageCache) {
			// Make sure it's an image we're interested in.
			if (!imageCache.containsKey(url))
				return;

			// Get the info on the cached file.  Note that the file
			// may have been invalidated since the notification was sent.
			if (entry.path != null)
				imageLoaded(url, entry.path);
		}
	}
	

	/**
	 * This method is invoked when a file is loaded by the file cache.
	 * 
	 * @param	url				The URL of the file that was loaded.
	 * @param	path			The path of the local copy of the file.
	 */
	private void imageLoaded(URL url, File path) {
		// If we don't have a size yet, forget it.
		if (imageWidth == 0 || imageHeight == 0)
			return;
		
		// Load the bitmap.  If if fails, tell the cache we have a
		// corrupted file.
        Bitmap img = null;
		try {
		    img = BitmapFactory.decodeFile(path.getPath());
	        if (img == null) {
	            // Can't load it?  Corrupted?  Invalidate the cache.
	            // But maybe we shouldn't be so hasty...
//	          Log.i(TAG, "ImageAtom: invalidate " + path.getPath());
//	          fileCache.invalidate(url);
	            return;
	        }
		} catch (OutOfMemoryError e) {
		    // Can't load it right now.
		    return;
		}
		
		// Scale the bitmap to size and cache the scaled version.
		img = Bitmap.createScaledBitmap(img, imageWidth, imageHeight, true);
		imageCache.put(url, img);
		
		// Set as the current image, if it is.
		if (currentImage.equals(url))
			currentBitmap = img;
	}
	

	// ******************************************************************** //
	// View Control.
	// ******************************************************************** //
	
	/**
	 * Select which image is displayed in the view.
	 * 
	 * @param	index			Index of the image to display.
	 */
	public void setDisplayedImage(int index) {
		synchronized (imageCache) {
			currentImage = imageUrls[index];
			currentBitmap = imageCache.get(currentImage);
		}
	}
	

	// ******************************************************************** //
	// View Drawing.
	// ******************************************************************** //
	
	/**
	 * This method is called to ask the element to draw itself.
	 * 
	 * @param	canvas		Canvas to draw into.
	 * @param	paint		The Paint which was set up in initializePaint().
     * @param   now         Nominal system time in ms. of this update.
	 */
	@Override
	protected void drawBody(Canvas canvas, Paint paint, long now) {
		// Drawing is easy, if the image is loaded.
		if (currentBitmap != null)
			canvas.drawBitmap(currentBitmap, imageX, imageY, null);
		else {
			paint.setColor(0xffffffff);
			canvas.drawText(statusString, imageX + 4, imageY + 14, paint);
		}
	}

	
    // ******************************************************************** //
    // Class Data.
    // ******************************************************************** //

    // Debugging tag.
	@SuppressWarnings("unused")
	private static final String TAG = "tricorder";
	

	// ******************************************************************** //
	// Private Data.
	// ******************************************************************** //

	// The list of URLs we're to display.
	private URL[] imageUrls = null;
	
	// Current image position and size.
	private int imageX = 0;
	private int imageY = 0;
	private int imageWidth = 0;
	private int imageHeight = 0;
	
	// File cache our images are saved in.  These are the raw images off
	// the web, in whatever their original size is.
	private CachedFile fileCache = null;

	// Cache of displayed images.  These are scaled to our current size.
	private HashMap<URL, Bitmap> imageCache = null;
	
	// Handler used to schedule loading and scaling of images, so it doesn't
	// freeze the main thread.
	private Handler loadHandler = null;

	// The currently displayed image's URL, and its Bitmap.  Bitmap
	// is null if not loaded yet.
	private URL currentImage = null;
	private Bitmap currentBitmap = null;
	
	// Status string displayed in place of the image.
	private String statusString = null;
	
}

