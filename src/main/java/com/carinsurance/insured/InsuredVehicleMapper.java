package com.carinsurance.insured;

import com.carinsurance.insured.dto.InsuredVehicleResponse;
import com.carinsurance.vehicle.VehicleCatalogMapper;
import com.carinsurance.vehicle.VehicleValuation;
import java.util.Locale;
import org.springframework.stereotype.Component;

@Component
public class InsuredVehicleMapper {

    private final VehicleCatalogMapper catalogMapper;

    public InsuredVehicleMapper(VehicleCatalogMapper catalogMapper) {
        this.catalogMapper = catalogMapper;
    }

    public InsuredVehicleResponse toResponse(InsuredVehicle vehicle) {
        VehicleValuation valuation = vehicle.getValuation();
        return new InsuredVehicleResponse(
                vehicle.getId(),
                vehicle.getInsured().getId(),
                vehicle.getLicensePlate(),
                catalogMapper.toResponse(valuation.getCatalog()),
                catalogMapper.toResponse(valuation),
                vehicle.getCreatedAt(),
                vehicle.getUpdatedAt());
    }

    public static String normalizePlate(String licensePlate) {
        return licensePlate.trim().toUpperCase(Locale.ROOT);
    }
}
