package com.sun.javaone.aerith.ui;

import java.awt.AlphaComposite;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.font.FontRenderContext;
import java.awt.font.TextLayout;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import com.sun.javaone.aerith.util.Bundles;
import org.jdesktop.animation.timing.Animator;
import org.jdesktop.animation.timing.TimingTarget;
import org.jdesktop.animation.timing.interpolation.PropertySetter;
import org.jdesktop.fuse.InjectedResource;
import org.jdesktop.fuse.ResourceInjector;

public final class WaitOverlay extends JPanel {
    private String text;
    private int iconHeight;
    private int iconWidth;
    private double loadingRotation;
    private Animator loadingTimer;
    private Animator glowTimer;
    private JComponent clip;
    private float fade = 0.0f;
    private float opacity = 1.0f;
    private float glowFactor = 0.0f;

    ////////////////////////////////////////////////////////////////////////////
    // THEME SPECIFIC FIELDS
    ////////////////////////////////////////////////////////////////////////////
    @InjectedResource
    private BufferedImage waitCircle;
    @InjectedResource
    private BufferedImage javaCup;
    @InjectedResource
    private BufferedImage javaCupGlow;
    @InjectedResource
    private Color color;
    @InjectedResource
    private Font messageFont;
    @InjectedResource
    private Color messageColor;

    WaitOverlay(JComponent clip) {
        ResourceInjector.get().inject(this);

        setLayout(new BorderLayout());
        setOpaque(false);

        this.clip = clip;
        this.text = Bundles.getMessage(getClass(), "TXT_PleaseWait");
        this.iconWidth = waitCircle.getWidth();
        this.iconHeight = waitCircle.getHeight();
    }

    @Override
    public void setVisible(boolean visible) {
        if (visible) {
            super.setVisible(visible);

            //Cycle cycle = new Cycle(1500, 1000 / 12);
           //Envelope envelope = new Envelope(TimingController.INFINITE,
                                             //0,
                                             //RepeatBehavior.FORWARD,
                                            //EndBehavior.HOLD);
            //loadingTimer = new TimingController(cycle, envelope,
            //                                    new LoadingMessenger());
            
            //loadingTimer = new Animator(1500, new LoadingMessenger());
            loadingTimer = new Animator(1500, new LoadingMessenger());
            loadingTimer.setResolution((int)1000/12);
            loadingTimer.setRepeatCount(Animator.INFINITE);
            loadingTimer.setRepeatBehavior(Animator.RepeatBehavior.LOOP);
            loadingTimer.setEndBehavior(Animator.EndBehavior.HOLD);
            loadingTimer.start();

            //cycle = new Cycle(800, 30);
            //envelope = new Envelope(TimingController.INFINITE,
            //                        0,
            //                        RepeatBehavior.REVERSE,
            //                        EndBehavior.RESET);
            
            glowTimer = new Animator(800);
            glowTimer.setRepeatCount(Animator.INFINITE);
            glowTimer.setEndBehavior(Animator.EndBehavior.RESET);
            glowTimer.setRepeatBehavior(Animator.RepeatBehavior.REVERSE);
            glowTimer.setDuration(30);
            glowTimer.addTarget( new TimingTarget() {
                public void begin(){}
                public void end(){}
                public void repeat(){}
                public void timingEvent(float f) {
                    glowFactor = f;
                    repaint();
                }
            });
            
            /*
            glowTimer = new TimingController(cycle, envelope,
                                             new TimingTarget() {
                public void begin() {
                }
                public void end() {
                }
                public void timingEvent(long l, long l0, float f) {
                    glowFactor = f;
                    repaint();
                }
            });*/
            glowTimer.start();

            startFadeIn();
        } else if (loadingTimer != null) {
            TransitionManager.showTransitionPanel();
            startFadeOut();
        }
    }

    private void startFadeIn() {
        PropertySetter ps = new PropertySetter(this, "fade", 0.0f, 1.0f );
        Animator timer = new Animator(2000,ps);
        timer.setDuration(10);
        timer.setAcceleration(0.7f);
        timer.setDeceleration(0.3f);
        timer.start();
    }

    private void startFadeOut() {        
        PropertySetter fadePs = new PropertySetter(this, "fade", 1.0f, 0.0f );
        PropertySetter opacityPs = new PropertySetter(this, "opacity", 1.0f, 0.0f );
        Animator timer = new Animator(2000,fadePs);
        timer.addTarget(opacityPs);
        timer.setDuration(10);
        timer.setAcceleration(0.7f);
        timer.setDeceleration(0.3f);
        timer.addTarget(new TimingTarget() {
            public void end() {
                WaitOverlay.super.setVisible(false);
                if (loadingTimer != null) {
                    loadingTimer.stop();
                    loadingTimer = null;
                }
                if (glowTimer != null) {
                    glowTimer.stop();
                    glowTimer = null;
                }
                TransitionManager.killOverlay();
            }
        
            public void begin() {}        
            public void timingEvent(float f) {}            
            public void repeat(){}
        });
        timer.start();
    }
    
    public void setFade(float fade) {
        this.fade  = fade;
        repaint();
    }

    public float getFade() {
        return fade;
    }

    public void setOpacity(float opacity) {
        this.opacity = opacity;
        repaint();
    }

    public float getOpacity() {
        return opacity;
    }
    
    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        setupGraphics(g2);

        paintBackground(g2);

        Composite composite = g2.getComposite();
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, fade));
        
        
        int y = (getHeight() - javaCup.getHeight()) / 3 - waitCircle.getHeight();

        Composite composite2 = g2.getComposite();
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER,
                                                   (float) (fade * glowFactor)));
        g2.drawImage(javaCupGlow,
                     (getWidth() - javaCup.getWidth()) / 2,
                     y + javaCup.getHeight() / 3, null);
        g2.setComposite(composite2);
        g2.drawImage(javaCup,
                     (getWidth() - javaCup.getWidth()) / 2,
                     y + javaCup.getHeight() / 3, null);
        float x = paintText(g2, y);
        paintIcon(g2, x, y);
        
        
        g2.setComposite(composite);
    }

    private void paintIcon(Graphics2D g2, float x, int y) {
        Rectangle rect = getClipBounds();
        Graphics2D g2d = (Graphics2D) g2.create();
        g2d.rotate(loadingRotation * 2.0 * Math.PI,
                   x + iconWidth / 2.0,
                   y + rect.getY() + rect.getHeight() / 2.0);
        g2d.drawImage(waitCircle,
                      (int) x, y + (int) (rect.getY() +
                                         (rect.getHeight() - iconHeight) / 2.0),
                      null);
    }

    private Rectangle getClipBounds() {
        Point point = clip.getLocation();
        point = SwingUtilities.convertPoint(clip, point, this);
        return new Rectangle(point.x, point.y, clip.getWidth(), clip.getHeight());
    }

    private float paintText(Graphics2D g2, int y) {
        g2.setFont(messageFont);
        FontRenderContext context = g2.getFontRenderContext();
        TextLayout layout = new TextLayout(text, messageFont, context);
        Rectangle2D bounds = layout.getBounds();
        
        Rectangle rect = getClipBounds();
        float x = rect.x + (rect.width - waitCircle.getWidth(this) -
                            (float) bounds.getWidth()) / 2.0f;
        
        g2.setColor(messageColor);
        layout.draw(g2,
                    x + iconWidth + 10,
                    y + layout.getAscent() +
                    rect.y + (rect.height - layout.getAscent() -
                              layout.getDescent()) / 2.0f);
        
        return x;
    }

    private void paintBackground(Graphics2D g2) {
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                            RenderingHints.VALUE_ANTIALIAS_OFF);
        Composite composite = g2.getComposite();
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER,
                                                   opacity));
        g2.setColor(color);
        Rectangle rect = g2.getClipBounds().intersection(getClipBounds());
        g2.fillRect(rect.x, rect.y, rect.width, rect.height);
        g2.setComposite(composite);
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                            RenderingHints.VALUE_ANTIALIAS_ON);
    }

    private static void setupGraphics(Graphics2D g2) {
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                            RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                            RenderingHints.VALUE_INTERPOLATION_BILINEAR);
    }
    
    private class LoadingMessenger implements TimingTarget {
        public void timingEvent(float fraction) {
            loadingRotation = fraction - fraction % (1.0 / 12.0);

//            if (x < 0.0f) {
//                repaint();
//            } else {
//                Rectangle rect = clip.getBounds();
//                repaint((int) x - 2, rect.y - 2 + (rect.height - iconHeight) / 2,
//                        iconWidth + 4, iconHeight + 4);
//            }
        }

        public void begin() {}
        public void end() {}        
        public void repeat() {}        
    }
}
