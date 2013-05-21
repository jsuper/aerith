package org.progx.jogl.rendering;
import java.awt.image.BufferedImage;

import javax.media.opengl.GL;

public class ReflectedQuad extends Quad {
    // reflection
    protected float fadeDistance = 0.8f;
    protected float reflectionTransparency = 3.8f;

    public ReflectedQuad(float x, float y, float z,
                         float width, float height,
                         BufferedImage textureImage) {
        super(x, y, z, width, height, textureImage);
    }
    
    public float getFadeDistance() {
        return fadeDistance;
    }

    public void setFadeDistance(float fadeDistance) {
        this.fadeDistance = fadeDistance;
    }

    public float getReflectionTransparency() {
        return reflectionTransparency;
    }

    public void setReflectionTransparency(float reflectionTransparency) {
        this.reflectionTransparency = reflectionTransparency;
    }

    // rendering
    @Override
    public void render(GL gl, boolean antiAliased) {
        float alpha = 1.0f;
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

        gl.glEnable(GL.GL_BLEND);
        if (!antiAliased) {
            gl.glBlendFunc(GL.GL_ONE, GL.GL_ONE_MINUS_SRC_ALPHA);
        } else {
            gl.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA);
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
        
        alpha /= reflectionTransparency;
        
        gl.glColor4f(antiAliased ? 1 : alpha, antiAliased ? 1 : alpha, antiAliased ? 1 : alpha, alpha);
        gl.glTexCoord2f(tx2, ty2);
        gl.glVertex3f(x + width, y, z);
        gl.glTexCoord2f(tx1, ty2);
        gl.glVertex3f(x, y, z);
        gl.glColor4f(0.0f, 0.0f, 0.0f, 0.0f);
        gl.glTexCoord2f(tx1, ty2 * (1 - fadeDistance));
        gl.glVertex3f(x, y - (height * fadeDistance), z);
        gl.glTexCoord2f(tx2, ty2 * (1 - fadeDistance));
        gl.glVertex3f(x + width, y - (height * fadeDistance), z);

        gl.glEnd();
        
        gl.glDisable(GL.GL_TEXTURE_2D);
        //gl.glDisable(GL.GL_BLEND);
    }
}