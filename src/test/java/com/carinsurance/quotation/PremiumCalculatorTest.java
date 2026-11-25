package com.carinsurance.quotation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import com.carinsurance.location.Zone;
import com.carinsurance.quotation.dto.QuotationResponse.Coverage;
import com.carinsurance.quotation.dto.QuotationResponse.PaymentOption;
import com.carinsurance.quotation.dto.QuotationResponse.PlanQuote;
import com.carinsurance.vehicle.VehicleType;
import java.math.BigDecimal;
import java.util.Map;
import org.junit.jupiter.api.Test;

class PremiumCalculatorTest {

    private static final QuotationVehicleParameter CAR = new QuotationVehicleParameter(
            VehicleType.CAR, new BigDecimal("200.00"), new BigDecimal("0.0040"), new BigDecimal("0.0150"));
    private static final QuotationZoneFactor GBA = new QuotationZoneFactor(
            new Zone("GBA"), new BigDecimal("1.20"), new BigDecimal("1.60"), new BigDecimal("1.10"));
    private static final QuotationDriverAgeFactor AGE_21_24 =
            new QuotationDriverAgeFactor(21, 24, new BigDecimal("1.35"), new BigDecimal("1.40"));
    private static final BigDecimal SUM_INSURED = new BigDecimal("20000.00");

    private static QuotationCharge charge(String issuanceFee) {
        return new QuotationCharge(
                QuotationCharge.SINGLETON_ID,
                new BigDecimal("0.0500"),
                new BigDecimal(issuanceFee),
                new BigDecimal("0.0800"),
                new BigDecimal("0.2100"));
    }

    private static PlanQuote quote(Plan plan) {
        return PremiumCalculator.planQuote(
                plan, PremiumCalculator.coveragePremiums(SUM_INSURED, CAR, GBA, AGE_21_24), charge("10.00"));
    }

    @Test
    void premiumsOfEveryCoverage() {
        // liability: 200 x 1.20 x 1.35; theft: 20000 x 0.004 x 1.60 (no age factor); damage: 20000 x 0.015 x 1.10 x
        // 1.40
        assertThat(PremiumCalculator.coveragePremiums(SUM_INSURED, CAR, GBA, AGE_21_24))
                .isEqualTo(Map.of(
                        CoverageType.LIABILITY, new BigDecimal("324.00"),
                        CoverageType.THEFT, new BigDecimal("128.00"),
                        CoverageType.DAMAGE, new BigDecimal("462.00")));
    }

    @Test
    void civilLiabilityOnlyHasLiability() {
        PlanQuote quote = quote(Plan.CIVIL_LIABILITY);

        assertThat(quote.name()).isEqualTo(Plan.CIVIL_LIABILITY);
        assertThat(quote.coverages()).containsExactly(new Coverage(CoverageType.LIABILITY, new BigDecimal("324.00")));
        assertThat(quote.netPremium()).isEqualByComparingTo("324.00");
    }

    @Test
    void thirdPartyExtendedAddsTheft() {
        PlanQuote quote = quote(Plan.THIRD_PARTY_EXTENDED);

        assertThat(quote.coverages())
                .containsExactly(
                        new Coverage(CoverageType.LIABILITY, new BigDecimal("324.00")),
                        new Coverage(CoverageType.THEFT, new BigDecimal("128.00")));
        assertThat(quote.netPremium()).isEqualByComparingTo("452.00");
    }

    @Test
    void fullCoverageAddsDamage() {
        PlanQuote quote = quote(Plan.FULL_COVERAGE);

        assertThat(quote.coverages())
                .containsExactly(
                        new Coverage(CoverageType.LIABILITY, new BigDecimal("324.00")),
                        new Coverage(CoverageType.THEFT, new BigDecimal("128.00")),
                        new Coverage(CoverageType.DAMAGE, new BigDecimal("462.00")));
        assertThat(quote.netPremium()).isEqualByComparingTo("914.00");
        assertThat(quote.paymentOptions())
                .extracting(PaymentOption::installments, PaymentOption::totalPremium)
                .containsExactly(tuple(1, new BigDecimal("1173.34")), tuple(12, new BigDecimal("1267.21")));
    }

    @Test
    void coveragePremiumsAreRoundedHalfUpToCents() {
        // 123.45 x 1.20 x 1.35 = 199.989 -> 199.99
        QuotationVehicleParameter vehicle = new QuotationVehicleParameter(
                VehicleType.CAR, new BigDecimal("123.45"), new BigDecimal("0.0040"), new BigDecimal("0.0150"));

        assertThat(PremiumCalculator.coveragePremiums(SUM_INSURED, vehicle, GBA, AGE_21_24))
                .containsEntry(CoverageType.LIABILITY, new BigDecimal("199.99"));
    }

    @Test
    void singlePaymentHasNoFinancingSurcharge() {
        PaymentOption option = PremiumCalculator.paymentOption(new BigDecimal("1253000.00"), 1, charge("15000.00"));

        assertThat(option.installments()).isEqualTo(1);
        assertThat(option.adminSurcharge()).isEqualByComparingTo("62650.00");
        assertThat(option.issuanceFee()).isEqualByComparingTo("15000.00");
        assertThat(option.financingSurcharge()).isEqualByComparingTo("0.00");
        assertThat(option.taxableBase()).isEqualByComparingTo("1330650.00");
        assertThat(option.vat()).isEqualByComparingTo("279436.50");
        assertThat(option.totalPremium()).isEqualByComparingTo("1610086.50");
        assertThat(option.firstInstallment()).isEqualByComparingTo("1610086.50");
        assertThat(option.installmentAmount()).isEqualByComparingTo("1610086.50");
    }

    @Test
    void twelveInstallmentsAddFinancingAndFirstInstallmentTakesLeftoverCents() {
        PaymentOption option = PremiumCalculator.paymentOption(new BigDecimal("1253000.00"), 12, charge("15000.00"));

        assertThat(option.installments()).isEqualTo(12);
        assertThat(option.adminSurcharge()).isEqualByComparingTo("62650.00");
        assertThat(option.issuanceFee()).isEqualByComparingTo("15000.00");
        assertThat(option.financingSurcharge()).isEqualByComparingTo("106452.00");
        assertThat(option.taxableBase()).isEqualByComparingTo("1437102.00");
        assertThat(option.vat()).isEqualByComparingTo("301791.42");
        assertThat(option.totalPremium()).isEqualByComparingTo("1738893.42");
        assertThat(option.firstInstallment()).isEqualByComparingTo("144907.84");
        assertThat(option.installmentAmount()).isEqualByComparingTo("144907.78");
        assertThat(option.firstInstallment().add(option.installmentAmount().multiply(BigDecimal.valueOf(11))))
                .isEqualByComparingTo(option.totalPremium());
    }

    @Test
    void surchargesAndVatAreRoundedToCents() {
        PaymentOption option = PremiumCalculator.paymentOption(new BigDecimal("914.00"), 12, charge("10.00"));

        // admin 45.70; subtotal 969.70; financing 77.576 -> 77.58; base 1047.28; vat 219.9288 -> 219.93
        assertThat(option.financingSurcharge()).isEqualTo(new BigDecimal("77.58"));
        assertThat(option.taxableBase()).isEqualTo(new BigDecimal("1047.28"));
        assertThat(option.vat()).isEqualTo(new BigDecimal("219.93"));
        assertThat(option.totalPremium()).isEqualTo(new BigDecimal("1267.21"));
        assertThat(option.installmentAmount()).isEqualTo(new BigDecimal("105.60"));
        assertThat(option.firstInstallment()).isEqualTo(new BigDecimal("105.61"));
    }
}
