package com.carinsurance.quotation;

import java.math.BigDecimal;
import java.util.List;

/**
 * How much a quoted plan costs when paid in {@code installments}. Stored as JSON in its plan; the first installment
 * takes the leftover cents of dividing the total.
 */
public record PaymentOption(
        int installments,
        BigDecimal adminSurcharge,
        BigDecimal issuanceFee,
        BigDecimal financingSurcharge,
        BigDecimal taxableBase,
        BigDecimal vat,
        BigDecimal totalPremium,
        BigDecimal firstInstallment,
        BigDecimal installmentAmount) {

    /** The installment counts every plan is quoted with, one payment option each. */
    public static final List<Integer> INSTALLMENT_OPTIONS = List.of(1, 12);
}
