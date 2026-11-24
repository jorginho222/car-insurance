package com.carinsurance.quotation;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;

/**
 * Multipliers a driver age range applies to the civil liability and damage coverage prices when quoting. Ranges are
 * inclusive on both ends, and a {@code null} max age means no upper limit. The database rejects overlapping ranges,
 * gaps between ranges, and a last range with a max age.
 */
@Entity
@Table(name = "quotation_driver_age_factor")
public class QuotationDriverAgeFactor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "min_age", nullable = false)
    private Integer minAge;

    @Column(name = "max_age")
    private Integer maxAge;

    @Column(name = "civil_liability", nullable = false, precision = 5, scale = 2)
    private BigDecimal civilLiability;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal damage;

    protected QuotationDriverAgeFactor() {}

    public QuotationDriverAgeFactor(Integer minAge, Integer maxAge, BigDecimal civilLiability, BigDecimal damage) {
        this.minAge = minAge;
        this.maxAge = maxAge;
        this.civilLiability = civilLiability;
        this.damage = damage;
    }

    public Long getId() {
        return id;
    }

    public Integer getMinAge() {
        return minAge;
    }

    /** {@code null} when the range has no upper limit. */
    public Integer getMaxAge() {
        return maxAge;
    }

    public BigDecimal getCivilLiability() {
        return civilLiability;
    }

    public BigDecimal getDamage() {
        return damage;
    }
}
