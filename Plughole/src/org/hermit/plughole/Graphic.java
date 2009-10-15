
/**
 * Plughole: a rolling-ball accelerometer game.
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


package org.hermit.plughole;

import java.util.HashMap;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.RectF;


/**
 * Class representing a graphic or text drawn on the game board.  It doesn't
 * interact with the game in any way, it's just decoration.
 */
class Graphic
	extends Element
{

	// ******************************************************************** //
	// Public Constants.
	// ******************************************************************** //

	/**
	 * Bitmap for a blank hole.
	 * TODO: Not an animation.
	 */
	public static final int[] BLANK_HOLE = {
		R.drawable.blank_hole,
	};
	

	/**
	 * Bitmaps making up the lava hole animation.
	 */
	public static final int[] LAVA_ANIM = {
		R.drawable.red_hole_01,
		R.drawable.red_hole_02,
		R.drawable.red_hole_03,
		R.drawable.red_hole_04,
		R.drawable.red_hole_05,
		R.drawable.red_hole_06,
		R.drawable.red_hole_07,
		R.drawable.red_hole_08,
		R.drawable.red_hole_09,
		R.drawable.red_hole_10,
		R.drawable.red_hole_11,
		R.drawable.red_hole_12,
	};
	

	/**
	 * Bitmaps making up the teleport hole animation.
	 */
	public static final int[] PORT_ANIM = {
		R.drawable.blue_hole_01,
		R.drawable.blue_hole_02,
		R.drawable.blue_hole_03,
		R.drawable.blue_hole_04,
		R.drawable.blue_hole_05,
		R.drawable.blue_hole_06,
		R.drawable.blue_hole_07,
		R.drawable.blue_hole_08,
	};
	

	/**
	 * Bitmaps making up the lava hole animation.
	 */
	public static final int[] EXIT_ANIM = {
		R.drawable.green_hole_01,
		R.drawable.green_hole_02,
		R.drawable.green_hole_03,
		R.drawable.green_hole_04,
		R.drawable.green_hole_05,
	};
	

	/**
	 * Bitmaps making up the lava hole animation.
	 */
	public static final int[] GREEN_ARROW = {
		R.drawable.green_arrow_01,
		R.drawable.green_arrow_02,
		R.drawable.green_arrow_03,
		R.drawable.green_arrow_04,
		R.drawable.green_arrow_05,
		R.drawable.green_arrow_06,
		R.drawable.green_arrow_07,
		R.drawable.green_arrow_08,
		R.drawable.green_arrow_09,
		R.drawable.green_arrow_10,
	};
	
	
	// ******************************************************************** //
	// Constructor.
	// ******************************************************************** //

	/**
	 * Create a graphic which displays an image.
	 * 
	 * @param	app				Application context.  This provides
	 * 							access to resources and image loading.
	 * @param	imgId			Resource ID of the graphic.
	 * @param	box				The bounding box for the graphic.
	 * @param	xform			Transform to apply to the raw data.
	 */
	public Graphic(Plughole app, int imgId, RectF box, Matrix xform)
	{
		this(app, new int[] { imgId }, box, xform, false);
	}

	
	/**
	 * Create a graphic which displays an image.
	 * 
	 * @param	app				Application context.  This provides
	 * 							access to resources and image loading.
	 * @param	imgId			Resource ID of the graphic.
	 * @param	box				The bounding box for the graphic.
	 * @param	xform			Transform to apply to the raw data.
	 * @param	norotate		If true, do *not* rotate the image for display
	 * 							on different screen orientations.  This means
	 * 							that the image top will always be towards
	 * 							screen top.  (In other words, the image
	 * 							effectively rotates with the screen -- think
	 * 							about it).  If false, the image is rotated
	 * 							so it always lines up the same way with the
	 * 							game board.
	 */
	public Graphic(Plughole app, int imgId, RectF box,
				   Matrix xform, boolean norotate)
	{
		this(app, new int[] { imgId }, box, xform, norotate);
	}
	

	/**
	 * Create a graphic which displays an animation.
	 * 
	 * @param	app				Application context.  This provides
	 * 							access to resources and image loading.
	 * @param	imgIds			Resource IDs of the graphics in the animation.
	 * @param	box				The bounding box for the graphic.
	 * @param	xform			Transform to apply to the raw data.
	 */
	public Graphic(Plughole app, int[] imgIds, RectF box, Matrix xform)
	{
		this(app, imgIds, box, xform, false);
	}


	/**
	 * Create a graphic which displays an animation.
	 * 
	 * @param	app				Application context.  This provides
	 * 							access to resources and image loading.
	 * @param	imgIds			Resource IDs of the graphics in the animation.
	 * @param	box				The bounding box for the graphic.
	 * @param	xform			Transform to apply to the raw data.
	 * @param	norotate		If true, do *not* rotate the image for display
	 * 							on different screen orientations.  This means
	 * 							that the image top will always be towards
	 * 							screen top.  (In other words, the image
	 * 							effectively rotates with the screen -- think
	 * 							about it).  If false, the image is rotated
	 * 							so it always lines up the same way with the
	 * 							game board.
	 */
	public Graphic(Plughole app, int[] imgIds, RectF box,
				   Matrix xform, boolean norotate)
	{
		super(app);
		
		imageIds = imgIds;
		
		// Calculate the actual geometry of the graphic.
		bounds = xform.transform(box);
		int w = (int) (Math.round(bounds.right - bounds.left));
		int h = (int) (Math.round(bounds.bottom - bounds.top));
		
		// Get the bitmaps.  We don't want to re-use a cached image if the
		// size is new or if the rotation has changed, so key on all three
		// factors as well as the ID.
		bitmaps = new Bitmap[imageIds.length];
		Matrix.ORotate rotate = norotate ? Matrix.ORotate.NONE : xform.getRotation();
		for (int i = 0; i < imageIds.length; ++i) {
			long key = (long) imageIds[i] | (long) w << 32 |
					   (long) h << 43 | (long) rotate.degrees << 54;
			Bitmap img = imageCache.get(key);
			if (img == null) {
				img = app.getScaledBitmap(imageIds[i], w, h, rotate);
				imageCache.put(key, img);
			}
			bitmaps[i] = img;
		}
		
		animOffset = rndInt(imageIds.length);
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
		final long off = time - clock;
		final int frame = (int) ((off / 100 + animOffset) % bitmaps.length);
		canvas.drawBitmap(bitmaps[frame], bounds.left, bounds.top, null);
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

	// Actual bounding box of this graphic in the scaled playing board.
	private RectF bounds;

	// The resource IDs of the images to draw.
	private int[] imageIds;

	// Bitmap images.  If null, not set yet.
	private Bitmap[] bitmaps = null;
	
	// Animation initial offset.
	private int animOffset;

}

