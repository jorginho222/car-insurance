package com.carinsurance.vehicle.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record VehicleRequest(
        @NotNull Long insuredId,
        @NotBlank @Size(max = 100) String brand,
        @NotBlank @Size(max = 100) String model,
        @NotNull @Min(1900) Integer year,

        @NotBlank
        @Size(max = 20)
        @Pattern(regexp = "^[A-Za-z0-9 -]+$", message = "must contain only letters, digits, spaces or hyphens")
        String licensePlate) {}
