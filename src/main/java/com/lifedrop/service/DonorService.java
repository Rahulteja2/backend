package com.lifedrop.service;

import com.lifedrop.model.BloodGroup;
import com.lifedrop.model.Donor;
import com.lifedrop.repo.DonorRepository;
import org.springframework.stereotype.Service;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
public class DonorService {
    private final DonorRepository repo;

    public DonorService(DonorRepository repo) { this.repo = repo; }

    public Optional<Donor> profileOf(String email) { return repo.findByUserEmail(email); }

    /** Creates the donor's profile, or updates it if it already exists. */
    public Donor saveProfile(String email, Donor in) {
        Donor d = repo.findByUserEmail(email).orElseGet(Donor::new);
        d.setUserEmail(email);
        d.setName(in.getName());
        d.setBloodGroup(in.getBloodGroup());
        d.setCity(in.getCity());
        d.setPhone(in.getPhone());
        d.setLastDonationDate(in.getLastDonationDate());
        return repo.save(d);
    }

    /** Compatible donors for a patient group, eligible donors first, exact matches next. */
    public List<Donor> search(BloodGroup patientGroup, String city, boolean eligibleOnly) {
        List<BloodGroup> groups = BloodGroup.donorsFor(patientGroup);
        List<Donor> found = (city == null || city.isBlank())
            ? repo.findByBloodGroupIn(groups)
            : repo.findByBloodGroupInAndCityIgnoreCase(groups, city.trim());
        return found.stream()
            .filter(d -> !eligibleOnly || d.isEligible())
            .sorted(Comparator.comparing(Donor::isEligible).reversed()
                .thenComparing(d -> d.getBloodGroup() != patientGroup))
            .toList();
    }
}