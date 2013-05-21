package com.sun.animation.transitions;

import java.awt.Component;
import java.awt.Graphics;
import java.util.HashMap;
import java.util.Map;
import javax.swing.JComponent;

import com.sun.animation.effects.ComponentState;

/**
 * This class manages the animated rendering of the various components
 * during the transitions.  For each component in the start and end
 * screens of the transitioning component, there is a call to 
 * <code>addStart</code> or <code>addEnd</code>.  Information for the
 * start and end states are stored in individual <code>AnimationState</code>
 * on a per-component basis.  
 *
 * During the transition, the elapsed fraction is updated by a call to
 * <code>setFraction</code> and then a repaint event triggers a later
 * call to <code>paint</code>, which asks each of the <code>
 * AnimationState</code> structures to render themselves using the
 * current animation fraction.
 * 
 * @author Chet Haase
 */
class AnimationManager {
    
    private Map<Component, AnimationState> componentAnimationStates =
        new HashMap<Component, AnimationState>();
    
    AnimationManager() {
    }
    
    private AnimationState getExistingAnimationState(Component component) {
        return componentAnimationStates.get(component);
    }
    
    /**
     * This is called from the TransitionPanel to set the fraction elapsed
     * of the current transition.  This is 
     */
    void setFraction(float fraction) {
    }
    
    /**
     * Reset the AnimationStates; this clears out the old structure of
     * states after we are done with the previous transition
     */
    void reset() {
        componentAnimationStates.clear();
    }
    
    /** 
     * Init the animation; this sets up all of the individual animations
     * based on default or custom effects for each component
     */
    void init() {
        for (AnimationState state : componentAnimationStates.values()) {
            state.init();
        }
    }
    
    /**
     * Add a start state for the given component
     * @param component The individual component to be animated
     */
    void addStart(JComponent component) {
	AnimationState existingAnimState = getExistingAnimationState(component);
	if (existingAnimState != null) {
	    // Already have an end state, add this start state to existing 
	    // structure
	    existingAnimState.setStart(new ComponentState(component));
	} else {
	    AnimationState animState = new AnimationState(component, true);
            componentAnimationStates.put(component, animState);
	}
    }

    /**
     * Add an end state for the given component
     * @param component The individual component to be animated
     */
    void addEnd(JComponent component) {
	AnimationState existingAnimState = getExistingAnimationState(component);
	if (existingAnimState != null) {
	    // Already have a start state, add this end state to existing 
	    // structure
	    existingAnimState.setEnd(new ComponentState(component));
	} else {
	    AnimationState animState = new AnimationState(component, false);
            componentAnimationStates.put(component, animState);
	}
    }

    /**
     * This method is called during the transition animation.
     * Iterate through the various <code>AnimationState</code> objects,
     * asking each one to paint itself into the <code>Graphics</code>
     * object with the current animationFrame.
     * @param g The <code>Graphics</code> object that the animating objects
     * need to render themselves into.
     */
    void paint(Graphics g, float fraction) {
        for (AnimationState state : componentAnimationStates.values()) {
            state.paint(g, fraction);
        }
    }
}

