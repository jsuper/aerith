package com.sun.javaone.aerith.ui;

import java.awt.AlphaComposite;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionAdapter;
import java.awt.font.FontRenderContext;
import java.awt.font.TextLayout;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import org.jdesktop.animation.timing.Animator;
import org.jdesktop.animation.timing.TimingTarget;
import org.jdesktop.fuse.InjectedResource;
import org.jdesktop.fuse.ResourceInjector;
import org.jdesktop.swingx.util.SwingWorker;

import com.aetrion.flickr.people.User;
import com.aetrion.flickr.photosets.Photoset;
import com.sun.javaone.aerith.g2d.Reflection;
import com.sun.javaone.aerith.model.Trip;
import com.sun.javaone.aerith.util.Bundles;
import com.sun.javaone.aerith.util.FileUtils;

class LobbyPanel extends JPanel {
    private JPanel buttonPanel;
    private AlbumSelector3D albumSelector;

    private float shadowOffsetX;
    private float shadowOffsetY;

    private boolean mouseEnter = false;

    ////////////////////////////////////////////////////////////////////////////
    // THEME SPECIFIC FIELDS
    ////////////////////////////////////////////////////////////////////////////
    @InjectedResource
    private float shadowOpacity;
    @InjectedResource
    private Color shadowColor;
    @InjectedResource
    private int shadowDistance;
    @InjectedResource
    private int shadowDirection;
    @InjectedResource
    private Font categoryFont;
    @InjectedResource
    private Font categorySmallFont;
    @InjectedResource
    private float categorySmallOpacity;
    @InjectedResource
    private Color categoryColor;
    @InjectedResource
    private Color categoryHighlightColor;

    LobbyPanel() {
        ResourceInjector.get().inject(this);

        setOpaque(false);
        setLayout(new BorderLayout());

        computeShadow();

        add(BorderLayout.NORTH, buildContactsPanel());
        add(BorderLayout.CENTER, buildTopPicksPanel());
    }

    private JPanel buildContactsPanel() {
        JPanel contacts = new JPanel(new BorderLayout());
        contacts.setOpaque(false);
        contacts.add(BorderLayout.NORTH, new BackgroundTitle(
                Bundles.getMessage(getClass(), "TXT_SelectContact")));
        buttonPanel = new JPanel();
        buttonPanel.setOpaque(false);
        contacts.add(BorderLayout.CENTER, buttonPanel);
        contacts.add(BorderLayout.WEST, Box.createHorizontalStrut(60));
        contacts.add(BorderLayout.EAST, Box.createHorizontalStrut(60));
        return contacts;
    }

    private JPanel buildTopPicksPanel() {
        JPanel randomPicks = new JPanel(new BorderLayout());
        randomPicks.setOpaque(false);
        randomPicks.add(BorderLayout.NORTH, new BackgroundTitle(
            Bundles.getMessage(getClass(), "TXT_RandomPicks")));
        albumSelector = new AlbumSelector3D();
        albumSelector.addActionListener(new ShowAlbumHandler());
        randomPicks.add(BorderLayout.CENTER, albumSelector);
        randomPicks.add(BorderLayout.WEST, Box.createHorizontalStrut(60));
        randomPicks.add(BorderLayout.EAST, Box.createHorizontalStrut(60));
        return randomPicks;
    }

    private void computeShadow() {
        double rads = Math.toRadians(shadowDirection);
        shadowOffsetX = (float) Math.cos(rads) * shadowDistance;
        shadowOffsetY = (float) Math.sin(rads) * shadowDistance;
    }

    private JButton createButton(String name, String description, String image) {
        return new TaskButton(name, description, image);
    }

    void setRandomPicks(final Photoset[] picks) {
        Thread addAlbums = new Thread(new Runnable() {
            public void run() {
                albumSelector.removeAllAlbums();
                for (Photoset photoset : picks) {
                    albumSelector.addAlbum(photoset);
                }
                synchronized (TransitionManager.LOCK) {
                    TransitionManager.ready = true;
                    TransitionManager.LOCK.notifyAll();
                }
                albumSelector.start();
            }
        });
        addAlbums.start();
    }

    void setTasks(final User mainContact) {
        buttonPanel.removeAll();
        buttonPanel.setLayout(new GridBagLayout());

        int counter = 0;
        JButton button;
        buttonPanel.add(button = createButton("View Photos", "Your photo albums", "/resources/photos/task-view-photos.png"),
                        new GridBagConstraints(counter % 2, counter / 2,
                                               1, 1,
                                               0.5, 0.5,
                                               GridBagConstraints.NORTHWEST,
                                               GridBagConstraints.HORIZONTAL,
                                               new Insets(0, 0, 0, (counter + 1) % 2 != 0 ? 30 : 0),
                                               0, 0));
        button.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                TransitionManager.showAlbums(mainContact);
            }
        });
        counter++;
        buttonPanel.add(button = createButton("Create Trip Report", "Build an interactive report", "/resources/photos/task-create-trip.png"),
                        new GridBagConstraints(counter % 2, counter / 2,
                                               1, 1,
                                               0.5, 0.5,
                                               GridBagConstraints.NORTHWEST,
                                               GridBagConstraints.HORIZONTAL,
                                               new Insets(0, 0, 0, (counter + 1) % 2 != 0 ? 30 : 0),
                                               0, 0));
        button.addActionListener(new ShowTripReportHandler());
        counter++;
        buttonPanel.add(button = createButton("View Trip Report", "Retrace your own steps", "/resources/photos/task-view-trip.png"),
                        new GridBagConstraints(counter % 2, counter / 2,
                                               1, 1,
                                               0.5, 0.5,
                                               GridBagConstraints.NORTHWEST,
                                               GridBagConstraints.HORIZONTAL,
                                               new Insets(0, 0, 0, (counter + 1) % 2 != 0 ? 30 : 0),
                                               0, 0));
        button.addActionListener(new ResumeTripReportHandler());
        counter++;
        buttonPanel.add(button = createButton("Log Out", "Return to the log in screen", "/resources/photos/task-logout.png"),
                        new GridBagConstraints(counter % 2, counter / 2,
                                               1, 1,
                                               0.5, 0.5,
                                               GridBagConstraints.NORTHWEST,
                                               GridBagConstraints.HORIZONTAL,
                                               new Insets(0, 0, 0, (counter + 1) % 2 != 0 ? 30 : 0),
                                               0, 0));
        button.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                TransitionManager.showLoginOverlay(true);
            }
        });
    }

    private class TaskButton extends JButton {
        private Dimension componentDimension = new Dimension(0, 0);
        private BufferedImage image;
        private Rectangle clickable;

        private float ghostValue = 0.0f;
        private float newFraction = 0.0f;

        private int distance_r;
        private int distance_g;
        private int distance_b;

        private int color_r;
        private int color_g;
        private int color_b;

        private final String name;
        private final String description;
        private final String imageName;

        private TaskButton(String name, String description, String imageName) {
            this.name = name;
            this.description = description;
            this.imageName = imageName;

            setFocusable(false);

            setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));

            setOpaque(false);
            setContentAreaFilled(false);
            setFocusPainted(false);
            setBorderPainted(false);

            new ImageFetcher().execute();

            color_r = categoryColor.getRed();
            color_g = categoryColor.getGreen();
            color_b = categoryColor.getBlue();

            addMouseListener(new MouseClickHandler());
            addMouseMotionListener(new GhostHandler());
            HiglightHandler higlightHandler = new HiglightHandler();
            addMouseListener(higlightHandler);
            addMouseMotionListener(higlightHandler);
        }

        @Override
        protected void fireActionPerformed(ActionEvent e) {
            // consume action performed events here
            // see MouseClickHandler instead
        }

        private class ImageFetcher extends SwingWorker {
            @Override
            protected Object doInBackground() throws Exception {
                getImage();
                return image;
            }

            @Override
            protected void done() {
                componentDimension = computeDimension();
                revalidate();
            }
        }

        private void getImage() {
            try {
                this.image = ImageIO.read(getClass().getResource(imageName));
            } catch (IOException e) {
                e.printStackTrace();
            }
            BufferedImage mask = Reflection.createGradientMask(image.getWidth(),
                                                               image.getHeight());
            this.image = Reflection.createReflectedPicture(image, mask);
        }

        private Dimension computeDimension() {
            Insets insets = getInsets();

            FontMetrics metrics = getFontMetrics(categoryFont);
            Rectangle2D bounds = metrics.getMaxCharBounds(getGraphics());
            int height = (int) bounds.getHeight() + metrics.getLeading();
            int nameWidth = SwingUtilities.computeStringWidth(metrics, name);

            metrics = getFontMetrics(categorySmallFont);
            bounds = metrics.getMaxCharBounds(getGraphics());
            height += bounds.getHeight();
            int descWidth = SwingUtilities.computeStringWidth(metrics,
                                                              description == null ? "" : description);

            int width = Math.max(nameWidth, descWidth);
            width += image.getWidth() + 10;

            clickable = new Rectangle(insets.left, insets.top /*+ 4*/,
                                      width /*+ insets.left + insets.right*/,
                                      height /*+ insets.top + insets.bottom*/);
            HyperlinkHandler.add(this, clickable);

            height = Math.max(height, image.getHeight());
            height += 4;

            return new Dimension(width + insets.left + insets.right,
                                 height + insets.top + insets.bottom);
        }

        @Override
        public Dimension getMinimumSize() {
            return getPreferredSize();
        }

        @Override
        public Dimension getPreferredSize() {
            return componentDimension;
        }

        @Override
        protected void paintComponent(Graphics g) {
            if (!isVisible()) {
                return;
            }

            Graphics2D g2 = (Graphics2D) g;
            setupGraphics(g2);

            float y = paintText(g2);
            paintImage(g2, y);
        }

        private void paintImage(Graphics2D g2, float y) {
            Insets insets = getInsets();

            if (ghostValue > 0.0f) {
                int newWidth = (int) (image.getWidth() * (1.0 + ghostValue / 2.0));
                int newHeight = (int) (image.getHeight() * (1.0 + ghostValue / 2.0));

                Composite composite = g2.getComposite();
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER,
                                                           0.7f * (1.0f - ghostValue)));
                g2.drawImage(image,
                             insets.left + (image.getWidth() - newWidth) / 2,
                             4 + (int) (y - newHeight / (5.0 / 3.0)) -
                             (image.getWidth() - newWidth) / 2,
                             newWidth, newHeight, null);
                g2.setComposite(composite);
            }

            g2.drawImage(image, null,
                         insets.left,
                         4 + (int) (y - image.getHeight() / (5.0 / 3.0)));
        }

        private float paintText(Graphics2D g2) {
            g2.setFont(categoryFont);

            Insets insets = getInsets();

            FontRenderContext context = g2.getFontRenderContext();
            TextLayout layout = new TextLayout(name,
                                               categoryFont, context);

            float x = image.getWidth() + 10.0f;
            x += insets.left;
            float y = 4.0f + layout.getAscent() - layout.getDescent();
            y += insets.top;

            g2.setColor(shadowColor);
            Composite composite = g2.getComposite();
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER,
                                                       shadowOpacity));
            layout.draw(g2, shadowOffsetX + x, shadowOffsetY + y);
            g2.setComposite(composite);

            g2.setColor(new Color(color_r, color_g, color_b));
            layout.draw(g2, x, y);
            y += layout.getDescent();

            layout = new TextLayout(description == null ? " " : description,
                                    categorySmallFont, context);
            y += layout.getAscent();
            composite = g2.getComposite();
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER,
                                                       categorySmallOpacity));
            layout.draw(g2, x, y);
            g2.setComposite(composite);

            return y;
        }

        private void setupGraphics(Graphics2D g2) {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                                RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                                RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        }

        private final class GhostHandler extends MouseMotionAdapter {
            private Animator timer;

            @Override
            public void mouseMoved(MouseEvent e) {
                if (!clickable.contains(e.getPoint()) || mouseEnter) {
                    return;
                }

                if (timer != null && timer.isRunning()) {
                    return;
                }

                distance_r = categoryHighlightColor.getRed() -
                    categoryColor.getRed();
                distance_g = categoryHighlightColor.getGreen() -
                    categoryColor.getGreen();
                distance_b = categoryHighlightColor.getBlue() -
                    categoryColor.getBlue();

                timer = new Animator(450, new AnimateGhost());
                timer.start();
            }
        }

        private final class MouseClickHandler extends MouseAdapter {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (clickable.contains(e.getPoint())) {
                    for (ActionListener l : getActionListeners()) {
                        l.actionPerformed(new ActionEvent(TaskButton.this, 462, ""));
                    }
                }
            }
        }

        private final class AnimateGhost implements TimingTarget {
            public void timingEvent(float fraction) {
                ghostValue = fraction;
                repaint();
            }

            public void begin() {
                ghostValue = 0.0f;
            }

            public void end() {
                ghostValue = 0.0f;
                repaint();
            }
            public void repeat() {            	
            }
        }

        private final class HiglightHandler extends MouseMotionAdapter implements MouseListener {
            private Animator timer;
            
            @Override
            public void mouseMoved(MouseEvent e) {
                if (clickable.contains(e.getPoint())) {
                    if (!mouseEnter) {
                        mouseEnter = true;
                        if (timer != null && timer.isRunning()) {
                            timer.stop();
                        }
                        timer = new Animator(450, new AnimateHighlight(true));
                        timer.start();
                    }
                } else if (mouseEnter) {
                    mouseEnter = false;
                    if (timer != null && timer.isRunning()) {
                        timer.stop();
                    }
                    timer = new Animator(450, new AnimateHighlight(false));
                    timer.start();
                }
            }

            public void mouseClicked(MouseEvent e) {
            }

            public void mousePressed(MouseEvent e) {
            }

            public void mouseReleased(MouseEvent e) {
            }

            public void mouseEntered(MouseEvent e) {
                mouseEnter = false;
            }

            public void mouseExited(MouseEvent e) {
            }
        }

        private final class AnimateHighlight implements TimingTarget {
            private boolean forward;
            private float oldValue;

            AnimateHighlight(boolean forward) {
                this.forward = forward;
                oldValue = newFraction;
            }

            public void repeat() {            	
            }
            
            public void timingEvent(float fraction) {
                newFraction = oldValue + fraction * (forward ? 1.0f : -1.0f);

                if (newFraction > 1.0f) {
                    newFraction = 1.0f;
                } else if (newFraction < 0.0f) {
                    newFraction = 0.0f;
                }

                color_r = (int) (categoryColor.getRed() + distance_r * newFraction);
                color_g = (int) (categoryColor.getGreen() + distance_g * newFraction);
                color_b = (int) (categoryColor.getBlue() + distance_b * newFraction);

                repaint();
            }

            public void begin() {
            }

            public void end() {
            }
        }
    }

    private class ShowAlbumHandler implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            Photoset selectedAlbum = albumSelector.getSelectedAlbum();
            TransitionManager.showSlideshow(selectedAlbum.getOwner(),
                                            selectedAlbum);
        }
    }
    private static class ShowTripReportHandler implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            TransitionManager.showTripReport();
        }
    }
    private static class ResumeTripReportHandler implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            try {
                Trip t = FileUtils.readTrip(new File("saved-trips"));
                TransitionManager.showTripReport(t);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }
}