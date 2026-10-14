package com.lifedrop.controller;

import com.lifedrop.model.Role;
import com.lifedrop.model.User;
import com.lifedrop.repo.DonorRepository;
import com.lifedrop.repo.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@RestController
@RequestMapping("/api/admin")
public class AdminController {
    private final UserRepository users;
    private final DonorRepository donors;

    public AdminController(UserRepository users, DonorRepository donors) {
        this.users = users;
        this.donors = donors;
    }

    @GetMapping("/users")
    public List<AdminUserSummary> users() {
        return users.findAll().stream()
            .map(user -> new AdminUserSummary(user.getId(), user.getName(), user.getEmail(), user.getRole()))
            .toList();
    }

    @DeleteMapping("/users/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Transactional
    public void deleteUser(@PathVariable Long id) {
        User user = users.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        if (user.getRole() == Role.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Admin accounts cannot be deleted");
        }
        donors.findByUserEmail(user.getEmail()).ifPresent(donors::delete);
        users.delete(user);
    }

    public record AdminUserSummary(Long id, String name, String email, Role role) {}
}
