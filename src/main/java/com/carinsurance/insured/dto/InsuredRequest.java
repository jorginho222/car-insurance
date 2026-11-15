package com.carinsurance.insured.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record InsuredRequest(
        @NotBlank @Size(max = 100) String firstName,
        @NotBlank @Size(max = 100) String lastName,
        @NotBlank @Size(max = 50) String documentNumber,
        @NotNull @Past LocalDate dateOfBirth,
        @NotBlank @Email @Size(max = 255) String email,

        @NotBlank @Pattern(regexp = "^\\+?[0-9 ()-]{7,30}$", message = "must be a valid phone number")
        String phone,

        @NotNull @Valid AddressDto address,
        @NotBlank @Size(max = 50) String licenseNumber,
        @NotNull @PastOrPresent LocalDate licenseIssueDate) {}
