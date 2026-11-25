package com.carinsurance.quotation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;

import com.carinsurance.common.exception.ResourceNotFoundException;
import com.carinsurance.location.Locality;
import com.carinsurance.location.LocalityRepository;
import com.carinsurance.location.Zone;
import com.carinsurance.quotation.dto.QuotationRequest;
import com.carinsurance.quotation.dto.QuotationResponse;
import com.carinsurance.quotation.dto.QuotationResponse.PaymentOption;
import com.carinsurance.quotation.dto.QuotationResponse.PlanQuote;
import com.carinsurance.vehicle.VehicleCatalog;
import com.carinsurance.vehicle.VehicleCatalogService;
import com.carinsurance.vehicle.VehicleCatalogTestData;
import com.carinsurance.vehicle.VehicleType;
import com.carinsurance.vehicle.VehicleValuation;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class QuotationServiceTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 11, 25);
    private static final long VALUATION_ID = 10L;
    private static final long LOCALITY_ID = 20L;
    private static final long ZONE_ID = 2L;

    @Mock
    private VehicleCatalogService catalogService;

    @Mock
    private LocalityRepository localityRepository;

    @Mock
    private QuotationVehicleParameterRepository vehicleParameterRepository;

    @Mock
    private QuotationZoneFactorRepository zoneFactorRepository;

    @Mock
    private QuotationDriverAgeFactorRepository driverAgeFactorRepository;

    @Mock
    private QuotationChargeRepository chargeRepository;

    private QuotationService service;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(TODAY.atStartOfDay().toInstant(ZoneOffset.UTC), ZoneOffset.UTC);
        service = new QuotationService(
                catalogService,
                localityRepository,
                vehicleParameterRepository,
                zoneFactorRepository,
                driverAgeFactorRepository,
                chargeRepository,
                new QuotationProperties(15),
                clock);

        VehicleCatalog catalog =
                VehicleCatalogTestData.catalog(1L, "Toyota", "Corolla", "2.0 XEI CVT", VehicleType.CAR);
        VehicleValuation valuation =
                VehicleCatalogTestData.valuation(catalog, VALUATION_ID, 2021, "20000.00", LocalDate.of(2026, 1, 1));
        Zone zone = new Zone("GBA");
        zone.setId(ZONE_ID);
        Locality locality = new Locality("MORON", "Buenos Aires", "1708", zone);
        locality.setId(LOCALITY_ID);

        given(catalogService.getSelectableValuation(VALUATION_ID)).willReturn(valuation);
        given(localityRepository.findWithZoneById(LOCALITY_ID)).willReturn(Optional.of(locality));
        given(vehicleParameterRepository.findByVehicleType(VehicleType.CAR))
                .willReturn(Optional.of(new QuotationVehicleParameter(
                        VehicleType.CAR,
                        new BigDecimal("200.00"),
                        new BigDecimal("0.0040"),
                        new BigDecimal("0.0150"))));
        given(zoneFactorRepository.findByZoneId(ZONE_ID))
                .willReturn(Optional.of(new QuotationZoneFactor(
                        zone, new BigDecimal("1.20"), new BigDecimal("1.60"), new BigDecimal("1.10"))));
        given(driverAgeFactorRepository.findByAge(18))
                .willReturn(Optional.of(
                        new QuotationDriverAgeFactor(18, 20, new BigDecimal("1.60"), new BigDecimal("1.70"))));
        given(driverAgeFactorRepository.findByAge(22))
                .willReturn(Optional.of(
                        new QuotationDriverAgeFactor(21, 24, new BigDecimal("1.35"), new BigDecimal("1.40"))));
        given(chargeRepository.getCurrent())
                .willReturn(new QuotationCharge(
                        QuotationCharge.SINGLETON_ID,
                        new BigDecimal("0.0500"),
                        new BigDecimal("10.00"),
                        new BigDecimal("0.0800"),
                        new BigDecimal("0.2100")));
    }

    private static QuotationRequest request(LocalDate birthDate) {
        return new QuotationRequest(VALUATION_ID, birthDate, LOCALITY_ID);
    }

    @Test
    void quotesEveryPlan() {
        QuotationResponse response = service.quote(request(LocalDate.of(2004, 5, 1)));

        assertThat(response.validUntil()).isEqualTo(LocalDate.of(2026, 12, 10));
        assertThat(response.vehicle().description()).isEqualTo("Toyota Corolla 2.0 XEI CVT");
        assertThat(response.vehicle().year()).isEqualTo(2021);
        assertThat(response.vehicle().vehicleType()).isEqualTo(VehicleType.CAR);
        assertThat(response.vehicle().sumInsured()).isEqualByComparingTo("20000.00");
        assertThat(response.risk()).isEqualTo(new QuotationResponse.Risk("1708", "MORON", "Buenos Aires", "GBA", 22));
        assertThat(response.plans())
                .extracting(PlanQuote::name, quote -> quote.coverages().size(), PlanQuote::netPremium)
                .containsExactly(
                        tuple(Plan.CIVIL_LIABILITY, 1, new BigDecimal("324.00")),
                        tuple(Plan.THIRD_PARTY_EXTENDED, 2, new BigDecimal("452.00")),
                        tuple(Plan.FULL_COVERAGE, 3, new BigDecimal("914.00")));
        assertThat(response.plans())
                .allSatisfy(quote -> assertThat(quote.paymentOptions())
                        .extracting(PaymentOption::installments)
                        .containsExactly(1, 12));
        PlanQuote fullCoverage = response.plans().get(2);
        assertThat(fullCoverage.paymentOptions().get(0).totalPremium()).isEqualByComparingTo("1173.34");
        assertThat(fullCoverage.paymentOptions().get(1).totalPremium()).isEqualByComparingTo("1267.21");
    }

    @Test
    void driverAgeIsCompletedYearsAtTheIssueDate() {
        // turns 23 tomorrow
        QuotationResponse response = service.quote(request(LocalDate.of(2003, 11, 26)));

        assertThat(response.risk().driverAge()).isEqualTo(22);
    }

    @Test
    void driverTurningEighteenTodayIsAccepted() {
        QuotationResponse response = service.quote(request(LocalDate.of(2008, 11, 25)));

        assertThat(response.risk().driverAge()).isEqualTo(18);
    }

    @Test
    void driverUnderEighteenIsRejected() {
        assertThatThrownBy(() -> service.quote(request(LocalDate.of(2008, 11, 26))))
                .isInstanceOf(UnderAgeDriverException.class)
                .hasMessage("Driver must be at least 18 years old (age: 17)");
        verifyNoInteractions(catalogService, localityRepository);
    }

    @Test
    void unknownLocalityIsNotFound() {
        given(localityRepository.findWithZoneById(anyLong())).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.quote(request(LocalDate.of(1990, 1, 1))))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Locality");
    }
}
