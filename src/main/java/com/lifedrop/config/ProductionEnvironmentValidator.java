package com.lifedrop.config;

import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Profiles;
import com.lifedrop.security.CorsOrigins;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

@Configuration(proxyBeanMethods = false)
public class ProductionEnvironmentValidator {
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
    private static final List<String> REQUIRED_VARIABLES = List.of(
        "SPRING_DATASOURCE_URL",
        "SPRING_DATASOURCE_USERNAME",
        "SPRING_DATASOURCE_PASSWORD",
        "JWT_SECRET",
        "APP_ADMIN_EMAIL",
        "APP_ADMIN_PASSWORD",
        "CORS_ALLOWED_ORIGINS"
    );

    @Bean
    static BeanFactoryPostProcessor validateProductionEnvironment(ConfigurableEnvironment environment) {
        return beanFactory -> validate(environment);
    }

    public static void validate(ConfigurableEnvironment environment) {
        if (!environment.acceptsProfiles(Profiles.of("production"))) {
            return;
        }

        List<String> missing = REQUIRED_VARIABLES.stream()
            .filter(name -> {
                String value = environment.getProperty(name);
                return value == null || value.isBlank();
            })
            .toList();
        if (!missing.isEmpty()) {
            throw new BeanDefinitionStoreException(
                "Missing required production environment variables: " + String.join(", ", missing));
        }

        String jwtSecret = environment.getProperty("JWT_SECRET");
        if (jwtSecret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new BeanDefinitionStoreException("JWT_SECRET must contain at least 32 UTF-8 bytes for HMAC signing");
        }

        String adminPassword = environment.getProperty("APP_ADMIN_PASSWORD");
        if (adminPassword.length() < 12) {
            throw new BeanDefinitionStoreException("APP_ADMIN_PASSWORD must contain at least 12 characters");
        }
        if (!EMAIL_PATTERN.matcher(environment.getProperty("APP_ADMIN_EMAIL")).matches()) {
            throw new BeanDefinitionStoreException("APP_ADMIN_EMAIL must contain a valid email address");
        }

        validateMysqlUrl(environment.getProperty("SPRING_DATASOURCE_URL"));
        try {
            CorsOrigins.parse(environment.getProperty("CORS_ALLOWED_ORIGINS"), true);
        } catch (IllegalArgumentException e) {
            throw new BeanDefinitionStoreException(e.getMessage());
        }
    }

    private static void validateMysqlUrl(String value) {
        try {
            if (value == null || !value.regionMatches(true, 0, "jdbc:mysql://", 0, "jdbc:mysql://".length())) {
                throw new IllegalArgumentException();
            }

            URI uri = new URI(value.substring("jdbc:".length()));
            String host = uri.getHost();
            String normalizedHost = host == null ? "" : host.toLowerCase(Locale.ROOT);
            if (!"mysql".equalsIgnoreCase(uri.getScheme())
                    || host == null
                    || uri.getPort() < 1
                    || uri.getPort() > 65535
                    || uri.getRawUserInfo() != null
                    || uri.getRawPath() == null
                    || uri.getRawPath().length() < 2
                    || normalizedHost.equals("localhost")
                    || normalizedHost.endsWith(".localhost")
                    || normalizedHost.equals("0.0.0.0")
                    || normalizedHost.equals("::1")
                    || normalizedHost.matches("127(?:\\.\\d{1,3}){3}")) {
                throw new IllegalArgumentException();
            }

            Set<String> secureModes = Set.of("verify_identity", "verify_ca");
            boolean usesVerifiedTls = uri.getRawQuery() != null
                && java.util.Arrays.stream(uri.getRawQuery().split("&"))
                    .map(parameter -> parameter.split("=", 2))
                    .anyMatch(pair -> pair.length == 2
                        && pair[0].equalsIgnoreCase("sslMode")
                        && secureModes.contains(pair[1].toLowerCase(Locale.ROOT)));
            if (!usesVerifiedTls) {
                throw new IllegalArgumentException();
            }
        } catch (URISyntaxException | IllegalArgumentException e) {
            throw new BeanDefinitionStoreException(
                "SPRING_DATASOURCE_URL must be a remote MySQL JDBC URL with a database, explicit port, "
                    + "and sslMode=VERIFY_IDENTITY or VERIFY_CA; credentials must be supplied separately");
        }
    }
}
