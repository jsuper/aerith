package com.sun.javaone.aerith.ui;

import java.awt.Component;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JLabel;
import javax.swing.JList;

import com.sun.javaone.aerith.model.Trip;
import com.sun.javaone.aerith.model.Trip.Waypoint;


class TripWaypointListRenderer extends DefaultListCellRenderer {
    
    @Override
    public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
        Component c = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
        Waypoint wp = (Trip.Waypoint) value;
        JLabel label = (JLabel) c;
        StringBuffer text = new StringBuffer();
        if(wp.getName() != null) {
            text.append(wp.getName());
        } else {
            text.append("unnamed");
        }
        text.append("   ");
        text.append("("+wp.getPhotoCount()+")");
        text.append("    (X)");
        label.setText(text.toString());
        return c;
    }
}