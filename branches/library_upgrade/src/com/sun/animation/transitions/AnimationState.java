package com.sun.animation.transitions;

import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import javax.swing.JComponent;

import com.sun.animation.effects.ComponentEffect;
import com.sun.animation.effects.ComponentState;
import com.sun.animation.effects.CompositeEffect;
import com.sun.animation.effects.EffectsManager;
import com.sun.animation.effects.FadeIn;
import com.sun.animation.effects.FadeOut;
import com.sun.animation.effects.Move;
import com.sun.animation.effects.Scale;
import com.sun.animation.effects.Unchanging;

/**
 * This class holds the start and/or end states for a JComponent.  It also
 * determine (at <code>init()</code> time) the Effect to use during the
 * upcoming transition and calls the appropriate Effect during the
 * <code>paint()</code> method to cause the correct rendering of the
 * component during the transition.
 *
 * @author Chet Haase
 */
class AnimationState {
    
    /**
     * The component for this AnimationState; there is one component per
     * state, with either a start, an end, or both states.
     */
    private JComponent component;
    
    /**
     * Start/end states for this AnimationState; these may be set to a non-null
     * value or not, depending on whether the component exists in the
     * respective screen of the transition.
     */
    private ComponentState start, end;
    
    /**
     * Effect used to transition between the start and end states for this
     * AnimationState.  This effect is set during the init() method just
     * prior to running the transition.
     */
    private ComponentEffect effect;
    
    /**
     * Constructs a new AnimationState with either the start
     * or end state for the component.
     */
    AnimationState(JComponent component, boolean isStart) {
        this.component = component;
        ComponentState compState = new ComponentState(component);
        if (isStart) {
            start = compState;
        } else {
            end = compState;
        }
    }
    
    void setStart(ComponentState compState) {
        start = compState;
    }

    void setEnd(ComponentState compState) {
        end = compState;
    }
    
    ComponentState getStart() {
        return start;
    }
    
    ComponentState getEnd() {
        return end;
    }
    
    Component getComponent() {
        return component;
    }

    /**
     * Called just prior to running the transition.  This method examines the
     * start and end states as well as the ComponentEffect repository to 
     * determine the appropriate Effect to use during the transition for
     * this AnimationState.  If there is an existing custom effect defined
     * for the component for this type of transition, we will use that
     * effect, otherwise we will default to the appropriate effect (fading
     * in, fading out, or moving/resizing).
     */
    void init() {
        if (start == null) {
            effect = new FadeIn(end);
        } else if (end == null) {
            effect = new FadeOut(start);
        } else {
            effect = EffectsManager.getEffect(component,
                EffectsManager.TransitionType.CHANGING);
            if (effect == null) {
                // No custom effect; use move/scale combinations
                // as appropriate
                boolean move = false, scale= false;
                if (start.getX() != end.getX() || start.getY() != end.getY()) {
                    move = true;
                }
                if (start.getWidth() != end.getWidth() ||
                        start.getHeight() != end.getHeight()) {
                    scale = true;
                }
                if (move) {
                    if (scale) {
                        // move/scale
                        ComponentEffect moveEffect = new Move(start, end);
                        ComponentEffect scaleEffect = new Scale(start, end);
                        effect = new CompositeEffect(moveEffect);
                        ((CompositeEffect)effect).addEffect(scaleEffect);
                    } else {
                        // just move
                        effect = new Move(start, end);
                    }
                } else {
                    if (scale) {
                        // just scale
                        effect = new Scale(start, end);
                    } else {
                        // Noop
                        effect = new Unchanging(start, end);
                    }
                }
            } else {
                // Custom effect; set it up for this transition
                effect.setStart(start);
                effect.setEnd(end);
            }
        }
    }
    
    /**
     * Render this AnimationState into the given Graphics object with the
     * given elapsed fraction for the transition.  This is done by calling
     * into the effect to first set up the Graphics object given the
     * transition fraction and then to do the actual rendering using the
     * Graphics object.
     */
    void paint(Graphics g, float fraction) {
        if (effect != null) {
            Graphics2D g2d = (Graphics2D)g.create();
            effect.setup(g2d, fraction);
            effect.paint(g2d);
            g2d.dispose();
        }
    }
}

