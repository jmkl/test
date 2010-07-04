
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

import java.util.HashMap;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.RectF;
import android.util.Log;


/**
 * Class representing a graphic drawn on the game board.  It doesn't
 * interact with the game in any way, it's just decoration.  The image
 * is static, and can't be turned on or off.
 */
class Graphic
	extends Visual
{
	
	// ******************************************************************** //
	// Constructor.
	// ******************************************************************** //
	
	/**
	 * Create a graphic which displays an image.
	 * 
	 * @param	app			Application context.  This provides
	 * 						access to resources and image loading.
	 * @param	id			The ID of this element.
	 * @param	imgId		Resource ID of the graphic.
	 * @param	xform		Transform to apply to the raw data.
	 * @param	norotate	If true, do *not* rotate the image for display
	 * 						on different screen orientations.  This means
	 * 						that the image top will always be towards
	 * 						screen top.  (In other words, the image
	 * 						effectively rotates with the screen -- think
	 * 						about it).  If false, the image is rotated
	 * 						so it always lines up the same way with the
	 * 						game board.
	 */
	public Graphic(Plughole app, String id, int imgId,
				   Matrix xform, boolean norotate)
	{
		super(app, id, null, xform);
		
		imageId = imgId;
	}
	

    // ******************************************************************** //
    // Building.
    // ******************************************************************** //

    /**
     * Set the visual rectangular box of this element.  This is called when
     * we're added to our parent.
     * 
     * @param   rect        The rectangle, in level co-ordinates, suitable for
     *                      attaching Graphics to.
     */
    @Override
    void setRect(RectF rect) {
        // Calculate the actual geometry of the graphic.
        Matrix xform = getTransform();
        bounds = xform.transform(rect);
        int w = (int) (Math.round(bounds.right - bounds.left));
        int h = (int) (Math.round(bounds.bottom - bounds.top));
        
        // Get the bitmap.  We don't want to re-use a cached image if the
        // size is new or if the rotation has changed, so key on all three
        // factors as well as the ID.
        Plughole app = getApp();
        Matrix.ORotate rotate = norotate ? Matrix.ORotate.NONE : xform.getRotation();
        long key = (long) imageId | (long) w << 32 |
                                (long) h << 43 | (long) rotate.degrees << 54;
        Bitmap img = imageCache.get(key);
        if (img == null) {
            Log.v(TAG, "Allocate image for " + getId());
            img = app.getScaledBitmap(imageId, w, h, rotate);
            imageCache.put(key, img);
        }
        bitmap = img;
    }
    

	// ******************************************************************** //
	// Accessors.
	// ******************************************************************** //

    /**
     * Get the bounding box of this graphic.
     */
    RectF getBounds() {
        return bounds;
    }
    

	// ******************************************************************** //
	// Drawing.
	// ******************************************************************** //

	/**
	 * Draw this graphic onto the given canvas.
	 * 
	 * @param	canvas			Canvas to draw on.
	 * @param	time			Total level time in ms.  A time of zero
	 * 							indicates that we're drawing statically,
	 * 							not in the game loop.
	 * @param	clock			Level time remaining in ms.
	 */
	@Override
	protected void draw(Canvas canvas, long time, long clock) {
		canvas.drawBitmap(bitmap, bounds.left, bounds.top, null);
	}

	
	// ******************************************************************** //
	// Class Data.
	// ******************************************************************** //

    // Debugging tag.
	@SuppressWarnings("unused")
	private static final String TAG = "plughole";

	// This is a cache of the images we've been asked to draw, scaled
	// to size.  The cache is indexed by the integer resource ID of
	// the image,combined with the size of the scaled image.
	private static HashMap<Long, Bitmap> imageCache =
												new HashMap<Long, Bitmap>();
	
	
	// ******************************************************************** //
	// Private Data.
	// ******************************************************************** //
	
	// If true, do *not* rotate the image for display on different screen
	// orientations.  This means that the image top will always be towards
	// screen top.  (In other words, the image effectively rotates with
	// the screen -- think about it).  If false, the image is rotated
	// so it always lines up the same way with the game board.
	private boolean norotate = true;

	// Actual bounding box of this graphic in the scaled playing board.
	private RectF bounds;

	// The resource ID of the image to draw.
	private int imageId;

	// Bitmap image.  If null, not set yet.
	private Bitmap bitmap = null;

}

