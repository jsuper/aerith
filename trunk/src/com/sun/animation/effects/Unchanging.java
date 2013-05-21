/*
 * Unchanging.java
 *
 * Created on January 24, 2006, 12:55 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package com.sun.animation.effects;

import java.awt.Graphics2D;
/**
 *
 * @author Chet
 */
public class Unchanging extends ComponentEffect {
    
    /** Creates a new instance of Unchanging */
    public Unchanging() {
    }

    public Unchanging(ComponentState start, ComponentState end) {
	setComponentStates(start, end);
    }
    
    public void setup(Graphics2D g2d, float fraction) {
	g2d.translate(start.getX(), start.getY());
        super.setup(g2d, fraction);
    }
}
