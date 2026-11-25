package com.carinsurance.quotation.dto;

import com.carinsurance.quotation.Plan;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import java.time.LocalDate;

public record QuotationRequest(
        @NotNull Plan plan,
        @NotNull Long valuationId,
        @NotNull @Past LocalDate birthDate,
        @NotNull Long localityId) {}
