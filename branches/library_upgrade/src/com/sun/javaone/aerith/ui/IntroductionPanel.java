package com.sun.javaone.aerith.ui;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import javax.swing.JComponent;
import javax.swing.SwingUtilities;

import com.sun.javaone.aerith.g2d.GraphicsUtil;
import java.awt.LinearGradientPaint;
import com.sun.javaone.aerith.model.DataType;
import org.jdesktop.animation.timing.Animator;
import org.jdesktop.animation.timing.Animator.EndBehavior;
import org.jdesktop.animation.timing.Animator.RepeatBehavior;
import org.jdesktop.animation.timing.TimingTarget;
import org.jdesktop.animation.timing.interpolation.PropertySetter;
import org.jdesktop.fuse.InjectedResource;
import org.jdesktop.fuse.ResourceInjector;

public class IntroductionPanel extends JComponent {
    @InjectedResource
    private LinearGradientPaint backgroundGradient;
    @InjectedResource
    private BufferedImage logo;

    private final JComponent clip;
    private float fade = 0.0f;
    private float fadeOut = 0.0f;
    private BufferedImage gradientImage;

    IntroductionPanel(final DataType type, final JComponent clip) {
        ResourceInjector.get().inject(this);
        this.clip = clip;
    }

    @Override
    public void setVisible(boolean visible) {
        super.setVisible(visible);

        if (visible) {
            startFadeIn();
        }
    }

    private void startFadeIn() {
        
        PropertySetter ps = new PropertySetter( this, "fade", 0.0f, 1.0f );        
        Animator animator = new Animator(2000, ps );
        animator.setResolution(10);
        animator.addTarget(new TimingTarget() {
            public void end() {
                startFadeOut();
            }

            public void begin() {
            }

            public void timingEvent(float f) {
            }
            
            public void repeat() {                
            }
        });
        animator.setAcceleration(0.7f);
        animator.setDeceleration(0.3f);
        animator.start();
    }

    private void startFadeOut() {
        PropertySetter ps = new PropertySetter( this, "fadeOut", 0.0f, 1.0f );
        Animator animator = new Animator(1200, ps );
        animator.setResolution(10);
        animator.addTarget(new TimingTarget() {
            public void end() {
                //TransitionManager.showWaitOverlay();
                TransitionManager.showLoginOverlay();
            }
        
            public void begin() {
            }
        
            public void timingEvent(float f) {
            }
            
            public void repeat() {                
            }
        });
        animator.setAcceleration(0.7f);
        animator.setDeceleration(0.3f);
        animator.start();
    }
    
    public void setFade(float fade) {
        this.fade  = fade;
        repaint();
    }

    public float getFade() {
        return fade;
    }
    
    public void setFadeOut(float fade) {
        this.fadeOut  = fade;
        repaint();
    }

    public float getFadeOut() {
        return fadeOut;
    }
    
    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        
        setupGraphics(g2);
        paintBackground(g2);
        paintLogo(g2);
        paintForeground(g2);
    }

    private void paintForeground(Graphics2D g2) {
        if (fadeOut > 0.0f) {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                                RenderingHints.VALUE_ANTIALIAS_OFF);
            g2.setColor(new Color(0.0f, 0.0f, 0.0f, fadeOut));
            Rectangle rect = g2.getClipBounds().intersection(getClipBounds());
            g2.fillRect(rect.x, rect.y, rect.width, rect.height);
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                                RenderingHints.VALUE_ANTIALIAS_ON);
        }
    }

    private static void setupGraphics(Graphics2D g2) {
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                            RenderingHints.VALUE_ANTIALIAS_ON);
    }

    private void paintLogo(Graphics2D g2) {
        int x = (getWidth() - logo.getWidth()) / 2;
        int y = (getHeight() - logo.getHeight()) / 2;
        
        Composite composite = g2.getComposite();
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, fade));
        g2.drawImage(logo, x, y, null);
        g2.setComposite(composite);
    }

    private Rectangle getClipBounds() {
        Point point = clip.getLocation();
        point = SwingUtilities.convertPoint(clip, point, this);
        return new Rectangle(point.x, point.y, clip.getWidth(), clip.getHeight());
    }

    private void paintBackground(Graphics2D g2) {
        Rectangle rect = getClipBounds();
        if (gradientImage == null) {
            gradientImage = GraphicsUtil.createCompatibleImage(rect.width, rect.height);
            Graphics2D g2d = (Graphics2D) gradientImage.getGraphics();
            g2d.setPaint(backgroundGradient);
            g2d.fillRect(0, 0, rect.width, rect.height);
            g2d.dispose();
        }

        g2.drawImage(gradientImage, rect.x, rect.y, null);
    }
}
