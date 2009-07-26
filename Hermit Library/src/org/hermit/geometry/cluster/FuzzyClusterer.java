
/**
 * cluster: routines for cluster analysis.
 *
 * <p>This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as
 * published by the Free Software Foundation (see COPYING).
 *
 * <p>This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * General Public License for more details.
 *
 * <p>You should have received a copy of the GNU General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA
 * 02111-1307, USA.
 */


package org.hermit.geometry.cluster;


import java.util.Random;

import org.hermit.geometry.MathTools;
import org.hermit.geometry.Point;
import org.hermit.geometry.Region;


/**
 * An implementation of Lloyd's k-clusterMeans clustering algorithm.
 */
public class FuzzyClusterer implements Clusterer {
	
    /**
     * Prepare a clustering pass on the indicated data.
     * 
     * @param  points      The array of dataPoints to be clustered.
     * @param  ids         Array of cluster numbers which this call will
     *                     fill in, defining which cluster each point
     *                     belongs to.  The caller must leave the data here
     *                     intact between iterations.
     * @param  means       Array of x,y values in which to place centroids
     *                     of the clusters.
     * @param  region      The region of the plane in which the points lie.
     */
    public void prepare(Point[] points, int[] ids, double[][] means, Region region) {
	    // Save the data arrays.
	    dataPoints = points;
		numPoints = points.length;
		numClusters = means.length;

		// Set up the strengths array.
		clusterStrengths = new double[numPoints][numClusters];

		// Set the initial cluster centroids to be random values
		// within the data region.
        double x = region.getX1();
        double y = region.getY1();
        double w = region.getWidth();
        double h = region.getHeight();
		for (int i = 0; i < numClusters; ++i) {
			means[i][0] = random.nextDouble() * w + x;
			means[i][1] = random.nextDouble() * h + y;
		}
        
        // Make an initial assignment of points to clusters, so on the first
		// iteration we have a basis for computing centroids.
        assignPoints(ids, means);
	}


    /**
     * Runs a single iteration of the clustering algorithm on the stored data.
     * 
     * <p>After each iteration, the cluster IDs and cluster means should
     * be consistent with each other.
     * 
     * @param  ids         Array of cluster numbers which this call will
     *                     fill in, defining which cluster each point
     *                     belongs to.  The caller must leave the data here
     *                     intact between iterations.
     * @param  means       Array of x,y values in which we will place the
     *                     centroids of the clusters.
     * @return             true if the algorithm has converged.
     */
	public boolean iterate(int[] ids, double[][] means) {
        System.out.println("Fuzzy: iterate");
        
        // Compute the new centroids of the clusters, based on the existing
	    // point assignments.
        boolean converged = computeCentroids(ids, means);
        
	    // Assign data points to clusters based on the new centroids.
        if (!converged)
            assignPoints(ids, means);
        
        // Our convergence criterion is no change in the means.
        return converged;
	}

	
	/**
	 * Compute the centroids of all the clusters.
     * 
     * @param  ids          Array of cluster numbers which this call will
     *                      fill in, defining which cluster each point
     *                      belongs to.  The caller must leave the data here
     *                      intact between iterations.
     * @param  means        Array of x,y values in which we will place the
     *                      centroids of the clusters.
     * @return              true iff none of the means moved by a significant
     *                      amount.
	 */
	private boolean computeCentroids(int[] ids, double[][] means) {
        boolean dirty = false;
        for (int c = 0; c < numClusters; ++c) {
            // Compute the weighted sum of the data points.
            double tx = 0.0, ty = 0.0, tot = 0.0;
            for (int p = 0; p < numPoints; ++p) {
                Point point = dataPoints[p];
                double str = Math.pow(clusterStrengths[p][c], M);
                tx += point.getX() * str;
                ty += point.getY() * str;
                tot += str;
            }
            
            // Calculate the mean, and see if it's different from
            // the previous one.
            final double nx = tx / tot;
            final double ny = ty / tot;
            if (!MathTools.eq(means[c][0], nx) || !MathTools.eq(means[c][1], ny)) {
                means[c][0] = nx;
                means[c][1] = ny;
                dirty = true;
            }
        }
        
        return !dirty;
	}
	
	
	/**
	 * Assign each point in the data array to the cluster whose centroid
	 * it is closest to.
     * 
     * @param  ids          Array of cluster numbers which this call will
     *                      fill in, defining which cluster each point
     *                      belongs to.  The caller must leave the data here
     *                      intact between iterations.
     * @param  means        Array of x,y values in which we will place the
     *                      centroids of the clusters.
     * @return              true iff none of the points changed to a different
     *                      cluster.
	 */
	private boolean assignPoints(int[] ids, double[][] means) {
        // Assign each point to a cluster, according to which cluster
        // centroid it is closest to.  Set dirty to true if any point
        // changes to a different cluster.
        boolean dirty = false;
        for (int p = 0; p < numPoints; ++p) {
            Point point = dataPoints[p];
            int closest = -1;
            double maxStrength = 0;
            
            for (int c = 0; c < means.length; ++c) {
                double dist = computeDistance(point, means[c]);
                double sum = 0.0;
                for (int j = 0; j < means.length; ++j) {
                    double dj = computeDistance(point, means[j]);
                    sum += Math.pow(dist / dj, 2 / (M - 1));
                }
                clusterStrengths[p][c] = 1 / sum;
                if (1 / sum > maxStrength) {
                    maxStrength = 1 / sum;
                    closest = c;
                }
            }
            
            if (closest != ids[p]) {
                ids[p] = closest;
                dirty = true;
            }
        }

        return !dirty;
	}
	
	   
	/**
	 * Computes the Cartesian distance between two points.
	 */
	private static final double computeDistance(Point a, double[] b) {
	    final double dx = a.getX() - b[0];
	    final double dy = a.getY() - b[1];
		return Math.sqrt(dx * dx + dy * dy);
	}

	
    // ******************************************************************** //
    // Class Data.
    // ******************************************************************** //
	
	// RNG used to set initial mean values.
    private static final Random random = new Random();
    
    // M (power factor) used for calculating weights.  Must be > 1.0.
    private static final double M = 2.0;

    
    // ******************************************************************** //
    // Private Data.
    // ******************************************************************** //
   
    // The number of points in the data set on a given pass.
    private int numPoints;
    
    // The desired number of clusters.
    private int numClusters;
    
    // During a pass -- multiple iterations -- this points to the
    // array of data points.
    private Point[] dataPoints;
    
    // For each point p, clusterStrengths[p][c] is its degree of
    // belonging to each cluster c.
    private double[][] clusterStrengths;

}

