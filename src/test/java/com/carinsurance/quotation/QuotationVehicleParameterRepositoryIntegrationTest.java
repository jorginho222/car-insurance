package com.carinsurance.quotation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.carinsurance.TestcontainersConfiguration;
import com.carinsurance.vehicle.VehicleType;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

/** Runs against the parameters seeded by migration V9. */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class QuotationVehicleParameterRepositoryIntegrationTest {

    @Autowired
    private QuotationVehicleParameterRepository repository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void everyVehicleTypeHasItsSeededParameters() {
        assertThat(repository.count()).isEqualTo(VehicleType.values().length);
        assertParameters(VehicleType.CAR, "200.00", "0.0040", "0.0150");
        assertParameters(VehicleType.MOTORCYCLE, "80.00", "0.0180", "0.0300");
        assertParameters(VehicleType.SUV, "227.00", "0.0055", "0.0130");
        assertParameters(VehicleType.PICKUP, "253.00", "0.0065", "0.0140");
        assertParameters(VehicleType.VAN, "267.00", "0.0045", "0.0160");
        assertParameters(VehicleType.TRUCK, "600.00", "0.0030", "0.0180");
        assertParameters(VehicleType.BUS, "1000.00", "0.0015", "0.0200");
    }

    @Test
    void aVehicleTypeCannotHaveTwoParameterRows() {
        assertThatThrownBy(() -> repository.saveAndFlush(new QuotationVehicleParameter(
                        VehicleType.CAR, BigDecimal.ONE, new BigDecimal("0.01"), new BigDecimal("0.01"))))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void ratesMustBeFractionsBelowOne() {
        // 4 meant as 4% is rejected: it must be 0.04.
        assertThatThrownBy(() -> jdbcTemplate.update(
                        "UPDATE quotation_vehicle_parameter SET theft_rate = 4 WHERE vehicle_type = 'CAR'"))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> jdbcTemplate.update(
                        "UPDATE quotation_vehicle_parameter SET damage_rate = 1 WHERE vehicle_type = 'CAR'"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private void assertParameters(
            VehicleType type, String civilLiabilityBaseAmount, String theftRate, String damageRate) {
        QuotationVehicleParameter parameter = repository.findByVehicleType(type).orElseThrow();
        assertThat(parameter.getCivilLiabilityBaseAmount()).isEqualByComparingTo(civilLiabilityBaseAmount);
        assertThat(parameter.getTheftRate()).isEqualByComparingTo(theftRate);
        assertThat(parameter.getDamageRate()).isEqualByComparingTo(damageRate);
    }
}
