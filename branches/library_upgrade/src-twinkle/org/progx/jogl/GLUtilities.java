package org.progx.jogl;
import javax.media.opengl.GL;
import javax.vecmath.Matrix4f;
import javax.vecmath.Point3f;
import javax.vecmath.Vector3f;

import org.progx.jogl.rendering.Renderable;

public class GLUtilities {
    public static void drawLocalAxis(GL gl, float axisLength) {
        Vector3f x = new Vector3f(1.0f, 0.0f, 0.0f);
        Vector3f y = new Vector3f(0.0f, 1.0f, 0.0f);
        Vector3f z = new Vector3f(0.0f, 0.0f, 1.0f);
        
        gl.glLineWidth(2);
        gl.glBegin(GL.GL_LINES);
        gl.glColor3f(1, 0, 0);
        gl.glVertex3f(0, 0, 0);
        gl.glVertex3f(x.x * axisLength, x.y * axisLength, x.z * axisLength);
    
        gl.glColor3f(0, 1, 0);
        gl.glVertex3f(0, 0, 0);
        gl.glVertex3f(y.x * axisLength, y.y * axisLength, y.z * axisLength);
    
        gl.glColor3f(0, 0, 1);
        gl.glVertex3f(0, 0, 0);
        gl.glVertex3f(z.x * axisLength, z.y * axisLength, z.z * axisLength);
        gl.glEnd();
    
        gl.glPopAttrib();
    }
    
    public static void renderBillboard(GL gl,  Vector3f camPos, Vector3f camUp,
                                       Renderable item, boolean antiAliased) {
        Point3f pos = item.getPosition();
        
        Vector3f look = new Vector3f();
        Vector3f right = new Vector3f();
        Vector3f up = new Vector3f();

        look.sub(camPos, pos);
        look.normalize();

        right.cross(camUp, look);
        up.cross(look, right);

        gl.glMultMatrixf(new float[] { right.x, right.y, right.z, 0.0f,
                                       up.x, up.y, up.z, 0.0f,
                                       look.x, look.y, look.z, 0.0f,
                                       pos.x, pos.y, pos.z, 1 }, 0);
        
        item.render(gl, antiAliased);
    }

    public static void getCameraVectors(GL gl, Vector3f camPos, Vector3f camUp) {
        float[] matrix = new float[16];
        gl.glGetFloatv(GL.GL_MODELVIEW_MATRIX, matrix, 0);
    
        camPos.set(new float[] { -matrix[12], -matrix[13], -matrix[14] });
        camUp.set(new float[] { matrix[1], matrix[5], matrix[9] });
        
        matrix[12] = matrix[13] = matrix[14] = 0;
        Matrix4f view = new Matrix4f(matrix);
        view.transform(camPos);
    }
    
    public static void renderAntiAliased(GL gl, Renderable scene) {
        gl.glEnable(GL.GL_BLEND);
        gl.glEnable(GL.GL_POLYGON_SMOOTH);
        gl.glHint(GL.GL_PERSPECTIVE_CORRECTION_HINT, GL.GL_NICEST);
        gl.glHint(GL.GL_POLYGON_SMOOTH_HINT, GL.GL_NICEST);
        gl.glDisable(GL.GL_DEPTH);
        
        scene.render(gl, true);
    }

    public static void setFrustum(GL gl,
                                  double left, double right, double bottom, 
                                  double top, double near, double far, double pixdx, 
                                  double pixdy, double eyedx, double eyedy, double focus) {
        int[] viewport = new int[4];
        gl.glGetIntegerv(GL.GL_VIEWPORT, viewport, 0);
        
        double xwsize = right - left;
        double ywsize = top - bottom;
        
        double dx = -(pixdx * xwsize / (double) viewport[2] + eyedx * near / focus);
        double dy = -(pixdy * ywsize / (double) viewport[3] + eyedy * near / focus);
        
        gl.glMatrixMode(GL.GL_PROJECTION);
        gl.glLoadIdentity();
        gl.glFrustum(left + dx, right + dx, bottom + dy, top + dy, near, far);
        gl.glMatrixMode(GL.GL_MODELVIEW);
        gl.glLoadIdentity();
        gl.glTranslatef((float) -eyedx, (float) -eyedy, 0.0f); 
    }
    
    public static void setPerspective(GL gl,
                                      double fovy, double aspect, 
                                      double near, double far, double pixdx, double pixdy, 
                                      double eyedx, double eyedy, double focus) {
        double fov2 = ((fovy * Math.PI) / 180.0) / 2.0;
        
        double top = near / (Math.cos(fov2) / Math.sin(fov2));
        double bottom = -top;
        
        double right = top * aspect;
        double left = -right;
        
        setFrustum(gl,
                   left, right, bottom, top, near, far,
                   pixdx, pixdy, eyedx, eyedy, focus);
    } 
}
