package com.sun.animation.transitions;

import java.awt.Graphics;
import java.awt.Point;
import javax.swing.JComponent;
import javax.swing.SwingUtilities;

/**
 * This is the component where the transition animations actually run.
 * During a transition, this layer becomes visible in the TransitionPanel.
 * Regular repaint() events occur on the TransitionPanel, which trickle down
 * to paint() events here.  That method, in turn, calls paint() on
 * the AnimationManager to handle rendering the various elements of the
 * animation into the Graphics object.
 *
 * @author Chet Haase
 */
class AnimationLayer extends JComponent {
    
    private Point componentLocation = new Point();
    private final ScreenTransition screenTransition;
    
    public AnimationLayer(ScreenTransition screenTransition) {
        setOpaque(false);        
        this.screenTransition = screenTransition;
    }
    
    /**
     * Called from TransitionPanel to setup the correct location to
     * copy the animation to in the glass pane
     */
    public void setupBackground(JComponent targetComponent) {
        componentLocation.setLocation(0, 0);
        componentLocation =
            SwingUtilities.convertPoint(
                targetComponent, componentLocation,
                targetComponent.getRootPane().getGlassPane());
    }
    
    /**
     * Called during normal Swing repaint process on the TransitionPanel.
     * This simply copies the transitionImage from ScreenTransition into
     * the appropriate location in the glass pane.
     */
    @Override
    public void paintComponent(Graphics g) {
        g.translate(componentLocation.x, componentLocation.y);
        g.drawImage(screenTransition.getTransitionImage(), 0, 0, null);
    }
}
