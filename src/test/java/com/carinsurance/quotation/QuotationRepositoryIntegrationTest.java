package com.carinsurance.quotation;

import static org.assertj.core.api.Assertions.assertThat;

import com.carinsurance.TestcontainersConfiguration;
import com.carinsurance.location.Locality;
import com.carinsurance.location.LocalityRepository;
import com.carinsurance.vehicle.VehicleCatalog;
import com.carinsurance.vehicle.VehicleCatalogRepository;
import com.carinsurance.vehicle.VehicleCatalogTestData;
import com.carinsurance.vehicle.VehicleType;
import com.carinsurance.vehicle.VehicleValuation;
import com.carinsurance.vehicle.VehicleValuationRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class QuotationRepositoryIntegrationTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 11, 25);

    @Autowired
    private QuotationRepository repository;

    @Autowired
    private VehicleCatalogRepository catalogRepository;

    @Autowired
    private VehicleValuationRepository valuationRepository;

    @Autowired
    private LocalityRepository localityRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private TransactionTemplate transactionTemplate;

    private VehicleValuation valuation;
    private Locality locality;

    @BeforeEach
    void setUp() {
        VehicleCatalogTestData data = new VehicleCatalogTestData(catalogRepository, valuationRepository);
        VehicleCatalog catalog =
                data.saveCatalog(VehicleCatalogTestData.uniqueMake(), "Alpha", "1.0 Base", VehicleType.CAR);
        valuation = data.saveValuation(catalog, 2021, "20000.00", LocalDate.of(2026, 1, 1));
        locality = localityRepository.findByPostalCode("1602").getFirst();
    }

    private static PaymentOption option(int installments, String total, String first, String each) {
        return new PaymentOption(
                installments,
                new BigDecimal("45.70"),
                new BigDecimal("10.00"),
                new BigDecimal(installments == 1 ? "0.00" : "77.58"),
                new BigDecimal("1047.28"),
                new BigDecimal("219.93"),
                new BigDecimal(total),
                new BigDecimal(first),
                new BigDecimal(each));
    }

    private Quotation newQuotation(LocalDate validUntil) {
        Quotation quotation = new Quotation(
                "T-" + UUID.randomUUID().toString().substring(0, 16),
                "driver@example.com",
                Instant.now().truncatedTo(ChronoUnit.MICROS),
                validUntil,
                valuation,
                locality,
                LocalDate.of(2004, 5, 1),
                22);
        quotation.addPlan(new QuotationPlan(
                Plan.CIVIL_LIABILITY,
                List.of(new PlanCoverage(CoverageType.LIABILITY, new BigDecimal("324.00"))),
                new BigDecimal("324.00"),
                List.of(option(1, "1173.34", "1173.34", "1173.34"))));
        quotation.addPlan(new QuotationPlan(
                Plan.FULL_COVERAGE,
                List.of(
                        new PlanCoverage(CoverageType.LIABILITY, new BigDecimal("324.00")),
                        new PlanCoverage(CoverageType.THEFT, new BigDecimal("128.00")),
                        new PlanCoverage(CoverageType.DAMAGE, new BigDecimal("462.00"))),
                new BigDecimal("914.00"),
                List.of(option(1, "1173.34", "1173.34", "1173.34"), option(12, "1267.21", "105.61", "105.60"))));
        return repository.saveAndFlush(quotation);
    }

    @Test
    void storesAndReadsBackPlansCoveragesAndPaymentOptions() {
        Quotation saved = newQuotation(TODAY.plusDays(15));

        transactionTemplate.executeWithoutResult(status -> {
            Quotation found = repository.findDetailedById(saved.getId()).orElseThrow();

            assertThat(found.getStatus()).isEqualTo(QuotationStatus.QUOTED);
            assertThat(found.getQuotedAt()).isEqualTo(saved.getQuotedAt());
            assertThat(found.getValidUntil()).isEqualTo(TODAY.plusDays(15));
            assertThat(found.getValuation().getCatalog().getModel()).isEqualTo("Alpha");
            assertThat(found.getLocality().getZone().getName()).isEqualTo("GBA");
            assertThat(found.getDriverAge()).isEqualTo(22);
            assertThat(found.getPlans())
                    .extracting(QuotationPlan::getName)
                    .containsExactly(Plan.CIVIL_LIABILITY, Plan.FULL_COVERAGE);

            QuotationPlan fullCoverage = found.getPlans().get(1);
            assertThat(fullCoverage.getCoverages())
                    .extracting(PlanCoverage::type)
                    .containsExactly(CoverageType.LIABILITY, CoverageType.THEFT, CoverageType.DAMAGE);
            assertThat(fullCoverage.getCoverages().get(2).premium()).isEqualByComparingTo("462.00");
            assertThat(fullCoverage.getNetPremium()).isEqualByComparingTo("914.00");
            assertThat(fullCoverage.getPaymentOptions()).hasSize(2);
            PaymentOption twelve = fullCoverage.getPaymentOptions().get(1);
            assertThat(twelve.installments()).isEqualTo(12);
            assertThat(twelve.financingSurcharge()).isEqualByComparingTo("77.58");
            assertThat(twelve.totalPremium()).isEqualByComparingTo("1267.21");
            assertThat(twelve.firstInstallment()).isEqualByComparingTo("105.61");
            assertThat(twelve.installmentAmount()).isEqualByComparingTo("105.60");
        });
    }

    @Test
    void paymentOptionsAreStoredAsJsonb() {
        Quotation saved = newQuotation(TODAY.plusDays(15));

        String type = jdbcTemplate.queryForObject("""
                SELECT jsonb_typeof(payment_options) FROM quotation_plan
                WHERE quotation_id = ? AND name = 'FULL_COVERAGE'""", String.class, saved.getId());
        Integer installments = jdbcTemplate.queryForObject("""
                SELECT (payment_options -> 1 ->> 'installments')::int FROM quotation_plan
                WHERE quotation_id = ? AND name = 'FULL_COVERAGE'""", Integer.class, saved.getId());

        assertThat(type).isEqualTo("array");
        assertThat(installments).isEqualTo(12);
    }

    @Test
    void expiresOnlyQuotedQuotationsPastTheirValidUntil() {
        Quotation pastDue = newQuotation(TODAY.minusDays(1));
        Quotation lastValidDay = newQuotation(TODAY);
        Quotation acceptedPastDue = newQuotation(TODAY.minusDays(1));
        jdbcTemplate.update("UPDATE quotation SET status = 'ACCEPTED' WHERE id = ?", acceptedPastDue.getId());

        int expired = transactionTemplate.execute(status -> repository.expireQuotedBefore(TODAY));

        assertThat(expired).isGreaterThanOrEqualTo(1);
        assertThat(storedStatus(pastDue)).isEqualTo("EXPIRED");
        assertThat(storedStatus(lastValidDay)).isEqualTo("QUOTED");
        assertThat(storedStatus(acceptedPastDue)).isEqualTo("ACCEPTED");
    }

    @Test
    void deletingAQuotationDeletesItsPlansAndCoverages() {
        Quotation saved = newQuotation(TODAY.plusDays(15));

        repository.deleteById(saved.getId());

        assertThat(count("SELECT count(*) FROM quotation_plan WHERE quotation_id = ?", saved.getId()))
                .isZero();
        assertThat(count("""
                        SELECT count(*) FROM quotation_plan_coverage c
                        WHERE NOT EXISTS (SELECT 1 FROM quotation_plan p WHERE p.id = c.quotation_plan_id)""", null)).isZero();
    }

    private String storedStatus(Quotation quotation) {
        return jdbcTemplate.queryForObject(
                "SELECT status FROM quotation WHERE id = ?", String.class, quotation.getId());
    }

    private int count(String sql, Long id) {
        return id == null
                ? jdbcTemplate.queryForObject(sql, Integer.class)
                : jdbcTemplate.queryForObject(sql, Integer.class, id);
    }
}
