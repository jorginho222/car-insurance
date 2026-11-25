package com.carinsurance.quotation;

import com.carinsurance.common.exception.ResourceNotFoundException;
import com.carinsurance.location.Locality;
import com.carinsurance.location.LocalityRepository;
import com.carinsurance.location.Zone;
import com.carinsurance.quotation.dto.QuotationRequest;
import com.carinsurance.quotation.dto.QuotationResponse;
import com.carinsurance.quotation.dto.QuotationResponse.PlanQuote;
import com.carinsurance.vehicle.VehicleCatalog;
import com.carinsurance.vehicle.VehicleCatalogService;
import com.carinsurance.vehicle.VehicleValuation;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.Period;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Calculates a quotation for every plan. Nothing is stored. */
@Service
@Transactional(readOnly = true)
public class QuotationService {

    static final int MINIMUM_DRIVER_AGE = 18;

    private static final String LOCALITY = "Locality";

    private final VehicleCatalogService catalogService;
    private final LocalityRepository localityRepository;
    private final QuotationVehicleParameterRepository vehicleParameterRepository;
    private final QuotationZoneFactorRepository zoneFactorRepository;
    private final QuotationDriverAgeFactorRepository driverAgeFactorRepository;
    private final QuotationChargeRepository chargeRepository;
    private final QuotationProperties properties;
    private final Clock clock;

    public QuotationService(
            VehicleCatalogService catalogService,
            LocalityRepository localityRepository,
            QuotationVehicleParameterRepository vehicleParameterRepository,
            QuotationZoneFactorRepository zoneFactorRepository,
            QuotationDriverAgeFactorRepository driverAgeFactorRepository,
            QuotationChargeRepository chargeRepository,
            QuotationProperties properties,
            Clock clock) {
        this.catalogService = catalogService;
        this.localityRepository = localityRepository;
        this.vehicleParameterRepository = vehicleParameterRepository;
        this.zoneFactorRepository = zoneFactorRepository;
        this.driverAgeFactorRepository = driverAgeFactorRepository;
        this.chargeRepository = chargeRepository;
        this.properties = properties;
        this.clock = clock;
    }

    public QuotationResponse quote(QuotationRequest request) {
        LocalDate today = LocalDate.now(clock);
        int driverAge = Period.between(request.birthDate(), today).getYears();
        if (driverAge < MINIMUM_DRIVER_AGE) {
            throw new UnderAgeDriverException(driverAge, MINIMUM_DRIVER_AGE);
        }

        VehicleValuation valuation = catalogService.getSelectableValuation(request.valuationId());
        VehicleCatalog catalog = valuation.getCatalog();
        Locality locality = localityRepository
                .findWithZoneById(request.localityId())
                .orElseThrow(() -> new ResourceNotFoundException(LOCALITY, request.localityId()));
        Zone zone = locality.getZone();

        QuotationVehicleParameter vehicleParameter = vehicleParameterRepository
                .findByVehicleType(catalog.getVehicleType())
                .orElseThrow(() -> missing("vehicle parameter for vehicle type " + catalog.getVehicleType()));
        QuotationZoneFactor zoneFactor = zoneFactorRepository
                .findByZoneId(zone.getId())
                .orElseThrow(() -> missing("zone factor for zone " + zone.getName()));
        QuotationDriverAgeFactor ageFactor = driverAgeFactorRepository
                .findByAge(driverAge)
                .orElseThrow(() -> missing("driver age factor for age " + driverAge));
        QuotationCharge charge = chargeRepository.getCurrent();

        Map<CoverageType, BigDecimal> premiums =
                PremiumCalculator.coveragePremiums(valuation.getValue(), vehicleParameter, zoneFactor, ageFactor);
        List<PlanQuote> plans = Arrays.stream(Plan.values())
                .map(plan -> PremiumCalculator.planQuote(plan, premiums, charge))
                .toList();

        return new QuotationResponse(
                today.plusDays(properties.validityDays()),
                new QuotationResponse.Vehicle(
                        "%s %s %s".formatted(catalog.getMake(), catalog.getModel(), catalog.getModelVersion()),
                        catalog.getMake(),
                        catalog.getModel(),
                        catalog.getModelVersion(),
                        valuation.getYear(),
                        catalog.getVehicleType(),
                        valuation.getValue()),
                new QuotationResponse.Risk(
                        locality.getPostalCode(),
                        locality.getName(),
                        locality.getProvince(),
                        zone.getName(),
                        driverAge),
                plans);
    }

    private static IllegalStateException missing(String what) {
        return new IllegalStateException("Quotation configuration is missing the " + what);
    }
}
