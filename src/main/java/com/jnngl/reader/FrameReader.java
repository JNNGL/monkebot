/*
 * All Rights Reserved
 *
 * Copyright (c) 2023 JNNGL
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

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
