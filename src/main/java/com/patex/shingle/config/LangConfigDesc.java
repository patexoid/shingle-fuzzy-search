package com.patex.shingle.config;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

@Data
@Slf4j
public class LangConfigDesc {
    private String lang;
    private String langClass;
    private String langConfig;

    public LangConfig createLangConfig() {
        try {
            @SuppressWarnings("unchecked")
            Class<LangConfig> langConfigClass = (Class<LangConfig>) Class.forName(langClass);
            if (langConfig == null) {
                return langConfigClass.getConstructor().newInstance();
            }
            Yaml yaml = new Yaml(new Constructor(langConfigClass));
            try (InputStream confgIs = getConfigYaml()) {
                if (confgIs != null) {
                    log.error("Cant load lang config for lang {}, lang class: {} config: {}",
                            lang, langClass, langConfig);
                }
                return yaml.load(confgIs);
            } catch (IOException e) {
                log.error("Cant load lang config for lang {}, lang class: {} config: {}, reason: {}",
                        lang, langClass, langConfig, e.getMessage());
            }
        } catch (ReflectiveOperationException e) {
            log.error("Cant load config for lang {}, lang class: {} config: {}, reason: {}",
                    lang, langClass, langConfig, e.getMessage());

            log.debug(e.getMessage(), e);
        }
        return null;
    }

    private InputStream getConfigYaml() throws IOException {
        File fileConfig = new File(langConfig);
        if (fileConfig.exists()) {
            return new FileInputStream(fileConfig);
        }
        return this.getClass().getClassLoader().getResourceAsStream(langConfig);
    }
}
