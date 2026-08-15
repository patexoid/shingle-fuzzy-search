package com.patex.shingle.config;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ConfigurationTest {

    @Test
    public void shouldLoadEnglishLangConfig() {
        LangConfig config = Configuration.getDefaultConfig().getLangConfig("eng");

        assertTrue(config.getSkipWords().contains("and"));
        assertEquals(5, config.getAverageWordLength());
    }

    @Test
    public void shouldFallBackToDefaultLangForUnknownLanguage() {
        LangConfig unknown = Configuration.getDefaultConfig().getLangConfig("klingon");
        LangConfig defaultLang = Configuration.getDefaultConfig().getLangConfig(null);

        assertEquals(defaultLang.getAverageWordLength(), unknown.getAverageWordLength());
        assertEquals(defaultLang.getDelimiters(), unknown.getDelimiters());
    }

    @Test
    public void shouldNormalizeToLowerCase() {
        LangConfig config = Configuration.getDefaultConfig().getLangConfig("eng");

        assertEquals("hello", config.normalize("HELLO"));
    }
}
