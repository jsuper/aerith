/*
 * AerithPanelPainter.java
 *
 * Created on March 31, 2006, 2:26 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package com.sun.javaone.aerith.ui;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;
import javax.swing.JComponent;

import org.jdesktop.swingx.painter.AbstractPainter;

/**
 * Simple CompositePainter that gives the look we want
 *
 * @author rbair
 */
public class AerithPanelPainter extends AbstractPainter {
    /** Creates a new instance of AerithPanelPainter */
    public AerithPanelPainter() {
        super();
        setUseCache(false);
        setAntialiasing(RenderingHints.VALUE_ANTIALIAS_ON);
    }

    @Override
    public void paintBackground(Graphics2D g2, JComponent c) {
        float alpha = 0.8f;
        Composite composite = g2.getComposite();
        if (composite instanceof AlphaComposite) {
            alpha *= ((AlphaComposite) composite).getAlpha();
        }
        
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
        g2.setColor(new Color(0.0f, 0.0f, 0.0f));
        RoundRectangle2D rect = new RoundRectangle2D.Double(0, 0,
            c.getWidth() - 1, c.getHeight() - 1, 24, 24);
        g2.fill(rect);
        
        Ellipse2D ellipse = new Ellipse2D.Double(-c.getWidth(),
            c.getHeight() / 3.0, c.getWidth() * 3.0,
            c.getHeight() * 2.0);

        Area area = new Area(new Rectangle(0, 0,
            c.getWidth(), c.getHeight()));
        area.subtract(new Area(ellipse));
        area.intersect(new Area(rect));
     
        alpha = .1f;
        if (composite instanceof AlphaComposite) {
            alpha *= ((AlphaComposite) composite).getAlpha();
        }
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
        g2.setColor(new Color(1.0f, 1.0f, 1.0f));
        g2.fill(area);
        g2.setComposite(composite);
    }
}
