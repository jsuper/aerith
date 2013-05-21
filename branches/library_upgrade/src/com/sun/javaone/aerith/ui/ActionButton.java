package com.sun.javaone.aerith.ui;

import com.sun.javaone.aerith.g2d.GraphicsUtil;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.font.TextLayout;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.ButtonModel;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.plaf.ColorUIResource;
import javax.swing.plaf.basic.BasicButtonUI;
import org.jdesktop.animation.timing.Animator;

import org.jdesktop.animation.timing.TimingTarget;
import org.jdesktop.fuse.InjectedResource;
import org.jdesktop.fuse.ResourceInjector;

/**
 *
 * @author rbair
 * @author bpasson
 */
public class ActionButton extends JButton {

    private float shadowOffsetX;
    private float shadowOffsetY;
    
    @InjectedResource(key="Common.shadowColor")
    private Color shadowColor;
    @InjectedResource(key="Common.shadowDirection")
    private int shadowDirection;
    
    @InjectedResource()
    private Image mainButton, mainButtonPressed,
                  normalButton, normalButtonPressed, buttonHighlight;
    @InjectedResource
    private int shadowDistance;
    @InjectedResource
    private Insets sourceInsets;
    @InjectedResource
    private Dimension buttonDimension;
    @InjectedResource
    private Color buttonForeground;
    @InjectedResource
    private Font buttonFont;
    
    private float ghostValue;
    
    private boolean main = false;

    public ActionButton(String text) {
        this();
        setText(text);
    }
    
    public ActionButton(Action a) {
        this();
        setAction(a);
    }
    
    public ActionButton() {
        
        ResourceInjector.get().inject(this);
        
        computeShadow();
        
        addMouseListener(new HiglightHandler());
        
        setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        setForeground(buttonForeground);
        setFont(buttonFont);
        setContentAreaFilled(false);
        setBorderPainted(false);
        setFocusable(false);
        
        // Hacky? Hacky!
        setUI(new BasicButtonUI() {
            @Override
            public Dimension getMinimumSize(JComponent c) {
                return getPreferredSize(c);
            }
            
            @Override
            public Dimension getMaximumSize(JComponent c) {
                return getPreferredSize(c);
            }
            
            @Override
            public Dimension getPreferredSize(JComponent c) {
                Insets insets = c.getInsets();
                Dimension d = new Dimension(buttonDimension);
                d.width += insets.left + insets.right;
                d.height += insets.top + insets.bottom;
                return d;
            }
        });
    }
    
    public void setMain(boolean main) {
        boolean old = isMain();
        this.main = main;
        firePropertyChange("main", old, isMain());
    }
    
    public boolean isMain() {
        return main;
    }
    
    private void computeShadow() {
        double rads = Math.toRadians(shadowDirection);
        shadowOffsetX = (float) Math.cos(rads) * shadowDistance;
        shadowOffsetY = (float) Math.sin(rads) * shadowDistance;
    }
    
    private Image getImage(boolean armed) {
        if (isMain()) {
            return armed ? mainButtonPressed : mainButton;
        } else {
            return armed ? normalButtonPressed : normalButton;
        }
    }
    
    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
        
        ButtonModel m = getModel();
        Insets insets = getInsets();
        
        int width = getWidth() - insets.left - insets.right;
        int height = getHeight() - insets.top - insets.bottom;
        
        GraphicsUtil.tileStretchPaint(g2,this,(BufferedImage) getImage(m.isArmed()), sourceInsets);
        
        if (ghostValue > 0.0f) {
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            
            float alphaValue = ghostValue;
            Composite composite = g2.getComposite();
            if (composite instanceof AlphaComposite) {
                alphaValue *= ((AlphaComposite) composite).getAlpha();
            }
            
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER,
                    alphaValue));
            
            g2.drawImage(buttonHighlight,
                    insets.left + 2, insets.top + 2,
                    width - 4, height - 4, null);
            g2.setComposite(composite);
        }
        
        FontMetrics fm = getFontMetrics(getFont());
        TextLayout layout = new TextLayout(getText(),
                getFont(),
                g2.getFontRenderContext());
        Rectangle2D bounds = layout.getBounds();
        
        int x = (int) (getWidth() - insets.left - insets.right -
                bounds.getWidth()) / 2;
        //x -= 2;
        int y = (getHeight() - insets.top - insets.bottom -
                 fm.getMaxAscent() - fm.getMaxDescent()) / 2;
        y += fm.getAscent() - 1;
        
        if (m.isArmed()) {
            x += 1;
            y += 1;
        }
        
        g2.setColor(shadowColor);
        layout.draw(g2,
                x + (int) Math.ceil(shadowOffsetX),
                y + (int) Math.ceil(shadowOffsetY));
        g2.setColor(getForeground());
        layout.draw(g2, x, y);
    }
    
    private final class HiglightHandler extends MouseAdapter {
        private Animator animator;
        
        @Override
        public void mouseEntered(MouseEvent e) {
            if (animator != null && animator.isRunning()) {
                animator.stop();
            }
            animator = new Animator(300, new AnimateGhost());
            animator.setDirection(Animator.Direction.FORWARD);
            animator.setEndBehavior(Animator.EndBehavior.HOLD);
            animator.setResolution((int)1000/30);
            animator.start();
        }
        
        @Override
        public void mouseExited(MouseEvent e) {
            if (animator != null && animator.isRunning()) {
                animator.stop();
            }
            animator = new Animator(300, new AnimateGhost());
            animator.setInitialFraction(1.0f);
            animator.setDirection(Animator.Direction.BACKWARD);
            animator.setEndBehavior(Animator.EndBehavior.HOLD);
            animator.setResolution((int)1000/30);
            animator.start();
        }
    }
    
    private final class AnimateGhost implements TimingTarget {        
        AnimateGhost() {
        }
        
        public void timingEvent(float fraction) {
            ghostValue = fraction;            
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
