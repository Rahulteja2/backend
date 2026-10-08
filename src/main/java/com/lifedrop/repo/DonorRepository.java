package com.lifedrop.repo;

import com.lifedrop.model.BloodGroup;
import com.lifedrop.model.Donor;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface DonorRepository extends JpaRepository<Donor, Long> {
    List<Donor> findByBloodGroupIn(List<BloodGroup> groups);
    List<Donor> findByBloodGroupInAndCityIgnoreCase(List<BloodGroup> groups, String city);
    Optional<Donor> findByUserEmail(String userEmail);
}