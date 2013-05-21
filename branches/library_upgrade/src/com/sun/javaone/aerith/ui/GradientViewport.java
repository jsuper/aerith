package com.sun.javaone.aerith.ui;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import javax.swing.JComponent;
import javax.swing.JViewport;

public class GradientViewport extends JViewport {
    private int gradientLength = 10;
    public enum Orientation { DEFAULT, HORIZONTAL, VERTICAL, BOTH };
    private Orientation orientation;
    private BufferedImage img;
    
    public GradientViewport() {
        this(Color.black, 10, Orientation.HORIZONTAL);
    }
    
    public GradientViewport(Color background, int gradientLength, Orientation orientation) {
        this.setBackground(background);
        this.gradientLength = gradientLength;
        this.orientation = orientation;
    }
    
    protected void paintChildren(Graphics g) {
        // draw children to a buffered image
        if (img == null || img.getWidth() != getWidth() ||
            img.getHeight() != getHeight()) {
            img = new BufferedImage(getWidth(), getHeight(),
                BufferedImage.TYPE_INT_ARGB);   
        }

        Graphics2D g2 = img.createGraphics();

        Composite composite = g2.getComposite();
        g2.setComposite(AlphaComposite.Clear);
        g2.fillRect(0, 0, getWidth(), getHeight());
        g2.setComposite(composite);
        super.paintChildren(g2);
        
        // paint over the children
        g2.setComposite(AlphaComposite.DstOut);
        GradientPaint gp = new GradientPaint(
                    0,0, getBackground(),//new Color(144,147,155,255),
                    gradientLength,0, setAlpha(getBackground(),0));//new Color(144,147,155,0));
        g2.setPaint(gp);
        g2.fillRect(0,0,gradientLength,getHeight());
        
        g2.translate(getWidth()-gradientLength,0);
        GradientPaint gp2 = new GradientPaint(
                    0,0, setAlpha(getBackground(),0),
                    gradientLength,0, getBackground());
        g2.setPaint(gp2);
        g2.fillRect(0,0,gradientLength,getHeight());
        g2.translate(-getWidth()+gradientLength,-0);
        
        
        // draw the buffered image to the screen
        g.drawImage(img,0,0,null);
    }
    
    public void setView(JComponent view) {
        view.setOpaque(false);
        super.setView(view);
    }
    
    private static Color setAlpha(Color col, int alpha) {
        return new Color(col.getRed(),col.getGreen(),col.getBlue(),alpha);
    }
}