package com.jnngl.translator;

public class MonkeTranslatorV1_1 extends MonkeTranslatorV1 {

    @Override
    public String getName() {
        return "MonkeLang v1.1";
    }

    @Override
    public String[] getDictionary() {
        return new String[] {
                "у", "У", "а", "А", "y", " ", "A", "a"
        };
    }
}
