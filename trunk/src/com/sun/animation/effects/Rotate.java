package com.sun.animation.effects;

import java.awt.Component;
import java.awt.Graphics2D;

/**
 * This ComponentEffect rotates an image through a given number of degrees
 * during the animated transition.  It subclasses ComponentImageEffect to
 * use an image for redrawing the component instead of re-rendering
 * the component each time.
 *
 * @author Chet Haase
 */
public class Rotate extends ComponentEffect {
    
    /** The x coordinate of the center location of the component. */
    private final int xCenter;
    /** The y coordinate of the center location of the component. */
    private final int yCenter;
    /** The total number of degrees to sweep through during the transition. */
    private final int degrees;

    /**
     * Construct a Rotate effect for a given component with the number of 
     * degrees you wish to rotate through during the transition.  This 
     * constructor will result in an effect that rotates around the center
     * of the component
     */
    public Rotate(Component component, int degrees) {
        this.degrees = degrees;
        
        // Rotate around the center of the component
        xCenter = component.getWidth() / 2;
        yCenter = component.getHeight() / 2;
    }
    
    /** 
     * Construct a Rotate effect for a given component with the number
     * of degrees you wish to rotate through during the transition and the
     * center of rotation to use.
     */
    public Rotate(ComponentState start, ComponentState end,
                  int degrees, int xCenter, int yCenter)
    {
	setComponentStates(start, end);
	this.degrees = degrees;
	this.xCenter = xCenter;
	this.yCenter = yCenter;
    }

    @Override
    public void setup(Graphics2D g2d, float fraction) {
        // translate back and forth to rotate around the right point
	g2d.translate(xCenter, yCenter);
	g2d.rotate(Math.toRadians(fraction * degrees));
	g2d.translate(-xCenter, -yCenter);
        super.setup(g2d, fraction);        
    }    
}
