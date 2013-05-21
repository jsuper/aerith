/*
 * TripEditPanel.java
 *
 * Created on March 30, 2006, 8:07 PM
 */

package com.sun.javaone.aerith.ui;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DragGestureEvent;
import java.awt.dnd.DragGestureListener;
import java.awt.dnd.DragSource;
import java.awt.dnd.DragSourceDragEvent;
import java.awt.dnd.DragSourceDropEvent;
import java.awt.dnd.DragSourceEvent;
import java.awt.dnd.DragSourceListener;
import java.awt.dnd.DragSourceMotionListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.swing.AbstractListModel;
import javax.swing.ButtonGroup;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.UIManager;

import org.jdesktop.animation.timing.Animator;
import org.jdesktop.animation.timing.TimingTarget;
import org.jdesktop.animation.timing.interpolation.PropertySetter;
import org.jdesktop.swingx.JXMapViewer;
import org.jdesktop.swingx.JXPanel;
import org.jdesktop.swingx.mapviewer.DefaultTileFactory;
import org.jdesktop.swingx.mapviewer.GeoPosition;
import org.jdesktop.swingx.mapviewer.MapOverlay;
import org.jdesktop.swingx.mapviewer.TilePoint;
import org.jdesktop.swingx.mapviewer.TileProviderInfo;

import com.sun.javaone.aerith.model.Trip;
import com.sun.javaone.aerith.ui.dnd.DragAndDropLock;
import com.sun.javaone.aerith.ui.dnd.GhostGlassPane;
import com.sun.javaone.aerith.ui.plaf.AerithScrollbarUI;
import com.sun.javaone.aerith.ui.plaf.AerithSliderUI;

/**
 *
 * @author  jm158417
 */
public class TripEditPanel extends javax.swing.JPanel {
    public static Image LOADING;
    static {
        try {
            LOADING = ImageIO.read(TripEditPanel.class.getResource("/resources/photos/loading.png"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private Trip trip;
    private PhotoListModel photoListModel;
    private TripLoadingListener tripLoadingListener;

    /** Creates new form TripEditPanel */
    @SuppressWarnings("unchecked")
    public TripEditPanel() {
        trip = new Trip();
        initComponents();
        mapViewer.addPropertyChangeListener("zoom",new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent evt) {
                zoomSlider.setValue(15 - (Integer)evt.getNewValue());
            }
        });
        
        mapViewer.setLoadingImage(LOADING);
        
        
        mapViewer.setFactory(new DefaultTileFactory(new TileProviderInfo(0,7,9,
            256, true, true, // tile size is 256 and x/y orientation is normal
            "http://mapwow.com/gmap/zoom",
            "x","y","z") {
                public String getTileUrl(int zoom, TilePoint tilePoint) {
                    String url = this.baseURL + zoom + "maps/"+
                            tilePoint.getX()+"_"+tilePoint.getY()+"_"+zoom+".jpg";
                    return url;
                }
        }));
        mapViewer.setZoom(6);
        
        zoomSlider.setUI(new AerithSliderUI());

        imageList.setOpaque(false);
        imageListScrollPane.getViewport().setOpaque(false);
        imageListScrollPane.getHorizontalScrollBar().setUI(new AerithScrollbarUI());

        addFadeAdapter(jXPanel1);
        addFadeAdapter(jXPanel2);
        addFadeAdapter(jXPanel3);

        mapViewer.setAddressLocation(new GeoPosition(39.09103, -94.413269));
        mapViewer.setRecenterOnClickEnabled(false);
        //imageList.setTransferHandler(new PhotoExportHandler());

        tripLoadingListener = new TripLoadingListener();

        //setup the list models
        photoListModel = new PhotoListModel();
        imageList.setModel(photoListModel);
        imageList.setCellRenderer(new PhotoListCellRenderer());

        TripWaypointMapOverlay waypointOverlay = new TripWaypointMapOverlay(this);
        mapViewer.setTransferHandler(waypointOverlay.getImportHandler());
        mapViewer.setMapOverlay(new CompoundMapOverlay<JXMapViewer>(
                new TripPathMapOverlay(this),
                waypointOverlay));

        ButtonGroup toolGroup = new ButtonGroup();
        toolGroup.add(this.add);
        toolGroup.add(this.pan);
        toolGroup.add(this.draw);

        imageListScrollPane.setViewport(new GradientViewport(
                    new Color(144,147,155), 35, GradientViewport.Orientation.HORIZONTAL));
        imageListScrollPane.getViewport().setView(imageList);

        this.setTransferHandler(null);

        DragSource drag = DragSource.getDefaultDragSource();
        drag.createDefaultDragGestureRecognizer(this.imageList,DnDConstants.ACTION_COPY, new PhotoDragGestureListener());
        drag.addDragSourceListener(new PhotoDragSourceListener());
        drag.addDragSourceMotionListener(new PhotoDragSourceMotionListener());
    }

    private static void addFadeAdapter(JXPanel panel) {
        MouseAdapter adapter = new FadeAdapter(panel);
        addFadeAdapter(adapter, panel);
    }

    private static void addFadeAdapter(MouseAdapter a, JComponent c) {
        c.addMouseListener(a);
        for (Component child : c.getComponents()) {
            if (child instanceof JComponent) {
                addFadeAdapter(a, (JComponent) child);
            }
        }
    }

    private static class FadeAdapter extends MouseAdapter {
        private Animator enterController;
        private Animator exitController;
        private JXPanel panel;
        private float originalOpacity;

        private FadeAdapter(JXPanel panel) {
            this.panel = panel;
            this.originalOpacity = panel.getAlpha();
        }

        @Override
        public void mouseEntered(MouseEvent evt) {
            if (evt.getButton() != 0) {
                return;
            }

            if (enterController != null && enterController.isRunning()) {
                return;
            }

            if (exitController != null && exitController.isRunning()) {
                exitController.stop();
            }

            enterController = PropertySetter.createAnimator(400, panel,"alpha",panel.getAlpha(), 0.999999f);
            enterController.addTarget(new TimingTarget() {
                public void begin() {
                }
                public void end() {
                }
                public void timingEvent(float f) {
                    panel.repaint();
                }
                public void repeat() {                	
                }
            });
            enterController.setAcceleration(0.7f);
            enterController.setDeceleration(0.3f);
            enterController.start();
        }

        @Override
        public void mouseExited(MouseEvent evt) {
            Point p = (Point) evt.getLocationOnScreen().clone();
            SwingUtilities.convertPointFromScreen(p, panel.getParent());
            if (panel.getBounds().contains(p)) {
                return;
            }

            if (exitController != null && exitController.isRunning()) {
                return;
            }

            if (enterController != null && enterController.isRunning()) {
                enterController.stop();
            }
            
            exitController = PropertySetter.createAnimator(400, panel, "alpha", panel.getAlpha(), originalOpacity);
            exitController.addTarget(new TimingTarget() {
                public void begin() {
                }
                public void end() {
                }
                public void timingEvent(float f) {
                    panel.repaint();
                }
                public void repeat() {
                }
            });
            exitController.setAcceleration(0.7f);
            exitController.setDeceleration(0.3f);
            exitController.start();
        }
    }

    public JXMapViewer getMapViewer() {
        return mapViewer;
    }

    public Trip getTrip() {
        return this.trip;
    }

    public void setTrip(Trip t) {

        if (this.trip != null) {
            this.trip.removePropertyChangeListener("photoLoaded", tripLoadingListener);
        }
        Trip old = getTrip();
        this.trip = t;
        if (this.trip != null) {
            this.trip.addPropertyChangeListener("photoLoaded", tripLoadingListener);
        }

        firePropertyChange("trip", old, getTrip());

        photoListModel = new PhotoListModel();
        imageList.setModel(photoListModel);
//        SwingUtilities.invokeLater(new Runnable() {
//            public void run() {
//                photoListModel.fireContentsChanged();
//            }
//        }); -Xmx512m -Dhttp.proxyHost=webcache.central.sun.com -Dhttp.proxyPort=8080  -Dsun.java2d.d3d=true  -Dsun.java2d.accthreshold=0 -Dsun.java2d.translaccel=true -Dathena.largePictures
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc=" Generated Code ">//GEN-BEGIN:initComponents
    private void initComponents() {
        com.sun.javaone.aerith.ui.AerithPanelPainter aerithPanelPainter1;
        java.awt.GridBagConstraints gridBagConstraints;

        aerithPanelPainter1 = new com.sun.javaone.aerith.ui.AerithPanelPainter();
        imageListScrollPane = new javax.swing.JScrollPane();
        imageList = new javax.swing.JList();
        mapViewer = new org.jdesktop.swingx.JXMapViewer();
        jXPanel2 = new org.jdesktop.swingx.JXPanel();
        add = new javax.swing.JToggleButton();
        draw = new javax.swing.JToggleButton();
        pan = new javax.swing.JToggleButton();
        jXPanel1 = new org.jdesktop.swingx.JXPanel();
        zoomSlider = new javax.swing.JSlider();
        zoomIn = new javax.swing.JButton();
        zoomOut = new javax.swing.JButton();
        editContainer = new javax.swing.JPanel();
        jXPanel3 = new org.jdesktop.swingx.JXPanel();
        jLabel1 = new javax.swing.JLabel();
        findAddressField = new javax.swing.JTextField();

        setLayout(new java.awt.BorderLayout());

        setOpaque(false);
        imageListScrollPane.setBorder(javax.swing.BorderFactory.createEmptyBorder(1, 1, 6, 1));
        imageListScrollPane.setHorizontalScrollBarPolicy(javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
        imageListScrollPane.setVerticalScrollBarPolicy(javax.swing.ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
        imageListScrollPane.setOpaque(false);
        imageList.setModel(new javax.swing.AbstractListModel() {
            String[] strings = { "Photo goes here" };
            public int getSize() { return strings.length; }
            public Object getElementAt(int i) { return strings[i]; }
        });
        imageList.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        imageList.setLayoutOrientation(javax.swing.JList.HORIZONTAL_WRAP);
        imageList.setMaximumSize(new java.awt.Dimension(32767, 32767));
        imageList.setMinimumSize(new java.awt.Dimension(81, 56));
        imageList.setOpaque(false);
        imageList.setPreferredSize(null);
        imageList.setVisibleRowCount(1);
        imageListScrollPane.setViewportView(imageList);

        add(imageListScrollPane, java.awt.BorderLayout.NORTH);

        mapViewer.setLayout(new java.awt.GridBagLayout());

        mapViewer.setOpaque(false);
        mapViewer.setZoom(13);
        jXPanel2.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.CENTER, 6, 3));

        jXPanel2.setAlpha(0.6F);
        jXPanel2.setAutoscrolls(true);
        jXPanel2.setBackgroundPainter(aerithPanelPainter1);
        add.setIcon(new javax.swing.ImageIcon(getClass().getResource("/resources/photos/tool-waypoint.png")));
        add.setToolTipText("Add a waypoint");
        add.setBorder(javax.swing.BorderFactory.createEmptyBorder(2, 2, 2, 2));
        add.setContentAreaFilled(false);
        add.setFocusPainted(false);
        add.setMargin(new java.awt.Insets(2, 2, 2, 2));
        add.setPressedIcon(new javax.swing.ImageIcon(getClass().getResource("/resources/photos/tool-waypoint-pressed.png")));
        add.setRolloverIcon(new javax.swing.ImageIcon(getClass().getResource("/resources/photos/tool-waypoint-over.png")));
        add.setSelectedIcon(new javax.swing.ImageIcon(getClass().getResource("/resources/photos/tool-waypoint-pressed.png")));
        add.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                addActionPerformed(evt);
            }
        });

        jXPanel2.add(add);

        draw.setIcon(new javax.swing.ImageIcon(getClass().getResource("/resources/photos/tool-draw.png")));
        draw.setToolTipText("Draw your trip");
        draw.setBorder(null);
        draw.setBorderPainted(false);
        draw.setContentAreaFilled(false);
        draw.setFocusPainted(false);
        draw.setMargin(new java.awt.Insets(2, 2, 2, 2));
        draw.setPressedIcon(new javax.swing.ImageIcon(getClass().getResource("/resources/photos/tool-draw-pressed.png")));
        draw.setRolloverIcon(new javax.swing.ImageIcon(getClass().getResource("/resources/photos/tool-draw-over.png")));
        draw.setSelectedIcon(new javax.swing.ImageIcon(getClass().getResource("/resources/photos/tool-draw-pressed.png")));
        draw.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                drawActionPerformed(evt);
            }
        });

        jXPanel2.add(draw);

        pan.setIcon(new javax.swing.ImageIcon(getClass().getResource("/resources/photos/tool-pan.png")));
        pan.setSelected(true);
        pan.setToolTipText("Move the view");
        pan.setBorderPainted(false);
        pan.setContentAreaFilled(false);
        pan.setFocusPainted(false);
        pan.setMargin(new java.awt.Insets(2, 2, 2, 2));
        pan.setMaximumSize(new java.awt.Dimension(24, 24));
        pan.setMinimumSize(new java.awt.Dimension(24, 24));
        pan.setRolloverIcon(new javax.swing.ImageIcon(getClass().getResource("/resources/photos/tool-pan-over.png")));
        pan.setSelectedIcon(new javax.swing.ImageIcon(getClass().getResource("/resources/photos/tool-pan-pressed.png")));
        pan.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                panActionPerformed(evt);
            }
        });

        jXPanel2.add(pan);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.SOUTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(10, 10, 10, 10);
        mapViewer.add(jXPanel2, gridBagConstraints);

        jXPanel1.setLayout(new java.awt.BorderLayout());

        jXPanel1.setAlpha(0.6F);
        jXPanel1.setBackgroundPainter(aerithPanelPainter1);
        jXPanel1.setMaximumSize(new java.awt.Dimension(29, 222));
        jXPanel1.setMinimumSize(new java.awt.Dimension(29, 222));
        jXPanel1.setPreferredSize(new java.awt.Dimension(29, 222));
        zoomSlider.setMajorTickSpacing(5);
        zoomSlider.setMaximum(15);
        zoomSlider.setMinorTickSpacing(1);
        zoomSlider.setOrientation(javax.swing.JSlider.VERTICAL);
        zoomSlider.setSnapToTicks(true);
        zoomSlider.setValue(2);
        zoomSlider.setBorder(javax.swing.BorderFactory.createEmptyBorder(1, 1, 1, 1));
        zoomSlider.setFocusable(false);
        zoomSlider.setMaximumSize(new java.awt.Dimension(27, 161));
        zoomSlider.setMinimumSize(new java.awt.Dimension(27, 161));
        zoomSlider.setOpaque(false);
        zoomSlider.setPreferredSize(new java.awt.Dimension(27, 161));
        zoomSlider.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                zoomSliderStateChanged(evt);
            }
        });

        jXPanel1.add(zoomSlider, java.awt.BorderLayout.CENTER);

        zoomIn.setIcon(new javax.swing.ImageIcon(getClass().getResource("/resources/photos/zoom-in.png")));
        zoomIn.setBorder(javax.swing.BorderFactory.createEmptyBorder(2, 1, 2, 1));
        zoomIn.setBorderPainted(false);
        zoomIn.setContentAreaFilled(false);
        zoomIn.setFocusPainted(false);
        zoomIn.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        zoomIn.setIconTextGap(0);
        zoomIn.setMargin(new java.awt.Insets(0, 0, 0, 0));
        zoomIn.setPressedIcon(new javax.swing.ImageIcon(getClass().getResource("/resources/photos/zoom-in-pressed.png")));
        zoomIn.setRolloverIcon(new javax.swing.ImageIcon(getClass().getResource("/resources/photos/zoom-in-over.png")));
        zoomIn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                zoomInActionPerformed(evt);
            }
        });

        jXPanel1.add(zoomIn, java.awt.BorderLayout.NORTH);

        zoomOut.setIcon(new javax.swing.ImageIcon(getClass().getResource("/resources/photos/zoom-out.png")));
        zoomOut.setBorder(javax.swing.BorderFactory.createEmptyBorder(2, 1, 3, 1));
        zoomOut.setBorderPainted(false);
        zoomOut.setContentAreaFilled(false);
        zoomOut.setFocusPainted(false);
        zoomOut.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        zoomOut.setIconTextGap(0);
        zoomOut.setMargin(new java.awt.Insets(0, 0, 0, 0));
        zoomOut.setPressedIcon(new javax.swing.ImageIcon(getClass().getResource("/resources/photos/zoom-out-pressed.png")));
        zoomOut.setRolloverIcon(new javax.swing.ImageIcon(getClass().getResource("/resources/photos/zoom-out-over.png")));
        zoomOut.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                zoomOutActionPerformed(evt);
            }
        });

        jXPanel1.add(zoomOut, java.awt.BorderLayout.SOUTH);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(10, 10, 10, 10);
        mapViewer.add(jXPanel1, gridBagConstraints);

        editContainer.setLayout(null);

        editContainer.setOpaque(false);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        mapViewer.add(editContainer, gridBagConstraints);

        jXPanel3.setAlpha(0.6F);
        jXPanel3.setBackgroundPainter(aerithPanelPainter1);
        jXPanel3.setPreferredSize(new java.awt.Dimension(228, 38));
        jLabel1.setFont(new java.awt.Font("Tahoma", 1, 12));
        jLabel1.setForeground(new java.awt.Color(255, 255, 255));
        jLabel1.setText("Find: ");

        findAddressField.setFont(new java.awt.Font("Tahoma", 1, 11));
        findAddressField.setText("4220 Network Circle, Santa Clara, CA");
        findAddressField.setBorder(javax.swing.BorderFactory.createCompoundBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)), javax.swing.BorderFactory.createEmptyBorder(2, 2, 2, 2)));
        findAddressField.setDisabledTextColor(new java.awt.Color(255, 255, 255));
        findAddressField.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                findAddressFieldActionPerformed(evt);
            }
        });

        org.jdesktop.layout.GroupLayout jXPanel3Layout = new org.jdesktop.layout.GroupLayout(jXPanel3);
        jXPanel3.setLayout(jXPanel3Layout);
        jXPanel3Layout.setHorizontalGroup(
            jXPanel3Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jXPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .add(jLabel1)
                .add(3, 3, 3)
                .add(findAddressField, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 172, Short.MAX_VALUE)
                .addContainerGap())
        );
        jXPanel3Layout.setVerticalGroup(
            jXPanel3Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jXPanel3Layout.createSequentialGroup()
                .add(8, 8, 8)
                .add(jXPanel3Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel1)
                    .add(findAddressField, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)))
        );
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.SOUTHEAST;
        gridBagConstraints.insets = new java.awt.Insets(10, 10, 10, 10);
        mapViewer.add(jXPanel3, gridBagConstraints);

        add(mapViewer, java.awt.BorderLayout.CENTER);

    }// </editor-fold>//GEN-END:initComponents

    private void findAddressFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_findAddressFieldActionPerformed
        try {
            String[] addr = findAddressField.getText().split(",");
            GeoPosition geo = JXMapViewer.getPositionForAddress(addr[0].trim(),
                                                                addr[1].trim(),
                                                                addr[2].trim());
            mapViewer.setAddressLocation(geo);
            mapViewer.setZoom(1);
        } catch (IOException ex) {
            System.out.println(ex);
            ex.printStackTrace();
        }
    }//GEN-LAST:event_findAddressFieldActionPerformed

    @SuppressWarnings("unchecked")
    private void zoomInActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_zoomInActionPerformed
        if (zoomSlider.getValue() < zoomSlider.getMaximum()) {
            //zoomSlider.setValue(zoomSlider.getValue() + 1);
            //mapViewer.setZoomDirection(-1);

            final MapOverlay<JXMapViewer> oldOverlay = (MapOverlay<JXMapViewer>) mapViewer.getMapOverlay();
            mapViewer.setMapOverlay(new ZoomingMapOverlay(mapViewer,-1));
            Animator enterController = PropertySetter.createAnimator(500, mapViewer, "zoomScale", mapViewer.getZoomScale(),2.0f);
            enterController.addTarget(new TimingTarget() {
                public void begin() {
                }
                public void end() {
                    zoomSlider.setValue(zoomSlider.getValue() + 1);
                    mapViewer.setZoomScale(1f);
                    mapViewer.setMapOverlay(oldOverlay);
                    mapViewer.repaint();
                }
                public void timingEvent(float f) {
                    mapViewer.repaint();
                }
                public void repeat() {                	
                }
            });
            enterController.start();

            /*
            Cycle cycle = new Cycle(500, 10);
            Envelope envelope = new Envelope(1, 0, RepeatBehavior.FORWARD, EndBehavior.HOLD);
            PropertyRange fadeRange = PropertyRange.createPropertyRangeFloat("zoomScale",
                                        mapViewer.getZoomScale(), 2.0f);
            TimingController enterController = new TimingController(cycle, envelope,
                    new ObjectModifier(mapViewer, fadeRange));
            enterController.addTarget(new TimingTarget() {
                public void begin() {
                }
                public void end() {
                    zoomSlider.setValue(zoomSlider.getValue() + 1);
                    mapViewer.setZoomScale(1f);
                    mapViewer.repaint();
                }
                public void timingEvent(long l, long l0, float f) {
                    mapViewer.repaint();
                }
            });
            enterController.start();
            */
        }
    }//GEN-LAST:event_zoomInActionPerformed

    @SuppressWarnings("unchecked")
    private void zoomOutActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_zoomOutActionPerformed
        if (zoomSlider.getValue() > 0) {
            //zoomSlider.setValue(zoomSlider.getValue() - 1);
            /*
            mapViewer.setZoomDirection(+1);
            Cycle cycle = new Cycle(500, 10);
            Envelope envelope = new Envelope(1, 0, RepeatBehavior.FORWARD, EndBehavior.HOLD);
            PropertyRange fadeRange = PropertyRange.createPropertyRangeFloat("zoomScale",
                                        mapViewer.getZoomScale(), 0.5f);
            TimingController enterController = new TimingController(cycle, envelope,
                    new ObjectModifier(mapViewer, fadeRange));
            enterController.addTarget(new TimingTarget() {
                public void begin() {
                }
                public void end() {
                    zoomSlider.setValue(zoomSlider.getValue() - 1);
                    mapViewer.setZoomScale(1f);
                    mapViewer.repaint();
                }
                public void timingEvent(long l, long l0, float f) {
                    mapViewer.repaint();
                }
            });
            enterController.start();
             */
            //zoomSlider.setValue(zoomSlider.getValue() - 1);
            //mapViewer.setZoomDirection(+1);
            final MapOverlay<JXMapViewer> oldOverlay = (MapOverlay<JXMapViewer>) mapViewer.getMapOverlay();
            mapViewer.setMapOverlay(new ZoomingMapOverlay(mapViewer,+1));
            Animator enterController = PropertySetter.createAnimator(500, mapViewer, "zoomScale", mapViewer.getZoomScale(), 0.5f);
            enterController.addTarget(new TimingTarget() {
                public void begin() {
                }
                public void end() {
                    zoomSlider.setValue(zoomSlider.getValue() - 1);
                    mapViewer.setZoomScale(1f);
                    mapViewer.setMapOverlay(oldOverlay);
                    mapViewer.repaint();
                }
                public void timingEvent(float f) {
                    mapViewer.repaint();
                }
                public void repeat() {                	
                }
            });
            enterController.start();
            
        }
    }//GEN-LAST:event_zoomOutActionPerformed
    
    private void panActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_panActionPerformed
        mapViewer.setPanEnabled(pan.isSelected());
    }//GEN-LAST:event_panActionPerformed
    
    private void drawActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_drawActionPerformed
        mapViewer.setPanEnabled(pan.isSelected());
    }//GEN-LAST:event_drawActionPerformed
    
    private void addActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_addActionPerformed
        mapViewer.setPanEnabled(pan.isSelected());
    }//GEN-LAST:event_addActionPerformed
    
    private void zoomSliderStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_zoomSliderStateChanged
        mapViewer.setZoom(15 - zoomSlider.getValue());
    }//GEN-LAST:event_zoomSliderStateChanged
    
    public static void main(String[] args) throws Exception {
        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        JFrame frame = new JFrame("Map Editor Test");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        TripEditPanel panel = new TripEditPanel();
        frame.add(panel);
        frame.pack();
        frame.setSize(780,614);
        frame.setVisible(true);
    }

    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    protected javax.swing.JToggleButton add;
    javax.swing.JToggleButton draw;
    javax.swing.JPanel editContainer;
    private javax.swing.JTextField findAddressField;
    private javax.swing.JList imageList;
    private javax.swing.JScrollPane imageListScrollPane;
    private javax.swing.JLabel jLabel1;
    private org.jdesktop.swingx.JXPanel jXPanel1;
    private org.jdesktop.swingx.JXPanel jXPanel2;
    private org.jdesktop.swingx.JXPanel jXPanel3;
    org.jdesktop.swingx.JXMapViewer mapViewer;
    protected javax.swing.JToggleButton pan;
    private javax.swing.JButton zoomIn;
    private javax.swing.JButton zoomOut;
    private javax.swing.JSlider zoomSlider;
    // End of variables declaration//GEN-END:variables
    
   
    private static final class CompoundMapOverlay<T extends JXMapViewer> implements MapOverlay<T> {
        private MapOverlay<T>[] overlays;
        
        public CompoundMapOverlay(MapOverlay<T>... overlays) {
            this.overlays = overlays;
        }
        
        public void paint(Graphics2D g, T map) {
            for (MapOverlay<T> overlay : overlays) {
                overlay.paint(g, map);
            }
        }
        
        public void mouseClicked(MouseEvent e) {
            for (MapOverlay<T> overlay : overlays) {
                overlay.mouseClicked(e);
            }
        }
        
        public void mousePressed(MouseEvent e) {
            for (MapOverlay<T> overlay : overlays) {
                overlay.mousePressed(e);
            }
        }
        
        public void mouseReleased(MouseEvent e) {
            for (MapOverlay<T> overlay : overlays) {
                overlay.mouseReleased(e);
            }
        }
        
        public void mouseEntered(MouseEvent e) {
            for (MapOverlay<T> overlay : overlays) {
                overlay.mouseEntered(e);
            }
        }
        
        public void mouseExited(MouseEvent e) {
            for (MapOverlay<T> overlay : overlays) {
                overlay.mouseExited(e);
            }
        }
        
        public void mouseDragged(MouseEvent e) {
            for (MapOverlay<T> overlay : overlays) {
                overlay.mouseDragged(e);
            }
        }
        
        public void mouseMoved(MouseEvent e) {
            for (MapOverlay<T> overlay : overlays) {
                overlay.mouseMoved(e);
            }
        }
    }
    
    private class PhotoListModel extends AbstractListModel {
        public PhotoListModel() {
        }
        
        public Object getElementAt(int index) {
            return trip.getPhoto(index);
        }
        
        public int getSize() {
            return trip == null ? 0 : trip.getPhotoCount();
        }
        
        public void fireContentsChanged() {
            fireContentsChanged(this, 0, getSize());
        }
    }
    
    private final class TripLoadingListener implements PropertyChangeListener {
        public void propertyChange(PropertyChangeEvent evt) {
            photoListModel.fireContentsChanged();
        }
    }

    private class TravelBackToOrigin implements ActionListener {
        private boolean isInitialized;
        private long start;

        private Point startPoint;
        private Point endPoint;
        private GhostGlassPane glassPane;
        
        private static final double INITIAL_SPEED = 500.0;
        private static final double INITIAL_ACCELERATION = 6000.0;

        private TravelBackToOrigin(GhostGlassPane glassPane, Point start, Point end) {
            this.glassPane = glassPane;
            this.startPoint = start;
            this.endPoint = end;
            isInitialized = false;
        }
        
        public void actionPerformed(ActionEvent e) {
            if (!isInitialized) {
                isInitialized = true;
                start = System.currentTimeMillis();
            }
            
            long elapsed = System.currentTimeMillis() - start;
            double time = (double) elapsed / 1000.0;

            double a = (endPoint.y - startPoint.y) / (double) (endPoint.x - startPoint.x);
            double b = endPoint.y - a * endPoint.x;
            
            int travelX = (int) (INITIAL_ACCELERATION * time * time * 0.5 + INITIAL_SPEED * time);
            if (startPoint.x > endPoint.x) {
                travelX = -travelX;
            }
            
            int travelY = (int) ((startPoint.x + travelX) * a + b);
            int distanceX = Math.abs(startPoint.x - endPoint.x);

            if (Math.abs(travelX) >= distanceX) {
                ((Timer) e.getSource()).stop();

                glassPane.setPoint(endPoint);
                glassPane.repaint(glassPane.getRepaintRect());
                
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        glassPane.setImage(null);
                        glassPane.setVisible(false);
                    }
                });
                DragAndDropLock.setLocked(false);
                
                return;
            }
            
            glassPane.setPoint(new Point(startPoint.x + travelX,
                                         travelY));
            
            glassPane.repaint(glassPane.getRepaintRect());
        }
    }

    
    private Point dragStartPoint = null;
    private class PhotoDragGestureListener implements DragGestureListener {

        public void dragGestureRecognized(DragGestureEvent dge) {
            if (DragAndDropLock.isLocked()) {
                DragAndDropLock.setDragAndDropStarted(false);
                return;
            }
            DragAndDropLock.setLocked(true);
            DragAndDropLock.setDragAndDropStarted(true);
            dge.startDrag(Cursor.getDefaultCursor(), 
                    new PhotoWrapperTransferable((PhotoWrapper) TripEditPanel.this.imageList.getSelectedValue()));
            GhostGlassPane glassPane = new GhostGlassPane();
            ((JFrame) SwingUtilities.windowForComponent(TripEditPanel.this)).setGlassPane(glassPane);
            glassPane.setVisible(true);
            dragStartPoint = (Point) dge.getDragOrigin().clone();
            Point p = dragStartPoint;
            SwingUtilities.convertPointToScreen(p, TripEditPanel.this.imageList);
            SwingUtilities.convertPointFromScreen(p, glassPane);
            dragStartPoint = p;
            glassPane.setPoint(p);
            BufferedImage img = ((PhotoWrapper) imageList.getSelectedValue()).getSmallSquareImage();
            if (img == null) {
                return;
            }
            glassPane.setImage(img, img.getWidth());
            glassPane.setVisible(true);
            glassPane.repaint();
        }
    }

    private class PhotoDragSourceListener implements DragSourceListener {

        public void dragDropEnd(DragSourceDropEvent dsde) {
            if (!DragAndDropLock.isDragAndDropStarted()) {
                return;
            }
            DragAndDropLock.setDragAndDropStarted(false);
            GhostGlassPane glassPane = (GhostGlassPane) SwingUtilities.getRootPane(TripEditPanel.this.imageList).getGlassPane();
            Point p = (Point) dsde.getLocation().clone();
            SwingUtilities.convertPointFromScreen(p, glassPane);
            
            if (!dsde.getDropSuccess()) {
                // do failure animation back to source of the drag
                /*
                Point end = (Point) comp.getLocation().clone();
                // convert from source component to the glasspane
                SwingUtilities.convertPointToScreen(end, imageList.getParent());
                SwingUtilities.convertPointFromScreen(end, glassPane);
                end.x += comp.getWidth() / 2;
                end.y += comp.getHeight() / 2;*/
                Timer backTimer = new Timer(1000 / 60, new TravelBackToOrigin(glassPane, p, dragStartPoint));
                backTimer.start();
            } else {
                glassPane.setPoint(p);
                glassPane.repaint(glassPane.getRepaintRect());
                glassPane.setImage(null);
                glassPane.setVisible(false);
                DragAndDropLock.setLocked(false);
            }
        }

        public void dragEnter(DragSourceDragEvent dsde) {
        }

        public void dragExit(DragSourceEvent dse) {
        }

        public void dragOver(DragSourceDragEvent dsde) {
        }

        public void dropActionChanged(DragSourceDragEvent dsde) {
        }
    }

    private class PhotoDragSourceMotionListener implements DragSourceMotionListener {

        public void dragMouseMoved(DragSourceDragEvent dsde) {
            if (!DragAndDropLock.isDragAndDropStarted()) {
                return;
            }
            GhostGlassPane glassPane = (GhostGlassPane) SwingUtilities.getRootPane(TripEditPanel.this.imageList).getGlassPane();
            Point p = (Point) dsde.getLocation().clone();
            SwingUtilities.convertPointFromScreen(p, glassPane);
            glassPane.setPoint(p);
            glassPane.repaint(glassPane.getRepaintRect());
        }
    }


    private class ZoomingMapOverlay<T extends JXMapViewer> implements MapOverlay<T> {
        BufferedImage start, end;
        int direction;
        public ZoomingMapOverlay(JXMapViewer map, int direction) {
            this.direction = direction;
            start = new BufferedImage(map.getWidth(),map.getHeight(),BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2 = start.createGraphics();
            g2.setClip(new Rectangle(0,0,map.getWidth(),map.getHeight()));
            map.paintComponent(g2);
            map.setZoom(map.getZoom()+direction);
            end= new BufferedImage(map.getWidth(),map.getHeight(),BufferedImage.TYPE_INT_ARGB);
            Graphics2D g3 = end.createGraphics();
            g3.setClip(new Rectangle(0,0,map.getWidth(),map.getHeight()));
            map.paintComponent(g3);
            map.setZoom(map.getZoom()-direction);
            
        }
        public void mouseClicked(MouseEvent e) {
        }
        public void mouseDragged(MouseEvent e) {
        }
        public void mouseEntered(MouseEvent e) {
        }
        public void mouseExited(MouseEvent e) {
        }
        public void mouseMoved(MouseEvent e) {
        }
        public void mousePressed(MouseEvent e) {
        }
        public void mouseReleased(MouseEvent e) {
        }
        public void paint(Graphics2D g, JXMapViewer map) {
            //System.out.println("painting overlay at: " + map.getZoomScale());
            
            float tx = map.getWidth()/2;
            float ty = map.getHeight()/2;
            float s1 = map.getZoomScale();
            float s2 = 0.5f * map.getZoomScale();
            float a1 = s1-1;
            float a2 = 1-a1;
            if(direction == +1) {
                a2 = 2*s1 - 1;
                a1 = 1-a2;
                s2 = 2*s1;
            }
            
            Graphics2D g2 = (Graphics2D)g.create();
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            
            g2.translate(tx,ty);
            g2.scale(s1,s1);
            g2.translate(-tx,-ty);
            g2.drawImage(start,0,0,null);
            g2.translate(tx,ty);
            g2.scale(1/s1,1/s1);
            g2.translate(-tx,-ty);
            
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER,a1));
            g2.translate(tx,ty);
            g2.scale(s2,s2);
            g2.translate(-tx,-ty);
            g2.drawImage(end,0,0,null);
            
        }
    }
}
