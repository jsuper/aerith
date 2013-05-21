package com.sun.javaone.aerith.model;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import javax.imageio.ImageIO;

import com.aetrion.flickr.Flickr;
import com.aetrion.flickr.FlickrException;
import com.aetrion.flickr.contacts.ContactsInterface;
import com.aetrion.flickr.favorites.FavoritesInterface;
import com.aetrion.flickr.people.PeopleInterface;
import com.aetrion.flickr.people.User;
import com.aetrion.flickr.photos.PhotosInterface;
import com.aetrion.flickr.photosets.Photoset;
import com.aetrion.flickr.photosets.PhotosetsInterface;
import com.aetrion.flickr.tags.TagsInterface;
import org.xml.sax.SAXException;

public class FlickrService {
    private static Flickr flickr = null;

    private FlickrService() {
    }

    @SuppressWarnings("static-access")
    public synchronized static Flickr getFlickr() {
        if (flickr == null) {
            flickr = new Flickr("af1e08e71047433b04fe4bcf4397c0b6");
            Flickr.tracing = false;
        }

        return flickr;
    }

    public static ContactsInterface getContactsInterface() {
        return getFlickr().getContactsInterface();
    }
    
    public static PeopleInterface getPeopleInterface() {
        return getFlickr().getPeopleInterface();
    }

    public static FavoritesInterface getFavoritesInterface() {
        return getFlickr().getFavoritesInterface();
    }

    public static PhotosetsInterface getPhotosetsInterface() {
        return getFlickr().getPhotosetsInterface();
    }

    public static PhotosInterface getPhotosInterface() {
        return getFlickr().getPhotosInterface();
    }

    public static TagsInterface getTagsInterface() {
        return getFlickr().getTagsInterface();
    }
    
    @SuppressWarnings("unchecked")
    public static Photoset[] getAlbums(User user) {
        PhotosetsInterface photosetsInterface = FlickrService.getPhotosetsInterface();
        try {
            return ((Collection<Photoset>) photosetsInterface.getList(user.getId()).getPhotosets()).toArray(new Photoset[0]);
        } catch (IOException e) {
        } catch (SAXException e) {
        } catch (FlickrException e) {
        }
        
        return new Photoset[0];
    }

    public static BufferedImage getBuddyIcon(User contact) {
        int iconServer = contact.getIconServer();
        String url = "";
        if (iconServer > 0) {
            url = "http://static.flickr.com/" + iconServer + "/buddyicons/" + contact.getId() + ".jpg";
        } else {
            url = "http://www.flickr.com/images/buddyicon.jpg";
        }

        try {
            return ImageIO.read(new URL(url));
        } catch (MalformedURLException e) {
        } catch (IOException e) {
        }
        
        return null;
    }
}
