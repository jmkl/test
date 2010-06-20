
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

import android.graphics.RectF;


/**
 * A 2-D transformation matrix.  This is a mutable class: transformations
 * are combined by changing this matrix in place.  Translation, orthogonal
 * rotation, and uniform (in X/Y) scaling are the supported operations.
 * 
 * In addition to the basic matrix, we keep separate track of the accumulated
 * rotation and scale.  This is required for scaling images and rotating text.
 */
final class Matrix {

	// ******************************************************************** //
	// Public Types.
	// ******************************************************************** //

	/**
	 * This enum specifies an orthogonal rotation; i.e. by a multiple
	 * of 90°.
	 */
	public static enum ORotate {
		LEFT(-90, 0, -1),
		NONE(0, 1, 0),
		RIGHT(90, 0, 1),
		FULL(180, -1, 0);
		
		ORotate(int degrees, int cos, int sin) {
			this.degrees = degrees;
			this.cos = cos;
			this.sin = sin;
		}
		
		// Return the result of adding another rotation to this one.
		public ORotate add(ORotate other) {
			switch ((degrees + other.degrees + 360) % 360) {
			case 0:
			default:
				return NONE;
			case 90:
				return RIGHT;
			case 180:
				return FULL;
			case 270:
				return LEFT;
			}
		}
		
		// The rotation in degrees, for convenience.
		public final int degrees;
		
		// The cosine and sine of the rotation angle.
		public final int cos;
		public final int sin;
	}
	
	
	// ******************************************************************** //
	// Constructor.
	// ******************************************************************** //
	
	/**
	 * Create an identity matrix.
	 */
	public Matrix() {
		matrix = new double[][] {
			{ 1, 0, 0 },
			{ 0, 1, 0 },
			{ 0, 0, 1 },
		};
		scale = 1;
		rotation = ORotate.NONE;
		
		ttemp = new double[3][3];
	}

	
	// ******************************************************************** //
	// Transformation Creation.
	// ******************************************************************** //
	
	/**
	 * Add a translation to this matrix.
	 * 
	 * @param	x			X offset to displace by.
	 * @param	y			Y offset to displace by.
	 */
	public final void translate(double x, double y) {
		ttemp[0][0] = 1;
		ttemp[0][1] = 0;
		ttemp[0][2] = x;
		ttemp[1][0] = 0;
		ttemp[1][1] = 1;
		ttemp[1][2] = y;
		ttemp[2][0] = 0;
		ttemp[2][1] = 0;
		ttemp[2][2] = 1;
		matrix = multiply(matrix, ttemp);
	}

	
	/**
	 * Add a scaling to this matrix.
	 * 
	 * @param	s			Factor to scale by.
	 */
	public final void scale(double s) {
		ttemp[0][0] = s;
		ttemp[0][1] = 0;
		ttemp[0][2] = 0;
		ttemp[1][0] = 0;
		ttemp[1][1] = s;
		ttemp[1][2] = 0;
		ttemp[2][0] = 0;
		ttemp[2][1] = 0;
		ttemp[2][2] = 1;
		matrix = multiply(matrix, ttemp);
		scale = scale * s;
	}

	
	/**
	 * Add an orthogonal rotation to this matrix.
	 * 
	 * @param	r			Factor to rotate by.
	 */
	public final void rotate(ORotate r) {
		ttemp[0][0] = r.cos;
		ttemp[0][1] = -r.sin;
		ttemp[0][2] = 0;
		ttemp[1][0] = r.sin;
		ttemp[1][1] = r.cos;
		ttemp[1][2] = 0;
		ttemp[2][0] = 0;
		ttemp[2][1] = 0;
		ttemp[2][2] = 1;
		matrix = multiply(matrix, ttemp);
		rotation = rotation.add(r);
	}


	// ******************************************************************** //
	// State Access.
	// ******************************************************************** //

	/**
	 * Get the total accumulated scale.
	 * 
	 * @return             The total scaling factor.
	 */
	public double getScale() {
		return scale;
	}
	

	/**
	 * Get the total accumulated rotation.
     * 
     * @return             The total rotation.
	 */
	public ORotate getRotation() {
		return rotation;
	}
	

	// ******************************************************************** //
	// Object Transformation.
	// ******************************************************************** //

	/**
	 * Transform the given Point by this matrix.
	 * 
	 * @param	point		The point.
	 * @return				A new point which is the input point transformed
	 * 						by this matrix.
	 */
	public Point transform(Point point) {
		double[][] p = {
			{ point.x },
			{ point.y },
			{ 1 },
		};
		
		p = multiply(matrix, p);
		return new Point(p[0][0], p[1][0]);
	}
	

	/**
	 * Transform the given Point by this matrix.  We provide this for
	 * convenience to save making a temp Point.
	 * 
	 * @param	x			X co-ordinate of the point.
	 * @param	y			Y co-ordinate of the point.
	 * @return				A new point which is the input point transformed
	 * 						by this matrix.
	 */
	public Point transform(double x, double y) {
		double[][] p = {
			{ x },
			{ y },
			{ 1 },
		};
		
		p = multiply(matrix, p);
		return new Point(p[0][0], p[1][0]);
	}
	

	/**
	 * Transform the given RectF by this matrix.
	 * 
	 * @param	rect		The rectangle to transform.
	 * @return				A new point which is the input point transformed
	 * 						by this matrix.
	 */
	public RectF transform(RectF rect) {
		// Since we only support orthogonal rotation, we can simply transform
		// the two corners, and the result defines the new rect.
		double[][] p1 = {
			{ rect.left },
			{ rect.top },
			{ 1 },
		};
		double[][] p2 = {
			{ rect.right },
			{ rect.bottom },
			{ 1 },
		};

		p1 = multiply(matrix, p1);
		p2 = multiply(matrix, p2);
		final double left = Math.min(p1[0][0], p2[0][0]);
		final double top = Math.min(p1[1][0], p2[1][0]);
		final double right = Math.max(p1[0][0], p2[0][0]);
		final double bottom = Math.max(p1[1][0], p2[1][0]);
		return new RectF((float) left, (float) top,
						 (float) right, (float) bottom);
	}
	

	// ******************************************************************** //
	// Matrix Arithmetic.
	// ******************************************************************** //
	
	/**
	 * General matrix multiplication.  Multiply the two given matrices and
	 * return the product.  The dimensions of the input matrices must
	 * be [m][n] and [n][p].  The contents of m1 and m2 are not altered.
	 * 
	 * @param	m1			First matrix, of dimension [m][n].
	 * @param	m2			Second matrix, of dimension [n][p].
	 * @return				The matrix product, of dimension [m][p].
	 * @exception	ArrayIndexOutOfBoundsException	The input matrices
	 * 						do not meet the requirements.
	 */
	private static final double[][] multiply(double[][] m1, double[][] m2) {
		if (m1.length < 1 || m2.length < 1 || m1[0].length != m2.length)
			throw new ArrayIndexOutOfBoundsException(
										"matrix dimensions do not match");
		
		int m = m1.length;
		int n = m1[0].length;
		int p = m2[0].length;
		double[][] result = new double[m][p];
		
		for (int i = 0; i < m; ++i) {
			for (int j = 0; j < p; ++j) {
				double sum = 0;
				for (int k = 0; k < n; ++k)
					sum += m1[i][k] * m2[k][j];
				result[i][j] = sum;
			}
		}
		
		return result;
	}
	

	// ******************************************************************** //
	// Utilities.
	// ******************************************************************** //

	@Override
	public String toString() {
		return new String("" + 1);
	}
	
	
	// ******************************************************************** //
	// Public Data.
	// ******************************************************************** //
	
	// The current value of this matrix.  The indices are [row][column].
	private double[][] matrix;
	
	// Temp value used to add a transformation.
	private double[][] ttemp;
	
	// Total accumulated scale.
	private double scale = 1;
	
	// Total accumulated rotation.
	private ORotate rotation = ORotate.NONE;

}

