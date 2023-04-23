package com.jnngl.reader;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URLConnection;

public class ImageIOFrameReader implements FrameReader {

    private BufferedImage image;

    public ImageIOFrameReader(URLConnection connection) throws IOException {
        image = ImageIO.read(connection.getInputStream());
    }

    @Override
    public int getWidth() {
        return image.getWidth();
    }

    @Override
    public int getHeight() {
        return image.getHeight();
    }

    @Override
    public float getFrameRate() {
        return 1;
    }

    @Override
    public BufferedImage readFrame() {
        try {
            return image;
        } finally {
            image = null;
        }
    }

    @Override
    public void close() {
    }
}
