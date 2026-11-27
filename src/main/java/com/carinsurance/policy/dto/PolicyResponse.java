package com.carinsurance.policy.dto;

import com.carinsurance.insured.dto.InsuredResponse;
import com.carinsurance.insured.dto.InsuredVehicleResponse;
import com.carinsurance.location.dto.LocalityResponse;
import com.carinsurance.policy.PaymentMethod;
import com.carinsurance.quotation.PaymentOption;
import com.carinsurance.quotation.Plan;
import com.carinsurance.quotation.PlanCoverage;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/** A policy. It covers [validFrom, validTo): validTo is the day after the last covered day. */
public record PolicyResponse(
        Long id,
        String number,
        LocalDate validFrom,
        LocalDate validTo,
        Quotation quotation,
        PolicyPlan plan,
        PaymentOption paymentOption,
        PaymentMethod paymentMethod,
        InsuredResponse insured,
        InsuredVehicleResponse vehicle,
        LocalityResponse locality,
        Instant createdAt) {

    public record Quotation(Long id, String code) {}

    public record PolicyPlan(Long id, Plan name, List<PlanCoverage> coverages, BigDecimal netPremium) {}
}
