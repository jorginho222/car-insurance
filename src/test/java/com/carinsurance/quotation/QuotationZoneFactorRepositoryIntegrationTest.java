package com.carinsurance.quotation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.carinsurance.TestcontainersConfiguration;
import com.carinsurance.location.Zone;
import com.carinsurance.location.ZoneRepository;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;

/** Runs against the factors seeded by migration V8 (table renamed in V9). */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class QuotationZoneFactorRepositoryIntegrationTest {

    @Autowired
    private QuotationZoneFactorRepository repository;

    @Autowired
    private ZoneRepository zoneRepository;

    @Test
    void everyZoneHasItsSeededFactors() {
        assertThat(repository.count()).isEqualTo(4);
        assertFactors("GBA", "1.20", "1.60", "1.10");
        assertFactors("CABA", "1.15", "1.30", "1.05");
        assertFactors("Ciudades grandes del interior", "1.00", "1.00", "1.25");
        assertFactors("Resto del país", "0.85", "0.70", "0.90");
    }

    @Test
    void aZoneCannotHaveTwoFactorRows() {
        Zone gba = zoneRepository.findByName("GBA").orElseThrow();

        assertThatThrownBy(() -> repository.saveAndFlush(
                        new QuotationZoneFactor(gba, BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private void assertFactors(String zoneName, String civilLiability, String theft, String damage) {
        Zone zone = zoneRepository.findByName(zoneName).orElseThrow();
        QuotationZoneFactor factor = repository.findByZoneId(zone.getId()).orElseThrow();
        assertThat(factor.getCivilLiability()).isEqualByComparingTo(civilLiability);
        assertThat(factor.getTheft()).isEqualByComparingTo(theft);
        assertThat(factor.getDamage()).isEqualByComparingTo(damage);
    }
}
