package com.carinsurance.policy;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Issues policy numbers such as {@code AU-2026-0000123}: the issue year and a correlative number that restarts at 1
 * every year.
 *
 * <p>Must run inside the transaction that stores the policy. The year's counter row stays locked until that
 * transaction ends, so concurrent policies get distinct numbers, and a rolled-back policy gives its number back.
 */
@Component
public class PolicyNumberGenerator {

    private static final String NEXT_VALUE = """
            INSERT INTO policy_number_counter (year, last_value) VALUES (?, 1)
            ON CONFLICT (year) DO UPDATE SET last_value = policy_number_counter.last_value + 1
            RETURNING last_value""";

    private final JdbcTemplate jdbcTemplate;

    public PolicyNumberGenerator(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public String next(int year) {
        Long number = jdbcTemplate.queryForObject(NEXT_VALUE, Long.class, year);
        return format(year, number);
    }

    static String format(int year, long number) {
        return "AU-%d-%07d".formatted(year, number);
    }
}
