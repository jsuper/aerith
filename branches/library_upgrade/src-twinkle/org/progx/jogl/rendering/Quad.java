package org.progx.jogl.rendering;

import java.awt.Rectangle;
import java.awt.image.BufferedImage;

import javax.media.opengl.GL;

import org.progx.jogl.Texture;

public class Quad extends Renderable {
    // texture
    protected BufferedImage textureImage = null;
    protected Texture texture = null;
    protected Rectangle textureCrop = null;
    
    // geometry
    protected float width, height;
    
    // alpha
    protected float alpha = 1.0f;
    
    public Quad(float x, float y, float z,
                float width, float height,
                BufferedImage textureImage) {
        super(x, y, z);
        setDimension(width, height);
        setTextureImage(textureImage);
    }
    
    public void setDimension(float width, float height) {
        this.width = width;
        this.height = height;
    }
    
    public float getWidth() {
        return width;
    }
    
    public float getHeight() {
        return height;
    }
    
    public float getAlpha() {
        return alpha;
    }

    public void setAlpha(float alpha) {
        this.alpha = alpha;
    }

    public void setTextureImage(BufferedImage textureImage) {
        if (textureImage == null) {
            throw new IllegalArgumentException("Quad texture cannot be null.");
        }
        
        this.textureImage = textureImage;
        setTextureCrop(null);
    }
    
    public Rectangle getTextureCrop() {
        return textureCrop;
    }
    
    public void setTextureCrop(Rectangle textureCrop) {
        if (textureCrop == null) {
            textureCrop = new Rectangle(0, 0,
                                        textureImage.getWidth(),
                                        textureImage.getHeight());
        }
        
        this.textureCrop = textureCrop;
    }
    
    public void init(GL gl) {
        texture = Texture.getInstance(gl, textureImage, true);
    }
    
    public void dispose(GL gl) {
        texture.dispose(gl);
        textureImage = null;
    }
    
    // rendering
    public void render(GL gl, boolean antiAliased) {
        float[] crop = texture.getSubImageTextureCoords(textureCrop.x,
                                                        textureCrop.y,
                                                        textureCrop.x + textureCrop.width,
                                                        textureCrop.y + textureCrop.height);
        float tx1 = crop[0];
        float ty1 = crop[1];
        float tx2 = crop[2];
        float ty2 = crop[3];
        
        float x = -width / 2.0f;
        float y = -height / 2.0f;
        float z = 0.0f;
        
        if (alpha < 1.0f) {
            gl.glEnable(GL.GL_BLEND);
            if (!antiAliased) {
                gl.glBlendFunc(GL.GL_ONE, GL.GL_ONE_MINUS_SRC_ALPHA);
            } else {
                gl.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA);
            }
        }
        gl.glEnable(GL.GL_TEXTURE_2D);
        texture.bind(gl);
        gl.glTexEnvi(GL.GL_TEXTURE_ENV, GL.GL_TEXTURE_ENV_MODE, GL.GL_MODULATE);    

        gl.glBegin(GL.GL_QUADS);

        // render solid/upright texture
        gl.glColor4f(antiAliased ? 1 : alpha, antiAliased ? 1 : alpha, antiAliased ? 1 : alpha, alpha);
        gl.glTexCoord2f(tx2, ty1);
        gl.glVertex3f(x + width, y + height, z);
        gl.glTexCoord2f(tx1, ty1);
        gl.glVertex3f(x, y + height, z);
        gl.glTexCoord2f(tx1, ty2);
        gl.glVertex3f(x, y, z);
        gl.glTexCoord2f(tx2, ty2);
        gl.glVertex3f(x + width, y, z);
        
        gl.glEnd();
        
        gl.glDisable(GL.GL_TEXTURE_2D);
        if (alpha < 1.0f) {
            //gl.glDisable(GL.GL_BLEND);
        }
    }
}