package com.sun.javaone.aerith.ui;

import java.awt.BorderLayout;
import javax.swing.JPanel;

import com.sun.animation.transitions.ScreenTransition;
import com.sun.animation.transitions.TransitionTarget;
import com.sun.javaone.aerith.model.Trip;
import org.progx.twinkle.ui.PictureViewer;

class TransitionPanel extends JPanel {
    private static final int TRANSITION_TIME_IN_MILLIS = 500;
    
    private final LobbyPanel lobbyPanel;
    private final AlbumsPanel albumsPanel;
    private PictureViewer slideshowPanel;
    private final JPanel content;
    private final TripReportPanel tripReportPanel;
    
    private enum ScreenType { MAIN, ALBUMS, SLIDESHOW, TRIP_REPORT }
    private ScreenType currentScreen;
    private ScreenType oldScreen;
    private final ScreenTransition screenTransition;

    TransitionPanel(final NavigationHeader navigationHeader) {
        super(new BorderLayout());
        setOpaque(false);

        content = new JPanel();
        content.setLayout(new BorderLayout());
        content.setOpaque(false);

        lobbyPanel = new LobbyPanel();
        albumsPanel = new AlbumsPanel();
        slideshowPanel = new PictureViewer();
        content.add(lobbyPanel, BorderLayout.CENTER);
        tripReportPanel = new TripReportPanel();

        add(navigationHeader, BorderLayout.NORTH);
        add(content, BorderLayout.CENTER);

        currentScreen = ScreenType.MAIN;
        screenTransition = new ScreenTransition(content,
                                                new ContentTransitionTarget());
    }
    
    void setContentVisible(boolean visible) {
        content.setVisible(visible);
    }
    
    void showCatalogPanel() {
        startTransition(ScreenType.MAIN);
    }
    
    void showTripReportPanel() {
        showTripReportPanel(null);
    }
    
    void showTripReportPanel(Trip t) {
        tripReportPanel.setTrip(t);
        startTransition(ScreenType.TRIP_REPORT);
    }
    
    void showCategoryPanel() {
        startTransition(ScreenType.ALBUMS);
    }
    
    void showSlideshowPanel() {
        startTransition(ScreenType.SLIDESHOW);
    }
    
    private void startTransition(ScreenType newScreen) {
        if (newScreen != currentScreen) {
            oldScreen = currentScreen;
            currentScreen = newScreen;
            screenTransition.startTransition(TRANSITION_TIME_IN_MILLIS);
        }
    }
    
    LobbyPanel getContactsPanel() {
        return lobbyPanel;
    }
    
    AlbumsPanel getAlbumsPanel() {
        return albumsPanel;
    }
    
    PictureViewer getSlideshowPanel() {
        return slideshowPanel;
    }
    
    TripReportPanel getTripPlannerPanel() {
        return tripReportPanel;
    }

    private class ContentTransitionTarget implements TransitionTarget {
        public void resetCurrentScreen() {
            content.removeAll();
        }

        public void setupNextScreen() {
            switch(currentScreen) {
                case MAIN:
                    content.add(lobbyPanel, BorderLayout.CENTER);
                    break;
                case ALBUMS:
                    content.add(albumsPanel, BorderLayout.CENTER);
                    break;
                case TRIP_REPORT:
                    content.add(tripReportPanel, BorderLayout.CENTER);
                    break;
                case SLIDESHOW:
                    content.add(slideshowPanel, BorderLayout.CENTER);
                    break;
                default:
                    assert false;
                    break;
            }
        }

        public void transitionComplete() {
            if (oldScreen == ScreenType.SLIDESHOW) {
                slideshowPanel.dispose();
            }
        }
    }

    void resetSlideshowPanel() {
        slideshowPanel = new PictureViewer();
    }
}
