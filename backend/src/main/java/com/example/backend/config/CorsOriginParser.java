package com.example.backend.config;

import java.net.URI;
import java.util.Arrays;
import java.util.List;

final class CorsOriginParser {
    private CorsOriginParser() {}

    static List<String> parse(String configuredOrigins) {
        List<String> origins = Arrays.stream(configuredOrigins.split(","))
                .map(String::trim)
                .filter(origin -> !origin.isEmpty())
                .map(CorsOriginParser::validate)
                .distinct()
                .toList();
        if (origins.isEmpty()) {
            throw new IllegalStateException("At least one CORS origin must be configured.");
        }
        return origins;
    }

    private static String validate(String origin) {
        try {
            URI uri = URI.create(origin);
            boolean validScheme = "http".equalsIgnoreCase(uri.getScheme())
                    || "https".equalsIgnoreCase(uri.getScheme());
            boolean validPath = uri.getRawPath() == null || uri.getRawPath().isEmpty()
                    || "/".equals(uri.getRawPath());
            if (!validScheme || uri.getHost() == null || uri.getUserInfo() != null
                    || uri.getQuery() != null || uri.getFragment() != null || !validPath
                    || origin.contains("*")) {
                throw new IllegalArgumentException();
            }
            return uri.getScheme() + "://" + uri.getRawAuthority();
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException("Invalid CORS origin: " + origin, exception);
        }
    }
}
