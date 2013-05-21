package com.sun.javaone.aerith.ui.fullscreen;

import java.awt.AWTEvent;
import java.awt.Color;
import java.awt.DisplayMode;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.event.AWTEventListener;
import java.awt.event.KeyEvent;
import java.awt.image.BufferStrategy;
import java.awt.image.BufferedImage;
import java.io.File;
import java.net.URL;
import javax.swing.JFrame;

import com.sun.javaone.aerith.g2d.GraphicsUtil;
import org.jdesktop.swingx.mapviewer.LocalResponseCache;

public class FullScreenManager {
    private final FullScreenRenderer renderer;
    private BufferStrategy strategy;
    private Frame frame;
    private FullScreenManager.EscapeKeyListener escapeListener;

    public FullScreenManager(FullScreenRenderer renderer) {
        this.renderer = renderer;
    }

    public void enterFullScreen() {
        GraphicsEnvironment environment = GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsDevice device = environment.getDefaultScreenDevice();

//        DisplayMode displayMode = device.getDisplayMode();

        try {
            frame = new JFrame();
            frame.setUndecorated(true);
            frame.setIgnoreRepaint(true);

            device.setFullScreenWindow(frame);
//            if (device.isDisplayChangeSupported()) {
//                device.setDisplayMode(new DisplayMode(800, 500, 32,
//                                                      DisplayMode.REFRESH_RATE_UNKNOWN));
//            }

            frame.setBackground(Color.BLACK);
            frame.createBufferStrategy(2);
            this.strategy = frame.getBufferStrategy();

            run();
        } finally {
            device.setFullScreenWindow(null);
//            device.setDisplayMode(displayMode);
        }
    }

    private void run() {
        setupEscapeKey();

        renderer.start();
        while (!renderer.isDone()) {
            Graphics g = null;
            try {
                g = strategy.getDrawGraphics();
                if (!strategy.contentsLost()) {
                    renderer.render(g, frame.getBounds());
                }
            } finally {
                if (g != null) {
                    g.dispose();
                }
            }
            strategy.show();
        }
        renderer.end();
        frame.dispose();

        removeEscapeKey();
    }

    private void removeEscapeKey() {
        Toolkit.getDefaultToolkit().removeAWTEventListener(escapeListener);
    }

    private void setupEscapeKey() {
        escapeListener = new EscapeKeyListener();
        Toolkit.getDefaultToolkit().addAWTEventListener(escapeListener,
                                                        KeyEvent.KEY_EVENT_MASK);
    }

    private class EscapeKeyListener implements AWTEventListener {
        public void eventDispatched(AWTEvent event) {
            KeyEvent keyEvent = (KeyEvent) event;
            if (keyEvent.getID() == KeyEvent.KEY_RELEASED &&
                    keyEvent.getKeyCode() == KeyEvent.VK_ESCAPE) {
                renderer.cancel();
            }
        }
    }
    
    public static void launch(URL baseDir) throws Exception {
        LocalResponseCache.installResponseCache();

        GraphicsEnvironment environment = GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsDevice device = environment.getDefaultScreenDevice();
        DisplayMode displayMode = device.getDisplayMode();
        BufferedImage image;

        Robot robot = new Robot();
        image = robot.createScreenCapture(
            new Rectangle(0, 0, displayMode.getWidth(),
                          displayMode.getHeight()));
        image = GraphicsUtil.toCompatibleImage(image);

        FullScreenRenderer renderer = new IndyFullScreenRenderer(image, baseDir);
        FullScreenManager manager = new FullScreenManager(renderer);

        manager.enterFullScreen();
    }

    public static void main(String... args) {
        try {
            launch(new File(".").toURI().toURL());
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
