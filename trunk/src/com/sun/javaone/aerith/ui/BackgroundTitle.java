package com.sun.javaone.aerith.ui;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Polygon;
import java.awt.RenderingHints;
import java.awt.font.FontRenderContext;
import java.awt.font.TextLayout;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.ConvolveOp;
import java.awt.image.Kernel;
import javax.swing.JComponent;

import org.jdesktop.fuse.InjectedResource;
import org.jdesktop.fuse.ResourceInjector;

class BackgroundTitle extends JComponent {
    private String text;
    private Image titleImage;
    private int titleHeight;
    private int titleWidth;
    
    private float shadowOffsetX;
    private float shadowOffsetY;
    
    ////////////////////////////////////////////////////////////////////////////
    // THEME SPECIFIC FIELDS
    ////////////////////////////////////////////////////////////////////////////
    @InjectedResource
    private float shadowOpacity;
    @InjectedResource
    private Color shadowColor;
    @InjectedResource
    private int shadowDistance;
    @InjectedResource
    private int shadowDirection;
    @InjectedResource
    private Font titleFont;
    @InjectedResource
    private Color titleColor;
    @InjectedResource
    private float titleOpacity;
    @InjectedResource
    private int preferredHeight;

    BackgroundTitle(final String text) {
        ResourceInjector.get().inject(this);
        setOpaque(false);
        
        this.text = text;
        computeShadow();
    }
    
    void setText(final String text) {
        this.text = text;
        titleImage = null;
        repaint();
    }
    
    private void computeShadow() {
        double rads = Math.toRadians(shadowDirection);
        shadowOffsetX = (float) Math.cos(rads) * shadowDistance;
        shadowOffsetY = (float) Math.sin(rads) * shadowDistance;
    }

    @Override
    public Dimension getPreferredSize() {
        Dimension size = super.getPreferredSize();
        size.height = preferredHeight;
        return size;
    }
    
    @Override
    public Dimension getMaximumSize() {
        Dimension size = super.getMaximumSize();
        size.height = preferredHeight;
        return size;
    }
    
    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        
        if (titleImage == null) {
            titleImage = createTitleImage(g2);
        }
        
        Composite composite = g2.getComposite();
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER,
                                                   titleOpacity));
        g2.drawImage(titleImage,
                     (getWidth() - titleWidth) / 2,
                     (getHeight() - titleHeight) / 2, null);
        g2.setComposite(composite);
    }
    
    private Image createTitleImage(Graphics2D g2) {
        FontRenderContext context = g2.getFontRenderContext();
        TextLayout layout = new TextLayout(text, titleFont, context);
        Rectangle2D bounds = layout.getBounds();
        
        BufferedImage image = new BufferedImage(getWidth() - 120,
                                                (int) bounds.getHeight() + 23,
                                                BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = image.createGraphics();
        setupGraphics(g2d);

        int[] arrowX  = { getWidth() - 135,
                          getWidth() - 125,
                          getWidth() - 135 };
        int[] arrowY  = { 3 + (int) bounds.getHeight() + 7,
                          7 + (int) bounds.getHeight() + 7,
                          12 + (int) bounds.getHeight() + 7 };
        int   npoints = 3;
        Polygon arrow = new Polygon(arrowX, arrowY, npoints); 
        
        Composite composite = g2d.getComposite();
        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER,
                                                    shadowOpacity));
        g2d.setColor(shadowColor);
        layout.draw(g2d,
                    shadowOffsetX + 5.0f,
                    layout.getAscent() - layout.getDescent() + shadowOffsetY + 5.0f);
        g2d.fillRect(5 + (int) shadowOffsetX,
                     (int) shadowOffsetY + 5 + (int) bounds.getHeight() + 7,
                     getWidth() - 135, 5);
        g2d.translate(0, shadowOffsetY);
        g2d.fill(arrow);
        g2d.translate(0, -shadowOffsetY);
        g2d.setComposite(composite);
        
        g2d.setColor(titleColor);
        layout.draw(g2d, 5.0f, 5.0f + layout.getAscent() - layout.getDescent());

        g2d.fillRect(5, 5 + (int) bounds.getHeight() + 7, getWidth() - 135, 5);
        g2d.fill(arrow);
        g2d.dispose();
        
        titleWidth = image.getWidth();
        titleHeight = image.getHeight();
        
        Kernel kernel = new Kernel(3, 3, new float[] { 1f/9f, 1/9f, 1f/9f,
                                                       1f/9f, 1/9f, 1f/9f,
                                                       1f/9f, 1/9f, 1f/9f });
        ConvolveOp op = new ConvolveOp(kernel);

        return op.filter(op.filter(image, null), null);
    }

    private static void setupGraphics(Graphics2D g2) {
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                            RenderingHints.VALUE_ANTIALIAS_ON);
    }
}
