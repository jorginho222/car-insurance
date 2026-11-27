package com.carinsurance.policy;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Serializes the issuing of policies for the same license plate, so two concurrent requests can't both pass the
 * overlapping-policy check. Uses a PostgreSQL transaction-level advisory lock, released when the transaction ends.
 */
@Component
public class LicensePlateLock {

    private final JdbcTemplate jdbcTemplate;

    public LicensePlateLock(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /** Waits until no other transaction holds the lock of {@code licensePlate} (normalized), then takes it. */
    @Transactional(propagation = Propagation.MANDATORY)
    public void lock(String licensePlate) {
        jdbcTemplate.query("SELECT pg_advisory_xact_lock(hashtext(?))", rs -> {}, "policy-plate:" + licensePlate);
    }
}
