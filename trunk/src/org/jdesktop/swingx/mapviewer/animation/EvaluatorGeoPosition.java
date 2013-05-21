package org.jdesktop.swingx.mapviewer.animation;

import org.jdesktop.animation.timing.interpolation.Evaluator;
import org.jdesktop.swingx.mapviewer.GeoPosition;

public class EvaluatorGeoPosition extends Evaluator {

	@Override
	public Object evaluate(Object arg0, Object arg1, float fraction) {
		GeoPosition v0 = (GeoPosition) arg0;
		GeoPosition v1 = (GeoPosition) arg1;
		double latitude = ((v1.getLatitude() - v0.getLatitude()) * fraction) + v0.getLatitude();
        double longitude = ((v1.getLongitude() - v0.getLongitude()) * fraction) + v0.getLongitude();
        return new GeoPosition(latitude, longitude);
	}

}
