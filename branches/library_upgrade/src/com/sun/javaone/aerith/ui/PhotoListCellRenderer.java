package com.sun.javaone.aerith.ui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Insets;
import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;

class PhotoListCellRenderer extends DefaultListCellRenderer {
    private final Border empty = BorderFactory.createEmptyBorder(3, 3, 5, 3);
    private final Border selection = new CompoundBorder(
        BorderFactory.createEmptyBorder(0, 0, 2, 0),
        BorderFactory.createLineBorder(Color.WHITE, 3));

    PhotoListCellRenderer() {
    }

    @Override
    public Component getListCellRendererComponent(JList list, Object value, int index,
                                                  boolean isSelected, boolean cellHasFocus) {
        JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index,
                isSelected, cellHasFocus);
        PhotoWrapper photo = (PhotoWrapper) value;
        label.setText("");
        label.setOpaque(false);
        
        if (photo.isSmallSquareImageLoaded()) {
            //BufferedImage img = photo.getSmallSquareImage();
            //img = GraphicsUtil.createThumbnail(img,100);
            label.setIcon(photo.getIcon());
            //label.setIcon(new ImageIcon(img));
        } else {
            label.setIcon(new ImageIcon(TripEditPanel.LOADING));
            //label.setText("Loading...");
        }
        
        label.setBackground(null);
        label.setBorder(isSelected ? selection : empty);
        label.setOpaque(false);

        return label;
    }
    
    @Override
    public boolean isOpaque() { 
        return false;
    }
    
    @Override
    protected void paintComponent(Graphics g) {
        Insets insets = getInsets();
        if (getIcon() != null) {
            getIcon().paintIcon(this, g, insets.left, insets.top);
        }
    }

    @Override
    public Dimension getMaximumSize() {
        return this.getPreferredSize();
    }

    @Override
    public Dimension getMinimumSize() {
        return this.getPreferredSize();
    }

    @Override
    public Dimension getPreferredSize() {
        Insets insets = getInsets();
        if (getIcon() == null) {
            return new Dimension(56, 58);
        }
        return new Dimension(getIcon().getIconWidth() + insets.left + insets.right,
            getIcon().getIconHeight() + insets.top + insets.bottom);
    }
}
