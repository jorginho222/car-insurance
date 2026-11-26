package com.carinsurance.quotation;

import com.carinsurance.common.exception.InvalidRequestException;
import com.carinsurance.common.exception.ResourceNotFoundException;
import com.carinsurance.location.Locality;
import com.carinsurance.location.LocalityRepository;
import com.carinsurance.location.Zone;
import com.carinsurance.quotation.dto.QuotationRequest;
import com.carinsurance.quotation.dto.QuotationResponse;
import com.carinsurance.vehicle.VehicleCatalogService;
import com.carinsurance.vehicle.VehicleType;
import com.carinsurance.vehicle.VehicleValuation;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Period;
import java.util.Locale;
import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Quotes every plan and stores the quotation; finds stored quotations by id, email or code. */
@Service
@Transactional(readOnly = true)
public class QuotationService {

    static final int MINIMUM_DRIVER_AGE = 18;

    private static final String LOCALITY = "Locality";
    private static final String QUOTATION = "Quotation";

    private final QuotationRepository repository;
    private final VehicleCatalogService catalogService;
    private final LocalityRepository localityRepository;
    private final QuotationVehicleParameterRepository vehicleParameterRepository;
    private final QuotationZoneFactorRepository zoneFactorRepository;
    private final QuotationDriverAgeFactorRepository driverAgeFactorRepository;
    private final QuotationChargeRepository chargeRepository;
    private final QuotationCodeGenerator codeGenerator;
    private final QuotationMapper mapper;
    private final QuotationProperties properties;
    private final Clock clock;

    public QuotationService(
            QuotationRepository repository,
            VehicleCatalogService catalogService,
            LocalityRepository localityRepository,
            QuotationVehicleParameterRepository vehicleParameterRepository,
            QuotationZoneFactorRepository zoneFactorRepository,
            QuotationDriverAgeFactorRepository driverAgeFactorRepository,
            QuotationChargeRepository chargeRepository,
            QuotationCodeGenerator codeGenerator,
            QuotationMapper mapper,
            QuotationProperties properties,
            Clock clock) {
        this.repository = repository;
        this.catalogService = catalogService;
        this.localityRepository = localityRepository;
        this.vehicleParameterRepository = vehicleParameterRepository;
        this.zoneFactorRepository = zoneFactorRepository;
        this.driverAgeFactorRepository = driverAgeFactorRepository;
        this.chargeRepository = chargeRepository;
        this.codeGenerator = codeGenerator;
        this.mapper = mapper;
        this.properties = properties;
        this.clock = clock;
    }

    @Transactional
    public QuotationResponse quote(QuotationRequest request) {
        Instant now = Instant.now(clock);
        LocalDate today = LocalDate.now(clock);
        int driverAge = Period.between(request.birthDate(), today).getYears();
        if (driverAge < MINIMUM_DRIVER_AGE) {
            throw new UnderAgeDriverException(driverAge, MINIMUM_DRIVER_AGE);
        }

        VehicleValuation valuation = catalogService.getSelectableValuation(request.valuationId());
        VehicleType vehicleType = valuation.getCatalog().getVehicleType();
        Locality locality = localityRepository
                .findWithZoneById(request.localityId())
                .orElseThrow(() -> new ResourceNotFoundException(LOCALITY, request.localityId()));
        Zone zone = locality.getZone();

        QuotationVehicleParameter vehicleParameter = vehicleParameterRepository
                .findByVehicleType(vehicleType)
                .orElseThrow(() -> missing("vehicle parameter for vehicle type " + vehicleType));
        QuotationZoneFactor zoneFactor = zoneFactorRepository
                .findByZoneId(zone.getId())
                .orElseThrow(() -> missing("zone factor for zone " + zone.getName()));
        QuotationDriverAgeFactor ageFactor = driverAgeFactorRepository
                .findByAge(driverAge)
                .orElseThrow(() -> missing("driver age factor for age " + driverAge));
        QuotationCharge charge = chargeRepository.getCurrent();

        Map<CoverageType, BigDecimal> premiums =
                PremiumCalculator.coveragePremiums(valuation.getValue(), vehicleParameter, zoneFactor, ageFactor);
        Quotation quotation = new Quotation(
                codeGenerator.next(today.getYear()),
                normalizeEmail(request.email()),
                now,
                today.plusDays(properties.validityDays()),
                valuation,
                locality,
                request.birthDate(),
                driverAge);
        for (Plan plan : Plan.values()) {
            quotation.addPlan(PremiumCalculator.planQuote(plan, premiums, charge));
        }

        return mapper.toResponse(repository.save(quotation), today);
    }

    public QuotationResponse findById(Long id) {
        Quotation quotation =
                repository.findDetailedById(id).orElseThrow(() -> new ResourceNotFoundException(QUOTATION, id));
        return mapper.toResponse(quotation, LocalDate.now(clock));
    }

    /**
     * Open quotations matching the given email or the given code (exactly one of them): only
     * {@link QuotationStatus#QUOTED} ones still valid today (valid-until today or later). The email and the code are
     * matched ignoring case and surrounding spaces.
     *
     * @throws InvalidRequestException if neither or both are given
     */
    public Page<QuotationResponse> search(String email, String code, Pageable pageable) {
        String normalizedEmail = normalizeEmail(email);
        String normalizedCode = normalizeCode(code);
        if ((normalizedEmail == null) == (normalizedCode == null)) {
            throw new InvalidRequestException("Search quotations by either an email or a code, not both");
        }
        LocalDate today = LocalDate.now(clock);
        Page<Quotation> page = normalizedEmail != null
                ? repository.findOpenByEmail(normalizedEmail, today, pageable)
                : repository.findOpenByCode(normalizedCode, today, pageable);
        return page.map(quotation -> mapper.toResponse(quotation, today));
    }

    private static String normalizeEmail(String email) {
        return email == null || email.isBlank() ? null : email.trim().toLowerCase(Locale.ROOT);
    }

    private static String normalizeCode(String code) {
        return code == null || code.isBlank() ? null : code.trim().toUpperCase(Locale.ROOT);
    }

    private static IllegalStateException missing(String what) {
        return new IllegalStateException("Quotation configuration is missing the " + what);
    }
}
