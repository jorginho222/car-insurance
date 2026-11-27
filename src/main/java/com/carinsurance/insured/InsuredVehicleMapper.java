package com.carinsurance.insured;

import com.carinsurance.insured.dto.InsuredVehicleResponse;
import com.carinsurance.vehicle.VehicleCatalogMapper;
import com.carinsurance.vehicle.VehicleValuation;
import java.util.Locale;
import org.springframework.stereotype.Component;

@Component
public class InsuredVehicleMapper {

    /**
     * An Argentine plate once normalized (see {@link #normalizePlate(String)}): old car {@code ABC123}, Mercosur car
     * {@code AB123CD}, old motorcycle {@code 123ABC} or Mercosur motorcycle {@code A123BCD}.
     */
    public static final String PLATE_PATTERN =
            "^([A-Z]{3}\\d{3}|[A-Z]{2}\\d{3}[A-Z]{2}|\\d{3}[A-Z]{3}|[A-Z]\\d{3}[A-Z]{3})$";

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
                vehicle.getEngineNumber(),
                vehicle.getChassisNumber(),
                catalogMapper.toResponse(valuation.getCatalog()),
                catalogMapper.toResponse(valuation),
                vehicle.getCreatedAt(),
                vehicle.getUpdatedAt());
    }

    /** Upper-cased without spaces or hyphens, so {@code "ab 123-cd"} and {@code "AB123CD"} are the same plate. */
    public static String normalizePlate(String licensePlate) {
        return licensePlate.replaceAll("[\\s-]", "").toUpperCase(Locale.ROOT);
    }

    /** Trimmed and upper-cased (engine and chassis numbers). */
    public static String normalizeIdentifier(String value) {
        return value.trim().toUpperCase(Locale.ROOT);
    }
}
