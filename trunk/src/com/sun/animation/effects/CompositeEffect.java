package com.sun.animation.effects;

import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.List;

/**
 * This Effect combines one or more sub-effects to create a more complex
 * and interesting effect.  For example, you could create an effect that
 * both moves and scales by creating a CompositeEffect with the Move
 * and Scale effects.
 *
 * Composite effects are created by simply adding effects in the order
 * that you want them combined.
 *
 * @author Chet Haase
 */
public class CompositeEffect extends ComponentEffect {
    
    /**
     * The list of effects in the CompositeEffect.
     */
    private final List<ComponentEffect> effects =
        new ArrayList<ComponentEffect>();
    
    /**
     * Creates a CompositeEffect with no sub-effects.  Additional sub-effects
     * should be added via the <code>addEffect</code> method.
     */
    public CompositeEffect() {
    }
    
    /**
     * Creates a CompositeEffect with the given effect as the first
     * sub-effect.  Additional sub-effects
     * should be added via the <code>addEffect</code> method.
     */
    public CompositeEffect(ComponentEffect effect) {
        addEffect(effect);
    }
    
    /**
     * Adds an additional effect to this CompositeEffect.  This effect is
     * added to the end of the existing list of effects, and will be processed
     * after the other effects have been processed.
     */
    public void addEffect(ComponentEffect effect) {
	effects.add(effect);
        if (effect.getRenderComponent()) {
            this.setRenderComponent(true);
        }
        if (start == null) {
            start = effect.getStart();
        }
        if (end == null) {
            end = effect.getEnd();
        }
    }
        
    /**
     * This method is called during the initialization process of a
     * transition and allows the effects to set up the start state for
     * each effect.
     */
    public void setStart(ComponentState start) {
        for (ComponentEffect effect : effects) {
	    effect.setStart(start);
	}
        super.setStart(start);
    }
    
    /**
     * This method is called during the initialization process of a
     * transition and allows the effects to set up the end state for
     * each effect.
     */
    public void setEnd(ComponentState end) {
        for (ComponentEffect effect : effects) {
	    effect.setEnd(end);
	}
        super.setEnd(end);
    }

    /**
     * This method is called during each frame of the transition animation
     * and allows the effect to set up the Graphics state according to the
     * various sub-effects in this CompositeEffect.
     */
    @Override
    public void setup(Graphics2D g2d, float fraction) {
	for (int i = 0; i < effects.size(); ++i) {
	    ComponentEffect effect = effects.get(i);
	    effect.setup(g2d, fraction);
            // Grab the image for this effect from one of the sub-effects
            // REMIND: This breaks down if we have a CompositeEffect
            // composed of several non-image-based effects
	    if (!renderComponent && componentImage == null) {
		setComponentImage(effect.componentImage);
	    }
	}
        super.setup(g2d, fraction);
    }
    
}
