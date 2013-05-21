package org.progx.jogl.rendering;

import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;

import javax.imageio.ImageIO;

public class RenderableFactory {
    public static Renderable createBillboard(Renderable item) {
        return new Billboard(item);
    }
    
    public static Renderable createReflectedQuad(float x, float y, float z,
                                                 float w, float h,
                                                 URL texture, Rectangle crop) {
        try {
            BufferedImage image = ImageIO.read(texture);
            return createReflectedQuad(x, y, z, w, h, image, crop);
        } catch (IOException e) {
        }
        
        return null;
    }

    public static Renderable createReflectedQuad(float x, float y, float z,
                                                 float w, float h,
                                                 URL texture, Rectangle crop,
                                                 String name) {
        Renderable quad = createReflectedQuad(x, y, z, w, h, texture, crop);
        ((ReflectedQuad) quad).setName(name);
        return quad;
    }
    
    public static Renderable createReflectedQuad(float x, float y, float z,
                                                 float w, float h,
                                                 BufferedImage texture, Rectangle crop) {
        ReflectedQuad quad = new ReflectedQuad(x, y, z, w, h, texture);
        quad.setTextureCrop(crop);
        return quad;
    }

    public static Renderable createReflectedQuad(float x, float y, float z,
                                                 float w, float h,
                                                 BufferedImage texture, Rectangle crop,
                                                 String name) {
        Renderable quad = createReflectedQuad(x, y, z, w, h, texture, crop);
        quad.setName(name);
        return quad;
    }
    
    public static Renderable createQuad(float x, float y, float z,
                                        float w, float h,
                                        URL texture, Rectangle crop) {
        try {
            BufferedImage image = ImageIO.read(texture);
            return createQuad(x, y, z, w, h, image, crop);
        } catch (IOException e) {
        }
        
        return null;
    }
    
    public static Renderable createQuad(float x, float y, float z,
                                        float w, float h,
                                        URL texture, Rectangle crop,
                                        String name) {
        Renderable quad = createQuad(x, y, z, w, h, texture, crop);
        quad.setName(name);
        return quad;
    }
    
    public static Renderable createQuad(float x, float y, float z,
                                        float w, float h,
                                        BufferedImage texture, Rectangle crop) {
        Quad quad = new Quad(x, y, z, w, h, texture);
        quad.setTextureCrop(crop);
        return quad;
    }

    public static Renderable createQuad(float x, float y, float z,
                                                 float w, float h,
                                                 BufferedImage texture, Rectangle crop,
                                                 String name) {
        Quad quad = new Quad(x, y, z, w, h, texture);
        quad.setTextureCrop(crop);
        quad.setName(name);
        return quad;
    }
}
