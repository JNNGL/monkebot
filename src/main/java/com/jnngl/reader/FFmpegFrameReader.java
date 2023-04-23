package com.jnngl.reader;

import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.FrameConverter;
import org.bytedeco.javacv.Java2DFrameConverter;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URLConnection;

public class FFmpegFrameReader implements FrameReader {

    private static final FrameConverter<BufferedImage> FRAME_CONVERTER = new Java2DFrameConverter();

    private final FFmpegFrameGrabber grabber;

    public FFmpegFrameReader(URLConnection connection) throws IOException {
        grabber = new FFmpegFrameGrabber(connection.getInputStream());
        grabber.start();
    }

    @Override
    public int getWidth() {
        return grabber.getImageWidth();
    }

    @Override
    public int getHeight() {
        return grabber.getImageHeight();
    }

    @Override
    public float getFrameRate() {
        return (float) grabber.getFrameRate();
    }

    @Override
    public BufferedImage readFrame() throws IOException {
        return FRAME_CONVERTER.convert(grabber.grabFrame(false, true, true, false, false));
    }

    @Override
    public void close() throws Exception {
        grabber.stop();
        grabber.close();
    }
}
