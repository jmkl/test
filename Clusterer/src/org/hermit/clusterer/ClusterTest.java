
/**
 * clusterer: a testbed application for cluster analysis.
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


package org.hermit.clusterer;


import java.util.ArrayList;

import org.hermit.geometry.Graph;
import org.hermit.geometry.Point;
import org.hermit.geometry.Region;
import org.hermit.geometry.cluster.Clusterer;
import org.hermit.geometry.cluster.FuzzyClusterer;
import org.hermit.geometry.cluster.KMeansClusterer;
import org.hermit.geometry.generator.Generator;
import org.hermit.geometry.generator.NuclearGenerator;
import org.hermit.geometry.voronoi.Fortune;

import android.content.Intent;
import android.widget.Toast;


/**
 * Clustering test viewer.  This class implments a simple view which
 * displays the results of a clustering test as it runs.
 */
public class ClusterTest
{

    // ******************************************************************** //
    // Constructors.
    // ******************************************************************** //

    /**
     * Create a cluster test with specified parameters.
     * 
     * @param   context         Our application context.
     * @param   region          The region of the plane to cover.
     * @param   intent          An intent containing the parameters for the
     *                          test.
     */
    public ClusterTest(ClusterActivity context, Region region, Intent intent) {
        appContext = context;
        screenRegion = region;
        
        // Set the clustering algorithm based on the intent.
        String which = intent.getStringExtra("algorithm");
        if (which == null) {
            Toast.makeText(appContext, R.string.no_algo_msg, Toast.LENGTH_SHORT);
            clusterer = new KMeansClusterer();
        } else if (which.equals("kmeans"))
            clusterer = new KMeansClusterer();
        else if (which.equals("fuzzy"))
            clusterer = new FuzzyClusterer();
        else {
            Toast.makeText(appContext, R.string.unknown_algo_msg, Toast.LENGTH_SHORT);
            clusterer = new KMeansClusterer();
        }
        
//        generator = new RandomGenerator();
        generator = new NuclearGenerator(NUM_CLUSTERS);
        
        // Set up the data.
        prepare();
    }
    
    
    // ******************************************************************** //
    // Data Setup.
    // ******************************************************************** //

    /**
     * Resets the state: picks a new random set of points.
     */
    private void prepare() {
        dataPoints = generator.createPoints(screenRegion, NUM_POINTS);
        
        reset();
    }
    

    // ******************************************************************** //
    // Cluster Analysis.
    // ******************************************************************** //

    /**
     * Resets the state: picks a new random set of points.
     */
    public void reset() {
        synchronized (this) {
            refPoints = generator.getReferencePoints();

            clusterIds = new int[NUM_POINTS];
            clusterMeans = new double[NUM_CLUSTERS][];
            for (int c = 0; c < NUM_CLUSTERS; ++c)
                clusterMeans[c] = new double[2];

            // Set up the clusterer itself.
            clusterer.prepare(dataPoints, clusterIds, clusterMeans, screenRegion);

            clusterGraph = null;
            iterateCount = 0;
            hasConverged = false;
            clusterDuration = 0;
            voronoiDuration = 0;
        }
    }
    

    /**
     * Run this test for one iteration.  If it hasn't been initialised,
     * initialise it now.
     * 
     * @return              true iff the algorithm has converged.
     */
    public boolean step() {
        synchronized (this) {
            // Run one clustering iteration.
            long cstart = System.currentTimeMillis();
            hasConverged = clusterer.iterate(clusterIds, clusterMeans);
            long cdone = System.currentTimeMillis();
            clusterDuration = cdone - cstart;

            // Generate the Voronoi graph of the result.
            long vstart = cdone;
            clusterGraph = computeVoronoi(clusterMeans);
            long vdone = System.currentTimeMillis();
            voronoiDuration = vdone - vstart;

            ++iterateCount;

            return hasConverged;
        }
    }


    /**
     * Compute the Voronoi graph for the given set of data points.
     */
    private Graph computeVoronoi(double[][] means) {
        ArrayList<Point> meanVecs = new ArrayList<Point>(means.length);
        for (int i = 0; i < means.length; ++i)
            meanVecs.add(new Point(means[i][0], means[i][1]));
        return Fortune.ComputeVoronoiGraph(meanVecs);
    }


    // ******************************************************************** //
    // Accessors.
    // ******************************************************************** //

    /**
     * Get the data points for this test.
     * 
     * @return              The data points for this test.  null if not
     *                      generated yet.
     */
    public Point[] getDataPoints() {
        return dataPoints;
    }
    

    /**
     * Get the reference points used to generate the test data.
     * 
     * @return              The reference points for this test.  null if not
     *                      generated yet, or if the data was generated
     *                      without reference points.
     */
    public Point[] getRefPoints() {
        return refPoints;
    }
    

    /**
     * Determine whether the algorithm has converged.
     * 
     * @return              true if the algorithm has converged.
     */
    public boolean isConverged() {
        return hasConverged;
    }
    

    /**
     * Get the number of iterations we have done on the data.
     * 
     * @return              The iteration count.
     */
    public int getIterations() {
        return iterateCount;
    }
    

    /**
     * Get the computed centroids (means) of the clusters.
     * 
     * @return              The cluster centroids.  null if not
     *                      generated yet.
     */
    public double[][] getClusterMeans() {
        return clusterMeans;
    }
    

    /**
     * Get the IDs of the clusters to which each point has been assigned.
     * 
     * @return              The cluster IDs, one per point.  null if not
     *                      generated yet.
     */
    public int[] getClusterIds() {
        return clusterIds;
    }
    

    /**
     * Get the Voronoi diagram of the computed clusters.
     * 
     * @return              The Voronoi diagram of the computed clusters.
     *                      null if not generated yet.
     */
    public Graph getClusterGraph() {
        return clusterGraph;
    }
    

    /**
     * Get the time taken for the most recent iteration of cluster
     * analysis.
     * 
     * @return              The time in ms the most recent clustering
     *                      iteration took.
     */
    public long getClusterTime() {
        return clusterDuration;
    }
    

    /**
     * Get the time taken to generate the most recent Voronoi diagram.
     * 
     * @return              The time in ms the most Voronoi diagram
     *                      took.
     */
    public long getVoronoiTime() {
        return voronoiDuration;
    }
    
    
    // ******************************************************************** //
    // Private Constants.
    // ******************************************************************** //

    // Debugging tag.
    @SuppressWarnings("unused")
    private static final String TAG = "Cluster";

    private static final int NUM_POINTS = 200;

    private static final int NUM_CLUSTERS = 5;

    
    // ******************************************************************** //
    // Private Data.
    // ******************************************************************** //

    // Our app context.
    private final ClusterActivity appContext;
    
    // The region of the 2D real plane which is mapped to the screen.
    private final Region screenRegion;
    
    // The data set generator we're using.
    private Generator generator = null;
    
    // The clustering algorithm we're using.
    private Clusterer clusterer = null;

    // The data points for this test.
    private Point[] dataPoints = null;
    
    // The reference points which were used in generating the data;
    // used to check the results of the clustering algorithm.
    private Point[] refPoints = null;
    
    // The computed centroids of the clusters.
    private double[][] clusterMeans = null;
    
    // The IDs of the clusters to which each point has been assigned.
    private int[] clusterIds = null;

    // The Voronoi diagram of the computed clusters.
    private Graph clusterGraph = null;
    
    // The number of iterations we have done on the current data set.
    private int iterateCount = 0;
    
    // True if the algorithm has converged.
    private boolean hasConverged = false;
    
    // The time taken for cluster analysis in the most recent iteration,
    // and the time taken to compute the Voronoi diagram.
    private long clusterDuration = 0;
    private long voronoiDuration = 0;

}

