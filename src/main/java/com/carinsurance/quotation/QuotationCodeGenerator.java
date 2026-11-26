package com.carinsurance.quotation;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Issues quotation codes such as {@code Q-2026-000123}: the issue year and a correlative number that restarts at 1
 * every year.
 *
 * <p>Must run inside the transaction that stores the quotation. The year's counter row stays locked until that
 * transaction ends, so concurrent quotations get distinct numbers, and a rolled-back quotation gives its number back.
 */
@Component
public class QuotationCodeGenerator {

    private static final String NEXT_VALUE = """
            INSERT INTO quotation_code_counter (year, last_value) VALUES (?, 1)
            ON CONFLICT (year) DO UPDATE SET last_value = quotation_code_counter.last_value + 1
            RETURNING last_value""";

    private final JdbcTemplate jdbcTemplate;

    public QuotationCodeGenerator(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public String next(int year) {
        Long number = jdbcTemplate.queryForObject(NEXT_VALUE, Long.class, year);
        return format(year, number);
    }

    static String format(int year, long number) {
        return "Q-%d-%06d".formatted(year, number);
    }
}
