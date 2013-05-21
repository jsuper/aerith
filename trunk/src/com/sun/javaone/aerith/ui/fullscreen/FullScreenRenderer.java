package com.sun.javaone.aerith.ui.fullscreen;

import java.awt.Graphics;
import java.awt.Rectangle;

public interface FullScreenRenderer {
    public boolean isDone();
    public void start();
    public void end();
    public void render(Graphics g, Rectangle bounds);
    public void cancel();
}
