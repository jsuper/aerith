package com.sun.animation.effects;

/**
 * Simple subclass of Fade effect that will fade a component from nothing to
 * an existing end state.
 *
 * @author Chet Haase
 */
public class FadeIn extends Fade {
    
    /** 
     * Creates a new instance of FadeIn with the given end state.
     *
     * @param end The <code>ComponentState</code> at the end of the
     * transition; this is what we are fading into (from nothing into this
     * final representation).
     */
    public FadeIn(ComponentState end) {
	setEnd(end);
    }
}
    