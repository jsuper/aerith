package org.progx.jogl.util;

import java.util.Comparator;

import javax.media.opengl.GL;
import javax.vecmath.Matrix4f;
import javax.vecmath.Point3f;

import org.progx.jogl.rendering.Renderable;


public class DepthComparator implements Comparator<Renderable> {
    private Point3f camPos;
    private Matrix4f view;

    public DepthComparator(GL gl) {
        float[] matrix = new float[16];
        gl.glGetFloatv(GL.GL_MODELVIEW_MATRIX, matrix, 0);
        camPos = new Point3f(-matrix[12], -matrix[13], -matrix[14]);
        view = new Matrix4f(matrix);
        view.transpose();
    }

    public int compare(Renderable r1, Renderable r2) {
        if (r1 == null) {
            return -1;
        }
        if (r2 == null) {
            return 1;
        }
        if (r1 == r2) {
            return 0;
        }
        
        Point3f p1 = r1.getPosition();
        view.transform(p1);
        float distance1 = camPos.distance(p1);
        
        Point3f p2 = r2.getPosition();
        view.transform(p2);
        float distance2 = camPos.distance(p2);
        
        return (int) (distance2 - distance1);
    }
}
