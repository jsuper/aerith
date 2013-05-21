/*
 * TileGrabber.java
 *
 * Created on April 28, 2006, 4:43 AM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package com.sun.javaone.aerith.ui.fullscreen;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.geom.GeneralPath;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.FileImageOutputStream;

import com.sun.javaone.aerith.model.Trip;
import com.sun.javaone.aerith.util.FileUtils;
import org.jdesktop.swingx.JXMapViewer;
import org.jdesktop.swingx.mapviewer.Tile;
import org.jdesktop.swingx.mapviewer.TileFactory;
import org.jdesktop.swingx.mapviewer.TilePoint;

/**
 * Class to grab all the tiles at a specific zoom level along the travel path.
 * @author Richard Bair
 */
class TileGrabber {
    
    /** Creates a new instance of TileGrabber */
    private TileGrabber() {
    }
    
    private static int TILE_LOAD_SEMAPHORE = 0;
    private static ImageWriter writer = null;
    private static ImageWriteParam writeParam = null;
    private static BufferedImage finalImage = null;
    
    public static void main(String[] args) {
        try {
            Iterator<ImageWriter> witr = ImageIO.getImageWritersByFormatName("jpg");
            if (witr.hasNext()) {
                writer = witr.next();
                writeParam = writer.getDefaultWriteParam();
                writeParam.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                writeParam.setCompressionQuality(0.9f);
            }
            
            
            Trip t = FileUtils.readTrip(new File("saved-trips"));
            BufferedImage map = createIndyMap(t);
            FileImageOutputStream out = new FileImageOutputStream(new File("indy-map.jpg"));
            writer.setOutput(out);
            IIOImage foolio = new IIOImage(map, null, null);
            writer.write(null, foolio, writeParam);
//            ImageIO.write(map, "png", new File("indy-map.png"));
            System.exit(0);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
    
    public static BufferedImage createIndyMap(Trip t) {
        try {
            GeneralPath path = t.getPath();
            
            //following the path, grab the tiles for the path, and all around it (the path is always in the center of the screen)
            //assume a zoom level of 7
            final int zoom = 8;
            
            //gotta be able to convert from a point along the path to the world coord
            JXMapViewer mapviewer = new JXMapViewer();
            final TileFactory tileFactory = mapviewer.getTileFactory();
            //Dimension mapSize = tileFactory.getMapSize(zoom);
            path = getScaledPath(path, tileFactory, zoom);
            
            Point2D[] points = getPathPoints(path);
            
            //now that I have the path at the proper zoom, I can convert from world coords to tile coords
            Set<TilePoint> tiles = new HashSet<TilePoint>();
            TilePoint topMostPoint = null;
            TilePoint leftMostPoint = null;
            TilePoint rightMostPoint = null;
            TilePoint bottomMostPoint = null;
            for (Point2D point : points) {
                TilePoint tile = tileFactory.getTileCoordinate(point);
                tiles.add(tile);
                
                if (topMostPoint == null || topMostPoint.getY() > tile.getY()) {
                    topMostPoint = tile;
                }
                
                if (leftMostPoint == null || leftMostPoint.getX() > tile.getX()) {
                    leftMostPoint = tile;
                }
                
                if (rightMostPoint == null || rightMostPoint.getX() < tile.getX()) {
                    rightMostPoint = tile;
                }
                
                if (bottomMostPoint == null || bottomMostPoint.getY() < tile.getY()) {
                    bottomMostPoint = tile;
                }
            }
            
            //back out a big so that I have enough tiles to do the job
            topMostPoint = new TilePoint(topMostPoint.getX(), topMostPoint.getY() - 3);
            leftMostPoint = new TilePoint(leftMostPoint.getX() - 3, leftMostPoint.getY());
            bottomMostPoint = new TilePoint(bottomMostPoint.getX(), bottomMostPoint.getY() + 3);
            rightMostPoint = new TilePoint(rightMostPoint.getX() + 3, rightMostPoint.getY());
            tiles.add(topMostPoint);
            tiles.add(leftMostPoint);
            tiles.add(bottomMostPoint);
            tiles.add(rightMostPoint);

            for (TilePoint tile : tiles.toArray(new TilePoint[0])) {
                //add the tiles all around it
                for (int x=tile.getX()-3; x < tile.getX() + 3; x++) {
                    for (int y=tile.getY()-3; y<tile.getY() + 3; y++) {
                        tiles.add(new TilePoint(x, y));
                    }
                }                
            }
            
            TILE_LOAD_SEMAPHORE = tiles.size();
            
            //construct one big image with these tiles
            //calculate the size of the image:
            int width = (rightMostPoint.getX() - leftMostPoint.getX() + 1) * tileFactory.getTileSize();
            int height = (bottomMostPoint.getY() - topMostPoint.getY() + 1) * tileFactory.getTileSize();
            
            final BufferedImage map = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            final Graphics2D gfx = map.createGraphics();
            
            final int topOffset = topMostPoint.getY() * tileFactory.getTileSize();
            final int leftOffset = leftMostPoint.getX() * tileFactory.getTileSize();
            
            PropertyChangeListener loadListener = new PropertyChangeListener() {
                public void propertyChange(PropertyChangeEvent evt) {
                    Tile tile = (Tile)evt.getSource();
                    BufferedImage tileImage = tile.getImage();
                    if (tileImage != null) {
                        TilePoint point = tile.getLocation();
                        gfx.drawImage(tileImage, null,
                                (point.getX() * tileFactory.getTileSize()) - leftOffset,
                                (point.getY() * tileFactory.getTileSize()) - topOffset);
                        TILE_LOAD_SEMAPHORE--;
                        System.out.println(TILE_LOAD_SEMAPHORE);
                    }
                    
                    //if the last tile has been painted, then go ahead and save the image
                    if (TILE_LOAD_SEMAPHORE == 0) {
                        finalImage = map;
                    }
                }
            };
            
            for (TilePoint tilePoint : tiles) {
                Tile tile = tileFactory.getTile(tilePoint, zoom);
                tile.addUniquePropertyChangeListener("loaded", loadListener);
                tile.getImage();
            }
            while (finalImage == null) {
                //noinspection BusyWait
                Thread.sleep(1000);
            }
            return finalImage;
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
    }
    
    public static GeneralPath getScaledPath(GeneralPath original, TileFactory tileFactory, int zoomLevel) {
        //create a new GP that is like the old one, but scaled
        GeneralPath path = new GeneralPath();
        PathIterator itr = original.getPathIterator(new AffineTransform());
        Dimension worldInPixels = tileFactory.getMapSize(zoomLevel);
        
        while (!itr.isDone()) {
            float[] segment = new float[6]; //must use floats because lineTo etc use floats
            int pathType = itr.currentSegment(segment);
            
            switch (pathType) {
                case PathIterator.SEG_CLOSE:
                    path.closePath();
                    break;
                case PathIterator.SEG_CUBICTO:
                    path.curveTo(
                            (segment[0]*worldInPixels.width),
                            (segment[1]*worldInPixels.height),
                            (segment[2]*worldInPixels.width),
                            (segment[3]*worldInPixels.height),
                            (segment[4]*worldInPixels.width),
                            (segment[5]*worldInPixels.height));
                    break;
                case PathIterator.SEG_LINETO:
                    path.lineTo(
                            (segment[0]*worldInPixels.width),
                            (segment[1]*worldInPixels.height));
                    break;
                case PathIterator.SEG_MOVETO:
                    path.moveTo(
                            (segment[0]*worldInPixels.width),
                            (segment[1]*worldInPixels.height));
                    break;
                case PathIterator.SEG_QUADTO:
                    path.quadTo(
                            (segment[0]*worldInPixels.width),
                            (segment[1]*worldInPixels.height),
                            (segment[2]*worldInPixels.width),
                            (segment[3]*worldInPixels.height));
                    break;
            }
            itr.next();
        }
        return path;
    }
    
    /**
     * Adjusts the path points such that they will be in relation to the top left corner of the indy map
     * image instead of the virtual world
     * @noinspection ConstantConditions
     */
    public static void adjustPathPoints(Point2D[] points, TileFactory factory) {
        TilePoint topMostPoint = null;
        TilePoint leftMostPoint = null;
        for (Point2D point : points) {
            TilePoint tile = factory.getTileCoordinate(point);
            if (topMostPoint == null || topMostPoint.getY() > tile.getY()) {
                topMostPoint = tile;
            }

            if (leftMostPoint == null || leftMostPoint.getX() > tile.getX()) {
                leftMostPoint = tile;
            }
        }
        
        for (int i=0; i<points.length; i++) {
            points[i] = new Point2D.Double(
                    points[i].getX() - ((leftMostPoint.getX()-3) * 256), 
                    points[i].getY() - ((topMostPoint.getY()-3) * 256));
        }
    }
    
    /**
     * returns just the points
     */
    public static Point2D[] getPathPoints(GeneralPath path) {
        List<Point2D> points = new ArrayList<Point2D>();
        PathIterator itr = path.getPathIterator(new AffineTransform());
        while (!itr.isDone()) {
            float[] segment = new float[6]; //must use floats because lineTo etc use floats
            int pathType = itr.currentSegment(segment);
            
            switch (pathType) {
                case PathIterator.SEG_CLOSE:
                case PathIterator.SEG_CUBICTO:
                case PathIterator.SEG_QUADTO:
                    break;
                case PathIterator.SEG_MOVETO:
                case PathIterator.SEG_LINETO:
                    //capture the point
                    points.add(new Point2D.Double(segment[0], segment[1]));
                    break;
            }
            itr.next();
        }
        return points.toArray(new Point2D.Double[0]);
    }
}
