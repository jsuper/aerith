/*
 * GeoPositionEvaluator.java
 *
 * Created on 2 maart 2007, 14:37
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package org.jdesktop.swingx.mapviewer.animation;

import org.jdesktop.animation.timing.interpolation.Evaluator;
import org.jdesktop.swingx.mapviewer.GeoPosition;

/**
 * Evaluator for GeoPosition objects.
 * @author bpasson
 */
public class GeoPositionEvaluator extends Evaluator<GeoPosition> {
    
    /** Creates a new instance of GeoPositionEvaluator */
    public GeoPositionEvaluator() {
    }

    public GeoPosition evaluate(GeoPosition v0, GeoPosition v1, float fraction) {
            double latitude = ((v1.getLatitude() - v0.getLatitude()) * fraction) + v0.getLatitude();
            double longitude = ((v1.getLongitude() - v0.getLongitude()) * fraction) + v0.getLongitude();
            return new GeoPosition(latitude, longitude);
    }    
}
