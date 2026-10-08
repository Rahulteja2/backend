package com.lifedrop.repo;

import com.lifedrop.model.BloodRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface BloodRequestRepository extends JpaRepository<BloodRequest, Long> {
    List<BloodRequest> findByStatusOrderByCreatedAtDesc(String status);
}