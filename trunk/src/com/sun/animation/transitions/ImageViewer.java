/*
 * ImageViewer.java
 *
 * Created on February 1, 2006, 6:55 AM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package com.sun.animation.transitions;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;
import java.io.IOException;
import java.net.URL;
import javax.imageio.ImageIO;
import javax.swing.JComponent;
import javax.swing.JFrame;

/**
 *
 * @author Chet
 */
public class ImageViewer extends JFrame {
    
    private Image image;
    
    /** Creates a new instance of ImageViewer */
    public ImageViewer(Image image) {
        this.image = image;
        this.setSize(image.getWidth(null) + 10, image.getHeight(null) + 30);
        ImageViewerComponent component = new ImageViewerComponent();
        component.setPreferredSize(new Dimension(image.getWidth(null),
                image.getHeight(null)));
        add(component);
        pack();
        setVisible(true);
    }
    
    class ImageViewerComponent extends JComponent {
        
        public void paint(Graphics g) {
            g.drawImage(image, 0, 0, null);
        }
    }
    
    public static void main(String[] args) throws IOException {
        URL url = ImageViewer.class.getResource("/resources/wood.jpg") ;
        ImageViewer iv = new ImageViewer(ImageIO.read(url));
        iv.setVisible(true);
    }
}
