package org.progx.jogl;

import java.awt.Graphics;

import javax.media.opengl.GL;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLEventListener;
import javax.media.opengl.GLJPanel;
import javax.media.opengl.glu.GLU;

/**
 * @author campbelc
 */
public class CompositeGLPanel extends GLJPanel implements GLEventListener {
    private static GLU glu = new GLU();
    private static final boolean OPENGL_PIPELINE_WORK_AROUND = Boolean.getBoolean("sun.java2d.opengl");
    static {
        System.out.println("[TWINKLE] OpenGL pipeline work around: "+ OPENGL_PIPELINE_WORK_AROUND);
    }

    private boolean hasDepth;

    public CompositeGLPanel(boolean isOpaque, boolean hasDepth) {
        super(getCaps(isOpaque), null, null);
        setOpaque(isOpaque);
        this.hasDepth = hasDepth;
        addGLEventListener(this);
    }

    private static GLCapabilities getCaps(boolean opaque) {
        GLCapabilities caps = new GLCapabilities();
        
        if (!opaque) {
            caps.setAlphaBits(8);
        }

        return caps;
    }

    @Override
    public void paintComponent(Graphics g) {
        render2DBackground(g);
        super.paintComponent(g);
        render2DForeground(g);
    }

    public void init(GLAutoDrawable drawable) {
        GL gl = drawable.getGL();
        if (hasDepth) {
            gl.glEnable(GL.GL_DEPTH_TEST);
        }
        gl.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
    }

    protected void render2DBackground(Graphics g) {
    }

    protected void render3DScene(GL gl, GLU glu) {
    }

    protected void render2DForeground(Graphics g) {
    }

    public void display(GLAutoDrawable drawable) {
        GL gl = drawable.getGL();
        int clearBits = 0;
        if (hasDepth) {
            clearBits |= GL.GL_DEPTH_BUFFER_BIT;
        }
        if (!shouldPreserveColorBufferIfTranslucent()) {
            clearBits |= GL.GL_COLOR_BUFFER_BIT;
        }
        if (clearBits != 0) {
            gl.glClear(clearBits);
        }
        render3DScene(gl, glu);
    }

    public void reshape(GLAutoDrawable drawable,
                        int x, int y, int width, int height) {
        GL gl = drawable.getGL();

        if (!OPENGL_PIPELINE_WORK_AROUND) {
            gl.glViewport(0, 0, width, height);
        }
        gl.glMatrixMode(GL.GL_PROJECTION);
        gl.glLoadIdentity();
        if (hasDepth) {
            double aspectRatio = (double) width / (double) height;
            glu.gluPerspective(45.0, aspectRatio, 1.0, 400.0);
        } else {
            double aspectRatio = (double) width / (double) height;
            glu.gluPerspective(45.0, aspectRatio, 1.0, 400.0);
            //gl.glOrtho(0.0, width, height, 0.0, -100.0, 100.0);
        }

        gl.glMatrixMode(GL.GL_MODELVIEW);
        gl.glLoadIdentity();
    }

    public void displayChanged(GLAutoDrawable drawable,
                               boolean modeChanged, boolean deviceChanged) {
    }
}
