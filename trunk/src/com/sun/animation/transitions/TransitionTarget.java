package com.sun.animation.transitions;

/**
 * This interface is implemented by the "target" of ScreenTransition.  This
 * target will be called by ScreenTransition during the transition setup
 * and completion process, so that the application can setup and reset
 * the application state appropriately.
 * 
 * @author Chet Haase
 */
public interface TransitionTarget {
    
    /**
     * This method is called during the <code>startTransition</code> method
     * of ScreenTransition.
     *
     * Implementors will want to remove components from the ScreenTransition
     * or otherwise change the layout to get things into a plain state between
     * the previous and next screens of the application.  The purpose of this
     * approach is to allow the application to have a more complicated or
     * interesting background to the ScreenTransition's content pane than a 
     * typical plain background.  This background will be used as the 
     * background to the animation.
     */
    public void resetCurrentScreen();
    
    /**
     * This method is called during the <code>startTransition</code> method
     * of TransitionPanel.
     *
     * Implementors will need to add components to ScreenTransition in this
     * method.  This tells ScreenTransition the end-state of the components
     * for the upcoming transition.  After this method is complete, 
     * ScreenTransition has the information it needs to run the transition
     * and the animation will begin.
     */
    public void setupNextScreen();
    
    /**
     * This method is called when the transition ends.
     * 
     * Implementors may choose to do any cleanup or post-processing in this
     * method.  For example, multiple transitions may be chained in
     * sequence by using this method to kick off successive transitions.
     */
    public void transitionComplete();
    
}

