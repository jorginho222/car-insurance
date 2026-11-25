package com.carinsurance.quotation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.carinsurance.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Runs against the factors seeded by migration V10. Statements that must be rejected run through {@link JdbcTemplate}
 * without a surrounding transaction, so each one commits on its own and the deferred gap check runs.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class QuotationDriverAgeFactorRepositoryIntegrationTest {

    @Autowired
    private QuotationDriverAgeFactorRepository repository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Test
    void everyRangeHasItsSeededFactors() {
        assertThat(repository.count()).isEqualTo(6);
        assertRange(18, 20, "1.60", "1.70");
        assertRange(21, 24, "1.35", "1.40");
        assertRange(25, 29, "1.10", "1.10");
        assertRange(30, 64, "1.00", "1.00");
        assertRange(65, 74, "1.05", "1.10");
        assertRange(75, null, "1.20", "1.25");
    }

    @Test
    void findByAgeIncludesBothEndsOfEachRange() {
        assertCivilLiabilityForAge(18, "1.60");
        assertCivilLiabilityForAge(20, "1.60");
        assertCivilLiabilityForAge(21, "1.35");
        assertCivilLiabilityForAge(24, "1.35");
        assertCivilLiabilityForAge(25, "1.10");
        assertCivilLiabilityForAge(64, "1.00");
        assertCivilLiabilityForAge(65, "1.05");
        assertCivilLiabilityForAge(74, "1.05");
        assertCivilLiabilityForAge(75, "1.20");
        assertCivilLiabilityForAge(99, "1.20");
        assertThat(repository.findByAge(17)).isEmpty();
    }

    @Test
    void overlappingRangesAreRejected() {
        assertRejected("INSERT INTO quotation_driver_age_factor (min_age, max_age, civil_liability, damage)"
                + " VALUES (20, 22, 1, 1)");
        assertRejected("INSERT INTO quotation_driver_age_factor (min_age, max_age, civil_liability, damage)"
                + " VALUES (80, NULL, 1, 1)");
    }

    @Test
    void gapsBetweenRangesAreRejected() {
        assertRejected("DELETE FROM quotation_driver_age_factor WHERE min_age = 25");
        assertRejected("UPDATE quotation_driver_age_factor SET max_age = 23 WHERE min_age = 21");
    }

    @Test
    void lastRangeMustHaveNoMaxAge() {
        assertRejected("UPDATE quotation_driver_age_factor SET max_age = 90 WHERE min_age = 75");
    }

    @Test
    void splittingARangeInOneTransactionIsAccepted() {
        transactionTemplate.executeWithoutResult(status -> {
            jdbcTemplate.update("UPDATE quotation_driver_age_factor SET max_age = 49 WHERE min_age = 30");
            jdbcTemplate.update("INSERT INTO quotation_driver_age_factor (min_age, max_age, civil_liability, damage)"
                    + " VALUES (50, 64, 0.95, 0.95)");
        });
        try {
            assertCivilLiabilityForAge(49, "1.00");
            assertCivilLiabilityForAge(50, "0.95");
        } finally {
            transactionTemplate.executeWithoutResult(status -> {
                jdbcTemplate.update("DELETE FROM quotation_driver_age_factor WHERE min_age = 50");
                jdbcTemplate.update("UPDATE quotation_driver_age_factor SET max_age = 64 WHERE min_age = 30");
            });
        }
        assertRange(30, 64, "1.00", "1.00");
    }

    private void assertRejected(String sql) {
        assertThatThrownBy(() -> jdbcTemplate.update(sql)).isInstanceOf(DataIntegrityViolationException.class);
    }

    private void assertRange(int minAge, Integer maxAge, String civilLiability, String damage) {
        QuotationDriverAgeFactor factor = repository.findByAge(minAge).orElseThrow();
        assertThat(factor.getMinAge()).isEqualTo(minAge);
        assertThat(factor.getMaxAge()).isEqualTo(maxAge);
        assertThat(factor.getCivilLiability()).isEqualByComparingTo(civilLiability);
        assertThat(factor.getDamage()).isEqualByComparingTo(damage);
    }

    private void assertCivilLiabilityForAge(int age, String civilLiability) {
        assertThat(repository.findByAge(age).orElseThrow().getCivilLiability()).isEqualByComparingTo(civilLiability);
    }
}
