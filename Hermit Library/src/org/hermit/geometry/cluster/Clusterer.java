
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

import org.hermit.geometry.Point;
import org.hermit.geometry.Region;


/**
 * A generic interface to a clustering algorithm.
 */
public interface Clusterer {
    
	/**
	 * Prepare a clustering pass on the indicated data.
	 * 
	 * @param  points      The array of points to be clustered.
     * @param  ids         Array of cluster numbers which this call will
     *                     fill in, defining which cluster each point
     *                     belongs to.  The caller must leave the data here
     *                     intact between iterations.
	 * @param  means       Array of x,y values in which to place centroids
	 *                     of the clusters.
	 * @param  region      The region of the plane in which the points lie.
	 */
    public void prepare(Point[] points, int[] ids, double[][] means, Region region);

	/**
	 * Runs a single iteration of the clustering algorithm on the stored data.
     * 
     * <p>After each iteration, the cluster IDs and cluster means should
     * be consistent with each other.
	 * 
	 * @param  ids         Array of cluster numbers which this call will
	 *                     fill in, defining which cluster each point
	 *                     belongs to.
	 * @param  means       Array of x,y values in which we will place the
     *                     centroids of the clusters.
	 * @return             true if the algorithm has converged.
	 */
    public boolean iterate(int[] ids, double[][] means);
	
}

