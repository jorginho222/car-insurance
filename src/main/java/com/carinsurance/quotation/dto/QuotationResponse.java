package com.carinsurance.quotation.dto;

import com.carinsurance.quotation.CoverageType;
import com.carinsurance.quotation.Plan;
import com.carinsurance.vehicle.VehicleType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record QuotationResponse(LocalDate validUntil, Vehicle vehicle, Risk risk, PlanQuote plan) {

    public record Vehicle(
            String description,
            String make,
            String model,
            String modelVersion,
            Integer year,
            VehicleType vehicleType,
            BigDecimal sumInsured) {}

    public record Risk(String postalCode, String locality, String province, String zone, int driverAge) {}

    public record PlanQuote(
            Plan name, List<Coverage> coverages, BigDecimal netPremium, List<PaymentOption> paymentOptions) {}

    public record Coverage(CoverageType type, BigDecimal premium) {}

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
}
