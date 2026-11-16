package com.carinsurance.vehicle;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Builds catalog entries and valuations for tests. The static methods build detached entities with ids for unit
 * tests; the instance methods persist them for integration tests.
 */
public final class VehicleCatalogTestData {

    private final VehicleCatalogRepository catalogRepository;
    private final VehicleValuationRepository valuationRepository;

    public VehicleCatalogTestData(
            VehicleCatalogRepository catalogRepository, VehicleValuationRepository valuationRepository) {
        this.catalogRepository = catalogRepository;
        this.valuationRepository = valuationRepository;
    }

    /** A make that no other test or the seed data uses, so lookups by make only see this test's rows. */
    public static String uniqueMake() {
        return "ZZ-" + UUID.randomUUID().toString().substring(0, 8);
    }

    public static VehicleCatalog catalog(Long id, String make, String model, String modelVersion, VehicleType type) {
        VehicleCatalog catalog = new VehicleCatalog();
        catalog.setId(id);
        catalog.setMake(make);
        catalog.setModel(model);
        catalog.setModelVersion(modelVersion);
        catalog.setVehicleType(type);
        return catalog;
    }

    public static VehicleValuation valuation(
            VehicleCatalog catalog, Long id, int year, String value, LocalDate validFrom) {
        VehicleValuation valuation = new VehicleValuation();
        valuation.setId(id);
        valuation.setYear(year);
        valuation.setValue(new BigDecimal(value));
        valuation.setValidFrom(validFrom);
        catalog.addValuation(valuation);
        return valuation;
    }

    public VehicleCatalog saveCatalog(String make, String model, String modelVersion, VehicleType type) {
        return catalogRepository.saveAndFlush(catalog(null, make, model, modelVersion, type));
    }

    public VehicleValuation saveValuation(VehicleCatalog catalog, int year, String value, LocalDate validFrom) {
        VehicleValuation valuation = new VehicleValuation();
        valuation.setCatalog(catalog);
        valuation.setYear(year);
        valuation.setValue(new BigDecimal(value));
        valuation.setValidFrom(validFrom);
        return valuationRepository.saveAndFlush(valuation);
    }
}
