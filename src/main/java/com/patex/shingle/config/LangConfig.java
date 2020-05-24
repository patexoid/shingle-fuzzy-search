package com.patex.shingle.config;

public interface LangConfig {

    String normalize(String s);

    java.util.Set<String> getSkipWords();

    int getAverageWordLength();

    String getDelimiters();
}
