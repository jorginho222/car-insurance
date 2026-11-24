package com.carinsurance.location.importing;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ZoneResolverTest {

    private final ZoneResolver resolver = new ZoneResolver();

    @Test
    void cabaByProvinceOrLocalityName() {
        assertThat(zone("Ciudad Autonoma de Buenos Aires", "PALERMO", "1425")).isEqualTo(ZoneResolver.CABA);
        assertThat(zone("Otra", "Ciudad Autonoma de Buenos Aires", "1000")).isEqualTo(ZoneResolver.CABA);
    }

    @Test
    void gbaByPostalCode() {
        assertThat(zone("Buenos Aires", "FLORIDA", "1602")).isEqualTo(ZoneResolver.GBA);
        assertThat(zone("Buenos Aires", "ADROGUE", "1846")).isEqualTo(ZoneResolver.GBA);
    }

    @Test
    void largeInteriorCitiesByPostalCode() {
        assertThat(zone("Buenos Aires", "LA PLATA", "1900")).isEqualTo(ZoneResolver.LARGE_INTERIOR_CITIES);
        assertThat(zone("Santa Fe", "ROSARIO", "2000")).isEqualTo(ZoneResolver.LARGE_INTERIOR_CITIES);
        assertThat(zone("Neuquen", "NEUQUEN", "8300")).isEqualTo(ZoneResolver.LARGE_INTERIOR_CITIES);
    }

    @Test
    void everythingElseIsRestOfCountry() {
        assertThat(zone("Catamarca", "EL RECREO", "4139")).isEqualTo(ZoneResolver.REST_OF_COUNTRY);
    }

    private String zone(String province, String name, String postalCode) {
        return resolver.zoneName(new LocalityRow(province, name, postalCode));
    }
}
