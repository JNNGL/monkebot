package com.jnngl.reader;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;

public interface FrameReader extends AutoCloseable {

    List<String> IIO_MIME_TYPES = List.of("image/png", "image/jpeg", "image/bmp");

    int getWidth();

    int getHeight();

    float getFrameRate();

    BufferedImage readFrame() throws IOException;

    static FrameReader createFrameReader(URL url) throws IOException {
        URLConnection connection = url.openConnection();
        if (IIO_MIME_TYPES.contains(connection.getContentType())) {
            return new ImageIOFrameReader(connection);
        } else {
            return new FFmpegFrameReader(connection);
        }
    }
}
