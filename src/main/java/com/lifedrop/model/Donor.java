package com.lifedrop.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

@Entity
@Getter @Setter
public class Donor {
    public static final int GAP_DAYS = 90;

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @Column(unique = true)
    private String userEmail;          // links the profile to the login account

    @NotBlank private String name;
    @NotNull @Enumerated(EnumType.STRING) private BloodGroup bloodGroup;
    @NotBlank private String city;
    private String phone;
    private LocalDate lastDonationDate;

    public boolean isEligible() {
        return lastDonationDate == null
            || ChronoUnit.DAYS.between(lastDonationDate, LocalDate.now()) >= GAP_DAYS;
    }

    public LocalDate getNextEligibleDate() {
        return lastDonationDate == null ? null : lastDonationDate.plusDays(GAP_DAYS);
    }
}