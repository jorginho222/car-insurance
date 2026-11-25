package com.carinsurance.quotation.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import java.time.LocalDate;

public record QuotationRequest(
        @NotNull Long valuationId,
        @NotNull @Past LocalDate birthDate,
        @NotNull Long localityId) {}
