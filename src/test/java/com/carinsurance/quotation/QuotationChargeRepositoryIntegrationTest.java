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

/** Runs against the row seeded by migration V11. */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class QuotationChargeRepositoryIntegrationTest {

    @Autowired
    private QuotationChargeRepository repository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void seededChargesHaveTheDefaults() {
        QuotationCharge charge = repository.getCurrent();

        assertThat(repository.count()).isEqualTo(1);
        assertThat(charge.getAdminSurchargeRate()).isEqualByComparingTo("0.05");
        assertThat(charge.getIssuanceFeeAmount()).isEqualByComparingTo("10.00");
        assertThat(charge.getFinancingSurchargeRate()).isEqualByComparingTo("0.08");
        assertThat(charge.getVatRate()).isEqualByComparingTo("0.21");
    }

    @Test
    void thereCanBeOnlyOneRow() {
        assertThatThrownBy(() -> jdbcTemplate.update("INSERT INTO quotation_charge (id) VALUES (2)"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
