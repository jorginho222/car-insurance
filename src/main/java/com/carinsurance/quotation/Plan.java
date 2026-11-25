package com.carinsurance.quotation;

import java.util.List;

/** Insurance plans. Each one includes the coverages of the previous one plus one more. */
public enum Plan {
    CIVIL_LIABILITY(List.of(CoverageType.LIABILITY)),
    THIRD_PARTY_EXTENDED(List.of(CoverageType.LIABILITY, CoverageType.THEFT)),
    FULL_COVERAGE(List.of(CoverageType.LIABILITY, CoverageType.THEFT, CoverageType.DAMAGE));

    private final List<CoverageType> coverages;

    Plan(List<CoverageType> coverages) {
        this.coverages = coverages;
    }

    public List<CoverageType> getCoverages() {
        return coverages;
    }
}
