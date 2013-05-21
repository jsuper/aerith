package com.sun.javaone.aerith.ui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.GeneralPath;
import java.awt.geom.PathIterator;

import com.sun.javaone.aerith.model.Trip;
import org.jdesktop.swingx.JXMapViewer;
import org.jdesktop.swingx.mapviewer.MapOverlay;
import org.jdesktop.swingx.painter.AbstractPainter;


final class TripPathMapOverlay extends AbstractPainter<JXMapViewer> implements MapOverlay<JXMapViewer> {
    private static final int[] STROKE_AT_ZOOM = new int[] {
        5,  //zoom level 0 (all the way zoomed out)
        5,  //zoom level 1
        6,  //zoom level 2
        7,  //zoom level 3
        10, //zoom level 4
        10, //zoom level 5
        10, //zoom level 6
        10, //zoom level 7
        10, //zoom level 8
        10, //zoom level 9
        10, //zoom level 10
        10, //zoom level 11
        10, //zoom level 12
        10, //zoom level 13
        10, //zoom level 14
        10, //zoom level 15
        10, //zoom level 16
        10  //zoom level 17
    };
    private static final Color PATH_COLOR = new Color(214, 31, 20, 178);
    private final TripEditPanel tripEditPanel;

    public TripPathMapOverlay(TripEditPanel tripEditPanel) {
        this.tripEditPanel = tripEditPanel;
        super.setUseCache(false);
        setAntialiasing(RenderingHints.VALUE_ANTIALIAS_ON);
    }
    
    protected void paintBackground(Graphics2D g, JXMapViewer map) {
        //attempt to draw the drawPath
        Trip trip = tripEditPanel.getTrip();
        GeneralPath drawPath = trip == null ? null : trip.getPath();
        if (drawPath != null) {
            //pick a stroke width according to the zoom level
            g.setStroke(new BasicStroke(STROKE_AT_ZOOM[map.getZoom()], BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.setColor(PATH_COLOR);
            
            //create a new GP that is like the old one, but scaled
            GeneralPath path = new GeneralPath();
            PathIterator itr = drawPath.getPathIterator(new AffineTransform());
            Dimension worldInPixels = map.getTileFactory().getMapSize(map.getZoom());
            Point viewportOrigin = map.getViewportBounds().getLocation();
                        
            while (!itr.isDone()) {
                float[] segment = new float[6]; //must use floats because lineTo etc use floats
                int pathType = itr.currentSegment(segment);

                switch (pathType) {
                    case PathIterator.SEG_CLOSE:
                        path.closePath();
                        break;
                    case PathIterator.SEG_CUBICTO:
                        path.curveTo(
                                (segment[0]*worldInPixels.width) - (float)viewportOrigin.getX(),
                                (segment[1]*worldInPixels.height) - (float)viewportOrigin.getY(),
                                (segment[2]*worldInPixels.width) - (float)viewportOrigin.getX(),
                                (segment[3]*worldInPixels.height) - (float)viewportOrigin.getY(),
                                (segment[4]*worldInPixels.width) - (float)viewportOrigin.getX(),
                                (segment[5]*worldInPixels.height) - (float)viewportOrigin.getY());
                        break;
                    case PathIterator.SEG_LINETO:
                        path.lineTo(
                                (segment[0]*worldInPixels.width) - (float)viewportOrigin.getX(),
                                (segment[1]*worldInPixels.height) - (float)viewportOrigin.getY());
                        break;
                    case PathIterator.SEG_MOVETO:
                        path.moveTo(
                                (segment[0]*worldInPixels.width) - (float)viewportOrigin.getX(),
                                (segment[1]*worldInPixels.height) - (float)viewportOrigin.getY());
                        break;
                    case PathIterator.SEG_QUADTO:
                        path.quadTo(
                                (segment[0]*worldInPixels.width) - (float)viewportOrigin.getX(),
                                (segment[1]*worldInPixels.height) - (float)viewportOrigin.getY(),
                                (segment[2]*worldInPixels.width) - (float)viewportOrigin.getX(),
                                (segment[3]*worldInPixels.height) - (float)viewportOrigin.getY());
                        break;
                }
                itr.next();
            }
            g.draw(path);
        }
    }

    public void mouseClicked(MouseEvent evt) {
        if (this.tripEditPanel.draw.isSelected()) {
            handleDrawing(evt);
        }
    }
    
    public void mouseDragged(MouseEvent evt) {
        if (this.tripEditPanel.draw.isSelected()) {
            handleDrawing(evt);
        }
    }

    public void mouseReleased(MouseEvent evt) {
        GeneralPath drawPath = tripEditPanel.getTrip().getPath();
        if (this.tripEditPanel.draw.isSelected()  && drawPath != null) {
//                drawPath.closePath();
        }
    }

    private void handleDrawing(MouseEvent evt) {
        if (this.tripEditPanel.draw.isSelected()) {
            JXMapViewer map = tripEditPanel.getMapViewer();
            Dimension worldInPixels = map.getTileFactory().getMapSize(map.getZoom());
            Point viewportOrigin = map.getViewportBounds().getLocation();
            GeneralPath drawPath = tripEditPanel.getTrip().getPath();
            if (drawPath == null) {
                drawPath = new GeneralPath();
                drawPath.moveTo(
                        (float)((evt.getX()+viewportOrigin.getX())/worldInPixels.width),
                        (float)((evt.getY()+viewportOrigin.getY())/worldInPixels.height));
                tripEditPanel.getTrip().setPath(drawPath);
            }  else {
                drawPath.lineTo(
                        (float) ((evt.getX()+viewportOrigin.getX())/worldInPixels.width), 
                        (float) ((evt.getY()+viewportOrigin.getY())/worldInPixels.height));
            }
            this.tripEditPanel.mapViewer.repaint();
        }
    }
    
    public void mousePressed(MouseEvent e) {}
    public void mouseMoved(MouseEvent e) {}
    public void mouseEntered(MouseEvent e) {}
    public void mouseExited(MouseEvent e) {}
}