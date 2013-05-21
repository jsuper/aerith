package com.sun.animation.effects;

/**
 * Simple subclass of Fade effect that will fade a component from an existing
 * start state to nothing.
 *
 * @author Chet Haase
 */
public class FadeOut extends Fade {
    
    /** 
     * Creates a new instance of FadeOut with the given start state.
     *
     * @param start The <code>ComponentState</code> at the beginning of the
     * transition; this is what we are fading (from this starting
     * representation into nothing).
     */
    public FadeOut(ComponentState start) {
	setStart(start);
    }
}
