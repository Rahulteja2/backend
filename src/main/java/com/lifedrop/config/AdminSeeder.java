package com.lifedrop.config;

import com.lifedrop.model.Role;
import com.lifedrop.model.User;
import com.lifedrop.repo.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class AdminSeeder {
    @Bean
    CommandLineRunner seedAdmin(UserRepository users, PasswordEncoder encoder,
                                @Value("${app.admin.email}") String email,
                                @Value("${app.admin.password}") String password) {
        return args -> {
            if (!users.existsByRole(Role.ADMIN)) {
                User admin = new User();
                admin.setName("Administrator");
                admin.setEmail(email.toLowerCase());
                admin.setPasswordHash(encoder.encode(password));
                admin.setRole(Role.ADMIN);
                users.save(admin);
            }
        };
    }
}