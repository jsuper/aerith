/*
 * DragAndDropLock.java
 *
 * Created on April 3, 2006, 12:51 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package com.sun.javaone.aerith.ui.dnd;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 *
 * @author jm158417
 */
public class DragAndDropLock {
    private static AtomicBoolean locked = new AtomicBoolean(false);
    private static AtomicBoolean startedDnD = new AtomicBoolean(false);
    
    public static boolean isLocked() {
        return locked.get();
    }
    
    public static void setLocked(boolean isLocked) {
        locked.set(isLocked);
    }
    
    public static boolean isDragAndDropStarted() {
        return startedDnD.get();
    }
    
    public static void setDragAndDropStarted(boolean isLocked) {
        startedDnD.set(isLocked);
    }
}