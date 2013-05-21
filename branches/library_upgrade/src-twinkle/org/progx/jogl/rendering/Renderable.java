package org.progx.jogl.rendering;

import javax.media.opengl.GL;
import javax.vecmath.Point3f;
import javax.vecmath.Point3i;

public abstract class Renderable {
    protected int rotationX, rotationY, rotationZ;
    protected float scaleX, scaleY, scaleZ;
    protected float x, y, z;
    protected String name;
    
    public Renderable() {
        this(0.0f, 0.0f, 0.0f);
    }
    
    public Renderable(float x, float y, float z) {
        setPosition(x, y, z);
        setRotation(0, 0, 0);
        setScale(1.0f, 1.0f, 1.0f);
    }
    
    public Renderable(float[] coordinates) {
        this(coordinates[0], coordinates[1], coordinates[2]);
    }
    
    public Renderable(Point3f pos) {
        this(pos.x, pos.y, pos.z);
    }

    public void init(GL gl) {
    }
    
    public void dispose(GL gl) {
    }

    public abstract void render(GL gl, boolean antiAliased);
    
    public void setPosition(float[] coordinates) {
        if (coordinates.length < 3) {
            throw new IllegalArgumentException("3 coordinates are required."); 
        }
        
        setPosition(coordinates[0], coordinates[1], coordinates[2]);
    }
    
    public void setPosition(Point3f pos) {
        setPosition(pos.x, pos.y, pos.z);        
    }
    
    public void setPosition(float x, float y, float z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }
    
    public Point3f getPosition() {
        return new Point3f(x, y, z);
    }
    
    public void setRotation(int x, int y, int z) {
        this.rotationX = x;
        this.rotationY = y;
        this.rotationZ = z;
    }
    
    public void setRotation(int[] coordinates) {
        if (coordinates.length < 3) {
            throw new IllegalArgumentException("3 coordinates are required."); 
        }
        
        setRotation(coordinates[0], coordinates[1], coordinates[2]);
    }
    
    public void setRotation(Point3i rot) {
        setRotation(rot.x, rot.y, rot.z);        
    }
    
    public Point3i getRotation() {
        return new Point3i(rotationX, rotationY, rotationZ);
    }
    
    public void setScale(float x, float y, float z) {
        this.scaleX = x;
        this.scaleY = y;
        this.scaleZ = z;
    }
    
    public void setScale(float[] coordinates) {
        if (coordinates.length < 3) {
            throw new IllegalArgumentException("3 coordinates are required."); 
        }
        
        setScale(coordinates[0], coordinates[1], coordinates[2]);
    }
    
    public void setScale(Point3f scale) {
        setScale(scale.x, scale.y, scale.z);        
    }
    
    public Point3f getScale() {
        return new Point3f(scaleX, scaleY, scaleZ);
    }
    
    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public String toString() {
        return name;
    }
}