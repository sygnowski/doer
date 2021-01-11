package io.github.s7i.doer.command;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import java.io.File;
import java.io.IOException;

public interface YamlParser {

    File getYamlFile();

    default <T> T parseYaml(Class<T> clazz) {
        var objectMapper = new ObjectMapper(new YAMLFactory());
        try {
            return objectMapper.readValue(getYamlFile(), clazz);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
