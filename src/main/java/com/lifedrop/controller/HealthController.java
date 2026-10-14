package com.lifedrop.controller;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.Map;

@RestController
public class HealthController {
    private final JdbcTemplate jdbc;

    public HealthController(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @GetMapping("/health")
    public Map<String, String> health() {
        Integer result = jdbc.queryForObject("SELECT 1", Integer.class);
        if (!Integer.valueOf(1).equals(result)) {
            throw new IllegalStateException("Database health check failed");
        }
        return Map.of("status", "UP");
    }
}
