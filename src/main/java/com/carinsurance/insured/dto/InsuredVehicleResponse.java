package com.carinsurance.insured.dto;

import com.carinsurance.vehicle.dto.VehicleCatalogResponse;
import com.carinsurance.vehicle.dto.VehicleValuationResponse;
import java.time.Instant;

public record InsuredVehicleResponse(
        Long id,
        Long insuredId,
        String licensePlate,
        String engineNumber,
        String chassisNumber,
        VehicleCatalogResponse catalog,
        VehicleValuationResponse valuation,
        Instant createdAt,
        Instant updatedAt) {}
