package com.sun.javaone.aerith.ui;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.imageio.ImageIO;
import javax.swing.SwingUtilities;

import com.aetrion.flickr.people.User;
import com.aetrion.flickr.photos.Photo;
import com.aetrion.flickr.photos.PhotoList;
import com.aetrion.flickr.photosets.Photoset;
import com.aetrion.flickr.photosets.PhotosetsInterface;
import com.sun.javaone.aerith.g2d.LinearGradientTypeLoader;
import com.sun.javaone.aerith.model.DataManager;
import com.sun.javaone.aerith.model.DataType;
import com.sun.javaone.aerith.model.FlickrService;
import com.sun.javaone.aerith.model.Trip;
import com.sun.javaone.aerith.model.flickr.Catalog;
import com.sun.javaone.aerith.util.Bundles;
import org.jdesktop.fuse.ResourceInjector;
import org.jdesktop.fuse.TypeLoaderFactory;
import org.jdesktop.fuse.swing.SwingModule;
import org.jdesktop.swingx.util.SwingWorker;
import org.progx.twinkle.ui.PictureViewer;

public class TransitionManager {
    // TODO: CHANGE THIS @#!
    private static final String FLICKR_USER_ID = "romainguy";

    static boolean ready = false;
    static final Object LOCK = new Object();
    
    private static MainFrame mainFrame;
    private static NavigationHeader navPanel;
    private static TransitionPanel transPanel;

    private TransitionManager() {
    }
    
    /**
     * Creates the main application frame.  Should be called from the
     * EDT only.
     */
    public static MainFrame createMainFrame() {
        ResourceInjector.addModule(new SwingModule());
        ResourceInjector.get().load(MainFrame.class,
                                    "/resources/" +
                                    DataType.PHOTOS.toString().toLowerCase() +
                                    ".uitheme");
        TypeLoaderFactory.addTypeLoader(new LinearGradientTypeLoader());
        
        navPanel = new NavigationHeader();
        transPanel = new TransitionPanel(navPanel);
        mainFrame = new MainFrame(transPanel);
        
        if (System.getProperty("aerith.noIntro") == null) {
            showIntroduction();
        } else {
            System.out.println("[ATHENA] Introduction disabled");
        }

        return mainFrame;
    }
    
    static MainFrame getMainFrame() {
        return mainFrame;
    }
    
    static void showTransitionPanel() {
        mainFrame.showTransitionPanel();
    }
    
    static void showIntroduction() {
        mainFrame.showIntroduction();
    }

    static void showLoginOverlay() {
        showLoginOverlay(false);
    }
    
    static void showLoginOverlay(boolean visible) {
        mainFrame.showLoginOverlay(visible);
    }
    
    static void showWaitOverlay() {
        mainFrame.showWaitOverlay();
        String userid = mainFrame.getUserName();
        (new CatalogLoader(userid)).execute();
    }
    
    static void hideWaitOverlay() {
        mainFrame.hideWaitOverlay();
    }
    
    static void killOverlay() {
        mainFrame.killOverlay();
    }
    
    static void showMainScreen(Catalog contacts) {
        if (contacts != null) {
            LobbyPanel lobbyPanel = transPanel.getContactsPanel();
            //contactsPanel.setContacts(contacts);
            lobbyPanel.setTasks(contacts.getUsers()[0]);
            lobbyPanel.setRandomPicks(contacts.getRandomPicks());
            navPanel.clearLinks();
            navPanel.addLink(Bundles.getMessage(TransitionManager.class, "TXT_Lobby"));
        }
        transPanel.showCatalogPanel();
    }
    
    static void showAlbums(User contact) {
        if (contact != null) {
            transPanel.getAlbumsPanel().setContact(contact);
            navPanel.addLink(Bundles.getMessage(TransitionManager.class, "TXT_YourAlbums"));
        }
        transPanel.showCategoryPanel();
    }
    
    static void showTripReport() {
        navPanel.addLink("Trip Report");
        transPanel.showTripReportPanel();
    }
    
    static void showTripReport(Trip t) {
        navPanel.addLink("Trip Report");
        transPanel.showTripReportPanel(t);
    }
    
    static void showSlideshow(User user, Photoset album) {
        transPanel.getAlbumsPanel().setContact(user);
        navPanel.addLink(Bundles.getMessage(TransitionManager.class, "TXT_YourAlbums"));
        showSlideshow(album);
    }
    
    static void showSlideshow(Photoset album) {
        navPanel.addLink(album.getTitle());
        transPanel.resetSlideshowPanel();
        transPanel.showSlideshowPanel();
        populateSlideshow(album);
    }
    
    private static void populateSlideshow(final Photoset album) {
        final PictureViewer viewer = transPanel.getSlideshowPanel();
        final ExecutorService service = Executors.newFixedThreadPool(4);
        final PhotosetsInterface photosetsInterface = FlickrService.getPhotosetsInterface();
        
        new Thread(new Runnable() {
            public void run() {
                PhotoList photos;
                try {
                    photos = photosetsInterface.getPhotos(album.getId());
                } catch (Exception e) {
                    return;
                }

                int perPage = photos.getPerPage();
                for (int i = 0; i < perPage; i++) {
                    Photo photo = (Photo) photos.get(i);
                    service.execute(new PictureLoader(viewer, photo));
                }
            }
        }).start();
    }
    
    public static class PictureLoader implements Runnable {
        private static boolean useLargePicture = false;
        static {
            useLargePicture = System.getProperty("athena.largePictures") != null;
            System.out.println("[ATHENA] Use large picture: " + useLargePicture);
        }
        
        private final PictureViewer viewer;
        private final Photo photo;

        public PictureLoader(PictureViewer viewer, Photo photo) {
            this.viewer = viewer;
            this.photo = photo;
        }

        public void run() {
            try {
                BufferedImage image = ImageIO.read(new URL(useLargePicture ? 
                                                           photo.getLargeUrl() :
                                                           photo.getMediumUrl()));
                if (image != null) {
                    viewer.addPicture(photo.getTitle(), image);
                }
            } catch (MalformedURLException e) {
            } catch (IOException e) {
            }
        }
    }

    private static class CatalogLoader extends SwingWorker<Catalog, Object> {
        private String userid;
        public CatalogLoader(String userId) {
            this.userid = userId;
        }
        @SuppressWarnings("unchecked")
        @Override
        public Catalog doInBackground() {
            return DataManager.get(userid);
        }
        
        @Override
        protected void done() {
            try {
                TransitionManager.showMainScreen(get());
                new Thread(new Runnable() {
                    public void run() {
                        while (!TransitionManager.ready) {
                            synchronized(LOCK) {
                                try {
                                    LOCK.wait();
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                        SwingUtilities.invokeLater(new Runnable() {
                            public void run() {
                                TransitionManager.hideWaitOverlay();
                            }
                        });
                    }
                }).start();
            } catch (Exception ignore) {
                ignore.printStackTrace();
            }
        }
    }
}
