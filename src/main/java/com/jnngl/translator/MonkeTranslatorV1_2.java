package com.jnngl.translator;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

public class MonkeTranslatorV1_2 extends MonkeTranslatorV1_1 {

    @Override
    public String getName() {
        return "MonkeLang v1.2";
    }

    public byte[] compress(byte[] bytes) throws IOException {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Deflater deflater = new Deflater(Deflater.BEST_COMPRESSION, false);
            deflater.setInput(bytes);
            deflater.finish();

            byte[] output = new byte[1024];
            while (!deflater.finished()) {
                outputStream.write(output, 0, deflater.deflate(output));
            }

            deflater.end();
            return outputStream.toByteArray();
        }
    }

    @Override
    public byte[] getTextBytes(String text) {
        byte[] rawBytes = super.getTextBytes(text);
        try {
            byte[] compressed = compress(rawBytes);
            return compressed.length < rawBytes.length ? compressed : rawBytes;
        } catch (IOException e) {
            return rawBytes;
        }
    }

    public byte[] decompress(byte[] bytes) throws IOException, DataFormatException {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Inflater inflater = new Inflater(false);
            inflater.setInput(bytes);

            byte[] output = new byte[1024];
            while (!inflater.finished()) {
                outputStream.write(output, 0, inflater.inflate(output));
            }

            inflater.end();
            return outputStream.toByteArray();
        }
    }

    @Override
    public String getTextFromBytes(byte[] bytes) {
        byte[] decompressed;
        try {
            decompressed = decompress(bytes);
        } catch (IOException | DataFormatException e) {
            decompressed = bytes;
        }

        return super.getTextFromBytes(decompressed);
    }
}
