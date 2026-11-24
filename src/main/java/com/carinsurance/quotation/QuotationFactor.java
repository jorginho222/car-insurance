package com.carinsurance.quotation;

import com.carinsurance.location.Zone;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;

/** Multipliers a zone applies to the price of each coverage when quoting. One per zone. */
@Entity
@Table(name = "quotation_factor")
public class QuotationFactor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "zone_id", nullable = false, unique = true)
    private Zone zone;

    @Column(name = "civil_liability", nullable = false, precision = 5, scale = 2)
    private BigDecimal civilLiability;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal theft;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal damage;

    protected QuotationFactor() {}

    public QuotationFactor(Zone zone, BigDecimal civilLiability, BigDecimal theft, BigDecimal damage) {
        this.zone = zone;
        this.civilLiability = civilLiability;
        this.theft = theft;
        this.damage = damage;
    }

    public Long getId() {
        return id;
    }

    public Zone getZone() {
        return zone;
    }

    public BigDecimal getCivilLiability() {
        return civilLiability;
    }

    public BigDecimal getTheft() {
        return theft;
    }

    public BigDecimal getDamage() {
        return damage;
    }
}
