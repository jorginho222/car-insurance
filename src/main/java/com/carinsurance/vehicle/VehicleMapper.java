package com.carinsurance.vehicle;

import com.carinsurance.vehicle.dto.VehicleRequest;
import com.carinsurance.vehicle.dto.VehicleResponse;
import java.util.Locale;
import org.springframework.stereotype.Component;

@Component
public class VehicleMapper {

    public Vehicle toEntity(VehicleRequest request) {
        Vehicle vehicle = new Vehicle();
        updateEntity(vehicle, request);
        return vehicle;
    }

    /** Copies scalar fields only; the owning insured is managed by the service through {@code Insured} helpers. */
    public void updateEntity(Vehicle vehicle, VehicleRequest request) {
        vehicle.setBrand(request.brand().trim());
        vehicle.setModel(request.model().trim());
        vehicle.setYear(request.year());
        vehicle.setLicensePlate(normalizePlate(request.licensePlate()));
    }

    public VehicleResponse toResponse(Vehicle vehicle) {
        return new VehicleResponse(
                vehicle.getId(),
                vehicle.getInsured().getId(),
                vehicle.getBrand(),
                vehicle.getModel(),
                vehicle.getYear(),
                vehicle.getLicensePlate(),
                vehicle.getCreatedAt(),
                vehicle.getUpdatedAt());
    }

    public static String normalizePlate(String licensePlate) {
        return licensePlate.trim().toUpperCase(Locale.ROOT);
    }
}
