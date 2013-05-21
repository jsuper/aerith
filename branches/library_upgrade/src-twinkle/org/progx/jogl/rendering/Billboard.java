package org.progx.jogl.rendering;

import javax.media.opengl.GL;
import javax.vecmath.Point3f;
import javax.vecmath.Point3i;
import javax.vecmath.Vector3f;

import org.progx.jogl.GLUtilities;

public class Billboard extends Renderable {
    private Renderable item;

    public Billboard(Renderable item) {
        this.item = item;
        setPosition(0.0f, 0.0f, 0.0f);
        setScale(1.0f, 1.0f, 1.0f);
        setRotation(0, 0, 0);
    }

    public void init(GL gl) {
        item.init(gl);
    }

    public Renderable getItem() {
        return item;
    }

    public void setItem(Renderable item) {
        this.item = item;
    }

    public void render(GL gl, boolean antiAliased) {
        Vector3f camPos = new Vector3f();
        Vector3f camUp = new Vector3f();
        GLUtilities.getCameraVectors(gl, camPos, camUp);
        gl.glPushMatrix();
        GLUtilities.renderBillboard(gl, camPos, camUp, item, antiAliased);
        gl.glPopMatrix();
    }

    public Point3f getPosition() {
        return item.getPosition();
    }

    public void setPosition(Point3f position) {
        if (item == null) {
            return;
        }
        item.setPosition(position);
    }
    
    public void setPosition(float x, float y, float z) {
        if (item == null) {
            return;
        }
        item.setPosition(x, y, z);
    }
    
    public void setPosition(float[] coordinates) {
        if (item == null) {
            return;
        }
        item.setPosition(coordinates);
    }

    public Point3i getRotation() {
        return new Point3i(0, 0, 0);
    }

    public Point3f getScale() {
        return item.getScale();
    }

    public void setRotation(int x, int y, int z) {
    }

    public void setRotation(int[] coordinates) {
    }

    public void setRotation(Point3i rot) {
    }

    public void setScale(float x, float y, float z) {
        if (item == null) {
            return;
        }
        item.setScale(x, y, z);
    }

    public void setScale(float[] coordinates) {
        if (item == null) {
            return;
        }
        item.setScale(coordinates);
    }

    public void setScale(Point3f scale) {
        if (item == null) {
            return;
        }
        item.setScale(scale);
    }
}
