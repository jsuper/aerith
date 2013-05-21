package com.sun.javaone.aerith.ui.plaf;

import java.awt.Dimension;
import java.awt.Graphics;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.plaf.basic.BasicArrowButton;
import javax.swing.plaf.metal.MetalScrollButton;

class AerithScrollButton extends MetalScrollButton {
    private static Icon BUTTON_WEST;
    private static Icon BUTTON_EAST;
    private static Icon BUTTON_NORTH;
    private static Icon BUTTON_SOUTH;
    static {
        BUTTON_WEST = new ImageIcon(AerithScrollbarUI.class.getResource("/resources/photos/scrollbar-button-west.png"));
        BUTTON_EAST = new ImageIcon(AerithScrollbarUI.class.getResource("/resources/photos/scrollbar-button-east.png"));
        BUTTON_NORTH = new ImageIcon(AerithScrollbarUI.class.getResource("/resources/photos/scrollbar-button-north.png"));
        BUTTON_SOUTH = new ImageIcon(AerithScrollbarUI.class.getResource("/resources/photos/scrollbar-button-south.png"));
    }
    
    AerithScrollButton(int direction, int width, boolean freeStanding) {
        super(direction, width, freeStanding);
        setOpaque(false);
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
        return new Dimension(17, 17);
    }

    @Override
    public void paint(Graphics g) {
        switch (getDirection()) {
            case BasicArrowButton.WEST:
                BUTTON_WEST.paintIcon(null, g, 0, 0);
                break;
            case BasicArrowButton.EAST:
                BUTTON_EAST.paintIcon(null, g, 0, 0);
                break;
            case BasicArrowButton.NORTH:
                BUTTON_NORTH.paintIcon(null, g, 0, 0);
                break;
            case BasicArrowButton.SOUTH:
                BUTTON_SOUTH.paintIcon(null, g, 0, 0);
                break;
        }
    }
}
