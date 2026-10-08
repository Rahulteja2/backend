package com.lifedrop.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Getter @Setter
public class BloodRequest {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull @Enumerated(EnumType.STRING) private BloodGroup patientGroup;
    @NotBlank private String location;
    @Min(1) private int units = 1;
    private boolean urgent;
    private String status = "OPEN";
    private LocalDateTime createdAt = LocalDateTime.now();
}