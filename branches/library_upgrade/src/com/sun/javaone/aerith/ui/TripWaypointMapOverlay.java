package com.sun.javaone.aerith.ui;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Composite;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.datatransfer.DataFlavor;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.util.HashSet;
import java.util.Set;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.TransferHandler;

import com.sun.javaone.aerith.g2d.AnimationUtil;
import com.sun.javaone.aerith.g2d.GraphicsUtil;
import com.sun.javaone.aerith.model.Trip;
import com.sun.javaone.aerith.ui.dnd.GhostGlassPane;
import org.jdesktop.animation.timing.Animator;
import org.jdesktop.animation.timing.TimingTarget;
import org.jdesktop.animation.timing.interpolation.KeyFrames;
import org.jdesktop.animation.timing.interpolation.KeyValues;
import org.jdesktop.animation.timing.interpolation.PropertySetter;
import org.jdesktop.swingx.JXMapViewer;
import org.jdesktop.swingx.JXPanel;
import org.jdesktop.swingx.mapviewer.GeoPosition;
import org.jdesktop.swingx.mapviewer.Waypoint;
import org.jdesktop.swingx.mapviewer.WaypointMapOverlay;
import org.jdesktop.swingx.mapviewer.WaypointRenderer;
import org.jdesktop.swingx.mapviewer.animation.GeoPositionEvaluator;


final public class TripWaypointMapOverlay extends WaypointMapOverlay<JXMapViewer> implements WaypointRenderer {
    private final TripEditPanel tripEditPanel;

    private Waypoint selected = null;

    private static final Color NORMAL_COLOR = new Color(0f, 0f, 0f, .5f);
    private static final Color STROKE_COLOR = new Color(0f, 0f, 0f, .8f);
    private static final Color HOVER_COLOR = new Color(0f, 0f, 0f, .8f);
    private static final Color FONT_COLOR = Color.WHITE;

    WaypointEditPanel waypointEditPanel;
    private PhotoImportHandler importHandler;

    public TripWaypointMapOverlay(TripEditPanel tripEditPanel) {
        super();
        this.tripEditPanel = tripEditPanel;
        importHandler = new PhotoImportHandler();
        setRenderer(this);

        // create and size the waypoint edit panel
        waypointEditPanel = new WaypointEditPanel();
        JFrame frame = new JFrame();
        frame.add(waypointEditPanel);
        frame.pack();
        frame.remove(waypointEditPanel);
    }

    public PhotoImportHandler getImportHandler() {
        return importHandler;
    }

    @Override
    public Set<Waypoint> getWaypoints() {
        return new HashSet<Waypoint>(this.tripEditPanel.getTrip().getWaypoints());
    }

    public Animator createMapMoveAnimation(GeoPosition newPosition) {
        KeyValues<GeoPosition> keyValues = KeyValues.create(new GeoPositionEvaluator(),this.tripEditPanel.getMapViewer().getCenterPosition(), newPosition);
        KeyFrames keyFrames = new KeyFrames(keyValues);
        PropertySetter ps = new PropertySetter(this.tripEditPanel.getMapViewer(),"centerPosition",keyFrames);
        
        Animator mapAnimator = new Animator(400,ps);
        mapAnimator.setResolution(10);
        mapAnimator.setAcceleration(0.7f);
        mapAnimator.setDeceleration(0.3f);
        return mapAnimator;
    }

    @Override
    public void mouseClicked(MouseEvent evt) {
        if (SwingUtilities.isMiddleMouseButton(evt) ||
            (SwingUtilities.isLeftMouseButton(evt) && evt.getClickCount() == 2)) {
            Rectangle bounds = this.tripEditPanel.getMapViewer().getViewportBounds();
            double x = bounds.getX() + evt.getX();
            double y = bounds.getY() + evt.getY();
            int zoom = this.tripEditPanel.getMapViewer().getZoom();
            GeoPosition geopos = tripEditPanel.getMapViewer().getTileFactory().pixelToGeo(new Point2D.Double(x,y),zoom);
            createMapMoveAnimation(geopos).start();
            //createMapMoveAnimation(GoogleUtil.getPosition(new Point2D.Double(x, y), zoom)).start();
        } else if (this.tripEditPanel.pan.isSelected()) {
            final Trip.Waypoint wp = findWaypoint(evt.getPoint());
            if (wp != null) {
                //animate recentering the map
                Animator mapAnimator = createMapMoveAnimation(wp.getPosition());
                mapAnimator.addTarget(new TimingTarget() {
                    public void begin() {}
                    public void end() {
                        stopRolloverAnimation();
                        waypointEditPanel.setWaypoint(wp);
                        waypointEditPanel.setAlpha(0.01f);
                        tripEditPanel.editContainer.add(waypointEditPanel);
                        waypointEditPanel.setLocation(100,30);
                        tripEditPanel.editContainer.repaint();
                        AnimationUtil.createFadeInAnimation(waypointEditPanel).start();
                    }
                    public void timingEvent(float f) {}
                    public void repeat(){}
                });
                mapAnimator.start();
            }
        } else if (this.tripEditPanel.add.isSelected()) {
            //must adjust for the viewport.... this is lame and should be in JXMapViewer, I think
            Rectangle bounds = tripEditPanel.mapViewer.getViewportBounds();
            int x = (int)(evt.getPoint().getX() + bounds.getX());
            int y = (int)(evt.getPoint().getY() + bounds.getY());

            //GeoPosition p = GoogleUtil.getPosition(new Point2D.Double(x, y), tripEditPanel.mapViewer.getZoom());
            
            GeoPosition p = tripEditPanel.getMapViewer().getTileFactory().pixelToGeo(
                    new Point2D.Double(x, y), tripEditPanel.mapViewer.getZoom());
            
            this.tripEditPanel.getTrip().addWaypoint(new com.sun.javaone.aerith.model.Trip.Waypoint(p));
            this.tripEditPanel.pan.setSelected(true);
            this.tripEditPanel.mapViewer.setPanEnabled(true);
            this.tripEditPanel.mapViewer.repaint();
            mouseClicked(evt);
        }
    }

    /**
     * If the mouse hovers over a waypoint, it becomes "selected", and should be
     * rendered specially
     */
    @Override
    public void mouseMoved(MouseEvent e) {
        //is this mouse point hovering over a waypoint?
        Waypoint oldSelected = selected;
        selected = findWaypoint(e.getPoint());
        // if we just move on to a waypoint
        if(oldSelected != selected) {
            if(selected != null) {
                startRolloverAnimation();
            } else {
                fadeoutWaypoint = oldSelected;
                stopRolloverAnimation();
            }
        }
        super.mouseMoved(e);
    }

    private Waypoint fadeoutWaypoint = null;
    public float selectedFade = 0.0f;
    public float getSelectedFade() {
        return selectedFade;
    }
    public void setSelectedFade(float selectedFade) {
        this.selectedFade = selectedFade;
    }

    private Animator enterController, exitController;
    private void startRolloverAnimation() {
        if (enterController != null && enterController.isRunning()) {
            return;
        }

        if (exitController != null && exitController.isRunning()) {
            exitController.stop();
        }
        
        PropertySetter ps = new PropertySetter(this,"selectedFade", getSelectedFade(), 0.99f);
        enterController = new Animator(400,ps);
        enterController.setRepeatBehavior(Animator.RepeatBehavior.LOOP);
        enterController.setResolution(10);
        enterController.addTarget(new TimingTarget() {
            public void begin() {
            }
            public void end() {
            }
            public void timingEvent(float f) {
                tripEditPanel.mapViewer.repaint();
            }
            public void repeat(){}
        });
        enterController.setAcceleration(0.7f);
        enterController.setDeceleration(0.3f);
        enterController.start();
    }

    private void stopRolloverAnimation() {
        if (exitController != null && exitController.isRunning()) {
            return;
        }
        
        if (enterController != null && enterController.isRunning()) {
            enterController.stop();
        }

        PropertySetter ps = new PropertySetter(this,"selectedFade",getSelectedFade(),0.0f);
        exitController = new Animator(400,ps);
        exitController.setResolution(10);
        exitController.setRepeatBehavior(Animator.RepeatBehavior.LOOP);
        exitController.addTarget(new TimingTarget() {
            public void begin() {
            }
            public void end() {
                fadeoutWaypoint = null;
            }
            public void timingEvent(float f) {
                tripEditPanel.mapViewer.repaint();
            }
            public void repeat(){}
        });
        exitController.setAcceleration(0.7f);
        exitController.setDeceleration(0.3f);
        exitController.start();
    }

    private Trip.Waypoint findWaypoint(final Point screenPoint) {
        for(Trip.Waypoint w : this.tripEditPanel.getTrip().getWaypoints()) {
            //Point2D point = GoogleUtil.getBitmapCoordinate(w.getPosition(), tripEditPanel.mapViewer.getZoom());
            Point2D point = tripEditPanel.getMapViewer().getTileFactory().getBitmapCoordinate(
                    w.getPosition(), tripEditPanel.mapViewer.getZoom());
            //convert to screen coords
            Rectangle bounds = tripEditPanel.mapViewer.getViewportBounds();
            int x = (int)(point.getX() - bounds.getX());
            int y = (int)(point.getY() - bounds.getY());

            Rectangle rect = new Rectangle(x - 10, y - 10, 20, 20);

            if (rect.contains(screenPoint)) {
                //selected = w;
                //found = true;
                //break;
                return w;
            }
        }
        return null;
    }

    /** @noinspection BooleanMethodNameMustStartWithQuestion*/
    public boolean paintWaypoint(Graphics2D g, JXMapViewer map, Waypoint waypoint) {
        //Point2D center = GoogleUtil.getBitmapCoordinate(waypoint.getPosition(), map.getZoom());
        Point2D center = map.getTileFactory().getBitmapCoordinate(waypoint.getPosition(), map.getZoom());
        //must adjust for the viewport.... this is lame and should be in JXMapViewer, I think
        Rectangle bounds = map.getViewportBounds();
        int x = (int)(center.getX() - bounds.getX());
        int y = (int)(center.getY() - bounds.getY());

        g.setStroke(new BasicStroke(3f));
        g.setColor(waypoint == selected ? HOVER_COLOR : NORMAL_COLOR);
        g.fillOval(x-10,y-10,20,20);
        g.setColor(STROKE_COLOR);
        g.drawOval(x-10,y-10,20,20);


//        String number = Integer.toString(tripEditPanel.getTrip().getWaypoints().indexOf(waypoint)+1);
        Font oldFont = g.getFont();
        g.setFont(g.getFont().deriveFont(Font.BOLD, 14f));

        g.setColor(FONT_COLOR);

        //TextLayout tl = new TextLayout(number, g.getFont(), g.getFontRenderContext());
        //Rectangle2D textBounds = tl.getBounds();
        //int xx = x-10+(int)(20-textBounds.getWidth())/2;
        //int yy = y-10+(int)(20-textBounds.getHeight())/2 + (int)textBounds.getHeight();
        //g.drawRect(xx, yy, (int)textBounds.getWidth(), (int)textBounds.getHeight());
        //g.drawString(number, xx, yy);
        g.setFont(oldFont);
        return waypoint == selected || waypoint == fadeoutWaypoint;
    }


    @Override
    protected void paintWaypointSummary(Graphics2D g, JXMapViewer map, Waypoint wp) {
        Trip.Waypoint waypoint = (Trip.Waypoint) wp;
        
        
        Rectangle summaryBounds = getSummaryBounds(map, waypoint);
        g.translate(summaryBounds.x, summaryBounds.y);

        JComponent dummy = new JXPanel();//exists soley to pass to the painter
        dummy.setSize(summaryBounds.getSize());
        AerithPanelPainter ap = new AerithPanelPainter();
        Composite old_comp = g.getComposite();
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER,getSelectedFade()));
        ap.paintBackground(g, dummy);

        g.setColor(Color.WHITE);
        g.setFont(g.getFont().deriveFont(Font.BOLD, 14f));
        trimAndPaint(g, waypoint.getName(), 18, 10, 20);
        g.setFont(g.getFont().deriveFont(Font.PLAIN, 12f));
        trimAndPaint(g, waypoint.getTitle(), 23, 10, 36);
        g.setStroke(new BasicStroke(1f));

        if (waypoint.getPhotoCount() > 0) {
            try {
                BufferedImage image = waypoint.getPhoto(0).getImage();
                if (image != null) {
                    image = GraphicsUtil.createThumbnail(image, 150);
                    CroppedImageIcon icon = new CroppedImageIcon(image, 37);
                    icon.paintIcon(null, g, 10, 43);
                }
            }  catch (Exception ex) {
                ex.printStackTrace();
            }
        }

        g.setComposite(old_comp);
        g.translate(-summaryBounds.x,-summaryBounds.y);
    }

    private static void trimAndPaint(Graphics2D g, String title, int length, int x, int y) {
        if(title != null) {
            if(title.length() > length) {
                title = title.substring(0,length) + "...";
            }
            g.drawString(title,x,y);
        }
    }

    private static Rectangle getSummaryBounds(JXMapViewer map, Trip.Waypoint waypoint) {
        //Point2D center = GoogleUtil.getBitmapCoordinate(waypoint.getPosition(), map.getZoom());
        Point2D center = map.getTileFactory().getBitmapCoordinate(waypoint.getPosition(), map.getZoom());
        Rectangle bounds = map.getViewportBounds();
        int x = (int)(center.getX() - bounds.getX());
        int y = (int)(center.getY() - bounds.getY());
        return new Rectangle(
                x-55, y+20,
                170, 90
                );
    }

    private class PhotoImportHandler extends TransferHandler {
        private Animator hoverAnimator;

        private PhotoImportHandler() {
        }

        private Animator createAnimator(Component glassPane) {
            KeyFrames keyFrames = new KeyFrames(KeyValues.create(new Float[] { 1.0f, .85f }));
            PropertySetter ps = new PropertySetter(glassPane, "zoom", keyFrames );
            
            Animator hoverAnimator = new Animator(300,ps);
            hoverAnimator.setResolution(10);
            hoverAnimator.setRepeatCount(Animator.INFINITE);
            hoverAnimator.setRepeatBehavior(Animator.RepeatBehavior.REVERSE);
            hoverAnimator.setAcceleration(0.7f);
            hoverAnimator.setDeceleration(0.3f);
            return hoverAnimator;
        }
        
        @Override
        public boolean canImport(TransferHandler.TransferSupport support) {
            boolean flavorSupported = false;
            for (DataFlavor flavor : support.getDataFlavors()) {
                if (PhotoWrapperTransferable.FLICKR_FLAVOR.equals(flavor)) {
                    flavorSupported = true;
                }
            }
            if (!flavorSupported) {
                return false;
            }
            
            Point p = support.getDropLocation().getDropPoint();
            //if the p point is hovering over a waypoint...
            Trip.Waypoint wp = findWaypoint(p);
            if (wp != null && hoverAnimator == null) {
                hoverAnimator = createAnimator(SwingUtilities.getRootPane(tripEditPanel).getGlassPane());
                hoverAnimator.start();
            } else if (wp == null && hoverAnimator != null) {
                hoverAnimator.stop();
                ((GhostGlassPane)SwingUtilities.getRootPane(tripEditPanel).getGlassPane()).setZoom(1.0f);
                hoverAnimator = null;
            }
            
            return wp != null;
        }

        @Override
        public boolean importData(TransferHandler.TransferSupport support) {
            try {
                boolean retValue = false;
                for (DataFlavor flavor : support.getDataFlavors()) {
                    if (PhotoWrapperTransferable.FLICKR_FLAVOR.equals(flavor)) {
                        PhotoWrapper photo = (PhotoWrapper) support.getTransferable().getTransferData(flavor);
                        Trip.Waypoint wp = findWaypoint(support.getDropLocation().getDropPoint());
                        if (wp != null) {
                            wp.addPhoto(photo);
                            retValue = true;
                        }
                    }
                }
                
                if (hoverAnimator != null) {
                    hoverAnimator.stop();
                    ((GhostGlassPane)SwingUtilities.getRootPane(tripEditPanel).getGlassPane()).setZoom(1.0f);
                    hoverAnimator = null;
                }
                
                return retValue;
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            return false;
        }
    }

}
