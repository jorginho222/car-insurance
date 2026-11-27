package com.carinsurance.policy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.carinsurance.common.exception.ConflictException;
import com.carinsurance.common.exception.InvalidRequestException;
import com.carinsurance.common.exception.ResourceNotFoundException;
import com.carinsurance.insured.Insured;
import com.carinsurance.insured.InsuredMapper;
import com.carinsurance.insured.InsuredService;
import com.carinsurance.insured.InsuredVehicle;
import com.carinsurance.insured.InsuredVehicleMapper;
import com.carinsurance.insured.InsuredVehicleService;
import com.carinsurance.insured.NewInsured;
import com.carinsurance.insured.NewInsuredVehicle;
import com.carinsurance.location.Locality;
import com.carinsurance.location.LocalityMapper;
import com.carinsurance.location.Zone;
import com.carinsurance.policy.dto.PolicyRequest;
import com.carinsurance.policy.dto.PolicyResponse;
import com.carinsurance.quotation.CoverageType;
import com.carinsurance.quotation.PaymentOption;
import com.carinsurance.quotation.Plan;
import com.carinsurance.quotation.PlanCoverage;
import com.carinsurance.quotation.Quotation;
import com.carinsurance.quotation.QuotationPlan;
import com.carinsurance.quotation.QuotationRepository;
import com.carinsurance.quotation.QuotationStatus;
import com.carinsurance.vehicle.VehicleCatalog;
import com.carinsurance.vehicle.VehicleCatalogMapper;
import com.carinsurance.vehicle.VehicleCatalogTestData;
import com.carinsurance.vehicle.VehicleType;
import com.carinsurance.vehicle.VehicleValuation;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class PolicyServiceTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 11, 27);
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-11-27T15:00:00Z"), ZoneOffset.UTC);

    @Mock
    private PolicyRepository repository;

    @Mock
    private QuotationRepository quotationRepository;

    @Mock
    private InsuredService insuredService;

    @Mock
    private InsuredVehicleService vehicleService;

    @Mock
    private PolicyNumberGenerator numberGenerator;

    @Mock
    private LicensePlateLock plateLock;

    private PolicyService service;
    private Quotation quotation;
    private QuotationPlan fullCoverage;
    private PaymentOption twelveInstallments;
    private VehicleValuation valuation;
    private Locality locality;

    @BeforeEach
    void setUp() {
        PolicyMapper mapper = new PolicyMapper(
                new InsuredMapper(), new InsuredVehicleMapper(new VehicleCatalogMapper()), new LocalityMapper());
        service = new PolicyService(
                repository,
                quotationRepository,
                insuredService,
                vehicleService,
                numberGenerator,
                plateLock,
                mapper,
                CLOCK);

        VehicleCatalog catalog = VehicleCatalogTestData.catalog(1L, "Toyota", "Corolla", "2.0 XEI", VehicleType.CAR);
        valuation = VehicleCatalogTestData.valuation(catalog, 20L, 2021, "20000.00", LocalDate.of(2026, 1, 1));
        Zone zone = new Zone("GBA");
        ReflectionTestUtils.setField(zone, "id", 2L);
        locality = new Locality("FLORIDA", "Buenos Aires", "1602", zone);
        locality.setId(812L);

        quotation = new Quotation(
                "Q-2026-000007",
                "driver@example.com",
                Instant.parse("2026-11-20T10:00:00Z"),
                TODAY.plusDays(8),
                valuation,
                locality,
                LocalDate.of(1990, 5, 17),
                36);
        quotation.setId(7L);
        PaymentOption onePayment = option(1, "1173.34");
        twelveInstallments = option(12, "1267.21");
        QuotationPlan civilLiability = plan(70L, Plan.CIVIL_LIABILITY, onePayment, twelveInstallments);
        fullCoverage = plan(72L, Plan.FULL_COVERAGE, onePayment, twelveInstallments);
        quotation.addPlan(civilLiability);
        quotation.addPlan(fullCoverage);
    }

    private static PaymentOption option(int installments, String total) {
        BigDecimal amount = new BigDecimal(total);
        return new PaymentOption(
                installments, BigDecimal.ONE, BigDecimal.TEN, BigDecimal.ZERO, amount, amount, amount, amount, amount);
    }

    private static QuotationPlan plan(long id, Plan name, PaymentOption... options) {
        QuotationPlan plan = new QuotationPlan(
                name,
                List.of(new PlanCoverage(CoverageType.LIABILITY, new BigDecimal("324.00"))),
                new BigDecimal("914.00"),
                List.of(options));
        ReflectionTestUtils.setField(plan, "id", id);
        return plan;
    }

    private static PolicyRequest request(long planId, int installments, LocalDate validFrom) {
        return new PolicyRequest(
                7L,
                planId,
                installments,
                PaymentMethod.CARD,
                validFrom,
                "Jane",
                "Doe",
                "30123456",
                "jane@example.com",
                "+54 11 5555 1234",
                "Av. Rivadavia 1234",
                "B-123456",
                LocalDate.of(2010, 6, 1),
                "ab 123-cd",
                "ENG-12345",
                "8AP17000000123456");
    }

    private void givenTheQuotationExists() {
        given(quotationRepository.findForPolicyById(7L)).willReturn(Optional.of(quotation));
    }

    private void givenCreationSucceeds() {
        given(insuredService.create(any())).willAnswer(inv -> {
            NewInsured data = inv.getArgument(0);
            Insured insured = new InsuredMapper().toEntity(data);
            insured.setId(3L);
            return insured;
        });
        given(vehicleService.create(any(), any(), any())).willAnswer(inv -> {
            Insured insured = inv.getArgument(0);
            NewInsuredVehicle data = inv.getArgument(2);
            InsuredVehicle vehicle = new InsuredVehicle();
            vehicle.setId(4L);
            vehicle.setValuation(inv.getArgument(1));
            vehicle.setLicensePlate(InsuredVehicleMapper.normalizePlate(data.licensePlate()));
            vehicle.setEngineNumber(data.engineNumber());
            vehicle.setChassisNumber(data.chassisNumber());
            insured.addVehicle(vehicle);
            return vehicle;
        });
        given(numberGenerator.next(2026)).willReturn("AU-2026-0000001");
        given(repository.saveAndFlush(any(Policy.class))).willAnswer(inv -> {
            Policy policy = inv.getArgument(0);
            ReflectionTestUtils.setField(policy, "id", 9L);
            return policy;
        });
    }

    @Test
    void createIssuesThePolicyAndAcceptsTheQuotation() {
        givenTheQuotationExists();
        givenCreationSucceeds();

        PolicyResponse response = service.create(request(72L, 12, TODAY.plusDays(3)));

        assertThat(response.id()).isEqualTo(9L);
        assertThat(response.number()).isEqualTo("AU-2026-0000001");
        assertThat(response.validFrom()).isEqualTo(TODAY.plusDays(3));
        assertThat(response.validTo()).isEqualTo(TODAY.plusDays(3).plusYears(1));
        assertThat(response.quotation().code()).isEqualTo("Q-2026-000007");
        assertThat(response.plan().id()).isEqualTo(72L);
        assertThat(response.plan().name()).isEqualTo(Plan.FULL_COVERAGE);
        assertThat(response.paymentOption()).isEqualTo(twelveInstallments);
        assertThat(response.paymentMethod()).isEqualTo(PaymentMethod.CARD);
        assertThat(response.locality().name()).isEqualTo("FLORIDA");
        assertThat(response.vehicle().licensePlate()).isEqualTo("AB123CD");
        assertThat(quotation.getStatus()).isEqualTo(QuotationStatus.ACCEPTED);
        verify(plateLock).lock("AB123CD");
        verify(repository)
                .existsOverlapping(
                        "AB123CD", TODAY.plusDays(3), TODAY.plusDays(3).plusYears(1));
    }

    @Test
    void insuredTakesBirthDateAndLocalityFromTheQuotation() {
        givenTheQuotationExists();
        givenCreationSucceeds();

        service.create(request(72L, 1, TODAY));

        ArgumentCaptor<NewInsured> insured = ArgumentCaptor.forClass(NewInsured.class);
        verify(insuredService).create(insured.capture());
        assertThat(insured.getValue().dateOfBirth()).isEqualTo(LocalDate.of(1990, 5, 17));
        assertThat(insured.getValue().address().getStreet()).isEqualTo("Av. Rivadavia 1234");
        assertThat(insured.getValue().address().getLocality()).isEqualTo("FLORIDA");
        assertThat(insured.getValue().address().getProvince()).isEqualTo("Buenos Aires");
        assertThat(insured.getValue().address().getPostalCode()).isEqualTo("1602");
        assertThat(insured.getValue().address().getCountry()).isEqualTo("Argentina");
        verify(vehicleService).create(any(), eq(valuation), any());
    }

    @Test
    void unknownQuotationIsNotFound() {
        given(quotationRepository.findForPolicyById(7L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(request(72L, 1, TODAY))).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void planOfAnotherQuotationIsRejected() {
        givenTheQuotationExists();

        assertThatThrownBy(() -> service.create(request(99L, 1, TODAY)))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("not a plan of quotation");
        assertNothingCreated();
    }

    @Test
    void installmentsOtherThanTheOfferedOnesAreRejected() {
        givenTheQuotationExists();

        assertThatThrownBy(() -> service.create(request(72L, 6, TODAY)))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("[1, 12]");
        assertNothingCreated();
    }

    @Test
    void validFromInThePastIsRejected() {
        givenTheQuotationExists();

        assertThatThrownBy(() -> service.create(request(72L, 1, TODAY.minusDays(1))))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("validFrom");
        assertNothingCreated();
    }

    @Test
    void validFromUpTo30DaysAheadIsAcceptedButNotLater() {
        givenTheQuotationExists();
        givenCreationSucceeds();

        assertThat(service.create(request(72L, 1, TODAY.plusDays(30))).validFrom())
                .isEqualTo(TODAY.plusDays(30));
        assertThatThrownBy(() -> service.create(request(72L, 1, TODAY.plusDays(31))))
                .isInstanceOf(InvalidRequestException.class);
    }

    @Test
    void quotationThatIsNotQuotedIsRejected() {
        quotation.accept(TODAY);
        givenTheQuotationExists();

        assertThatThrownBy(() -> service.create(request(72L, 1, TODAY)))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("ACCEPTED");
        assertNothingCreated();
    }

    @Test
    void plateWithAnOverlappingPolicyIsRejected() {
        givenTheQuotationExists();
        given(repository.existsOverlapping(eq("AB123CD"), any(), any())).willReturn(true);

        assertThatThrownBy(() -> service.create(request(72L, 1, TODAY)))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("AB123CD");
        assertNothingCreated();
    }

    @Test
    void findByIdThrowsWhenMissing() {
        given(repository.findDetailedById(5L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(5L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Policy with id 5 not found");
    }

    private void assertNothingCreated() {
        verify(insuredService, never()).create(any());
        verify(numberGenerator, never()).next(anyInt());
        verify(repository, never()).saveAndFlush(any());
    }
}
