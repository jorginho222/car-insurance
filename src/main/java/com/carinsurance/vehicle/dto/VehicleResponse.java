package com.carinsurance.vehicle.dto;

import java.time.Instant;

public record VehicleResponse(
        Long id,
        Long insuredId,
        String brand,
        String model,
        Integer year,
        String licensePlate,
        Instant createdAt,
        Instant updatedAt) {}
