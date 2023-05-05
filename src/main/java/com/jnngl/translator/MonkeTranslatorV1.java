package com.jnngl.translator;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;

public class MonkeTranslatorV1 implements MonkeTranslator {

    @Override
    public String getName() {
        return "MonkeLang v1.0";
    }

    public int monkeHash(String text) {
        return Byte.toUnsignedInt((byte) text.hashCode());
    }

    public String monkeEncodeHash(int hash) {
        return Integer.toUnsignedString(hash, 4)
                .replace('0', 'у')
                .replace('1', 'У')
                .replace('2', 'а')
                .replace('3', 'А');
    }

    public String getSeparator() {
        return " ";
    }

    public String getPrefix() {
        return "#";
    }

    public String getEnding() {
        return "а";
    }

    public String[] getDictionary() {
        return new String[] {
                "у", "У", "а", "А", " "
        };
    }

    public String getRegex() {
        String[] dictionary = getDictionary();
        StringBuilder regex = new StringBuilder("^[");
        for (String s : dictionary) {
            regex.append(s);
        }

        return regex + "]+$";
    }

    @Override
    public String translateToMonke(String text) {
        String[] dictionary = getDictionary();
        StringBuilder monkeBuilder = new StringBuilder();

        BigInteger integer = new BigInteger(1, (getPrefix() + text).getBytes(StandardCharsets.UTF_8));
        String monkeText = integer.toString(dictionary.length);
        monkeBuilder.append(monkeEncodeHash(monkeHash(monkeText)));
        monkeBuilder.append(getSeparator());
        monkeBuilder.append(monkeText);
        monkeBuilder.append(getEnding());

        String result = monkeBuilder.toString();
        for (int i = 0; i < dictionary.length; i++) {
            result = result.replace(Integer.toString(i), dictionary[i]);
        }

        return result;
    }

    public String prepareForTranslation(String text) {
        if (!text.matches(getRegex())) {
            return null;
        }

        String ending = getEnding();
        if (!text.endsWith(ending)) {
            return null;
        }

        int index = text.lastIndexOf(ending);
        text = new StringBuilder(text)
                .replace(index, index + ending.length(), "")
                .toString();

        return text;
    }

    public int monkeDecodeHash(String hash) {
        hash = hash
                .replace('у', '0')
                .replace('У', '1')
                .replace('а', '2')
                .replace('А', '3');
        return Integer.parseUnsignedInt(hash, 4);
    }

    @Override
    public String translateFromMonke(String text) {
        if ((text = prepareForTranslation(text)) == null) {
            return null;
        }

        String separator = getSeparator();
        if (!text.contains(separator)) {
            return null;
        }

        String[] parts = text.split(separator, 2);

        text = parts[1];
        String[] dictionary = getDictionary();
        for (int i = 0; i < dictionary.length; i++) {
            text = text.replace(dictionary[i], Integer.toString(i));
        }

        if (monkeHash(text) != monkeDecodeHash(parts[0])) {
            return null;
        }


        BigInteger integer = new BigInteger(text, dictionary.length);
        String translated = new String(integer.toByteArray(), StandardCharsets.UTF_8);
        String prefix = getPrefix();
        if (!translated.startsWith(prefix)) {
            return null;
        }

        return translated.substring(prefix.length());
    }
}
