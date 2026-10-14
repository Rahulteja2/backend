package com.lifedrop.config;

import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Profiles;
import java.util.List;

@Configuration(proxyBeanMethods = false)
public class ProductionEnvironmentValidator {
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
        return beanFactory -> {
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
        };
    }
}
