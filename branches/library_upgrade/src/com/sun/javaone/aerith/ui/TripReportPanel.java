package com.sun.javaone.aerith.ui;

import java.awt.AlphaComposite;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Desktop;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JComponent;
import javax.swing.JPanel;

import com.aetrion.flickr.photos.Photo;
import com.sun.javaone.aerith.g2d.GraphicsUtil;
import com.sun.javaone.aerith.model.FlickrService;
import org.jdesktop.swingx.mapviewer.LocalResponseCache;
import com.sun.javaone.aerith.model.Trip;
import com.sun.javaone.aerith.util.FileUtils;
import org.jdesktop.fuse.InjectedResource;
import org.jdesktop.fuse.ResourceInjector;

class TripReportPanel extends JPanel {
    private Trip trip;
    private TripEditPanel editPanel;
    ////////////////////////////////////////////////////////////////////////////
    // THEME SPECIFIC FIELDS
    ////////////////////////////////////////////////////////////////////////////
    /** @noinspection UNUSED_SYMBOL*/
    @InjectedResource
    private Color websiteBackground;

    TripReportPanel() {        
        ResourceInjector.get().inject(this);
        setOpaque(false);
        setLayout(new BorderLayout());

        ActionButton previewButton = new ActionButton(new PreviewAction());
        ActionButton publishButton = new ActionButton(new PublishAction());
        publishButton.setMain(true);
        
        JPanel buttonPanel = new JPanel();
        buttonPanel.setOpaque(false);
        buttonPanel.add(new ActionButton(new SaveTripAction()));
        buttonPanel.add(previewButton);
        buttonPanel.add(publishButton);
//                try {
//                    tripReportPanel.setTrip(FileUtils.readTrip(new File("./")));
//                } catch (Exception ex) {
//                    ex.printStackTrace();
//                }
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));
        add(BorderLayout.SOUTH, buttonPanel);
        editPanel = new TripEditPanel();
        add(BorderLayout.CENTER, new RoundedPanel(editPanel));
        add(BorderLayout.WEST, Box.createHorizontalStrut(20));
        add(BorderLayout.NORTH, Box.createVerticalStrut(20));
        add(BorderLayout.EAST, Box.createHorizontalStrut(20));
    }
    
    public Trip getTrip() {
        return trip;
    }
    
    public void setTrip(Trip t) {
        if (t == null) {
            final Trip trip = new Trip();
//            Runnable r = new Runnable() {
//                public void run() {
        ///////////////FOR TESTING -- must be removed soon
                    trip.setName("My Trip Name");
                    trip.setSummary("My Trip Summary");
                    trip.setTitle("My Trip Title");

// FOR TRIP RECONSTRUCTION PURPOSE
//            Trip myTrip = null;
//            try {
//                myTrip = FileUtils.readTrip(new File("saved-trips"));
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//
//            trip.setName("Road Trip 2006");
//            trip.setSummary("A road trip in the south west.");
//            trip.setTitle("Road Trip 2006");
//            assert myTrip != null;
//            trip.setPath(myTrip.getPath());
//            trip.setWaypoints(myTrip.getWaypoints());

        ///////////////END OF TESTING
        
                    try {
                        com.aetrion.flickr.photosets.PhotosetsInterface photosetsInterface = FlickrService.getPhotosetsInterface();
                        com.aetrion.flickr.photos.PhotoList photos = photosetsInterface.getPhotos("72057594067354711");
                        for (Object obj : photos) {
                            Photo photo = (Photo)obj;
                            PhotoWrapper wrap = new PhotoWrapper(photo);
                            trip.addPhoto(wrap);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
//                }
//            };
            t = trip;
//            new Thread(r).start();
        }
        this.trip = t;
        if (editPanel != null) {
            editPanel.setTrip(t);
        }
    }

    private static class RoundedPanel extends JPanel {
        private BufferedImage cache;

        public RoundedPanel(JComponent component) {
            super(new BorderLayout());
            setOpaque(false);
            component.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
            add(component);
        }

        @Override
        protected void paintComponent(Graphics g) {
            if (cache == null || cache.getWidth() != getWidth() ||
                cache.getHeight() != getHeight()) {
                cache = new BufferedImage(getWidth(), getHeight(),
                    BufferedImage.TYPE_INT_ARGB);

                Graphics2D g2 = cache.createGraphics();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                                    RenderingHints.VALUE_ANTIALIAS_ON);

                RoundRectangle2D rect = new RoundRectangle2D.Double(
                    0, 0, getWidth(), getHeight(), 12, 12);
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.5f));
                g2.setColor(Color.WHITE);
                g2.fill(rect);
            }

            g.drawImage(cache, 0, 0, null);
        }
    }
    
    private final class SaveTripAction extends AbstractAction {
        private SaveTripAction() {
            super("Save");
        }
        public void actionPerformed(ActionEvent evt) {
            try {
                FileUtils.saveTrip(new File("saved-trips"), trip);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }
    
    /**
     * Action for previewing the planned trip in the browser
     */
    private final class PreviewAction extends AbstractAction {
        private PreviewAction() {
            super("Preview");
        }

        public void actionPerformed(ActionEvent e) {
            try {
                File previewDir = new File("preview");
                URI uri = FileUtils.deployApplet(previewDir,
                        prepareDeploymentVariables(), trip);
                
                Desktop.getDesktop().browse(uri);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }
    
    private final class PublishAction extends AbstractAction {
        private PublishAction() {
            super("Publish");
        }

        public void actionPerformed(ActionEvent e) {
            try {
                Map<String,String> variables = prepareDeploymentVariables();
                
                String homeDir = System.getProperty("user.home");
                File deployDir = new File(homeDir + "/My Website/" + variables.get("TripReportTitle"));
                try {
                    FileUtils.deployApplet(deployDir, variables, trip);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
                
                URL url = new URL("http://localhost/" + makeUrlSafe(variables.get("TripReportTitle") + "/trip-report.html"));
                Desktop.getDesktop().browse(url.toURI());
                
                StringBuilder mail = new StringBuilder();
                mail.append("mailto:yourContact@domain.com?");
                mail.append("Content-Type=text/html");
                mail.append("&subject=")
                        .append(makeUrlSafe(variables.get("TripReportTitle")));
                mail.append("&body=")
                        .append(makeUrlSafe("<html><body>"))
                        .append(makeUrlSafe("<h2>"))
                        .append(makeUrlSafe(variables.get("TripReportTitle")))
                        .append(makeUrlSafe("</h2>"))
                        .append(makeUrlSafe("<a href=\""))
                        .append(makeUrlSafe(url.toString()))
                        .append(makeUrlSafe("\">"))
                        .append(makeUrlSafe(
                                "Go see the photos of my last trip!"))
                        .append(makeUrlSafe("</a></body></html>"));
                Desktop.getDesktop().mail(new URI(mail.toString()));
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }
    
    public static String makeUrlSafe(String input) throws UnsupportedEncodingException {
        return URLEncoder.encode(input, "UTF-8").replaceAll("\\+", "%20");
    }
    
    public Map<String,String> prepareDeploymentVariables() {
        Map<String,String> variables = new HashMap<String,String>();
        variables.put("TripReportTitle", "Freakin' Awesome US Tour");
        variables.put("BackgroundColor", GraphicsUtil.getColorHexString(websiteBackground));
        return variables;
    }
    
    public static void main(String[] args) {
        LocalResponseCache.installResponseCache();
        MainFrame frame = TransitionManager.createMainFrame();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
        TransitionManager.showTripReport();
    }
    
}
