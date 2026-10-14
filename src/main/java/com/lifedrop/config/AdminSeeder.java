package com.lifedrop.config;

import com.lifedrop.model.Role;
import com.lifedrop.model.User;
import com.lifedrop.repo.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.util.Locale;

@Configuration
public class AdminSeeder {
    @Bean
    CommandLineRunner seedAdmin(UserRepository users, PasswordEncoder encoder,
                                @Value("${app.admin.email}") String email,
                                @Value("${app.admin.password}") String password) {
        return args -> {
            if (email.isBlank() || password.isBlank() || password.length() < 12) {
                throw new IllegalStateException(
                    "Admin seed configuration requires a non-empty email and a password of at least 12 characters");
            }
            if (!users.existsByRole(Role.ADMIN)) {
                User admin = new User();
                admin.setName("Administrator");
                admin.setEmail(email.trim().toLowerCase(Locale.ROOT));
                admin.setPasswordHash(encoder.encode(password));
                admin.setRole(Role.ADMIN);
                users.save(admin);
            }
        };
    }
}