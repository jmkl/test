
/**
 * bentools: Voronoi diagram generator.  This is Benjamin Dittes'
 * C# implementation of Fortune's algorithm, translated to Java
 * by Ian Cameron Smith.
 * 
 * <p>The only license info I can see: "If you ever need a voronoi
 * clustering in C#, feel free to use my solution here."  See
 * http://bdittes.googlepages.com/
 *
 * <p>This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 */


package org.hermit.geometry.voronoi;

import org.hermit.geometry.MathTools;
import org.hermit.geometry.Point;


/**
 * @author clint
 * 
 */
class DataNode extends Node {

    public DataNode(Point DP) {
        DataPoint = DP;
    }


    public DataNode(Event P) {
        DataPoint = new Point(P.getX(), P.getY());
    }


    public Point DataPoint;
    

    CircleEvent CircleCheckDataNode(double ys) {
        final DataNode l = Node.LeftDataNode(this);
        final DataNode r = Node.RightDataNode(this);
        if (l == null || r == null || l.DataPoint == r.DataPoint
                || l.DataPoint == DataPoint || DataPoint == r.DataPoint)
            return null;
        if (MathTools.ccw(l.DataPoint, DataPoint, r.DataPoint) <= 0)
            return null;

        final Point Center = CircumCircleCenter(l.DataPoint,
                DataPoint, r.DataPoint);
        final CircleEvent VC = new CircleEvent(this, l, r, Center);
        if (VC.getY() >= ys)
            return VC;
        
        return null;
    }


    // ******************************************************************** //
    // Utilities.
    // ******************************************************************** //

    /**
     * Convert this instance to a String suitable for display.
     * 
     * @return             String representation of this instance.
     */
    @Override
    public String toString() {
        return super.toString() + DataPoint;
    }

}

