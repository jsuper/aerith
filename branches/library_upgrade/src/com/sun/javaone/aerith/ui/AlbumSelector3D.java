package com.sun.javaone.aerith.ui;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionAdapter;
import java.awt.event.MouseMotionListener;
import java.awt.font.FontRenderContext;
import java.awt.font.TextLayout;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import javax.swing.JPanel;
import javax.swing.Timer;

import com.aetrion.flickr.photosets.Photoset;
import com.sun.javaone.aerith.g2d.GraphicsUtil;
import com.sun.javaone.aerith.g2d.Reflection;
import com.sun.javaone.aerith.math.GaussianEquation;
import org.jdesktop.fuse.InjectedResource;
import org.jdesktop.fuse.ResourceInjector;

class AlbumSelector3D extends JPanel {
    private static final int ITEM_SIZE = 75;

    private static final int DISPLAY_WIDTH = ITEM_SIZE;
    private static final int DISPLAY_HEIGHT = (int) (ITEM_SIZE * 5.0 / 3.0);
    private static final int VERTICAL_OFFSET = (int) (DISPLAY_HEIGHT / 4.0);

    private List<Photoset> albums = Collections.synchronizedList(new ArrayList<Photoset>());
    private List<BufferedImage> avatars = Collections.synchronizedList(new ArrayList<BufferedImage>());

    private List<BufferedImage> textImages = new ArrayList<BufferedImage>();
    private List<Point2D> textPositions = new ArrayList<Point2D>();
    private int textImageIndex = -1;

    private boolean loadingDone = false;

    private Timer faderTimer = null;

    private float alphaLevel = 0.0f;
    private float textAlphaLevel = 0.0f;

    private int avatarIndex = -1;
    private int selectedAvatarIndex = -1;
    private double avatarPosition = 0.0;
    private double otherAvatarPosition = 0.0;
    private double verticalModifier = 0.0;
    private double avatarSpacing = 0.30;
    private GaussianEquation gaussian = new GaussianEquation(0.5);

    private boolean damaged = true;

    private DrawableAvatar[] drawableAvatars;

    private final MouseListener mouseAvatarSelector;
    private final MouseMotionListener cursorChanger;
    private final MouseMotionListener avatarZoomer;
    private final MouseListener avatarScroller;

    private float shadowOffsetX;
    private float shadowOffsetY;

    private boolean mouseOutside = true;

    ////////////////////////////////////////////////////////////////////////////
    // THEME SPECIFIC FIELDS
    ////////////////////////////////////////////////////////////////////////////
    /** @noinspection UNUSED_SYMBOL*/
    @InjectedResource
    private Font textFont;
    /** @noinspection UNUSED_SYMBOL*/
    @InjectedResource
    private Color textColor;
    /** @noinspection UNUSED_SYMBOL*/
    @InjectedResource
    private float shadowOpacity;
    /** @noinspection UNUSED_SYMBOL*/
    @InjectedResource
    private Color shadowColor;
    /** @noinspection UNUSED_SYMBOL*/
    @InjectedResource
    private int shadowDistance;
    /** @noinspection UNUSED_SYMBOL*/
    @InjectedResource
    private int shadowDirection;

    AlbumSelector3D() {
        ResourceInjector.get().inject(this);

        GridBagLayout layout = new GridBagLayout();
        setLayout(layout);

        setSigma(0.08);
        computeShadow();

        addComponentListener(new DamageManager());

        mouseAvatarSelector = new MouseAvatarSelector();
        cursorChanger = new CursorChanger();
        avatarZoomer = new AvatarZoomer();
        avatarScroller = new MouseSensor();

        addInputListeners();
    }

    void addActionListener(ActionListener l) {
        listenerList.add(ActionListener.class, l);
    }

    void removeActionListener(ActionListener l) {
        listenerList.remove(ActionListener.class, l);
    }

    protected void fireActionPerformed() {
        ActionEvent event = new ActionEvent(this, ActionEvent.ACTION_PERFORMED,
                                            "album_selected");

        Object[] listeners = listenerList.getListenerList();
        for (int i = listeners.length - 2; i >= 0; i -= 2) {
            if (listeners[i] == ActionListener.class) {
                ((ActionListener) listeners[i + 1]).actionPerformed(event);
            }
        }
    }

    Photoset getSelectedAlbum() {
        if (selectedAvatarIndex < 0) {
            return null;
        }

        return albums.get(selectedAvatarIndex);
    }

    boolean addAlbum(final Photoset album) {
        if(album == null)
            return false;
        try {
            BufferedImage image = album.getPrimaryPhoto().getSmallSquareImage();
            if (image.getWidth() != DISPLAY_WIDTH) {
                image = GraphicsUtil.createThumbnail(image, DISPLAY_WIDTH);
            }
            BufferedImage mask = Reflection.createGradientMask(image.getWidth(),
                                                               image.getHeight());
            image = Reflection.createReflectedPicture(image, mask);

            albums.add(album);
            avatars.add(image);
            textImages.add(null);
            textPositions.add(new Point2D.Double());
        } catch (IOException e) {
            return false;
        }

        return true;
    }

    void start() {
        setAvatarIndex((avatars.size() - 1) / 2);
        startFader();
        loadingDone = true;
        damaged = true;
        repaint();
    }

    void removeAllAlbums() {
        loadingDone = false;
        damaged = true;
        avatarIndex = -1;
        textImageIndex = -1;
        avatarPosition = 0.0;
        albums.clear();
        avatars.clear();
        textImages.clear();
        textPositions.clear();
        drawableAvatars = new DrawableAvatar[0];

        repaint();
    }

    private void setSigma(double sigma) {
        this.gaussian.setSigma(sigma);
        this.damaged = true;
        repaint();
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(DISPLAY_WIDTH * 5, (int) (DISPLAY_HEIGHT * 1.5));
    }

    @Override
    public Dimension getMinimumSize() {
        return getPreferredSize();
    }

    @Override
    public boolean isOpaque() {
        return false;
    }

    @Override
    public boolean isFocusable() {
        return true;
    }

    private void computeShadow() {
        double rads = Math.toRadians(shadowDirection);
        shadowOffsetX = (float) Math.cos(rads) * shadowDistance;
        shadowOffsetY = (float) Math.sin(rads) * shadowDistance;
    }

    @Override
    protected void paintComponent(Graphics g) {
        if (!isVisible()) {
            return;
        }

        if (!loadingDone && faderTimer == null) {
            return;
        }

        Insets insets = getInsets();

        int x = insets.left;
        int y = insets.top - VERTICAL_OFFSET;

        int width = getWidth() - insets.left - insets.right;
        int height = getHeight() - insets.top - insets.bottom;

        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                            RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                            RenderingHints.VALUE_ANTIALIAS_ON);
        Composite oldComposite = g2.getComposite();

        if (damaged) {
            drawableAvatars = sortAvatarsByDepth(x, y, width, height);
            damaged = false;
        }

        drawAvatars(g2, drawableAvatars);

        if (drawableAvatars.length > 0) {
            drawAvatarName(g2);
        }

        g2.setComposite(oldComposite);
    }

    private void drawAvatars(Graphics2D g2, DrawableAvatar[] drawableAvatars) {
       for (DrawableAvatar avatar: drawableAvatars) {
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER,
                                                       (float) avatar.getAlpha()));
            g2.drawImage(avatars.get(avatar.getIndex()),
                         (int) avatar.getX(), (int) avatar.getY(),
                         avatar.getWidth(), avatar.getHeight(), null);
        }
     }

    private DrawableAvatar[] sortAvatarsByDepth(int x, int y,
                                                int width, int height) {
        List<DrawableAvatar> drawables = new LinkedList<DrawableAvatar>();

        //noinspection SynchronizeOnNonFinalField
        synchronized (avatars) {
            for (int i = 0; i < avatars.size(); i++) {
                promoteAvatarToDrawable(drawables,
                                        x, y, width, height, i - avatarIndex);
            }
        }

        DrawableAvatar[] drawableAvatars = new DrawableAvatar[drawables.size()];
        drawableAvatars = drawables.toArray(drawableAvatars);
        Arrays.sort(drawableAvatars);
        return drawableAvatars;
    }

    private void drawAvatarName(Graphics2D g2) {
        if (textImageIndex == -1) {
            return;
        }

        Composite composite = g2.getComposite();
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER,
                                                   textAlphaLevel));

        Point2D p = textPositions.get(textImageIndex);
        BufferedImage image = textImages.get(textImageIndex);
        if (image == null) {
            FontRenderContext context = g2.getFontRenderContext();
            TextLayout layout = new TextLayout(albums.get(textImageIndex).getTitle(),
                                               textFont, context);
            Rectangle2D bounds = layout.getBounds();

            double bulletWidth = bounds.getWidth() + 12;
            double bulletHeight = bounds.getHeight() + layout.getDescent() + 4;

            p.setLocation((getWidth() - bulletWidth) / 2.0,
                          (getHeight() + DISPLAY_HEIGHT * 0.95 +
                           bounds.getHeight()) / 2.0 - VERTICAL_OFFSET);

            image = new BufferedImage((int) bulletWidth,
                                      (int) bulletHeight,
                                      BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2text = image.createGraphics();
            Composite composite2 = g2text.getComposite();
            g2text.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER,
                                                           shadowOpacity));
			g2text.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
									RenderingHints.VALUE_ANTIALIAS_ON);
            g2text.setColor(shadowColor);
            layout.draw(g2text, 6 + shadowOffsetX,
                        layout.getAscent() + shadowOffsetY);
            g2text.setComposite(composite2);
            g2text.setColor(textColor);
            layout.draw(g2text, 6, layout.getAscent());
            g2text.dispose();

            BufferedImage mask = Reflection.createGradientMask((int) bulletWidth,
                                                               (int) bulletHeight,
                                                               0.85f, 1.0f);
            textImages.set(textImageIndex,
                           Reflection.createReflectedPicture(image, mask));
        }

        DrawableAvatar selected = null;
        for (DrawableAvatar avatar : drawableAvatars) {
            if (avatar.getIndex() == textImageIndex) {
                selected = avatar;
                break;
            }
        }

        double x = p.getX();
        if (selected != null) {
             //x -= getWidth() / 2.0 - DISPLAY_WIDTH / 2.0 - selected.getX();
        }

        g2.drawImage(image, (int) x, (int) p.getY(), null);
        g2.setComposite(composite);
    }

    private void promoteAvatarToDrawable(List<DrawableAvatar> drawables,
                                         int x, int y, int width, int height,
                                         int offset) {

        double spacing = offset * avatarSpacing;
        double avatarPosition = this.avatarPosition + spacing;

        if (avatarIndex + offset < 0 ||
            avatarIndex + offset >= avatars.size()) {
            return;
        }

        int avatarWidth = DISPLAY_WIDTH;
        BufferedImage avatar = avatars.get(avatarIndex + offset);
        int avatarHeight = avatar.getHeight();

        double result = this.gaussian.compute(otherAvatarPosition + spacing);
        if (result > 1.0) {
            result = 1.0;
        }

        if (mouseOutside) {
            result = 0.0;
        }

        int newWidth = (int) (avatarWidth * (0.7 * result * verticalModifier + 0.9));
        if (newWidth == 0) {
            return;
        }

        int newHeight = (int) (avatarHeight * (0.7 * result * verticalModifier + 0.9));
        if (newHeight == 0) {
            return;
        }

        double avatar_x = x + (width - newWidth) / 2.0;
        double avatar_y = y + (height - newHeight / 2.0) / 2.0;

        double semiWidth = width / 2.0;
        avatar_x += avatarPosition * semiWidth;

        if (avatar_x >= width || avatar_x < -newWidth) {
            return;
        }

        drawables.add(new DrawableAvatar(avatarIndex + offset,
                                         avatar_x, avatar_y,
                                         newWidth, newHeight,
                                         avatarPosition, result));
    }

    private void addInputListeners() {
        addMouseListener(mouseAvatarSelector);
        addMouseMotionListener(cursorChanger);
        addMouseMotionListener(avatarZoomer);
        addMouseListener(avatarScroller);
    }

    private void setAvatarIndex(int index) {
        avatarIndex = index;
        //textImageIndex = index;
    }

    private DrawableAvatar getHitAvatar(int x, int y) {
        for (DrawableAvatar avatar: drawableAvatars) {
            Rectangle hit = new Rectangle((int) avatar.getX(),
                                          (int) avatar.getY(),
                                          avatar.getWidth(),
                                          (int) (avatar.getHeight() * 3.0 / 5.0));
            if (hit.contains(x, y)) {
                return avatar;
            }
        }
        return null;
    }

    private void startFader() {
        faderTimer = new Timer(1000 / 30, new FaderAction());
        faderTimer.start();
    }

    private double getMinPosition() {
        double min = getWidth();
        for (DrawableAvatar avatar : drawableAvatars) {
            if (avatar.getX() < min) {
                min = avatar.getX();
            }
        }
        return min;
    }

    private double getMaxPosition() {
        double max = 0.0;
        for (DrawableAvatar avatar : drawableAvatars) {
            if (avatar.getX() > max) {
                max = avatar.getX();
            }
        }
        return max;
    }

    private double getPosition(MouseEvent e) {
        if (drawableAvatars == null) {
            return 0.0;
        }

        double minPosition = getMinPosition() + DISPLAY_WIDTH / 2.0;
        double maxPosition = getMaxPosition() + DISPLAY_WIDTH / 2.0;
        double distance = maxPosition - minPosition;

        int mouse_x = e.getX();
        if (mouse_x < minPosition) {
            mouse_x = (int) minPosition;
        } else if (mouse_x > maxPosition) {
            mouse_x = (int) maxPosition;
        }

        return (distance / 2.0 + minPosition - mouse_x) / distance;
    }

    private final class FaderAction implements ActionListener {
        private long start = 0;

        private FaderAction() {
            alphaLevel = 0.0f;
            textAlphaLevel = 0.0f;
        }

        public void actionPerformed(ActionEvent e) {
            if (start == 0) {
                start = System.currentTimeMillis();
            }

            alphaLevel = (System.currentTimeMillis() - start) / 500.0f;
            textAlphaLevel = alphaLevel;
            if (alphaLevel > 1.0f) {
                alphaLevel = 1.0f;
                textAlphaLevel = 1.0f;
                faderTimer.stop();
            }

            repaint();
        }
    }

    private final class DrawableAvatar implements Comparable {
        private int index;
        private double x;
        private double y;
        private int width;
        private int height;
        private double zOrder;
        private double position;

        private DrawableAvatar(int index,
                               double x, double y, int width, int height,
                               double position, double zOrder) {
            this.index = index;
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            this.position = position;
            this.zOrder = zOrder;
        }

        public int compareTo(Object o) {
            double zOrder2 = ((DrawableAvatar) o).zOrder;
            if (zOrder < zOrder2) {
                return -1;
            } else if (zOrder > zOrder2) {
                return 1;
            }
            return 0;
        }

        public double getPosition() {
            return position;
        }

        public double getAlpha() {
            //return zOrder * alphaLevel;
            double alpha = alphaLevel - 0.2 * (1.0 - zOrder * verticalModifier);
            if (alpha < 0.0) {
                alpha = 0.0;
            }

            return alpha;
        }

        public int getHeight() {
            return height;
        }

        public int getIndex() {
            return index;
        }

        public int getWidth() {
            return width;
        }

        public double getX() {
            return x;
        }

        public double getY() {
            return y;
        }
    }

    private final class DamageManager extends ComponentAdapter {
        @Override
        public void componentResized(ComponentEvent e) {
            damaged = true;
        }
    }

    private final class CursorChanger extends MouseMotionAdapter {
        @Override
        public void mouseMoved(MouseEvent e) {
            DrawableAvatar avatar = null;
            if (drawableAvatars != null) {
                avatar = getHitAvatar(e.getX(), e.getY());
            }

            if (avatar != null) {
                setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                textImageIndex = avatar.getIndex();
                selectedAvatarIndex = avatar.getIndex();
            } else {
                setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
                textImageIndex = -1;
                selectedAvatarIndex = -1;
            }
        }
    }

    private final class MouseAvatarSelector extends MouseAdapter {
        @Override
        public void mouseClicked(MouseEvent e) {
            if (drawableAvatars != null) {
                if (e.getButton() == MouseEvent.BUTTON1) {
                    DrawableAvatar avatar = getHitAvatar(e.getX(), e.getY());
                    if (avatar != null) {
                        fireActionPerformed();
                    }
                }
            }
        }
    }

    private class AvatarZoomer extends MouseMotionAdapter {
        @Override
        public void mouseMoved(MouseEvent e) {
            if (drawableAvatars == null) {
                return;
            }

            otherAvatarPosition = getPosition(e);
            verticalModifier = 1.0 - (Math.abs(e.getY() - DISPLAY_HEIGHT / 2.0) / (DISPLAY_HEIGHT / 2.0));
            if (verticalModifier < 0.0) {
                verticalModifier = 0.0;
            }

            damaged = true;
            repaint();
        }
    }

    private final class MouseSensor extends MouseAdapter {
        @Override
        public void mouseEntered(MouseEvent e) {
            mouseOutside = false;
            damaged = true;
            repaint();
        }

        @Override
        public void mouseExited(MouseEvent e) {
            mouseOutside = true;

            setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
            textImageIndex = -1;
            selectedAvatarIndex = -1;

            damaged = true;
            repaint();
        }
    }
}