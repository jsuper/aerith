package org.progx.twinkle.ui;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

public class Picture {
    private String name;
    private BufferedImage image;

    private BufferedImage thumb = null;
    private int requestedThumbSize = 64;

    public Picture(String name, BufferedImage image) {
        this.name = name;
        this.image = image;
    }

    public BufferedImage getThumbnail(int thumbWidth) {
        if (thumbWidth >= image.getWidth()) {
            return image;
        }
        
        if (thumbWidth != requestedThumbSize || thumb == null) {
            requestedThumbSize = thumbWidth;
            generateThumbnail();
        }
        
        return thumb;
    }
    
    private void generateThumbnail() {
        float ratio = getRatio();
        int width = image.getWidth();
        thumb = image;
        
        do {
            width /= 2;
            if (width < requestedThumbSize) {
                width = requestedThumbSize;
            }
            
            BufferedImage temp = new BufferedImage(width, (int) (width / ratio), BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2 = temp.createGraphics();
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                                RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2.drawImage(thumb, 0, 0, temp.getWidth(), temp.getHeight(), null);
            g2.dispose();

            thumb = temp;
        } while (width != requestedThumbSize);
    }

    public BufferedImage getImage() {
        return image;
    }

    public String getName() {
        return name;
    }

    public float getRatio() {
        return (float) image.getWidth() / (float) image.getHeight();
    }
}
