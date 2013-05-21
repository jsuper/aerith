package com.sun.javaone.aerith.ui;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.font.FontRenderContext;
import java.awt.font.TextLayout;
import java.awt.image.BufferedImage;
import java.util.Stack;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.SwingUtilities;
import org.jdesktop.animation.timing.Animator;
import org.jdesktop.animation.timing.TimingTarget;

import org.jdesktop.fuse.InjectedResource;
import org.jdesktop.fuse.ResourceInjector;

class NavigationHeader extends JComponent {
    private final PathButtonHandler eventHandler;
    private final Stack<PathButton> buttonStack;

    ////////////////////////////////////////////////////////////////////////////
    // THEME SPECIFIC FIELDS
    ////////////////////////////////////////////////////////////////////////////
    @InjectedResource
    private Color lightColor;
    @InjectedResource
    private Color shadowColor;
    @InjectedResource
    private int preferredHeight;
    @InjectedResource
    private BufferedImage backgroundGradient;
    @InjectedResource
    private float titleAlpha;
    @InjectedResource
    private BufferedImage title;
    @InjectedResource
    private Font pathFont;
    @InjectedResource
    private Color pathColor;
    @InjectedResource
    private float pathShadowOpacity;
    @InjectedResource
    private Color pathShadowColor;
    @InjectedResource
    private int pathShadowDistance;
    @InjectedResource
    private int pathShadowDirection;
    @InjectedResource
    private BufferedImage pathSeparatorLeft;
    @InjectedResource
    private BufferedImage pathSeparatorRight;
    @InjectedResource
    private BufferedImage haloPicture;

    NavigationHeader() {
        ResourceInjector.get().inject(this);

        setLayout(new FlowLayout(FlowLayout.LEFT, 0, 0));
        add(Box.createRigidArea(new Dimension(2, 2)));

        this.eventHandler = new PathButtonHandler();
        this.buttonStack = new Stack<PathButton>();
    }

    void addLink(final String title) {
        if (buttonStack.size() > 0) {
            PathButton button = buttonStack.get(buttonStack.size() - 1);
            button.setHyperlinkCursor(true);
        }

        // REMIND: should only be called from EDT?
        PathButton pathButton = new PathButton(title);
        add(buttonStack.push(pathButton));

        revalidate();
        repaint();
    }

    void clearLinks() {
        while (buttonStack.size() > 0) {
            remove(buttonStack.pop());
        }
        revalidate();
        repaint();
    }

    private void removeLinksAbove(final PathButton pathButton) {
        while (!buttonStack.peek().equals(pathButton)) {
            remove(buttonStack.pop());
        }

        if (buttonStack.size() > 0) {
            PathButton button = buttonStack.get(buttonStack.size() - 1);
            button.setHyperlinkCursor(false);
        }

        revalidate();
        repaint();
    }

    @Override
    public Dimension getPreferredSize() {
        Dimension size = super.getPreferredSize();
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

        Graphics2D g2 = (Graphics2D) g;
        setupGraphics(g2);

        paintBackground(g2);
        paintLogo(g2);
    }

    private static void setupGraphics(final Graphics2D g2) {
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                            RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
    }

    private void paintLogo(final Graphics2D g2) {
        Composite composite = g2.getComposite();
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER,
                                                   titleAlpha));
        g2.drawImage(title, getWidth() - title.getWidth(), 0, null);
        g2.setComposite(composite);
    }

    private void paintBackground(final Graphics2D g2) {
        int height = backgroundGradient.getHeight();

        Rectangle bounds = g2.getClipBounds();
        g2.drawImage(backgroundGradient,
                     (int) bounds.getX(), 0,
                     (int) bounds.getWidth(), height,
                     null);

        g2.setColor(lightColor);
        g2.drawLine(0, height, getWidth(), height);

        g2.setColor(shadowColor);
        g2.drawLine(0, height + 1, getWidth(), height + 1);
    }

    private class PathButton extends JButton {
        private final float shadowOffsetX;
        private final float shadowOffsetY;
        private int textWidth;
        private Rectangle clickable;
        private float ghostValue = 0.0f;

        private PathButton(final String item) {
            super(item);

            setFont(pathFont);
            setFocusable(false);

            setBorderPainted(false);
            setContentAreaFilled(false);
            setFocusPainted(false);

            setMargin(new Insets(0, 0, 0, 0));

            FontMetrics metrics = getFontMetrics(pathFont);
            textWidth = SwingUtilities.computeStringWidth(metrics, getText());

            double rads = Math.toRadians(pathShadowDirection);
            shadowOffsetX = (float) Math.cos(rads) * pathShadowDistance;
            shadowOffsetY = (float) Math.sin(rads) * pathShadowDistance;

            addMouseListener(eventHandler);
            addMouseListener(new HiglightHandler());
        }

        private void setHyperlinkCursor(boolean hyperlink) {
            if (hyperlink) {
                FontMetrics metrics = getFontMetrics(pathFont);
                int textHeight = metrics.getHeight();

                int x = 10;
                if (this != buttonStack.get(0)) {
                    x += pathSeparatorRight.getWidth();
                }
                clickable = new Rectangle(x, metrics.getDescent(),
                                          textWidth, textHeight);
                HyperlinkHandler.add(this, clickable);
            } else {
                HyperlinkHandler.remove(this);
            }
        }

        @Override
        public Dimension getSize() {
            return getPreferredSize();
        }

        @Override
        public Dimension getPreferredSize() {
            int width = 20 + textWidth;
            if (this != buttonStack.peek()) {
                width += pathSeparatorLeft.getWidth();
            }
            if (this != buttonStack.get(0)) {
                width += pathSeparatorRight.getWidth();
            }

            return new Dimension(width, preferredHeight);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g;

            Composite composite = g2.getComposite();

            if (ghostValue > 0.0f && this != buttonStack.peek()) {
                int x = -5;
                if (this != buttonStack.peek()) {
                    x += pathSeparatorLeft.getWidth();
                }
                g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                                    RenderingHints.VALUE_INTERPOLATION_BILINEAR);

                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER,
                                                           ghostValue));
                g2.drawImage(haloPicture,
                             x, 0,
                             textWidth + 20, getHeight(), null);
            }

            float offset = 10.0f;
            if (this != buttonStack.get(0)) {
                offset += pathSeparatorRight.getWidth();
            }

            FontRenderContext context = g2.getFontRenderContext();
            TextLayout layout = new TextLayout(getText(), pathFont, context);

            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER,
                                                       pathShadowOpacity));
            g2.setColor(pathShadowColor);
            layout.draw(g2,
                        shadowOffsetX + offset,
                        layout.getAscent() + layout.getDescent() + shadowOffsetY);
            g2.setComposite(composite);

            g2.setColor(pathColor);
            layout.draw(g2,
                        offset, layout.getAscent() + layout.getDescent());

            if (this != buttonStack.peek()) {
                g2.drawImage(pathSeparatorLeft,
                             getWidth() - pathSeparatorLeft.getWidth(), 0,
                             null);
            }
            if (this != buttonStack.get(0)) {
                g2.drawImage(pathSeparatorRight, 0, 0, null);
            }
        }

        private final class HiglightHandler extends MouseAdapter {
            private Animator timer;
 
            @Override
            public void mouseEntered(MouseEvent e) {
                if (timer != null && timer.isRunning()) {
                    timer.stop();
                }
                timer = new Animator(300, new AnimateGhost(true));
                timer.setResolution((int)1000/30);
                timer.start();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                if (timer != null && timer.isRunning()) {
                    timer.stop();
                }
                timer = new Animator(300, new AnimateGhost(false));
                timer.setResolution((int)1000/30);
                timer.start();
            }
        }

        private final class AnimateGhost implements TimingTarget {
            private boolean forward;
            private float oldValue;

            AnimateGhost(boolean forward) {
                this.forward = forward;
                oldValue = ghostValue;
            }

            public void timingEvent(float fraction) {
                ghostValue = oldValue + fraction * (forward ? 1.0f : -1.0f);

                if (ghostValue > 1.0f) {
                    ghostValue = 1.0f;
                } else if (ghostValue < 0.0f) {
                    ghostValue = 0.0f;
                }

                repaint();
            }

            public void begin() {
            }

            public void end() {
            }
            
            public void repeat() {                
            }
        }
    }

    private class PathButtonHandler extends MouseAdapter {
        @Override
        public void mouseClicked(MouseEvent e) {
            PathButton pathButton = (PathButton) e.getSource();

            int index = buttonStack.indexOf(pathButton);
            if (index == buttonStack.size() - 1 ||
                !pathButton.clickable.contains(e.getPoint())) {
                return;
            }

            switch (index) {
                case 0:
                    TransitionManager.showMainScreen(null);
                    removeLinksAbove(pathButton);
                    break;
                case 1:
                    TransitionManager.showAlbums(null);
                    removeLinksAbove(pathButton);
                    break;
                default:
                    break;
            }
        }
    }
}