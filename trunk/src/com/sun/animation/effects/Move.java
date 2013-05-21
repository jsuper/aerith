package com.sun.animation.effects;

import java.awt.Graphics2D;

/**
 * ComponentEffect that moves a component from its position in the start
 * state to its position in the end state, based on linear interpolation
 * between the two points during the time of the animated transition.
 *
 * The class extends ComponentImageEffect to use a simple image copy
 * for rendering the component rather than re-rendering the actual
 * component during each frame.
 *
 * @author Chet Haase
 */
public class Move extends ComponentEffect {
    
    /**
     * REMIND: docs
     */
    public Move() {
    }
    
    /**
     * REMIND: docs
     */
    public Move(ComponentState start, ComponentState end) {
	setComponentStates(start, end);
    }
    
    @Override
    public void setup(Graphics2D g2d, float fraction) {
	//Image componentImage = null;
	int x, y;
        
        // Calculate the location and modify g2d appropriately
	x = (int)(start.getX() + (fraction * (end.getX() - start.getX())));
	y = (int)(start.getY() + (fraction * (end.getY() - start.getY())));
	g2d.translate(x, y);
        super.setup(g2d, fraction);
        
        /*
        // This next block is only concerned with grabbing the targetImage
        // that best represents the component; the larger the better.
	if (start.getWidth() != end.getWidth() || start.getHeight() != end.getHeight()) {
	    float widthFraction = (float)end.getWidth() / start.getWidth();
	    float heightFraction = (float)end.getHeight() / start.getHeight();
	    if (Math.abs(widthFraction - 1.0f) > Math.abs(heightFraction - 1.0f)) {
		// difference greater in width
		if (widthFraction < 1.0f) {
		    // start size larger then end size
		    componentImage = start.getSnapshot();
		} else {
		    componentImage = end.getSnapshot();
		}
	    } else {
		// different greater in height
		if (heightFraction < 1.0f) {
		    // start size larger than end size
		    componentImage = start.getSnapshot();
		} else {
		    componentImage = end.getSnapshot();
		}
	    }
	} else {
	    componentImage = start.getSnapshot();
	}
	targetImage = componentImage;
         **/
    }
}
