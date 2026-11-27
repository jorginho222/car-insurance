package com.carinsurance.quotation.dto;

import com.carinsurance.quotation.PaymentOption;
import com.carinsurance.quotation.Plan;
import com.carinsurance.quotation.PlanCoverage;
import com.carinsurance.quotation.QuotationStatus;
import com.carinsurance.vehicle.VehicleType;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/** A stored quotation for every plan, in {@code Plan} order. */
public record QuotationResponse(
        Long id,
        String code,
        String email,
        QuotationStatus status,
        Instant quotedAt,
        LocalDate validUntil,
        Vehicle vehicle,
        Risk risk,
        List<PlanQuote> plans) {

    public record Vehicle(
            String description,
            String make,
            String model,
            String modelVersion,
            Integer year,
            VehicleType vehicleType,
            BigDecimal sumInsured) {}

    public record Risk(String postalCode, String locality, String province, String zone, int driverAge) {}

    /** {@code id} identifies the quoted plan, e.g. to create a policy from it. */
    public record PlanQuote(
            Long id,
            Plan name,
            List<PlanCoverage> coverages,
            BigDecimal netPremium,
            List<PaymentOption> paymentOptions) {}
}
