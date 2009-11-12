
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

import java.util.Iterator;

import org.hermit.geometry.Edge;
import org.hermit.geometry.Graph;
import org.hermit.geometry.Point;
import org.hermit.geometry.Vector;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;


/**
 * A widget which displays cluster test data and results in graphical form.
 */
public class ClusterWidget
	extends View
{

	// ******************************************************************** //
    // Constructors.
    // ******************************************************************** //
	
	/**
	 * Create a cluster display widget.
	 * 
	 * @param	context			Parent application.
	 */
	public ClusterWidget(Context context) {
		super(context);
		init(context);
	}


	/**
	 * Create a cluster display widget.
	 * 
	 * @param	context			Parent application.
	 * @param	attrs			Layout attributes.
	 */
	public ClusterWidget(Context context, AttributeSet attrs) {
		super(context, attrs);
		init(context);
	}

	
	private void init(Context context) {
		clusterPaint = new Paint();
        clusterPaint.setAntiAlias(false);
	}


    // ******************************************************************** //
	// Geometry.
	// ******************************************************************** //
	 
    /**
     * This is called during layout when the size of this view has
     * changed.  This is where we first discover our window size, so set
     * our geometry to match.
     * 
     * @param	width			Current width of this view.
     * @param	height			Current height of this view.
     * @param	oldw			Old width of this view.  0 if we were
     * 							just added.
     * @param	oldh			Old height of this view.   0 if we were
     * 							just added.
     */
	@Override
	protected void onSizeChanged(int width, int height, int oldw, int oldh) {
    	super.onSizeChanged(width, height, oldw, oldh);

        Log.i(TAG, "Got size " + width + "x" + height);
    	if (width <= 0 || height <= 0)
    		return;
    	windowSize = Math.max(width, height);
    	haveSize = true;
    	
    	// Need to re-draw.
    	invalidate();
    }

	
	/**
	 * Determine whether this widget has a size.
	 * 
	 * @return                 true if we know our size.
	 */
	boolean hasSize() {
	    return haveSize;
	}
	
	
    // ******************************************************************** //
    // State Control.
    // ******************************************************************** //
	
    /**
     * Draws the current state of a clustering test into the
     * SurfaceView provided to this class.
     * 
     * @param  test         The test to display.
     */
    void set(ClusterTest test) {
        clusterTest = test;
        
        postInvalidate();
    }
    
    
	// ******************************************************************** //
	// Drawing.
	// ******************************************************************** //

	/**
	 * This method is called to ask the widget to draw itself.
	 * 
	 * @param	canvas		Canvas to draw into.
	 */
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // Black the screen.
        canvas.drawARGB(255, 0, 0, 0);

        // If no data yet, that's it.
        if (clusterTest == null)
            return;

        synchronized (clusterTest) {
            Point[] dataPoints = clusterTest.getDataPoints();
            if (dataPoints == null)
                return;

            // Draw in the reference points first, if there are any.
            clusterPaint.setStyle(Style.STROKE);
            Point[] dataRefs = clusterTest.getRefPoints();
            if (dataRefs != null) {
                clusterPaint.setColor(0xffffffff);
                for (int i = 0; i < dataRefs.length; ++i) {
                    Point ref = dataRefs[i];
                    final float x = ref.getXf();
                    final float y = ref.getYf();
                    canvas.drawCircle(x, y, 5, clusterPaint);
                }
            }

            // Colour all the data points according to their cluster assignments,
            // if any; else make them white.
            int[] clusterIds = clusterTest.getClusterIds();
            clusterPaint.setColor(0xffffffff);
            for (int i = 0; i < dataPoints.length; ++i) {
                Point point = dataPoints[i];
                if (clusterIds != null)
                    setColor(clusterIds[i]);
                final float x = point.getXf();
                final float y = point.getYf();
                canvas.drawRect(x - 1f, y - 1f, x + 1f, y + 1f, clusterPaint);
            }

            // Draw the cluster centroids, if any, as crosshairs.
            double[][] clusterMeans = clusterTest.getClusterMeans();
            if (clusterMeans != null) {
                for (int i = 0; i < clusterMeans.length; ++i) {
                    setColor(i);
                    double[] mean = clusterMeans[i];
                    float x = (float) mean[0];
                    float y = (float) mean[1];
                    canvas.drawLine(x - 6, y, x + 6, y, clusterPaint);
                    canvas.drawLine(x, y - 6, x, y + 6, clusterPaint);
                }
            }

            // If we have a Voronoi diagram, draw it in.
            Graph clusterGraph = clusterTest.getClusterGraph();
            if (clusterGraph != null) {
                clusterPaint.setColor(0xff00ffff);
                Iterator<Edge> edges = clusterGraph.getEdges();
                while (edges.hasNext())
                    drawEdge(edges.next(), canvas);
            }

            // Finally, draw in performance data.
            boolean converged = clusterTest.isConverged();
            int iterCount = clusterTest.getIterations();
            long clusterDuration = clusterTest.getClusterTime();
            long voronoiDuration = clusterTest.getVoronoiTime();
            long metric = (long) clusterTest.getClusterMetric();
            String ctext = iterCount + (converged ? " converged" : "");
            canvas.drawText("Iter:" + ctext, 4, 15, clusterPaint);
            canvas.drawText("Clus:" + clusterDuration, 4, 30, clusterPaint);
            canvas.drawText("Voro:" + voronoiDuration, 4, 45, clusterPaint);
            canvas.drawText(String.format("Qual:%8d", metric), 4, 60, clusterPaint);
        }
    }


	/**
	 * Set the drawing colour to the colour for the given cluster.
	 * 
	 * @param  cluster         The cluster we're drawing.
	 */
	private final void setColor(int cluster) {
	    clusterPaint.setColor(CLUSTER_COLOURS[cluster % CLUSTER_COLOURS.length]);
	}
	
	
	
	/**
	 * Draw an edge of the Voronoi diagram.  Handle the fact that the edge
	 * may be infinite or partly infinite.
	 * 
	 * @param  e               The edge to draw.
	 * @param  canvas          The Canvas to draw it into.
	 */
    private void drawEdge(Edge e, Canvas canvas) {
        Point p1, p2;
        if (e.isInfinite()) {
            // For an infinite edge, the endpoints we draw are calculated
            // from a reference point and a direction.  We draw to
            // "infinity" from the reference point in either direction;
            // in practice, we draw to some distance farther than the
            // screen width.
            Point fp = e.referencePoint();
            Vector dir = e.directionVector();
            final int dist = windowSize * 3;
            double x1 = fp.getX() - dir.getX() * dist;
            double y1 = fp.getY() - dir.getY() * dist;
            p1 = new Point(x1, y1);
            double x2 = fp.getX() + dir.getX() * dist;
            double y2 = fp.getY() + dir.getY() * dist;
            p2 = new Point(x2, y2);
        } else if (e.isPartlyInfinite()) {
            // If the edge is infinite in only one direction, then
            // calculate that endpoint for the rendered line as above.
            // The other endpoint is, of course, the non-infinite point
            // of the edge.
            p1 = e.referencePoint();
            Vector dir = e.directionVector();
            final int dist = windowSize * 3;
            double x2 = p1.getX() + dir.getX() * dist;
            double y2 = p1.getY() + dir.getY() * dist;
            p2 = new Point(x2, y2);
        } else {
            // If the edge is anchored at both ends, this is easy.
            p1 = e.getVertexA();
            p2 = e.getVertexB();
        }
            
        canvas.drawLine(p1.getXf(), p1.getYf(), p2.getXf(), p2.getYf(), clusterPaint);
    }
    

    // ******************************************************************** //
    // Private Constants.
    // ******************************************************************** //

    // Debugging tag.
    private static final String TAG = "Cluster";
    
    // The colours to use for the clusters.
    private static final int[] CLUSTER_COLOURS = new int[] { Color.RED,
            Color.argb(255, 255, 128, 0), Color.YELLOW, Color.GREEN, Color.BLUE,
            Color.argb(255, 255, 0, 255) };

    
	// ******************************************************************** //
	// Private Data.
	// ******************************************************************** //
    
    // True if we have a screen size.
    private boolean haveSize = false;

	// The larger of our window dimensions, in pixels.
    private int windowSize = 0;
    
	// Paint used for graphics.
	private Paint clusterPaint;

    // The test state currently being displayed.
    private ClusterTest clusterTest = null;

}

