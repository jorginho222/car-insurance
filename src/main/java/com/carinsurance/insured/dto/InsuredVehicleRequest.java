package com.carinsurance.insured.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * {@code valuationId} is the id of a valuation from {@code GET /api/v1/vehicle-catalog/{catalogId}/valuations}; it
 * identifies both the catalog entry (make, model, version, type) and the model year.
 */
public record InsuredVehicleRequest(
        @NotNull Long valuationId,

        @NotBlank
        @Size(max = 20)
        @Pattern(regexp = "^[A-Za-z0-9 -]+$", message = "must contain only letters, digits, spaces or hyphens")
        String licensePlate) {}
