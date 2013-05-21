package com.sun.animation.effects;


/**
 * ComponentEffect that resizes a component during the transition.
 * This effect uses an image representation of the component instead of
 * re-rendering the component on every frame.
 * 
 * @author Chet Haase
 */
public class Scale extends ComponentEffect {
    
    /** Creates a new instance of Scale */
    public Scale(ComponentState start, ComponentState end) {
	setComponentStates(start, end);
        // scaling effect, by default, will re-render Component every time
        setRenderComponent(true);
    }
    
//    @Override
//    public void setup(Graphics2D g2d, float fraction) {
//	AffineTransform xform = g2d.getTransform();
//        int startW = start.getWidth();
//        int startH = start.getHeight();
//        int endW = end.getWidth();
//        int endH = end.getHeight();
//	if (startW != endW || startH != endH) {
//	    float wFraction = (float)endW / startW;
//	    float hFraction = (float)endH / startH;
//	    float widthFactor;
//	    float heightFactor;
//	    widthFactor = startW + fraction * (endW - startW);
//	    widthFactor = widthFactor / startW;
//	    heightFactor = startH + fraction * (endH - startH);
//	    heightFactor = heightFactor / startH;
//	    if (Math.abs(wFraction - 1.0f) > Math.abs(hFraction - 1.0f)) {
//		// difference greater in width
//		if (wFraction >= 1.0f) {
//		    widthFactor *= ((float)startW / endW);
//		    heightFactor *= ((float)startH / endH);
//		}
//	    } else {
//		// different greater in height
//		if (hFraction >= 1.0f) {
//		    widthFactor *= ((float)startW / endW);
//		    heightFactor *= ((float)startH / endH);
//		}
//	    }
//	    // setup scaling operation around parent origin
//	    g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
//		    RenderingHints.VALUE_INTERPOLATION_BILINEAR);
//	    g2d.scale(widthFactor, heightFactor);
//	}
//        super.setup(g2d, fraction);
//    }
    
    
}
