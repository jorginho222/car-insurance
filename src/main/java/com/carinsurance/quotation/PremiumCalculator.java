package com.carinsurance.quotation;

import com.carinsurance.quotation.dto.QuotationResponse.Coverage;
import com.carinsurance.quotation.dto.QuotationResponse.PaymentOption;
import com.carinsurance.quotation.dto.QuotationResponse.PlanQuote;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * The premium arithmetic of a quotation, without any lookups. Every amount it returns is rounded to cents (HALF_UP),
 * and sums are built from the rounded parts, so the amounts in a quotation always add up exactly.
 */
final class PremiumCalculator {

    static final List<Integer> INSTALLMENT_OPTIONS = List.of(1, 12);

    private static final int CENTS = 2;

    private PremiumCalculator() {}

    /** The premium of every coverage. Plans pick theirs from this map with {@link #planQuote}. */
    static Map<CoverageType, BigDecimal> coveragePremiums(
            BigDecimal sumInsured,
            QuotationVehicleParameter vehicle,
            QuotationZoneFactor zone,
            QuotationDriverAgeFactor age) {
        Map<CoverageType, BigDecimal> premiums = new EnumMap<>(CoverageType.class);
        for (CoverageType type : CoverageType.values()) {
            premiums.put(type, premium(type, sumInsured, vehicle, zone, age));
        }
        return premiums;
    }

    /** The plan's coverages (in the plan's order), net premium and payment options. */
    static PlanQuote planQuote(Plan plan, Map<CoverageType, BigDecimal> premiums, QuotationCharge charge) {
        List<Coverage> coverages = plan.getCoverages().stream()
                .map(type -> new Coverage(type, premiums.get(type)))
                .toList();
        BigDecimal netPremium = netPremium(coverages);
        List<PaymentOption> paymentOptions = INSTALLMENT_OPTIONS.stream()
                .map(installments -> paymentOption(netPremium, installments, charge))
                .toList();
        return new PlanQuote(plan, coverages, netPremium, paymentOptions);
    }

    static BigDecimal netPremium(List<Coverage> coverages) {
        return coverages.stream().map(Coverage::premium).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Surcharges, fee and IVA for paying the net premium in {@code installments}. The financing surcharge applies
     * only when paying in more than one installment. The first installment takes the leftover cents.
     */
    static PaymentOption paymentOption(BigDecimal netPremium, int installments, QuotationCharge charge) {
        BigDecimal adminSurcharge = round(netPremium.multiply(charge.getAdminSurchargeRate()));
        BigDecimal issuanceFee = round(charge.getIssuanceFeeAmount());
        BigDecimal subtotal = netPremium.add(adminSurcharge).add(issuanceFee);
        BigDecimal financingSurcharge = installments > 1
                ? round(subtotal.multiply(charge.getFinancingSurchargeRate()))
                : round(BigDecimal.ZERO);
        BigDecimal taxableBase = subtotal.add(financingSurcharge);
        BigDecimal vat = round(taxableBase.multiply(charge.getVatRate()));
        BigDecimal total = taxableBase.add(vat);
        BigDecimal installmentAmount = total.divide(BigDecimal.valueOf(installments), CENTS, RoundingMode.DOWN);
        BigDecimal firstInstallment = total.subtract(installmentAmount.multiply(BigDecimal.valueOf(installments - 1)));
        return new PaymentOption(
                installments,
                adminSurcharge,
                issuanceFee,
                financingSurcharge,
                taxableBase,
                vat,
                total,
                firstInstallment,
                installmentAmount);
    }

    private static BigDecimal premium(
            CoverageType type,
            BigDecimal sumInsured,
            QuotationVehicleParameter vehicle,
            QuotationZoneFactor zone,
            QuotationDriverAgeFactor age) {
        BigDecimal premium = switch (type) {
            case LIABILITY ->
                vehicle.getCivilLiabilityBaseAmount()
                        .multiply(zone.getCivilLiability())
                        .multiply(age.getCivilLiability());
            case THEFT -> sumInsured.multiply(vehicle.getTheftRate()).multiply(zone.getTheft());
            case DAMAGE ->
                sumInsured
                        .multiply(vehicle.getDamageRate())
                        .multiply(zone.getDamage())
                        .multiply(age.getDamage());
        };
        return round(premium);
    }

    private static BigDecimal round(BigDecimal amount) {
        return amount.setScale(CENTS, RoundingMode.HALF_UP);
    }
}
