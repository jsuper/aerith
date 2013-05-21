package org.progx.jogl;
import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.Transparency;
import java.awt.color.ColorSpace;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.ComponentColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import javax.imageio.ImageIO;
import javax.media.opengl.GL;

/**
 * REMIND: translucent images will have premultiplied comps by default...
 */
public class Texture {

    /** The GL target type */
    private int target;
    /** The GL texture ID */
    private int texID;
    /** The width of the texture */
    private int texWidth;
    /** The height of the texture */
    private int texHeight;
    /** The width of the image */
    private int imgWidth;
    /** The height of the image */
    private int imgHeight;
    /**
      * If true, indicates that the image contained in this texture has
      * premultiplied color components.
      */
    private boolean premult;

    /** REMIND */
    public float tx1, ty1, tx2, ty2;

    private static ColorModel rgbaColorModelPremult =
        new ComponentColorModel(ColorSpace.getInstance(ColorSpace.CS_sRGB),
                                new int[] {8, 8, 8, 8}, true, true, 
                                Transparency.TRANSLUCENT,
                                DataBuffer.TYPE_BYTE);
    private static ColorModel rgbaColorModelNonPremult =
        new ComponentColorModel(ColorSpace.getInstance(ColorSpace.CS_sRGB),
                                new int[] {8, 8, 8, 8}, true, false, 
                                Transparency.TRANSLUCENT,
                                DataBuffer.TYPE_BYTE);
    private static ColorModel rgbColorModel =
        new ComponentColorModel(ColorSpace.getInstance(ColorSpace.CS_sRGB),
                                new int[] {8, 8, 8, 0}, false, false,
                                Transparency.OPAQUE,
                                DataBuffer.TYPE_BYTE);

    private Texture(int target, int texID, int texWidth, int texHeight, boolean premult) {
        this.target = target;
        this.texID = texID;
        this.texWidth = texWidth;
        this.texHeight = texHeight;
        this.premult = premult;
    }

    // REMIND: document premult stuff
    public static Texture getInstance(GL gl, File f) throws IOException {
        return getInstance(gl, f, true);
    }

    public static Texture getInstance(GL gl, File f, boolean premult) throws IOException {
        BufferedImage img = ImageIO.read(f);
        return getInstance(gl, img, premult);
    }

    public static Texture getInstance(GL gl, BufferedImage image) {
        return getInstance(gl, image, true);
    }

    public static Texture getInstance(GL gl, BufferedImage image, boolean premult) {
        boolean texNonPow2 = false;
        boolean texRectangle = false;
        Texture tex = null;

        if (texNonPow2) {

        } else if (texRectangle) {

        } else {
            int pixelFormat = image.getColorModel().hasAlpha() ? GL.GL_RGBA
                                                              : GL.GL_RGB;
            int texWidth = getNextPowerOfTwo(image.getWidth());
            int texHeight = getNextPowerOfTwo(image.getHeight());
            tex = createTexture(gl, GL.GL_TEXTURE_2D, pixelFormat, texWidth,
                                texHeight, premult);
            tex.updateImage(gl, image);
        }
        return tex;
    }

    private static Texture createTexture(GL gl, int target, int pixelFormat,
                                         int texWidth, int texHeight, boolean premult) {
        // REMIND
        int minFilter = GL.GL_LINEAR;
        int magFilter = GL.GL_LINEAR;

        int texID = createTextureID(gl);
        gl.glBindTexture(target, texID);
        gl.glTexImage2D(target, 0, pixelFormat, texWidth, texHeight, 0,
                        pixelFormat, GL.GL_UNSIGNED_BYTE, null);

        // REMIND
        if (target == GL.GL_TEXTURE_2D) {
            gl.glTexParameteri(target, GL.GL_TEXTURE_MIN_FILTER, minFilter);
            gl.glTexParameteri(target, GL.GL_TEXTURE_MAG_FILTER, magFilter);
            gl.glTexParameteri(target, GL.GL_TEXTURE_WRAP_S,
                               GL.GL_CLAMP_TO_EDGE);
            gl.glTexParameteri(target, GL.GL_TEXTURE_WRAP_T,
                               GL.GL_CLAMP_TO_EDGE);
        }

        return new Texture(target, texID, texWidth, texHeight, premult);
    }

    /**
     * Creates a new texture ID.
     * 
     * @param gl
     *            the context used to create the texture object.
     * @return a new texture ID
     */
    private static int createTextureID(GL gl) {
        int[] tmp = new int[1];
        gl.glGenTextures(1, tmp, 0);
        return tmp[0];
    }

    /**
     * Returns the nearest power of two that is larger than the given value.
     * 
     * @param val
     *            the value
     * @return the next power of 2
     */
    private static int getNextPowerOfTwo(int val) {
        int ret = 1;
        while (ret < val) {
            ret *= 2;
        }
        return ret;
    }

    /**
     * REMIND
     */
    private static ByteBuffer convertToByteBuffer(BufferedImage image, boolean premult) {
        int width = image.getWidth();
        int height = image.getHeight();

        // create a temporary image that is compatible with OpenGL
        ColorModel cm;
        if (image.getColorModel().hasAlpha()) {
            cm = premult ? rgbaColorModelPremult : rgbaColorModelNonPremult;
        } else {
            cm = rgbColorModel;
        }
        WritableRaster raster = cm.createCompatibleWritableRaster(width, height);
        BufferedImage texImage = new BufferedImage(cm, raster, premult, null);

        // copy the source image into the temporary image
        Graphics2D g = texImage.createGraphics();
        g.setComposite(AlphaComposite.Src);
        g.drawImage(image, 0, 0, null);
        g.dispose();

        // build a byte buffer from the temporary image
        byte[] data = ((DataBufferByte) texImage.getRaster().getDataBuffer())
                                                                             .getData();
        ByteBuffer imageBuffer = ByteBuffer.allocateDirect(data.length);
        imageBuffer.order(ByteOrder.nativeOrder());
        imageBuffer.put(data, 0, data.length);
        imageBuffer.rewind();

        return imageBuffer;
    }

    /**
     * Binds this texture to the specified GL context.
     * 
     * @param gl
     *            the GL context to bind to
     */
    public void bind(GL gl) {
        gl.glBindTexture(target, texID);
    }

    /**
     * Returns the texture coordinates corresponding to a subregion of the image
     * stored in this texture, as specified by the given bounding box. (yadda
     * yadda yadda)
     */
    public float[] getSubImageTextureCoords(int x1, int y1, int x2, int y2) {
        float[] coords = new float[4];
        // REMIND: different approach needed for GL_ARB_texture_rectangle
        coords[0] = (float) x1 / (float) texWidth;
        coords[1] = (float) y1 / (float) texHeight;
        coords[2] = (float) x2 / (float) texWidth;
        coords[3] = (float) y2 / (float) texHeight;
        return coords;
    }

    /**
     * REMIND
     */
    public void updateImage(GL gl, BufferedImage image) {
        // REMIND: we should ensure that the new image dimensions are not
        // larger than the current texture dimensions (or perhaps we
        // could clip the incoming image to the texture dimensions if the
        // image is larger?)
        imgWidth = image.getWidth();
        imgHeight = image.getHeight();
        setImageSize(imgWidth, imgHeight);

        int pixelFormat = image.getColorModel().hasAlpha() ? GL.GL_RGBA
                                                          : GL.GL_RGB;
        ByteBuffer imageBuffer = convertToByteBuffer(image, premult);
        bind(gl);
        gl.glPixelStorei(GL.GL_UNPACK_ALIGNMENT, pixelFormat == GL.GL_RGBA ? 4
                                                                          : 1);
        gl.glTexSubImage2D(target, 0, 0, 0, imgWidth, imgHeight, pixelFormat,
                           GL.GL_UNSIGNED_BYTE, imageBuffer);
    }

    /**
     * Disposes the native resources used by this texture object.
     * 
     * @param gl
     *            the GL context to use for disposing the texture object
     */
    public void dispose(GL gl) {
        gl.glDeleteTextures(1, new int[] { texID }, 0);
    }

    /**
     * REMIND
     */
    public int getTarget() {
        return target;
    }

    /**
     * Returns the width of the texture.
     * 
     * @return the width of the texture
     */
    public int getWidth() {
        return texWidth;
    }

    /**
     * Returns the height of the texture.
     * 
     * @return the height of the texture
     */
    public int getHeight() {
        return texHeight;
    }

    /**
     * Returns the width of the original image.
     * 
     * @return the width of the original image
     */
    public int getImageWidth() {
        return imgWidth;
    }

    /**
     * Returns the height of the original image.
     * 
     * @return the height of the original image
     */
    public int getImageHeight() {
        return imgHeight;
    }

    /**
     * Returns true if the image contained in this texture has premultiplied
     * color components.
     */
     public boolean isAlphaPremultiplied() {
         return premult;
     }

    /**
     * Updates the actual image dimensions (usually only called from
     * <code>updateImage</code>.
     */
    private void setImageSize(int width, int height) {
        imgWidth = width;
        imgHeight = height;
        tx1 = 0.0f;
        ty1 = 0.0f;
        tx2 = (float) imgWidth / (float) texWidth;
        ty2 = (float) imgHeight / (float) texHeight;
    }
}
