package com.lifedrop.controller;

import com.lifedrop.model.BloodRequest;
import com.lifedrop.repo.BloodRequestRepository;
import com.lifedrop.service.DonorService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/requests")
public class RequestController {
    private final BloodRequestRepository requests;
    private final DonorService donors;

    public RequestController(BloodRequestRepository requests, DonorService donors) {
        this.requests = requests;
        this.donors = donors;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> create(@Valid @RequestBody BloodRequest req) {
        BloodRequest saved = requests.save(req);
        int inCity = donors.search(saved.getPatientGroup(), saved.getLocation(), true).size();
        int everywhere = donors.search(saved.getPatientGroup(), null, true).size();
        return Map.of("request", saved, "eligibleDonorsInCity", inCity, "eligibleDonorsTotal", everywhere);
    }

    @GetMapping
    public List<BloodRequest> open() { return requests.findByStatusOrderByCreatedAtDesc("OPEN"); }
}