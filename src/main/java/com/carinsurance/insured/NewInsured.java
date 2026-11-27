package com.carinsurance.insured;

import java.time.LocalDate;

/** The data of an insured to create, see {@link InsuredService#create(NewInsured)}. Values are not yet normalized. */
public record NewInsured(
        String firstName,
        String lastName,
        String documentNumber,
        LocalDate dateOfBirth,
        String email,
        String phone,
        Address address,
        String licenseNumber,
        LocalDate licenseIssueDate) {}
