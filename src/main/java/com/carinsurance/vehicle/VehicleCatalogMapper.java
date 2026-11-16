package com.carinsurance.vehicle;

import com.carinsurance.vehicle.dto.VehicleCatalogResponse;
import com.carinsurance.vehicle.dto.VehicleValuationResponse;
import org.springframework.stereotype.Component;

@Component
public class VehicleCatalogMapper {

    public VehicleCatalogResponse toResponse(VehicleCatalog catalog) {
        return new VehicleCatalogResponse(
                catalog.getId(),
                catalog.getMake(),
                catalog.getModel(),
                catalog.getModelVersion(),
                catalog.getVehicleType());
    }

    public VehicleValuationResponse toResponse(VehicleValuation valuation) {
        return new VehicleValuationResponse(
                valuation.getId(),
                valuation.getCatalog().getId(),
                valuation.getYear(),
                valuation.getValue(),
                valuation.getValidFrom());
    }
}
