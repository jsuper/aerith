/*
 * PhotoWrapperTransferable.java
 *
 * Created on April 2, 2006, 5:36 AM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package com.sun.javaone.aerith.ui;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;

/**
 *
 * @author jm158417
 */
class PhotoWrapperTransferable implements Transferable {
    public static final DataFlavor FLICKR_FLAVOR = new DataFlavor(PhotoWrapper.class,"Flickr Photo");
    PhotoWrapper photo;
    PhotoWrapperTransferable(PhotoWrapper photo) {
        this.photo = photo;
    }
    public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException {
        if (!isDataFlavorSupported(flavor)) {
            throw new UnsupportedFlavorException(flavor);
        }
        return photo;
    }
    public DataFlavor[] getTransferDataFlavors() {
        return new DataFlavor[] { FLICKR_FLAVOR };
    }
    public boolean isDataFlavorSupported(DataFlavor flavor) {
        if (FLICKR_FLAVOR.equals(flavor)) {
            return true;
        }
        return false;
    }
}
