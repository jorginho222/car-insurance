package com.carinsurance.location.importing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;

class LocalityCsvReaderTest {

    private static final String HEADER = "\"provincia\",\"id\",\"localidad\",\"cp\",\"id_prov_mstr\"\n";

    private final LocalityCsvReader reader = new LocalityCsvReader();

    @Test
    void readsQuotedFields() {
        List<LocalityRow> rows = read(HEADER
                + "\"Santa Fe\",\"1\",\"ROSARIO\",\"2000\",\"21\"\n"
                + "\"Tucuman\",\"2\",\"VILLA MUÑECAS\",\"4000\",\"24\"\n");

        assertThat(rows)
                .containsExactly(
                        new LocalityRow("Santa Fe", "ROSARIO", "2000"),
                        new LocalityRow("Tucuman", "VILLA MUÑECAS", "4000"));
    }

    @Test
    void handlesEmbeddedCommasEscapedQuotesAndBom() {
        List<LocalityRow> rows =
                read("﻿" + HEADER + "\"Cordoba\",\"1\",\"BARRIO \"\"EL ALTO\"\", NORTE\",\"5000\",\"14\"\n");

        assertThat(rows).containsExactly(new LocalityRow("Cordoba", "BARRIO \"EL ALTO\", NORTE", "5000"));
    }

    @Test
    void skipsRowsWithoutPostalCodeAndDuplicates() {
        List<LocalityRow> rows = read(HEADER
                + "\"Ciudad Autonoma de Buenos Aires\",\"5001\",\"Ciudad Autonoma de Buenos Aires\",\"\",\"02\"\n"
                + "\"Buenos Aires\",\"1\",\"CANNING\",\"1804\",\"06\"\n"
                + "\"Buenos Aires\",\"2\",\"CANNING\",\"1804\",\"06\"\n"
                + "\n");

        assertThat(rows).containsExactly(new LocalityRow("Buenos Aires", "CANNING", "1804"));
    }

    @Test
    void missingColumnIsRejected() {
        assertThatThrownBy(() -> read("\"provincia\",\"localidad\"\n\"Salta\",\"SALTA\"\n"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Locality CSV has no 'cp' column");
    }

    @Test
    void wrongFieldCountIsRejectedWithLineNumber() {
        assertThatThrownBy(() -> read(HEADER + "\"Salta\",\"1\",\"SALTA\",\"4400\",\"17\"\n\"Salta\",\"SALTA\"\n"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Locality CSV line 3 has 2 fields, expected 5");
    }

    private List<LocalityRow> read(String csv) {
        return reader.read(new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8)));
    }
}
