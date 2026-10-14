package com.lifedrop.security;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public final class CorsOrigins {
    private CorsOrigins() {}

    public static List<String> parse(String value, boolean requireHttps) {
        if (value == null || value.isBlank()) {
            throw invalidOrigins();
        }

        List<String> origins = Arrays.stream(value.split(",", -1))
            .map(String::trim)
            .toList();
        if (origins.stream().anyMatch(origin -> origin.isEmpty() || !isOrigin(origin, requireHttps))) {
            throw invalidOrigins();
        }
        return origins.stream().distinct().toList();
    }

    private static boolean isOrigin(String value, boolean requireHttps) {
        if (value.equals("*")) {
            return false;
        }
        try {
            URI uri = new URI(value);
            String scheme = uri.getScheme();
            String host = uri.getHost();
            if (scheme == null || host == null || uri.getRawUserInfo() != null
                    || uri.getRawPath() != null && !uri.getRawPath().isEmpty()
                    || uri.getRawQuery() != null || uri.getRawFragment() != null
                    || uri.getPort() == 0 || uri.getPort() > 65535) {
                return false;
            }
            if (requireHttps) {
                return scheme.equalsIgnoreCase("https")
                    && !isLocalHost(host);
            }
            return scheme.equalsIgnoreCase("https")
                || scheme.equalsIgnoreCase("http");
        } catch (URISyntaxException e) {
            return false;
        }
    }

    private static boolean isLocalHost(String host) {
        String normalized = host.toLowerCase(Locale.ROOT);
        return normalized.equals("localhost")
            || normalized.endsWith(".localhost")
            || normalized.equals("0.0.0.0")
            || normalized.equals("::1")
            || normalized.startsWith("127.");
    }

    private static IllegalArgumentException invalidOrigins() {
        return new IllegalArgumentException(
            "CORS_ALLOWED_ORIGINS must be a comma-separated list of explicit origin URLs without paths or '*'");
    }
}
