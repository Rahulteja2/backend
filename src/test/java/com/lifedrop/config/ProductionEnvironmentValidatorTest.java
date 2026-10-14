package com.lifedrop.config;

import com.lifedrop.security.CorsOrigins;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProductionEnvironmentValidatorTest {
    private static final String VALID_JDBC_URL =
        "jdbc:mysql://mysql.example.test:3306/lifedrop?sslMode=VERIFY_IDENTITY&serverTimezone=UTC";

    @Test
    void productionAcceptsTheExactConfiguredVariableNames() {
        MockEnvironment environment = validProductionEnvironment();

        ProductionEnvironmentValidator.validate(environment);
    }

    @Test
    void productionDoesNotFallBackToLocalDatasourceSettings() {
        MockEnvironment environment = new MockEnvironment()
            .withProperty("JWT_SECRET", "test-only-secret-at-least-32-utf8-bytes")
            .withProperty("APP_ADMIN_EMAIL", "admin@example.test")
            .withProperty("APP_ADMIN_PASSWORD", "test-only-admin-password")
            .withProperty("CORS_ALLOWED_ORIGINS", "https://frontend.example.test");
        environment.setActiveProfiles("production");

        assertThatThrownBy(() -> ProductionEnvironmentValidator.validate(environment))
            .isInstanceOf(BeanDefinitionStoreException.class)
            .hasMessageContaining("SPRING_DATASOURCE_URL")
            .hasMessageContaining("SPRING_DATASOURCE_USERNAME")
            .hasMessageContaining("SPRING_DATASOURCE_PASSWORD");
    }

    @Test
    void productionReportsMissingVariablesByNameWithoutEchoingValues() {
        MockEnvironment environment = new MockEnvironment()
            .withProperty("SPRING_DATASOURCE_URL", VALID_JDBC_URL)
            .withProperty("SPRING_DATASOURCE_USERNAME", "test-user")
            .withProperty("SPRING_DATASOURCE_PASSWORD", "test-password")
            .withProperty("JWT_SECRET", "test-only-secret-at-least-32-utf8-bytes");
        environment.setActiveProfiles("production");

        assertThatThrownBy(() -> ProductionEnvironmentValidator.validate(environment))
            .isInstanceOf(BeanDefinitionStoreException.class)
            .hasMessageContaining("APP_ADMIN_EMAIL")
            .hasMessageContaining("APP_ADMIN_PASSWORD")
            .hasMessageContaining("CORS_ALLOWED_ORIGINS")
            .hasMessageNotContaining("test-password")
            .hasMessageNotContaining("test-only-secret");
    }

    @Test
    void productionRejectsShortJwtSecrets() {
        MockEnvironment environment = validProductionEnvironment()
            .withProperty("JWT_SECRET", "too-short");

        assertThatThrownBy(() -> ProductionEnvironmentValidator.validate(environment))
            .isInstanceOf(BeanDefinitionStoreException.class)
            .hasMessageContaining("JWT_SECRET")
            .hasMessageContaining("32 UTF-8 bytes");
    }

    @Test
    void jwtServiceRejectsShortHmacKeysBeforeSigning() {
        assertThatThrownBy(() -> new com.lifedrop.security.JwtService("weak", 60_000))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("32 UTF-8 bytes")
            .hasMessageNotContaining("weak");
        new com.lifedrop.security.JwtService("a".repeat(32), 60_000);
    }

    @Test
    void productionRejectsLocalAndTlsDisabledMysqlUrls() {
        MockEnvironment localUrl = validProductionEnvironment()
            .withProperty(
                "SPRING_DATASOURCE_URL",
                "jdbc:mysql://localhost:3306/lifedrop?sslMode=VERIFY_IDENTITY");
        MockEnvironment disabledTls = validProductionEnvironment()
            .withProperty(
                "SPRING_DATASOURCE_URL",
                "jdbc:mysql://mysql.example.test:3306/lifedrop?sslMode=DISABLED");

        assertThatThrownBy(() -> ProductionEnvironmentValidator.validate(localUrl))
            .isInstanceOf(BeanDefinitionStoreException.class)
            .hasMessageContaining("SPRING_DATASOURCE_URL");
        assertThatThrownBy(() -> ProductionEnvironmentValidator.validate(disabledTls))
            .isInstanceOf(BeanDefinitionStoreException.class)
            .hasMessageContaining("sslMode");
    }

    @Test
    void productionAcceptsCertificateAuthorityVerifiedMysqlTls() {
        MockEnvironment environment = validProductionEnvironment()
            .withProperty(
                "SPRING_DATASOURCE_URL",
                "jdbc:mysql://mysql.example.test:3306/lifedrop?sslMode=VERIFY_CA");

        ProductionEnvironmentValidator.validate(environment);
    }

    @Test
    void productionRequiresValidHttpsOriginsAndSupportsMultipleOrigins() {
        MockEnvironment environment = validProductionEnvironment()
            .withProperty("CORS_ALLOWED_ORIGINS", "https://frontend.example.test, https://admin.example.test");
        assertThatCode(() -> ProductionEnvironmentValidator.validate(environment)).doesNotThrowAnyException();
        assertThat(CorsOrigins.parse(
            "https://frontend.example.test, https://admin.example.test,https://frontend.example.test", true))
            .containsExactly("https://frontend.example.test", "https://admin.example.test");

        environment.setProperty("CORS_ALLOWED_ORIGINS", "https://frontend.example.test,*");
        assertThatThrownBy(() -> ProductionEnvironmentValidator.validate(environment))
            .isInstanceOf(BeanDefinitionStoreException.class)
            .hasMessageContaining("CORS_ALLOWED_ORIGINS");
    }

    @Test
    void productionRejectsHttpAndLocalCorsOrigins() {
        MockEnvironment localOrigin = validProductionEnvironment()
            .withProperty("CORS_ALLOWED_ORIGINS", "http://frontend.example.test");
        MockEnvironment loopbackOrigin = validProductionEnvironment()
            .withProperty("CORS_ALLOWED_ORIGINS", "https://127.0.0.1");

        assertThatThrownBy(() -> ProductionEnvironmentValidator.validate(localOrigin))
            .isInstanceOf(BeanDefinitionStoreException.class)
            .hasMessageContaining("CORS_ALLOWED_ORIGINS");
        assertThatThrownBy(() -> ProductionEnvironmentValidator.validate(loopbackOrigin))
            .isInstanceOf(BeanDefinitionStoreException.class)
            .hasMessageContaining("CORS_ALLOWED_ORIGINS");
    }

    @Test
    void nonProductionProfilesDoNotRequireProductionVariables() {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("local");
        ProductionEnvironmentValidator.validate(environment);
    }

    private MockEnvironment validProductionEnvironment() {
        MockEnvironment environment = new MockEnvironment()
            .withProperty("SPRING_DATASOURCE_URL", VALID_JDBC_URL)
            .withProperty("SPRING_DATASOURCE_USERNAME", "test-user")
            .withProperty("SPRING_DATASOURCE_PASSWORD", "test-password")
            .withProperty("JWT_SECRET", "test-only-secret-at-least-32-utf8-bytes")
            .withProperty("APP_ADMIN_EMAIL", "admin@example.test")
            .withProperty("APP_ADMIN_PASSWORD", "test-only-admin-password")
            .withProperty("CORS_ALLOWED_ORIGINS", "https://frontend.example.test");
        environment.setActiveProfiles("production");
        return environment;
    }
}
