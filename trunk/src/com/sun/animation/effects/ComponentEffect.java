package com.sun.animation.effects;

import java.awt.Graphics2D;
import java.awt.Image;
import javax.swing.JComponent;

/**
 * This is the base class for all effects that are used during 
 * screen transitions.  It is also the repository for caching and
 * retrieving custom effects.
 *
 * Subclasses of this base class will implement the <code>setup()</code>
 * and <code>paint()</code> methods as appropriate to set up the 
 * Graphics state and perform the rendering necessary to achieve the
 * desired effect.
 *
 * @author Chet Haase
 */
public abstract class ComponentEffect {

    /** Information about the start state used by this effect. */
    protected ComponentState start;
    /** Information about the end state used by this effect. */
    protected ComponentState end;
    /** Flag to indicate whether effect needs to re-render Component */
    protected boolean renderComponent = false;
    /**
     * The image that will be used during the transition, for effects that
     * opt to not re-render the components directly.  The image will be
     * set when the start and end states are set.
     */
    protected Image componentImage;
    /** Current x location. */
    protected int x;
    /** Current y location. */
    protected int y;
    /** Current width. */
    protected int width;
    /** Current height. */
    protected int height;

    protected JComponent getComponent() {
        if (start != null) {
            return start.getComponent();
        } else if (end != null) {
            return end.getComponent();
        }
        // Should not get here
        return null;
    }
    
    
    void setRenderComponent(boolean renderComponent) {
        this.renderComponent = renderComponent;
    }

    boolean getRenderComponent() {
        return renderComponent;
    }

    /**
     * Sets both the start and end states of this Effect.
     */
    public void setComponentStates(ComponentState start, ComponentState end) {
	this.start = start;
	this.end = end;
    }
    
    /**
     * Sets the start state of this Effect.
     */
    public void setStart(ComponentState start) {
	this.start = start;
    }
    
    public ComponentState getStart() {
        return start;
    }
    
    /**
     * Sets the end state of this Effect.
     */
    public void setEnd(ComponentState end) {
	this.end = end;
    }

    public ComponentState getEnd() {
        return end;
    }
    
    public Image getComponentImage() {
        return componentImage;
    }
    
    protected void setComponentImage(Image componentImage) {
        this.componentImage = componentImage;
    }

    private void createComponentImage() {
        if (start != null && end == null) {
            componentImage = start.getSnapshot();
        } else if (start == null && end != null) {
            componentImage = end.getSnapshot();
        } else if (start.getWidth() != end.getWidth() || 
                start.getHeight() != end.getHeight()) {
            // This block grabs the targetImage
            // that best represents the component; the larger the better.
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
    }

    /**
     * This method is called during each frame of the transition animation,
     * prior to the call to <code>paint()</code>.
     * Subclasses will implement this method to set up the Graphic state,
     * or other related state, that will be used in the following call to
     * the <code>paint()</code> method.  Note that changes to the 
     * <code>Graphics2D</code> object here will still be present in the
     * <code>Graphics2D</code> object that is passed into the 
     * <code>paint()</code> method, so this is a good time to set up things
     * such as transform state.
     * @param g2d the Graphics2D destination for this rendering
     * @param fraction The fraction of elapsed time in this animation
     */
    public void setup(Graphics2D g2d, float fraction) {
        if (!renderComponent && componentImage == null) {
            createComponentImage();
        }
        if (renderComponent) {
            x = getPosition(start.getX(), end.getX(), fraction);
            y = getPosition(start.getY(), end.getY(), fraction);
            width = getPosition(start.getWidth(), end.getWidth(), fraction);
            height  = getPosition(start.getHeight(), end.getHeight(), fraction);
        }
    }
    
    /**
     * This method is called during each frame of the transition animation,
     * after the call to <code>setup()</code>. 
     * Subclasses will implement this method to perform whatever rendering
     * is necessary to paint the transitioning component into the 
     * <code>Graphics2D</code> object with the desired effect.
     * @param g2d The Graphics2D destination for this rendering.  Note that
     * the state of this Graphics2D object is affected by the previous call
     * to <code>setup</code> so there may be no more need to perturb the 
     * graphics state any more; you may simply want to render the component
     * as appropriate.
     */
    public void paint(Graphics2D g2d) {
        if (!renderComponent && (componentImage != null)) {
            g2d.drawImage(componentImage, 0, 0, null);
        } else {
            getComponent().setBounds(x, y, width, height);
            getComponent().validate();
            ComponentState.paintComponentSingleBuffered(getComponent(), g2d);
        }
    }

    private static int getPosition(int start, int end, float fraction) {
        return (int)(fraction * (float)(end - start)) + start;
    }
}
