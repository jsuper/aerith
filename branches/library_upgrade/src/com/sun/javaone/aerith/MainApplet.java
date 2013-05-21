/*
 * MainApplet.java
 *
 * Created on March 30, 2006, 9:26 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package com.sun.javaone.aerith;

import com.sun.javaone.aerith.ui.fullscreen.FullScreenManager;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.swing.AbstractAction;
import javax.swing.ImageIcon;
import javax.swing.JApplet;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

import com.aetrion.flickr.photosets.PhotosetsInterface;
import com.sun.javaone.aerith.model.FlickrService;
import com.sun.javaone.aerith.model.Trip;
import com.sun.javaone.aerith.model.Trip.Waypoint;
import com.sun.javaone.aerith.ui.PhotoWrapper;
import com.sun.javaone.aerith.ui.TransitionManager;
import com.sun.javaone.aerith.util.FileUtils;
import java.awt.GridBagLayout;
import javax.swing.JButton;
import org.jdesktop.swingx.JXPanel;
import org.progx.twinkle.ui.PictureViewer;

/**
 *
 * @author rb156199
 */
public class MainApplet extends JApplet {
    private CardLayout layout;
    private SlideShowScreen slideshow;
    
    /**
     * Creates a new instance of MainApplet
     */
    public MainApplet() {
    }

    @Override
    public void init() {
        layout = new CardLayout();
        setLayout(layout);
        
        slideshow = new SlideShowScreen();
        add(slideshow, "slideshow");
        MainScreen main = new MainScreen();
        add(main, "main");
        layout.show(getContentPane(), "main");
    }
    
    private class MainScreen extends JXPanel {
        public MainScreen() {
            setBackground(Color.BLACK);
            
            setLayout(new GridBagLayout());
            JButton slideshowButton = new JButton(new ViewSlideshowAction());
            JButton movieButton = new JButton(new PlayMovieAction());
            
            slideshowButton.setBorderPainted(false);
            slideshowButton.setOpaque(false);
            slideshowButton.setContentAreaFilled(false);
            slideshowButton.setForeground(Color.WHITE);
            slideshowButton.setVerticalTextPosition(SwingConstants.BOTTOM);
            slideshowButton.setHorizontalTextPosition(SwingConstants.CENTER);
            slideshowButton.setFocusPainted(false);
            slideshowButton.setRolloverIcon(new ImageIcon(MainScreen.class.getResource("/resources/slideshow-rollover.png")));
            slideshowButton.setRolloverEnabled(true);
            
            movieButton.setBorderPainted(false);
            movieButton.setOpaque(false);
            movieButton.setContentAreaFilled(false);
            movieButton.setForeground(Color.WHITE);
            movieButton.setVerticalTextPosition(SwingConstants.BOTTOM);
            movieButton.setHorizontalTextPosition(SwingConstants.CENTER);
            movieButton.setFocusPainted(false);
            movieButton.setRolloverIcon(new ImageIcon(MainScreen.class.getResource("/resources/movie-rollover.png")));
            movieButton.setRolloverEnabled(true);
            
            add(slideshowButton, new GridBagConstraints(0, 0, 1, 1, 0, 0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(12, 12, 11, 11), 0, 0));
            add(movieButton, new GridBagConstraints(1, 0, 1, 1, 0, 0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(12, 12, 11, 11), 0 ,0));
        }
    }
    
    private class SlideShowScreen extends PictureViewer {
        public SlideShowScreen() {
        }

        private void run() {
            final ExecutorService service = Executors.newCachedThreadPool();
            final PhotosetsInterface photosetsInterface = FlickrService.getPhotosetsInterface();

            new Thread(new Runnable() {
                public void run() {
                    try {
                        //read the Trip from the URL
                        String url = getDocumentBase().toExternalForm();
                        Trip trip = FileUtils.readTrip(new URL(url.substring(0, url.lastIndexOf("/"))));
                        for (Waypoint wp : trip.getWaypoints()) {
                            for (PhotoWrapper photo : wp.getPhotos()) {
                                new TransitionManager.PictureLoader(SlideShowScreen.this, photo.getFlickrPhoto()).run();
                            }
                        }
                        SwingUtilities.invokeLater(new Runnable() {
                            public void run() {
                                SlideShowScreen.this.repaint();
                            }
                        });
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }).start();
        }
    }
    
    private class ViewSlideshowAction extends AbstractAction {
        public ViewSlideshowAction() {
            super("View Slideshow", new ImageIcon(ViewSlideshowAction.class.getResource("/resources/slideshow.png")));
        }
        public void actionPerformed(ActionEvent e) {
            layout.show(MainApplet.this.getContentPane(), "slideshow");
            slideshow.run();
        }
    }
    
    private class PlayMovieAction extends AbstractAction {
        public PlayMovieAction() {
            super("Play Movie", new ImageIcon(PlayMovieAction.class.getResource("/resources/movie.png")));
        }
        public void actionPerformed(ActionEvent e) {
            new Thread() {
                public void run() {
                    String url = getDocumentBase().toExternalForm();
                    try {
                        FullScreenManager.launch(new URL(url.substring(0, url.lastIndexOf("/")) + "/"));
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
            }.start();
        }
    }
}
