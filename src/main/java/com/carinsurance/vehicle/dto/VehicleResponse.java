package com.carinsurance.vehicle.dto;

import com.carinsurance.vehicle.VehicleType;
import java.time.Instant;

public record VehicleResponse(
        Long id,
        Long insuredId,
        VehicleType type,
        String brand,
        String model,
        Integer year,
        String licensePlate,
        Instant createdAt,
        Instant updatedAt) {}
