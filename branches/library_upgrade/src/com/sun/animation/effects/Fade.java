package com.sun.animation.effects;

import java.awt.AlphaComposite;
import java.awt.Graphics2D;

/**
 * ComponentEffect that performs a Fade (in or out) on the component.  This
 * is done by using an image of the component and altering the translucency (or
 * <code>AlphaComposite</code>) of the <code>Graphics2D</code> object
 * according to how far along the transition animation is.  
 *
 * This is an abstract class that relies on the FadeIn or FadeOut subclasses
 * to set up the end (FadeIn) or start (FadeOut) states appropriately.
 *
 * @author Chet Haase
 */
public abstract class Fade extends ComponentEffect {
    
    /**
     * This method is called prior to <code>paint()</code> during every 
     * frame of the transition animation.  It calculates the
     * opacity based on the elapsed fraction of the animation and
     * sets the <code>AlphaComposite</code> value on the 
     * <code>Graphics2D</code> object appropriately.
     */
    @Override
    public void setup(Graphics2D g2d, float fraction) {
	int x, y;
	float opacity;
	if (start == null) {
	    // fade-in
	    opacity = fraction;
            x = end.getX();
            y = end.getY();
	} else {
	    // fade-out
	    opacity = (1.0f - fraction);
            x = start.getX();
            y = start.getY();
	}
	g2d.translate(x, y);
	AlphaComposite newComposite = 
	    AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 
				       opacity);
	g2d.setComposite(newComposite);
        super.setup(g2d, fraction);
    }    
}
