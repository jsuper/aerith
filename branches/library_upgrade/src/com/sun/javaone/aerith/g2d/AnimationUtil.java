/*
 * AnimationUtil.java
 *
 * Created on April 3, 2006, 10:11 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package com.sun.javaone.aerith.g2d;

import org.jdesktop.animation.timing.Animator;
import org.jdesktop.animation.timing.interpolation.PropertySetter;
import org.jdesktop.swingx.JXPanel;

/**
 * A simple utility class for creating animatins.
 *
 * @author rbair
 * @author bpasson
 */
public final class AnimationUtil {

    /** Creates a new instance of AnimationUtil */
    private AnimationUtil() {
    }

    /**
     * Creates a fade in animation for the specified panel.
     *
     * @param panel a JXPanel, the panel to fade in.
     */
    public static Animator createFadeInAnimation(JXPanel panel) {
        return createFadeAnimation(panel, 0.01f, 0.99f);
    }

    public static Animator createFadeOutAnimation(JXPanel panel) {
        return createFadeAnimation(panel, 0.99f, 0.01f);
    }

    public static Animator createFadeAnimation(final JXPanel panel, float start, float end) {       
        
        PropertySetter target = new PropertySetter( panel, "alpha", start, end );
        Animator animator = new Animator(400, target);
        animator.setAcceleration(0.7f);
        animator.setDeceleration(0.3f);
        return animator;
    }
}
