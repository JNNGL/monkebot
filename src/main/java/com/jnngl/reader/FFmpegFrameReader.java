package com.jnngl.reader;

import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.FrameConverter;
import org.bytedeco.javacv.Java2DFrameConverter;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URLConnection;

public class FFmpegFrameReader implements FrameReader {

    private final FFmpegFrameGrabber grabber;
    private final FrameConverter<BufferedImage> frameConverter;

    public FFmpegFrameReader(URLConnection connection) throws IOException {
        this.frameConverter = new Java2DFrameConverter();
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
        return frameConverter.convert(grabber.grabFrame(false, true, true, false, false));
    }

    @Override
    public void close() throws Exception {
        grabber.stop();
        grabber.close();
        frameConverter.close();
    }
}
