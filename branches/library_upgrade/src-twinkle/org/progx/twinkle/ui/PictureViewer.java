package org.progx.twinkle.ui;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.font.TextLayout;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import javax.media.opengl.GL;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.glu.GLU;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.Box;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.vecmath.Point3f;
import javax.vecmath.Point3i;

import org.jdesktop.swingx.util.ShadowFactory;
import org.progx.jogl.CompositeGLPanel;
import org.progx.jogl.GLUtilities;
import org.progx.jogl.rendering.ReflectedQuad;
import org.progx.jogl.rendering.Renderable;
import org.progx.jogl.rendering.RenderableFactory;
import org.progx.math.equation.Equation;
import org.progx.twinkle.Debug;
import org.progx.twinkle.equation.AnimationEquation;

public class PictureViewer extends CompositeGLPanel {
    public static final String KEY_ACTION_NEXT_PICTURE = "next";
    public static final String KEY_ACTION_PREVIOUS_PICTURE = "previous";
    public static final String KEY_ACTION_SHOW_PICTURE = "show";
    
    private static boolean envAntiAliasing = true;
    static {
        envAntiAliasing = System.getProperty("twinkle.aa") == null;
        System.out.println("[TWINKLE] Use anti aliasing: " + envAntiAliasing);
    }
    private static boolean envTransparent = true;
    static {
        envTransparent = System.getProperty("twinkle.transparent") != null;
        System.out.println("[TWINKLE] Transparent: " + envTransparent);
    }
    
    private static final float QUAD_WIDTH = 65.0f;

    private static final int THUMB_WIDTH = 48;
    private static final double SELECTED_THUMB_RATIO = 0.35;
    private static final double SELECTED_THUMB_EXTRA_WIDTH = THUMB_WIDTH * SELECTED_THUMB_RATIO;
    
    private static final int INDEX_LEFT_PICTURE = 0;
    private static final int INDEX_SELECTED_PICTURE = 1;
    private static final int INDEX_NEXT_PICTURE = 2;
    private static final int INDEX_RIGHT_PICTURE = 3;

    private List<Picture> pictures = Collections.synchronizedList(new ArrayList<Picture>());
    private Renderable[] renderables = new Renderable[4];
    
    private Queue<Renderable> initQuadsQueue = new ConcurrentLinkedQueue<Renderable>();
    private Queue<Renderable> disposeQuadsQueue = new ConcurrentLinkedQueue<Renderable>();

    private float camPosX = 0.0f;
    private float camPosY = 0.0f;
    private float camPosZ = 100.0f;
    
    private int picturesStripHeight = 0;
    
    private BufferedImage textImage = null;
    private BufferedImage nextTextImage = null;
    private ShadowFactory shadowFactory = new ShadowFactory(11, 1.0f, Color.BLACK);
    private Font textFont;
    private float textAlpha = 1.0f;
    private Color grayColor = new Color(0xE1E1E1);

    private int selectedPicture = -1;
    private int nextPicture = -1;
    
    private boolean pictureIsShowing = false;
    private Equation curve = new AnimationEquation(2.8, -0.98);//3.6, -1.0);
    private Timer animator;
    
    private boolean antiAliasing = envAntiAliasing;
    private boolean stopRendering;

    private final Object animLock = new Object();
    
    public PictureViewer() {
        super(!envTransparent, /*true*/false);
        setPreferredSize(new Dimension(640, 480));
        
        addMouseWheelListener(new MouseWheelDriver());
        
        setFocusable(true);
        registerActions();

        textFont = getFont().deriveFont(Font.BOLD, 32.0f);

        createButtons();
    }

    public boolean isAntiAliasing() {
        return antiAliasing;
    }

    public void setAntiAliasing(boolean antiAliasing) {
        this.antiAliasing = antiAliasing && envAntiAliasing;
        repaint();
    }

    public void addPicture(final String name, final BufferedImage image) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                int size;
                Picture picture = new Picture(name, image);
                
                pictures.add(picture);
                size = pictures.size();

                if (size == 1) {
                    initQuadsQueue.add(createQuad(INDEX_SELECTED_PICTURE, 0));
                } else if (size - 1 == selectedPicture + 1) {
                    initQuadsQueue.add(createQuad(INDEX_NEXT_PICTURE, 1));
                } else if (size - 1 == nextPicture + 1) {
                    initQuadsQueue.add(createQuad(INDEX_RIGHT_PICTURE, 2));
                }
                
                float ratio = picture.getRatio();
                picturesStripHeight = Math.max(picturesStripHeight,
                                              (int) (THUMB_WIDTH + SELECTED_THUMB_EXTRA_WIDTH / ratio));

                getActionMap().get(KEY_ACTION_SHOW_PICTURE).setEnabled(selectedPicture >= 0);
                getActionMap().get(KEY_ACTION_NEXT_PICTURE).setEnabled(selectedPicture < size - 1);
                getActionMap().get(KEY_ACTION_PREVIOUS_PICTURE).setEnabled(selectedPicture > 0);
            }
        });
        repaint();
    }
    
    public void dispose() {
        stopRendering = true;
        for (Renderable renderable : renderables) {
            if (renderable != null) {
                disposeQuadsQueue.add(renderable);
            }
        }
        for (Picture picture : pictures) {
            picture.getImage().flush();
        }
        pictures.clear();
        repaint();
    }
    
    public void showSelectedPicture() {
        if (animator != null && animator.isRunning()) {
            return;
        }
        
        pictureIsShowing = !pictureIsShowing;
        ((ShowPictureAction) getActionMap().get(KEY_ACTION_SHOW_PICTURE)).toggleName();

        animator = new Timer(1000 / 60, new ZoomAnimation());
        animator.start();
    }

    public void nextPicture() {
        int size = pictures.size() - 1;

        if (selectedPicture < size) {
            showPicture(true);
        }
    }
    
    public void previousPicture() {
        if (selectedPicture > 0) {
            showPicture(false);
        }
    }
    
    @Override
    public void init(GLAutoDrawable drawable) {
        super.init(drawable);
        GL gl = drawable.getGL();

        initQuads(gl);
    }
    
    private void registerActions() {
        KeyStroke stroke;
        Action action;
        
        InputMap inputMap = getInputMap();
        ActionMap actionMap = getActionMap();

        stroke = KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, 0);
        inputMap.put(stroke, KEY_ACTION_NEXT_PICTURE);
        stroke = KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0);
        inputMap.put(stroke, KEY_ACTION_NEXT_PICTURE);

        action = new NextPictureAction();
        actionMap.put(KEY_ACTION_NEXT_PICTURE, action);
        
        stroke = KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, 0);
        inputMap.put(stroke, KEY_ACTION_PREVIOUS_PICTURE);
        stroke = KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0);
        inputMap.put(stroke, KEY_ACTION_PREVIOUS_PICTURE);

        action = new PreviousPictureAction();
        actionMap.put(KEY_ACTION_PREVIOUS_PICTURE, action);
        
        stroke = KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0);
        inputMap.put(stroke, KEY_ACTION_SHOW_PICTURE);
        stroke = KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0);
        inputMap.put(stroke, KEY_ACTION_SHOW_PICTURE);

        action = new ShowPictureAction();
        actionMap.put(KEY_ACTION_SHOW_PICTURE, action);
    }
    
    private void createButtons() {
        ControlButton button;
        ControlPanel buttonsPanel = new ControlPanel();
        
        button = new ControlButton(getActionMap().get(KEY_ACTION_PREVIOUS_PICTURE));
        buttonsPanel.add(button);
        button = new ControlButton(getActionMap().get(KEY_ACTION_SHOW_PICTURE));
        buttonsPanel.add(button);
        button = new ControlButton(getActionMap().get(KEY_ACTION_NEXT_PICTURE));
        buttonsPanel.add(button);

        setLayout(new GridBagLayout());
        add(Box.createGlue(), new GridBagConstraints(0, 0,
                                                     2, 1,
                                                     0.0, 1.0,
                                                     GridBagConstraints.LINE_START,
                                                     GridBagConstraints.VERTICAL, 
                                                     new Insets(0, 0, 0, 0),
                                                     0, 0));
        add(buttonsPanel, new GridBagConstraints(0, 1,
                                                 1, 1,
                                                 0.0, 0.0,
                                                 GridBagConstraints.LINE_START,
                                                 GridBagConstraints.NONE, 
                                                 new Insets(0, 13, 13, 0),
                                                 0, 0));
        add(Box.createHorizontalGlue(), new GridBagConstraints(1, 1,
                                                               1, 1,
                                                               1.0, 0.0,
                                                               GridBagConstraints.LINE_START,
                                                               GridBagConstraints.HORIZONTAL, 
                                                               new Insets(0, 0, 0, 0),
                                                               0, 0));
    }
    
    private void showPicture(final boolean next) {
        if (animator != null && animator.isRunning()) {
            return;
        }
        
        if (pictureIsShowing) {
            new Thread(new Runnable() {
                /** @noinspection BusyWait*/
                public void run() {
                    showSelectedPicture();
                    while (animator.isRunning()) {
                        synchronized(animLock) {
                            try {
                               animLock.wait();
                            } catch (InterruptedException ie) {
                                ie.printStackTrace();
                            }
                        }
                    }
                    showPicture(next);
                    while (animator.isRunning()) {
                        synchronized(animLock) {
                            try {
                               animLock.wait();
                            } catch (InterruptedException ie) {
                                ie.printStackTrace();
                            }
                        }
                    }
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            showSelectedPicture();
                        }
                    });
                }
            }).start();
            return;
        }
        
        animator = new Timer(10, new SlideAnimation(next));
        animator.start();
    }
    
    private Renderable createQuad(int index, int pictureNumber) {
        Picture picture = pictures.get(pictureNumber);

        if (picture == null || index > renderables.length) {
            return null;
        }

        float ratio = picture.getRatio();
        int width = (int) QUAD_WIDTH;
        int height = (int) (QUAD_WIDTH / ratio);
        if (ratio < 1.0f) {
            height = (int) (QUAD_WIDTH / 1.5);
            width = (int) (height * ratio);
        }
        
        Renderable quad = RenderableFactory.createReflectedQuad(0.0f, 0.0f, 0.0f,
                                                                width, height,
                                                                picture.getImage(), null,
                                                                picture.getName());
        renderables[index] = quad;
        
        if (index == INDEX_SELECTED_PICTURE) {
            selectedPicture = pictureNumber;
            
            quad.setPosition(-7.0f, 0.0f, 0.0f);
            quad.setRotation(0, 30, 0);
            
            textImage = generateTextImage(picture);
        } else if (index == INDEX_NEXT_PICTURE) {
            nextPicture = pictureNumber;
            
            quad.setScale(0.5f, 0.5f, 0.5f);
            quad.setPosition(36.0f, -height / 2.0f, 30.0f);
            quad.setRotation(0, -20, 0);
        } else if (index == INDEX_RIGHT_PICTURE) {
            quad.setScale(0.5f, 0.5f, 0.5f);
            quad.setPosition(196.0f, -height / 2.0f, 30.0f);
            quad.setRotation(0, -20, 0);
        } else if (index == INDEX_LEFT_PICTURE) {
            quad.setPosition(-7.0f - QUAD_WIDTH * 2.0f, 0.0f, 0.0f);
            quad.setRotation(0, 30, 0);
        }
        
        return quad;
    }

    private BufferedImage generateTextImage(Picture picture) {
        FontRenderContext context = getFontMetrics(textFont).getFontRenderContext();
        //Graphics2D globalGraphics = (Graphics2D) getGraphics();
        //globalGraphics.setFont(textFont);
        //FontRenderContext context = (globalGraphics).getFontRenderContext();
        GlyphVector vector = textFont.createGlyphVector(context, picture.getName());
        Rectangle bounds = vector.getPixelBounds(context, 0.0f, 0.0f);
        TextLayout layout = new TextLayout(picture.getName(), textFont, context);
        
        BufferedImage image = new BufferedImage((int) (bounds.getWidth()),
                                                (int) (layout.getAscent() + layout.getDescent() + layout.getLeading()),
                                                BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = image.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                            RenderingHints.VALUE_ANTIALIAS_ON);
        layout.draw(g2, 0, layout.getAscent());
        g2.dispose();
        
        BufferedImage shadow = shadowFactory.createShadow(image);
        BufferedImage composite = new BufferedImage(shadow.getWidth(),
                                                    shadow.getHeight(),
                                                    BufferedImage.TYPE_INT_ARGB);
        g2 = composite.createGraphics();
        g2.drawImage(shadow, null,
                     -1 - (shadow.getWidth() - image.getWidth()) / 2,
                     2 - (shadow.getHeight() - image.getHeight()) / 2);
        g2.drawImage(image, null, 0, 0);
        g2.dispose();
        
        shadow.flush();
        image.flush();
        
        return composite;
    }

    @Override
    protected void render2DBackground(Graphics g) {
        // NOTE: with antialiasing on the accum buffer creates a black backround
//        if (!antiAliasing) {
//            float h = getHeight() * 0.55f;
//
//            GradientPaint paint = new GradientPaint(0.0f, h, Color.BLACK,
//                                                    0.0f, getHeight(), new Color(0x4C4C4C));
//            Graphics2D g2 = (Graphics2D) g;
//            Paint oldPaint = g2.getPaint();
//            g2.setPaint(paint);
//            g2.fillRect(0, 0, getWidth(), getHeight());
//            g2.setPaint(oldPaint);
//        }

        if (!envTransparent) {
            g.setColor(Color.BLACK);
            Rectangle clip = g.getClipBounds();
            g.fillRect(clip.x, clip.y, clip.width, clip.height);
        }
    }

    @Override
    protected void render2DForeground(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        setupForegroundGraphics(g2);

        //paintPicturesStrip(g2);
        paintInfo(g2);
    }
    
    private void paintInfo(Graphics2D g2) {
        g2.setColor(Color.WHITE);
        
        if (Debug.isDebug()) {
            g2.drawString("X: " + camPosX, 5, 15);
            g2.drawString("Y: " + camPosY, 5, 30);
            g2.drawString("Z: " + camPosZ, 5, 45);
        }
        
        if (textImage != null) {
            Composite composite = g2.getComposite();
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, textAlpha));
            g2.drawImage(textImage, null,
                         (getWidth() - textImage.getWidth()) / 2,
                         (int) (getHeight() - textFont.getSize() * 1.7));
            g2.setComposite(composite);
        }
    }
    
    private static void setupForegroundGraphics(Graphics2D g2) {
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                            RenderingHints.VALUE_INTERPOLATION_BILINEAR);
    }

    @Override
    protected void render3DScene(final GL gl, final GLU glu) {
        if (stopRendering) {
            initAndDisposeQuads(gl);
            return;
        }
        
        initScene(gl);
        initAndDisposeQuads(gl);
        
        Renderable scene = new Renderable() {
            @Override
            public Point3f getPosition() {
                return null;
            }
        
            @Override
            public void render(GL gl, boolean antiAliased) {
                setupCamera(gl, glu);
                renderItems(gl, antiAliased);
            }

            @Override
            public void init(GL gl) {
            }
        };

        if (isAntiAliasing()) {
            GLUtilities.renderAntiAliased(gl, scene);
        } else {
            scene.render(gl, false);
        }
    }

    private void initQuads(GL gl) {
        for (Renderable item: renderables) {
            if (item != null) {
                item.init(gl);
            }
        }
    }
    
    private void initAndDisposeQuads(final GL gl) {
        while (!initQuadsQueue.isEmpty()) {
            Renderable quad = initQuadsQueue.poll();
            if (quad != null) {
                quad.init(gl);
            }
        }
        
        while (!disposeQuadsQueue.isEmpty()) {
            Renderable quad = disposeQuadsQueue.poll();
            if (quad != null) {
                quad.dispose(gl);
            }
        }
    }
    
    private static void initScene(GL gl) {
        gl.glMatrixMode(GL.GL_MODELVIEW);
        gl.glLoadIdentity();
    }

    private void setupCamera(GL gl, GLU glu) {
        glu.gluLookAt(camPosX, camPosY, camPosZ,
                      0.0f, 0.0f, 0.0f,
                      0.0f, 1.0f, 0.0f);
        gl.glTranslatef(0.0f, -1.0f, 0.0f);
    }

    private void renderItems(GL gl, boolean antiAliased) {
        for (Renderable renderable: renderables) {
            setAndRender(gl, renderable, antiAliased);
        }
    }
    
    private static void setAndRender(GL gl, Renderable renderable, boolean antiAliased) {
        if (renderable == null) {
            return;
        }
        
        Point3f pos = renderable.getPosition();
        Point3i rot = renderable.getRotation();
        Point3f scale = renderable.getScale();

        gl.glPushMatrix();
        gl.glScalef(scale.x, scale.y, scale.z);
        gl.glTranslatef(pos.x, pos.y + 4.0f, pos.z);
        gl.glRotatef(rot.x, 1.0f, 0.0f, 0.0f);
        gl.glRotatef(rot.y, 0.0f, 1.0f, 0.0f);
        gl.glRotatef(rot.z, 0.0f, 0.0f, 1.0f);
        
        renderable.render(gl, antiAliased);
        gl.glPopMatrix();
    }

    private final class ZoomAnimation implements ActionListener {
        private static final int ANIM_DELAY = 400;
        private long start;

        private ZoomAnimation() {
            start = System.currentTimeMillis();
        }

        public void actionPerformed(ActionEvent e) {
            long elapsed = System.currentTimeMillis() - start;
            if (elapsed >= ANIM_DELAY) {
                Timer timer = (Timer) e.getSource();
                timer.stop();
                synchronized (animLock) {
                    animLock.notifyAll();
                }
            } else {
                double factor = (double) elapsed / (double) ANIM_DELAY;
                animateQuads(curve.compute(factor));
            }
            repaint();
        }

        private void animateQuads(double factor) {
            if (!pictureIsShowing) {
                factor = 1.0 - factor;
            }
            
            Renderable quad = renderables[INDEX_SELECTED_PICTURE];
            Point3f position = quad.getPosition();
            
            quad.setRotation(0, (int) (30.0 * (1.0 - factor)), 0);
            quad.setPosition((float) (-7.0f * (1.0 - factor)),
                             position.y,
                             (float) (30.0 * factor));
      
            quad = renderables[INDEX_NEXT_PICTURE];
            if (quad != null) {
                position = quad.getPosition();
                quad.setPosition(36.0f + (float) (120.0f * factor),
                                 position.y,
                                 position.z);
            }
        }
    }

    private final class SlideAnimation implements ActionListener {
        private static final int ANIM_DELAY = 800;
        
        private final boolean next;
        private long start;

        private SlideAnimation(boolean next) {
            this.next = next;
            start =  System.currentTimeMillis();
            
            if (next) {
                if (nextPicture < pictures.size()) {
                    nextTextImage = generateTextImage(pictures.get(nextPicture));
                } else {
                    nextTextImage = null;
                }
            } else {
                if (selectedPicture > 0) {
                    nextTextImage = generateTextImage(pictures.get(selectedPicture - 1));
                } else {
                    nextTextImage = null;
                }
            }
        }

        public void actionPerformed(ActionEvent e) {
            long elapsed = System.currentTimeMillis() - start;
            if (elapsed >= ANIM_DELAY) {
                Timer timer = (Timer) e.getSource();
                timer.stop();

                if (next) {
                    selectNextPicture();
                } else {
                    selectPreviousPicture();
                }

                Action action = getActionMap().get(KEY_ACTION_NEXT_PICTURE);
                action.setEnabled(selectedPicture < pictures.size() - 1);

                action = getActionMap().get(KEY_ACTION_PREVIOUS_PICTURE);
                action.setEnabled(selectedPicture > 0 && pictures.size() > 1);

                synchronized (animLock) {
                    animLock.notifyAll();
                }
            } else {
                double factor = (double) elapsed / (double) ANIM_DELAY;
                double curvedFactor = curve.compute(factor);

                if (next) {
                    animateQuadsNext(curvedFactor);
                } else {
                    animateQuadsPrevious(1.0 - curvedFactor);
                }

                setTextAlpha(elapsed, factor);
            }
            
            repaint();
        }

        private void animateQuadsNext(double factor) {
            Renderable quad = renderables[INDEX_SELECTED_PICTURE];
            Point3f position = quad.getPosition();
            quad.setPosition(-7.0f - QUAD_WIDTH * 2.0f * (float) factor, position.y, position.z);
            
            ReflectedQuad reflected = (ReflectedQuad) renderables[INDEX_NEXT_PICTURE];
            if (reflected != null) {
                float scale = 0.5f + 0.5f * (float) factor;
      
                reflected.setScale(scale, scale, scale);
                reflected.setRotation(0, (int) (-20.0 + 50.0 * factor), 0);
                reflected.setPosition((float) (36.0f - 43.0f * factor),
                                      -reflected.getHeight() * (1.0f - scale),
                                      (float) (30.0 * (1.0 - factor)));
            }
            
            quad = renderables[INDEX_RIGHT_PICTURE];
            if (quad != null) {
                position = quad.getPosition();
                quad.setPosition(36.0f + 160.0f * (float) (1.0 - factor), position.y, position.z);
            }
        }
        
        private void animateQuadsPrevious(double factor) {
            ReflectedQuad reflected = (ReflectedQuad) renderables[INDEX_SELECTED_PICTURE];
            float scale = 0.5f + 0.5f * (float) factor;
  
            reflected.setScale(scale, scale, scale);
            reflected.setRotation(0, (int) (-20.0 + 50.0 * factor), 0);
            reflected.setPosition((float) (36.0f - 43.0f * factor),
                                  -reflected.getHeight() * (1.0f - scale),
                                  (float) (30.0 * (1.0 - factor)));
            
            Renderable quad = renderables[INDEX_NEXT_PICTURE];
            if (quad != null) {
                Point3f position = quad.getPosition();
                quad.setPosition(36.0f + 160.0f * (float) (1.0 - factor), position.y, position.z);
            }

            quad = renderables[INDEX_LEFT_PICTURE];
            if (quad != null) {
                Point3f position = quad.getPosition();
                quad.setPosition(-7.0f - QUAD_WIDTH * 2.0f * (float) factor, position.y, position.z);
            }
        }

        private void setTextAlpha(long elapsed, double factor) {
            if (elapsed < ANIM_DELAY / 2.0) {
                textAlpha = (float) (1.0 - 2.0 * factor);
            } else {
                textAlpha = (float) ((factor - 0.5) * 2.0);
                if (textAlpha > 1.0f) {
                    textAlpha = 1.0f;
                }
            }
            if (textAlpha < 0.1f) {
                textAlpha = 0.1f;
                textImage = nextTextImage;
            }
        }

        private void selectPreviousPicture() {
            selectedPicture--;
            nextPicture--;

            if (renderables[INDEX_RIGHT_PICTURE] != null) {
                disposeQuadsQueue.add(renderables[INDEX_RIGHT_PICTURE]);
            }
            
            Renderable quad = renderables[INDEX_NEXT_PICTURE];
            if (quad != null) { 
                renderables[INDEX_RIGHT_PICTURE] = quad;
                quad.setScale(0.5f, 0.5f, 0.5f);
                quad.setPosition(196.0f, -((ReflectedQuad) quad).getHeight() / 2.0f, 30.0f);
                quad.setRotation(0, -20, 0);
            }
            
            quad = renderables[INDEX_SELECTED_PICTURE];
            renderables[INDEX_NEXT_PICTURE] = quad;
            
            nextTextImage = generateTextImage(pictures.get(nextPicture));
            
            quad = renderables[INDEX_LEFT_PICTURE];
            renderables[INDEX_SELECTED_PICTURE] = quad;
            
            textImage = generateTextImage(pictures.get(selectedPicture));
            
            if (selectedPicture > 0) {
                initQuadsQueue.add(createQuad(INDEX_LEFT_PICTURE, selectedPicture - 1));
            } else {
                renderables[INDEX_LEFT_PICTURE] = null;
            }
        }

        private void selectNextPicture() {
            selectedPicture++;
            nextPicture++;
            
            if (renderables[INDEX_LEFT_PICTURE] != null) {
                disposeQuadsQueue.add(renderables[INDEX_LEFT_PICTURE]);
            }
            
            Renderable quad = renderables[INDEX_SELECTED_PICTURE];
            renderables[INDEX_LEFT_PICTURE] = quad;
            quad.setPosition(-7.0f - QUAD_WIDTH * 2.0f, 0.0f, 0.0f);
            quad.setRotation(0, 30, 0);
            
            quad = renderables[INDEX_NEXT_PICTURE];
            renderables[INDEX_SELECTED_PICTURE] = quad;
            
            textImage = generateTextImage(pictures.get(selectedPicture));
            
            if (nextPicture < pictures.size()) {
                quad = renderables[INDEX_RIGHT_PICTURE];
                renderables[INDEX_NEXT_PICTURE] = quad;
                nextTextImage = generateTextImage(pictures.get(nextPicture));
            } else {
                renderables[INDEX_NEXT_PICTURE] = null;
            }
            
            if (nextPicture < pictures.size() - 1) {
                initQuadsQueue.add(createQuad(INDEX_RIGHT_PICTURE, nextPicture + 1));
            } else {
                renderables[INDEX_RIGHT_PICTURE] = null;
            }
        }
    }
    
    private final class NextPictureAction extends AbstractAction {
        public NextPictureAction() {
            super("Next");
            ImageIcon nextIconActive = new ImageIcon(PictureViewer.class.getResource("images/pictureviewer-next-button.png"));
            ImageIcon nextIconPressed = new ImageIcon(PictureViewer.class.getResource("images/pictureviewer-next-button-pressed.png"));
            ImageIcon disabledIcon = new ImageIcon(PictureViewer.class.getResource("images/pictureviewer-next-disabled-button.png"));
            
            setEnabled(false);
            
            putValue("disabledIcon", disabledIcon);
            putValue("pressedIcon", nextIconPressed);
            putValue(Action.LARGE_ICON_KEY, nextIconActive);
            putValue(Action.ACTION_COMMAND_KEY, "next");
            putValue(Action.SHORT_DESCRIPTION, "Show next picture");
        }

        public void actionPerformed(ActionEvent e) {
            nextPicture();
        }
    }
    
    private final class PreviousPictureAction extends AbstractAction {
        public PreviousPictureAction() {
            super("Previous");
            ImageIcon previousIconActive = new ImageIcon(PictureViewer.class.getResource("images/pictureviewer-previous-button.png"));
            ImageIcon previousIconPressed = new ImageIcon(PictureViewer.class.getResource("images/pictureviewer-previous-button-pressed.png"));
            ImageIcon disabledIcon = new ImageIcon(PictureViewer.class.getResource("images/pictureviewer-previous-disabled-button.png"));

            setEnabled(false);
            
            putValue("disabledIcon", disabledIcon);
            putValue("pressedIcon", previousIconPressed);
            putValue(Action.LARGE_ICON_KEY, previousIconActive);
            putValue(Action.ACTION_COMMAND_KEY, "previous");
            putValue(Action.SHORT_DESCRIPTION, "Show previous picture");
        }

        public void actionPerformed(ActionEvent e) {
            previousPicture();
        }
    }
    
    private final class ShowPictureAction extends AbstractAction {
        private ImageIcon showIconActive;
        private ImageIcon showIconAll;
        private ImageIcon showIconPressed;
        private ImageIcon showIconAllPressed;
        
        public ShowPictureAction() {
            super(pictureIsShowing ? "Show All" : "Show Picture");
            
            showIconActive = new ImageIcon(PictureViewer.class.getResource("images/pictureviewer-show-button.png"));
            showIconPressed = new ImageIcon(PictureViewer.class.getResource("images/pictureviewer-show-button-pressed.png"));
            showIconAll = new ImageIcon(PictureViewer.class.getResource("images/pictureviewer-show-all-button.png"));
            showIconAllPressed = new ImageIcon(PictureViewer.class.getResource("images/pictureviewer-show-all-button-pressed.png"));
            
            ImageIcon disabledIcon = new ImageIcon(PictureViewer.class.getResource("images/pictureviewer-show-disabled-button.png"));
            
            setEnabled(false);

            putValue("disabledIcon", disabledIcon);
            putValue("pressedIcon", pictureIsShowing ? showIconAllPressed : showIconPressed);
            putValue(Action.LARGE_ICON_KEY, pictureIsShowing ? showIconAll : showIconActive);
            putValue(Action.ACTION_COMMAND_KEY, "show");
            putValue(Action.SHORT_DESCRIPTION, "Show selected picture");
        }

        public void actionPerformed(ActionEvent e) {
            showSelectedPicture();
        }
        
        public void toggleName() {
            putValue(Action.NAME, pictureIsShowing ? "Show All" : "Show Picture");
            putValue(Action.LARGE_ICON_KEY, pictureIsShowing ? showIconAll : showIconActive);
            putValue("pressedIcon", pictureIsShowing ? showIconAllPressed : showIconPressed);
        }
    }
    
    private final class ControlButton extends JButton implements PropertyChangeListener {
        public ControlButton(Action action) {
            super(action);

            getAction().addPropertyChangeListener(this);
            
            setPressedIcon((Icon) getAction().getValue("pressedIcon"));
            setDisabledIcon((Icon) getAction().getValue("disabledIcon"));
            
            setForeground(grayColor);
            setFocusable(false);
            setFocusPainted(false);
            setBorderPainted(false);
            setContentAreaFilled(false);
            setOpaque(false);
            setMargin(new Insets(0, 0, 0, 0));
            setText("");
            setHideActionText(true);
        }
        
        @Override
        public void setToolTipText(String text) {
        }

        public void propertyChange(PropertyChangeEvent evt) {
            if ("pressedIcon".equals(evt.getPropertyName())) {
                setPressedIcon((Icon) evt.getNewValue());
            }
        }
    }
    
    private static final class ControlPanel extends JPanel {
        private BufferedImage background;
        
        public ControlPanel() {
            super(new FlowLayout(FlowLayout.CENTER, 2, 2));
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            if (background == null) {
                createBackground();
            }

            g.drawImage(background, 0, 0, null);
        }

        private void createBackground() {
            background = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2 = background.createGraphics();
            
            g2.setColor(Color.WHITE);
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.3f));
            
            Insets insets = getInsets();
            RoundRectangle2D rect = new RoundRectangle2D.Double(insets.left, insets.top,
                                                                getWidth() - insets.right - insets.left,
                                                                getHeight() - insets.bottom - insets.top,
                                                                14, 14);
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.fill(rect);
            
            g2.dispose();
        }
    }
    
    private final class MouseWheelDriver implements MouseWheelListener {
        public void mouseWheelMoved(MouseWheelEvent e) {
            if (e.getWheelRotation() > 0) {
                nextPicture();
            } else {
                previousPicture();
            }
        }
    }
}