package com.jnngl.translator;

public interface MonkeTranslator {

    String getName();

    String translateToMonke(String text);

    String translateFromMonke(String text);
}
