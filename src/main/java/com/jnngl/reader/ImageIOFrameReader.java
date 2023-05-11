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
