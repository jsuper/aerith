package com.sun.animation.effects;

import java.awt.Graphics2D;
import java.awt.Image;

/**
 * Abstract class that contains the logic for rendering an image of the
 * component during the transition, rather than rendering the component
 * itself.
 *
 * @author Chet Haase
 */
public abstract class ComponentImageEffect extends ComponentEffect {
    
    /**
     * The image that will be rendered during the transition.  This image
     * must be created in subclasses prior to the call to <code>paint</code>.
     */
    protected Image targetImage;
    
    public ComponentImageEffect() {
    }

    /**
     * Copies the image into the given Graphics object
     */
    public void paint(Graphics2D g2d) {
	g2d.drawImage(targetImage, 0, 0, null);
    }
}
