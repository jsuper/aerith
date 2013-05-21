package com.sun.animation.transitions;

import java.awt.Component;
import java.awt.Container;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;

import javax.swing.JComponent;

import org.jdesktop.animation.timing.Animator;
import org.jdesktop.animation.timing.TimingTarget;

import com.sun.animation.effects.ComponentState;

/**
 * This class is used to facilitate animated transitions in an application.
 * 
 * ScreenTransition is given a container in a Swing application.  
 * When the application wishes to transition from one state of the application
 * to another, the <code>startTransition</code> method is called, which
 * calls back into the application to first reset the state of the 
 * application, then set up the following state of the application.
 * Then ScreenTransition runs an animation from the previous state 
 * of the application to the new state.
 * 
 * REMIND: There is some confusion in this and the effects package about
 *         the use of Component vs JComponent... If this framework is
 *         primarily intended for Swing, then maybe it would be good to
 *         standardize on JComponent?
 *
 * @author Chet Haase
 */
public class ScreenTransition implements TimingTarget {
    
    /*
     * Implementation detail: The key to making ScreenTransition work
     * correctly is having two different views or layers of ScreenTransition
     * under the covers.  One layer is the "containerLayer", which is where
     * the actual child components of ScreenTransition are placed.  The
     * other (more hidden) layer is the "animationLayer", which exists
     * solely for displaying the animations which take place during
     * any transition.
     *
     * The reason we cannot animate the transitions in the same container
     * where the actual components live (at least not trivially) is that
     * we need to layout the container for both states of a transition
     * prior to actually running the animation.  Moving the components
     * around during these layout phases cannot happen onscreen (the net
     * effect would be that the user would see the end result before
     * the transition), so we do the layout on an invisible container
     * instead.  Then the animation happens to transition between the
     * two states, in his separate animationLayer.
     *
     * Further detail: the "animationLayer" is set to be the glass pane
     * of the application frame.  Glass pane already has the functionality
     * we need (specifically, it overlays the area that we need to 
     * render during the transition); the only trick is that we must
     * position the rendering correctly since the glass pane typically
     * covers the entire application frame.
     */
    
    /**
     * Handles the structure and rendering of the actual
     * animation during the transitions.
     */
    private AnimationManager animationManager;
    
    /**
     * The component where the transition animation occurs.  This
     * component (which is set to be the glass pane) is visible
     * during the transition, but is otherwise invisible.
     */
    private AnimationLayer animationLayer;
    
    /**
     * The component supplied at contruction time that holds the
     * actual components added to ScreenTransition by the application.
     * Keeping this container separate from ScreenTransition allows us
     * to render the AnimationLayer during the transitions and separate
     * the animation sequence from the actual container of the components.
     */
    private JComponent containerLayer;
    
    /**
     * Image used to store the current state of the transition
     * animation.  This image will be rendered to during
     * timingEvent() and then copied into the glass pane during
     * the repaint cycle.
     */
    private BufferedImage transitionImage;
    
    /**
     * Background that will be copied into the transitionImage on
     * every frame.  This represents the default (empty) state of
     * the containerLayer; copying this into the transitionImage is
     * like erasing to the background of the real container in the
     * application.
     */
    private BufferedImage transitionImageBG;
    
    /**
     * The user-defined code which ScreenTransition will call
     * to reset and setup the previous/next states of the application
     * during the transition setup process.
     */
    private TransitionTarget transitionTarget;

    /**
     * If the application has already set their own custom glass pane,
     * we save that component here before using the glass pane for our
     * own purposes, and then we restore the original glass pane when
     * the animation has completed.
     */
    private Component savedGlassPane;
    
    /**
     * Timing engine for the transition animation.
     */
    private Animator timingController;

    /**
     * Constructor for ScreenTransition.  The application must supply the
     * JComponent that they wish to transition and the TransitionTarget
     * which supplies the callback methods called during the transition
     * process.
     * @param transitionComponent JComponent that the application wishes
     * to run the transition on.
     * @param transitionTarget Implementation of <code>TransitionTarget</code>
     * interface which will be called during transition process.
     */
    public ScreenTransition(JComponent transitionComponent,
                            TransitionTarget transitionTarget)
    {
        this.containerLayer = transitionComponent;
	this.transitionTarget = transitionTarget;
        
        this.animationManager = new AnimationManager();
        this.animationLayer = new AnimationLayer(this);
        this.animationLayer.setVisible(false);
    }

    /**
     * Returns the content pane used in this ScreenTransition.  Applications
     * can add components directly to this container if they wish (although
     * adding components to ScreenTransition will have the same effect).
     */
    public Container getContentPane() {
        return containerLayer;
    }
    
    /**
     * Returns image used during timingEvent rendering.  This is called by
     * AnimationLayer to get the contents for the glass pane
     */
    Image getTransitionImage() {
        return transitionImage;
    }

    /**
     * Returns true if the transition consists of completely opaque components.
     */
    boolean isOpaque() {
        return containerLayer.isOpaque();
    }
    
    /**
     * Implementation of the <code>TimingTarget</code> interface.  This method
     * is called repeatedly during the transition animation.  We change the
     * animation fraction in the AnimationManager and then force a repaint,
     * which will force the current transition state to be rendered.
     */
    public void timingEvent(float elapsedFraction)
    {
        Graphics2D gImg = (Graphics2D)transitionImage.getGraphics();

        // copy background to transition image image
        gImg.drawImage(transitionImageBG, 0, 0, null);

        // Render this frame of the animation
	animationManager.paint(gImg, elapsedFraction);
        
        gImg.dispose();
        
        // Force transitionImage to be copied to the glass pane
        animationLayer.repaint();
    }
    
    /**
     * Override of <code>TimingTarget.begin()</code>; nothing to do here.
     */
    public void begin() {}

    /**
     * Override of <code>TimingTarget.end()</code>; switch the visibility of
     * the containerLayer and animationLayer and force repaint.
     */
    public void end() {
        containerLayer.getRootPane().setGlassPane(savedGlassPane);
        containerLayer.getRootPane().getGlassPane().setVisible(false);
        animationLayer.setVisible(false);
        containerLayer.setVisible(true);
	containerLayer.repaint();
        timingController = null;
        transitionTarget.transitionComplete();
    }

    /**
     * Utility method to query whether a transition is currently taking place
     */
    public boolean isTransitioning() {
        return animationLayer.isVisible();
    }
    
    /**
     * Begin the transition from the current application state to the
     * next one.  This method will call twice into the TransitionTarget specified
     * in the ScreenTransition constructor: 
     * <code>resetCurrentScreen()</code> will be called to allow the application
     * to clean up the current screen and <code>setupNextScreen()</code> will
     * be called to allow the application to set up the state of the next
     * screen.  After these calls, the transition animation will begin.
     * 
     * REMIND: should be called from EDT only?
     *
     * @param transitionTimeMS The length of this transition in milliseconds
     */
    public void startTransition(int transitionTimeMS) {
        if (isTransitioning() && timingController != null) {
            // REMIND: Might want something more robust here, such as
            // putting the application into a state representative of the
            // current transition, rather than just jumping to the end state
            // of the transition
            timingController.stop();
        }
        
	// Reset the AnimationManager (this releases all previous transition
        // data structures)
	animationManager.reset();
        
        // Capture the current state of the application into the 
        // AnimationManager; this sets up the state we are transitioning from
        for (Component child : containerLayer.getComponents()) {
            if (child.isVisible() && (child instanceof JComponent)) {
                animationManager.addStart((JComponent)child);
            }
        }
        
        // Ask the transitionTarget to reset the application state; this gives
        // the application the chance to set up what will be the background 
        // for the transition
        // REMIND: Might want to go back to original approach of simply removing
        // all components from container instead of trusting the app to
        // do this; otherwise, our use of transitioinImageBG might not work
	transitionTarget.resetCurrentScreen();
                
        // Create the transition image
        int cw = containerLayer.getWidth();
        int ch = containerLayer.getHeight();
        if (transitionImage == null || 
                transitionImage.getWidth() != cw ||
                transitionImage.getHeight() != ch) {
            // Recreate transition image and background for new dimensions
            transitionImage = 
                    (BufferedImage)containerLayer.createImage(cw, ch);
            transitionImageBG = 
                    (BufferedImage)containerLayer.createImage(cw, ch);
        }
        Graphics gImg = transitionImageBG.getGraphics();
        ComponentState.paintComponentHierarchySingleBuffered(containerLayer, gImg);
        gImg.dispose();
        
        /**
         * Debug tool; if the transitions are messed up, this allows
         * us to see the individual transitionImageBG images we are
         * creating.  They display in their own JFrame on each
         * transition.
         */
        //ImageViewer imageViewer = new ImageViewer(transitionImageBG);
                
        // This records data in animationLayer used to copy the transition 
        // contents correctly into the glass pane
	animationLayer.setupBackground(containerLayer);
        
        // Make the animationLayer visible and the contentPane invisible.  This
        // frees us to validate the application state for the next screen while
        // keeping that new state invisible from the user; the animationLayer
        // will only display contents appropriate to the transition (the previous
        // state before the transition begins, the transitioning state during
        // the transition).
        savedGlassPane = containerLayer.getRootPane().getGlassPane();
        containerLayer.getRootPane().setGlassPane(animationLayer);
        containerLayer.getRootPane().getGlassPane().setVisible(true);
	containerLayer.setVisible(false);
        
        // Now that the contentPane is invisible to the user, have the
        // application setup the next screen.  This will define the end state
        // of the application for this transition.  
	transitionTarget.setupNextScreen();
        
        // Validating the container layer component ensures correct layout
        // for the next screen of the application
        containerLayer.validate();

        // Iterate through the visible components in the next application
        // screen and add those end states to the AnimationManager
        for (Component child : containerLayer.getComponents()) {
            if (child.isVisible() && (child instanceof JComponent)) {
                animationManager.addEnd((JComponent)child);
            }
        }
	
	// Init the AnimationManager; this sets up default or custom effects
        // for each of the components involved in the transition
	animationManager.init();
        
        // workaround: need glass pane to reflect initial contents when we
        // exit this function to avoid flash of blank container
        timingEvent(0);
        
        // Create the TimingController that will run the animation
	timingController = new Animator(transitionTimeMS, this);
	timingController.start();
    }

	public void repeat() {
		
	}
}

