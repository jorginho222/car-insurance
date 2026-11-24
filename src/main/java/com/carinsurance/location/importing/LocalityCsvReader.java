package com.carinsurance.location.importing;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * Reads the postal code master file ({@code provincia,id,localidad,cp,id_prov_mstr}). Columns are found by header
 * name; rows without a postal code and exact duplicates are dropped.
 */
@Component
public class LocalityCsvReader {

    static final String PROVINCE = "provincia";
    static final String NAME = "localidad";
    static final String POSTAL_CODE = "cp";

    public List<LocalityRow> read(InputStream csv) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(csv, StandardCharsets.UTF_8))) {
            String header = reader.readLine();
            if (header == null) {
                throw new IllegalArgumentException("Locality CSV is empty");
            }
            List<String> columns = parseLine(stripBom(header), 1);
            int provinceIdx = indexOf(columns, PROVINCE);
            int nameIdx = indexOf(columns, NAME);
            int postalCodeIdx = indexOf(columns, POSTAL_CODE);

            Set<LocalityRow> rows = new LinkedHashSet<>();
            String line;
            int lineNumber = 1;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                if (line.isBlank()) {
                    continue;
                }
                List<String> fields = parseLine(line, lineNumber);
                if (fields.size() != columns.size()) {
                    throw new IllegalArgumentException("Locality CSV line %d has %d fields, expected %d"
                            .formatted(lineNumber, fields.size(), columns.size()));
                }
                String postalCode = fields.get(postalCodeIdx).trim();
                if (postalCode.isEmpty()) {
                    continue;
                }
                rows.add(new LocalityRow(
                        fields.get(provinceIdx).trim(), fields.get(nameIdx).trim(), postalCode));
            }
            return List.copyOf(rows);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not read locality CSV", e);
        }
    }

    private static int indexOf(List<String> columns, String column) {
        int idx = columns.indexOf(column);
        if (idx < 0) {
            throw new IllegalArgumentException("Locality CSV has no '%s' column".formatted(column));
        }
        return idx;
    }

    private static String stripBom(String header) {
        return header.startsWith("﻿") ? header.substring(1) : header;
    }

    /** Splits one CSV line: fields may be quoted, with {@code ""} as an escaped quote and commas allowed inside. */
    static List<String> parseLine(String line, int lineNumber) {
        List<String> fields = new ArrayList<>();
        StringBuilder field = new StringBuilder();
        boolean quoted = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (quoted) {
                if (c == '"' && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    field.append('"');
                    i++;
                } else if (c == '"') {
                    quoted = false;
                } else {
                    field.append(c);
                }
            } else if (c == '"') {
                quoted = true;
            } else if (c == ',') {
                fields.add(field.toString());
                field.setLength(0);
            } else {
                field.append(c);
            }
        }
        if (quoted) {
            throw new IllegalArgumentException("Locality CSV line %d has an unclosed quote".formatted(lineNumber));
        }
        fields.add(field.toString());
        return fields;
    }
}
