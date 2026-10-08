package com.lifedrop.controller;

import com.lifedrop.model.BloodGroup;
import com.lifedrop.model.Donor;
import com.lifedrop.service.DonorService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;

@RestController
@RequestMapping("/api/donors")
public class DonorController {
    private final DonorService service;

    public DonorController(DonorService service) { this.service = service; }

    @GetMapping("/me")
    public Donor myProfile(Authentication auth) {
        return service.profileOf(auth.getName())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No donor profile yet"));
    }

    @PutMapping("/me")
    public Donor saveMyProfile(Authentication auth, @Valid @RequestBody Donor donor) {
        return service.saveProfile(auth.getName(), donor);
    }

    // Example: GET /api/donors/search?group=A_POS&city=Hyderabad&eligibleOnly=true
    @GetMapping("/search")
    public List<Donor> search(@RequestParam BloodGroup group,
                              @RequestParam(required = false) String city,
                              @RequestParam(defaultValue = "false") boolean eligibleOnly) {
        return service.search(group, city, eligibleOnly);
    }
}