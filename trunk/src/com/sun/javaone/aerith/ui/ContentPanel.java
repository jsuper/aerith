package com.sun.javaone.aerith.ui;

import java.awt.AlphaComposite;
import java.awt.Composite;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import javax.swing.JPanel;

import com.sun.javaone.aerith.g2d.GraphicsUtil;
import java.awt.LinearGradientPaint;
//import com.sun.javaone.aerith.g2d.LinearGradientPaint;
import com.sun.javaone.aerith.model.DataType;
import org.jdesktop.fuse.InjectedResource;
import org.jdesktop.fuse.ResourceInjector;

class ContentPanel extends JPanel {
    ////////////////////////////////////////////////////////////////////////////
    // THEME SPECIFIC FIELDS
    ////////////////////////////////////////////////////////////////////////////
    @InjectedResource
    private LinearGradientPaint backgroundGradient;
    @InjectedResource
    private BufferedImage light;
    @InjectedResource
    private float lightOpacity;

    // Intermediate image to avoid gradient repaints
    private Image gradientImage = null;

    ContentPanel() {
        ResourceInjector.get().inject(this);
    }

    @Override
    protected void paintComponent(Graphics g) {
        if (!isVisible()) {
            return;
        }

        Graphics2D g2 = (Graphics2D) g;

        if (gradientImage == null) {
            // Only create this once; this assumes that the size of this
            // container never changes
            gradientImage = GraphicsUtil.createCompatibleImage(getWidth(), getHeight());
            Graphics2D g2d = (Graphics2D) gradientImage.getGraphics();
            Composite composite = g2.getComposite();
            g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                                RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2d.setPaint(backgroundGradient);
            g2d.fillRect(0, 0, getWidth(), getHeight());
            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER,
                             lightOpacity));
            g2d.drawImage(light, 0, 0, getWidth(), light.getHeight(), null);
            g2d.setComposite(composite);
            g2d.dispose();
            // !!! ONLY BECAUSE WE NEVER RECREATE THE INTERMEDIATE IMAGE
            light = null;
        }

        g2.drawImage(gradientImage, 0, 0, null);
    }
}
