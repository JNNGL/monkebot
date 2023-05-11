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
