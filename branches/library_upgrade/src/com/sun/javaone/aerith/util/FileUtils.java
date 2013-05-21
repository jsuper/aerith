/*
 * FileUtils.java
 *
 * Created on March 30, 2006, 11:04 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package com.sun.javaone.aerith.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URL;
import java.nio.channels.FileChannel;
import java.util.Map;

import com.sun.javaone.aerith.model.DataManager;
import com.sun.javaone.aerith.model.Trip;

/**
 *
 * @author rb156199
 */
public class FileUtils {
    
    /** Creates a new instance of FileUtils */
    private FileUtils() {
    }
    
    public static void copyFile(File oldFile, File newFile) throws Exception {
        newFile.getParentFile().mkdirs();
        newFile.createNewFile(); //if necessary, creates the target file
        FileChannel srcChannel = new FileInputStream(oldFile).getChannel();
        FileChannel dstChannel = new FileOutputStream(newFile).getChannel();
        dstChannel.transferFrom(srcChannel, 0, srcChannel.size());
        srcChannel.close();
        dstChannel.close();
    }
    
    public static void saveTrip(File saveDir, Trip t) throws Exception {
        saveDir.mkdirs();
        File tripData = new File(saveDir.getAbsolutePath() + "/trip-data.xml");
        tripData.createNewFile();
        OutputStream out = new FileOutputStream(tripData);
        out.write(DataManager.serializeTrip(t).toString().getBytes());
        out.close();
    }
    
    public static Trip readTrip(File readDir) throws Exception {
        InputStream in = new FileInputStream(readDir.getAbsolutePath() + "/trip-data.xml");
        StringBuilder buffer = new StringBuilder();
        byte[] b = new byte[8096];
        int length = -1;
        while ((length = in.read(b)) != -1) {
            buffer.append(new String(b, 0, length));
        }
        in.close();
        return DataManager.deserializeTrip(buffer.toString());
    }
    
    public static Trip readTrip(URL urlDir) throws Exception {
        URL url = new URL(urlDir.toExternalForm() + "/trip-data.xml");
        InputStream in = url.openStream();
        StringBuilder buffer = new StringBuilder();
        byte[] b = new byte[8096];
        int length = -1;
        while ((length = in.read(b)) != -1) {
            buffer.append(new String(b, 0, length));
        }
        in.close();
        return DataManager.deserializeTrip(buffer.toString());
    }
    
    public static URI deployApplet(File deployDir, Map<String,String> variables, Trip t) throws Exception {
        //do the preview -- deploy to local directory
        InputStream in = FileUtils.class.getResourceAsStream("/resources/trip-report-template.html");

        StringBuilder buffer = new StringBuilder();
        byte[] data = new byte[1024];
        int length = -1;
        while ((length = in.read(data)) != -1) {
            buffer.append(new String(data, 0, length));
        }
        in.close();
        String html = buffer.toString();
        for (Map.Entry<String,String> entry : variables.entrySet()) {
            html = html.replaceAll("\\$\\{" + entry.getKey() + "\\}", entry.getValue());
        }

        //create the deploy directory if necessary
        deployDir.mkdirs(); //only creates it if it doesn't already exist
        String deployPath = deployDir.getAbsolutePath();

        //save the trip data
        saveTrip(deployDir, t);
        
        //save the html to disk
        File page = new File(deployPath + "/trip-report.html");
        page.createNewFile();

        FileOutputStream out = new FileOutputStream(page);
        out.write(html.getBytes());
        out.close();

        copyFile(new File("dist/lib/flickrapi-1.0a9.jar"), new File(deployPath + "/flickrapi-1.0a9.jar"));
        copyFile(new File("dist/lib/twinkle.jar"), new File(deployPath + "/twinkle.jar"));
        copyFile(new File("dist/lib/swingx-jxpanel-0.8.jar"), new File(deployPath + "/swingx-jxpanel-0.8.jar"));
        copyFile(new File("dist/lib/vecmath.jar"), new File(deployPath + "/vecmath.jar"));
        copyFile(new File("dist/lib/TimingFramework.jar"), new File(deployPath + "/TimingFramework.jar"));
        copyFile(new File("dist/lib/jl1.0.jar"), new File(deployPath + "/jl1.0.jar"));
        copyFile(new File("dist/Aerith.jar"), new File(deployPath + "/Aerith.jar"));
        copyFile(new File("dist/resources/corner1.png"), new File(deployPath + "/corner1.png"));
        copyFile(new File("dist/resources/corner2.png"), new File(deployPath + "/corner2.png"));
        copyFile(new File("dist/resources/wood.jpg"), new File(deployPath + "/wood.jpg"));
        copyFile(new File("dist/music/theme.mp3"), new File(deployPath + "/music/theme.mp3"));
        copyFile(new File("dist/saved-trips/trip-data.xml"), new File(deployPath + "/saved-trips/trip-data.xml"));
        copyFile(new File("dist/indy-map.jpg"), new File(deployPath + "/indy-map.jpg"));
        
        return page.toURI();
    }
}
