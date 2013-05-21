package com.sun.javaone.aerith.model.flickr;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import com.aetrion.flickr.FlickrException;
import com.aetrion.flickr.people.PeopleInterface;
import com.aetrion.flickr.people.User;
import com.aetrion.flickr.photosets.Photoset;
import com.aetrion.flickr.photosets.PhotosetsInterface;
import com.sun.javaone.aerith.model.FlickrService;
import org.xml.sax.SAXException;

public class Catalog {
    private final List<User> users = new ArrayList<User>();
    private Photoset[] randomPicks;
    
    public void addUsers(User... list) {
        for (User user : list) {
            System.out.println("adding user: " + user);
            users.add(user);
        }
    }
    
    public User[] getUsers() {
        return users.toArray(new User[0]);
    }
    
    @SuppressWarnings("unchecked")
    public void prefetch() {
        randomPicks = new Photoset[5];
        PhotosetsInterface photosetsInterface = FlickrService.getPhotosetsInterface();
        
        try {
            User user = users.get(0);
            Collection photosets = photosetsInterface.getList(user.getId()).getPhotosets();
            List<Photoset> shuffledSets = new ArrayList<Photoset>(photosets);
            Collections.shuffle(shuffledSets);
            int i = 0;
            for (Photoset set : shuffledSets) {
                Photoset info = photosetsInterface.getInfo(set.getId());
                PeopleInterface peopleInterface = FlickrService.getPeopleInterface();
                User owner = peopleInterface.getInfo(info.getOwner().getId());
                set.setOwner(owner);
                randomPicks[i++] = set;
                if (i >= randomPicks.length) {
                    break;
                }
            }
          } catch (IOException e) {
          } catch (SAXException e) {
          } catch (FlickrException e) {
          }
    }

    public Photoset[] getRandomPicks() {
        return randomPicks;
    }
}