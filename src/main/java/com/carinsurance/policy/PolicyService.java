package com.carinsurance.policy;

import com.carinsurance.common.exception.ConflictException;
import com.carinsurance.common.exception.InvalidRequestException;
import com.carinsurance.common.exception.ResourceNotFoundException;
import com.carinsurance.insured.Address;
import com.carinsurance.insured.Insured;
import com.carinsurance.insured.InsuredService;
import com.carinsurance.insured.InsuredVehicle;
import com.carinsurance.insured.InsuredVehicleMapper;
import com.carinsurance.insured.InsuredVehicleService;
import com.carinsurance.insured.NewInsured;
import com.carinsurance.insured.NewInsuredVehicle;
import com.carinsurance.location.Locality;
import com.carinsurance.policy.dto.PolicyRequest;
import com.carinsurance.policy.dto.PolicyResponse;
import com.carinsurance.quotation.PaymentOption;
import com.carinsurance.quotation.Quotation;
import com.carinsurance.quotation.QuotationPlan;
import com.carinsurance.quotation.QuotationRepository;
import java.time.Clock;
import java.time.LocalDate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Issues policies from quoted plans, creating the insured and the insured vehicle with them, and reads them by id.
 */
@Service
@Transactional(readOnly = true)
public class PolicyService {

    /** How many days after today a policy can start at the latest. */
    static final int MAX_DAYS_UNTIL_START = 30;

    static final String COUNTRY = "Argentina";

    private static final String POLICY = "Policy";

    private final PolicyRepository repository;
    private final QuotationRepository quotationRepository;
    private final InsuredService insuredService;
    private final InsuredVehicleService vehicleService;
    private final PolicyNumberGenerator numberGenerator;
    private final LicensePlateLock plateLock;
    private final PolicyMapper mapper;
    private final Clock clock;

    public PolicyService(
            PolicyRepository repository,
            QuotationRepository quotationRepository,
            InsuredService insuredService,
            InsuredVehicleService vehicleService,
            PolicyNumberGenerator numberGenerator,
            LicensePlateLock plateLock,
            PolicyMapper mapper,
            Clock clock) {
        this.repository = repository;
        this.quotationRepository = quotationRepository;
        this.insuredService = insuredService;
        this.vehicleService = vehicleService;
        this.numberGenerator = numberGenerator;
        this.plateLock = plateLock;
        this.mapper = mapper;
        this.clock = clock;
    }

    /**
     * Issues a policy from a quoted plan and marks its quotation as accepted.
     *
     * @throws ResourceNotFoundException if the quotation doesn't exist
     * @throws InvalidRequestException if the plan isn't one of the quotation's, the installments aren't offered, or
     *     validFrom isn't between today and {@value #MAX_DAYS_UNTIL_START} days from now
     * @throws ConflictException if the quotation isn't open (QUOTED and still valid) or the plate already has a policy
     *     overlapping the new one
     */
    @Transactional
    public PolicyResponse create(PolicyRequest request) {
        LocalDate today = LocalDate.now(clock);
        Quotation quotation = quotationRepository
                .findForPolicyById(request.quotationId())
                .orElseThrow(() -> new ResourceNotFoundException("Quotation", request.quotationId()));
        QuotationPlan plan = findPlan(quotation, request.planId());
        PaymentOption paymentOption = findPaymentOption(plan, request.installments());
        LocalDate validFrom = checkValidFrom(request.validFrom(), today);
        LocalDate validTo = Policy.validToFor(validFrom);

        quotation.accept(today);

        String plate = InsuredVehicleMapper.normalizePlate(request.licensePlate());
        plateLock.lock(plate);
        if (repository.existsOverlapping(plate, validFrom, validTo)) {
            throw new ConflictException("License plate %s already has a policy covering part of %s to %s"
                    .formatted(plate, validFrom, validTo));
        }

        Locality locality = quotation.getLocality();
        Insured insured = insuredService.create(new NewInsured(
                request.firstName(),
                request.lastName(),
                request.documentNumber(),
                quotation.getDriverBirthDate(),
                request.email(),
                request.phone(),
                new Address(
                        request.street(),
                        locality.getName(),
                        locality.getProvince(),
                        locality.getPostalCode(),
                        COUNTRY),
                request.licenseNumber(),
                request.licenseIssueDate()));
        InsuredVehicle vehicle = vehicleService.create(
                insured,
                quotation.getValuation(),
                new NewInsuredVehicle(request.licensePlate(), request.engineNumber(), request.chassisNumber()));

        Policy policy = new Policy(
                numberGenerator.next(today.getYear()),
                insured,
                vehicle,
                plan,
                locality,
                validFrom,
                paymentOption,
                request.paymentMethod());
        return mapper.toResponse(repository.saveAndFlush(policy));
    }

    public PolicyResponse findById(Long id) {
        return mapper.toResponse(
                repository.findDetailedById(id).orElseThrow(() -> new ResourceNotFoundException(POLICY, id)));
    }

    private static QuotationPlan findPlan(Quotation quotation, Long planId) {
        return quotation.getPlans().stream()
                .filter(plan -> plan.getId().equals(planId))
                .findFirst()
                .orElseThrow(() -> new InvalidRequestException(
                        "Plan %d is not a plan of quotation %s".formatted(planId, quotation.getCode())));
    }

    private static PaymentOption findPaymentOption(QuotationPlan plan, int installments) {
        if (!PaymentOption.INSTALLMENT_OPTIONS.contains(installments)) {
            throw new InvalidRequestException(
                    "installments must be one of %s".formatted(PaymentOption.INSTALLMENT_OPTIONS));
        }
        return plan.getPaymentOptions().stream()
                .filter(option -> option.installments() == installments)
                .findFirst()
                .orElseThrow(() -> new InvalidRequestException(
                        "Plan %d has no payment option of %d installments".formatted(plan.getId(), installments)));
    }

    private static LocalDate checkValidFrom(LocalDate validFrom, LocalDate today) {
        LocalDate latest = today.plusDays(MAX_DAYS_UNTIL_START);
        if (validFrom.isBefore(today) || validFrom.isAfter(latest)) {
            throw new InvalidRequestException("validFrom must be between %s and %s".formatted(today, latest));
        }
        return validFrom;
    }
}
