package com.sun.javaone.aerith.ui;

import java.awt.AlphaComposite;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.border.CompoundBorder;

import org.jdesktop.animation.timing.Animator;
import org.jdesktop.animation.timing.TimingTarget;
import org.jdesktop.animation.timing.interpolation.PropertySetter;
import org.jdesktop.fuse.InjectedResource;
import org.jdesktop.fuse.ResourceInjector;
import org.jdesktop.swingx.JXPanel;
import org.jdesktop.swingx.painter.Painter;

import com.sun.javaone.aerith.util.Bundles;

public class LoginOverlay extends JComponent implements ActionListener {
    @InjectedResource
    private Color hintColor, inputColor, inputBorderColor,
                  inputTextColor, inputSelectionColor,
                  inputSelectedTextColor;
    @InjectedResource
    private Font hintFont, inputFont;

    private final JComponent clip;
    private JTextField userNameInput;
    private JXPanel panel;
    private final boolean makeVisible;
    private float blackAlpha = 1.0f;

    LoginOverlay(JComponent clip, boolean makeVisible) {
        ResourceInjector.get().inject(this);
        this.clip = clip;
        this.makeVisible = makeVisible;

        panel = new JXPanel();
        panel.setBackgroundPainter(new Painter() {
            public void paint(Graphics2D arg0, JComponent arg1) {
            }
        });
        panel.setAlpha(0.001f);
        panel.setLayout(new GridBagLayout());

        int linesCount = 0;
        panel.add(Box.createVerticalStrut((int) (TransitionManager.getMainFrame().getHeight() / 2.5)),
                   new GridBagConstraints(0, linesCount++,
                                          3, 1,
                                          1.0, 0.0,
                                          GridBagConstraints.CENTER,
                                          GridBagConstraints.NONE,
                                          new Insets(0, 0, 0, 0),
                                          0, 0));
        panel.add(Box.createHorizontalStrut(200),
                  new GridBagConstraints(0, linesCount,
                                         1, 2,
                                         0.3, 0.0,
                                         GridBagConstraints.CENTER,
                                         GridBagConstraints.HORIZONTAL,
                                         new Insets(0, 0, 0, 0),
                                         0, 0));
        JLabel hint;
        panel.add(hint = new JLabel(Bundles.getMessage(getClass(), "TXT_LoginHint")),
                  new GridBagConstraints(1, linesCount,
                                         1, 1,
                                         1.0, 0.0,
                                         GridBagConstraints.LINE_START,
                                         GridBagConstraints.NONE,
                                         new Insets(0, 0, 2, 0),
                                         0, 0));
        hint.setForeground(hintColor);
        hint.setFont(hintFont);
        panel.add(Box.createHorizontalStrut(200),
                  new GridBagConstraints(2, linesCount++,
                                         1, 2,
                                         0.3, 0.0,
                                         GridBagConstraints.CENTER,
                                         GridBagConstraints.HORIZONTAL,
                                         new Insets(0, 0, 0, 0),
                                         0, 0));
        JPanel innerPanel = new JPanel(new BorderLayout());
        innerPanel.setBackground(inputColor);
        innerPanel.setBorder(new CompoundBorder(BorderFactory.createLineBorder(inputBorderColor, 2),
                                                BorderFactory.createEmptyBorder(2, 2, 2, 2)));
        innerPanel.add(userNameInput = new JTextField());
        panel.add(innerPanel,
                  new GridBagConstraints(1, linesCount++,
                                         1, 1,
                                         1.0, 0.0,
                                         GridBagConstraints.LINE_START,
                                         GridBagConstraints.HORIZONTAL,
                                         new Insets(0, 2, 6, 2),
                                         0, 0));
        userNameInput.setOpaque(false);
        userNameInput.setBorder(null);
        userNameInput.setForeground(inputTextColor);
        userNameInput.setSelectedTextColor(inputSelectedTextColor);
        userNameInput.setSelectionColor(inputSelectionColor);
        userNameInput.setCaretColor(inputSelectionColor);
        userNameInput.setFont(inputFont);

        JButton button;
        panel.add(button = new ActionButton(Bundles.getMessage(getClass(), "TXT_Login")),
                  new GridBagConstraints(1, linesCount++,
                                         1, 1,
                                         1.0, 0.0,
                                         GridBagConstraints.LINE_END,
                                         GridBagConstraints.NONE,
                                         new Insets(0, 0, 0, 0),
                                         0, 0));
        button.addActionListener(this);
        TransitionManager.getMainFrame().getRootPane().setDefaultButton(button);

        panel.add(Box.createVerticalGlue(),
                  new GridBagConstraints(0, linesCount,
                                         3, 1,
                                         1.0, 1.0,
                                         GridBagConstraints.CENTER,
                                         GridBagConstraints.VERTICAL,
                                         new Insets(0, 0, 0, 0),
                                         0, 0));
        setLayout(new BorderLayout());
        add(panel);
    }

    @Override
    public void setVisible(boolean visible) {
        if (makeVisible) {
            blackAlpha = 0.0f;
        }
        super.setVisible(visible);
        if (visible) {
            if (makeVisible) {
                startBlackFadeIn();
            } else {
                startFadeIn();
            }
        }
    }

    public void setBlackAlpha(float blackAlpha) {
        this.blackAlpha = blackAlpha;
        repaint();
    }

    public float getBlackAlpha() {
        return blackAlpha;
    }

    private void startBlackFadeIn() {
        Animator timer = PropertySetter.createAnimator(1200, this,"blackAlpha",0.0f, 1.0f);
        timer.addTarget(new TimingTarget() {
            public void repeat() {
            	
            }
        	public void end() {
                startFadeIn();
            }

            public void begin() {
            }

            public void timingEvent(float arg2) {
            }
        });
        timer.setAcceleration(0.7f);
        timer.setDeceleration(0.3f);
        timer.start();
    }

    private void startFadeIn() {
        Animator timer = PropertySetter.createAnimator(2000, panel,"alpha", 0.001f, 1.0f);
        timer.addTarget(new TimingTarget() {
            public void end() {
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        userNameInput.requestFocusInWindow();
                    }
                });
            }

            public void begin() {
            }

            public void timingEvent(float arg2) {
            }
            public void repeat() {            	
            }
        });
        timer.setAcceleration(0.7f);
        timer.setDeceleration(0.3f);
        timer.start();
    }

    private void startFadeOut() {        
        Animator timer = PropertySetter.createAnimator(1200,panel,"alpha", 1.0f,0.0f);
        timer.addTarget(new TimingTarget() {
            public void end() {
                TransitionManager.showWaitOverlay();
            }
        
            public void begin() {
            }
        
            public void timingEvent(float arg2) {
            }
            public void repeat() {            	
            }
        });
        timer.setAcceleration(0.7f);
        timer.setDeceleration(0.3f);
        timer.start();
    }
    
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
        paintBackground(g2);
    }
    
    private Rectangle getClipBounds() {
        Point point = clip.getLocation();
        point = SwingUtilities.convertPoint(clip, point, this);
        return new Rectangle(point.x, point.y, clip.getWidth(), clip.getHeight());
    }
    
    private void paintBackground(Graphics2D g2) {
        if (blackAlpha > 0.0f) {
            Composite composite = g2.getComposite();
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, blackAlpha));
            g2.setColor(Color.BLACK);
            Rectangle rect = g2.getClipBounds().intersection(getClipBounds());
            g2.fillRect(rect.x, rect.y, rect.width, rect.height);
            g2.setComposite(composite);
        }
    }

    public void actionPerformed(ActionEvent e) {
        TransitionManager.getMainFrame().getRootPane().setDefaultButton(null);
        startFadeOut();
    }
    
    public String getUserName() {
        return this.userNameInput.getText();
    }
}
