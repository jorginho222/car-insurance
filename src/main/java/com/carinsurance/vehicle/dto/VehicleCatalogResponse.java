package com.carinsurance.vehicle.dto;

import com.carinsurance.vehicle.VehicleType;

public record VehicleCatalogResponse(
        Long id, String make, String model, String modelVersion, VehicleType vehicleType) {}
