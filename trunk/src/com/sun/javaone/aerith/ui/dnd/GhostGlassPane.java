/*
 * GhostGlassPane.java
 *
 * Created on April 3, 2006, 12:50 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package com.sun.javaone.aerith.ui.dnd;

import java.awt.AlphaComposite;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Area;
import java.awt.image.BufferedImage;
import javax.swing.JPanel;
import javax.swing.Timer;

/**
 *
 * @author jm158417
 */
public class GhostGlassPane extends JPanel {
    private static final int ANIMATION_DELAY = 500;

    private BufferedImage dragged = null;
    private Point location = new Point(0, 0);
    private Point oldLocation = new Point(0, 0);
    
    private int width;
    private int height;
    private Rectangle visibleRect = null;
    
    private float zoom = 1.0f;
    private float alpha = 0.7f;

    public GhostGlassPane() {
        setOpaque(false);
    }

    public void setImage(BufferedImage dragged) {
        setImage(dragged, dragged == null ? 0 : dragged.getWidth());
    }
     
    public void setImage(BufferedImage dragged, int width) {
        if (dragged != null) {
            float ratio = (float) dragged.getWidth() / (float) dragged.getHeight();
            this.width = width;
            height = (int) (width / ratio);
        }

        this.dragged = dragged;
    }
    
    public float getZoom() {
        return zoom;
    }
    
    public void setZoom(float zoom) {
        this.zoom = zoom;
        repaint();
    }

    public void setPoint(Point location) {
        this.oldLocation = this.location;
        this.location = location;
    }
    
    public Rectangle getRepaintRect() {
        int x = (int) (location.getX() - (width * zoom / 2));
        int y = (int) (location.getY() - (height * zoom / 2));
        
        int x2 = (int) (oldLocation.getX() - (width * zoom / 2));
        int y2 = (int) (oldLocation.getY() - (height * zoom / 2));
        
        int width = (int) (this.width * zoom);
        int height = (int) (this.height * zoom);
        
        return new Rectangle(x, y, width, height).union(new Rectangle(x2, y2, width, height));
    }

    @Override
    protected void paintComponent(Graphics g) {
        if (dragged == null || !isVisible()) {
            return;
        }

        Graphics2D g2 = (Graphics2D) g.create();
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                            RenderingHints.VALUE_ANTIALIAS_ON);

        int x = (int) (location.getX() - (width * zoom / 2));
        int y = (int) (location.getY() - (height * zoom / 2));
        
        if (visibleRect != null) {
            g2.setClip(visibleRect);
        }
        
        if (visibleRect != null) {
            Area clip = new Area(visibleRect);
            g2.setClip(clip);
        }
        
        g2.drawImage(dragged, x, y, (int) (width * zoom), (int) (height * zoom), null);
    }

    public void startAnimation(Rectangle visibleRect) {
        this.visibleRect = visibleRect;
        new Timer(1000 / 30, new FadeOutAnimation()).start();
    }

    private class FadeOutAnimation implements ActionListener {
        private long start;

        FadeOutAnimation() {
            this.start = System.currentTimeMillis();
            oldLocation = location;
        }
        
        public void actionPerformed(ActionEvent e) {
            long elapsed = System.currentTimeMillis() - start;
            if (elapsed > ANIMATION_DELAY) {
                ((Timer) e.getSource()).stop();
                setVisible(false);
                zoom = 1.0f;
                alpha = 0.6f;
                visibleRect = null;
                dragged = null;
                DragAndDropLock.setLocked(false);
            } else {
                alpha = 0.6f - (0.6f * (float) elapsed / (float) ANIMATION_DELAY);
                zoom = 1.0f + 3.0f * ((float) elapsed / (float) ANIMATION_DELAY);
            }
            repaint(getRepaintRect());
        }
    }
}