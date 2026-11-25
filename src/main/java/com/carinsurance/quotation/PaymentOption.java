package com.carinsurance.quotation;

import java.math.BigDecimal;

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
        BigDecimal installmentAmount) {}
