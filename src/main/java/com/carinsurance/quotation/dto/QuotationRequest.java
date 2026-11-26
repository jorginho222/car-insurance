package com.carinsurance.quotation.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record QuotationRequest(
        @NotNull Long valuationId,
        @NotNull @Past LocalDate birthDate,
        @NotNull Long localityId,
        @NotBlank @Email @Size(max = 255) String email) {}
