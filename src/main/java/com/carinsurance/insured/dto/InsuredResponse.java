package com.carinsurance.insured.dto;

import java.time.Instant;
import java.time.LocalDate;

public record InsuredResponse(
        Long id,
        String firstName,
        String lastName,
        String documentNumber,
        LocalDate dateOfBirth,
        String email,
        String phone,
        AddressDto address,
        String licenseNumber,
        LocalDate licenseIssueDate,
        Instant createdAt,
        Instant updatedAt) {}
