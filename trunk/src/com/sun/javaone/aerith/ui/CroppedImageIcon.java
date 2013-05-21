/*
 * CroppedImageIcon.java
 *
 * Created on April 2, 2006, 3:47 AM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package com.sun.javaone.aerith.ui;

import java.awt.Component;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import javax.swing.Icon;

/**
 *
 * @author jm158417
 */

public final class CroppedImageIcon implements Icon {
    private final BufferedImage image;
    private final int height;
    
    public CroppedImageIcon(BufferedImage image, int height) {
        this.image = image;
        this.height = height;
    }
    
    public void paintIcon(Component c, Graphics g, int x, int y) {
        g.drawImage(image, x, y, x + image.getWidth(), y + height,
                0, image.getHeight() / 2 - height / 2,
                image.getWidth(), image.getHeight() / 2 + height / 2, null);
    }
    
    public int getIconWidth() {
        return image.getWidth();
    }
    
    public int getIconHeight() {
        return height;
    }
}
