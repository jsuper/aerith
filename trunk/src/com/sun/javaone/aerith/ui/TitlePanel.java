package com.sun.javaone.aerith.ui;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import javax.swing.AbstractAction;
import javax.swing.Box;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.SwingUtilities;
import javax.swing.event.MouseInputListener;

import org.jdesktop.fuse.InjectedResource;
import org.jdesktop.fuse.ResourceInjector;

class TitlePanel extends JComponent {
    private JButton closeButton;
    private JButton iconifyButton;

    ////////////////////////////////////////////////////////////////////////////
    // THEME SPECIFIC FIELDS
    ////////////////////////////////////////////////////////////////////////////
    @InjectedResource
    private int preferredHeight;
    @InjectedResource
    private Color lightColor;
    @InjectedResource
    private Color shadowColor;
    @InjectedResource
    private BufferedImage grip;
    @InjectedResource
    private BufferedImage backgroundGradient;
    @InjectedResource
    private Color inactiveLightColor;
    @InjectedResource
    private Color inactiveShadowColor;
    @InjectedResource
    private BufferedImage inactiveGrip;
    @InjectedResource
    private BufferedImage inactiveBackgroundGradient;
    @InjectedResource
    private BufferedImage close;
    @InjectedResource
    private BufferedImage closeInactive;
    @InjectedResource
    private BufferedImage closeOver;
    @InjectedResource
    private BufferedImage closePressed;
    @InjectedResource
    private BufferedImage minimize;
    @InjectedResource
    private BufferedImage minimizeInactive;
    @InjectedResource
    private BufferedImage minimizeOver;
    @InjectedResource
    private BufferedImage minimizePressed;

    TitlePanel() {
        ResourceInjector.get().inject(this);

        setLayout(new GridBagLayout());
        createButtons();
    }

    void installListeners() {
        MouseInputHandler handler = new MouseInputHandler();
        Window window = SwingUtilities.getWindowAncestor(this);
        window.addMouseListener(handler);
        window.addMouseMotionListener(handler);

        window.addWindowListener(new WindowHandler());
    }

    private void createButtons() {
        add(Box.createHorizontalGlue(),
            new GridBagConstraints(0, 0,
                                   1, 1,
                                   1.0, 1.0,
                                   GridBagConstraints.EAST,
                                   GridBagConstraints.HORIZONTAL,
                                   new Insets(0, 0, 0, 0),
                                   0, 0));

        add(iconifyButton = createButton(new IconifyAction(),
                                         minimize, minimizePressed, minimizeOver),
            new GridBagConstraints(1, 0,
                                   1, 1,
                                   0.0, 1.0,
                                   GridBagConstraints.NORTHEAST,
                                   GridBagConstraints.NONE,
                                   new Insets(1, 0, 0, 2),
                                   0, 0));
        add(closeButton = createButton(new CloseAction(),
                                       close, closePressed, closeOver),
            new GridBagConstraints(2, 0,
                                   1, 1,
                                   0.0, 1.0,
                                   GridBagConstraints.NORTHEAST,
                                   GridBagConstraints.NONE,
                                   new Insets(1, 0, 0, 2),
                                   0, 0));
    }

    private static JButton createButton(final AbstractAction action,
                                 final BufferedImage image,
                                 final Image pressedImage,
                                 final Image overImage) {
        JButton button = new JButton(action);
        button.setIcon(new ImageIcon(image));
        button.setPressedIcon(new ImageIcon(pressedImage));
        button.setRolloverIcon(new ImageIcon(overImage));
        button.setRolloverEnabled(true);
        button.setBorder(null);
        button.setBorderPainted(false);
        button.setContentAreaFilled(false);
        button.setMargin(new Insets(0, 0, 0, 0));
        button.setFocusable(false);
        button.setFocusPainted(false);
        button.setPreferredSize(new Dimension(image.getWidth(),
                                              image.getHeight()));
        return button;
    }

    private void close() {
        Window w = SwingUtilities.getWindowAncestor(this);
        w.dispatchEvent(new WindowEvent(w,
                                        WindowEvent.WINDOW_CLOSING));
    }

    private void iconify() {
        Frame frame = (Frame) SwingUtilities.getWindowAncestor(this);
        if (frame != null) {
            frame.setExtendedState(frame.getExtendedState() | Frame.ICONIFIED);
        }
    }

    @Override
    public Dimension getPreferredSize() {
        Dimension size = super.getPreferredSize();
        size.height = preferredHeight;
        return size;
    }

    @Override
    public Dimension getMinimumSize() {
        Dimension size = super.getMinimumSize();
        size.height = preferredHeight;
        return size;
    }

    @Override
    public Dimension getMaximumSize() {
        Dimension size = super.getMaximumSize();
        size.height = preferredHeight;
        return size;
    }

    @Override
    protected void paintComponent(Graphics g) {
        if (!isVisible()) {
            return;
        }

        boolean active = SwingUtilities.getWindowAncestor(this).isActive();

        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
            RenderingHints.VALUE_ANTIALIAS_OFF);

        Rectangle clip = g2.getClipBounds();
        g2.drawImage(active ? backgroundGradient : inactiveBackgroundGradient,
                     clip.x, 0, clip.width, getHeight() - 2, null);

        g2.setColor(active ? lightColor : inactiveLightColor);
        g2.drawLine(0, getHeight() - 1, getWidth(), getHeight() - 1);

        g2.setColor(active ? shadowColor : inactiveShadowColor);
        g2.drawLine(0, getHeight() - 2, getWidth(), getHeight() - 2);

        g2.drawImage(active ? grip : inactiveGrip, 0, 0, null);
    }

    private class CloseAction extends AbstractAction {
        public void actionPerformed(ActionEvent e) {
            close();
        }
    }

    private class IconifyAction extends AbstractAction {
        public void actionPerformed(ActionEvent e) {
            iconify();
        }
    }

    private class MouseInputHandler implements MouseInputListener {
        private boolean isMovingWindow;
        private int dragOffsetX;
        private int dragOffsetY;

        private static final int BORDER_DRAG_THICKNESS = 5;

        public void mousePressed(MouseEvent ev) {
            Point dragWindowOffset = ev.getPoint();
            Window w = (Window)ev.getSource();
            if (w != null) {
                w.toFront();
            }
            Point convertedDragWindowOffset = SwingUtilities.convertPoint(
                           w, dragWindowOffset, TitlePanel.this);

            Frame f = null;
            Dialog d = null;

            if (w instanceof Frame) {
                f = (Frame)w;
            } else if (w instanceof Dialog) {
                d = (Dialog)w;
            }

            int frameState = (f != null) ? f.getExtendedState() : 0;

            if (TitlePanel.this.contains(convertedDragWindowOffset)) {
                if ((f != null && ((frameState & Frame.MAXIMIZED_BOTH) == 0)
                        || (d != null))
                        && dragWindowOffset.y >= BORDER_DRAG_THICKNESS
                        && dragWindowOffset.x >= BORDER_DRAG_THICKNESS
                        && dragWindowOffset.x < w.getWidth()
                            - BORDER_DRAG_THICKNESS) {
                    isMovingWindow = true;
                    dragOffsetX = dragWindowOffset.x;
                    dragOffsetY = dragWindowOffset.y;
                }
            }
            else if (f != null && f.isResizable()
                    && ((frameState & Frame.MAXIMIZED_BOTH) == 0)
                    || (d != null && d.isResizable())) {
                dragOffsetX = dragWindowOffset.x;
                dragOffsetY = dragWindowOffset.y;
            }
        }

        public void mouseReleased(MouseEvent ev) {
            isMovingWindow = false;
        }

        public void mouseDragged(MouseEvent ev) {
            Window w = (Window)ev.getSource();

            if (isMovingWindow) {
                Point windowPt = MouseInfo.getPointerInfo().getLocation();
                windowPt.x = windowPt.x - dragOffsetX;
                windowPt.y = windowPt.y - dragOffsetY;
                w.setLocation(windowPt);
            }
        }

        public void mouseClicked(MouseEvent e) {
        }

        public void mouseEntered(MouseEvent e) {
        }

        public void mouseExited(MouseEvent e) {
        }

        public void mouseMoved(MouseEvent e) {
        }
    }

    private class WindowHandler extends WindowAdapter {
        @Override
        public void windowActivated(WindowEvent ev) {
            closeButton.setIcon(new ImageIcon(close));
            iconifyButton.setIcon(new ImageIcon(minimize));
            getRootPane().repaint();
        }

        @Override
        public void windowDeactivated(WindowEvent ev) {
            closeButton.setIcon(new ImageIcon(closeInactive));
            iconifyButton.setIcon(new ImageIcon(minimizeInactive));
            getRootPane().repaint();
        }
    }
}
