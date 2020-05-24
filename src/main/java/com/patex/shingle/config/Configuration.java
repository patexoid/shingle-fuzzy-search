package com.patex.shingle.config;


import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Collection;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@NoArgsConstructor
@Getter
@Setter
public class Configuration {

    public static final String SHINGLE_CONFIG = "shingle.config.file";

    public static final String DEFAULT_SHINGLE_CONFIG = "com/patex/shingle/config.yml";
    private static final Configuration defaultConfig;

    static {
        defaultConfig = loadDefault();
    }

    private Map<String, LangConfigDesc> langConfigsMap;
    private LangConfigDesc defaultLang;

    public Configuration(Collection<LangConfigDesc> langConfigs) {
        langConfigsMap = langConfigs.stream().collect(Collectors.toMap(LangConfigDesc::getLang, Function.identity()));
    }

    @SneakyThrows
    private static Configuration loadDefault() {
        String configFile = System.getProperty(SHINGLE_CONFIG);
        InputStream configIs = null;
        if (configFile != null) {
            if (new File(configFile).exists()) {
                configIs = new FileInputStream(configFile);
            } else {
                log.error("Can't load shingle config Config {} use default", configFile);
            }
        }
        if (configIs == null) {
            configIs = Configuration.class.getClassLoader().getResourceAsStream(DEFAULT_SHINGLE_CONFIG);
        }
        assert configIs != null;
        Yaml yaml = new Yaml(new Constructor(Configuration.class));
        try {
            return yaml.load(configIs);
        } finally {
            configIs.close();
        }
    }

    public static Configuration getDefaultConfig() {
        return defaultConfig;
    }

    public void setLangConfigs(Collection<LangConfigDesc> langConfigs) {
        langConfigsMap = langConfigs.stream().collect(Collectors.toMap(LangConfigDesc::getLang, Function.identity()));
    }

    public LangConfig getLangConfig(String lang) {
        return langConfigsMap.getOrDefault(lang, defaultLang).createLangConfig();
    }
}
