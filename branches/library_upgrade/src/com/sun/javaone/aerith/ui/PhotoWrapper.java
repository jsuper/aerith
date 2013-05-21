package com.sun.javaone.aerith.ui;

import java.awt.image.BufferedImage;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import javax.swing.Icon;
import javax.swing.ImageIcon;

import com.aetrion.flickr.photos.Photo;
import com.sun.javaone.aerith.g2d.GraphicsUtil;

public class PhotoWrapper implements Runnable {
    //private static final int ICONSIZE_1 = 133;
    private static final int ICONSIZE_2 = 50;
    private static ExecutorService service = Executors.newCachedThreadPool(new ThreadFactory() {
        private int count = 0;

        public Thread newThread(Runnable r) {
            Thread t = new Thread(r, "wrapper-pool-" + count++);
            t.setDaemon(true);
            return t;
        }
    });
    
    private Photo flickrPhoto;
    private boolean smallSquareImageLoaded = false;
    private BufferedImage smallSquareImage = null;
    
    private boolean imageLoaded = false;
    private BufferedImage image = null;
    
    private Icon icon;
    
    private PropertyChangeSupport support;
    
    
    public PhotoWrapper(Photo photo) {
        this.support = new PropertyChangeSupport(this);
        this.setFlickrPhoto(photo);
        service.submit(this);
    }
    
    public void addPropertyChangeListener(PropertyChangeListener pcl) {
        this.support.addPropertyChangeListener(pcl);
    }
    
    public void run() {
        try {
            smallSquareImage = GraphicsUtil.toCompatibleImage(
                getFlickrPhoto().getSmallSquareImage());
            smallSquareImageLoaded = true;
            BufferedImage scaled = GraphicsUtil.createThumbnail(smallSquareImage, ICONSIZE_2);
            icon = new ImageIcon(scaled);
            
            imageLoaded = true;
            image = GraphicsUtil.toCompatibleImage(getFlickrPhoto().getSmallImage());

            support.firePropertyChange("smallSquareImageLoaded", false, true);
            //support.firePropertyChange("imageLoaded", false, true);
        } catch (Exception ex) {
        }
    }
    
    public Icon getIcon() {
        return icon;
    }
    
    public boolean isSmallSquareImageLoaded() {
        return smallSquareImageLoaded;
    }
    
    public Photo getFlickrPhoto() {
        return flickrPhoto;
    }
    
    public void setFlickrPhoto(Photo flickrPhoto) {
        this.flickrPhoto = flickrPhoto;
    }

    public BufferedImage getSmallSquareImage() {
        return smallSquareImage;
    }
    
    public BufferedImage getImage() {
        return image;
    }
    
    public boolean isImageLoaded() {
        return imageLoaded;
    }
}