package com.carinsurance.quotation;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import java.math.BigDecimal;

/** The premium of one coverage of a quoted plan. */
@Embeddable
public record PlanCoverage(
        @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20)
        CoverageType type,

        @Column(nullable = false, precision = 14, scale = 2) BigDecimal premium) {}
