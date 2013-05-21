/*
 * Trip.java
 *
 * Created on March 31, 2006, 12:07 AM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package com.sun.javaone.aerith.model;

import java.awt.geom.GeneralPath;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;

import com.aetrion.flickr.photos.Photo;
import com.sun.javaone.aerith.ui.PhotoWrapper;
import org.jdesktop.swingx.JavaBean;
import org.jdesktop.swingx.mapviewer.DefaultWaypoint;
import org.jdesktop.swingx.mapviewer.GeoPosition;

/**
 *
 * @author rbair
 */
public class Trip extends JavaBean {
    private String name;
    private String summary;
    private String title;
    
    private List<PhotoWrapper> photos;
    private List<Waypoint> waypoints;
    private PropertyChangeListener imageListener;
    private GeneralPath path; //all values within this GP are stored between 0...1 so that
                              //I can scale according to the zoom level, and such
    
    
    /** Creates a new instance of Trip */
    public Trip() {
        photos = new ArrayList<PhotoWrapper>();
        waypoints = new ArrayList<Waypoint>();
        imageListener = new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent evt) {
                firePropertyChange("photoLoaded", false, true);
            }
        };
    }
    
    public void setPath(GeneralPath path) {
        GeneralPath old = getPath();
        this.path = path;
        firePropertyChange("path", old, getPath());
    }
    
    public GeneralPath getPath() {
        return path;
    }
    
    public void setName(String name) {
        String old = getName();
        this.name = name == null ? "Unnamed" : name;
        firePropertyChange("name", old, getName());
    }
    
    public String getName() {
        return name;
    }
    
    public void setSummary(String summary) {
        String old = getSummary();
        this.summary = summary == null ? "Enter Summary" : summary;
        firePropertyChange("summary", old, getSummary());
    }
    
    public String getSummary() {
        return summary;
    }
    
    public void setTitle(String title) {
        String old = getTitle();
        this.title = title == null ? "No Title" : title;
        firePropertyChange("title", old, getTitle());
    }
    
    public String getTitle() {
        return title;
    }
    
    public void addPhoto(int index, PhotoWrapper photo) {
        photo.addPropertyChangeListener(imageListener);
        photos.add(index, photo);
    }
    
    public void addPhoto(PhotoWrapper photo) {
        photo.addPropertyChangeListener(imageListener);
        photos.add(photo);
    }
    
    public void removePhoto(int index) {
        photos.remove(index);
    }
    
    public void removePhoto(PhotoWrapper photo) {
        photos.remove(photo);
    }
    
    public void setPhotos(List<PhotoWrapper> photos) {
        this.photos = photos == null ? new ArrayList<PhotoWrapper>() : new ArrayList<PhotoWrapper>(photos);
        firePropertyChange("photos", null, getPhotos());
    }
    
    public List<PhotoWrapper> getPhotos() {
        return new ArrayList<PhotoWrapper>(photos);
    }
    
    public int getPhotoCount() {
        return photos.size();
    }
    
    public PhotoWrapper getPhoto(int index) {
        return photos.get(index);
    }
    
    public void addWaypoint(int index, Waypoint waypoint) {
        waypoints.add(index, waypoint);
        firePropertyChange("waypoint",null,waypoint);
    }
    
    public void addWaypoint(Waypoint waypoint) {
        waypoints.add(waypoint);
        firePropertyChange("waypoint",null,waypoint);
    }
    
    public void removeWaypoint(int index) {
        waypoints.remove(index);
    }
    
    public void removeWaypoint(Waypoint waypoint) {
        waypoints.remove(waypoint);
    }
    
    public void setWaypoints(List<Waypoint> waypoints) {
        this.waypoints = waypoints == null ? new ArrayList<Waypoint>() : new ArrayList<Waypoint>(waypoints);
        firePropertyChange("waypoints", null, getWaypoints());
    }
    
    public List<Waypoint> getWaypoints() {
        return new ArrayList<Waypoint>(waypoints);
    }
    
    public int getWaypointCount() {
        return waypoints.size();
    }
    
    public Waypoint getWaypoint(int index) {
        return waypoints.get(index);
    }
    
    
    public static final class Waypoint extends DefaultWaypoint {
        private String name = "New Waypoint";
        private String title = "Click to edit";
        private String summary = "";
        private List<PhotoWrapper> photos = new ArrayList<PhotoWrapper>();
        PropertyChangeListener imageListener = new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent evt) {
                firePropertyChange("photoLoaded", false, true);
            }
        };

        public Waypoint() {
            super();
        }

        public Waypoint(GeoPosition worldCoords) {
            super(worldCoords);
        }

        public void setName(String name) {
            String old = getName();
            this.name = name == null ? "Unnamed" : name;
            firePropertyChange("name", old, getName());
        }

        public String getName() {
            return name;
        }

        public void setSummary(String summary) {
            String old = getSummary();
            this.summary = summary == null ? "Enter Summary" : summary;
            firePropertyChange("summary", old, getSummary());
        }

        public String getSummary() {
            return summary;
        }

        public void setTitle(String title) {
            String old = getTitle();
            this.title = title == null ? "No Title" : title;
            firePropertyChange("title", old, getTitle());
        }

        public String getTitle() {
            return title;
        }

        public void addPhoto(int index, PhotoWrapper photo) {
            photos.add(index, photo);
            photo.addPropertyChangeListener(imageListener);
            firePropertyChange("photo",null,photo);
        }

        public void addPhoto(PhotoWrapper photo) {
            photos.add(photo);
            photo.addPropertyChangeListener(imageListener);
            firePropertyChange("photo",null,photo);
        }

        public void removePhoto(int index) {
            photos.remove(index);
            firePropertyChange("photo",index,-1);
        }

        public void removePhoto(PhotoWrapper photo) {
            photos.remove(photo);
            firePropertyChange("photo",photo,null);
        }

        public void setPhotos(List<PhotoWrapper> photos) {
            this.photos = photos == null ? new ArrayList<PhotoWrapper>() : new ArrayList<PhotoWrapper>(photos);
            firePropertyChange("photos", null, getPhotos());
        }
    
        public List<PhotoWrapper> getPhotos() {
            return new ArrayList<PhotoWrapper>(photos);
        }
        
        public int getPhotoCount() {
            return photos.size();
        }

        public PhotoWrapper getPhoto(int index) {
            return photos.get(index);
        }
    }
    
    
    //FOR TESTING
    public static void main(String... args) {
        try {
            Trip t = new Trip();
            t.setName("My Trip Name");
            t.setSummary("My Trip Summary");
            t.setTitle("My Trip Title");
            Waypoint w1 = new Trip.Waypoint(new GeoPosition(100, 200));
            w1.setName("San Francisco");
            w1.setSummary("Here we ate pizza and ate chocolate ice cream");
            w1.setTitle("Hanging in the City");
            t.addWaypoint(w1);
            Waypoint w2 = new Trip.Waypoint(new GeoPosition(500, 350));
            w2.setName("Grand Canyon");
            w2.setSummary("Can you believe this view? Wow!");
            w2.setTitle("Ye Olde Grand Canyon");
            t.addWaypoint(w2);

            com.aetrion.flickr.photosets.PhotosetsInterface photosetsInterface = FlickrService.getPhotosetsInterface();
            com.aetrion.flickr.photos.PhotoList photos = photosetsInterface.getPhotos("72057594078268040");
            int index = 0;
            for (Object obj : photos) {
                (index % 2 == 0 ? w1 : w2).addPhoto(new PhotoWrapper((Photo)obj));
            }
            
            System.out.println(DataManager.serializeTrip(t));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
