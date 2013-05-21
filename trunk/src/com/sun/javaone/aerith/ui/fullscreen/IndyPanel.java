package com.sun.javaone.aerith.ui.fullscreen;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.BasicStroke;
import java.awt.geom.Point2D;
import java.awt.geom.Line2D;
import java.awt.image.BufferedImage;

import com.sun.javaone.aerith.g2d.GraphicsUtil;

public final class IndyPanel implements FullScreenRenderer {
    private static final Color PATH_COLOR = new Color(214, 31, 20);
    private BufferedImage map;
    private BufferedImage overlay;
    private BufferedImage[] photos;

    private Point2D oldPoint;
    private Graphics2D overlayGraphics;

    private int currentPhoto = -1;
    private float currentPhotoAlpha = 0.7f;
    
    private float alpha = 1.0f;

    IndyPanel(BufferedImage map, BufferedImage[] photos) {
        this.map = map;
        this.photos = photos;

        overlay = GraphicsUtil.createTranslucentCompatibleImage(map.getWidth(), map.getHeight());
        overlayGraphics = overlay.createGraphics();
        overlayGraphics.setColor(PATH_COLOR);
        overlayGraphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        overlayGraphics.setStroke(new BasicStroke(12, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
    }

    public void setOffset(Point2D p) {
        if (oldPoint != null) {
            overlayGraphics.draw(new Line2D.Double(oldPoint.getX(), oldPoint.getY(), p.getX(), p.getY()));
            oldPoint.setLocation(p);
        } else {
            oldPoint = (Point2D)p.clone();
        }
//        repaint();
    }

    public Point2D getOffset() {
        return oldPoint;
    }

    public void setCurrentPhoto(int index) {
        currentPhoto = index;
    }

    public void setCurrentPhotoAlpha(float alpha) {
        currentPhotoAlpha = alpha;
    }

    public void setAlpha(float alpha) {
        this.alpha = alpha;
    }

    public float getAlpha() {
        return alpha;
    }

    public float getCurrentPhotoAlpha() {
        return currentPhotoAlpha;
    }

    public int getCurrentPhoto() {
        return currentPhoto;
    }

    public boolean isDone() {
        return false;
    }

    public void start() {
    }

    public void end() {
        map.flush();
        overlay.flush();
        for (BufferedImage image : photos) {
            image.flush();
        }
    }

    public void render(Graphics g, Rectangle bounds) {
        Graphics2D gfx = (Graphics2D)g;

        int x = 0;
        int y = 0;
        if (oldPoint != null) {
            x = (int) ((int)oldPoint.getX() - (bounds.getWidth()/2));
            y = (int) ((int)oldPoint.getY() - (bounds.getHeight()/2));
        }

        Composite oldComposite = gfx.getComposite();
        //gfx.setComposite(AlphaComposite.SrcOver.derive(alpha));
        gfx.drawImage(map, -x, -y, null);

        gfx.setComposite(AlphaComposite.SrcOver.derive(.7f * alpha));
        gfx.drawImage(overlay, -x, -y, null);

        //blend on top of this map the current photo at the current alpha level
        if (currentPhoto >= 0) {
            x = (int)((bounds.getWidth() - photos[currentPhoto].getWidth()) / 2) + bounds.x;
            y = (int)((bounds.getHeight() - photos[currentPhoto].getHeight()) / 2) + bounds.y;
            gfx.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, currentPhotoAlpha * alpha));
            gfx.drawImage(photos[currentPhoto], null, x, y);
        }
        int nextPhoto = currentPhoto + 1;
        if (nextPhoto < photos.length) {
            x = (int)((bounds.getWidth() - photos[nextPhoto].getWidth()) / 2) + bounds.x;
            y = (int)((bounds.getHeight() - photos[nextPhoto].getHeight()) / 2) + bounds.y;
            gfx.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, Math.max(0, (.5f - currentPhotoAlpha) * alpha)));
            gfx.drawImage(photos[nextPhoto], null, x, y);
        }
        gfx.setComposite(oldComposite);
    }

    public void cancel() {
    }
}
