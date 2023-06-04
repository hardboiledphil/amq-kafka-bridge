package com.hardboiled.phil.amqkafkabridge.amqkafkabridge;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Optional;
import java.util.stream.Collectors;

public class ResourceReader {

    public static String readResourceToString(final String resourcePath) {
        var normalisedPath = resourcePath.startsWith("/") ? resourcePath : "/" + resourcePath;
        return Optional.ofNullable(ResourceReader.class.getResourceAsStream(normalisedPath))
                .map(InputStreamReader::new)
                .map(BufferedReader::new)
                .map(reader -> reader.lines().collect(Collectors.joining(System.lineSeparator())))
                .orElseThrow(() ->
                        new IllegalArgumentException("Resource '" + normalisedPath + "' does not exist in classpath"));
    }
}
