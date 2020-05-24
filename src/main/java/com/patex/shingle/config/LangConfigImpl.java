package com.patex.shingle.config;

import lombok.Data;

import java.util.Collections;
import java.util.Set;

@Data
public class LangConfigImpl implements LangConfig {

    private Set<String> skipWords = Collections.emptySet();
    private int averageWordLength;
    private String delimiters;

    @Override
    public String normalize(String s) {
        return s.toLowerCase();
    }
}
