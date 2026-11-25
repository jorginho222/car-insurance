package com.carinsurance.quotation;

import org.springframework.data.jpa.repository.JpaRepository;

public interface QuotationChargeRepository extends JpaRepository<QuotationCharge, Short> {

    /** The single charges row, seeded by migration V11. */
    default QuotationCharge getCurrent() {
        return findById(QuotationCharge.SINGLETON_ID)
                .orElseThrow(() -> new IllegalStateException("quotation_charge row is missing"));
    }
}
