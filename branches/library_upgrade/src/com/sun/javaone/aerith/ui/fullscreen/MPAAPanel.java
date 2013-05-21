package com.sun.javaone.aerith.ui.fullscreen;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.BasicStroke;
import java.awt.geom.Area;
import java.awt.geom.Point2D;
import java.awt.geom.Line2D;
import java.awt.image.BufferedImage;

import com.sun.javaone.aerith.g2d.GraphicsUtil;
import java.io.IOException;
import javax.imageio.ImageIO;

public final class MPAAPanel implements FullScreenRenderer {
    private float alpha = 0.0f;
    private BufferedImage mpaaImage;
    private Color background;
    
    MPAAPanel() {
        try {
            mpaaImage = GraphicsUtil.loadCompatibleImage(MPAAPanel.class.getResource("/resources/mpaa.png"));
            background = new Color(mpaaImage.getRGB(0, 0));
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    public void setAlpha(float alpha) {
        this.alpha = alpha;
    }

    public float getAlpha() {
        return alpha;
    }

    public boolean isDone() {
        return false;
    }

    public void start() {
    }

    public void end() {
    }

    public void render(Graphics g, Rectangle bounds) {
        Graphics2D gfx = (Graphics2D)g;
        Composite old = gfx.getComposite();
        gfx.setComposite(AlphaComposite.SrcOver.derive(alpha));
        int x = (int)(bounds.getCenterX() - mpaaImage.getWidth()/2);
        int y = (int)(bounds.getCenterY() - mpaaImage.getHeight()/2);
        Area area = new Area(bounds);
        area.subtract(new Area(new Rectangle(x, y, mpaaImage.getWidth(), mpaaImage.getHeight())));
        gfx.setColor(background);
        gfx.fill(area);
        gfx.drawImage(mpaaImage, null, x, y);
        gfx.setComposite(old);
    }

    public void cancel() {
    }
}
