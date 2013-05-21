package com.sun.javaone.aerith.ui.music;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import javazoom.jl.decoder.Bitstream;
import javazoom.jl.decoder.BitstreamException;
import javazoom.jl.decoder.Decoder;
import javazoom.jl.decoder.Header;
import javazoom.jl.decoder.JavaLayerException;
import javazoom.jl.decoder.SampleBuffer;
import javazoom.jl.player.AudioDevice;
import javazoom.jl.player.FactoryRegistry;

public class MusicPlayer {
    /**
     * The current frame number.
     *
     * @noinspection UNUSED_SYMBOL
     */
    private int frame = 0;
    /**
     * The MPEG audio bitstream.
     */
    // javac blank final bug.
    /*final*/ private Bitstream bitstream;
    /**
     * The MPEG audio decoder.
     */
    /*final*/ private Decoder decoder;
    /**
     * The AudioDevice the audio samples are written to.
     */
    private AudioDevice audio;
    /**
     * Has the player been closed?
     */
    private boolean closed = false;
    /**
     * Has the player played back all frames from the stream?
     */
    private boolean complete = false;
    private int lastPosition = 0;

    /**
     * Creates a new <code>Player</code> instance.
     */
    public MusicPlayer(InputStream stream) throws JavaLayerException {
        this(stream, null);
    }

    public MusicPlayer(InputStream stream, AudioDevice device)
            throws JavaLayerException {
        bitstream = new Bitstream(stream);
        decoder = new Decoder();
        if (device != null) {
            audio = device;
        } else {
            FactoryRegistry r = FactoryRegistry.systemRegistry();
            audio = r.createAudioDevice();
        }
        audio.open(decoder);
    }

    public void play() throws JavaLayerException {
        play(Integer.MAX_VALUE);
    }

    /**
     * Plays a number of MPEG audio frames.
     *
     * @param frames The number of frames to play.
     *
     * @return true if the last frame was played, or false if there are
     * more frames.
     * @noinspection BooleanMethodNameMustStartWithQuestion
     */
    public boolean play(int frames) throws JavaLayerException {
        boolean ret = true;
        while (frames-- > 0 && ret) {
            ret = decodeFrame();
        }
        if (!ret) {
            // last frame, ensure all data flushed to the audio device.
            AudioDevice out = audio;
            if (out != null) {
                out.flush();
                synchronized (this) {
                    complete = (!closed);
                    stop();
                }
            }
        }
        return ret;
    }

    /**
     * Cloases this player. Any audio currently playing is stopped
     * immediately.
     */
    public synchronized void stop() {
        AudioDevice out = audio;
        if (out != null) {
            closed = true;
            audio = null;
            // this may fail, so ensure object state is set up before
            // calling this method.
            out.close();
            lastPosition = out.getPosition();
            //noinspection EmptyCatchBlock
            try {
                bitstream.close();
            }
            catch (BitstreamException ex) {
            }
        }
    }

    /**
     * Returns the completed status of this player.
     *
     * @return true if all available MPEG audio frames have been
     * decoded, or false otherwise.
     */
    public synchronized boolean isComplete() {
        return complete;
    }

    /**
     * Retrieves the position in milliseconds of the current audio
     * sample being played. This method delegates to the <code>
     * AudioDevice</code> that is used by this player to sound
     * the decoded audio samples.
     */
    public int getPosition() {
        int position = lastPosition;
        AudioDevice out = audio;
        if (out != null) {
            position = out.getPosition();
        }
        return position;
    }

    /**
     * Decodes a single frame.
     *
     * @return true if there are no more frames to decode, false otherwise.
     *
     * @noinspection BooleanMethodNameMustStartWithQuestion
     */
    protected boolean decodeFrame() throws JavaLayerException {
        try {
            AudioDevice out = audio;
            if (out == null) {
                return false;
            }
            Header h = bitstream.readFrame();
            if (h == null) {
                return false;
            }

            // sample buffer set when decoder constructed
            SampleBuffer output =
                    (SampleBuffer) decoder.decodeFrame(h, bitstream);
            synchronized (this) {
                out = audio;
                if (out != null) {
                    out.write(output.getBuffer(), 0, output.getBufferLength());
                }
            }
            bitstream.closeFrame();
        }
        catch (RuntimeException ex) {
            throw new JavaLayerException("Exception decoding audio frame", ex);
        }
        return true;
    }

    public static void main(String[] args) {
        final MusicPlayer player = getMusicPlayer("./music/theme.mp3");
        new Thread(new Runnable() {
            public void run() {
                try {
                    player.play();
                } catch (JavaLayerException e) {
                    e.printStackTrace();
                }
            }
        }).start();
        try {
            Thread.sleep(10000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        player.stop();
    }

    public static MusicPlayer getMusicPlayer(String fileName) {
        try {
            InputStream in = getURLInputStream(fileName);
            AudioDevice dev = getAudioDevice();
            return new MusicPlayer(in, dev);
        } catch (IOException ex) {
            System.err.println("Problem playing file " + fileName);
            ex.printStackTrace();
        } catch (Exception ex) {
            System.err.println("Problem playing file " + fileName);
            ex.printStackTrace();
        }
        return null;
    }

    private static AudioDevice getAudioDevice() throws JavaLayerException {
        return FactoryRegistry.systemRegistry().createAudioDevice();
    }

    private static InputStream getURLInputStream(String fileName)
            throws Exception {
        return new BufferedInputStream(new FileInputStream(new File(fileName)));
    }
}
