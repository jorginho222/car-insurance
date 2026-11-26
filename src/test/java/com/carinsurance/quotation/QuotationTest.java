package com.carinsurance.quotation;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class QuotationTest {

    private static final LocalDate VALID_UNTIL = LocalDate.of(2026, 12, 10);

    private static Quotation quotation() {
        return new Quotation(
                "Q-2026-000001",
                "driver@example.com",
                Instant.parse("2026-11-25T13:00:00Z"),
                VALID_UNTIL,
                null,
                null,
                LocalDate.of(1990, 1, 1),
                36);
    }

    @Test
    void newQuotationIsQuoted() {
        assertThat(quotation().getStatus()).isEqualTo(QuotationStatus.QUOTED);
    }

    @Test
    void quotedIsStillValidOnItsValidUntilDate() {
        assertThat(quotation().statusOn(VALID_UNTIL)).isEqualTo(QuotationStatus.QUOTED);
    }

    @Test
    void quotedIsExpiredTheDayAfterItsValidUntilDate() {
        assertThat(quotation().statusOn(VALID_UNTIL.plusDays(1))).isEqualTo(QuotationStatus.EXPIRED);
    }

    @Test
    void acceptedStaysAcceptedAfterItsValidUntilDate() throws Exception {
        Quotation quotation = quotation();
        setStatus(quotation, QuotationStatus.ACCEPTED);

        assertThat(quotation.statusOn(VALID_UNTIL.plusDays(30))).isEqualTo(QuotationStatus.ACCEPTED);
    }

    @Test
    void addPlanLinksThePlanToTheQuotation() {
        Quotation quotation = quotation();
        QuotationPlan plan = new QuotationPlan(Plan.CIVIL_LIABILITY, List.of(), BigDecimal.ZERO, List.of());

        quotation.addPlan(plan);

        assertThat(quotation.getPlans()).containsExactly(plan);
        assertThat(plan.getQuotation()).isSameAs(quotation);
    }

    /** There is no way to accept a quotation yet (it comes with policies), so the test sets the field directly. */
    private static void setStatus(Quotation quotation, QuotationStatus status) throws Exception {
        Field field = Quotation.class.getDeclaredField("status");
        field.setAccessible(true);
        field.set(quotation, status);
    }
}
