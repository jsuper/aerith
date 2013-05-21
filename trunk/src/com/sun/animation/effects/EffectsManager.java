package com.sun.animation.effects;

import java.awt.Component;
import java.util.HashMap;
import java.util.Map;

/**
 * REMIND: docs...
 *
 * @author Chet Haase
 */
public class EffectsManager {
    
    /**
     * An enum that describes the type of transition that this effect
     * should be used for.
     */
    public static enum TransitionType {
        /**
         * Applies to components that exist in both the start
         * and end states of the transition.
         */
	CHANGING, 
        /**
         * Applies to components that do not exist in the
         * start state, but exist in the end state.
         */
	APPEARING, 
        /**
         * Applies to components that do not exist in the
         * end state, but exist in the start state.
         */
	DISAPPEARING
    };
    
    private static final Map<Component, ComponentEffect>
        cachedChangingEffects = new HashMap<Component, ComponentEffect>();
    private static final Map<Component, ComponentEffect>
        cachedAppearingEffects = new HashMap<Component, ComponentEffect>();
    private static final Map<Component, ComponentEffect>
        cachedDisappearingEffects = new HashMap<Component, ComponentEffect>();
    
    /**
     * This method is used to cache a custom effect on a per-component
     * basis for the application.  Note that these custom effects are
     * application wide for the duration of the process, or until a new
     * or null effect is set for this component.  Note also that custom
     * effects are registered according to the <code>TransitionType</code>.
     * So a custom <code>TransitionType.CHANGING</code> effect for a 
     * given component will have no bearing on the effect used in a 
     * transition where the component either appears or disappers between
     * the transition states.
     * @param component The Component that this effect should be applied to
     * @param effect The custom effect desired.  A null argument effectively
     * cancels any prior custom value for this component and this 
     * TransitionType
     * @param transitionType The type of transition to apply this effect on
     * @see TransitionType
     */
    public static void setEffect(Component component, ComponentEffect effect,
                                 TransitionType transitionType)
    {
	switch (transitionType) {
	    case CHANGING:
		cachedChangingEffects.put(component, effect);
		break;
	    case APPEARING:
		cachedAppearingEffects.put(component, effect);
		break;
	    case DISAPPEARING:
		cachedDisappearingEffects.put(component, effect);
		break;
	    default:
                throw new InternalError("unknown TransitionType");
	}
    }
    
    /**
     * This method is called during the setup phase for any transition.  It 
     * queries the cache for custom effects for a given component and
     * <code>TransitionType</code>
     * @param component The component we are querying on behalf of
     * @param transitionType The type of transition that the component
     * is going to undergo
     * @return ComponentEffect A null return value indicates that there is
     * no custom effect associated with this component and transition type
     */
    public static ComponentEffect getEffect(Component component,
                                            TransitionType transitionType)
    {
	switch (transitionType) {
	    case CHANGING:
		return cachedChangingEffects.get(component);
	    case APPEARING:
		return cachedAppearingEffects.get(component);
	    case DISAPPEARING:
		return cachedDisappearingEffects.get(component);
	    default:
                throw new InternalError("unknown TransitionType");
	}
    }

    /** Private constructor to prevent instantiation. */
    private EffectsManager() {
    }
}
