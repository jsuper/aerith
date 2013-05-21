/*
 * PathPainter.java
 *
 * Created on April 28, 2006, 7:07 AM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package com.sun.javaone.aerith.ui.fullscreen;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.File;

import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.JPanel;

import org.jdesktop.animation.timing.Animator;
import org.jdesktop.animation.timing.interpolation.KeyFrames;
import org.jdesktop.animation.timing.interpolation.KeyTimes;
import org.jdesktop.animation.timing.interpolation.KeyValues;
import org.jdesktop.animation.timing.interpolation.PropertySetter;
import org.jdesktop.swingx.JXMapViewer;

import com.sun.javaone.aerith.g2d.GraphicsUtil;
import com.sun.javaone.aerith.model.Trip;
import com.sun.javaone.aerith.util.FileUtils;

/**
 *
 * @author Richard Bair
 */
public class PathPainter {
    private PathPainter() {
    }

    public static void main(String[] args) {
        try {
            Trip t = FileUtils.readTrip(new File("saved-trips"));
//            BufferedImage img = TileGrabber.createIndyMap(t);
            BufferedImage timg = ImageIO.read(new File("indy-map.jpg"));
            BufferedImage img = GraphicsUtil.toCompatibleImage(timg);
            timg.flush();
            
            JXMapViewer map = new JXMapViewer();

            GeneralPath path = TileGrabber.getScaledPath(t.getPath(), map.getTileFactory(), 8);
            Point2D[] points = TileGrabber.getPathPoints(path);
            TileGrabber.adjustPathPoints(points, map.getTileFactory());
            double[] lengths = calculatePathLengths(points);
            System.out.println(lengths[lengths.length-1]);

            JFrame frame = new JFrame();
            ImagePanel panel = new ImagePanel(img);
            frame.setContentPane(panel);
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setSize(new Dimension(1024, 768));
            frame.setVisible(true);
            KeyValues<Point2D> values = KeyValues.create(points);            
            Animator timer = PropertySetter.createAnimator(300000, panel, "offset", new KeyFrames(values));
            timer.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    public static final class ImagePanel extends JPanel {
        private static final Color PATH_COLOR = new Color(214, 31, 20, 178);
        private BufferedImage image;
        
        private GeneralPath path = null;
        
        private double x;
        private double y;
        
        public ImagePanel(BufferedImage image) {
            this.image = image;
        }
        
        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D gfx = (Graphics2D)g;
            
            int x = (int)this.x - (getWidth()/2);
            int y = (int)this.y - (getHeight()/2);
            g.translate(-x, -y);
            g.drawImage(image, 0, 0, null);
            //now paint the general path
            if (path != null) {
                g.setColor(PATH_COLOR);
                Stroke oldStroke = gfx.getStroke();
                gfx.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                gfx.setStroke(new BasicStroke(12, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                gfx.draw(path);
                gfx.setStroke(oldStroke);
            }
            g.translate(x, y);
            
        }
        
        @Override
        public Dimension getPreferredSize() {
            return new Dimension(image.getWidth(), image.getHeight());
        }
        
        public void setOffset(Point2D p) {
            this.x = p.getX();
            this.y = p.getY();
            if (path == null) {
                path = new GeneralPath();
                path.moveTo(p.getX(), p.getY());
            } else {
                path.lineTo(p.getX(), p.getY());
            }
            repaint();
        }
        
        public Point2D getOffset() {
            return new Point2D.Double(x, y);
        }
    }
    
    private static double[] calculatePathLengths(Point2D[] points) {
        double[] lengths = new double[points.length];
        Point2D prev = null;
        for (int i=0; i<points.length; i++) {
            Point2D point = points[i];
            if (prev != null) {
                double deltaX = Math.abs(point.getX() - prev.getX());
                double deltaY = Math.abs(point.getY() - prev.getY());
                lengths[i] = Math.sqrt(deltaX*deltaX + deltaY*deltaY) + lengths[i-1];
            } else {
                lengths[0] = 0;
            }
            prev = point;
        }
        return lengths;
    }
    
    private static KeyTimes calculateKeyTimes(double[] pathLengths) {
        float[] times = new float[pathLengths.length];
        for (int i=0; i<pathLengths.length; i++) {
            times[i] = (float)(pathLengths[i]/pathLengths[pathLengths.length-1]);
        }
        return new KeyTimes(times);
    }
}
